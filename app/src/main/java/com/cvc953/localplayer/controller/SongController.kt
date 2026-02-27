package com.cvc953.localplayer.controller

import android.content.Context
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.model.SongRepository

class SongController(private val context: Context) {
    private val repository = SongRepository(context)

    fun getAllSongs(): List<Song> = repository.loadSongs()

    fun forceRescan(): List<Song> = repository.forceRescanSongs()

    fun searchSongs(query: String): List<Song> =
        getAllSongs().filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
        }

    fun getSongById(id: Long): Song? = getAllSongs().find { it.id == id }
}
