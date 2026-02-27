package com.cvc953.localplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.cvc953.localplayer.model.Playlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.controller.PlaylistController

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
}
