package com.cvc953.localplayer.ui.headers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
    Column(
        modifier = modifier.fillMaxWidth().padding(8.dp),
    ) {
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
        Image(
            painter = albumArt?.asImageBitmap()?.let { BitmapPainter(it) } ?: painterResource(R.drawable.ic_default_album),
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxWidth(1f)
                    .aspectRatio(1f)
                    // .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = albumName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Botones igual que PlaylistHeader
        val buttonColor = MaterialTheme.colorScheme.primary

        Text(text = "${albumSongs.size} canciones", fontSize = 16.sp, color = MaterialTheme.extendedColors.textSecondary)
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(60.dp),
            ) {
                Button(
                    onClick = {
                        if (albumSongs.isNotEmpty()) {
                            playbackViewModel.setShuffle(false)
                            playbackViewModel.playAlbum(albumName, artistName, albumSongs, songs)
                            playbackViewModel.updateDisplayOrder(albumSongs)
                            playbackViewModel.play(albumSongs.first())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Reproducir ahora",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text("Reproducir", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(60.dp),
            ) {
                Button(
                    onClick = {
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
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Aleatorio", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("Aleatorio", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
