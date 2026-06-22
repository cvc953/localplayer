package com.cvc953.localplayer.controller

import android.content.Context
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.model.SongRepository
import com.cvc953.localplayer.util.TagWriteInput
import com.cvc953.localplayer.util.TagWriteResult

data class AlbumTrackResult(
    val songId: Long,
    val title: String,
    val success: Boolean,
    val error: String? = null,
)

class SongController(
    private val context: Context,
) {
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

    fun deleteSong(song: Song): Result<Unit> {
        val deleted = repository.deleteSong(song)
        return if (deleted) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("No se pudo eliminar la canción: ${song.title}"))
        }
    }

    fun updateSongTags(
        songId: Long,
        input: TagWriteInput,
    ): Result<TagWriteResult> {
        val song =
            getSongById(songId)
                ?: return Result.failure(Exception("Song not found: $songId"))

        val result = repository.writeTags(song.uri, song.filePath, input)
        if (result.isSuccess) {
            repository.updateSongInCache(songId, input)
            song.filePath?.let { repository.scanSingleFile(it) }
        }
        return result
    }

    fun updateAlbumTags(
        albumName: String,
        artistName: String,
        input: TagWriteInput,
    ): List<AlbumTrackResult> {
        val songs =
            getAllSongs().filter {
                it.album.equals(albumName, ignoreCase = true) &&
                    it.artist.equals(artistName, ignoreCase = true)
            }

        val albumInput =
            TagWriteInput(
                album = input.album,
                artist = input.artist,
                genre = input.genre,
                year = input.year,
                coverArt = input.coverArt,
                coverMimeType = input.coverMimeType,
            )

        return songs.map { song ->
            val result = updateSongTags(song.id, albumInput)
            AlbumTrackResult(
                songId = song.id,
                title = song.title,
                success = result.isSuccess,
                error = result.exceptionOrNull()?.message,
            )
        }
    }
}
