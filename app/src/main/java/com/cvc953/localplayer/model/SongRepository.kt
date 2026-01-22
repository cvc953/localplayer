package com.cvc953.localplayer.model

import android.content.Context
import android.provider.MediaStore
import android.app.Application
import android.content.ContentUris
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher

class SongRepository(private val context: Context) {

    fun loadSongs(): List<Song> {
        val songs = mutableListOf<Song>()
        val resolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DURATION
        )

        val cursor = resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val title = it.getString(1)
                val artist = it.getString(2)
                val album = it.getString(3)
                val year = it.getInt(4)
                val duration = it.getLong(5)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val albumArt = extractAlbumArt(uri)

                songs.add(
                    Song(id, title, artist, album, year, uri, duration, albumArt)
                )
            }
        }

        return songs
    }

    private fun extractAlbumArt(uri: Uri): ByteArray? {
        return try {
            MediaMetadataRetriever().run {
                setDataSource(context, uri)
                embeddedPicture.also { release() }
            }
        } catch (e: Exception) {
            null
        }
    }
}
