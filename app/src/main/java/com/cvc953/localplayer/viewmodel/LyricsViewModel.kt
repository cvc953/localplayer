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
                            val text = ttmlFile.readText()
                            val (lrcLines, ttml) = LyricsController.parseLyrics(text)
                            _ttmlLyrics.value = ttml
                            _lyrics.value = lrcLines
                            _isLoading.value = false
                            return@launch
                        }
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

    fun clearLyrics() {
        _ttmlLyrics.value = null
        _lyrics.value = emptyList()
        _error.value = null
    }
}
