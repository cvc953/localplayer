
package com.cvc953.localplayer.viewmodel

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.SongController
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.preferences.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SongViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val controller = SongController(getApplication())
    private val appPrefs = AppPrefs(getApplication())
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

    // Job para debouncing del auto-scan
    private var autoScanJob: Job? = null

    // Observer para detectar cambios en la biblioteca de música
    private val mediaStoreObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                android.util.Log.d("SongViewModel", "MediaStore onChange detected")
                
                // Detectar cambios en la biblioteca y refrescar (si está activado)
                if (appPrefs.isAutoScanEnabled()) {
                    android.util.Log.d("SongViewModel", "Auto-scan enabled, scheduling library refresh")
                    scheduleLibraryRefresh()
                } else {
                    android.util.Log.d("SongViewModel", "Auto-scan disabled, skipping refresh")
                }
            }
        }

    private fun scheduleLibraryRefresh() {
        // Cancelar el job anterior si existe (debouncing)
        autoScanJob?.cancel()
        
        // Programar un nuevo escaneo con delay de 2 segundos
        autoScanJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("SongViewModel", "Debouncing auto-scan for 2 seconds...")
                delay(2000) // Esperar 2 segundos para agrupar múltiples cambios
                android.util.Log.d("SongViewModel", "Starting auto-scan library refresh")
                refreshMusicLibrary()
            } catch (e: Exception) {
                android.util.Log.e("SongViewModel", "Error in auto-scan", e)
            }
        }
    }

    private fun refreshMusicLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("SongViewModel", "refreshMusicLibrary: Starting scan")
                val newSongs = controller.forceRescan()
                val currentSongs = _songs.value

                android.util.Log.d("SongViewModel", "refreshMusicLibrary: Found ${newSongs.size} songs, current has ${currentSongs.size}")

                // Actualizar si hay cambios en el número de canciones o en los IDs
                val currentIds = currentSongs.map { it.id }.toSet()
                val newIds = newSongs.map { it.id }.toSet()
                val hasChanges = currentIds != newIds

                if (hasChanges) {
                    android.util.Log.d("SongViewModel", "refreshMusicLibrary: Changes detected, updating library")
                    _songs.value = newSongs
                } else {
                    android.util.Log.d("SongViewModel", "refreshMusicLibrary: No changes detected")
                }
            } catch (e: Exception) {
                android.util.Log.e("SongViewModel", "Error refreshing library", e)
            }
        }
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

    override fun onCleared() {
        // Desregistrar el observer cuando el ViewModel se destruya
        getApplication<Application>().contentResolver.unregisterContentObserver(mediaStoreObserver)
        autoScanJob?.cancel()
        super.onCleared()
    }
}
