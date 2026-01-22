package com.cvc953.localplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.player.PlayerController
import com.cvc953.localplayer.ui.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.cvc953.localplayer.model.SongRepository
import android.media.MediaPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlin.text.get


class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SongRepository(application)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState

    private var mediaPlayer: MediaPlayer? = null

    private val _isPlayerScreenVisible = MutableStateFlow(false)
    val isPlayerScreenVisible: StateFlow<Boolean> = _isPlayerScreenVisible

    private var progressJob: Job? = null



    init {
        viewModelScope.launch(Dispatchers.IO) {
            _songs.value = repository.loadSongs()
        }
    }

    /** FUNCIÓN ÚNICA PARA REPRODUCIR */
    /*fun playSong(song: Song) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(getApplication(), song.uri)
            prepare()
            start()
            //startProgressTracking()

            setOnCompletionListener {
                playNextSong()
            }
            _playerState.value = PlayerState(
                currentSong = song,
                isPlaying = true,
                duration = mediaPlayer?.duration?.toLong() ?: 0L
            )
            startPositionUpdates()
        }

        _playerState.value = PlayerState(
            currentSong = song,
            isPlaying = true
        )
    }*/

    fun playSong(song: Song) {
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(getApplication(), song.uri)
            prepare()
            start()

            setOnCompletionListener {
                playNextSong()
            }
        }

        _playerState.value = PlayerState(
            currentSong = song,
            isPlaying = true,
            position = 0L,
            duration = mediaPlayer?.duration?.toLong() ?: 0L
        )

        startPositionUpdates()
    }


    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                progressJob?.cancel()
                _playerState.value = _playerState.value.copy(isPlaying = false)
            } else {
                it.start()
                //startProgressTracking()
                startPositionUpdates()
                _playerState.value = _playerState.value.copy(isPlaying = true)
            }
        }
    }

    fun playNextSong() {
        val list = songs.value
        val currentSong = playerState.value.currentSong ?: return
        if (list.isEmpty()) return

        val currentIndex = list.indexOf(currentSong)
        val nextIndex = (currentIndex + 1) % list.size
        val nextSong = list[nextIndex]

        playSong(nextSong)
    }

    fun playPreviousSong() {
        val list = songs.value
        val currentSong = playerState.value.currentSong ?: return
        if (list.isEmpty()) return

        val currentIndex = list.indexOf(currentSong)
        val nextIndex = (currentIndex - 1) % list.size
        val previousSong = list[nextIndex]

        playSong(previousSong)
    }

    override fun onCleared() {
        progressJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onCleared()
    }

    fun openPlayerScreen() {
        _isPlayerScreenVisible.value = true
    }

    fun closePlayerScreen() {
        _isPlayerScreenVisible.value = false
    }


    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _playerState.update {
            it.copy(position = position)
        }
    }


    private var positionJob: Job? = null

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (true) {
                val player = mediaPlayer ?: break
                _playerState.update {
                    it.copy(
                        position = player.currentPosition.toLong(),
                        duration = player.duration.toLong()
                    )
                }
                delay(500)
            }
        }
    }


}
