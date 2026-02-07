package com.cvc953.localplayer.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.ContentObserver
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.model.Playlist
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.model.SongRepository
import com.cvc953.localplayer.services.MusicService
import com.cvc953.localplayer.ui.PlayerState
import com.cvc953.localplayer.ui.RepeatMode
import com.cvc953.localplayer.model.TtmlLyrics
import com.cvc953.localplayer.util.LrcLine
import com.cvc953.localplayer.util.parseLrc
import com.cvc953.localplayer.util.isTtml
import com.cvc953.localplayer.util.TtmlParser
import java.io.File
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

data class LyricLine(val timeMs: Long, val text: String)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        var instance: MainViewModel? = null
        private const val PREFS_NAME = "music_prefs"
        private const val LAST_SONG_URI = "last_song_uri"
        private const val LAST_SONG_TITLE = "last_song_title"
        private const val LAST_SONG_ARTIST = "last_song_artist"
        private const val LAST_IS_PLAYING = "last_is_playing"
        private const val PLAYLISTS_JSON = "playlists_json"
    }

    init {
        instance = this
    }

    private val repository = SongRepository(application)
    private val prefs: SharedPreferences =
            application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Lista de canciones
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    // Orden de visualización actual (usado para next/previous)
    private val _displayOrder = MutableStateFlow<List<Song>>(emptyList())
    val displayOrder: StateFlow<List<Song>> = _displayOrder

    // Estado del reproductor
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState

    private var mediaPlayer: MediaPlayer? = null

    private val _isPlayerScreenVisible = MutableStateFlow(false)
    val isPlayerScreenVisible: StateFlow<Boolean> = _isPlayerScreenVisible

    private var progressJob: Job? = null

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
    // Playlists
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists
    private var cachedShuffledRemaining: List<Song>? = null
    // Historial de reproducción para soportar "Anterior" respetando el orden
    private val playHistory: MutableList<Song> = mutableListOf()

    // Observer para detectar cambios en la biblioteca de música
    private val mediaStoreObserver =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    // Detectar cambios en la biblioteca y refrescar
                    refreshMusicLibrary()
                }
            }

    private fun refreshMusicLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newSongs = repository.loadSongs()
                val currentSongs = _songs.value

                // Solo actualizar si hay cambios (nuevas canciones o eliminadas)
                if (newSongs.size != currentSongs.size) {
                    _songs.value = newSongs.sortedBy { it.title }
                }
            } catch (e: Exception) {
                // Silenciosamente ignorar errores de lectura
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
                        "Re-escaneo completo: ${newSongs.size} canciones"
                )

                // Actualizar la lista con las nuevas canciones
                withContext(Dispatchers.Main) {
                    _songs.value = newSongs.sortedBy { it.title }
                    android.util.Log.d(
                            "MainViewModel",
                            "Lista actualizada en UI: ${_songs.value.size} canciones"
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

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
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
        _playlists.value = loadPlaylistsFromPrefs()
        // Registrar el observer para detectar cambios en la biblioteca
        getApplication<Application>()
                .contentResolver
                .registerContentObserver(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        true,
                        mediaStoreObserver
                )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Solo mostrar indicador si es la primera vez
                val firstScan = !repository.isFirstScanDone()
                if (firstScan) {
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
                            }
                    )
                    _isScanning.value = false
                } else {
                    val loaded = withContext(Dispatchers.IO) { repository.loadSongs() }
                    _songs.value = loaded.sortedBy { it.title }
                }

                // Cargar última canción reproducida
                loadLastSong()
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error inicializando ViewModel", e)
                _isScanning.value = false
            }
        }
    }

    fun playSong(song: Song, autoPlay: Boolean = true) {
        mediaPlayer?.release()

        try {
            mediaPlayer =
                    MediaPlayer().apply {
                        setDataSource(getApplication(), song.uri)
                        prepare()
                        if (autoPlay) start()

                        setOnCompletionListener {
                            if (_repeatMode.value == RepeatMode.ONE) {
                                // reinicia la misma canción
                                seekTo(0)
                                mediaPlayer?.start()
                                _playerState.update { it.copy(isPlaying = true) }
                            } else {
                                playNextSong()
                            }
                        }
                    }

            _playerState.value =
                    PlayerState(
                            currentSong = song,
                            isPlaying = autoPlay,
                            position = 0L,
                            duration = mediaPlayer?.duration?.toLong() ?: 0L
                    )

            if (autoPlay) startPositionUpdates() else progressJob?.cancel()
            loadLyricsForSong(song)
        } catch (e: Exception) {
            playNextSong()
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                progressJob?.cancel()
                _playerState.value = _playerState.value.copy(isPlaying = false)
                _playerState.value.currentSong?.let { song -> saveLastSong(song, false) }
            } else {
                it.start()
                // startProgressTracking()
                startPositionUpdates()
                _playerState.value = _playerState.value.copy(isPlaying = true)
                _playerState.value.currentSong?.let { song -> saveLastSong(song, true) }
            }
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
                            if (list.size == 1) currentSong
                            else {
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
                        if (nextIndex < list.size) list[nextIndex]
                        else if (_repeatMode.value == RepeatMode.ALL) list[0]
                        else return // NO hay siguiente canción si RepeatMode.NONE y estamos al
                        // final
                    }
                }

        // push current to history
        playHistory.add(currentSong)
        playSong(nextSong)
        startService(getApplication(), nextSong)
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
                        if (list.size == 1) currentSong
                        else {
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
                        if (prevIndex >= 0) list[prevIndex]
                        else if (_repeatMode.value == RepeatMode.ALL) list.last() else return
                    }
                }

        playSong(previousSong)
        startService(getApplication(), previousSong)
    }

    override fun onCleared() {
        progressJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        // Deregistrar el observer cuando el ViewModel se destruye
        getApplication<Application>().contentResolver.unregisterContentObserver(mediaStoreObserver)
        super.onCleared()
    }

    fun openPlayerScreen() {
        _isPlayerScreenVisible.value = true
    }

    fun closePlayerScreen() {
        _isPlayerScreenVisible.value = false
    }

    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _playerState.update { it.copy(position = position) }
    }

    private var positionJob: Job? = null

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob =
                viewModelScope.launch {
                    while (true) {
                        val player = mediaPlayer ?: break
                        _playerState.update {
                            it.copy(
                                    position = player.currentPosition.toLong(),
                                    duration = player.duration.toLong()
                            )
                        }
                        delay(50)
                    }
                }
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
                                        "FilePath obtenido: $audioFilePath"
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
                        
                        if (ttmlFile.exists()) {
                            try {
                                val text = ttmlFile.readText()
                                android.util.Log.d(
                                        "LyricsDebug",
                                        "✓ Archivo TTML encontrado: ${ttmlFile.name}, ${text.length} chars"
                                )
                                val parsed = TtmlParser.parseTtml(text)
                                android.util.Log.d(
                                        "LyricsDebug",
                                        "✓ TTML parseado: ${parsed.lines.size} líneas, type=${parsed.type}"
                                )
                                parsed.lines.forEachIndexed { i, line ->
                                    android.util.Log.d("LyricsDebug", "  Línea $i: '${line.text}' (${line.syllabus.size} sílabas)")
                                }
                                _ttmlLyrics.value = parsed
                                _lyrics.value = emptyList() // Limpiar letras LRC
                                android.util.Log.d(
                                        "LyricsDebug",
                                        "✓ StateFlow actualizado con TTML"
                                )
                                return@launch
                            } catch (e: Exception) {
                                android.util.Log.e("LyricsDebug", "Error leyendo TTML", e)
                            }
                        }

                        // Si no hay TTML, intentar con LRC
                        val lrcFile = File(audioDir, "$audioNameWithoutExt.lrc")
                        android.util.Log.d("LyricsDebug", "Buscando LRC: ${lrcFile.name}")

                        if (lrcFile.exists()) {
                            try {
                                val text = lrcFile.readText()
                                android.util.Log.d(
                                        "LyricsDebug",
                                        "✓ Archivo LRC encontrado: ${lrcFile.name}, ${text.length} chars"
                                )
                                _ttmlLyrics.value = null // Limpiar TTML
                                _lyrics.value = parseLrc(text)
                                android.util.Log.d(
                                        "LyricsDebug",
                                        "✓ Lyrics LRC parseadas: ${_lyrics.value.size} líneas"
                                )
                                return@launch
                            } catch (e: Exception) {
                                android.util.Log.e("LyricsDebug", "Error leyendo archivo", e)
                            }
                        }

                        // Buscar cualquier archivo TTML o LRC en el directorio
                        val ttmlFiles = audioDir.listFiles { _, name ->
                            name.endsWith(".ttml", ignoreCase = true)
                        }
                        
                        // Intentar con TTML primero
                        ttmlFiles?.forEach { file ->
                            android.util.Log.d("LyricsDebug", "Evaluando TTML: ${file.name}")
                            val ttmlNameWithoutExt = file.nameWithoutExtension
                            
                            val audioClean = audioNameWithoutExt
                                    .replace(Regex("[:\\\\/*?\"<>|]"), "")
                                    .lowercase()
                                    .trim()
                            val ttmlClean = ttmlNameWithoutExt
                                    .replace(Regex("[:\\\\/*?\"<>|]"), "")
                                    .lowercase()
                                    .trim()
                            
                            if (audioClean == ttmlClean) {
                                try {
                                    val text = file.readText()
                                    android.util.Log.d(
                                            "LyricsDebug",
                                            "✓ Match TTML encontrado: ${file.name}"
                                    )
                                    val parsed = TtmlParser.parseTtml(text)
                                    _ttmlLyrics.value = parsed
                                    _lyrics.value = emptyList()
                                    android.util.Log.d(
                                            "LyricsDebug",
                                            "✓ TTML cargado: ${parsed.lines.size} líneas"
                                    )
                                    return@launch
                                } catch (e: Exception) {
                                    android.util.Log.e("LyricsDebug", "Error leyendo TTML", e)
                                }
                            }
                        }
                        
                        // Si no hay TTML, buscar LRC
                        val lrcFiles =
                                audioDir.listFiles { _, name ->
                                    name.endsWith(".lrc", ignoreCase = true)
                                }
                        android.util.Log.d(
                                "LyricsDebug",
                                "Archivos .lrc encontrados: ${lrcFiles?.size ?: 0}"
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
                                            "✓ Match LRC encontrado: ${file.name}"
                                    )
                                    _ttmlLyrics.value = null
                                    _lyrics.value = parseLrc(text)
                                    android.util.Log.d(
                                            "LyricsDebug",
                                            "✓ Lyrics LRC cargadas: ${_lyrics.value.size} líneas"
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

    fun startService(context: Context, song: Song, isPlaying: Boolean = true) {
        ContextCompat.startForegroundService(
                context,
                Intent(context, MusicService::class.java).apply {
                    putExtra("SONG_URI", song.uri.toString())
                    putExtra("TITLE", song.title)
                    putExtra("ARTIST", song.artist)
                    putExtra("IS_PLAYING", isPlaying)
                }
        )
        saveLastSong(song, isPlaying)
    }

    private fun saveLastSong(song: Song, isPlaying: Boolean) {
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
                        "No hay canciones cargadas, no se puede restaurar última canción"
                )
                return
            }

            // Buscar la canción en la lista
            val song = _songs.value.find { it.uri.toString() == lastUri }

            if (song != null) {
                // Reproducir o preparar según estado previo
                playSong(song, autoPlay = lastIsPlaying)
                startService(getApplication(), song, lastIsPlaying)
            } else {
                android.util.Log.d(
                        "MainViewModel",
                        "Canción guardada ya no existe en la biblioteca"
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error al cargar última canción", e)
        }
    }
    fun createPlaylist(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false

        val exists = _playlists.value.any { it.name.equals(trimmed, ignoreCase = true) }
        if (exists) return false

        val updated = _playlists.value + Playlist(name = trimmed)
        _playlists.value = updated
        savePlaylistsToPrefs(updated)
        return true
    }
    fun deletePlaylist(name: String): Boolean {
        val updated = _playlists.value.filterNot { it.name == name }
        if (updated.size == _playlists.value.size) return false

        _playlists.value = updated
        savePlaylistsToPrefs(updated)
        return true
    }

    fun renamePlaylist(oldName: String, newName: String): Boolean {
        if (newName.isBlank()) return false
        if (newName == oldName) return true
        if (_playlists.value.any { it.name == newName }) return false

        val updated =
                _playlists.value.map { playlist ->
                    if (playlist.name == oldName) {
                        playlist.copy(name = newName)
                    } else {
                        playlist
                    }
                }

        _playlists.value = updated
        savePlaylistsToPrefs(updated)
        return true
    }

    fun addSongToPlaylist(playlistName: String, songId: Long): Boolean {
        val playlist = _playlists.value.find { it.name == playlistName } ?: return false

        // Si la canción ya está en la playlist, no agregarla
        if (songId in playlist.songIds) return false

        val updated =
                _playlists.value.map { p ->
                    if (p.name == playlistName) {
                        p.copy(songIds = p.songIds + songId)
                    } else {
                        p
                    }
                }

        _playlists.value = updated
        savePlaylistsToPrefs(updated)
        return true
    }

    fun removeSongFromPlaylist(playlistName: String, songId: Long): Boolean {
        val playlist = _playlists.value.find { it.name == playlistName } ?: return false
        if (songId !in playlist.songIds) return false

        val updated =
                _playlists.value.map { p ->
                    if (p.name == playlistName) {
                        p.copy(songIds = p.songIds.filterNot { it == songId })
                    } else {
                        p
                    }
                }

        _playlists.value = updated
        savePlaylistsToPrefs(updated)
        return true
    }

    fun addSongsToPlaylist(playlistName: String, songIds: List<Long>): Boolean {
        val playlist = _playlists.value.find { it.name == playlistName } ?: return false

        val newSongIds = songIds.filterNot { it in playlist.songIds }
        if (newSongIds.isEmpty()) return false

        val updated =
                _playlists.value.map { p ->
                    if (p.name == playlistName) {
                        p.copy(songIds = p.songIds + newSongIds)
                    } else {
                        p
                    }
                }

        _playlists.value = updated
        savePlaylistsToPrefs(updated)
        return true
    }

    fun removeSongsFromPlaylist(playlistName: String, songIds: List<Long>): Boolean {
        val playlist = _playlists.value.find { it.name == playlistName } ?: return false

        val updated =
                _playlists.value.map { p ->
                    if (p.name == playlistName) {
                        p.copy(songIds = p.songIds.filterNot { it in songIds })
                    } else {
                        p
                    }
                }

        _playlists.value = updated
        savePlaylistsToPrefs(updated)
        return true
    }

    private fun loadPlaylistsFromPrefs(): List<Playlist> {
        val raw = prefs.getString(PLAYLISTS_JSON, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<Playlist>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("name", "").trim()
                if (name.isEmpty()) continue
                val idsArray = obj.optJSONArray("songIds") ?: JSONArray()
                val ids = mutableListOf<Long>()
                for (j in 0 until idsArray.length()) {
                    ids.add(idsArray.optLong(j))
                }
                result.add(Playlist(name = name, songIds = ids))
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun savePlaylistsToPrefs(playlists: List<Playlist>) {
        val array = JSONArray()
        playlists.forEach { playlist ->
            val idsArray = JSONArray()
            playlist.songIds.forEach { idsArray.put(it) }
            val obj = JSONObject()
            obj.put("name", playlist.name)
            obj.put("songIds", idsArray)
            array.put(obj)
        }
        prefs.edit().putString(PLAYLISTS_JSON, array.toString()).apply()
    }

    fun isSongInPlaylist(playlistName: String, songId: Long): Boolean {
        val playlist = _playlists.value.find { it.name == playlistName } ?: return false
        return songId in playlist.songIds
    }
}
