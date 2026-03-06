package com.cvc953.localplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.LyricsController
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.model.TtmlLyrics
import com.cvc953.localplayer.util.LrcLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class LyricsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _lyrics = MutableStateFlow<List<LrcLine>>(emptyList())
    val lyrics: StateFlow<List<LrcLine>> = _lyrics
    private val _ttmlLyrics = MutableStateFlow<TtmlLyrics?>(null)
    val ttmlLyrics: StateFlow<TtmlLyrics?> = _ttmlLyrics
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadLyricsForSong(song: Song) {
        // Only load from cache to avoid expensive file IO / parsing on UI open.
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                var audioFilePath = song.filePath
                if (audioFilePath.isNullOrEmpty()) {
                    val resolver = getApplication<Application>().contentResolver
                    val projection = arrayOf(android.provider.MediaStore.Audio.Media.DATA)
                    resolver.query(song.uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val dataCol = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA)
                            if (dataCol >= 0) {
                                audioFilePath = cursor.getString(dataCol)
                            }
                        }
                    }
                }

                if (!audioFilePath.isNullOrEmpty()) {
                    val audioFile = File(audioFilePath)
                    val audioDir = audioFile.parentFile
                    val audioNameWithoutExt = audioFile.nameWithoutExtension
                    if (audioDir != null && audioDir.exists()) {
                        val ttmlFile = File(audioDir, "$audioNameWithoutExt.ttml")
                        if (ttmlFile.exists()) {
                            val key = com.cvc953.localplayer.util.TtmlCache.keyForFile(ttmlFile.absolutePath, ttmlFile.lastModified())
                            val cached = com.cvc953.localplayer.util.TtmlCache.loadCached(getApplication(), key)
                            if (cached != null && cached.lines.isNotEmpty()) {
                                val lrcLines = cached.lines.map { com.cvc953.localplayer.util.LrcLine(it.timeMs, it.text) }
                                _ttmlLyrics.value = cached
                                _lyrics.value = lrcLines
                                _isLoading.value = false
                                return@launch
                            }

                            // If there's a TTML file but no cache, parse it now (still on IO dispatcher)
                            try {
                                val text = ttmlFile.readText()
                                val parsed = com.cvc953.localplayer.util.TtmlParser.parseTtml(text)
                                
                                // Solo usar TTML si tiene líneas
                                if (parsed.lines.isNotEmpty()) {
                                    _ttmlLyrics.value = parsed
                                    _lyrics.value = parsed.lines.map { com.cvc953.localplayer.util.LrcLine(it.timeMs, it.text) }
                                    try {
                                        com.cvc953.localplayer.util.TtmlCache.saveCached(getApplication(), key, parsed)
                                    } catch (_: Exception) {}
                                    _isLoading.value = false
                                    return@launch
                                } else {
                                    android.util.Log.w("LyricsDebug", "TTML parseado pero vacío, intentando con LRC")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("LyricsDebug", "Error parseando TTML, intentando con LRC como fallback: ${e.message}")
                            }
                        }
                        // Si no hay TTML o falló el parsing, intentar con LRC
                        val lrcFile = File(audioDir, "$audioNameWithoutExt.lrc")
                        if (lrcFile.exists()) {
                            val text = lrcFile.readText()
                            val (lrcLines, ttml) = LyricsController.parseLyrics(text)
                            _ttmlLyrics.value = ttml
                            _lyrics.value = lrcLines
                            _isLoading.value = false
                            return@launch
                        }
                    }
                }

                _ttmlLyrics.value = null
                _lyrics.value = emptyList()
                _error.value = "No se encontraron letras para la canción."
            } catch (e: Exception) {
                _ttmlLyrics.value = null
                _lyrics.value = emptyList()
                _error.value = "Error cargando letras: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Prefetch TTML parsing and save to disk cache. Heavy IO and parsing; call on song start.
     */
    fun prefetchTtml(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var audioFilePath = song.filePath
                if (audioFilePath.isNullOrEmpty()) {
                    val resolver = getApplication<Application>().contentResolver
                    val projection = arrayOf(android.provider.MediaStore.Audio.Media.DATA)
                    resolver.query(song.uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val dataCol = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA)
                            if (dataCol >= 0) {
                                audioFilePath = cursor.getString(dataCol)
                            }
                        }
                    }
                }

                if (audioFilePath.isNullOrEmpty()) return@launch

                val audioFile = File(audioFilePath)
                val audioDir = audioFile.parentFile ?: return@launch
                val audioNameWithoutExt = audioFile.nameWithoutExtension

                // Try explicit .ttml next to audio
                val ttmlFile = File(audioDir, "$audioNameWithoutExt.ttml")
                if (ttmlFile.exists()) {
                    try {
                        val text = ttmlFile.readText()
                        val parsed = com.cvc953.localplayer.util.TtmlParser.parseTtml(text)
                        val key = com.cvc953.localplayer.util.TtmlCache.keyForFile(ttmlFile.absolutePath, ttmlFile.lastModified())
                        com.cvc953.localplayer.util.TtmlCache.saveCached(getApplication(), key, parsed)
                        return@launch
                    } catch (_: Exception) {
                    }
                }

                // Otherwise scan directory for matching TTML file
                val ttmlFiles = audioDir.listFiles { _, name -> name.endsWith(".ttml", ignoreCase = true) } ?: return@launch
                ttmlFiles.forEach { file ->
                    val ttmlNameWithoutExt = file.nameWithoutExtension
                    val audioClean = audioNameWithoutExt.replace(Regex("[:\\\\/*?\"<>|]"), "").lowercase().trim()
                    val ttmlClean = ttmlNameWithoutExt.replace(Regex("[:\\\\/*?\"<>|]"), "").lowercase().trim()
                    if (audioClean == ttmlClean) {
                        try {
                            val text = file.readText()
                            val parsed = com.cvc953.localplayer.util.TtmlParser.parseTtml(text)
                            val key = com.cvc953.localplayer.util.TtmlCache.keyForFile(file.absolutePath, file.lastModified())
                            com.cvc953.localplayer.util.TtmlCache.saveCached(getApplication(), key, parsed)
                            return@launch
                        } catch (_: Exception) {
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun clearLyrics() {
        _ttmlLyrics.value = null
        _lyrics.value = emptyList()
        _error.value = null
    }
}
