package com.cvc953.localplayer.controller

import org.junit.Test
import org.junit.Assert.*

/**
 * Tests para verificar que la búsqueda de canciones funciona correctamente
 * con caracteres especiales: ": ç č ć aã"
 *
 * Nota: Este test usa una estructura de datos simplificada para evitar
 * dependencias de Android (como Uri) en unit tests puros.
 */
class SongSearchTest {

    /**
     * Data class simplificada para pruebas (sin Uri)
     */
    data class TestSong(
        val id: Long,
        val title: String,
        val artist: String = "Unknown Artist",
        val album: String = "Unknown Album"
    )

    /**
     * Función de búsqueda pura (sin dependencias de Android)
     * Espeja la lógica de SongController.searchSongs()
     */
    private fun searchSongs(query: String, songs: List<TestSong>): List<TestSong> =
        songs.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
        }

    @Test
    fun testSearchWithDoubleColon() {
        val songs = listOf(
            TestSong(1, "Song: The Beginning"),
            TestSong(2, "Another Song")
        )

        val results = searchSongs(":", songs)
        assertEquals(1, results.size)
        assertEquals("Song: The Beginning", results[0].title)
    }

    @Test
    fun testSearchWithCedilla() {
        val songs = listOf(
            TestSong(1, "Français Café", artist = "Moussaka"),
            TestSong(2, "English Song")
        )

        val results = searchSongs("ç", songs)
        assertEquals(1, results.size)
        assertEquals("Français Café", results[0].title)
    }

    @Test
    fun testSearchWithCaron() {
        val songs = listOf(
            TestSong(1, "Czech Dream", artist = "Čeština Band"),
            TestSong(2, "Normal Artist")
        )

        val results = searchSongs("č", songs)
        assertEquals(1, results.size)
        assertEquals("Čeština Band", results[0].artist)
    }

    @Test
    fun testSearchWithAcute() {
        val songs = listOf(
            TestSong(1, "Song", artist = "Aćid Rock"),
            TestSong(2, "Another")
        )

        val results = searchSongs("ć", songs)
        assertEquals(1, results.size)
        assertEquals("Aćid Rock", results[0].artist)
    }

    @Test
    fun testSearchWithTilde() {
        val songs = listOf(
            TestSong(1, "Ãmazing", album = "Português Fado"),
            TestSong(2, "Normal Song")
        )

        val results = searchSongs("ã", songs)
        assertEquals(1, results.size)
        assertEquals("Português Fado", results[0].album)
    }

    @Test
    fun testSearchMultipleSpecialCharacters() {
        val songs = listOf(
            TestSong(
                1,
                "Café: Français",
                artist = "Čeština",
                album = "Ãustria Mix"
            ),
            TestSong(2, "Normal Song", "Normal Artist", "Normal Album")
        )

        // Buscar por colon y cedilla juntos (diferentes campos)
        val resultsWithColon = searchSongs(":", songs)
        val resultsWithCedilla = searchSongs("ç", songs)

        assertEquals(1, resultsWithColon.size)
        assertEquals(1, resultsWithCedilla.size)
        assertEquals(resultsWithColon[0].id, resultsWithCedilla[0].id)
    }

    @Test
    fun testSearchCaseInsensitiveWithSpecialChars() {
        val songs = listOf(
            TestSong(1, "CAFÉ SONG", artist = "Čeština"),
            TestSong(2, "café song", artist = "čeština")
        )

        // Ambas canciones coinciden con case-insensitive
        val resultUppercase = searchSongs("CAFÉ", songs)
        val resultLowercase = searchSongs("café", songs)
        val resultCaron = searchSongs("Č", songs)

        assertEquals(2, resultUppercase.size)  // Coincide con ambas (case-insensitive)
        assertEquals(2, resultLowercase.size)  // Coincide con ambas (case-insensitive)
        assertEquals(2, resultCaron.size)      // Ambas tienen Č/č
    }

    @Test
    fun testSearchNoResults() {
        val songs = listOf(
            TestSong(1, "English Song"),
            TestSong(2, "Another Normal Song")
        )

        val results = searchSongs("ç", songs)
        assertEquals(0, results.size)
    }

    @Test
    fun testSearchAllCharactersReported() {
        val songs = listOf(
            TestSong(
                1,
                "Café: Façade",  // contiene : ç
                artist = "Čeština Aćid",  // contiene č ć
                album = "Ãustria"  // contiene ã
            )
        )

        // Todos estos caracteres están realmente en los datos
        val specialChars = listOf(":", "ç", "č", "ć", "ã")

        for (char in specialChars) {
            val results = searchSongs(char, songs)
            assertTrue(
                "Búsqueda con carácter '$char' debe encontrar al menos 1 canción",
                results.isNotEmpty()
            )
        }
    }

    @Test
    fun testSearchWithWhitespaceAndSpecialChars() {
        val songs = listOf(
            TestSong(1, "Café Café: Morning", artist = "Čeština"),
            TestSong(2, "Normal: Song")
        )

        val results = searchSongs("Café:", songs)
        assertEquals(1, results.size)
        assertEquals("Café Café: Morning", results[0].title)
    }

    @Test
    fun testSearchMultipleMatches() {
        val songs = listOf(
            TestSong(1, "Café Song"),
            TestSong(2, "Café Noir", artist = "Café Band"),
            TestSong(3, "Café Latte", album = "Café Collection"),
            TestSong(4, "Tea Song")
        )

        val results = searchSongs("café", songs)
        assertEquals(3, results.size)
    }
}
