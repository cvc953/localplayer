package com.cvc953.localplayer.model

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Regresiones del escaneo de biblioteca: una fila con columnas nulas o una consulta que
 * falla no deben vaciar la lista de canciones.
 */
class LibraryScanHardeningTest {
    private val uri: Uri = mockk(relaxed = true)

    @Before
    fun setUp() {
        // android.util.Log es un stub en los tests JVM: hay que interceptarlo.
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // -------------------------
    // Mapeo de filas
    // -------------------------

    @Test
    fun nullMetadataColumnsProduceEmptyStrings() {
        val song =
            songFromRow(
                id = 7L,
                title = null,
                artist = null,
                album = null,
                year = 0,
                uri = uri,
                duration = 180_000L,
                filePath = "/storage/emulated/0/Music/a.mp3",
                trackNumber = 0,
                discNumber = 0,
                sampleRate = null,
                mimeType = null,
                dateAdded = 1L,
                dateModified = 2L,
                genre = null,
            )

        assertEquals(7L, song.id)
        assertEquals("", song.title)
        assertEquals("", song.artist)
        assertEquals("", song.album)
        assertEquals("", song.genre)
        assertEquals(180_000L, song.duration)
    }

    @Test
    fun missingFileMetadataStaysNull() {
        val song =
            songFromRow(
                id = 8L,
                title = "Song",
                artist = "Artist",
                album = "Album",
                year = 1999,
                uri = uri,
                duration = 90_000L,
                filePath = null,
                trackNumber = 3,
                discNumber = 1,
                sampleRate = null,
                mimeType = null,
                dateAdded = 0L,
                dateModified = 0L,
                genre = "Rock",
            )

        assertEquals(null, song.filePath)
        assertEquals(3, song.trackNumber)
        assertEquals("Rock", song.genre)
    }

    // -------------------------
    // Escaneo que falla
    // -------------------------

    @Test
    fun failedScanKeepsPreviousLibraryAndCache() = runBlocking {
        val dataSource = FakeDataSource(signature = LibrarySignature(2, 2L, 10L, 20L), songs = listOf(song(1L), song(2L)))
        val prefs = FakePreferences()
        val cache = FakeCacheStore()
        val repository = repository(dataSource, prefs, cache)

        assertEquals(2, repository.refresh(force = true).size)
        val writesAfterGoodScan = cache.writes

        dataSource.signature = null
        dataSource.songs = emptyList()

        val afterFailure = repository.refresh(force = true)

        assertEquals("la biblioteca no debe vaciarse cuando la consulta falla", 2, afterFailure.size)
        assertEquals("no debe persistirse un escaneo fallido", writesAfterGoodScan, cache.writes)
    }

    @Test
    fun queryExceptionKeepsPreviousLibrary() = runBlocking {
        val dataSource = FakeDataSource(signature = LibrarySignature(2, 2L, 10L, 20L), songs = listOf(song(1L), song(2L)))
        val prefs = FakePreferences()
        val cache = FakeCacheStore()
        val repository = repository(dataSource, prefs, cache)

        assertEquals(2, repository.refresh(force = true).size)

        dataSource.failOnQuery = true

        assertEquals(2, repository.refresh(force = true).size)
    }

    @Test
    fun successfulEmptyScanClearsLibrary() = runBlocking {
        val dataSource = FakeDataSource(signature = LibrarySignature(2, 2L, 10L, 20L), songs = listOf(song(1L), song(2L)))
        val prefs = FakePreferences()
        val cache = FakeCacheStore()
        val repository = repository(dataSource, prefs, cache)

        assertEquals(2, repository.refresh(force = true).size)

        // El usuario borró todas las canciones: la consulta responde bien y devuelve cero.
        dataSource.signature = LibrarySignature.EMPTY
        dataSource.songs = emptyList()

        assertEquals(0, repository.refresh(force = true).size)
    }

    // -------------------------
    // Utilidades
    // -------------------------

    private fun song(id: Long): Song =
        Song(
            id = id,
            title = "Song $id",
            artist = "Artist",
            album = "Album",
            year = null,
            uri = uri,
            duration = 120_000L,
            trackNumber = 0,
            discNumber = 0,
        )

    private fun repository(
        dataSource: LibraryDataSource,
        prefs: FakePreferences,
        cache: FakeCacheStore,
    ): SongRepository {
        val context = mockk<Context>(relaxed = true)
        every { context.checkSelfPermission(any()) } returns PackageManager.PERMISSION_GRANTED
        return SongRepository(
            context = context,
            prefs = prefs,
            dataSource = dataSource,
            cacheStore = cache,
            ioDispatcher = Dispatchers.Unconfined,
            uriFactory = { uri },
        )
    }

    private class FakeDataSource(
        var signature: LibrarySignature?,
        var songs: List<Song>,
    ) : LibraryDataSource {
        var failOnQuery = false

        override fun querySongs(): List<Song> {
            if (failOnQuery) throw IllegalStateException("MediaStore no disponible")
            return songs
        }

        override fun currentSignature(): LibrarySignature? = signature

        override fun countSongsForFolder(folderUriString: String): Int = songs.size
    }

    private class FakePreferences : LibraryPreferences {
        private var firstScanDone = false
        private var signature: String? = null

        override fun isFirstScanDone(): Boolean = firstScanDone

        override fun setFirstScanDone() {
            firstScanDone = true
        }

        override fun isAutoScanEnabled(): Boolean = true

        override fun getMusicFolderUris(): List<String> = listOf("content://test/tree/primary%3AMusic")

        override fun getLibrarySignature(): String? = signature

        override fun setLibrarySignature(raw: String?) {
            signature = raw
        }
    }

    private class FakeCacheStore : SongsCacheStore {
        var contents: String? = null
        var writes = 0

        override fun read(): String? = contents

        override fun write(json: String) {
            contents = json
            writes++
        }

        override fun clear() {
            contents = null
        }
    }
}
