package com.cvc953.localplayer.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.cvc953.localplayer.controller.AlbumTrackResult
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

data class AlbumEditFormState(
    val album: String = "",
    val artist: String = "",
    val genre: String = "",
    val year: String = "",
    val albumError: String? = null,
    val yearError: String? = null,
)

class AlbumEditViewModel(
    application: Application,
    private val albumName: String,
    private val artistName: String,
) : AndroidViewModel(application) {

    private val songController = SongController(application)

    companion object {
        private const val TAG = "AlbumEditVM"
    }

    private val _formState = MutableStateFlow(AlbumEditFormState())
    val formState: StateFlow<AlbumEditFormState> = _formState.asStateFlow()

    private val _coverArtState = MutableStateFlow<CoverArtState>(CoverArtState())
    val coverArtState: StateFlow<CoverArtState> = _coverArtState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveComplete = MutableSharedFlow<Unit>()
    val saveComplete: SharedFlow<Unit> = _saveComplete.asSharedFlow()

    private val _saveError = MutableSharedFlow<String>()
    val saveError: SharedFlow<String> = _saveError.asSharedFlow()

    private val _trackResults = MutableSharedFlow<List<AlbumTrackResult>>()
    val trackResults: SharedFlow<List<AlbumTrackResult>> = _trackResults.asSharedFlow()

    private val _trackCount = MutableStateFlow(0)
    val trackCount: StateFlow<Int> = _trackCount.asStateFlow()

    private var pendingCoverArt: ByteArray? = null
    private var pendingCoverMimeType: String? = null
    private var songUris: List<android.net.Uri> = emptyList()
    private var userSelectedCover = false

    init {
        loadAlbum()
    }

    private fun loadAlbum() {
        viewModelScope.launch(Dispatchers.IO) {
            val songs = songController.getAllSongs().filter {
                it.album.equals(albumName, ignoreCase = true) &&
                    it.artist.equals(artistName, ignoreCase = true)
            }
            songUris = songs.map { it.uri }
            _trackCount.value = songs.size

            if (songs.isNotEmpty()) {
                val first = songs.first()
                _formState.value = AlbumEditFormState(
                    album = first.album,
                    artist = first.artist,
                    genre = first.genre,
                    year = first.year?.toString() ?: "",
                )
                loadExistingCover(first.filePath, first.uri.toString())
            }
        }
    }

    private fun loadExistingCover(filePath: String?, uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Si el usuario ya eligió una imagen, no pisar
                if (userSelectedCover) return@launch
                val retriever = android.media.MediaMetadataRetriever()
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
            "album" -> current.copy(album = value, albumError = null)
            "artist" -> current.copy(artist = value)
            "genre" -> current.copy(genre = value)
            "year" -> current.copy(year = value, yearError = null)
            else -> current
        }
        _formState.value = updated
    }

    fun getSongUris(): List<android.net.Uri> = songUris

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

                val (targetW, targetH) = if (options.outWidth > options.outHeight) {
                    1024 to (1024 * options.outHeight / options.outWidth)
                } else {
                    (1024 * options.outWidth / options.outHeight) to 1024
                }

                // Decode bitmap from the SAME bytes with sample size
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = computeSampleSize(options.outWidth, options.outHeight)
                }
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

        var hasError = false
        if (state.album.isBlank()) {
            _formState.value = state.copy(albumError = "Album name cannot be empty")
            hasError = true
        }
        if (state.year.isNotBlank() && state.year.toIntOrNull() == null) {
            _formState.value = _formState.value.copy(yearError = "Year must be a valid number")
            hasError = true
        }
        if (hasError) return

        _isSaving.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {

                val input = TagWriteInput(
                    album = state.album,
                    artist = state.artist,
                    genre = state.genre,
                    year = state.year.takeIf { it.isNotBlank() },
                    coverArt = pendingCoverArt,
                    coverMimeType = pendingCoverMimeType,
                )

                val results = songController.updateAlbumTags(albumName, artistName, input)
                _trackResults.emit(results)

                val allSuccess = results.all { it.success }
                if (allSuccess) {
                    _saveComplete.emit(Unit)
                } else {
                    val failed = results.count { !it.success }
                    _saveError.emit("$failed track(s) failed to update")
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
