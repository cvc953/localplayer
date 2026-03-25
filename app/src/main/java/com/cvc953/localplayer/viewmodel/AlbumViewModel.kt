package com.cvc953.localplayer.viewmodel

import android.app.Application
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.AlbumController
import com.cvc953.localplayer.model.Album
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.preferences.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlbumViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val controller = AlbumController(getApplication())
    private val appPrefs = AppPrefs(getApplication())
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

    // Job para debouncing del auto-scan
    private var autoScanJob: Job? = null

    // Observer para detectar cambios en la biblioteca de música
    private val mediaStoreObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                android.util.Log.d("AlbumViewModel", "MediaStore onChange detected")

                // Detectar cambios en la biblioteca y refrescar (si está activado)
                if (appPrefs.isAutoScanEnabled()) {
                    android.util.Log.d("AlbumViewModel", "Auto-scan enabled, scheduling library refresh")
                    scheduleLibraryRefresh()
                } else {
                    android.util.Log.d("AlbumViewModel", "Auto-scan disabled, skipping refresh")
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
                    android.util.Log.d("AlbumViewModel", "Debouncing auto-scan for 2 seconds...")
                    delay(2000) // Esperar 2 segundos para agrupar múltiples cambios
                    android.util.Log.d("AlbumViewModel", "Starting auto-scan library refresh")
                    refreshMusicLibrary()
                } catch (e: Exception) {
                    android.util.Log.e("AlbumViewModel", "Error in auto-scan", e)
                }
            }
    }

    private fun refreshMusicLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("AlbumViewModel", "refreshMusicLibrary: Starting scan")
                val newAlbums = controller.getAllAlbums()
                val currentAlbums = _albums.value

                android.util.Log.d(
                    "AlbumViewModel",
                    "refreshMusicLibrary: Found ${newAlbums.size} albums, current has ${currentAlbums.size}",
                )

                // Actualizar si hay cambios
                if (newAlbums != currentAlbums) {
                    android.util.Log.d("AlbumViewModel", "refreshMusicLibrary: Changes detected, updating library")
                    _albums.value = newAlbums
                } else {
                    android.util.Log.d("AlbumViewModel", "refreshMusicLibrary: No changes detected")
                }
            } catch (e: Exception) {
                android.util.Log.e("AlbumViewModel", "Error refreshing library", e)
            }
        }
    }

    // Persistent grid/list view preference
    private val prefs = application.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)

    @Suppress("ktlint:standard:property-naming")
    private val PREF_VIEW_AS_GRID = "pref_view_as_grid"

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

    // Nueva función para cargar canciones por nombre de álbum y artista
    // Útil cuando se navega directamente al detalle sin pasar por la lista de álbumes
    fun loadSongsForAlbumByName(
        albumName: String,
        artistName: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Buscar el álbum que coincida con el nombre y el artista
                val normalizedRequestedArtists = normalizeArtistName(artistName).map { it.trim() }
                val matchingAlbum =
                    controller.getAllAlbums().find { album ->
                        val nameMatches = album.name.trim().equals(albumName.trim(), ignoreCase = true)
                        val artistMatches =
                            normalizeArtistName(album.artist).any { artist ->
                                normalizedRequestedArtists.any { requestedArtist ->
                                    artist.trim().equals(requestedArtist, ignoreCase = true)
                                }
                            }
                        nameMatches && artistMatches
                    }

                if (matchingAlbum != null) {
                    _songs.value = controller.getSongsForAlbum(matchingAlbum)
                } else {
                    // Si no encontramos el álbum exacto, intentamos cargar todas las canciones y filtrar localmente
                    // Esto es un fallback, pero debería funcionar si la lógica de getAlbumByName falla
                    // Por ahora, dejamos vacío o intentamos una búsqueda más amplia
                    _songs.value = emptyList()
                }
            } catch (e: Exception) {
                _songs.value = emptyList()
            }
        }
    }

    // Helper para normalizar artistas (copiado de AlbumController para evitar dependencia cíclica si fuera necesario, aunque aquí es interno)
    private fun normalizeArtistName(artist: String): List<String> =
        if (artist.trim().equals("AC/DC", ignoreCase = true)) {
            listOf("AC/DC")
        } else {
            artist
                .trim()
                .split(',', '/')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }

    override fun onCleared() {
        // Desregistrar el observer cuando el ViewModel se destruya
        getApplication<Application>().contentResolver.unregisterContentObserver(mediaStoreObserver)
        autoScanJob?.cancel()
        super.onCleared()
    }
}
