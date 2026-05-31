package com.cvc953.localplayer.ui.components

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.cvc953.localplayer.model.Playlist
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.extendedColors

@Suppress("ktlint:standard:function-naming")
@Composable
fun MultiSongSelectionBar(
    selectedSongIds: Set<Long>,
    songs: List<Song>,
    playlists: List<Playlist>,
    onClearSelection: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onAddSongToPlaylist: (playlistName: String, songId: Long) -> Unit,
    onAddToQueueNextAll: (List<Song>) -> Unit,
    onAddToQueueEndAll: (List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selectedSongIds.isEmpty()) return

    val context = LocalContext.current
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClearSelection) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${selectedSongIds.size} ${stringResource(R.string.songs_selected)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { showPlaylistDialog = true }) {
                    Icon(
                        Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = stringResource(R.string.add_to_playlist),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(
                    onClick = {
                        val favoritesName = "Favoritos"
                        val favoritesExists = playlists.any { it.name == favoritesName }
                        if (!favoritesExists) onCreatePlaylist(favoritesName)
                        selectedSongIds.forEach { songId ->
                            onAddSongToPlaylist(favoritesName, songId)
                        }
                        Toast
                            .makeText(
                                context,
                                context.getString(R.string.toast_added_to_playlist, favoritesName),
                                Toast.LENGTH_SHORT,
                            ).show()
                        onClearSelection()
                    },
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = stringResource(R.string.action_add_to_favorites),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(
                    onClick = {
                        val songList = selectedSongIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }
                        if (songList.isNotEmpty()) {
                            onAddToQueueNextAll(songList)
                            Toast.makeText(context, context.getString(R.string.toast_added_next), Toast.LENGTH_SHORT).show()
                        }
                        onClearSelection()
                    },
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = stringResource(R.string.action_add_next),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(
                    onClick = {
                        val songList = selectedSongIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }
                        if (songList.isNotEmpty()) {
                            onAddToQueueEndAll(songList)
                            Toast
                                .makeText(
                                    context,
                                    context.getString(R.string.toast_added_queue_end),
                                    Toast.LENGTH_SHORT,
                                ).show()
                        }
                        onClearSelection()
                    },
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = stringResource(R.string.action_add_to_queue_end),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }

    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            containerColor = MaterialTheme.extendedColors.surfaceSheet,
            title = { Text(stringResource(R.string.add_to_playlist_title), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            onClick = { showCreatePlaylistDialog = true },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ),
                        ) {
                            Text(stringResource(R.string.create_playlist_button))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (showCreatePlaylistDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                showCreatePlaylistDialog = false
                                newPlaylistName = ""
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            title = {
                                Text(
                                    stringResource(R.string.dialog_create_playlist_title),
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            },
                            text = {
                                OutlinedTextField(
                                    value = newPlaylistName,
                                    onValueChange = { newPlaylistName = it },
                                    singleLine = true,
                                    placeholder = { Text(stringResource(R.string.playlist_name_placeholder)) },
                                    colors =
                                        TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                            unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                            cursorColor = MaterialTheme.colorScheme.primary,
                                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                        ),
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (newPlaylistName.isNotBlank()) {
                                            onCreatePlaylist(newPlaylistName)
                                            selectedSongIds.forEach { songId ->
                                                onAddSongToPlaylist(newPlaylistName, songId)
                                            }
                                            Toast
                                                .makeText(
                                                    context,
                                                    context.getString(R.string.playlist_created_and_added, newPlaylistName),
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            newPlaylistName = ""
                                            showCreatePlaylistDialog = false
                                            showPlaylistDialog = false
                                            onClearSelection()
                                        }
                                    },
                                ) {
                                    Text(stringResource(R.string.action_create), color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onBackground)
                                }
                            },
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(playlists) { playlist ->
                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.extendedColors.surfaceSheet,
                                    ),
                                shape = RoundedCornerShape(8.dp),
                                onClick = {
                                    selectedSongIds.forEach { songId ->
                                        onAddSongToPlaylist(playlist.name, songId)
                                    }
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(R.string.song_added_to_playlist, playlist.name),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    showPlaylistDialog = false
                                    onClearSelection()
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = playlist.name,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontSize = 14.sp,
                                        )
                                        Text(
                                            text = stringResource(R.string.songs_count, playlist.songIds.size),
                                            color = MaterialTheme.extendedColors.textSecondarySoft,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.primary)
                }
            },
        )
    }
}
