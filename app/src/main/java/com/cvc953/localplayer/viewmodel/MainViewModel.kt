package com.cvc953.localplayer.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.cvc953.localplayer.model.SongRepository
import android.media.MediaPlayer
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.cvc953.localplayer.ui.RepeatMode
import com.cvc953.localplayer.util.LrcLine
import com.cvc953.localplayer.util.parseLrc
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import androidx.compose.runtime.State
import androidx.core.content.ContextCompat
import kotlinx.coroutines.withContext
import com.cvc953.localplayer.services.MusicService
import com.cvc953.localplayer.ui.MusicScreen


data class LyricLine(val timeMs: Long, val text: String)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        var instance: MainViewModel? = null
        private const val PREFS_NAME = "music_prefs"
        private const val LAST_SONG_URI = "last_song_uri"
        private const val LAST_SONG_TITLE = "last_song_title"
        private const val LAST_SONG_ARTIST = "last_song_artist"
        private const val LAST_IS_PLAYING = "last_is_playing"
    }
    
    init {
        instance = this
    }

    private val repository = SongRepository(application)
    private val prefs: SharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Lista de canciones
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    //Estado del reproductor
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

    private val _isScanning = mutableStateOf(false)
    val isScanning: State<Boolean> = _isScanning

    private val _scanProgress = mutableStateOf(0f)
    val scanProgress: State<Float> = _scanProgress

    // Cola de reproducción
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue

    fun addToQueueNext(song: Song) {
        val list = _queue.value.toMutableList()
        list.add(0, song)
        _queue.value = list
    }

    fun addToQueueEnd(song: Song) {
        val list = _queue.value.toMutableList()
        list.add(song)
        _queue.value = list
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val list = _queue.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _queue.value = list
    }

    fun getUpcomingSongs(): List<Song> {
        val current = playerState.value.currentSong ?: return _queue.value
        val base = songs.value
        val upcoming = mutableListOf<Song>()

        // Primero la cola explícita
        upcoming.addAll(_queue.value)

        val remaining = when {
            _isShuffle.value -> base.filter { it != current }.shuffled()
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
        viewModelScope.launch(Dispatchers.IO) {
            // Solo mostrar indicador si es la primera vez
            val firstScan = !repository.isFirstScanDone()
            if (firstScan) _isScanning.value = true

            val loadedSongs = withContext(Dispatchers.IO) { repository.loadSongs() }
            _songs.value = repository.loadSongs().sortedBy { it.title }

            _isScanning.value = false
            
            // Cargar última canción reproducida
            loadLastSong()
        }
    }

    fun playSong(song: Song, autoPlay: Boolean = true) {
        mediaPlayer?.release()

        try {
            mediaPlayer = MediaPlayer().apply {
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

            _playerState.value = PlayerState(
                currentSong = song,
                isPlaying = autoPlay,
                position = 0L,
                duration = mediaPlayer?.duration?.toLong() ?: 0L
            )

            if (autoPlay) startPositionUpdates() else progressJob?.cancel()
            loadLyricsForSong(song)

        } catch (e: Exception){
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
                //startProgressTracking()
                startPositionUpdates()
                _playerState.value = _playerState.value.copy(isPlaying = true)
                _playerState.value.currentSong?.let { song -> saveLastSong(song, true) }
            }
        }
    }

    fun playNextSong() {
        val list = songs.value
        val currentSong = playerState.value.currentSong ?: return
        if (list.isEmpty()) return

        // Priorizar canciones en cola
        popQueue()?.let { queued ->
            playSong(queued)
            startService(getApplication(), queued)
            return
        }

        val nextSong = when {
            _isShuffle.value -> {
                // Aleatorio diferente al actual
                if (list.size == 1) currentSong else {
                    var randomSong: Song
                    do {
                        randomSong = list.random()
                    } while (randomSong == currentSong)
                    randomSong
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
                else return // NO hay siguiente canción si RepeatMode.NONE y estamos al final
            }
        }

        playSong(nextSong)
        startService(getApplication(), nextSong)
    }



    fun playPreviousSong() {
        val list = songs.value
        val currentSong = playerState.value.currentSong ?: return
        if (list.isEmpty()) return

        val previousSong = when {
            _isShuffle.value -> {
                if (list.size == 1) currentSong else {
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
                else if (_repeatMode.value == RepeatMode.ALL) list.last()
                else return
            }
        }

        playSong(previousSong)
        startService(getApplication(), previousSong)
    }



    override fun onCleared() {
        progressJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
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
        _playerState.update {
            it.copy(position = position)
        }
    }


    private var positionJob: Job? = null

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
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
    }

    // Cambiar repetición
    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.NONE -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.NONE
        }
    }

    fun toggleLyrics() {
        _showLyrics.value = !_showLyrics.value
    }

    fun loadLyricsForSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver

            val baseName = song.title
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME
            )

            val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$baseName%.lrc")

            val uri = MediaStore.Files.getContentUri("external")

            val cursor = resolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val id = it.getLong(0)
                    val lrcUri = ContentUris.withAppendedId(uri, id)

                    val text = resolver.openInputStream(lrcUri)
                        ?.bufferedReader()
                        ?.use { r -> r.readText() }

                    _lyrics.value = text?.let { parseLrc(it) } ?: emptyList()
                } else {
                    _lyrics.value = emptyList()
                }
            }
        }
    }

    fun startService(context: Context, song: Song, isPlaying: Boolean = true) {
        ContextCompat.startForegroundService(context, Intent(context, MusicService::class.java).apply {
            putExtra("SONG_URI", song.uri.toString())
            putExtra("TITLE", song.title)
            putExtra("ARTIST", song.artist)
            putExtra("IS_PLAYING", isPlaying)
        })
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
        val lastUri = prefs.getString(LAST_SONG_URI, null) ?: return
        val lastTitle = prefs.getString(LAST_SONG_TITLE, "Reproduciendo") ?: "Reproduciendo"
        val lastArtist = prefs.getString(LAST_SONG_ARTIST, "") ?: ""
        val lastIsPlaying = prefs.getBoolean(LAST_IS_PLAYING, false)
        
        // Buscar la canción en la lista
        val song = _songs.value.find { it.uri.toString() == lastUri }
        
        if (song != null) {
            // Reproducir o preparar según estado previo
            playSong(song, autoPlay = lastIsPlaying)
            startService(getApplication(), song, lastIsPlaying)
        }
    }
}



