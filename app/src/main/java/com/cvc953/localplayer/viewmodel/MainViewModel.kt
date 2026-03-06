
package com.cvc953.localplayer.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.ContentObserver
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.PlayerController
import com.cvc953.localplayer.model.Playlist
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.model.SongRepository
import com.cvc953.localplayer.model.TtmlLyrics
import com.cvc953.localplayer.preferences.AppPrefs
import com.cvc953.localplayer.services.MusicService
import com.cvc953.localplayer.ui.PlayerState
import com.cvc953.localplayer.ui.RepeatMode
import com.cvc953.localplayer.util.LrcLine
import com.cvc953.localplayer.util.TtmlParser
import com.cvc953.localplayer.util.parseLrc
import com.cvc953.localplayer.viewmodel.AlbumViewModel
import com.cvc953.localplayer.viewmodel.ArtistViewModel
import com.cvc953.localplayer.viewmodel.LyricsViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.SongViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.collections.get
import kotlin.collections.set

/*class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
import com.cvc953.localplayer.model.Playlist
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.model.SongRepository
import com.cvc953.localplayer.model.TtmlLyrics
import com.cvc953.localplayer.preferences.AppPrefs
import com.cvc953.localplayer.services.MusicService
import com.cvc953.localplayer.ui.PlayerState
import com.cvc953.localplayer.ui.RepeatMode
import com.cvc953.localplayer.util.LrcLine
import com.cvc953.localplayer.util.TtmlParser
import com.cvc953.localplayer.util.parseLrc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class LyricLine(
    val timeMs: Long,
    val text: String,
)*/

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {
    companion object {
        var instance: MainViewModel? = null
        private const val PREFS_NAME = "music_prefs"
        private const val LAST_SONG_URI = "last_song_uri"
        private const val LAST_SONG_TITLE = "last_song_title"
        private const val LAST_SONG_ARTIST = "last_song_artist"
        private const val LAST_IS_PLAYING = "last_is_playing"
        private const val PLAYLISTS_JSON = "playlists_json"
        private const val PREF_VIEW_AS_GRID = "pref_view_as_grid"
    }

    init {
        instance = this
    }

    private val appPrefs = AppPrefs(application)

    private val _isSettingsVisible = MutableStateFlow(false)
    val isSettingsVisible: StateFlow<Boolean> = _isSettingsVisible
    private val _isAboutVisible = MutableStateFlow(false)
    val isAboutVisible: StateFlow<Boolean> = _isAboutVisible

    private val _folderUris = MutableStateFlow(appPrefs.getMusicFolderUris())
    val folderUris: StateFlow<List<String>> = _folderUris

    fun openSettingsScreen() {
        _isSettingsVisible.value = true
    }

    fun closeSettingsScreen() {
        _isSettingsVisible.value = false
    }

    fun openAboutScreen() {
        _isAboutVisible.value = true
    }

    fun closeAboutScreen() {
        _isAboutVisible.value = false
    }

    fun addMusicFolder(uri: String) {
        appPrefs.addMusicFolder(uri)
        _folderUris.value = appPrefs.getMusicFolderUris()
        manualRefreshLibrary()
        refreshFolderEntries()
    }

    fun removeMusicFolder(uri: String) {
        appPrefs.removeMusicFolder(uri)
        _folderUris.value = appPrefs.getMusicFolderUris()
        manualRefreshLibrary()
        refreshFolderEntries()
    }

    data class FolderEntry(
        val uri: String,
        val name: String,
        val count: Int,
    )

    private val _folderEntries = MutableStateFlow<List<FolderEntry>>(emptyList())
    val folderEntries: StateFlow<List<FolderEntry>> = _folderEntries

    private fun refreshFolderEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<FolderEntry>()
            val uris = appPrefs.getMusicFolderUris()
            uris.forEach { u ->
                var name = u
                try {
                    val treeId = DocumentsContract.getTreeDocumentId(Uri.parse(u))
                    // treeId often like "primary:Music/monochrome" -> show last path segment
                    val rel = treeId.removePrefix("primary:").trimStart('/')
                    if (rel.isNotEmpty()) {
                        val last = rel.substringAfterLast('/')
                        if (last.isNotEmpty()) name = last
                    }
                } catch (_: Exception) {
                    // fallback to last path segment of URI
                    try {
                        val p = Uri.parse(u).lastPathSegment
                        if (!p.isNullOrEmpty()) name = p
                    } catch (_: Exception) {
                    }
                }
                val count =
                    try {
                        repository.countSongsForFolder(u)
                    } catch (_: Exception) {
                        0
                    }
                list.add(FolderEntry(uri = u, name = name, count = count))
            }
            _folderEntries.value = list
        }
    }

    // ensure entries are refreshed when folders change
    init {
        // existing init stuff will run; refresh folder entries now
        refreshFolderEntries()
    }

    // onCleared is implemented later in the file; no-op here.

    private val repository = SongRepository(application)
    private val prefs: SharedPreferences =
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Lista de canciones
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    // ViewModels especializados
    val lyricsViewModel = LyricsViewModel(application)
    val songViewModel = SongViewModel(application)
    val artistViewModel = ArtistViewModel(application)
    val albumViewModel = AlbumViewModel(application)
    val playbackViewModel = PlaybackViewModel(application)

    // Aquí puedes exponer solo el estado global mínimo necesario y delegar toda la lógica a los ViewModels anteriores.

    // Orden de visualización actual (usado para next/previous)
    private val _displayOrder = MutableStateFlow<List<Song>>(emptyList())
    val displayOrder: StateFlow<List<Song>> = _displayOrder

    // Estado del reproductor
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState

    init {
        // Keep MainViewModel's player state in sync with the centralized PlaybackViewModel
        viewModelScope.launch {
            playbackViewModel.playerState.collect { state ->
                _playerState.value = state
            }
        }
        // Ensure when the PlayerController's internal queue is exhausted, MainViewModel advances
        try {
            val pc = PlayerController.getInstance(getApplication(), viewModelScope)
            
            pc.setOnQueueEndedListener {
                viewModelScope.launch {
                    try {
                        playNextSong()
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private val _autoScanEnabled = MutableStateFlow(appPrefs.isAutoScanEnabled())
    val autoScanEnabled: StateFlow<Boolean> = _autoScanEnabled

    private val _themeMode = MutableStateFlow(appPrefs.getThemeMode())
    val themeMode: StateFlow<String> = _themeMode

    private val _isPlayerScreenVisible = MutableStateFlow(false)
    val isPlayerScreenVisible: StateFlow<Boolean> = _isPlayerScreenVisible

    // Modos
    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle

    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode

    // Letras
    private val _showLyrics = MutableStateFlow(false)
    val showLyrics = _showLyrics.asStateFlow()
    private val _lyrics = MutableStateFlow<List<LrcLine>>(emptyList())
    val lyrics: StateFlow<List<LrcLine>> = _lyrics
    private val _ttmlLyrics = MutableStateFlow<TtmlLyrics?>(null)
    val ttmlLyrics: StateFlow<TtmlLyrics?> = _ttmlLyrics

    private val _isScanning = mutableStateOf(false)
    val isScanning: State<Boolean> = _isScanning

    private val _scanProgress = mutableStateOf(0f)
    val scanProgress: State<Float> = _scanProgress

    // Cola de reproducción
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue

    // Cache para el orden aleatorio persistente mientras shuffle esté activo
    private var cachedShuffledRemaining: List<Song>? = null

    // Historial de reproducción para soportar "Anterior" respetando el orden
    private val playHistory: MutableList<Song> = mutableListOf()

    // Job para debouncing del auto-scan
    private var autoScanJob: Job? = null

    // Observer para detectar cambios en la biblioteca de música
    private val mediaStoreObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                android.util.Log.d("MainViewModel", "MediaStore onChange detected, selfChange=$selfChange")
                
                // Detectar cambios en la biblioteca y refrescar (si está activado)
                if (appPrefs.isAutoScanEnabled()) {
                    android.util.Log.d("MainViewModel", "Auto-scan enabled, scheduling library refresh")
                    scheduleLibraryRefresh()
                } else {
                    android.util.Log.d("MainViewModel", "Auto-scan disabled, skipping refresh")
                }
            }
        }

    private fun scheduleLibraryRefresh() {
        // Cancelar el job anterior si existe (debouncing)
        autoScanJob?.cancel()
        
        // Programar un nuevo escaneo con delay de 2 segundos
        autoScanJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("MainViewModel", "Debouncing auto-scan for 2 seconds...")
                delay(2000) // Esperar 2 segundos para agrupar múltiples cambios
                android.util.Log.d("MainViewModel", "Starting auto-scan library refresh")
                refreshMusicLibrary()
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error in auto-scan", e)
            }
        }
    }

    private fun refreshMusicLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("MainViewModel", "refreshMusicLibrary: Starting scan")
                // When auto-scan is enabled we must force a full rescan to detect newly added files
                val newSongs = if (appPrefs.isAutoScanEnabled()) repository.forceRescanSongs() else repository.loadSongs()
                val currentSongs = _songs.value

                android.util.Log.d("MainViewModel", "refreshMusicLibrary: Found ${newSongs.size} songs, current has ${currentSongs.size}")

                // Actualizar si hay cambios en el número de canciones o en los IDs
                val currentIds = currentSongs.map { it.id }.toSet()
                val newIds = newSongs.map { it.id }.toSet()
                val hasChanges = currentIds != newIds

                if (hasChanges) {
                    android.util.Log.d("MainViewModel", "refreshMusicLibrary: Changes detected, updating library")
                    _songs.value = newSongs.sortedBy { it.title }
                } else {
                    android.util.Log.d("MainViewModel", "refreshMusicLibrary: No changes detected")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error refreshing library", e)
            }
        }
    }

    fun manualRefreshLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isScanning.value = true
                android.util.Log.d("MainViewModel", "Iniciando re-escaneo manual...")

                // Forzar re-escaneo completo ignorando el caché
                val newSongs = repository.forceRescanSongs()

                android.util.Log.d(
                    "MainViewModel",
                    "Re-escaneo completo: ${newSongs.size} canciones",
                )

                // Actualizar la lista con las nuevas canciones
                withContext(Dispatchers.Main) {
                    _songs.value = newSongs.sortedBy { it.title }
                    android.util.Log.d(
                        "MainViewModel",
                        "Lista actualizada en UI: ${_songs.value.size} canciones",
                    )
                }

                _isScanning.value = false
                android.util.Log.d("MainViewModel", "Re-escaneo completado exitosamente")
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error en re-escaneo manual", e)
                _isScanning.value = false
            }
        }
    }

    fun addToQueueNext(song: Song) {
        val list = _queue.value.toMutableList()
        list.add(0, song)
        _queue.value = list
        // si hay caché de shuffle, quitar la canción añadida
        cachedShuffledRemaining = cachedShuffledRemaining?.filter { it != song }
    }

    fun addToQueueEnd(song: Song) {
        val list = _queue.value.toMutableList()
        list.add(song)
        _queue.value = list
        cachedShuffledRemaining = cachedShuffledRemaining?.filter { it != song }
    }

    fun moveQueueItem(
        fromIndex: Int,
        toIndex: Int,
    ) {
        val list = _queue.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _queue.value = list
        // reordenar la caché no es necesario; el cache contiene sólo el resto
    }

    fun setUpcomingOrder(newOrder: List<Song>) {
        // Guardamos todo el orden de próximas canciones para que el drag funcione
        // tanto con cola manual como con el resto de la biblioteca.
        _queue.value = newOrder
        // eliminar de la caché cualquier canción que ahora esté en la cola explícita
        cachedShuffledRemaining = cachedShuffledRemaining?.filter { it !in _queue.value }
    }

    fun getUpcomingSongs(): List<Song> {
        val current = playerState.value.currentSong ?: return _queue.value
        // Usar displayOrder si está disponible, sino songs
        val base = if (_displayOrder.value.isNotEmpty()) _displayOrder.value else songs.value
        val upcoming = mutableListOf<Song>()

        // Primero la cola explícita
        upcoming.addAll(_queue.value)

        val remaining =
            when {
                _isShuffle.value -> {
                    // Usar caché si existe, si no generarla en este momento
                    if (cachedShuffledRemaining == null) {
                        val excluded = mutableSetOf<Song>()
                        excluded.addAll(_queue.value)
                        excluded.add(current)
                        cachedShuffledRemaining = base.filter { it !in excluded }.shuffled()
                    }
                    // Asegurarnos de quitar cualquier canción que ahora esté en la cola
                    cachedShuffledRemaining =
                        cachedShuffledRemaining?.filter { it !in _queue.value }
                    cachedShuffledRemaining ?: emptyList()
                }

                _repeatMode.value == RepeatMode.ALL -> {
                    val idx = base.indexOf(current)
                    if (idx == -1) base else base.drop(idx + 1) + base.take(idx)
                }

                else -> {
                    val idx = base.indexOf(current)
                    if (idx == -1) emptyList() else base.drop(idx + 1)
                }
            }

        upcoming.addAll(remaining)
        return upcoming
    }

    private fun popQueue(): Song? {
        val current = _queue.value
        if (current.isEmpty()) return null
        val list = current.toMutableList()
        val next = list.removeAt(0)
        _queue.value = list
        return next
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
        // Only perform initial scan if user previously selected a music folder
        val appPrefs = AppPrefs(getApplication())
        if (appPrefs.hasMusicFolderUri()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    // Solo mostrar indicador si es la primera vez y el usuario tiene activado el escaneo automático
                    val firstScan = !repository.isFirstScanDone()
                    if (firstScan && appPrefs.isAutoScanEnabled()) {
                        _isScanning.value = true
                        val temp = mutableListOf<Song>()
                        // Escaneo incremental: actualizamos _songs a medida que se encuentran canciones
                        repository.scanSongsStreaming(
                            onSongFound = { song ->
                                temp.add(song)
                                _songs.value = temp.sortedBy { it.title }
                            },
                            onProgress = { current, total ->
                                _scanProgress.value =
                                    if (total > 0) current.toFloat() / total else 0f
                            },
                        )
                        _isScanning.value = false
                    } else {
                        val loaded =
                            if (appPrefs.isAutoScanEnabled()) {
                                withContext(Dispatchers.IO) {
                                    repository.forceRescanSongs()
                                }
                            } else {
                                withContext(Dispatchers.IO) { repository.loadSongs() }
                            }
                        _songs.value = loaded.sortedBy { it.title }
                    }

                    // Cargar última canción reproducida
                    loadLastSong()
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "Error inicializando ViewModel", e)
                    _isScanning.value = false
                }
            }
        } else {

            _isScanning.value = false
        }
    }

    fun playSong(
        song: Song,
        autoPlay: Boolean = true,
    ) {
        // Delegate playback to PlaybackViewModel to avoid duplicate MediaPlayer instances
        try {
            playbackViewModel.play(song)
            // Prefetch lyrics using centralized LyricsViewModel which supports caching
            try {
                // Prefetch TTML parsing in background and cache it; UI will load from cache
                lyricsViewModel.prefetchTtml(song)
            } catch (_: Exception) {
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error delegating playSong", e)
            playNextSong()
        }
    }

    fun togglePlayPause() {
        // Delegate to playbackViewModel
        try {
            playbackViewModel.togglePlayPause()
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "togglePlayPause delegate failed", e)
        }
    }

    fun playNextSong() {
        // Usar displayOrder si está disponible, sino songs
        val list = if (_displayOrder.value.isNotEmpty()) _displayOrder.value else songs.value
        val currentSong = playerState.value.currentSong ?: return
        if (list.isEmpty()) return

        // Priorizar canciones en cola
        popQueue()?.let { queued ->
            // push current to history
            playHistory.add(currentSong)
            playSong(queued)
            startService(getApplication(), queued)
            return
        }

        val nextSong =
            when {
                _isShuffle.value -> {
                    // Respetar el orden aleatorio persistente generado al activar shuffle
                    if (cachedShuffledRemaining == null) {
                        val excluded = mutableSetOf<Song>()
                        excluded.addAll(_queue.value)
                        excluded.add(currentSong)
                        cachedShuffledRemaining = list.filter { it !in excluded }.shuffled()
                    }
                    val next = cachedShuffledRemaining?.firstOrNull()
                    if (next != null) {
                        // consumir el primero de la caché
                        cachedShuffledRemaining = cachedShuffledRemaining?.drop(1)
                        next
                    } else {
                        // fallback: elige cualquiera distinto
                        if (list.size == 1) {
                            currentSong
                        } else {
                            var randomSong: Song
                            do {
                                randomSong = list.random()
                            } while (randomSong == currentSong)
                            randomSong
                        }
                    }
                }

                _repeatMode.value == RepeatMode.ONE -> {
                    // Repetir la misma canción
                    currentSong
                }

                else -> {
                    // Avanza normalmente
                    val currentIndex = list.indexOf(currentSong)
                    val nextIndex = currentIndex + 1
                    if (nextIndex < list.size) {
                        list[nextIndex]
                    } else if (_repeatMode.value == RepeatMode.ALL) {
                        list[0]
                    } else {
                        return // NO hay siguiente canción si RepeatMode.NONE y estamos al
                    }
                    // final
                }
            }

        // push current to history and delegate playback
        playHistory.add(currentSong)
        try {
            playbackViewModel.play(nextSong)
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "playNextSong delegate failed", e)
        }
    }

    fun playPreviousSong() {
        // Usar displayOrder si está disponible, sino songs
        val list = if (_displayOrder.value.isNotEmpty()) _displayOrder.value else songs.value
        val currentSong = playerState.value.currentSong ?: return
        if (list.isEmpty()) return
        // Si hay historial, regresar a la última canción reproducida
        if (playHistory.isNotEmpty()) {
            val prev = playHistory.removeAt(playHistory.lastIndex)
            playSong(prev)
            startService(getApplication(), prev)
            return
        }

        val previousSong =
            when {
                _isShuffle.value -> {
                    // Si no hay historial, intentar tomar la última consumida de la caché
                    // (no es trivial recuperar la "anterior" en shuffle sin historial)
                    if (list.size == 1) {
                        currentSong
                    } else {
                        var randomSong: Song
                        do {
                            randomSong = list.random()
                        } while (randomSong == currentSong)
                        randomSong
                    }
                }

                _repeatMode.value == RepeatMode.ONE -> {
                    currentSong
                }

                else -> {
                    val currentIndex = list.indexOf(currentSong)
                    val prevIndex = currentIndex - 1
                    if (prevIndex >= 0) {
                        list[prevIndex]
                    } else if (_repeatMode.value == RepeatMode.ALL) {
                        list.last()
                    } else {
                        return
                    }
                }
            }

        try {
            playbackViewModel.play(previousSong)
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "playPreviousSong delegate failed", e)
        }
    }

    override fun onCleared() {
        // Cancelar cualquier auto-scan pendiente
        autoScanJob?.cancel()
        // Deregistrar el observer cuando el ViewModel se destruye
        getApplication<Application>().contentResolver.unregisterContentObserver(mediaStoreObserver)
        super.onCleared()
    }

    fun toggleAutoScan(enabled: Boolean) {
        android.util.Log.d("MainViewModel", "toggleAutoScan: $enabled")
        appPrefs.setAutoScanEnabled(enabled)
        _autoScanEnabled.value = enabled
        
        if (enabled) {
            // If enabling auto-scan, trigger an immediate refresh so new files are picked up
            android.util.Log.d("MainViewModel", "Auto-scan enabled, triggering immediate refresh")
            try {
                refreshMusicLibrary()
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error refreshing after enabling auto-scan", e)
            }
        } else {
            // If disabling, cancel any pending scan
            android.util.Log.d("MainViewModel", "Auto-scan disabled, cancelling pending scans")
            autoScanJob?.cancel()
        }
    }

    fun setThemeMode(mode: String) {
        appPrefs.setThemeMode(mode)
        _themeMode.value = mode
    }

    fun openPlayerScreen() {
        _isPlayerScreenVisible.value = true
    }

    fun closePlayerScreen() {
        _isPlayerScreenVisible.value = false
    }

    fun seekTo(position: Long) {
        playbackViewModel.seekTo(position)
    }

    // Cambiar aleatorio
    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
        if (_isShuffle.value) {
            // Generar y mantener un orden aleatorio en el momento de activar shuffle
            val current = _playerState.value.currentSong
            // Usar displayOrder si está disponible, sino songs
            val base = if (_displayOrder.value.isNotEmpty()) _displayOrder.value else songs.value
            val excluded = mutableSetOf<Song>()
            current?.let { excluded.add(it) }
            excluded.addAll(_queue.value)
            cachedShuffledRemaining = base.filter { it !in excluded }.shuffled()
        } else {
            // Al desactivar shuffle limpiamos la caché
            cachedShuffledRemaining = null
        }
    }

    // Cambiar repetición
    fun toggleRepeat() {
        _repeatMode.value =
            when (_repeatMode.value) {
                RepeatMode.NONE -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.NONE
            }
    }

    fun toggleLyrics() {
        _showLyrics.value = !_showLyrics.value
    }

    fun updateDisplayOrder(orderedSongs: List<Song>) {
        _displayOrder.value = orderedSongs
    }

    fun loadLyricsForSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("LyricsDebug", "Cargando lyrics para: ${song.title}")

                // Obtener la ruta del archivo (desde song.filePath o desde ContentResolver)
                var audioFilePath = song.filePath

                if (audioFilePath.isNullOrEmpty()) {
                    android.util.Log.d("LyricsDebug", "FilePath vacío, consultando ContentResolver")
                    // Si no tenemos filePath, obtenerlo del ContentResolver
                    val resolver = getApplication<Application>().contentResolver
                    val projection = arrayOf(MediaStore.Audio.Media.DATA)
                    resolver.query(song.uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                            if (dataCol >= 0) {
                                audioFilePath = cursor.getString(dataCol)
                                android.util.Log.d(
                                    "LyricsDebug",
                                    "FilePath obtenido: $audioFilePath",
                                )
                            }
                        }
                    }
                }

                android.util.Log.d("LyricsDebug", "FilePath final: $audioFilePath")

                // Si tenemos la ruta del archivo, buscar .lrc en el mismo directorio
                if (!audioFilePath.isNullOrEmpty()) {
                    val audioFile = File(audioFilePath)
                    val audioDir = audioFile.parentFile
                    val audioNameWithoutExt = audioFile.nameWithoutExtension

                    android.util.Log.d("LyricsDebug", "Archivo: ${audioFile.absolutePath}")
                    android.util.Log.d("LyricsDebug", "Directorio: ${audioDir?.absolutePath}")
                    android.util.Log.d("LyricsDebug", "Nombre sin ext: $audioNameWithoutExt")

                    if (audioDir != null && audioDir.exists()) {
                        android.util.Log.d("LyricsDebug", "Listando archivos en directorio...")

                        // Primero intentar con TTML para letras palabra por palabra
                        val ttmlFile = File(audioDir, "$audioNameWithoutExt.ttml")
                        android.util.Log.d("LyricsDebug", "Buscando TTML: ${ttmlFile.name}")

                        var ttmlParsedSuccessfully = false
                        if (ttmlFile.exists()) {
                            try {
                                val text = ttmlFile.readText()
                                android.util.Log.d(
                                    "LyricsDebug",
                                    "✓ Archivo TTML encontrado: ${ttmlFile.name}, ${text.length} chars",
                                )
                                val parsed = TtmlParser.parseTtml(text)
                                android.util.Log.d(
                                    "LyricsDebug",
                                    "✓ TTML parseado: ${parsed.lines.size} líneas, type=${parsed.type}",
                                )
                                
                                // Solo usar TTML si tiene líneas
                                if (parsed.lines.isNotEmpty()) {
                                    parsed.lines.forEachIndexed { i, line ->
                                        android.util.Log.d(
                                            "LyricsDebug",
                                            "  Línea $i: '${line.text}' (${line.syllabus.size} sílabas)",
                                        )
                                    }
                                    _ttmlLyrics.value = parsed
                                    _lyrics.value = emptyList() // Limpiar letras LRC
                                    android.util.Log.d(
                                        "LyricsDebug",
                                        "✓ StateFlow actualizado con TTML",
                                    )
                                    ttmlParsedSuccessfully = true
                                    return@launch
                                } else {
                                    android.util.Log.w("LyricsDebug", "TTML parseado pero vacío, intentando con LRC")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("LyricsDebug", "Error parseando TTML, intentando con LRC como fallback", e)
                            }
                        }

                        // Si no hay TTML o falló el parsing, intentar con LRC
                        val lrcFile = File(audioDir, "$audioNameWithoutExt.lrc")
                        android.util.Log.d("LyricsDebug", "Buscando LRC: ${lrcFile.name}")

                        if (lrcFile.exists()) {
                            try {
                                val text = lrcFile.readText()
                                android.util.Log.d(
                                    "LyricsDebug",
                                    "✓ Archivo LRC encontrado: ${lrcFile.name}, ${text.length} chars",
                                )
                                _ttmlLyrics.value = null // Limpiar TTML
                                _lyrics.value = parseLrc(text)
                                android.util.Log.d(
                                    "LyricsDebug",
                                    "✓ Lyrics LRC parseadas: ${_lyrics.value.size} líneas",
                                )
                                return@launch
                            } catch (e: Exception) {
                                android.util.Log.e("LyricsDebug", "Error leyendo archivo", e)
                            }
                        }

                        // Buscar cualquier archivo TTML o LRC en el directorio
                        val ttmlFiles =
                            audioDir.listFiles { _, name ->
                                name.endsWith(".ttml", ignoreCase = true)
                            }

                        // Intentar con TTML primero
                        var ttmlFoundAndParsed = false
                        ttmlFiles?.forEach { file ->
                            android.util.Log.d("LyricsDebug", "Evaluando TTML: ${file.name}")
                            val ttmlNameWithoutExt = file.nameWithoutExtension

                            val audioClean =
                                audioNameWithoutExt
                                    .replace(Regex("[:\\\\/*?\"<>|]"), "")
                                    .lowercase()
                                    .trim()
                            val ttmlClean =
                                ttmlNameWithoutExt
                                    .replace(Regex("[:\\\\/*?\"<>|]"), "")
                                    .lowercase()
                                    .trim()

                            if (audioClean == ttmlClean) {
                                try {
                                    val text = file.readText()
                                    android.util.Log.d(
                                        "LyricsDebug",
                                        "✓ Match TTML encontrado: ${file.name}",
                                    )
                                    val parsed = TtmlParser.parseTtml(text)
                                    
                                    // Solo usar TTML si tiene líneas
                                    if (parsed.lines.isNotEmpty()) {
                                        _ttmlLyrics.value = parsed
                                        _lyrics.value = emptyList()
                                        android.util.Log.d(
                                            "LyricsDebug",
                                            "✓ TTML cargado: ${parsed.lines.size} líneas",
                                        )
                                        ttmlFoundAndParsed = true
                                        return@launch
                                    } else {
                                        android.util.Log.w("LyricsDebug", "TTML parseado pero vacío, intentando con LRC")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("LyricsDebug", "Error parseando TTML, intentando con LRC como fallback", e)
                                }
                            }
                        }

                        // Si no hay TTML o falló el parsing, buscar LRC
                        val lrcFiles =
                            audioDir.listFiles { _, name ->
                                name.endsWith(".lrc", ignoreCase = true)
                            }
                        android.util.Log.d(
                            "LyricsDebug",
                            "Archivos .lrc encontrados: ${lrcFiles?.size ?: 0}",
                        )

                        lrcFiles?.forEach { file ->
                            android.util.Log.d("LyricsDebug", "Evaluando: ${file.name}")
                            val lrcNameWithoutExt = file.nameWithoutExtension

                            // Comparar ignorando caracteres problemáticos
                            val audioClean =
                                audioNameWithoutExt
                                    .replace(Regex("[:\\\\/*?\"<>|]"), "")
                                    .lowercase()
                                    .trim()
                            val lrcClean =
                                lrcNameWithoutExt
                                    .replace(Regex("[:\\\\/*?\"<>|]"), "")
                                    .lowercase()
                                    .trim()

                            android.util.Log.d("LyricsDebug", "  Audio clean: '$audioClean'")
                            android.util.Log.d("LyricsDebug", "  LRC clean: '$lrcClean'")

                            if (audioClean == lrcClean) {
                                try {
                                    val text = file.readText()
                                    android.util.Log.d(
                                        "LyricsDebug",
                                        "✓ Match LRC encontrado: ${file.name}",
                                    )
                                    _ttmlLyrics.value = null
                                    _lyrics.value = parseLrc(text)
                                    android.util.Log.d(
                                        "LyricsDebug",
                                        "✓ Lyrics LRC cargadas: ${_lyrics.value.size} líneas",
                                    )
                                    return@launch
                                } catch (e: Exception) {
                                    android.util.Log.e("LyricsDebug", "Error leyendo archivo", e)
                                }
                            }
                        }

                        android.util.Log.d("LyricsDebug", "✗ No se encontró .lrc coincidente")
                    } else {
                        android.util.Log.d("LyricsDebug", "✗ Directorio no existe o es null")
                    }
                }

                android.util.Log.d("LyricsDebug", "✗ No se encontraron letras")
                _ttmlLyrics.value = null
                _lyrics.value = emptyList()
            } catch (e: Exception) {
                android.util.Log.e("LyricsDebug", "✗ Excepción general", e)
                _ttmlLyrics.value = null
                _lyrics.value = emptyList()
            }
        }
    }

    fun startService(
        context: Context,
        song: Song,
        isPlaying: Boolean = true,
    ) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, MusicService::class.java).apply {
                putExtra("SONG_URI", song.uri.toString())
                putExtra("TITLE", song.title)
                putExtra("ARTIST", song.artist)
                putExtra("IS_PLAYING", isPlaying)
            },
        )
        saveLastSong(song, isPlaying)
    }

    private fun saveLastSong(
        song: Song,
        isPlaying: Boolean,
    ) {
        prefs.edit().apply {
            putString(LAST_SONG_URI, song.uri.toString())
            putString(LAST_SONG_TITLE, song.title)
            putString(LAST_SONG_ARTIST, song.artist)
            putBoolean(LAST_IS_PLAYING, isPlaying)
            apply()
        }
    }

    private fun loadLastSong() {
        try {
            val lastUri = prefs.getString(LAST_SONG_URI, null) ?: return
            val lastTitle = prefs.getString(LAST_SONG_TITLE, "Reproduciendo") ?: "Reproduciendo"
            val lastArtist = prefs.getString(LAST_SONG_ARTIST, "") ?: ""
            val lastIsPlaying = prefs.getBoolean(LAST_IS_PLAYING, false)

            // Verificar que haya canciones cargadas
            if (_songs.value.isEmpty()) {
                android.util.Log.d(
                    "MainViewModel",
                    "No hay canciones cargadas, no se puede restaurar última canción",
                )
                return
            }

            // Buscar la canción en la lista
            val song = _songs.value.find { it.uri.toString() == lastUri }

            if (song != null) {
                // Reproducir o preparar según estado previo
                playSong(song, autoPlay = lastIsPlaying)
                // NO iniciar el servicio aquí para evitar crash en arranque
            } else {
                android.util.Log.d(
                    "MainViewModel",
                    "Canción guardada ya no existe en la biblioteca",
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error al cargar última canción", e)
        }
    }

}
