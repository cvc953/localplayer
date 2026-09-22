package com.cvc953.localplayer.model

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.MatrixCursor
import android.provider.MediaStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LibrarySyncRegressionTest {
    @Test
    fun signatureUsesMaximumValuesAcrossAllSongs() {
        val signature =
            computeSignature(
                listOf(
                    SongKey(id = 10, dateAdded = 100, dateModified = 200),
                    SongKey(id = 20, dateAdded = 300, dateModified = 400),
                ),
            )

        assertEquals(LibrarySignature(2, 20, 300, 400), signature)
    }

    @Test
    fun mediaStoreSignatureUsesDatesFromEveryRow() {
        val context = mockk<Context>()
        val resolver = mockk<ContentResolver>()
        val cursor =
            MatrixCursor(
                arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.Audio.Media.DATE_MODIFIED,
                ),
            ).apply {
                addRow(arrayOf(20L, 300L, 400L))
                addRow(arrayOf(10L, 100L, 500L))
            }
        every { context.contentResolver } returns resolver
        every { resolver.query(any(), any(), any(), any(), any()) } returns cursor

        val signature = MediaStoreDataSource(context) { emptyList() }.currentSignature()

        assertEquals(LibrarySignature(2, 20, 300, 500), signature)
    }

    @Test
    fun refreshKeepsPreviousLibraryWhenMediaStoreQueryFails() = runBlocking {
        val context = mockk<Context>()
        every { context.checkSelfPermission(any()) } returns PackageManager.PERMISSION_GRANTED
        val prefs = FakeLibraryPreferences()
        val cache = FakeCacheStore()
        val dataSource = FakeDataSource()
        val repository =
            SongRepository(
                context = context,
                prefs = prefs,
                dataSource = dataSource,
                cacheStore = cache,
                ioDispatcher = Dispatchers.Unconfined,
            )

        val first = song(1)
        dataSource.songs = listOf(first)
        assertEquals(listOf(first), repository.refresh(force = true))

        dataSource.failure = true
        val result = repository.refresh(force = true)

        assertSame(first, result.single())
        assertEquals(listOf(first), repository.songs.value)
        assertEquals(1, cache.writeCount)
    }

    private fun song(id: Long) =
        Song(
            id = id,
            title = "Song $id",
            artist = "Artist",
            album = "Album",
            duration = 60_000,
            uri = android.net.Uri.parse("content://song/$id"),
        )

    private class FakeDataSource : LibraryDataSource {
        var songs = emptyList<Song>()
        var failure = false

        override fun querySongs(): List<Song> {
            if (failure) throw IllegalStateException("temporary MediaStore failure")
            return songs
        }

        override fun currentSignature() = LibrarySignature(songs.size, songs.maxOfOrNull { it.id } ?: 0)

        override fun countSongsForFolder(folderUriString: String) = songs.size
    }

    private class FakeLibraryPreferences : LibraryPreferences {
        var signature: String? = null
        override fun isFirstScanDone() = false
        override fun setFirstScanDone() = Unit
        override fun isAutoScanEnabled() = true
        override fun getMusicFolderUris() = emptyList<String>()
        override fun getLibrarySignature() = signature
        override fun setLibrarySignature(raw: String?) { signature = raw }
    }

    private class FakeCacheStore : SongsCacheStore {
        var writeCount = 0
        override fun read(): String? = null
        override fun write(json: String) { writeCount++ }
        override fun clear() = Unit
    }
}