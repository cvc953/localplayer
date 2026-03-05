package com.cvc953.localplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.PlayerController
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.PlayerState
import com.cvc953.localplayer.ui.RepeatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.collections.get
import kotlin.compareTo
import kotlin.text.compareTo
import android.content.Intent
import androidx.core.content.ContextCompat
import com.cvc953.localplayer.services.MusicService

class PlaybackViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val playerController = PlayerController.getInstance(getApplication(), viewModelScope)
    private val prefs = com.cvc953.localplayer.preferences.AppPrefs(getApplication())
    val playerState: StateFlow<PlayerState> = playerController.state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue

    // backup of explicit order before enabling shuffle so we can restore it
    private var _originalQueueBeforeShuffle: List<Song>? = null

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat

    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode

    // Expose current playback position as StateFlow for lyrics and progress UI
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    // Track current album being played (album name and artist)
    private var currentAlbumName: String? = null
    private var currentAlbumArtist: String? = null
    private var _nextAlbumName: String? = null
    private var _nextAlbumArtist: String? = null


    // Callback to load next album
    private var onAlbumEndedCallback: ((String, String) -> Unit)? = null

    init {
        // Restore persisted playback state (queue, shuffle, repeat, last position)
        try {
            val prefs = com.cvc953.localplayer.preferences.AppPrefs(getApplication())
            val repo = com.cvc953.localplayer.model.SongRepository(getApplication())
            val allSongs = repo.loadSongs()
            
            // Restore next album info
            _nextAlbumName = prefs.loadNextAlbumName()
            _nextAlbumArtist = prefs.loadNextAlbumArtist()
            
            val queueUris = prefs.loadPlaybackQueue()
            if (queueUris.isNotEmpty()) {
                val restored = queueUris.mapNotNull { uriStr ->
                    allSongs.find { it.uri.toString() == uriStr }
                }
                if (restored.isNotEmpty()) {
                    var queueToUse = restored
                    // set shuffle/repeat flags
                    val shuffleEnabled = prefs.loadShuffleEnabled()
                    _isShuffle.value = shuffleEnabled
                    prefs.loadRepeatMode()?.let { modeStr ->
                        try {
                            _repeatMode.value = com.cvc953.localplayer.ui.RepeatMode.valueOf(modeStr)
                            _isRepeat.value = _repeatMode.value != com.cvc953.localplayer.ui.RepeatMode.NONE
                            playerController.setRepeatMode(_repeatMode.value)
                        } catch (_: Exception) {}
                    }

                    val lastUri = prefs.loadLastSongUri()
                    val startIndexOrig = if (lastUri != null) restored.indexOfFirst { it.uri.toString() == lastUri } else -1
                    var startIndex = startIndexOrig
                    if (shuffleEnabled && startIndexOrig >= 0) {
                        val currentSong = restored[startIndexOrig]
                        val rest = restored.filter { it != currentSong }.shuffled()
                        queueToUse = listOf(currentSong) + rest
                        startIndex = 0
                    }
                    _queue.value = queueToUse
                    if (startIndex >= 0) {
                        try {
                            val wasPlaying = prefs.loadIsPlaying()
                            val pos = prefs.loadPlaybackPosition()
                            // Inicializa el reproductor en pausa, hace seek y reanuda solo si corresponde
                            playerController.playNow(
                                queueToUse,
                                startIndex,
                                startPaused = !wasPlaying,
                                seekToPosition = if (pos > 0L) pos else null,
                                resumeAfterSeek = wasPlaying
                            )
                            _currentPosition.value = pos
                            // Lanzar MusicService para mostrar la notificación aunque esté en pausa
                            val song = queueToUse.getOrNull(startIndex)
                            if (song != null) {
                                val intent = Intent(getApplication<Application>(), MusicService::class.java).apply {
                                    action = MusicService.ACTION_UPDATE_STATE
                                    putExtra("IS_PLAYING", wasPlaying)
                                    putExtra("POSITION", pos)
                                    putExtra("SONG_URI", song.uri.toString())
                                    putExtra("TITLE", song.title)
                                    putExtra("ARTIST", song.artist)
                                }
                                ContextCompat.startForegroundService(getApplication(), intent)
                            }
                        } catch (_: Exception) {}
                    } else {
                        // no last song, keep controller queue in sync without starting playback
                        try { playerController.queueEnd(queueToUse) } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
        }

        // Periodically update current position while playing
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                if (playerController.isPlaying()) {
                    _currentPosition.value = playerController.getCurrentPosition()
                }
                kotlinx.coroutines.delay(200L)
            }
        }
    }

    fun play(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                // Sync repeat mode with PlayerController before playing
                playerController.setRepeatMode(_repeatMode.value)
                
                // Si la canción no es la única en la cola, sincroniza la cola con la lista actual
                val queue = _queue.value
                val index = queue.indexOfFirst { it.id == song.id }
                if (queue.size > 1) {
                    // Si tenemos una cola establecida con >1 canción, usa playNow
                    // Si la canción está en la cola, reproduce desde ese índice; si no, desde el principio
                    val indexToPlay = if (index >= 0) index else 0
                    playerController.playNow(queue, indexToPlay)
                } else if (queue.size == 1) {
                    // Si la cola tiene solo 1 canción, asegúrate de que sea la correcta
                    playerController.playNow(queue, 0)
                } else {
                    // Si no hay cola establecida, reproduce solo esta canción
                    playerController.playSong(song)
                }
                _currentPosition.value = 0L
                // Persistir estado de reproducción
                try {
                    prefs.saveLastSongUri(song.uri.toString())
                    prefs.savePlaybackPosition(0L)
                    prefs.saveIsPlaying(true)
                    prefs.savePlaybackQueue(_queue.value.map { it.uri.toString() })
                } catch (_: Exception) {}
                try {
                    val intent = Intent(getApplication<Application>(), MusicService::class.java).apply {
                        putExtra("SONG_URI", song.uri.toString())
                        putExtra("TITLE", song.title)
                        putExtra("ARTIST", song.artist)
                        putExtra("IS_PLAYING", true)
                    }
                    ContextCompat.startForegroundService(getApplication(), intent)
                } catch (_: Exception) {
                }
            } catch (e: Exception) {
                _error.value = "Error al reproducir: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun pause() {
        playerController.pause()
        _currentPosition.value = playerController.getCurrentPosition()
        try {
            prefs.savePlaybackPosition(_currentPosition.value)
            prefs.saveIsPlaying(false)
        } catch (_: Exception) {}
        // Update service
        try {
            val intent = Intent(getApplication<Application>(), MusicService::class.java).apply {
                action = MusicService.ACTION_UPDATE_STATE
                putExtra("IS_PLAYING", false)
                putExtra("POSITION", _currentPosition.value)
            }
            ContextCompat.startForegroundService(getApplication(), intent)
        } catch (_: Exception) {
        }
    }

    fun resume() {
        playerController.resume()
        _currentPosition.value = playerController.getCurrentPosition()
        try {
            prefs.savePlaybackPosition(_currentPosition.value)
            prefs.saveIsPlaying(true)
        } catch (_: Exception) {}
        try {
                val song = playerState.value.currentSong
            val intent = Intent(getApplication<Application>(), MusicService::class.java).apply {
                action = MusicService.ACTION_UPDATE_STATE
                putExtra("IS_PLAYING", true)
                putExtra("POSITION", _currentPosition.value)
                if (song != null) {
                    putExtra("SONG_URI", song.uri.toString())
                    putExtra("TITLE", song.title)
                    putExtra("ARTIST", song.artist)
                }
            }
            ContextCompat.startForegroundService(getApplication(), intent)
        } catch (_: Exception) {
        }
    }

    fun playAlbum(albumName: String, artistName: String, songs: List<Song>) {
        currentAlbumName = albumName
        currentAlbumArtist = artistName
        
        // Load all albums and add them to the queue
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val repo = com.cvc953.localplayer.model.SongRepository(getApplication())
                val allSongs = repo.loadSongs()
                
                // Extract unique albums from all songs
                val uniqueAlbums = mutableListOf<Pair<String, String>>()
                val seenKeys = mutableSetOf<String>()
                
                for (song in allSongs) {
                    val albumKey = "${song.album.trim().lowercase()}|${song.artist.trim().lowercase()}"
                    if (seenKeys.add(albumKey)) {
                        uniqueAlbums.add(Pair(song.album.trim(), song.artist.trim()))
                    }
                }
                
                // Sort albums alphabetically by name
                val sortedAlbums = uniqueAlbums.sortedBy { it.first.lowercase() }
                
                // Find current album index
                val currentIndex = sortedAlbums.indexOfFirst { (name, artist) ->
                    name.equals(albumName, ignoreCase = true) && artist.equals(artistName, ignoreCase = true)
                }
                
                if (currentIndex >= 0) {
                    // Build complete queue starting from current album
                    val queueAlbums = sortedAlbums.drop(currentIndex) + sortedAlbums.take(currentIndex)
                    
                    // Build full queue with all songs from all albums in order
                    val fullQueue = mutableListOf<Song>()
                    for ((albumName, albumArtist) in queueAlbums) {
                        val albumSongs = allSongs.filter { song ->
                            song.album.trim().equals(albumName, ignoreCase = true) &&
                                song.artist.trim().equals(albumArtist, ignoreCase = true)
                        }.sortedWith(compareBy<Song>({ it.discNumber }, { it.trackNumber }))
                        fullQueue.addAll(albumSongs)
                    }
                    
                    // Update the queue with all albums
                    if (fullQueue.isNotEmpty()) {
                        android.util.Log.d("PlaybackViewModel", "playAlbum - Setting queue with ${fullQueue.size} songs from ${queueAlbums.size} albums")
                        _queue.value = fullQueue
                        try { 
                            prefs.savePlaybackQueue(fullQueue.map { it.uri.toString() })
                        } catch (_: Exception) {}
                    }
                    
                    // Find and save next album
                    if (currentIndex < sortedAlbums.size - 1) {
                        val nextAlbum = sortedAlbums[currentIndex + 1]
                        _nextAlbumName = nextAlbum.first
                        _nextAlbumArtist = nextAlbum.second
                        try {
                            prefs.saveNextAlbum(_nextAlbumName, _nextAlbumArtist)
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PlaybackViewModel", "Error loading albums for queue", e)
            }
        }
        
        // Configure listener for when the album ends naturally
        playerController.setOnQueueEndedListener {
            if (currentAlbumName == albumName && currentAlbumArtist == artistName) {
                if (_nextAlbumName != null && _nextAlbumArtist != null) {
                    loadNextAlbumAutomatically(_nextAlbumName!!, _nextAlbumArtist!!)
                }
            }
        }
        
        // Configure listener for when user skips to next at the end of album
        playerController.setOnNextAtEndListener {
            if (currentAlbumName == albumName && currentAlbumArtist == artistName) {
                if (_nextAlbumName != null && _nextAlbumArtist != null) {
                    loadNextAlbumAutomatically(_nextAlbumName!!, _nextAlbumArtist!!)
                }
            }
        }
    }

    fun setNextAlbumCallback(callback: ((nextAlbumName: String, nextAlbumArtist: String) -> Unit)?) {
        onAlbumEndedCallback = callback
    }

    fun loadNextAlbumAutomatically(nextAlbumName: String, nextAlbumArtist: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val repo = com.cvc953.localplayer.model.SongRepository(getApplication())
                val allSongs = repo.loadSongs()
                
                // Get all songs from the next album
                val nextAlbumSongs = allSongs.filter { song ->
                    song.album.trim().equals(nextAlbumName, ignoreCase = true) &&
                        song.artist.trim().equals(nextAlbumArtist, ignoreCase = true)
                }.sortedWith(compareBy<Song>({ it.discNumber }, { it.trackNumber }))
                
                if (nextAlbumSongs.isNotEmpty()) {
                    // Setup for the next album (this reconfigures listeners)
                    playAlbum(nextAlbumName, nextAlbumArtist, nextAlbumSongs)
                    // Start playing the next album
                    updateDisplayOrder(nextAlbumSongs)
                    play(nextAlbumSongs.first())
                }
            } catch (e: Exception) {
                // Silently ignore errors
            }
        }
    }

    fun stop() {
        playerController.stop()
        _currentPosition.value = 0L
        try {
            prefs.savePlaybackPosition(0L)
            prefs.saveIsPlaying(false)
        } catch (_: Exception) {}
        try {
            val intent = Intent(getApplication<Application>(), MusicService::class.java).apply {
                action = MusicService.ACTION_UPDATE_STATE
                putExtra("IS_PLAYING", false)
                putExtra("POSITION", 0L)
            }
            ContextCompat.startForegroundService(getApplication(), intent)
        } catch (_: Exception) {
        }
    }

    fun seekTo(position: Long) {
        playerController.seekTo(position)
        _currentPosition.value = position
        try { prefs.savePlaybackPosition(position) } catch (_: Exception) {}
    }

    fun updatePosition() {
        val pos = playerController.getCurrentPosition()
        _currentPosition.value = pos
    }

    fun addToQueueNext(song: Song) {
        val currentQueue = _queue.value.toMutableList()
        val currentIndex = playerState.value.currentSong?.let { currentQueue.indexOf(it) } ?: -1
        val insertIndex = if (currentIndex >= 0) currentIndex + 1 else 0
        currentQueue.add(insertIndex, song)
        _queue.value = currentQueue
        try { prefs.savePlaybackQueue(_queue.value.map { it.uri.toString() }); playerController.replaceQueue(_queue.value, keepCurrentSong = true) } catch (_: Exception) {}
    }

    fun addToQueueEnd(song: Song) {
        val currentQueue = _queue.value.toMutableList()
        currentQueue.add(song)
        _queue.value = currentQueue
        try { prefs.savePlaybackQueue(_queue.value.map { it.uri.toString() }); playerController.replaceQueue(_queue.value, keepCurrentSong = true) } catch (_: Exception) {}
    }

    fun removeFromQueue(song: Song) {
        _queue.value = _queue.value.filterNot { it.id == song.id }
        try { prefs.savePlaybackQueue(_queue.value.map { it.uri.toString() }); playerController.replaceQueue(_queue.value, keepCurrentSong = true) } catch (_: Exception) {}
    }

    fun clearQueue() {
        _queue.value = emptyList()
        try { prefs.savePlaybackQueue(emptyList()); playerController.replaceQueue(emptyList(), keepCurrentSong = false) } catch (_: Exception) {}
    }

    fun updateDisplayOrder(songs: List<Song>) {
        _queue.value = songs
        try { prefs.savePlaybackQueue(_queue.value.map { it.uri.toString() }) } catch (_: Exception) {}
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
        val current = playerState.value.currentSong
        if (_isShuffle.value) {
            // enable shuffle: keep a backup so we can restore later
            if (_originalQueueBeforeShuffle == null) _originalQueueBeforeShuffle = _queue.value.toList()
            val rest = _queue.value.filter { it != current }.shuffled()
            val newQueue = if (current != null && _queue.value.contains(current)) listOf(current) + rest else rest
            _queue.value = newQueue
            try { playerController.replaceQueue(newQueue, keepCurrentSong = true) } catch (_: Exception) {}
        } else {
            // disable shuffle: sort the queue alphabetically by title then artist
            val sorted = _queue.value.sortedWith(compareBy({ it.title.lowercase() }, { it.artist.lowercase() }))
            _queue.value = sorted
            try { playerController.replaceQueue(sorted, keepCurrentSong = true) } catch (_: Exception) {}
            _originalQueueBeforeShuffle = null
        }
        try {
            prefs.saveShuffleEnabled(_isShuffle.value)
            // clear persisted explicit queue when user toggles shuffle to avoid restoring a stale order
            prefs.savePlaybackQueue(emptyList())
        } catch (_: Exception) {}
    }

    fun setShuffle(enabled: Boolean) {
        if (_isShuffle.value != enabled) {
            toggleShuffle()
        }
    }

    fun toggleRepeat() {
        // cycle through RepeatMode similar to previous MainViewModel behavior
        _repeatMode.value =
            when (_repeatMode.value) {
                RepeatMode.NONE -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.NONE
            }
        _isRepeat.value = _repeatMode.value != RepeatMode.NONE
        android.util.Log.d("PlaybackViewModel", "toggleRepeat - new mode: ${_repeatMode.value}")
        playerController.setRepeatMode(_repeatMode.value)
        try { prefs.saveRepeatMode(_repeatMode.value.name) } catch (_: Exception) {}
    }

    fun getUpcomingSongs(): List<Song> {
        val current = playerState.value.currentSong ?: return _queue.value
        val all = _queue.value
        if (all.isEmpty()) return emptyList()

        return when {
            _isShuffle.value -> {
                all.filter { it != current }.shuffled()
            }
            _repeatMode.value == RepeatMode.ALL -> {
                val idx = all.indexOf(current)
                if (idx == -1) all else all.drop(idx + 1) + all.take(idx)
            }
            else -> {
                val idx = all.indexOf(current)
                if (idx == -1) emptyList() else all.drop(idx + 1)
            }
        }
    }

    fun playNextSong() {
        android.util.Log.d("PlaybackViewModel", "playNextSong() - delegating to PlayerController.next()")
        // Delegate to PlayerController which handles repeat mode correctly
        playerController.next()
    }

    fun playPreviousSong() {
        android.util.Log.d("PlaybackViewModel", "playPreviousSong() - delegating to PlayerController.previous()")
        // Delegate to PlayerController
        playerController.previous()
    }

    fun togglePlayPause() {
        if (playerState.value.isPlaying) pause() else resume()
    }
}
