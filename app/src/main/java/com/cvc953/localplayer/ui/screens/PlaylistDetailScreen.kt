package com.cvc953.localplayer.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.ui.SongItem
import com.cvc953.localplayer.ui.components.DraggableSwipeRow
import com.cvc953.localplayer.ui.extendedColors
import com.cvc953.localplayer.ui.headers.PlaylistHeader
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlaylistViewModel

@Suppress("ktlint:standard:function-naming")
@Composable
fun PlaylistDetailScreen(
    playlistViewModel: PlaylistViewModel,
    playbackViewModel: PlaybackViewModel,
    playlistName: String,
    onBack: () -> Unit,
) {
    val songs by playlistViewModel.songs.collectAsState()
    val playerState by playbackViewModel.playerState.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()
    val context = LocalContext.current
    val appPrefs =
        remember {
            com.cvc953.localplayer.preferences
                .AppPrefs(context)
        }

    BackHandler { onBack() }

    val playlist = remember(playlists, playlistName) { playlists.find { it.name == playlistName } }

    // Orden de la playlist: PLAYLIST, AZ, ZA
    var order by rememberSaveable(playlistName) { mutableStateOf(appPrefs.getPlaylistOrder(playlistName)) }
    LaunchedEffect(order) { appPrefs.setPlaylistOrder(playlistName, order) }

    val playlistSongs =
        remember(songs, playlist, order) {
            if (playlist != null) {
                val base = playlist.songIds.mapNotNull { id -> songs.find { it.id == id } }
                when (order) {
                    "AZ" -> base.sortedBy { it.title.lowercase() }
                    "ZA" -> base.sortedByDescending { it.title.lowercase() }
                    else -> base
                }
            } else {
                emptyList()
            }
        }

    val availableSongs =
        remember(songs, playlist) {
            if (playlist != null) {
                songs.filter { it.id !in playlist.songIds }
            } else {
                emptyList()
            }
        }

    // val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlistName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Dropdown para orden
            var sortMenuExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { sortMenuExpanded = true }) {
                    Icon(Icons.Default.Sort, contentDescription = "Ordenar", tint = MaterialTheme.colorScheme.onBackground)
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    containerColor = MaterialTheme.extendedColors.surfaceSheet,
                ) {
                    DropdownMenuItem(
                        text = { Text("Por playlist", color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            order = "PLAYLIST"
                            sortMenuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Título A-Z", color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            order = "AZ"
                            sortMenuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Título Z-A", color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            order = "ZA"
                            sortMenuExpanded = false
                        },
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                // Header igual que en AlbumDetailScreen
                PlaylistHeader(
                    playlist = playlist,
                    songs = songs,
                    context = context,
                    playlistSongs = playlistSongs,
                    playbackViewModel = playbackViewModel,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            items(playlistSongs) { song ->
                val isCurrent = playerState.currentSong?.id == song.id
                DraggableSwipeRow(onSwipeThreshold = {
                    playbackViewModel.addToQueueNext(song)
                    Toast.makeText(context, "Añadido como siguiente", Toast.LENGTH_SHORT).show()
                }) {
                    SongItem(
                        song = song,
                        isPlaying = isCurrent,
                        onClick = {
                            playbackViewModel.updateDisplayOrder(playlistSongs)
                            playbackViewModel.play(song)
                        },
                        onQueueNext = {
                            playbackViewModel.addToQueueNext(song)
                            Toast.makeText(context, "Añadido como siguiente", Toast.LENGTH_SHORT).show()
                        },
                        onQueueEnd = {
                            playbackViewModel.addToQueueEnd(song)
                            Toast.makeText(context, "Añadido al final de la cola", Toast.LENGTH_SHORT).show()
                        },
                        playlists = playlists,
                        onAddToPlaylist = { targetPlaylistName, songId ->
                            playlistViewModel.addSongToPlaylist(targetPlaylistName, songId)
                            Toast.makeText(context, "Añadido a $targetPlaylistName", Toast.LENGTH_SHORT).show()
                        },
                        onRemoveFromPlaylist = {
                            playlistViewModel.removeSongFromPlaylist(
                                playlistName,
                                song.id,
                            )
                            Toast.makeText(context, "Eliminado de la lista", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        }
    }
}
