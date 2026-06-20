package com.cvc953.localplayer.controller

import android.content.Context
import com.cvc953.localplayer.model.Genre
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.model.SongRepository

class GenreController(
    private val context: Context,
) {
    private val repository = SongRepository(context)

    fun getAllGenres(): List<Genre> = repository.getAllGenres()

    fun searchGenres(query: String): List<Genre> =
        getAllGenres().filter { it.name.contains(query, ignoreCase = true) }

    fun getGenreByName(name: String): Genre? = getAllGenres().find { it.name.equals(name, ignoreCase = true) }

    fun getSongsForGenre(genre: Genre): List<Song> = repository.getSongsForGenre(genre.name)
}
