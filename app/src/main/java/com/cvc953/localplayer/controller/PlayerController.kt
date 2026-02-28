package com.cvc953.localplayer.controller

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.PlayerState
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
) {
    private var mediaPlayer: MediaPlayer? = null
    private val queue = mutableListOf<Song>()
    private var currentIndex = -1

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    private var progressJob: Job? = null
    private var onQueueEnded: (() -> Unit)? = null
    private var onAudioSessionIdChanged: ((Int) -> Unit)? = null

    fun playNow(
        songs: List<Song>,
        startIndex: Int = 0,
    ) {
        queue.clear()
        queue.addAll(songs)
        currentIndex = startIndex
        playCurrent()
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

    private fun playCurrent() {
        if (currentIndex !in queue.indices) {
            stop()
            return
        }
        val song = queue[currentIndex]
        play(song)
    }

    private fun play(song: Song) {
        mediaPlayer?.release()
        mediaPlayer =
            MediaPlayer().apply {
                setDataSource(context, song.uri)
                prepare()
                start()
                // notify listener about audio session id once created
                try {
                    val sid = audioSessionId
                    Log.d("PlayerController", "MediaPlayer created audioSessionId=$sid")
                    onAudioSessionIdChanged?.invoke(sid)
                } catch (t: Throwable) {
                    Log.w("PlayerController", "onAudioSessionIdChanged invoke failed", t)
                }
                setOnCompletionListener {
                    if (currentIndex + 1 < queue.size) {
                        currentIndex++
                        playCurrent()
                    } else {
                        _state.update { it.copy(isPlaying = false) }
                        try {
                            onQueueEnded?.invoke()
                        } catch (_: Exception) {
                        }
                    }
                }
            }

        _state.value = PlayerState(currentSong = song, isPlaying = true, position = 0L, duration = mediaPlayer?.duration?.toLong() ?: 0L)

        startProgressUpdates()
    }

    fun setOnAudioSessionIdChangedListener(listener: ((Int) -> Unit)?) {
        onAudioSessionIdChanged = listener
        try { Log.d("PlayerController", "setOnAudioSessionIdChangedListener registered") } catch (_: Exception) {}
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
        _state.value = PlayerState()
    }

    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _state.update { s -> s.copy(position = position) }
    }

    fun next() {
        if (currentIndex + 1 < queue.size) {
            currentIndex++
            playCurrent()
        } else {
            stop()
        }
    }

    fun previous() {
        if (currentIndex - 1 >= 0) {
            currentIndex--
            playCurrent()
        } else {
            seekTo(0L)
        }
    }

    fun release() {
        progressJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
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
