package com.cvc953.localplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.ArtistController
import com.cvc953.localplayer.model.Artist
import com.cvc953.localplayer.model.Song
import kotlinx.coroutines.Dispatchers
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

    // Persistent grid/list view preference
    private val prefs = application.getSharedPreferences("music_prefs", 0)
    private val PREF_VIEW_AS_GRID = "pref_view_as_grid_artist"

    fun isGridViewPreferred(): Boolean = prefs.getBoolean(PREF_VIEW_AS_GRID, true)

    fun setGridViewPreferred(value: Boolean) {
        prefs.edit().putBoolean(PREF_VIEW_AS_GRID, value).apply()
    }

    init {
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
}
