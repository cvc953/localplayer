package com.cvc953.localplayer.ui.screens

import android.app.Application
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.R
import com.cvc953.localplayer.model.SongRepository
import com.cvc953.localplayer.ui.SongItem
import com.cvc953.localplayer.ui.components.DraggableSwipeRow
import com.cvc953.localplayer.ui.components.NativeSearchBar
import com.cvc953.localplayer.viewmodel.ArtistViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.SongViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Suppress("ktlint:standard:function-naming")
@Composable
fun ArtistSongsScreen(
    artistViewModel: ArtistViewModel,
    playbackViewModel: PlaybackViewModel,
    songViewModel: SongViewModel = viewModel(),
    artistName: String,
    onBack: () -> Unit,
) {
    val repo = remember { SongRepository(artistViewModel.getApplication<Application>()) }
    val allSongs = remember { repo.loadSongs() }
    val artistSongs = allSongs.filter { song -> normalizeArtistName(song.artist).any { it.equals(artistName, ignoreCase = true) } }
    val context = LocalContext.current
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
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    val visibleSongs =
        remember(artistSongsSorted, searchQuery) {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) {
                artistSongsSorted
            } else {
                artistSongsSorted.filter { song ->
                    song.title.lowercase().contains(q) ||
                        song.album.lowercase().contains(q) ||
                        song.artist.lowercase().contains(q)
                }
            }
        }

    BackHandler { onBack() }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
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
            Text(
                text = artistName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    showSearchBar = !showSearchBar
                    if (!showSearchBar) searchQuery = ""
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.action_search),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        if (showSearchBar) {
            NativeSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = stringResource(R.string.search_songs_placeholder),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(visibleSongs) { song ->
                DraggableSwipeRow(
                    onSwipeThreshold = {
                        playbackViewModel.addToQueueNext(song)
                    },
                    onSwipeLeftThreshold = {
                        playbackViewModel.addToQueueEnd(song)
                    },
                ) {
                    SongItem(
                        song = song,
                        isPlaying = false,
                        onClick = {
                            playbackViewModel.setShuffle(false)
                            playbackViewModel.playArtist(artistName, artistSongsSorted, allSongs)
                            playbackViewModel.updateDisplayOrder(artistSongsSorted)
                            playbackViewModel.play(song)
                        },
                        onQueueNext = { playbackViewModel.addToQueueNext(song) },
                        onQueueEnd = { playbackViewModel.addToQueueEnd(song) },
                        playlists = emptyList(),
                        onAddToPlaylist = { _, _ -> },
                        onDelete = { song ->
                            songViewModel.deleteSong(
                                song,
                                onSuccess = {
                                    Toast.makeText(context, context.getString(R.string.toast_song_deleted), Toast.LENGTH_SHORT).show()
                                },
                                onError = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}
