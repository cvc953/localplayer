package com.cvc953.localplayer.controller

import android.content.Context
import com.cvc953.localplayer.model.Playlist
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class PlaylistController(private val context: Context) {
    private val fileName = "playlists.json"

    fun loadPlaylists(): List<Playlist> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()
        return try {
            val text = file.readText()
            val json = JSONArray(text)
            val list = mutableListOf<Playlist>()
            for (i in 0 until json.length()) {
                val o = json.getJSONObject(i)
                val name = o.getString("name")
                val songIds = o.optJSONArray("songIds")?.let { arr ->
                    List(arr.length()) { idx -> arr.getLong(idx) }
                } ?: emptyList()
                val imageUri = o.optString("imageUri", null)
                list.add(Playlist(name, songIds, imageUri))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePlaylists(playlists: List<Playlist>) {
        val file = File(context.filesDir, fileName)
        val json = JSONArray()
        playlists.forEach { playlist ->
            val obj = JSONObject()
            obj.put("name", playlist.name)
            obj.put("songIds", JSONArray(playlist.songIds))
            if (playlist.imageUri != null) obj.put("imageUri", playlist.imageUri)
            json.put(obj)
        }
        file.writeText(json.toString())
    }

    fun createPlaylist(playlists: List<Playlist>, name: String): List<Playlist> {
        if (playlists.any { it.name == name }) return playlists
        val updated = playlists + Playlist(name, emptyList())
        savePlaylists(updated)
        return updated
    }

    fun deletePlaylist(playlists: List<Playlist>, name: String): List<Playlist> {
        val updated = playlists.filterNot { it.name == name }
        savePlaylists(updated)
        return updated
    }

    fun renamePlaylist(playlists: List<Playlist>, oldName: String, newName: String): List<Playlist> {
        if (playlists.any { it.name == newName }) return playlists
        val updated = playlists.map {
            if (it.name == oldName) it.copy(name = newName) else it
        }
        savePlaylists(updated)
        return updated
    }

    fun addSongToPlaylist(playlists: List<Playlist>, playlistName: String, songId: Long): List<Playlist> {
        val updated = playlists.map {
            if (it.name == playlistName && !it.songIds.contains(songId))
                it.copy(songIds = it.songIds + songId)
            else it
        }
        savePlaylists(updated)
        return updated
    }

    fun removeSongFromPlaylist(playlists: List<Playlist>, playlistName: String, songId: Long): List<Playlist> {
        val updated = playlists.map {
            if (it.name == playlistName)
                it.copy(songIds = it.songIds - songId)
            else it
        }
        savePlaylists(updated)
        return updated
    }

    fun reorderPlaylistSongs(playlists: List<Playlist>, playlistName: String, newSongIds: List<Long>): List<Playlist> {
        val updated = playlists.map {
            if (it.name == playlistName) it.copy(songIds = newSongIds) else it
        }
        savePlaylists(updated)
        return updated
    }
}
