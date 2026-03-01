
package com.cvc953.localplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.SongController
import com.cvc953.localplayer.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SongViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val controller = SongController(getApplication())
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs
    private val _selectedSong = MutableStateFlow<Song?>(null)
    val selectedSong: StateFlow<Song?> = _selectedSong
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    init {
        // Load songs when the ViewModel is created so UI shows available music
        loadSongs()
    }


    fun manualRefreshLibrary() {
        loadSongs(forceRescan = true, showScanning = true)
    }

    fun loadSongs(forceRescan: Boolean = false, showScanning: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (showScanning) _isScanning.value = true
            _isLoading.value = true
            _error.value = null
            try {
                val result = if (forceRescan) controller.forceRescan() else controller.getAllSongs()
                android.util.Log.d("SongViewModel", "loadSongs: Loaded ${result.size} songs, forceRescan=$forceRescan")
                _songs.value = result
            } catch (e: Exception) {
                android.util.Log.e("SongViewModel", "loadSongs: Error", e)
                _songs.value = emptyList()
                _error.value = "Error cargando canciones: ${e.message}"
            } finally {
                _isLoading.value = false
                if (showScanning) _isScanning.value = false
            }
        }
    }

    fun searchSongs(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                _songs.value = controller.searchSongs(query)
            } catch (e: Exception) {
                _songs.value = emptyList()
                _error.value = "Error buscando canciones: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectSong(song: Song) {
        _selectedSong.value = song
    }

    fun clearSelection() {
        _selectedSong.value = null
    }
}
