package com.cvc953.localplayer.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import com.cvc953.localplayer.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ArtistsScreen(viewModel: MainViewModel, onArtistClick: (String) -> Unit) {
    val songs by viewModel.songs.collectAsState()
    val isScanning by viewModel.isScanning
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(ArtistSortMode.TITLE_ASC) }

    val artists = remember(songs) { songs.groupBy { it.artist.ifBlank { "Desconocido" } }.toList() }

    val filteredArtists =
            remember(artists, searchQuery) {
                val q = searchQuery.trim().lowercase()
                if (q.isEmpty()) artists else artists.filter { it.first.lowercase().contains(q) }
            }

    val sortedArtists =
            remember(filteredArtists, sortMode) {
                when (sortMode) {
                    ArtistSortMode.TITLE_ASC -> filteredArtists.sortedBy { it.first.lowercase() }
                    ArtistSortMode.TITLE_DESC ->
                            filteredArtists.sortedByDescending { it.first.lowercase() }
                }
            }

    if (isScanning) {
        Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Escaneando canciones", color = Color.White)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = "Artistas",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Ordenar", tint = Color.White)
                    }
                    DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            containerColor = Color(0xFF1A1A1A)
                    ) {
                        DropdownMenuItem(
                                text = { Text("Título A-Z", color = Color.White) },
                                onClick = {
                                    sortMode = ArtistSortMode.TITLE_ASC
                                    sortMenuExpanded = false
                                }
                        )
                        DropdownMenuItem(
                                text = { Text("Título Z-A", color = Color.White) },
                                onClick = {
                                    sortMode = ArtistSortMode.TITLE_DESC
                                    sortMenuExpanded = false
                                }
                        )
                    }
                }

                IconButton(
                        onClick = {
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) searchQuery = ""
                        }
                ) { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.White) }
            }

            if (showSearchBar) {
                OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = { Text("Buscar por artista", color = Color(0xFF808080)) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors =
                                TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF1A1A1A),
                                        unfocusedContainerColor = Color(0xFF1A1A1A),
                                        focusedIndicatorColor = Color(0xFF2196F3),
                                        unfocusedIndicatorColor = Color(0xFF404040),
                                        cursorColor = Color(0xFF2196F3),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Color(0xFF2196F3),
                                        unfocusedLabelColor = Color(0xFF808080)
                                )
                )
            }

            LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                            PaddingValues(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedArtists) { (artistName, artistSongs) ->
                    val context = LocalContext.current
                    val mainAlbum =
                            remember(artistSongs) {
                                artistSongs
                                        .groupBy { it.album.ifBlank { "Desconocido" } }
                                        .maxByOrNull { it.value.size }
                            }
                    val representativeSong = mainAlbum?.value?.firstOrNull()
                    var albumArt by
                            remember(representativeSong?.uri) { mutableStateOf<Bitmap?>(null) }

                    LaunchedEffect(representativeSong?.uri) {
                        withContext(Dispatchers.IO) {
                            try {
                                val uri = representativeSong?.uri ?: return@withContext
                                val retriever = MediaMetadataRetriever()
                                retriever.setDataSource(context, uri)
                                retriever.embeddedPicture?.let {
                                    albumArt = BitmapFactory.decodeByteArray(it, 0, it.size)
                                }
                                retriever.release()
                            } catch (_: Exception) {}
                        }
                    }

                    Row(
                            modifier =
                                    Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable {
                                        onArtistClick(artistName)
                                    },
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
                                    text = artistName,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                    text = "${artistSongs.size} canciones",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class ArtistSortMode {
    TITLE_ASC,
    TITLE_DESC
}

@Composable
fun ArtistDetailScreen(viewModel: MainViewModel, artistName: String, onBack: () -> Unit) {
    val songs by viewModel.songs.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val artistSongs = remember(songs, artistName) { songs.filter { it.artist == artistName } }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = artistName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
                Text(text = "${artistSongs.size} canciones", color = Color.Gray, fontSize = 12.sp)
            }
        }

        LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                        PaddingValues(start = 16.dp, top = 8.dp, bottom = 16.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(artistSongs) { song ->
                val isCurrent = playerState.currentSong?.id == song.id
                SongItem(
                        song = song,
                        isPlaying = isCurrent,
                        onClick = {
                            // Usar el orden del artista como cola de reproduccion
                            viewModel.updateDisplayOrder(artistSongs)
                            viewModel.playSong(song)
                            viewModel.startService(context, song)
                        },
                        onQueueNext = { viewModel.addToQueueNext(song) },
                        onQueueEnd = { viewModel.addToQueueEnd(song) }
                )
            }
        }
    }
}
