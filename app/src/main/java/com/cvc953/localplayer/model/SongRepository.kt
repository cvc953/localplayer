package com.cvc953.localplayer.model

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.cvc953.localplayer.preferences.AppPrefs
import org.json.JSONArray
import org.json.JSONObject

class SongRepository(private val context: Context) {

    private val prefs = AppPrefs(context)

    /**
     * Lista de rutas que deben ser excluidas del escaneo. Esto incluye directorios de apps de
     * mensajería, notificaciones del sistema, tonos, alarmas, efectos de sonido y otras apps que
     * almacenan audio pero que no son canciones de música.
     */
    private fun isExcludedPath(filePath: String?): Boolean {
        if (filePath == null) return false

        val excludedPaths =
                listOf(
                        // Apps de mensajería y redes sociales
                        "/WhatsApp/",
                        "/Snapchat/",
                        "/TikTok/",
                        "/Instagram/",
                        "/facebook/",
                        "/Discord/",
                        "/Viber/",
                        "/Signal/",
                        "/Skype/",
                        "/Messenger/",
                        "/.telegram/",
                        "/Android/data/com.whatsapp/",
                        "/Android/data/com.telegram/",
                        "/Android/data/com.snapchat/",
                        "/Android/data/com.tiktok/",
                        "/Android/data/com.instagram/",
                        "/Android/data/com.facebook/",
                        "/Android/data/com.discord/",
                        "/Android/data/com.viber/",
                        "/Android/data/org.signal/",
                        "/Android/data/com.skype/",
                        "/Android/data/com.facebook.orca/",
                        "/Android/media/com.whatsapp/",
                        "/Android/media/com.telegram/",
                        "/Android/media/com.snapchat/",
                        "/Android/media/com.tiktok/",
                        "/Android/media/com.instagram/",
                        "/Android/media/com.facebook/",
                        "/Android/media/com.discord/",
                        "/Android/media/com.viber/",
                        "/Android/media/org.signal/",
                        "/Android/media/com.skype/",
                        "/Android/media/com.facebook.orca/",
                        // Directorios del sistema (notificaciones, tonos, alarmas)
                        "/Notifications/",
                        "/Ringtones/",
                        "/Alarms/",
                        "/UI/",
                        "/system/media/audio/notifications/",
                        "/system/media/audio/ringtones/",
                        "/system/media/audio/alarms/",
                        "/system/media/audio/ui/",
                        // Grabaciones de voz y llamadas
                        "/Recordings/",
                        "/Voice Recorder/",
                        "/Call Recording/",
                        "/Call Recordings/",
                        "/Voice/",
                        "/Sounds/",
                        "/AudioRecorder/",
                        // Directorios de apps de juegos (suelen tener .ogg como efectos)
                        "/Android/obb/",
                        "/Android/data/com.game",
                        "/Android/data/com.unity",
                        "/game_data/",
                        "/assets/sounds/",
                        "/assets/audio/"
                )

        return excludedPaths.any { filePath.contains(it, ignoreCase = true) }
    }

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

    /**
     * Fuerza un re-escaneo completo de la biblioteca, ignorando el caché. Útil para actualizar
     * manualmente la biblioteca.
     */
    fun forceRescanSongs(): List<Song> {
        android.util.Log.d("SongRepository", "Iniciando forceRescanSongs...")
        val songs = scanSongsFromMediaStore()
        android.util.Log.d(
                "SongRepository",
                "Escaneo completado: ${songs.size} canciones encontradas"
        )
        if (songs.isNotEmpty()) {
            saveSongsToCache(songs)
            prefs.setFirstScanDone()
            android.util.Log.d("SongRepository", "Caché actualizado")
        } else {
            android.util.Log.w("SongRepository", "No se encontraron canciones en el escaneo")
        }
        return songs
    }

    private fun hasAudioPermission(): Boolean {
        val permission =
                if (Build.VERSION.SDK_INT >= 33) android.Manifest.permission.READ_MEDIA_AUDIO
                else android.Manifest.permission.READ_EXTERNAL_STORAGE

        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    // -------------------------
    // MediaStore (1ª vez)
    // -------------------------
    private fun scanSongsFromMediaStore(): List<Song> {
        val list = mutableListOf<Song>()
        var totalScanned = 0
        var excluded = 0

        val projection =
                arrayOf(
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.YEAR,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.DATA
                )

        val cursor =
                context.contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        MediaStore.Audio.Media.IS_MUSIC + "!= 0",
                        null,
                        null
                )
                        ?: return emptyList()

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val yearCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {
                totalScanned++
                val id = it.getLong(idCol)
                val uri =
                        Uri.withAppendedPath(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                id.toString()
                        )
                val filePath = it.getString(dataCol)
                val duration = it.getLong(durCol)

                // Excluir archivos de WhatsApp y otras apps de mensajería
                if (isExcludedPath(filePath)) {
                    excluded++
                    continue
                }

                // Excluir archivos muy cortos (notificaciones, efectos de sonido)
                // Duración mínima: 30 segundos (30000 ms)
                if (duration < 30000) {
                    excluded++
                    continue
                }

                // No cargar carátula durante el escaneo para mayor velocidad
                // Las carátulas se cargarán bajo demanda cuando se necesiten
                list.add(
                        Song(
                                id = id,
                                title = it.getString(titleCol),
                                artist = it.getString(artistCol),
                                album = it.getString(albumCol),
                                year = it.getInt(yearCol),
                                uri = uri,
                                duration = duration,
                                albumArt = null,
                                filePath = filePath
                        )
                )
            }
        }

        android.util.Log.d(
                "SongRepository",
                "Escaneo: Total=$totalScanned, Excluidos=$excluded, Agregados=${list.size}"
        )
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

            context.openFileOutput("songs_cache.json", Context.MODE_PRIVATE).use {
                it.write(json.toString().toByteArray())
            }
        } catch (e: Exception) {
            android.util.Log.e("SongRepository", "Error saving cache", e)
        }
    }

    private fun loadSongsFromCache(): List<Song> {
        try {
            val text =
                    context.openFileInput("songs_cache.json").bufferedReader().use { it.readText() }

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
        val cursor =
                context.contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        arrayOf(MediaStore.Audio.Media._ID),
                        MediaStore.Audio.Media.IS_MUSIC + "!= 0",
                        null,
                        null
                )
        return cursor?.count ?: 0
    }

    fun scanSongs(onProgress: (current: Int, total: Int) -> Unit): List<Song> {

        val projection =
                arrayOf(
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

        val cursor =
                context.contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        MediaStore.Audio.Media.IS_MUSIC + "!= 0",
                        null,
                        null
                )
                        ?: return emptyList()

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
                val uri =
                        Uri.withAppendedPath(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                id.toString()
                        )
                val filePath = it.getString(dataCol)
                val duration = it.getLong(durCol)

                // Excluir archivos de WhatsApp y otras apps de mensajería
                if (isExcludedPath(filePath)) {
                    continue
                }

                // Excluir archivos muy cortos (notificaciones, efectos de sonido)
                // Duración mínima: 30 segundos (30000 ms)
                if (duration < 30000) {
                    continue
                }

                val song =
                        Song(
                                id = id,
                                title = it.getString(titleCol),
                                artist = it.getString(artistCol),
                                album = it.getString(albumCol),
                                year = it.getInt(yearCol),
                                uri = uri,
                                duration = duration,
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
     * Variante del escaneo que notifica cada canción encontrada. Llama a [onSongFound] por cada
     * canción y a [onProgress] durante el proceso.
     */
    fun scanSongsStreaming(
            onSongFound: (Song) -> Unit,
            onProgress: (current: Int, total: Int) -> Unit
    ): List<Song> {
        val projection =
                arrayOf(
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

        val cursor =
                context.contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        MediaStore.Audio.Media.IS_MUSIC + "!= 0",
                        null,
                        null
                )
                        ?: return emptyList()

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
                val uri =
                        Uri.withAppendedPath(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                id.toString()
                        )
                val filePath = it.getString(dataCol)
                val duration = it.getLong(durCol)

                // Excluir archivos de WhatsApp y otras apps de mensajería
                if (isExcludedPath(filePath)) {
                    continue
                }

                // Excluir archivos muy cortos (notificaciones, efectos de sonido)
                // Duración mínima: 30 segundos (30000 ms)
                if (duration < 30000) {
                    continue
                }

                val song =
                        Song(
                                id = id,
                                title = it.getString(titleCol),
                                artist = it.getString(artistCol),
                                album = it.getString(albumCol),
                                year = it.getInt(yearCol),
                                uri = uri,
                                duration = duration,
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
