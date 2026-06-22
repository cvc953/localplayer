package com.cvc953.localplayer.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.cvc953.localplayer.controller.SongController
import com.cvc953.localplayer.util.TagWriteInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

data class SongEditFormState(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val genre: String = "",
    val year: String = "",
    val trackNumber: String = "",
    val discNumber: String = "",
    val titleError: String? = null,
    val yearError: String? = null,
)

data class CoverArtState(
    val currentBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
)

class SongEditViewModel(
    application: Application,
    private val songId: Long,
) : AndroidViewModel(application) {

    private val songController = SongController(application)

    companion object {
        private const val TAG = "SongEditVM"
    }

    private val _formState = MutableStateFlow(SongEditFormState())
    val formState: StateFlow<SongEditFormState> = _formState.asStateFlow()

    private val _coverArtState = MutableStateFlow(CoverArtState())
    val coverArtState: StateFlow<CoverArtState> = _coverArtState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveComplete = MutableSharedFlow<Unit>()
    val saveComplete: SharedFlow<Unit> = _saveComplete.asSharedFlow()

    private val _saveError = MutableSharedFlow<String>()
    val saveError: SharedFlow<String> = _saveError.asSharedFlow()

    /** JPEG bytes of the selected cover art, null = keep existing */
    private var pendingCoverArt: ByteArray? = null
    private var pendingCoverMimeType: String? = null
    private var songUri: android.net.Uri? = null
    private var userSelectedCover = false

    init {
        loadSong()
    }

    private fun loadSong() {
        viewModelScope.launch(Dispatchers.IO) {
            val song = songController.getSongById(songId) ?: return@launch
            songUri = song.uri
            _formState.value = SongEditFormState(
                title = song.title,
                artist = song.artist,
                album = song.album,
                genre = song.genre,
                year = song.year?.toString() ?: "",
                trackNumber = song.trackNumber.toString(),
                discNumber = song.discNumber.toString(),
            )
            loadExistingCover(song.filePath, song.uri.toString())
        }
    }

    private fun loadExistingCover(filePath: String?, uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (userSelectedCover) return@launch
                val retriever = MediaMetadataRetriever()
                if (filePath != null) {
                    retriever.setDataSource(filePath)
                } else {
                    retriever.setDataSource(getApplication(), Uri.parse(uri))
                }
                val data = retriever.embeddedPicture
                retriever.release()
                if (data != null && !userSelectedCover) {
                    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                    _coverArtState.value = CoverArtState(currentBitmap = bitmap)
                }
            } catch (_: Exception) { }
        }
    }

    fun onFieldChanged(field: String, value: String) {
        val current = _formState.value
        val updated = when (field) {
            "title" -> current.copy(title = value, titleError = null)
            "artist" -> current.copy(artist = value)
            "album" -> current.copy(album = value)
            "genre" -> current.copy(genre = value)
            "year" -> current.copy(year = value, yearError = null)
            "trackNumber" -> current.copy(trackNumber = value)
            "discNumber" -> current.copy(discNumber = value)
            else -> current
        }
        _formState.value = updated
    }

    fun getSongUri(): android.net.Uri? = songUri

    fun onCoverSelected(uri: Uri) {
        userSelectedCover = true
        _coverArtState.value = _coverArtState.value.copy(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()

                // Leer el stream UNA SOLA vez a bytes
                val imageBytes = context.contentResolver.openInputStream(uri)
                    ?.use { it.readBytes() }
                    ?: throw Exception("Cannot open image")

                // Decode bounds from the bytes
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
                if (options.outWidth <= 0 || options.outHeight <= 0) {
                    throw Exception("Failed to decode image bounds")
                }

                // Compute sample size
                val (targetW, targetH) = if (options.outWidth > options.outHeight) {
                    1024 to (1024 * options.outHeight / options.outWidth)
                } else {
                    (1024 * options.outWidth / options.outHeight) to 1024
                }
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = computeSampleSize(options.outWidth, options.outHeight)
                }

                // Decode bitmap from the same bytes
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, decodeOptions)
                    ?: throw Exception("Failed to decode image")

                val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                if (bitmap != scaled) bitmap.recycle()

                val outputStream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                val bytes = outputStream.toByteArray()

                pendingCoverArt = bytes
                pendingCoverMimeType = context.contentResolver.getType(uri) ?: "image/jpeg"

                val config = scaled.config ?: Bitmap.Config.ARGB_8888
                val displayBitmap = scaled.copy(config, false)
                if (displayBitmap != scaled) scaled.recycle()

                _coverArtState.value = CoverArtState(
                    currentBitmap = displayBitmap,
                    isLoading = false,
                )
            } catch (e: Exception) {
                Log.e(TAG, "onCoverSelected failed", e)
                _coverArtState.value = _coverArtState.value.copy(isLoading = false)
                _saveError.tryEmit("Invalid image")
            }
        }
    }

    fun save() {
        val state = _formState.value

        // Validate
        var hasError = false
        var titleError: String? = null
        var yearError: String? = null

        if (state.title.isBlank()) {
            titleError = "Title cannot be empty"
            hasError = true
        }
        if (state.year.isNotBlank() && state.year.toIntOrNull() == null) {
            yearError = "Year must be a valid number"
            hasError = true
        }
        if (hasError) {
            _formState.value = state.copy(titleError = titleError, yearError = yearError)
            return
        }

        _isSaving.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {

                val input = TagWriteInput(
                    title = state.title,
                    artist = state.artist,
                    album = state.album,
                    genre = state.genre,
                    year = state.year,
                    trackNumber = state.trackNumber,
                    discNumber = state.discNumber,
                    coverArt = pendingCoverArt,
                    coverMimeType = pendingCoverMimeType,
                )

                val result = songController.updateSongTags(songId, input)
                if (result.isSuccess) {
                    _saveComplete.emit(Unit)
                } else {
                    _saveError.emit(result.exceptionOrNull()?.message ?: "Save failed")
                }
            } catch (e: Exception) {
                _saveError.emit(e.message ?: "Save failed")
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun computeSampleSize(outWidth: Int, outHeight: Int): Int {
        var sampleSize = 1
        while (outWidth / sampleSize > 2048 || outHeight / sampleSize > 2048) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
