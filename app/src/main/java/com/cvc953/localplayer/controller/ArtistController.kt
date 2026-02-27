package com.cvc953.localplayer.controller

import android.content.Context
import com.cvc953.localplayer.model.Artist
import com.cvc953.localplayer.model.SongRepository

class ArtistController(private val context: Context) {
    private val repository = SongRepository(context)

    fun getAllArtists(): List<Artist> = repository.getAllArtists()

    fun searchArtists(query: String): List<Artist> =
        getAllArtists().filter { it.name.contains(query, ignoreCase = true) }

    fun getArtistByName(name: String): Artist? = getAllArtists().find { it.name.equals(name, ignoreCase = true) }
}
