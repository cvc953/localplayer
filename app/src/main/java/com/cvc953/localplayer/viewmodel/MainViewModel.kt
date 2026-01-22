package com.cvc953.localplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.PlayerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.cvc953.localplayer.model.SongRepository

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SongRepository(application)

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
        val currentState = _playerState.value
        if (currentState.currentSong == song) {
            _playerState.value = currentState.copy(isPlaying = !currentState.isPlaying)
        } else {
            _playerState.value = PlayerUiState(currentSong = song, isPlaying = true)
        }
    }
}
