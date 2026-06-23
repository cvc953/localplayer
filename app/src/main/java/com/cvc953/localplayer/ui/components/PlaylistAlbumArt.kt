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

/** Simple LRU-like cache for playlist album art, keyed by playlist song IDs. */
private val albumArtCache =
    object : LinkedHashMap<Int, Bitmap>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Bitmap>): Boolean =
            size > 48
    }

@Suppress("ktlint:standard:function-naming")
@Composable
fun PlaylistAlbumArt(
    playlistSongIds: List<Long>,
    songMap: Map<Long, Song>,
    context: android.content.Context,
    modifier: Modifier = Modifier,
    customImageUri: String? = null,
) {
    var albumBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val cacheKey = remember(playlistSongIds, customImageUri) {
        if (customImageUri != null) customImageUri.hashCode() else playlistSongIds.hashCode()
    }

    LaunchedEffect(cacheKey) {
        // Check cache first
        synchronized(albumArtCache) {
            albumArtCache[cacheKey]?.let { cached ->
                albumBitmap = cached
                return@LaunchedEffect
            }
        }

        val bitmap =
            if (customImageUri != null) {
                loadCustomImage(context, customImageUri)
            } else if (playlistSongIds.isEmpty()) {
                null
            } else {
                loadFirstSongArt(context, playlistSongIds, songMap)
            }

        if (bitmap != null) {
            synchronized(albumArtCache) { albumArtCache[cacheKey] = bitmap }
        }
        albumBitmap = bitmap
    }

    if (albumBitmap != null) {
        Image(
            painter = BitmapPainter(albumBitmap!!.asImageBitmap()),
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

private suspend fun loadCustomImage(
    context: android.content.Context,
    uri: String,
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(android.net.Uri.parse(uri))
        inputStream?.use { BitmapFactory.decodeStream(it) }
    } catch (_: Exception) {
        null
    }
}

private suspend fun loadFirstSongArt(
    context: android.content.Context,
    playlistSongIds: List<Long>,
    songMap: Map<Long, Song>,
): Bitmap? = withContext(Dispatchers.IO) {
    val firstSong =
        playlistSongIds.firstNotNullOfOrNull { id -> songMap[id] }
            ?: return@withContext null

    try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, firstSong.uri)
        val data = retriever.embeddedPicture
        retriever.release()
        if (data != null) {
            BitmapFactory.decodeByteArray(data, 0, data.size)
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}
