package com.cvc953.localplayer.ui.screens

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.cvc953.localplayer.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.model.SongRepository
import com.cvc953.localplayer.ui.SongItem
import com.cvc953.localplayer.ui.components.DraggableSwipeRow
import com.cvc953.localplayer.ui.headers.ArtistHeader
import com.cvc953.localplayer.ui.theme.md_textSecondary
import com.cvc953.localplayer.viewmodel.ArtistViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("ktlint:standard:function-naming")
@Composable
fun ArtistDetailScreen(
    artistViewModel: ArtistViewModel,
    playbackViewModel: PlaybackViewModel,
    playlistViewModel: PlaylistViewModel,
    artistName: String,
    onAlbumClick: (albumName: String, artistName: String) -> Unit,
    onViewAllSongs: () -> Unit,
    onBack: () -> Unit,
) {
    val repo = remember { SongRepository(artistViewModel.getApplication<Application>()) }
    val allSongs = remember { repo.loadSongs() }
    val playerState by playbackViewModel.playerState.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()
    val artistSongs =
        remember(allSongs, artistName) {
            allSongs.filter { song ->
                normalizeArtistName(song.artist).any {
                    it.equals(
                        artistName,
                        ignoreCase = true,
                    )
                }
            }
        }
    val artistSongsSorted =
        remember(artistSongs) {
            artistSongs.sortedWith(
                compareBy(
                    { it.album },
                    { it.discNumber },
                    { it.trackNumber },
                ),
            )
        }
    val context = LocalContext.current

    BackHandler { onBack() }

    Column(
        modifier = Modifier.Companion.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier.Companion.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Companion.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.action_go_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(modifier = Modifier.Companion.width(8.dp))

            Column(modifier = Modifier.Companion.weight(1f)) {
                Text(
                    text = artistName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Companion.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Companion.Ellipsis,
                )
            }
        }

        val maxItems = 6

        LazyColumn(
            modifier = Modifier.Companion.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                ArtistHeader(
                    artistViewModel,
                    artistName,
                    playbackViewModel,
                    Modifier.Companion.padding(16.dp),
                    onViewAllSongs,
                )
            }
            items(artistSongsSorted.take(maxItems)) { song ->
                val isCurrent = playerState.currentSong?.id == song.id

                DraggableSwipeRow(
                    onSwipeThreshold = {
                        playbackViewModel.addToQueueNext(song)
                        Toast.makeText(context, context.getString(R.string.toast_added_next_count, 1), Toast.LENGTH_SHORT).show()
                    },
                    onSwipeLeftThreshold = {
                        playbackViewModel.addToQueueEnd(song)
                        Toast.makeText(context, context.getString(R.string.toast_added_queue_end_count, 1), Toast.LENGTH_SHORT).show()
                    },
                ) {
                    SongItem(
                        song = song,
                        isPlaying = isCurrent,
                        onClick = {
                            playbackViewModel.setShuffle(false)
                            playbackViewModel.playArtist(artistName, artistSongsSorted, allSongs)
                            playbackViewModel.updateDisplayOrder(artistSongsSorted)
                            playbackViewModel.play(song)
                        },
                        onQueueNext = {
                            playbackViewModel.addToQueueNext(song)
                            Toast
                                .makeText(context, context.getString(R.string.toast_added_next_count, 1), Toast.LENGTH_SHORT)
                                .show()
                        },
                        onQueueEnd = {
                            playbackViewModel.addToQueueEnd(song)
                            Toast
                                .makeText(
                                    context,
                                    "Añadido al final de la cola",
                                    Toast.LENGTH_SHORT,
                                ).show()
                        },
                        playlists = playlists,
                        onAddToPlaylist = { playlistName, songId ->
                            playlistViewModel.addSongToPlaylist(playlistName, songId)
                            Toast
                                .makeText(context, context.getString(R.string.toast_added_to_playlist, playlistName), Toast.LENGTH_SHORT)
                                .show()
                        },
                    )
                }
            }
            // LazyRow de álbumes del artista
            item {
                val albums =
                    remember(artistSongs) {
                        artistSongs
                            .groupBy { it.album.ifBlank { "Desconocido" } }
                            .filterKeys { it.isNotBlank() }
                            .toList()
                    }
                if (albums.isNotEmpty()) {
                    Column(modifier = Modifier.Companion.fillMaxWidth()) {
                        Text(
                            text = "Álbumes",
                            fontWeight = FontWeight.Companion.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier =
                                Modifier.Companion.padding(
                                    start = 8.dp,
                                    top = 8.dp,
                                    bottom = 8.dp,
                                ),
                        )
                        LazyRow(
                            // contentPadding = PaddingValues(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(albums) { (albumName, albumSongs) ->
                                val context = LocalContext.current
                                val representativeSong = albumSongs.firstOrNull()
                                var albumArt by remember(representativeSong?.uri) {
                                    mutableStateOf<Bitmap?>(
                                        null,
                                    )
                                }
                                LaunchedEffect(representativeSong?.uri) {
                                    withContext(Dispatchers.IO) {
                                        try {
                                            val uri = representativeSong?.uri ?: return@withContext
                                            val retriever = MediaMetadataRetriever()
                                            retriever.setDataSource(context, uri)
                                            retriever.embeddedPicture?.let {
                                                albumArt =
                                                    BitmapFactory.decodeByteArray(it, 0, it.size)
                                            }
                                            retriever.release()
                                        } catch (_: Exception) {
                                        }
                                    }
                                }
                                Column(
                                    modifier =
                                        Modifier.Companion
                                            .width(120.dp)
                                            .clickable { onAlbumClick(albumName, artistName) },
                                    horizontalAlignment = Alignment.Companion.CenterHorizontally,
                                ) {
                                    Image(
                                        painter =
                                            albumArt?.let { BitmapPainter(it.asImageBitmap()) }
                                                ?: painterResource(R.drawable.ic_default_album),
                                        contentDescription = null,
                                        modifier =
                                            Modifier.Companion
                                                .size(100.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Companion.Crop,
                                    )
                                    Spacer(modifier = Modifier.Companion.height(6.dp))
                                    Text(
                                        text = albumName,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        textAlign = TextAlign.Companion.Center,
                                        overflow = TextOverflow.Companion.Ellipsis,
                                    )
                                    Text(
                                        text = "${albumSongs.size} canciones",
                                        color = md_textSecondary,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
