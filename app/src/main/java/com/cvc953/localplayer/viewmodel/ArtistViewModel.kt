package com.cvc953.localplayer.viewmodel

import android.app.Application
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.ArtistController
import com.cvc953.localplayer.model.Artist
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.preferences.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ArtistViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _artistSongs = MutableStateFlow<List<com.cvc953.localplayer.model.Song>>(emptyList())

    /**
     * Returns a StateFlow of the songs for the given artist name.
     * Triggers a background load if the artist changes.
     */
    fun getSongsForArtist(artistName: String): StateFlow<List<com.cvc953.localplayer.model.Song>> {
        viewModelScope.launch(Dispatchers.IO) {
            val allSongs =
                com.cvc953.localplayer.model
                    .SongRepository(getApplication())
                    .loadSongs()
            _artistSongs.value = allSongs.filter { it.artist.equals(artistName, ignoreCase = true) }
        }
        return _artistSongs
    }

    private val controller = ArtistController(getApplication())
    private val appPrefs = AppPrefs(getApplication())
    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists
    private val _selectedArtist = MutableStateFlow<Artist?>(null)
    val selectedArtist: StateFlow<Artist?> = _selectedArtist
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
                android.util.Log.d("ArtistViewModel", "MediaStore onChange detected")

                // Detectar cambios en la biblioteca y refrescar (si está activado)
                if (appPrefs.isAutoScanEnabled()) {
                    android.util.Log.d("ArtistViewModel", "Auto-scan enabled, scheduling library refresh")
                    scheduleLibraryRefresh()
                } else {
                    android.util.Log.d("ArtistViewModel", "Auto-scan disabled, skipping refresh")
                }
            }
        }

    private fun scheduleLibraryRefresh() {
        // Cancelar el job anterior si existe (debouncing)
        autoScanJob?.cancel()

        // Programar un nuevo escaneo con delay de 2 segundos
        autoScanJob =
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    android.util.Log.d("ArtistViewModel", "Debouncing auto-scan for 2 seconds...")
                    delay(2000) // Esperar 2 segundos para agrupar múltiples cambios
                    android.util.Log.d("ArtistViewModel", "Starting auto-scan library refresh")
                    refreshMusicLibrary()
                } catch (e: Exception) {
                    android.util.Log.e("ArtistViewModel", "Error in auto-scan", e)
                }
            }
    }

    private fun refreshMusicLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("ArtistViewModel", "refreshMusicLibrary: Starting scan")
                val newArtists = controller.getAllArtists()
                val currentArtists = _artists.value

                android.util.Log.d(
                    "ArtistViewModel",
                    "refreshMusicLibrary: Found ${newArtists.size} artists, current has ${currentArtists.size}",
                )

                // Actualizar si hay cambios
                if (newArtists != currentArtists) {
                    android.util.Log.d("ArtistViewModel", "refreshMusicLibrary: Changes detected, updating library")
                    _artists.value = newArtists
                } else {
                    android.util.Log.d("ArtistViewModel", "refreshMusicLibrary: No changes detected")
                }
            } catch (e: Exception) {
                android.util.Log.e("ArtistViewModel", "Error refreshing library", e)
            }
        }
    }

    // Persistent grid/list view preference
    private val prefs = application.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)
    private val PREF_VIEW_AS_GRID = "pref_view_as_grid_artist"

    fun isGridViewPreferred(): Boolean = prefs.getBoolean(PREF_VIEW_AS_GRID, true)

    fun setGridViewPreferred(value: Boolean) {
        prefs.edit().putBoolean(PREF_VIEW_AS_GRID, value).apply()
    }

    init {
        // Registrar el observer para detectar cambios en la biblioteca
        getApplication<Application>()
            .contentResolver
            .registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaStoreObserver,
            )

        loadArtists()
    }

    fun loadArtists() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                _artists.value = controller.getAllArtists()
            } catch (e: Exception) {
                _artists.value = emptyList()
                _error.value = "Error cargando artistas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchArtists(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                _artists.value = controller.searchArtists(query)
            } catch (e: Exception) {
                _artists.value = emptyList()
                _error.value = "Error buscando artistas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectArtist(artist: Artist) {
        _selectedArtist.value = artist
    }

    fun clearSelection() {
        _selectedArtist.value = null
    }

    override fun onCleared() {
        // Desregistrar el observer cuando el ViewModel se destruya
        getApplication<Application>().contentResolver.unregisterContentObserver(mediaStoreObserver)
        autoScanJob?.cancel()
        super.onCleared()
    }
}
