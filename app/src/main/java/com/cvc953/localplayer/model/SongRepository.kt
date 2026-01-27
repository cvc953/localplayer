package com.cvc953.localplayer.model

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.cvc953.localplayer.preferences.AppPrefs
import org.json.JSONArray
import org.json.JSONObject
import com.cvc953.localplayer.model.Song

class SongRepository(private val context: Context) {

    private val prefs = AppPrefs(context)

    fun loadSongs(): List<Song> {
        if (prefs.isFirstScanDone()) {
            val cached = loadSongsFromCache()
            if (cached.isNotEmpty()) return cached
        }

        val songs = scanSongsFromMediaStore()

        if (songs.isNotEmpty()) {
            saveSongsToCache(songs)
            prefs.setFirstScanDone()
        }

        return songs
    }

    private fun hasAudioPermission(): Boolean {
        val permission =
            if (Build.VERSION.SDK_INT >= 33)
                android.Manifest.permission.READ_MEDIA_AUDIO
            else
                android.Manifest.permission.READ_EXTERNAL_STORAGE

        return context.checkSelfPermission(permission) ==
                PackageManager.PERMISSION_GRANTED
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
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
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
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val uri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                val filePath = it.getString(dataCol)

                // Cargar la carátula
                val albumArt = try {
                    MediaMetadataRetriever().run {
                        setDataSource(context, uri)
                        embeddedPicture.also { release() }
                    }
                } catch (_: Exception) {
                    null
                }

                list.add(
                    Song(
                        id = id,
                        title = it.getString(titleCol),
                        artist = it.getString(artistCol),
                        album = it.getString(albumCol),
                        year = it.getInt(yearCol),
                        uri = uri,
                        duration = it.getLong(durCol),
                        albumArt = albumArt,
                        filePath = filePath
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
        try {
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
                        put("filePath", it.filePath ?: "")
                    }
                )
            }

            context.openFileOutput("songs_cache.json", Context.MODE_PRIVATE)
                .use { it.write(json.toString().toByteArray()) }
        } catch (e: Exception) {
            android.util.Log.e("SongRepository", "Error saving cache", e)
        }
    }

    private fun loadSongsFromCache(): List<Song> {
        try {
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
                        duration = o.getLong("duration"),
                        filePath = o.optString("filePath", null).takeIf { it.isNotEmpty() }
                    )
                )
            }

            return list
        } catch (e: Exception) {
            // Si hay error al cargar la caché (archivo corrupto, no existe, etc.)
            // retornar lista vacía para forzar un re-escaneo
            android.util.Log.e("SongRepository", "Error loading cache", e)
            return emptyList()
        }
    }

    fun isFirstScanDone(): Boolean = prefs.isFirstScanDone()

    private fun countSongs(): Int {
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media._ID),
            MediaStore.Audio.Media.IS_MUSIC + "!= 0",
            null,
            null
        )
        return cursor?.count ?: 0
    }

    fun scanSongs(
        onProgress: (current: Int, total: Int) -> Unit
    ): List<Song> {

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DURATION
        )

        val total = countSongs()
        var current = 0
        val list = mutableListOf<Song>()

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
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val uri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                val filePath = it.getString(dataCol)

                val song = Song(
                    id = id,
                    title = it.getString(titleCol),
                    artist = it.getString(artistCol),
                    album = it.getString(albumCol),
                    year = it.getInt(yearCol),
                    uri = uri,
                    duration = it.getLong(durCol),
                    albumArt = null,
                    filePath = filePath
                )

                list.add(song)

                current++
                onProgress(current, total)
            }
        }

        return list
    }

    /**
     * Variante del escaneo que notifica cada canción encontrada.
     * Llama a [onSongFound] por cada canción y a [onProgress] durante el proceso.
     */
    fun scanSongsStreaming(
        onSongFound: (Song) -> Unit,
        onProgress: (current: Int, total: Int) -> Unit
    ): List<Song> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        val total = countSongs()
        var current = 0
        val list = mutableListOf<Song>()

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

                val song = Song(
                    id = id,
                    title = it.getString(titleCol),
                    artist = it.getString(artistCol),
                    album = it.getString(albumCol),
                    year = it.getInt(yearCol),
                    uri = uri,
                    duration = it.getLong(durCol),
                    albumArt = null
                )

                list.add(song)
                onSongFound(song)

                current++
                onProgress(current, total)
            }
        }

        if (list.isNotEmpty()) {
            saveSongsToCache(list)
            prefs.setFirstScanDone()
        }

        return list
    }



}

