package com.cvc953.localplayer.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas

fun createCombinedAlbumArt(
    bitmaps: List<Bitmap?>,
    size: Int = 1024,
): Bitmap {
    val canvas = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvasDrawer = Canvas(canvas)
    val count = minOf(4, bitmaps.size)
    if (count == 0) {
        return canvas
    }

    val halfSize = size / 2

    when (count) {
        1 -> {
            val bitmap = bitmaps[0] ?: return canvas
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true)
            canvasDrawer.drawBitmap(scaledBitmap, 0f, 0f, null)
        }

        2 -> {
            for (i in 0 until 2) {
                val bitmap = bitmaps[i] ?: continue
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, halfSize, size, true)
                val x = i * halfSize
                canvasDrawer.drawBitmap(scaledBitmap, x.toFloat(), 0f, null)
            }
        }

        3 -> {
            val leftBitmap = bitmaps[0]
            if (leftBitmap != null) {
                val scaledLeft = Bitmap.createScaledBitmap(leftBitmap, halfSize, size, true)
                canvasDrawer.drawBitmap(scaledLeft, 0f, 0f, null)
            }
            for (i in 1 until 3) {
                val bitmap = bitmaps[i] ?: continue
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, halfSize, halfSize, true)
                val x = halfSize
                val y = (i - 1) * halfSize
                canvasDrawer.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), null)
            }
        }

        else -> {
            for (i in 0 until 4) {
                val bitmap = bitmaps[i] ?: continue
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, halfSize, halfSize, true)
                val x = (i % 2) * halfSize
                val y = (i / 2) * halfSize
                canvasDrawer.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), null)
            }
        }
    }

    return canvas
}
