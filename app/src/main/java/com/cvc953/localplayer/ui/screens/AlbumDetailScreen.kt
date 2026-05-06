package com.cvc953.localplayer.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.cvc953.localplayer.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.SongItem
import com.cvc953.localplayer.ui.components.DraggableSwipeRow
import com.cvc953.localplayer.ui.headers.AlbumHeader
import com.cvc953.localplayer.viewmodel.AlbumViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlaylistViewModel

@Suppress("ktlint:standard:function-naming")
@Composable
fun AlbumDetailScreen(
    albumViewModel: AlbumViewModel,
    playbackViewModel: PlaybackViewModel,
    playlistViewModel: PlaylistViewModel,
    albumName: String,
    artistName: String,
    onBack: () -> Unit,
) {
    val songs by albumViewModel.songs.collectAsState()
    val playerState by playbackViewModel.playerState.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()

    // Cargar canciones cuando el componente se compone
    LaunchedEffect(albumName, artistName) {
        albumViewModel.loadSongsForAlbumByName(albumName, artistName)
    }

    // Filtrar canciones del álbum donde el artista participa (en cualquier posición)
    // Nota: Aunque loadSongsForAlbumByName ya filtra, mantenemos este filtro por si acaso o para ordenar
    val albumSongs =
        remember(songs, albumName, artistName) {
            val normalizedRequestedArtists = normalizeArtistName(artistName).map { it.trim() }
            songs
                .filter { song ->
                    val albumMatches =
                        normalizeAlbumName(song.album).any {
                            it.equals(
                                albumName.trim(),
                                ignoreCase = true,
                            )
                        }
                    val artistMatches =
                        normalizeArtistName(song.artist).any { artist ->
                            normalizedRequestedArtists.any { requestedArtist ->
                                artist.trim().equals(requestedArtist, ignoreCase = true)
                            }
                        }
                    albumMatches && artistMatches
                }.sortedWith(compareBy<Song>({ it.discNumber }, { it.trackNumber }))
        }
    val context = LocalContext.current

    BackHandler { onBack() }

    Column(
        modifier = Modifier.Companion.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier =
                Modifier.Companion
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.Companion.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_go_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(modifier = Modifier.Companion.width(8.dp))

            Column(modifier = Modifier.Companion.weight(1f)) {
                Text(
                    text = albumName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Companion.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Companion.Ellipsis,
                    textAlign = TextAlign.Companion.Left,
                )
                // Text(text = "${albumSongs.size} canciones", color = MaterialTheme.extendedColors.texMeta, fontSize = 12.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.Companion.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                AlbumHeader(
                    albumViewModel,
                    albumName,
                    artistName,
                    playbackViewModel,
                    Modifier.Companion.padding(16.dp),
                )
            }
            items(albumSongs) { song ->
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
                            // Usar el orden del album como cola de reproduccion
                            playbackViewModel.setShuffle(false)
                            playbackViewModel.playAlbum(albumName, artistName, albumSongs, songs)
                            playbackViewModel.updateDisplayOrder(albumSongs)
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
                                            context.getString(R.string.toast_added_queue_end),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                        },
                        playlists = playlists,
                        onAddToPlaylist = { playlistName, songId ->
                            playlistViewModel.addSongToPlaylist(playlistName, songId)
                            Toast
                                .makeText(
                                    context,
                                    "Añadido a playlist '$playlistName'",
                                    Toast.LENGTH_SHORT,
                                ).show()
                        },
                    )
                }
            }
        }
    }
}
