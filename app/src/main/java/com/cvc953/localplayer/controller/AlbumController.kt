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

    fun getSongsForAlbum(album: Album): List<Song> = repository.loadSongs().filter { it.album == album.name && it.artist == album.artist }
}
