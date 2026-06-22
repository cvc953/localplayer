package com.cvc953.localplayer.util

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File

/**
 * Input for tag writing operations.
 * Nullable fields keep their existing value when null.
 */
data class TagWriteInput(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val year: String? = null,
    val trackNumber: String? = null,
    val discNumber: String? = null,
    val coverArt: ByteArray? = null,
    val coverMimeType: String? = "image/jpeg",
)

/**
 * Result of a tag write operation.
 */
data class TagWriteResult(
    val filePath: String,
    val success: Boolean,
    val error: String? = null,
)

/**
 * Writes audio file tags using jAudioTagger.
 *
 * Two write strategies:
 * - **Direct write** (API < 29): writes directly via [AudioFileIO.write].
 * - **SAF copy-write** (API >= 29): copies to temp, edits, writes back via
 *   [ContentResolver.openOutputStream] on the MediaStore URI.
 */
object TagWriter {

    private const val TAG = "TagWriter"

    /**
     * Write tags to an audio file.
     *
     * @param context Android context (for SAF temp files and ContentResolver).
     * @param filePath Absolute path to the audio file. May be null on API 29+.
     * @param uri Content URI from MediaStore (for SAF write-back on API 29+).
     * @param input Tag fields to write. Null fields are left unchanged.
     * @return Result with [TagWriteResult] on success, or an exception on failure.
     */
    fun writeTags(
        context: Context,
        filePath: String?,
        uri: android.net.Uri,
        input: TagWriteInput,
    ): Result<TagWriteResult> {
        return try {
            if (filePath == null && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return Result.failure(IllegalStateException("filePath is null on pre-Q device"))
            }

            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeViaSaf(context, filePath, uri, input)
            } else {
                writeDirect(File(filePath!!), input)
            }
            if (result.success) {
                Result.success(result)
            } else {
                Result.failure(Exception(result.error ?: "Tag write failed (no error)"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Tag write failed", e)
            Result.failure(e)
        }
    }

    private fun writeDirect(file: File, input: TagWriteInput): TagWriteResult {
        try {
            val audioFile = AudioFileIO.read(file)
            applyFields(audioFile.tag, input)
            applyCoverArt(audioFile, input)
            audioFile.commit()
            return TagWriteResult(filePath = file.absolutePath, success = true)
        } catch (e: Exception) {
            Log.e(TAG, "Direct write failed for ${file.name}", e)
            return TagWriteResult(filePath = file.absolutePath, success = false, error = e.message)
        }
    }

    private fun writeViaSaf(
        context: Context,
        filePath: String?,
        uri: android.net.Uri,
        input: TagWriteInput,
    ): TagWriteResult {
        val tempDir = File(context.cacheDir, "tag-writer-temp")
        tempDir.mkdirs()
        val displayPath = filePath ?: uri.toString()

        val tempFile = if (filePath != null) {
            File(tempDir, File(filePath).name)
        } else {
            File(tempDir, "temp_${System.nanoTime()}")
        }

        try {
            // 1. Copy to temp — usar ContentResolver en API 30+ (scoped storage)
            val useContentResolver = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || filePath == null
            if (useContentResolver) {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.e(TAG, "writeViaSaf: openInputStream returned null for $uri")
                    return TagWriteResult(
                        filePath = displayPath,
                        success = false,
                        error = "Cannot open file for reading",
                    )
                }
                inputStream.use { stream ->
                    tempFile.outputStream().use { outputStream ->
                        val bytes = stream.copyTo(outputStream)
                    }
                }
            } else {
                File(filePath!!).copyTo(tempFile, overwrite = true)
            }

            // 2. Edit temp file
            val audioFile = AudioFileIO.read(tempFile)
            applyFields(audioFile.tag, input)
            applyCoverArt(audioFile, input)
            audioFile.commit()

            // 3. Write back via ContentResolver
            val outputStream = context.contentResolver.openOutputStream(uri, "w")
            if (outputStream == null) {
                Log.e(TAG, "writeViaSaf: openOutputStream returned null for $uri")
                return TagWriteResult(
                    filePath = displayPath,
                    success = false,
                    error = "Cannot open output stream",
                )
            }
            outputStream.use { out ->
                tempFile.inputStream().use { input ->
                    val written = input.copyTo(out)
                }
            }

            return TagWriteResult(filePath = displayPath, success = true)
        } catch (e: Exception) {
            Log.e(TAG, "SAF write failed for $displayPath", e)
            val recoveryDir = File(context.cacheDir, "tag-writer-recovery")
            recoveryDir.mkdirs()
            tempFile.copyTo(File(recoveryDir, tempFile.name), overwrite = true)
            return TagWriteResult(filePath = displayPath, success = false, error = e.message)
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private fun applyFields(tag: Tag?, input: TagWriteInput) {
        if (tag == null) return
        input.title?.let { tag.setField(FieldKey.TITLE, it) }
        input.artist?.let { tag.setField(FieldKey.ARTIST, it) }
        input.album?.let { tag.setField(FieldKey.ALBUM, it) }
        input.genre?.let { tag.setField(FieldKey.GENRE, it) }
        input.year?.let { tag.setField(FieldKey.YEAR, it) }
        input.trackNumber?.let { tag.setField(FieldKey.TRACK, it) }
        input.discNumber?.let { tag.setField(FieldKey.DISC_NO, it) }
    }

    private fun applyCoverArt(audioFile: AudioFile, input: TagWriteInput) {
        val coverData = input.coverArt ?: return
        if (coverData.isEmpty()) return
        val tag = audioFile.tag ?: return

        // Compute image dimensions from the JPEG bytes (needed for FLAC picture block)
        val imgOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(coverData, 0, coverData.size, imgOpts)
        val imgWidth = imgOpts.outWidth.coerceAtLeast(0)
        val imgHeight = imgOpts.outHeight.coerceAtLeast(0)

        val artwork = ArtworkFactory.getNew().apply {
            binaryData = coverData
            mimeType = input.coverMimeType ?: "image/jpeg"
            pictureType = 3  // Front cover
            description = ""
            if (imgWidth > 0) {
                setWidth(imgWidth)
                setHeight(imgHeight)
            }
        }

        tag.deleteArtworkField()

        // Try normal path first — works for MP3 (ID3v2), crashes for FLAC on Android
        // because FlacTag.createField(Artwork) calls Artwork.setImageFromData()
        // which internally uses javax.imageio.ImageIO (not available on Android).
        try {
            tag.setField(artwork)
            return
        } catch (_: NoClassDefFoundError) {
        } catch (e: Exception) {
            Log.w(TAG, "applyCoverArt: setField failed", e)
        }

        if (tag !is FlacTag) {
            throw IllegalStateException("Cannot apply cover art to ${tag.javaClass.simpleName}")
        }

        // FlacTag.createArtworkField() creates a MetadataBlockDataPicture directly
        // WITHOUT calling setImageFromData() — no ImageIO dependency.
        val mimeType = input.coverMimeType ?: "image/jpeg"
        val imgField = tag.createArtworkField(
            coverData,       // imageData
            3,               // pictureType (3 = Front Cover)
            mimeType,        // mimeType
            "",              // description
            imgWidth,        // width
            imgHeight,       // height
            24,              // colourDepth (safe RGB default for JPEG)
            0,               // indexedColourCount
        )
        tag.setField(imgField)
    }
}
