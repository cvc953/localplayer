package com.cvc953.localplayer.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.cvc953.localplayer.R
import com.cvc953.localplayer.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("ktlint:standard:function-naming")
@Composable
fun PlaylistAlbumArt(
    playlistSongIds: List<Long>,
    songs: List<Song>,
    context: android.content.Context,
    modifier: Modifier = Modifier,
) {
    var combinedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(playlistSongIds) {
        if (playlistSongIds.isEmpty()) {
            combinedBitmap = null
            return@LaunchedEffect
        }

        val bitmaps = mutableListOf<Bitmap?>()
        val firstFourSongs =
            playlistSongIds.take(4).mapNotNull { songId -> songs.find { it.id == songId } }

        firstFourSongs.forEach { song ->
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, song.uri)
                retriever.embeddedPicture?.let {
                    var bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                    // Scale up the bitmap to improve quality
                    if (bitmap != null && (bitmap.width < 500 || bitmap.height < 500)) {
                        // Use bilinear filtering for better quality upscaling
                        bitmap = Bitmap.createScaledBitmap(bitmap, 1024, 1024, true)
                    }
                    bitmaps.add(bitmap)
                }
                retriever.release()
            } catch (_: Exception) {
                bitmaps.add(null)
            }
        }

        // Rellenar con nulls si hay menos de 4 canciones
        while (bitmaps.size < minOf(4, firstFourSongs.size)) {
            bitmaps.add(null)
        }

        if (bitmaps.isNotEmpty()) {
            withContext(Dispatchers.Default) { combinedBitmap = createCombinedAlbumArt(bitmaps) }
        }
    }

    if (combinedBitmap != null) {
        Image(
            painter = BitmapPainter(combinedBitmap!!.asImageBitmap()),
            contentDescription = "Carátula de la playlist",
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Image(
            painter = painterResource(R.drawable.ic_default_album),
            contentDescription = "Carátula por defecto",
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}
