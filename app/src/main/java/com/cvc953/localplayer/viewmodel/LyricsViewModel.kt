package com.cvc953.localplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.controller.LyricsController
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.model.TtmlLyrics
import com.cvc953.localplayer.util.EmbeddedLyricsExtractor
import com.cvc953.localplayer.util.LrcLine
import com.cvc953.localplayer.util.isInstrumentalContent
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
    private val _isInstrumental = MutableStateFlow(false)
    val isInstrumental: StateFlow<Boolean> = _isInstrumental

    fun loadLyricsForSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _isInstrumental.value = false
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

                            try {
                                val text = ttmlFile.readText()
                                val parsed = com.cvc953.localplayer.util.TtmlParser.parseTtml(text)
                                if (parsed.lines.isNotEmpty()) {
                                    _ttmlLyrics.value = parsed
                                    _lyrics.value = parsed.lines.map { com.cvc953.localplayer.util.LrcLine(it.timeMs, it.text) }
                                    try {
                                        com.cvc953.localplayer.util.TtmlCache.saveCached(getApplication(), key, parsed)
                                    } catch (_: Exception) {}
                                    _isLoading.value = false
                                    return@launch
                                }
                            } catch (_: Exception) {
                            }
                        }

                        val lrcFile = File(audioDir, "$audioNameWithoutExt.lrc")
                        if (lrcFile.exists()) {
                            val text = lrcFile.readText()

                            // Detectar [Instrumental] en el texto crudo
                            _isInstrumental.value = isInstrumentalContent(text)

                            val (lrcLines, ttml) = LyricsController.parseLyrics(text)

                            // THE FIX: si no hay líneas con timestamp y no es instrumental → "sin letra"
                            val hasTimed = lrcLines.any { !it.isMetadata }
                            if (!hasTimed && !_isInstrumental.value) {
                                _ttmlLyrics.value = null
                                _lyrics.value = emptyList()
                                _isLoading.value = false
                                return@launch
                            }

                            _ttmlLyrics.value = ttml
                            _lyrics.value = lrcLines
                            _isLoading.value = false
                            return@launch
                        }
                    }
                }

                // 3. Si no hay archivos externos, probar letras embedidas en los tags del audio
                if (!audioFilePath.isNullOrEmpty()) {
                    val embedded = EmbeddedLyricsExtractor.extract(audioFilePath, song.duration)
                    if (embedded != null) {
                        val rawText = embedded.lrcLines.joinToString("\n") { it.text }
                        _isInstrumental.value = isInstrumentalContent(rawText)
                        _lyrics.value = embedded.lrcLines
                        _ttmlLyrics.value = embedded.ttmlLyrics
                        _isLoading.value = false
                        return@launch
                    }
                }

                _ttmlLyrics.value = null
                _lyrics.value = emptyList()
                _isInstrumental.value = false
                _error.value = "No se encontraron letras para la canción."
            } catch (e: Exception) {
                _ttmlLyrics.value = null
                _lyrics.value = emptyList()
                _isInstrumental.value = false
                _error.value = "Error cargando letras: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

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
        _isInstrumental.value = false
        _error.value = null
    }
}
