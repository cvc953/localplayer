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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SongRepository(application)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState

    private var mediaPlayer: MediaPlayer? = null

    init {
        viewModelScope.launch {
            _songs.value = repository.loadSongs()
        }
    }

    fun onSongClicked(song: Song) {
        val current = _playerState.value.currentSong

        if (current?.id == song.id) {
            togglePlayPause()
        } else {
            playSong(song)
        }
    }

    private fun playSong(song: Song) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(getApplication(), song.uri)
            prepare()
            start()
        }

        _playerState.value = PlayerState(
            currentSong = song,
            isPlaying = true
        )
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _playerState.value = _playerState.value.copy(isPlaying = false)
            } else {
                it.start()
                _playerState.value = _playerState.value.copy(isPlaying = true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}


/*class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SongRepository(application)
    private val playerController = PlayerController(application)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _songs.value = repository.loadSongs()
        }
    }

    fun onSongClicked(song: Song) {
        playerController.toggle(song) { currentSong, isPlaying ->
            _playerState.value = PlayerUiState(currentSong, isPlaying)
        }
    }

    override fun onCleared() {
        playerController.release()
        super.onCleared()
    }
}*/
