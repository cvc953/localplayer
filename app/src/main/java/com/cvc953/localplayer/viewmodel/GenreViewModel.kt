package com.cvc953.localplayer.viewmodel

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.GenreController
import com.cvc953.localplayer.model.Genre
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.preferences.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GenreViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val controller = GenreController(getApplication())
    private val appPrefs = AppPrefs(getApplication())
    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres: StateFlow<List<Genre>> = _genres
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    // Job para debouncing del auto-scan
    private var autoScanJob: Job? = null

    // Observer para detectar cambios en la biblioteca de música
    private val mediaStoreObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                android.util.Log.d("GenreViewModel", "MediaStore onChange detected")

                if (appPrefs.isAutoScanEnabled()) {
                    android.util.Log.d("GenreViewModel", "Auto-scan enabled, scheduling library refresh")
                    scheduleLibraryRefresh()
                } else {
                    android.util.Log.d("GenreViewModel", "Auto-scan disabled, skipping refresh")
                }
            }
        }

    private fun scheduleLibraryRefresh() {
        autoScanJob?.cancel()
        autoScanJob =
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    delay(2000)
                    refreshMusicLibrary()
                } catch (e: Exception) {
                    android.util.Log.e("GenreViewModel", "Error in auto-scan", e)
                }
            }
    }

    private fun refreshMusicLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newGenres = controller.getAllGenres()
                val currentGenres = _genres.value
                if (newGenres != currentGenres) {
                    _genres.value = newGenres
                }
            } catch (e: Exception) {
                android.util.Log.e("GenreViewModel", "Error refreshing library", e)
            }
        }
    }

    init {
        getApplication<Application>()
            .contentResolver
            .registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaStoreObserver,
            )
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

    override fun onCleared() {
        getApplication<Application>().contentResolver.unregisterContentObserver(mediaStoreObserver)
        autoScanJob?.cancel()
        super.onCleared()
    }
}
