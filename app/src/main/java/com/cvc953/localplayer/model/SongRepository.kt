/*package com.cvc953.localplayer.model

import android.content.Context
import android.provider.MediaStore
import android.content.ContentUris
import android.media.MediaMetadataRetriever
import android.net.Uri


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
}*/


package com.cvc953.localplayer.model

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.graphics.painter.BitmapPainter
import com.cvc953.localplayer.preferences.AppPrefs
import org.json.JSONArray
import org.json.JSONObject

class SongRepository(private val context: Context) {

    private val prefs = AppPrefs(context)

    fun loadSongs(): List<Song> {
        return if (prefs.isFirstScanDone()) {
            loadSongsFromCache()
        } else {
            val songs = scanSongsFromMediaStore()
            saveSongsToCache(songs)
            prefs.setFirstScanDone()
            songs
        }
    }

    // -------------------------
    // MediaStore (1ª vez)
    // -------------------------
    private fun scanSongsFromMediaStore(): List<Song> {
        val list = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DURATION
        )

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            MediaStore.Audio.Media.IS_MUSIC + "!= 0",
            null,
            null
        ) ?: return emptyList()

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val yearCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val uri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

                list.add(
                    Song(
                        id = id,
                        title = it.getString(titleCol),
                        artist = it.getString(artistCol),
                        album = it.getString(albumCol),
                        year = it.getInt(yearCol),
                        uri = uri,
                        duration = it.getLong(durCol)
                    )
                )
            }
        }

        return list
    }

    // -------------------------
    // Cache
    // -------------------------
    private fun saveSongsToCache(songs: List<Song>) {
        val json = JSONArray()

        songs.forEach {
            json.put(
                JSONObject().apply {
                    put("id", it.id)
                    put("title", it.title)
                    put("artist", it.artist)
                    put("album", it.album)
                    put("year", it.year)
                    put("uri", it.uri.toString())
                    put("duration", it.duration)
                }
            )
        }

        context.openFileOutput("songs_cache.json", Context.MODE_PRIVATE)
            .use { it.write(json.toString().toByteArray()) }
    }

    private fun loadSongsFromCache(): List<Song> {
        val text = context.openFileInput("songs_cache.json")
            .bufferedReader()
            .use { it.readText() }

        val json = JSONArray(text)
        val list = mutableListOf<Song>()

        for (i in 0 until json.length()) {
            val o = json.getJSONObject(i)
            list.add(
                Song(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    artist = o.getString("artist"),
                    album = o.getString("album"),
                    year = o.getInt("year"),
                    uri = Uri.parse(o.getString("uri")),
                    duration = o.getLong("duration")
                )
            )
        }

        return list
    }
}

