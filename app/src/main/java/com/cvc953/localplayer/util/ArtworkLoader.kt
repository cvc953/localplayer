package com.cvc953.localplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File

/**
 * Utility object for safely decoding and loading album artwork with proper downsampling
 * and exception handling to prevent OutOfMemoryErrors and native resource leaks.
 */
object ArtworkLoader {

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun decodeSampledBitmapFromByteArray(data: ByteArray, targetSizePx: Int): Bitmap? {
        if (data.isEmpty()) return null
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            options.inSampleSize = calculateInSampleSize(options, targetSizePx, targetSizePx)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
        } catch (_: OutOfMemoryError) {
            try {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 4
                }
                BitmapFactory.decodeByteArray(data, 0, data.size, options)
            } catch (_: Throwable) {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun decodeSampledBitmapFromFile(filePath: String, targetSizePx: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            options.inSampleSize = calculateInSampleSize(options, targetSizePx, targetSizePx)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(filePath, options)
        } catch (_: OutOfMemoryError) {
            try {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 4
                }
                BitmapFactory.decodeFile(filePath, options)
            } catch (_: Throwable) {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun loadBitmapFromUri(context: Context, uri: Uri, targetSizePx: Int = 256): Bitmap? {
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val embedded = retriever.embeddedPicture
            if (embedded != null && embedded.isNotEmpty()) {
                decodeSampledBitmapFromByteArray(embedded, targetSizePx)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever?.release()
            } catch (_: Exception) {
            }
        }
    }

    fun loadArtworkForSong(
        context: Context,
        songUri: Uri?,
        filePath: String?,
        targetSizePx: Int = 256,
    ): Bitmap? {
        if (songUri != null) {
            val bitmap = loadBitmapFromUri(context, songUri, targetSizePx)
            if (bitmap != null) return bitmap
        }
        if (!filePath.isNullOrBlank()) {
            try {
                val file = File(filePath)
                val dir = file.parentFile
                if (dir != null && dir.exists() && dir.isDirectory) {
                    val candidates = listOf("cover.jpg", "folder.jpg", "album.jpg", "front.jpg", "cover.png", "folder.png")
                    for (name in candidates) {
                        val candidate = File(dir, name)
                        if (candidate.exists() && candidate.length() > 0) {
                            val bmp = decodeSampledBitmapFromFile(candidate.absolutePath, targetSizePx)
                            if (bmp != null) return bmp
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
        return null
    }
}
