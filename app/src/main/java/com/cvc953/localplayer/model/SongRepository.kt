package com.cvc953.localplayer.model

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.cvc953.localplayer.preferences.AppPrefs
import com.cvc953.localplayer.util.TagWriteInput
import com.cvc953.localplayer.util.TagWriteResult
import com.cvc953.localplayer.util.TagWriter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * singleton app-scoped state
 */
class SongRepository internal constructor(
    private val context: Context,
    private val prefs: LibraryPreferences,
    private val dataSource: LibraryDataSource,
    private val cacheStore: SongsCacheStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val uriFactory: (String) -> Uri = { Uri.parse(it) },
) {
    companion object {
        private const val TAG = "SongRepository"
        private const val AUTO_REFRESH_DEBOUNCE_MS = 1_500L

        @Volatile
        private var instance: SongRepository? = null

        fun getInstance(context: Context): SongRepository {
            val appContext = context.applicationContext
            return instance ?: synchronized(this) {
                instance ?: run {
                    val appPrefs = AppPrefs(appContext)
                    SongRepository(
                        context = appContext,
                        prefs = appPrefs,
                        dataSource = MediaStoreDataSource(appContext) { appPrefs.getMusicFolderUris() },
                        cacheStore = FileSongsCacheStore(appContext),
                    ).also { instance = it }
                }
            }
        }
    }

    private enum class LoadState {
        NONE,
        CACHE,
        FRESH,
    }

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val refreshMutex = Mutex()

    @Volatile
    private var loadState = LoadState.NONE

    private var ensureJob: Job? = null
    private var autoRefreshJob: Job? = null

    @Volatile
    private var observing = false

    private val _songs = MutableStateFlow<List<Song>>(emptyList())

    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isFirstScanning = MutableStateFlow(false)

    val isFirstScanning: StateFlow<Boolean> = _isFirstScanning.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)

    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun loadSongs(): List<Song> {
        if (loadState == LoadState.NONE) {
            val cached = readCache()
            if (cached != null && cached.isNotEmpty()) {
                _songs.value = cached
                loadState = LoadState.CACHE
            }
            ensureLoadedAsync()
        }
        return _songs.value
    }

    fun isFirstScanDone(): Boolean = prefs.isFirstScanDone()

    fun ensureLoadedAsync() {
        if (loadState == LoadState.FRESH) return
        if (ensureJob?.isActive == true) return
        ensureJob = scope.launch { ensureLoaded() }
    }

    suspend fun ensureLoaded(): List<Song> =
        withContext(ioDispatcher) {
            if (loadState == LoadState.FRESH) return@withContext _songs.value
            if (!hasAudioPermission()) return@withContext _songs.value

            val cached = readCache()
            val storedSignature = LibrarySignature.deserialize(prefs.getLibrarySignature())
            val currentSignature = dataSource.currentSignature()

            if (cached != null && cached.isNotEmpty() && storedSignature != null && currentSignature != null && storedSignature == currentSignature) {
                _songs.value = cached
                loadState = LoadState.FRESH
                return@withContext cached
            }

            if (currentSignature == null && cached != null && cached.isNotEmpty()) {
                _songs.value = cached
                loadState = LoadState.CACHE
                return@withContext cached
            }

            refresh(force = false, blocking = cached.isNullOrEmpty())
        }

    suspend fun refresh(
        force: Boolean = false,
        blocking: Boolean = false,
    ): List<Song> =
        withContext(ioDispatcher) {
            refreshMutex.withLock {
                if (!hasAudioPermission()) return@withLock _songs.value

                if (blocking) _isFirstScanning.value = true else _isSyncing.value = true
                try {
                    val sourceSignature = dataSource.currentSignature()
                    val storedSignature = LibrarySignature.deserialize(prefs.getLibrarySignature())

                    if (!force && loadState == LoadState.FRESH && sourceSignature != null && sourceSignature == storedSignature) {
                        return@withLock _songs.value
                    }

                    val scanned =
                        dataSource
                            .querySongs()
                            .filter { isPlayableMusic(it.filePath, it.duration) }

                    val previous = _songs.value
                    // Un escaneo que no pudo leer la fuente no es una biblioteca vacía: se
                    // conserva lo anterior y no se persiste nada. Antes esta guarda era
                    // inalcanzable porque currentSignature() nunca devolvía null.
                    if (scanned.isEmpty() && sourceSignature == null) {
                        Log.w(TAG, "refresh: MediaStore no respondió; se conserva la biblioteca actual (${previous.size})")
                        return@withLock previous
                    }

                    val currentKeys = scanned.map { it.toKey() }
                    val changed =
                        loadState != LoadState.FRESH ||
                            diffLibrary(previous.map { it.toKey() }, currentKeys).hasChanges

                    if (changed) {
                        _songs.value = scanned
                    }

                    persistCache(scanned, sourceSignature ?: computeSignature(currentKeys))
                    loadState = LoadState.FRESH
                    _songs.value
                } catch (e: Exception) {
                    Log.e(TAG, "refresh: error escaneando la biblioteca", e)
                    _songs.value
                } finally {
                    if (blocking) _isFirstScanning.value = false else _isSyncing.value = false
                }
            }
        }

    fun startObserving() {
        if (observing) return
        observing = true
        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaStoreObserver,
            )
        } catch (e: Exception) {
            observing = false
            Log.e(TAG, "startObserving: no se pudo registrar el observer", e)
        }
    }

    fun stopObserving() {
        if (!observing) return
        try {
            context.contentResolver.unregisterContentObserver(mediaStoreObserver)
        } catch (e: Exception) {
            Log.w(TAG, "stopObserving: no se pudo desregistrar el observer", e)
        }
        observing = false
        autoRefreshJob?.cancel()
    }

    fun onAppForegrounded() {
        scope.launch {
            if (!prefs.isAutoScanEnabled()) return@launch
            if (!hasAudioPermission()) return@launch

            val current = dataSource.currentSignature()
            val stored = LibrarySignature.deserialize(prefs.getLibrarySignature())
            if (loadState != LoadState.FRESH || (current != null && current != stored)) {
                refresh(force = true)
            }
        }
    }

    private val mediaStoreObserver: ContentObserver by lazy {
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                if (!prefs.isAutoScanEnabled()) return
                scheduleAutoRefresh()
            }
        }
    }

    private fun scheduleAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob =
            scope.launch {
                try {
                    delay(AUTO_REFRESH_DEBOUNCE_MS)
                    refresh(force = false)
                } catch (e: Exception) {
                    Log.e(TAG, "auto refresh failed", e)
                }
            }
    }

    // -------------------------
    // Caché
    // -------------------------

    fun invalidateCache() {
        cacheStore.clear()
        prefs.setLibrarySignature(null)
        loadState = LoadState.NONE
        ensureJob?.cancel()
        ensureJob = null
        ensureLoadedAsync()
    }

    private fun persistCache(
        songs: List<Song>,
        signature: LibrarySignature,
    ) {
        try {
            cacheStore.write(SongsCacheCodec.encode(songs.map { it.toCached() }))
            prefs.setLibrarySignature(signature.serialize())
            prefs.setFirstScanDone()
        } catch (e: Exception) {
            Log.e(TAG, "persistCache: no se pudo guardar la caché", e)
        }
    }

    private fun readCache(): List<Song>? {
        val cached = SongsCacheCodec.decode(cacheStore.read()) ?: return null
        return cached.map { it.toSong() }
    }

    private fun Song.toCached(): CachedSong =
        CachedSong(
            id = id,
            title = title,
            artist = artist,
            album = album,
            year = year,
            uri = uri.toString(),
            duration = duration,
            filePath = filePath,
            trackNumber = trackNumber,
            discNumber = discNumber,
            sampleRate = sampleRate,
            mimeType = mimeType,
            dateAdded = dateAdded,
            dateModified = dateModified,
            genre = genre,
        )

    private fun CachedSong.toSong(): Song =
        Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            year = year,
            uri = uriFactory(uri),
            duration = duration,
            albumArt = null,
            filePath = filePath,
            trackNumber = trackNumber,
            discNumber = discNumber,
            sampleRate = sampleRate,
            mimeType = mimeType,
            dateAdded = dateAdded,
            dateModified = dateModified,
            genre = genre,
        )

    private fun Song.toKey(): SongKey = SongKey(id = id, dateAdded = dateAdded, dateModified = dateModified)

    /**
     * Devuelve la lista de géneros únicos con el número de canciones de cada uno.
     */
    fun getAllGenres(): List<Genre> {
        val songs = loadSongs()
        return songs
            .groupBy { it.genre.ifBlank { "Desconocido" } }
            .map { (name, songs) -> Genre(name, songs.size) }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Devuelve las canciones que pertenecen a un género específico.
     */
    fun getSongsForGenre(genreName: String): List<Song> =
        loadSongs().filter { song ->
            val genre = song.genre.ifBlank { "Desconocido" }
            genre.equals(genreName, ignoreCase = true)
        }

    /**
     * Devuelve la lista de artistas únicos con el número de canciones de cada uno.
     */
    fun getAllArtists(): List<Artist> {
        val songs = loadSongs()
        return songs
            .groupBy { it.artist.ifBlank { "Desconocido" } }
            .map { (name, songs) -> Artist(name, songs.size) }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Devuelve la lista de álbumes únicos con el número de canciones de cada uno.
     */
    fun getAllAlbums(): List<Album> {
        val songs = loadSongs()
        return songs
            .groupBy { Pair(it.album.ifBlank { "Desconocido" }, it.artist.ifBlank { "Desconocido" }) }
            .map { (key, songs) -> Album(key.first, key.second, songs.size) }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.artist.lowercase() }))
    }

    fun countSongsForFolder(folderUriString: String): Int =
        try {
            dataSource.countSongsForFolder(folderUriString)
        } catch (e: Exception) {
            Log.w(TAG, "countSongsForFolder failed", e)
            0
        }

    /**
     * Elimina una canción del dispositivo a través de MediaStore y limpia la caché.
     * En API 30+, el sistema ya borró el archivo via [MediaStore.createDeleteRequest],
     * solo actualizamos el estado y refrescamos.
     */
    fun deleteSong(song: Song): Boolean {
        val removeFromState = {
            _songs.value = _songs.value.filterNot { it.id == song.id }
            invalidateCache()
            scope.launch { refresh(force = true) }
            true
        }

        // En API 30+ el sistema ya borró el archivo via createDeleteRequest
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return removeFromState()
        }
        return try {
            if (context.contentResolver.delete(song.uri, null, null) > 0) {
                removeFromState()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting song", e)
            false
        }
    }

    /**
     * Escribe tags en un archivo de audio usando TagWriter.
     */
    fun writeTags(
        uri: Uri,
        filePath: String?,
        input: TagWriteInput,
    ): Result<TagWriteResult> = TagWriter.writeTags(context, filePath, uri, input)

    /**
     * Actualiza los campos editables en el estado en memoria y en la caché para una canción.
     */
    fun updateSongInCache(
        songId: Long,
        input: TagWriteInput,
    ) {
        val current = _songs.value
        val index = current.indexOfFirst { it.id == songId }
        if (index < 0) return

        val updated =
            current[index].let { song ->
                song.copy(
                    title = input.title ?: song.title,
                    artist = input.artist ?: song.artist,
                    album = input.album ?: song.album,
                    genre = input.genre ?: song.genre,
                    year = input.year?.toIntOrNull() ?: song.year,
                )
            }

        _songs.value = current.toMutableList().also { it[index] = updated }
        try {
            cacheStore.write(SongsCacheCodec.encode(_songs.value.map { it.toCached() }))
        } catch (e: Exception) {
            Log.w(TAG, "updateSongInCache: no se pudo actualizar la caché", e)
        }
    }

    /**
     * Solicita al MediaScanner que re-escanee un archivo específico.
     */
    fun scanSingleFile(filePath: String) {
        try {
            MediaScannerConnection.scanFile(context, arrayOf(filePath), null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning file: $filePath", e)
        }
    }

    // -------------------------
    // Permisos
    // -------------------------

    private fun hasAudioPermission(): Boolean {
        val permission =
            if (Build.VERSION.SDK_INT >= 33) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}

class FileSongsCacheStore(
    private val context: Context,
) : SongsCacheStore {
    private val fileName = "songs_cache.json"

    override fun read(): String? =
        try {
            context.openFileInput(fileName).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        }

    override fun write(json: String) {
        try {
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(json.toByteArray())
            }
        } catch (e: Exception) {
            Log.e("FileSongsCacheStore", "No se pudo escribir la caché", e)
        }
    }

    override fun clear() {
        try {
            context.deleteFile(fileName)
        } catch (e: Exception) {
            Log.w("FileSongsCacheStore", "No se pudo borrar la caché", e)
        }
    }
}
