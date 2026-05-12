package com.cvc953.localplayer.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cvc953.localplayer.R
import com.cvc953.localplayer.model.Playlist
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.screens.formatDuration
import com.cvc953.localplayer.ui.theme.ExtendedColors
import com.cvc953.localplayer.ui.theme.LocalExtendedColors
import com.cvc953.localplayer.viewmodel.PlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    get() = LocalExtendedColors.current

@Suppress("ktlint:standard:function-naming")
@Composable
fun SongItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onQueueNext: () -> Unit,
    onQueueEnd: () -> Unit,
    playlists: List<Playlist> = emptyList(),
    onAddToPlaylist: ((String, Long) -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    playlistViewModel: PlaylistViewModel = viewModel(),
) {
    val context = LocalContext.current
    var albumArt by remember { mutableStateOf<Bitmap?>(null) }

    // Cargar mensajes para Toasts y UI
    val removedFromFavoritesMsg = stringResource(R.string.removed_from_favorites)
    val addedToFavoritesMsg = stringResource(R.string.added_to_favorites)
    val actionCancelMsg = stringResource(R.string.action_cancel)

    LaunchedEffect(song.uri) {
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, song.uri)
                retriever.embeddedPicture?.let {
                    albumArt = BitmapFactory.decodeByteArray(it, 0, it.size)
                }
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .clickable { onClick() }
                .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter =
                albumArt?.let { BitmapPainter(it.asImageBitmap()) }
                    ?: painterResource(R.drawable.ic_default_album),
            contentDescription = null,
            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                color = MaterialTheme.extendedColors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(text = formatDuration(song.duration), color = MaterialTheme.extendedColors.texMeta, fontSize = 12.sp)

        var menuExpanded by remember { mutableStateOf(false) }
        var showPlaylistDialog by remember { mutableStateOf(false) }
        var showCreatePlaylistDialog by remember { mutableStateOf(false) }
        var newPlaylistName by remember { mutableStateOf("") }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.action_more_options),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = MaterialTheme.extendedColors.surfaceSheet,
                modifier = Modifier.background(MaterialTheme.extendedColors.surfaceSheet),
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_play_now), color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        menuExpanded = false
                        onClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_add_next), color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        menuExpanded = false
                        onQueueNext()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_add_to_queue_end), color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        menuExpanded = false
                        onQueueEnd()
                    },
                )
                if (onRemoveFromPlaylist != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_remove_from_playlist), color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            menuExpanded = false
                            onRemoveFromPlaylist()
                        },
                    )
                }
                if (playlists.isNotEmpty() && onAddToPlaylist != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_to_playlist), color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            menuExpanded = false
                            showPlaylistDialog = true
                        },
                    )
                }
                DropdownMenuItem(
                    text = {
                        val isFav = playlists.any { it.name == "Favoritos" && it.songIds.contains(song.id) }
                        Text(
                            text = if (isFav) stringResource(R.string.action_remove_from_favorites) else stringResource(R.string.action_add_to_favorites),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        val favoritesName = "Favoritos"
                        var favorites = playlists.find { it.name == favoritesName }
                        if (favorites == null) {
                            playlistViewModel.createPlaylist(favoritesName)
                            favorites = playlists.find { it.name == favoritesName }
                        }

                        val isFavorite = favorites?.songIds?.contains(song.id) == true
                        if (isFavorite) {
                            playlistViewModel.removeSongFromPlaylist(favoritesName, song.id)
                            Toast.makeText(context, removedFromFavoritesMsg, Toast.LENGTH_SHORT).show()
                        } else {
                            playlistViewModel.addSongToPlaylist(favoritesName, song.id)
                            Toast.makeText(context, addedToFavoritesMsg, Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
        }

        if (showPlaylistDialog && playlists.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showPlaylistDialog = false },
                containerColor = MaterialTheme.extendedColors.surfaceSheet,
                title = { Text(stringResource(R.string.add_to_playlist_title), color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                        ) {
                            Button(
                                onClick = { showCreatePlaylistDialog = true },
                                colors =
                                    androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                    ),
                            ) {
                                Text(stringResource(R.string.create_playlist_button))
                            }
                        }

                        Spacer(modifier = Modifier.width(0.dp).padding(vertical = 3.dp))

                        if (showCreatePlaylistDialog) {
                            AlertDialog(
                                onDismissRequest = {
                                    showCreatePlaylistDialog = false
                                    newPlaylistName = ""
                                },
                                containerColor = MaterialTheme.colorScheme.surface,
                                title = { Text(stringResource(R.string.dialog_create_playlist_title), color = MaterialTheme.colorScheme.onBackground) },
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
                                                playlistViewModel.createPlaylist(newPlaylistName)
                                                playlistViewModel.addSongToPlaylist(newPlaylistName, song.id)
                                                Toast.makeText(context, context.getString(R.string.playlist_created_and_added, newPlaylistName), Toast.LENGTH_SHORT).show()
                                                newPlaylistName = ""
                                                showCreatePlaylistDialog = false
                                                showPlaylistDialog = false
                                            }
                                        },
                                    ) {
                                        Text(stringResource(R.string.action_create), color = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showCreatePlaylistDialog = false }) {
                                        Text(actionCancelMsg, color = MaterialTheme.colorScheme.onBackground)
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
                                            .padding(vertical = 6.dp)
                                            .clickable {
                                                onAddToPlaylist?.invoke(playlist.name, song.id)
                                                Toast.makeText(context, context.getString(R.string.song_added_to_playlist, playlist.name), Toast.LENGTH_SHORT).show()
                                                showPlaylistDialog = false
                                            },
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor = LocalExtendedColors.current.surfaceSheet,
                                        ),
                                    shape = RoundedCornerShape(8.dp),
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
                                                color = LocalExtendedColors.current.textSecondarySoft,
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
                        Text(actionCancelMsg, color = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        }
    }
}
