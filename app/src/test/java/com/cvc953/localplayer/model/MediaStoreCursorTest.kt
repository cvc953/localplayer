package com.cvc953.localplayer.model

import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.Runs
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Reproduce, sin dispositivo, lo que devuelve un proveedor de MediaStore cuando una fila
 * trae columnas en NULL o esconde alguna columna.
 *
 * Este es el escenario de issue #59: el contador de carpetas proyectaba sólo `_ID` y veía las
 * canciones, mientras el escaneo completo se caía en la primera fila sin tags.
 */
class MediaStoreCursorTest {
    private val columns =
        listOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.CD_TRACK_NUMBER,
            MediaStore.Audio.Media.DISC_NUMBER,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.GENRE,
            "sample_rate",
        )

    @Before
    fun setUp() {
        // android.util.Log y los métodos estáticos de Uri son stubs en los tests JVM.
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        mockkStatic(Uri::class)
        every { Uri.withAppendedPath(any(), any<String>()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun rowsWithNullTagsDoNotAbortTheScan() {
        val rows =
            (1..3L).map { id ->
                row(
                    id = id,
                    title = null,
                    artist = null,
                    album = null,
                    genre = null,
                )
            }

        val out = mutableListOf<Song>()
        readSongsFromCursor(cursor(columns, rows), mockk(relaxed = true), out)

        assertEquals("las tres filas deben sobrevivir", 3, out.size)
        assertTrue(out.all { it.title.isEmpty() })
        assertTrue(out.all { it.artist.isEmpty() })
        assertTrue(out.all { it.album.isEmpty() })
        assertTrue(out.all { it.genre.isEmpty() })
        assertEquals(listOf(1L, 2L, 3L), out.map { it.id })
    }

    @Test
    fun hiddenColumnDoesNotAbortTheScan() {
        val available = columns - MediaStore.Audio.Media.ALBUM - "sample_rate"
        val rows =
            listOf(
                row(id = 10L, title = "Song", artist = "Artist", album = "Album", genre = "Rock"),
                row(id = 11L, title = "Other", artist = null, album = null, genre = null),
            )

        val out = mutableListOf<Song>()
        readSongsFromCursor(cursor(available, rows), mockk(relaxed = true), out)

        assertEquals(2, out.size)
        assertEquals("", out[0].album)
        assertEquals("Rock", out[0].genre)
        assertEquals(null, out[1].artist?.takeIf { it.isNotEmpty() })
    }

    @Test
    fun nullTagsAreReplacedButRealTagsSurvive() {
        val rows =
            listOf(
                row(id = 20L, title = "Tema", artist = null, album = "Disco", genre = "Jazz"),
            )

        val out = mutableListOf<Song>()
        readSongsFromCursor(cursor(columns, rows), mockk(relaxed = true), out)

        assertEquals(1, out.size)
        assertEquals("Tema", out[0].title)
        assertEquals("", out[0].artist)
        assertEquals("Disco", out[0].album)
        assertEquals("Jazz", out[0].genre)
        assertEquals(180_000L, out[0].duration)
    }

    // -------------------------
    // Utilidades
    // -------------------------

    private fun row(
        id: Long,
        title: String?,
        artist: String?,
        album: String?,
        genre: String?,
    ): Map<String, Any?> =
        mapOf(
            MediaStore.Audio.Media._ID to id,
            MediaStore.Audio.Media.TITLE to title,
            MediaStore.Audio.Media.ARTIST to artist,
            MediaStore.Audio.Media.ALBUM to album,
            MediaStore.Audio.Media.YEAR to 1999,
            MediaStore.Audio.Media.DURATION to 180_000L,
            MediaStore.Audio.Media.DATA to "/storage/emulated/0/Music/$id.mp3",
            MediaStore.Audio.Media.CD_TRACK_NUMBER to 1,
            MediaStore.Audio.Media.DISC_NUMBER to 1,
            MediaStore.Audio.Media.MIME_TYPE to "audio/mpeg",
            MediaStore.Audio.Media.DATE_ADDED to 1_700_000_000L,
            MediaStore.Audio.Media.DATE_MODIFIED to 1_700_000_100L,
            MediaStore.Audio.Media.GENRE to genre,
            "sample_rate" to 44_100,
        )

    /** Cursor falso: sólo expone las columnas de [columns] y responde NULL donde el mapa lo pide. */
    private fun cursor(
        columns: List<String>,
        rows: List<Map<String, Any?>>,
    ): Cursor {
        val cursor = mockk<Cursor>()
        val indexOf = columns.withIndex().associate { (position, name) -> name to position }
        var position = -1

        fun current(): Map<String, Any?> = rows[position]

        every { cursor.getColumnIndex(any<String>()) } answers { indexOf[firstArg<String>()] ?: -1 }
        every { cursor.getColumnIndexOrThrow(any<String>()) } answers {
            indexOf[firstArg<String>()] ?: throw IllegalArgumentException("columna inexistente: ${firstArg<String>()}")
        }
        every { cursor.count } returns rows.size
        every { cursor.moveToNext() } answers {
            position++
            position < rows.size
        }
        every { cursor.getLong(any<Int>()) } answers {
            (current()[columns[firstArg<Int>()]] as? Number)?.toLong() ?: 0L
        }
        every { cursor.getInt(any<Int>()) } answers {
            (current()[columns[firstArg<Int>()]] as? Number)?.toInt() ?: 0
        }
        every { cursor.getString(any<Int>()) } answers {
            current()[columns[firstArg<Int>()]] as? String
        }
        every { cursor.close() } just Runs
        return cursor
    }
}
