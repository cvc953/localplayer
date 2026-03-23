package com.cvc953.localplayer.controller

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.PlayerState
import com.cvc953.localplayer.ui.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerController(
    private val context: Context,
    private val scope: CoroutineScope? = null,
) : AudioManager.OnAudioFocusChangeListener {
    private var mediaPlayer: MediaPlayer? = null
    private val queue = mutableListOf<Song>()
    private var currentIndex = -1
    
    // Audio focus
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var pausedByAudioFocus: Boolean = false

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    private var progressJob: Job? = null
    private var onQueueEnded: (() -> Unit)? = null
    private var onAudioSessionIdChanged: ((Int) -> Unit)? = null
    private var onNextAtEnd: (() -> Unit)? = null

    private var pendingSeek: Long? = null
    private var pendingResume: Boolean = false
    private var repeatMode: RepeatMode = RepeatMode.NONE
    fun playNow(
        songs: List<Song>,
        startIndex: Int = 0,
        startPaused: Boolean = false,
        seekToPosition: Long? = null,
        resumeAfterSeek: Boolean = false,
    ) {
        val isSameQueue = queue.size == songs.size && queue.zip(songs).all { it.first.id == it.second.id }
        val isSameIndex = currentIndex == startIndex
        if (isSameQueue && isSameIndex && currentIndex in queue.indices) {
            // Ya está sonando la misma canción en la misma posición, no reiniciar
            return
        }
        queue.clear()
        queue.addAll(songs)
        currentIndex = startIndex
        pendingSeek = seekToPosition
        pendingResume = resumeAfterSeek
        playCurrent(startPaused)
    }

    fun playSong(song: Song) {
        playNow(listOf(song))
    }

    fun queueNext(songs: List<Song>) {
        val insertIndex = (currentIndex + 1).coerceAtMost(queue.size)
        queue.addAll(insertIndex, songs)
    }

    fun queueEnd(songs: List<Song>) {
        queue.addAll(songs)
    }

    /**
     * Replace the internal queue without automatically starting playback.
     * If [keepCurrentSong] is true and the current song exists in [songs],
     * keep the currentIndex pointing to that song; otherwise set to 0.
     */
    fun replaceQueue(songs: List<Song>, keepCurrentSong: Boolean = true) {
        val currentSong = if (currentIndex in queue.indices) queue[currentIndex] else null
        queue.clear()
        queue.addAll(songs)
        currentIndex = if (keepCurrentSong && currentSong != null) {
            val idx = queue.indexOfFirst { it.id == currentSong.id }
            if (idx >= 0) idx else 0
        } else {
            0
        }
    }

    private fun playCurrent(startPaused: Boolean = false) {
        if (currentIndex !in queue.indices) {
            stop()
            return
        }
        val song = queue[currentIndex]
        play(song, startPaused)
    }

    private fun play(song: Song, startPaused: Boolean = false) {
        mediaPlayer?.release()
        
        // Request audio focus before starting playback
        requestAudioFocus()
        
        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, song.uri)
            setOnPreparedListener { mp ->
                // Si hay un seek pendiente, hacerlo aquí
                pendingSeek?.let { pos ->
                    try { mp.seekTo(pos.toInt()) } catch (_: Exception) {}
                    pendingSeek = null
                }
                // Actualizar duración real al preparar
                val realDuration = try { mp.duration.toLong() } catch (_: Exception) { song.duration }
                _state.update { it.copy(duration = realDuration) }
                if (!startPaused || pendingResume) {
                    try { mp.start() } catch (_: Exception) {}
                    pendingResume = false
                }
                
                // Notify audio session id changed immediately after audio starts
                try {
                    val sid = audioSessionId
                    Log.d("PlayerController", "MediaPlayer created audioSessionId=$sid")
                    onAudioSessionIdChanged?.invoke(sid)
                } catch (t: Throwable) {
                    Log.w("PlayerController", "onAudioSessionIdChanged invoke failed", t)
                }
            }
            prepareAsync()
            setOnCompletionListener {
                Log.d("PlayerController", "Song completed - repeatMode: $repeatMode, currentIndex: $currentIndex, queue.size: ${queue.size}")
                when (repeatMode) {
                    RepeatMode.ONE -> {
                        // Repeat the same song
                        Log.d("PlayerController", "RepeatMode.ONE - Repeating current song")
                        playCurrent()
                    }
                    RepeatMode.ALL -> {
                        if (currentIndex + 1 < queue.size) {
                            currentIndex++
                            playCurrent()
                        } else {
                            // Loop back to the beginning
                            Log.d("PlayerController", "RepeatMode.ALL - Looping to beginning")
                            currentIndex = 0
                            playCurrent()
                        }
                    }
                    RepeatMode.NONE -> {
                        if (currentIndex + 1 < queue.size) {
                            currentIndex++
                            playCurrent()
                        } else {
                            // At the end with no repeat - just pause
                            Log.d("PlayerController", "RepeatMode.NONE - End of queue, pausing")
                            _state.update { it.copy(isPlaying = false) }
                            releaseAudioFocus()
                        }
                    }
                }
            }
        }

        // Inicializar el estado con la duración del Song, se actualizará al preparar
        _state.value = PlayerState(currentSong = song, isPlaying = !startPaused || pendingResume, position = 0L, duration = song.duration)

        startProgressUpdates()
    }

    fun setOnAudioSessionIdChangedListener(listener: ((Int) -> Unit)?) {
        onAudioSessionIdChanged = listener
        try { Log.d("PlayerController", "setOnAudioSessionIdChangedListener registered") } catch (_: Exception) {}
    }

    fun setRepeatMode(mode: RepeatMode) {
        repeatMode = mode
        Log.d("PlayerController", "RepeatMode set to: $mode")
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        val s = scope ?: return
        progressJob =
            s.launch {
                while (true) {
                    try {
                        val mp = mediaPlayer ?: break
                        val pos = try { mp.currentPosition.toLong() } catch (_: IllegalStateException) { break }
                        _state.update { it.copy(position = pos) }
                    } catch (_: Exception) {
                        break
                    }
                    delay(200L)
                }
            }
    }

    fun togglePlayPause() {
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) mp.pause() else mp.start()
                val isPlaying = try { mp.isPlaying } catch (_: IllegalStateException) { false }
                val pos = try { mp.currentPosition.toLong() } catch (_: IllegalStateException) { _state.value.position }
                _state.update { s -> s.copy(isPlaying = isPlaying, position = pos) }
            } catch (_: IllegalStateException) {
                // ignore invalid state transitions
                _state.update { s -> s.copy(isPlaying = false) }
            }
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        _state.update { it.copy(isPlaying = false) }
    }

    fun resume() {
        mediaPlayer?.start()
        _state.update { it.copy(isPlaying = true) }
    }

    fun stop() {
        progressJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        releaseAudioFocus()
        _state.value = PlayerState()
    }

    fun seekTo(position: Long) {
        mediaPlayer?.let { mp ->
            val wasPlaying = try { mp.isPlaying } catch (_: Exception) { false }
            try {
                mp.seekTo(position.toInt())
                if (!wasPlaying) mp.pause() // Forzar pausa si estaba en pausa
            } catch (_: Exception) {}
        }
        _state.update { s -> s.copy(position = position) }
    }

    fun next() {
        Log.d("PlayerController", "next() called - repeatMode: $repeatMode, currentIndex: $currentIndex, queue.size: ${queue.size}")
        
        // RepeatMode.ONE means repeat the current song
        if (repeatMode == RepeatMode.ONE) {
            Log.d("PlayerController", "RepeatMode.ONE - Replaying current song")
            playCurrent()
            return
        }
        
        if (currentIndex + 1 < queue.size) {
            currentIndex++
            playCurrent()
        } else {
            // When reaching the end, handle based on repeat mode
            Log.d("PlayerController", "At end of queue - repeatMode: $repeatMode")
            when (repeatMode) {
                RepeatMode.ALL -> {
                    // Loop back to the beginning
                    Log.d("PlayerController", "RepeatMode.ALL - Looping to beginning")
                    currentIndex = 0
                    playCurrent()
                }
                RepeatMode.NONE -> {
                    // Do nothing - stay at the last song
                    Log.d("PlayerController", "RepeatMode.NONE - At end, ignoring next button")
                }
                else -> {
                    // Shouldn't reach here
                    Log.w("PlayerController", "Unexpected repeat mode at end: $repeatMode")
                }
            }
        }
    }

    fun previous() {
        Log.d("PlayerController", "previous() called - repeatMode: $repeatMode, currentIndex: $currentIndex")
        
        // RepeatMode.ONE means repeat the current song
        if (repeatMode == RepeatMode.ONE) {
            Log.d("PlayerController", "RepeatMode.ONE - Replaying current song")
            playCurrent()
            return
        }
        
        if (currentIndex - 1 >= 0) {
            currentIndex--
            playCurrent()
        } else {
            Log.d("PlayerController", "At beginning - seeking to 0")
            seekTo(0L)
        }
    }

    fun release() {
        progressJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        releaseAudioFocus()
    }

    fun getCurrentPosition(): Long {
        return try {
            mediaPlayer?.currentPosition?.toLong() ?: 0L
        } catch (_: IllegalStateException) {
            0L
        }
    }

    /** Return the underlying MediaPlayer audio session id, or 0 if not available. */
    fun getAudioSessionId(): Int {
        return try {
            mediaPlayer?.audioSessionId ?: 0
        } catch (_: Exception) {
            0
        }
    }

    fun getDuration(): Long {
        return try {
            mediaPlayer?.duration?.toLong() ?: 0L
        } catch (_: IllegalStateException) {
            0L
        }
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (_: IllegalStateException) {
            false
        }
    }

    fun setOnQueueEndedListener(listener: (() -> Unit)?) {
        onQueueEnded = listener
    }

    fun setOnNextAtEndListener(listener: (() -> Unit)?) {
        onNextAtEnd = listener
    }

    /**
     * Request audio focus for music playback.
     * Called when playback starts to ensure this app has priority for audio output.
     */
    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener(this)
                    .build()
                
                val result = audioManager.requestAudioFocus(audioFocusRequest!!)
                Log.d("PlayerController", "Audio focus requested (Android 8+): ${if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) "GRANTED" else "DENIED"}")
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.requestAudioFocus(
                    this,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
                Log.d("PlayerController", "Audio focus requested (Android <8): ${if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) "GRANTED" else "DENIED"}")
            }
            pausedByAudioFocus = false
        } catch (e: Exception) {
            Log.w("PlayerController", "Error requesting audio focus", e)
        }
    }

    /**
     * Release audio focus when playback ends.
     * Allows other apps to play audio.
     */
    private fun releaseAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let {
                    audioManager.abandonAudioFocusRequest(it)
                    Log.d("PlayerController", "Audio focus released (Android 8+)")
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(this)
                Log.d("PlayerController", "Audio focus released (Android <8)")
            }
            pausedByAudioFocus = false
        } catch (e: Exception) {
            Log.w("PlayerController", "Error releasing audio focus", e)
        }
    }

    /**
     * Handle audio focus changes from the system.
     * Called when another app gains focus or when we regain focus.
     */
    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // We gained audio focus, resume playback if we paused due to focus loss
                Log.d("PlayerController", "Audio focus: GAINED")
                if (pausedByAudioFocus && mediaPlayer != null) {
                    try {
                        mediaPlayer?.start()
                        _state.update { it.copy(isPlaying = true) }
                        pausedByAudioFocus = false
                    } catch (e: Exception) {
                        Log.w("PlayerController", "Error resuming after audio focus gain", e)
                    }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // We lost audio focus permanently, pause playback
                Log.d("PlayerController", "Audio focus: LOST permanently")
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                        _state.update { it.copy(isPlaying = false) }
                        pausedByAudioFocus = true
                    }
                } catch (e: Exception) {
                    Log.w("PlayerController", "Error pausing after audio focus loss", e)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // We lost audio focus temporarily (e.g., incoming call), pause playback
                Log.d("PlayerController", "Audio focus: LOST (transient)")
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                        _state.update { it.copy(isPlaying = false) }
                        pausedByAudioFocus = true
                    }
                } catch (e: Exception) {
                    Log.w("PlayerController", "Error pausing due to transient audio focus loss", e)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // We lost audio focus transiently but can continue playing at lower volume
                // (e.g., navigation instructions, message notification)
                Log.d("PlayerController", "Audio focus: LOST (transient, can duck)")
                try {
                    // Reduce volume to 30% for ducking
                    mediaPlayer?.setVolume(0.3f, 0.3f)
                    pausedByAudioFocus = false
                } catch (e: Exception) {
                    Log.w("PlayerController", "Error applying audio duck", e)
                }
            }
        }
    }

    companion object {
        @Suppress("ktlint:standard:property-naming")
        @Volatile
        private var INSTANCE: PlayerController? = null

        fun getInstance(
            context: Context,
            scope: CoroutineScope? = null,
        ): PlayerController =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlayerController(context.applicationContext, scope).also { INSTANCE = it }
            }
    }
}
