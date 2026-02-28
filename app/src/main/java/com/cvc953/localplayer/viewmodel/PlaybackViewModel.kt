package com.cvc953.localplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.PlayerController
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.PlayerState
import com.cvc953.localplayer.ui.RepeatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.collections.get
import kotlin.compareTo
import kotlin.text.compareTo
import android.content.Intent
import androidx.core.content.ContextCompat
import com.cvc953.localplayer.services.MusicService

class PlaybackViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val playerController = PlayerController.getInstance(getApplication(), viewModelScope)
    private val prefs = com.cvc953.localplayer.preferences.AppPrefs(getApplication())
    val playerState: StateFlow<PlayerState> = playerController.state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue

    // backup of explicit order before enabling shuffle so we can restore it
    private var _originalQueueBeforeShuffle: List<Song>? = null

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat

    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode

    // Expose current playback position as StateFlow for lyrics and progress UI
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    init {
        // Restore persisted playback state (queue, shuffle, repeat, last position)
        try {
            val prefs = com.cvc953.localplayer.preferences.AppPrefs(getApplication())
            val repo = com.cvc953.localplayer.model.SongRepository(getApplication())
            val allSongs = repo.loadSongs()
            val queueUris = prefs.loadPlaybackQueue()
            if (queueUris.isNotEmpty()) {
                val restored = queueUris.mapNotNull { uriStr ->
                    allSongs.find { it.uri.toString() == uriStr }
                }
                if (restored.isNotEmpty()) {
                    _queue.value = restored
                    // set shuffle/repeat flags
                    _isShuffle.value = prefs.loadShuffleEnabled()
                    prefs.loadRepeatMode()?.let { modeStr ->
                        try {
                            _repeatMode.value = com.cvc953.localplayer.ui.RepeatMode.valueOf(modeStr)
                            _isRepeat.value = _repeatMode.value != com.cvc953.localplayer.ui.RepeatMode.NONE
                        } catch (_: Exception) {}
                    }

                    // If there was a last song, attempt to start player controller in paused state
                    val lastUri = prefs.loadLastSongUri()
                    val startIndex = if (lastUri != null) restored.indexOfFirst { it.uri.toString() == lastUri } else -1
                    if (startIndex >= 0) {
                        try {
                            playerController.playNow(restored, startIndex)
                            val pos = prefs.loadPlaybackPosition()
                            if (pos > 0L) playerController.seekTo(pos)
                            // Respect last playing flag
                            val wasPlaying = prefs.loadIsPlaying()
                            if (!wasPlaying) playerController.pause()
                            _currentPosition.value = pos
                        } catch (_: Exception) {}
                    } else {
                        // no last song, keep controller queue in sync without starting playback
                        try { playerController.queueEnd(restored) } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
        }

        // Periodically update current position while playing
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                if (playerController.isPlaying()) {
                    _currentPosition.value = playerController.getCurrentPosition()
                }
                kotlinx.coroutines.delay(200L)
            }
        }
    }

    fun play(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                // Si la canción no es la única en la cola, sincroniza la cola con la lista actual
                val queue = _queue.value
                val index = queue.indexOfFirst { it.id == song.id }
                if (queue.size > 1 && index >= 0) {
                    playerController.playNow(queue, index)
                } else {
                    playerController.playSong(song)
                }
                _currentPosition.value = 0L
                // Persistir estado de reproducción
                try {
                    prefs.saveLastSongUri(song.uri.toString())
                    prefs.savePlaybackPosition(0L)
                    prefs.saveIsPlaying(true)
                    prefs.savePlaybackQueue(_queue.value.map { it.uri.toString() })
                } catch (_: Exception) {}
                try {
                    val intent = Intent(getApplication<Application>(), MusicService::class.java).apply {
                        putExtra("SONG_URI", song.uri.toString())
                        putExtra("TITLE", song.title)
                        putExtra("ARTIST", song.artist)
                        putExtra("IS_PLAYING", true)
                    }
                    ContextCompat.startForegroundService(getApplication(), intent)
                } catch (_: Exception) {
                }
            } catch (e: Exception) {
                _error.value = "Error al reproducir: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun pause() {
        playerController.pause()
        _currentPosition.value = playerController.getCurrentPosition()
        try {
            prefs.savePlaybackPosition(_currentPosition.value)
            prefs.saveIsPlaying(false)
        } catch (_: Exception) {}
        // Update service
        try {
            val intent = Intent(getApplication<Application>(), MusicService::class.java).apply {
                action = MusicService.ACTION_UPDATE_STATE
                putExtra("IS_PLAYING", false)
                putExtra("POSITION", _currentPosition.value)
            }
            ContextCompat.startForegroundService(getApplication(), intent)
        } catch (_: Exception) {
        }
    }

    fun resume() {
        playerController.resume()
        _currentPosition.value = playerController.getCurrentPosition()
        try {
            prefs.savePlaybackPosition(_currentPosition.value)
            prefs.saveIsPlaying(true)
        } catch (_: Exception) {}
        try {
                val song = playerState.value.currentSong
            val intent = Intent(getApplication<Application>(), MusicService::class.java).apply {
                action = MusicService.ACTION_UPDATE_STATE
                putExtra("IS_PLAYING", true)
                putExtra("POSITION", _currentPosition.value)
                if (song != null) {
                    putExtra("SONG_URI", song.uri.toString())
                    putExtra("TITLE", song.title)
                    putExtra("ARTIST", song.artist)
                }
            }
            ContextCompat.startForegroundService(getApplication(), intent)
        } catch (_: Exception) {
        }
    }

    fun stop() {
        playerController.stop()
        _currentPosition.value = 0L
        try {
            prefs.savePlaybackPosition(0L)
            prefs.saveIsPlaying(false)
        } catch (_: Exception) {}
        try {
            val intent = Intent(getApplication<Application>(), MusicService::class.java).apply {
                action = MusicService.ACTION_UPDATE_STATE
                putExtra("IS_PLAYING", false)
                putExtra("POSITION", 0L)
            }
            ContextCompat.startForegroundService(getApplication(), intent)
        } catch (_: Exception) {
        }
    }

    fun seekTo(position: Long) {
        playerController.seekTo(position)
        _currentPosition.value = position
        try { prefs.savePlaybackPosition(position) } catch (_: Exception) {}
    }

    fun updatePosition() {
        val pos = playerController.getCurrentPosition()
        _currentPosition.value = pos
    }

    fun addToQueueNext(song: Song) {
        val currentQueue = _queue.value.toMutableList()
        val currentIndex = playerState.value.currentSong?.let { currentQueue.indexOf(it) } ?: -1
        val insertIndex = if (currentIndex >= 0) currentIndex + 1 else 0
        currentQueue.add(insertIndex, song)
        _queue.value = currentQueue
        try { prefs.savePlaybackQueue(_queue.value.map { it.uri.toString() }); playerController.replaceQueue(_queue.value, keepCurrentSong = true) } catch (_: Exception) {}
    }

    fun addToQueueEnd(song: Song) {
        val currentQueue = _queue.value.toMutableList()
        currentQueue.add(song)
        _queue.value = currentQueue
        try { prefs.savePlaybackQueue(_queue.value.map { it.uri.toString() }); playerController.replaceQueue(_queue.value, keepCurrentSong = true) } catch (_: Exception) {}
    }

    fun removeFromQueue(song: Song) {
        _queue.value = _queue.value.filterNot { it.id == song.id }
        try { prefs.savePlaybackQueue(_queue.value.map { it.uri.toString() }); playerController.replaceQueue(_queue.value, keepCurrentSong = true) } catch (_: Exception) {}
    }

    fun clearQueue() {
        _queue.value = emptyList()
        try { prefs.savePlaybackQueue(emptyList()); playerController.replaceQueue(emptyList(), keepCurrentSong = false) } catch (_: Exception) {}
    }

    fun updateDisplayOrder(songs: List<Song>) {
        _queue.value = songs
        try { prefs.savePlaybackQueue(_queue.value.map { it.uri.toString() }); playerController.replaceQueue(_queue.value, keepCurrentSong = true) } catch (_: Exception) {}
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
        val current = playerState.value.currentSong
        if (_isShuffle.value) {
            // enable shuffle: keep a backup so we can restore later
            if (_originalQueueBeforeShuffle == null) _originalQueueBeforeShuffle = _queue.value.toList()
            val rest = _queue.value.filter { it != current }.shuffled()
            val newQueue = if (current != null && _queue.value.contains(current)) listOf(current) + rest else rest
            _queue.value = newQueue
            try { playerController.replaceQueue(newQueue, keepCurrentSong = true) } catch (_: Exception) {}
        } else {
            // disable shuffle: sort the queue alphabetically by title then artist
            val sorted = _queue.value.sortedWith(compareBy({ it.title.lowercase() }, { it.artist.lowercase() }))
            _queue.value = sorted
            try { playerController.replaceQueue(sorted, keepCurrentSong = true) } catch (_: Exception) {}
            _originalQueueBeforeShuffle = null
        }
        try {
            prefs.saveShuffleEnabled(_isShuffle.value)
            // clear persisted explicit queue when user toggles shuffle to avoid restoring a stale order
            prefs.savePlaybackQueue(emptyList())
        } catch (_: Exception) {}
    }

    fun toggleRepeat() {
        // cycle through RepeatMode similar to previous MainViewModel behavior
        _repeatMode.value =
            when (_repeatMode.value) {
                RepeatMode.NONE -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.NONE
            }
        _isRepeat.value = _repeatMode.value != RepeatMode.NONE
        try { prefs.saveRepeatMode(_repeatMode.value.name) } catch (_: Exception) {}
    }

    fun getUpcomingSongs(): List<Song> {
        val current = playerState.value.currentSong ?: return _queue.value
        val all = _queue.value
        if (all.isEmpty()) return emptyList()

        return when {
            _isShuffle.value -> {
                all.filter { it != current }.shuffled()
            }
            _repeatMode.value == RepeatMode.ALL -> {
                val idx = all.indexOf(current)
                if (idx == -1) all else all.drop(idx + 1) + all.take(idx)
            }
            else -> {
                val idx = all.indexOf(current)
                if (idx == -1) emptyList() else all.drop(idx + 1)
            }
        }
    }

    fun playNextSong() {
        val queue = _queue.value
        val currentSong = playerState.value.currentSong
        if (queue.isEmpty() || playerState.value.currentSong == null) return
        val currentIndex = queue.indexOf(currentSong)
        val nextIndex = if (currentIndex >= 0 && currentIndex + 1 < queue.size) currentIndex + 1 else 0
        val next = queue[nextIndex]
        play(next)
    }

    fun playPreviousSong() {
        val queue = _queue.value
        val currentSong = playerState.value.currentSong
        if (queue.isEmpty() || playerState.value.currentSong == null) return
        val currentIndex = queue.indexOf(currentSong)
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else queue.size - 1
        val prev = queue[prevIndex]
        play(prev)
    }

    fun togglePlayPause() {
        if (playerState.value.isPlaying) pause() else resume()
    }
}
