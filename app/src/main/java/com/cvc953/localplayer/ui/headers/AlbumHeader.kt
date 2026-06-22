package com.cvc953.localplayer.ui.headers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.R
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.extendedColors
import com.cvc953.localplayer.ui.screens.normalizeAlbumName
import com.cvc953.localplayer.ui.screens.normalizeArtistName
import com.cvc953.localplayer.viewmodel.AlbumViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("ktlint:standard:function-naming")
@Composable
fun AlbumHeader(
    viewModel: AlbumViewModel,
    albumName: String,
    artistName: String,
    playbackViewModel: PlaybackViewModel,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    val songs by viewModel.songs.collectAsState()
    val albumSongs =
        remember(songs, albumName, artistName) {
            val normalizedRequestedArtists = normalizeArtistName(artistName).map { it.trim() }
            songs
                .filter { song ->
                    val albumMatches = normalizeAlbumName(song.album).any { it.equals(albumName.trim(), ignoreCase = true) }
                    val artistMatches =
                        normalizeArtistName(song.artist).any { artist ->
                            normalizedRequestedArtists.any { requestedArtist ->
                                artist.trim().equals(requestedArtist, ignoreCase = true)
                            }
                        }
                    albumMatches && artistMatches
                }.sortedWith(compareBy<Song>({ it.discNumber }, { it.trackNumber }))
        }
    var albumArt by remember { mutableStateOf<Bitmap?>(null) }
    val firstSong = albumSongs.firstOrNull()
    val context = LocalContext.current

    LaunchedEffect(firstSong?.uri, firstSong?.filePath) {
        withContext(Dispatchers.IO) {
            try {
                val uri = firstSong?.uri ?: return@withContext
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val picture = retriever.embeddedPicture
                retriever.release()
                if (picture != null && picture.isNotEmpty()) {
                    albumArt = BitmapFactory.decodeByteArray(picture, 0, picture.size)
                }

                if (albumArt == null) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bmp = BitmapFactory.decodeStream(stream)
                            if (bmp != null) albumArt = bmp
                        }
                    } catch (_: Exception) {
                    }
                }

                if (albumArt == null) {
                    val path = firstSong.filePath
                    if (!path.isNullOrBlank()) {
                        try {
                            val dir = java.io.File(path).parentFile
                            val candidates = listOf("cover.jpg", "folder.jpg", "album.jpg", "front.jpg", "cover.png", "folder.png")
                            for (name in candidates) {
                                val f = java.io.File(dir, name)
                                if (f.exists() && f.length() > 0) {
                                    val bmp = BitmapFactory.decodeFile(f.absolutePath)
                                    if (bmp != null) {
                                        albumArt = bmp
                                        break
                                    }
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    val albumArtImage = @Composable {
        Image(
            painter = albumArt?.asImageBitmap()?.let { BitmapPainter(it) } ?: painterResource(R.drawable.ic_default_album),
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxWidth(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
    }

    val infoColumn = @Composable {
        Column {
            Text(
                text = albumName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            val totalDurationMs = remember(albumSongs) {
                var sum = 0L
                for (song in albumSongs) sum += song.duration
                sum
            }
            Text(
                text = buildString {
                    append(albumSongs.size)
                    append(" canciones")
                    if (totalDurationMs > 0) {
                        append(" • ")
                        val totalSec = totalDurationMs / 1000
                        val h = totalSec / 3600
                        val m = (totalSec % 3600) / 60
                        val s = totalSec % 60
                        if (h > 0) {
                            append("$h:")
                            append("%02d:%02d".format(m, s))
                        } else {
                            append("%02d:%02d".format(m, s))
                        }
                    }
                },
                fontSize = 16.sp,
                color = MaterialTheme.extendedColors.textSecondary,
            )

            Spacer(modifier = Modifier.height(20.dp))

            val buttonColor = MaterialTheme.colorScheme.primary

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val scope = rememberCoroutineScope()
                val playAnim = remember { Animatable(1f) }
                val shuffleAnim = remember { Animatable(1f) }

                Box(
                    modifier = Modifier.weight(1f).height(60.dp),
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                playAnim.snapTo(0.92f)
                                playAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                            }
                            if (albumSongs.isNotEmpty()) {
                                playbackViewModel.setShuffle(false)
                                playbackViewModel.playAlbum(albumName, artistName, albumSongs, songs)
                                playbackViewModel.updateDisplayOrder(albumSongs)
                                playbackViewModel.play(albumSongs.first())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = playAnim.value; scaleY = playAnim.value },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.action_play_now), tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(stringResource(R.string.action_play), color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).height(60.dp),
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                shuffleAnim.snapTo(0.92f)
                                shuffleAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                            }
                            if (albumSongs.isNotEmpty()) {
                                val shuffled = albumSongs.shuffled()
                                playbackViewModel.setShuffle(true)
                                playbackViewModel.playAlbum(albumName, artistName, shuffled, songs)
                                playbackViewModel.updateDisplayOrder(shuffled)
                                playbackViewModel.play(shuffled.first())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = shuffleAnim.value; scaleY = shuffleAnim.value },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = stringResource(R.string.action_shuffle), tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(stringResource(R.string.action_shuffle), color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    if (isLandscape) {
        Row(
            modifier = modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(modifier = Modifier.weight(0.4f)) { albumArtImage() }
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.weight(0.6f)) { infoColumn() }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth().padding(8.dp),
        ) {
            albumArtImage()
            Spacer(modifier = Modifier.height(8.dp))
            infoColumn()
        }
    }
}
