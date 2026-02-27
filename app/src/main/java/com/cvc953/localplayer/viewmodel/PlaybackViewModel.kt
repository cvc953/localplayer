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
    val playerState: StateFlow<PlayerState> = playerController.state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue

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
                playerController.playSong(song)
                _currentPosition.value = 0L
                _currentPosition.value = 0L
                // Notify service about the new song so notification shows correct metadata
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
    }

    fun addToQueueEnd(song: Song) {
        val currentQueue = _queue.value.toMutableList()
        currentQueue.add(song)
        _queue.value = currentQueue
    }

    fun removeFromQueue(song: Song) {
        _queue.value = _queue.value.filterNot { it.id == song.id }
    }

    fun clearQueue() {
        _queue.value = emptyList()
    }

    fun updateDisplayOrder(songs: List<Song>) {
        _queue.value = songs
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
        if (_isShuffle.value) {
            _queue.value = _queue.value.shuffled()
        } else {
            // Optionally restore original order if you keep a backup
        }
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
    }

    fun getUpcomingSongs(): List<Song> {
        val current = playerState.value.currentSong ?: return _queue.value
        val base = _queue.value // use explicit queue as primary source
        val upcoming = mutableListOf<Song>()
        upcoming.addAll(_queue.value.filter { it != current })

        val remaining = when {
            _isShuffle.value -> {
                val excluded = mutableSetOf<Song>()
                excluded.addAll(_queue.value)
                excluded.add(current)
                // simple shuffle of remaining songs (not cached)
                val allSongs = listOf<Song>()
                allSongs.filter { it !in excluded }.shuffled()
            }
            _repeatMode.value == RepeatMode.ALL -> {
                val all = _queue.value
                val idx = all.indexOf(current)
                if (idx == -1) all else all.drop(idx + 1) + all.take(idx)
            }
            else -> {
                val all = _queue.value
                val idx = all.indexOf(current)
                if (idx == -1) emptyList() else all.drop(idx + 1)
            }
        }

        upcoming.addAll(remaining)
        return upcoming
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
