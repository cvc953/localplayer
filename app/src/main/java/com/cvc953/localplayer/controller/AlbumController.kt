package com.cvc953.localplayer.controller

import android.content.Context
import com.cvc953.localplayer.model.Album
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.model.SongRepository

class AlbumController(
    private val context: Context,
) {
    private val repository = SongRepository(context)

    fun getAllAlbums(): List<Album> = repository.getAllAlbums()

    fun searchAlbums(query: String): List<Album> = getAllAlbums().filter { it.name.contains(query, ignoreCase = true) }

    fun getAlbumByName(name: String): Album? = getAllAlbums().find { it.name.equals(name, ignoreCase = true) }

    fun getSongsForAlbum(album: Album): List<Song> {
        // Normalización igual que en la UI: solo el primer artista normalizado
        fun normalizeAlbumName(name: String): List<String> = name.trim().split(',', '/').map { it.trim() }.filter { it.isNotEmpty() }
        fun normalizeArtistName(artist: String): List<String> =
            if (artist.trim().equals("AC/DC", ignoreCase = true)) listOf("AC/DC")
            else artist.trim().split(',', '/').map { it.trim() }.filter { it.isNotEmpty() }

        val mainArtist = normalizeArtistName(album.artist).firstOrNull() ?: album.artist
        return repository.loadSongs().filter { song ->
            normalizeAlbumName(song.album).any { it.equals(album.name.trim(), ignoreCase = true) } &&
            normalizeArtistName(song.artist).firstOrNull()?.equals(mainArtist, ignoreCase = true) == true
        }
    }
}
