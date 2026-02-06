package com.cvc953.localplayer.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.cvc953.localplayer.model.Playlist
import com.cvc953.localplayer.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SongItem(
        song: Song,
        isPlaying: Boolean,
        onClick: () -> Unit,
        onQueueNext: () -> Unit,
        onQueueEnd: () -> Unit,
        playlists: List<Playlist> = emptyList(),
        onAddToPlaylist: ((String, Long) -> Unit)? = null
) {

    val context = LocalContext.current
    var albumArt by remember { mutableStateOf<Bitmap?>(null) } // aquí se guarda la carátula

    // Este bloque se ejecuta cada vez que el Composable se monta o cambia la canción
    LaunchedEffect(song.uri) {
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, song.uri) // URI de la canción
                retriever.embeddedPicture?.let {
                    albumArt = BitmapFactory.decodeByteArray(it, 0, it.size)
                }
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x00FFFFFF))
                            .clickable { onClick() }
                            .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
                painter = albumArt?.let { BitmapPainter(it.asImageBitmap()) }
                                ?: painterResource(R.drawable.ic_default_album),
                contentDescription = null,
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = song.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
            Text(
                    text = song.artist,
                    color = Color(0xFFCCCCCC),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
        }

        Text(text = formatDuration(song.duration), color = Color(0xFFCCCCCC), fontSize = 12.sp)

        var menuExpanded by remember { mutableStateOf(false) }
        var showPlaylistDialog by remember { mutableStateOf(false) }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Más opciones",
                        tint = Color.White
                )
            }
            DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = Color(0xFF1A1A1A),
                    modifier = Modifier.background(Color(0xFF1A1A1A))
            ) {
                DropdownMenuItem(
                        text = { Text("Reproducir ahora", color = Color.White) },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                )
                DropdownMenuItem(
                        text = { Text("Añadir como siguiente", color = Color.White) },
                        onClick = {
                            menuExpanded = false
                            onQueueNext()
                        }
                )
                DropdownMenuItem(
                        text = { Text("Añadir al final", color = Color.White) },
                        onClick = {
                            menuExpanded = false
                            onQueueEnd()
                        }
                )
                if (playlists.isNotEmpty() && onAddToPlaylist != null) {
                    DropdownMenuItem(
                            text = { Text("Agregar a playlist", color = Color.White) },
                            onClick = {
                                menuExpanded = false
                                showPlaylistDialog = true
                            }
                    )
                }
            }
        }

        if (showPlaylistDialog && playlists.isNotEmpty()) {
            AlertDialog(
                    onDismissRequest = { showPlaylistDialog = false },
                    containerColor = Color(0xFF1A1A1A),
                    title = { Text("Agregar a lista", color = Color.White) },
                    text = {
                        LazyColumn {
                            items(playlists) { playlist ->
                                Text(
                                        text = playlist.name,
                                        color = Color.White,
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .clickable {
                                                            onAddToPlaylist?.invoke(
                                                                    playlist.name,
                                                                    song.id
                                                            )
                                                            showPlaylistDialog = false
                                                        }
                                                        .padding(12.dp),
                                        fontSize = 14.sp
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPlaylistDialog = false }) {
                            Text("Cancelar", color = Color(0xFF2196F3))
                        }
                    }
            )
        }
    }
}
