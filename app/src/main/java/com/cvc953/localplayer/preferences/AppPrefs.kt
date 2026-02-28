package com.cvc953.localplayer.preferences

import android.content.Context

class AppPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(
        "localplayer_prefs",
        Context.MODE_PRIVATE
    )

    fun isFirstScanDone(): Boolean =
        prefs.getBoolean("first_scan_done", false)

    fun setFirstScanDone() {
        prefs.edit().putBoolean("first_scan_done", true).apply()
    }

    fun getMusicFolderUri(): String? = prefs.getString("music_folder_uri", null)

    fun setMusicFolderUri(uri: String) {
        prefs.edit().putString("music_folder_uri", uri).apply()
        // keep single-value compatibility: also store in list
        val list = getMusicFolderUris().toMutableList()
        if (!list.contains(uri)) {
            list.add(0, uri)
            prefs.edit().putString("music_folder_uris", org.json.JSONArray(list).toString()).apply()
        }
    }

    fun hasMusicFolderUri(): Boolean = getMusicFolderUri() != null

    fun getMusicFolderUris(): List<String> {
        val raw = prefs.getString("music_folder_uris", null) ?: return getMusicFolderUri()?.let { listOf(it) } ?: emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) list.add(arr.getString(i))
            list
        } catch (_: Exception) {
            getMusicFolderUri()?.let { listOf(it) } ?: emptyList()
        }
    }

    fun addMusicFolder(uri: String) {
        val list = getMusicFolderUris().toMutableList()
        if (!list.contains(uri)) {
            list.add(uri)
            prefs.edit().putString("music_folder_uris", org.json.JSONArray(list).toString()).apply()
        }
        // keep single-value compatibility
        if (getMusicFolderUri() == null) prefs.edit().putString("music_folder_uri", uri).apply()
    }

    fun removeMusicFolder(uri: String) {
        val list = getMusicFolderUris().toMutableList()
        if (list.remove(uri)) {
            prefs.edit().putString("music_folder_uris", org.json.JSONArray(list).toString()).apply()
        }
        // if removed was the single value, clear it
        if (getMusicFolderUri() == uri) prefs.edit().remove("music_folder_uri").apply()
    }

    // Equalizer preferences
    fun isEqualizerEnabled(): Boolean = prefs.getBoolean("equalizer_enabled", false)

    fun setEqualizerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("equalizer_enabled", enabled).apply()
    }

    fun getEqualizerPresetIndex(): Int = prefs.getInt("equalizer_preset_index", -1)

    fun setEqualizerPresetIndex(index: Int) {
        prefs.edit().putInt("equalizer_preset_index", index).apply()
    }

    fun getCustomBandLevels(): List<Int> {
        val raw = prefs.getString("equalizer_custom_levels", null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            val list = mutableListOf<Int>()
            for (i in 0 until arr.length()) list.add(arr.optInt(i))
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setCustomBandLevels(levels: List<Int>) {
        prefs.edit().putString("equalizer_custom_levels", org.json.JSONArray(levels).toString()).apply()
    }

    // User-defined presets stored as array of objects {name: string, levels: [int...]}
    fun getUserPresets(): List<Pair<String, List<Int>>> {
        val raw = prefs.getString("equalizer_user_presets", null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            val list = mutableListOf<Pair<String, List<Int>>>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val name = obj.optString("name", "")
                val levelsArr = obj.optJSONArray("levels")
                val levels = mutableListOf<Int>()
                if (levelsArr != null) for (j in 0 until levelsArr.length()) levels.add(levelsArr.optInt(j))
                if (name.isNotEmpty()) list.add(Pair(name, levels))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setUserPresets(presets: List<Pair<String, List<Int>>>) {
        try {
            val arr = org.json.JSONArray()
            presets.forEach { (name, levels) ->
                val obj = org.json.JSONObject()
                obj.put("name", name)
                obj.put("levels", org.json.JSONArray(levels))
                arr.put(obj)
            }
            prefs.edit().putString("equalizer_user_presets", arr.toString()).apply()
        } catch (_: Exception) {
        }
    }

    fun addUserPreset(name: String, levels: List<Int>) {
        val list = getUserPresets().toMutableList()
        list.add(Pair(name, levels))
        setUserPresets(list)
    }

    fun removeUserPreset(name: String) {
        val list = getUserPresets().filterNot { it.first == name }
        setUserPresets(list)
    }

    // Auto-scan preference: whether app should scan automatically when folders change or on startup
    fun isAutoScanEnabled(): Boolean = prefs.getBoolean("auto_scan_enabled", true)

    fun setAutoScanEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_scan_enabled", enabled).apply()
    }

    // Theme preference: "system", "light", or "dark"
    fun getThemeMode(): String = prefs.getString("theme_mode", "sistema") ?: "sistema"

    fun setThemeMode(mode: String) {
        if (mode != "sistema" && mode != "claro" && mode != "oscuro") return
        prefs.edit().putString("theme_mode", mode).apply()
    }

    // Playback state persistence (queue, current song, position, shuffle, repeat, playing)
    fun savePlaybackQueue(uris: List<String>) {
        prefs.edit().putString("playback_queue", org.json.JSONArray(uris).toString()).apply()
    }

    fun loadPlaybackQueue(): List<String> {
        val raw = prefs.getString("playback_queue", null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) list.add(arr.getString(i))
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveLastSongUri(uri: String?) {
        if (uri == null) prefs.edit().remove("last_song_uri").apply() else prefs.edit().putString("last_song_uri", uri).apply()
    }

    fun loadLastSongUri(): String? = prefs.getString("last_song_uri", null)

    fun savePlaybackPosition(positionMs: Long) {
        prefs.edit().putLong("playback_position", positionMs).apply()
    }

    fun loadPlaybackPosition(): Long = prefs.getLong("playback_position", 0L)

    fun saveShuffleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("playback_shuffle", enabled).apply()
    }

    fun loadShuffleEnabled(): Boolean = prefs.getBoolean("playback_shuffle", false)

    fun saveRepeatMode(mode: String) {
        prefs.edit().putString("playback_repeat", mode).apply()
    }

    fun loadRepeatMode(): String? = prefs.getString("playback_repeat", null)

    fun saveIsPlaying(isPlaying: Boolean) {
        prefs.edit().putBoolean("playback_is_playing", isPlaying).apply()
    }

    fun loadIsPlaying(): Boolean = prefs.getBoolean("playback_is_playing", false)
}
