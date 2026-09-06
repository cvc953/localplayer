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
import com.cvc953.localplayer.util.ArtworkLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** LRU cache for combined playlist album art, keyed by playlist song IDs hash. */
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
    var combinedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val cacheKey = remember(playlistSongIds, customImageUri) {
        if (customImageUri != null) customImageUri.hashCode() else playlistSongIds.hashCode()
    }

    LaunchedEffect(cacheKey) {
        // Check cache first
        synchronized(albumArtCache) {
            albumArtCache[cacheKey]?.let { cached ->
                combinedBitmap = cached
                return@LaunchedEffect
            }
        }

        val bitmap =
            if (customImageUri != null) {
                loadCustomImage(context, customImageUri)
            } else if (playlistSongIds.isEmpty()) {
                null
            } else {
                loadCombinedArt(context, playlistSongIds, songMap)
            }

        if (bitmap != null) {
            synchronized(albumArtCache) { albumArtCache[cacheKey] = bitmap }
        }
        combinedBitmap = bitmap
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

private suspend fun loadCustomImage(
    context: android.content.Context,
    uri: String,
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        ArtworkLoader.loadThumbnail(context, android.net.Uri.parse(uri), null, 256)
    } catch (_: Exception) {
        null
    }
}

private suspend fun loadCombinedArt(
    context: android.content.Context,
    playlistSongIds: List<Long>,
    songMap: Map<Long, Song>,
): Bitmap? = withContext(Dispatchers.IO) {
    val firstFourSongs =
        playlistSongIds.take(4).mapNotNull { id -> songMap[id] }

    val bitmaps = mutableListOf<Bitmap?>()
    for (song in firstFourSongs) {
        try {
            val bmp = ArtworkLoader.loadThumbnail(context, song.uri, song.filePath, 128)
            bitmaps.add(bmp)
        } catch (_: Exception) {
            bitmaps.add(null)
        }
    }
    while (bitmaps.size < firstFourSongs.size) {
        bitmaps.add(null)
    }

    if (bitmaps.isNotEmpty()) {
        createCombinedAlbumArt(bitmaps)
    } else {
        null
    }
}
