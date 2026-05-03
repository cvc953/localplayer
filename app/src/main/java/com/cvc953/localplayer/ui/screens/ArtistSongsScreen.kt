package com.cvc953.localplayer.ui.screens

import android.app.Application
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.model.SongRepository
import com.cvc953.localplayer.ui.SongItem
import com.cvc953.localplayer.ui.components.DraggableSwipeRow
import com.cvc953.localplayer.viewmodel.ArtistViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel

@Suppress("ktlint:standard:function-naming")
@Composable
fun ArtistSongsScreen(
    artistViewModel: ArtistViewModel,
    playbackViewModel: PlaybackViewModel,
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
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(modifier = Modifier.Companion.width(8.dp))
            Text(
                text = artistName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Companion.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Companion.Ellipsis,
            )
        }
        LazyColumn(
            modifier = Modifier.Companion.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(artistSongs) { song ->
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
                    )
                }
            }
        }
    }
}
