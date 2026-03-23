package com.cvc953.localplayer.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.model.Playlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.controller.PlaylistController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {
    private val controller = PlaylistController(application)
    private val _playlists = MutableStateFlow<List<Playlist>>(controller.loadPlaylists())
    val playlists: StateFlow<List<Playlist>> = _playlists

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val songRepository = com.cvc953.localplayer.model.SongRepository(application)
    private val _songs = MutableStateFlow<List<Song>>(songRepository.loadSongs())
    val songs: StateFlow<List<Song>> = _songs

    fun addSongToPlaylist(playlistName: String, songId: Long) {
        _playlists.value = controller.addSongToPlaylist(_playlists.value, playlistName, songId)
    }

    fun addSongsToPlaylist(playlistName: String, songIds: List<Long>) {
        var current = _playlists.value
        songIds.forEach { id -> current = controller.addSongToPlaylist(current, playlistName, id) }
        _playlists.value = current
    }

    fun removeSongFromPlaylist(playlistName: String, songId: Long) {
        _playlists.value = controller.removeSongFromPlaylist(_playlists.value, playlistName, songId)
    }

    fun removeSongsFromPlaylist(playlistName: String, songIds: List<Long>) {
        var current = _playlists.value
        songIds.forEach { id -> current = controller.removeSongFromPlaylist(current, playlistName, id) }
        _playlists.value = current
    }

    fun createPlaylist(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (_playlists.value.any { it.name.equals(trimmed, ignoreCase = true) }) return false
        _playlists.value = controller.createPlaylist(_playlists.value, trimmed)
        return true
    }

    fun deletePlaylist(name: String) {
        _playlists.value = controller.deletePlaylist(_playlists.value, name)
    }

    fun renamePlaylist(oldName: String, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false
        if (_playlists.value.any { it.name.equals(trimmed, ignoreCase = true) }) return false
        _playlists.value = controller.renamePlaylist(_playlists.value, oldName, trimmed)
        return true
    }

    fun reloadPlaylists() {
        _isScanning.value = true
        _playlists.value = controller.loadPlaylists()
        _isScanning.value = false
    }

    fun getPlaylistsJson(): String {
        val playlists = _playlists.value
        val json = org.json.JSONArray()
        playlists.forEach { playlist ->
            val obj = org.json.JSONObject()
            obj.put("name", playlist.name)
            obj.put("songIds", org.json.JSONArray(playlist.songIds))
            json.put(obj)
        }
        return json.toString()
    }

    fun isSongInPlaylist(playlistName: String, songId: Long): Boolean {
        val p = _playlists.value.find { it.name == playlistName } ?: return false
        return p.songIds.contains(songId)
    }

    /**
     * Export all playlists as a JSON file into Music/localplayer. Returns true on success.
     * Call from a coroutine (e.g. viewModelScope.launch { val ok = exportPlaylistsToMusicFolder() })
     */
    suspend fun exportPlaylistsToMusicFolder(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val musicDir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                val targetDir = File(musicDir, "localplayer")
                if (!targetDir.exists()) targetDir.mkdirs()

                val file =
                    File(targetDir, "playlists_export_${System.currentTimeMillis()}.json")

                val array = org.json.JSONArray()
                _playlists.value.forEach { playlist ->
                    val idsArray = org.json.JSONArray()
                    playlist.songIds.forEach { idsArray.put(it) }
                    val obj = org.json.JSONObject()
                    obj.put("name", playlist.name)
                    obj.put("songIds", idsArray)
                    array.put(obj)
                }

                file.writeText(array.toString())
                true
            } catch (e: Exception) {
                android.util.Log.e("PlaylistViewModel", "Error exporting playlists", e)
                false
            }
        }

    /**
     * Import all JSON playlists found in Music/localplayer. Returns number of playlists imported.
     * Call from a coroutine (e.g. viewModelScope.launch { val n = importPlaylistsFromMusicFolder() })
     */
    suspend fun importPlaylistsFromMusicFolder(): Int =
        withContext(Dispatchers.IO) {
            try {
                val musicDir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                val targetDir = File(musicDir, "localplayer")
                if (!targetDir.exists() || !targetDir.isDirectory) return@withContext 0

                val files =
                    targetDir.listFiles { f ->
                        f.extension.equals("json", ignoreCase = true)
                    }
                        ?: return@withContext 0
                var imported = 0

                files.forEach { f ->
                    try {
                        val text = f.readText()
                        val array =
                            if (text.trimStart().startsWith("[")) {
                                org.json.JSONArray(text)
                            } else {
                                org.json.JSONArray().apply {
                                    put(org.json.JSONObject(text))
                                }
                            }
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val name = obj.optString("name", "").trim()
                            if (name.isEmpty()) continue
                            val idsArr = obj.optJSONArray("songIds") ?: org.json.JSONArray()
                            val ids = mutableListOf<Long>()
                            for (j in 0 until idsArr.length()) ids.add(idsArr.optLong(j))

                            // Skip if playlist with same name exists
                            if (_playlists.value.any { it.name.equals(name, ignoreCase = true) }) {
                                continue
                            }

                            _playlists.value = controller.createPlaylist(_playlists.value, name)
                            if (ids.isNotEmpty()) {
                                var current = _playlists.value
                                ids.forEach { id -> 
                                    current = controller.addSongToPlaylist(current, name, id)
                                }
                                _playlists.value = current
                            }
                            imported++
                        }
                    } catch (e: Exception) {
                        // ignore file parse errors
                        android.util.Log.w("PlaylistViewModel", "Error parsing playlist file: ${f.name}", e)
                    }
                }

                imported
            } catch (e: Exception) {
                android.util.Log.e("PlaylistViewModel", "Error importing playlists", e)
                0
            }
        }

    /**
     * Import playlists from provided JSON strings. Returns number imported.
     */
    suspend fun importPlaylistsFromJsonStrings(jsonStrings: List<String>): Int =
        withContext(Dispatchers.IO) {
            try {
                var imported = 0
                jsonStrings.forEach { text ->
                    try {
                        val array =
                            if (text.trimStart().startsWith("[")) {
                                org.json.JSONArray(text)
                            } else {
                                org.json.JSONArray().apply {
                                    put(org.json.JSONObject(text))
                                }
                            }
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val name = obj.optString("name", "").trim()
                            if (name.isEmpty()) continue
                            val idsArr = obj.optJSONArray("songIds") ?: org.json.JSONArray()
                            val ids = mutableListOf<Long>()
                            for (j in 0 until idsArr.length()) ids.add(idsArr.optLong(j))

                            if (_playlists.value.any { it.name.equals(name, ignoreCase = true) }) continue

                            _playlists.value = controller.createPlaylist(_playlists.value, name)
                            if (ids.isNotEmpty()) {
                                var current = _playlists.value
                                ids.forEach { id -> 
                                    current = controller.addSongToPlaylist(current, name, id)
                                }
                                _playlists.value = current
                            }
                            imported++
                        }
                    } catch (e: Exception) {
                        // ignore parse errors per-string
                        android.util.Log.w("PlaylistViewModel", "Error parsing JSON string", e)
                    }
                }
                imported
            } catch (e: Exception) {
                android.util.Log.e("PlaylistViewModel", "Error importing playlists from strings", e)
                0
            }
        }
}
