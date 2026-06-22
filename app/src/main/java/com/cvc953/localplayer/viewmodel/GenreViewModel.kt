package com.cvc953.localplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.GenreController
import com.cvc953.localplayer.model.Genre
import com.cvc953.localplayer.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GenreViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val controller = GenreController(getApplication())
    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres: StateFlow<List<Genre>> = _genres
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    init {
        loadGenres()
    }

    fun loadGenres() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                _genres.value = controller.getAllGenres()
            } catch (e: Exception) {
                _genres.value = emptyList()
                _error.value = "Error cargando géneros: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchGenres(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                _genres.value = controller.searchGenres(query)
            } catch (e: Exception) {
                _genres.value = emptyList()
                _error.value = "Error buscando géneros: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSongsForGenre(genre: Genre) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _songs.value = controller.getSongsForGenre(genre)
            } catch (e: Exception) {
                _songs.value = emptyList()
            }
        }
    }
}
