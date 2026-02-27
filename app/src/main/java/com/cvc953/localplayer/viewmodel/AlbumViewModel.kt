package com.cvc953.localplayer.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.AlbumController
import com.cvc953.localplayer.model.Album
import com.cvc953.localplayer.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlbumViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val controller = AlbumController(getApplication())
    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums
    private val _selectedAlbum = MutableStateFlow<Album?>(null)
    val selectedAlbum: StateFlow<Album?> = _selectedAlbum
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs
    private val _isScanning = mutableStateOf(false)
    val isScanning: State<Boolean> = _isScanning

    // Persistent grid/list view preference
    private val prefs = application.getSharedPreferences("music_prefs", 0)

    @Suppress("ktlint:standard:property-naming")
    private val PREF_VIEW_AS_GRID = "pref_view_as_grid"

    fun isGridViewPreferred(): Boolean = prefs.getBoolean(PREF_VIEW_AS_GRID, true)

    fun setGridViewPreferred(value: Boolean) {
        prefs.edit().putBoolean(PREF_VIEW_AS_GRID, value).apply()
    }

    init {
        // Load albums when ViewModel is created so UI shows content
        loadAlbums()
    }

    fun loadAlbums() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                _albums.value = controller.getAllAlbums()
            } catch (e: Exception) {
                _albums.value = emptyList()
                _error.value = "Error cargando álbumes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchAlbums(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                _albums.value = controller.searchAlbums(query)
            } catch (e: Exception) {
                _albums.value = emptyList()
                _error.value = "Error buscando álbumes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectAlbum(album: Album) {
        _selectedAlbum.value = album
        loadSongsForAlbum(album)
    }

    private fun loadSongsForAlbum(album: Album) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _songs.value = controller.getSongsForAlbum(album)
            } catch (e: Exception) {
                _songs.value = emptyList()
            }
        }
    }

    fun clearSelection() {
        _selectedAlbum.value = null
        _songs.value = emptyList()
    }
}
