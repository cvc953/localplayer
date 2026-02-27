@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.cvc953.localplayer.ui

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.R
import com.cvc953.localplayer.model.Playlist
import com.cvc953.localplayer.model.SongRepository
import com.cvc953.localplayer.ui.theme.md_textSecondary
import com.cvc953.localplayer.viewmodel.ArtistViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("ktlint:standard:function-naming")
@Composable
fun ArtistsScreen(
    artistViewModel: ArtistViewModel,
    playbackViewModel: PlaybackViewModel,
    onArtistClick: (String) -> Unit,
) {
    val artists by artistViewModel.artists.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(ArtistSortMode.TITLE_ASC) }
    var viewAsGrid by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current
    val activity = context as? Activity
    var lastBackPressTime by remember { mutableStateOf(0L) }

    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 1000) {
            activity?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, "Presiona de nuevo para salir", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredArtists =
        remember(artists, searchQuery) {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) artists else artists.filter { it.name.lowercase().contains(q) }
        }

    val sortedArtists =
        remember(filteredArtists, sortMode) {
            when (sortMode) {
                ArtistSortMode.TITLE_ASC -> filteredArtists.sortedBy { it.name.lowercase() }
                ArtistSortMode.TITLE_DESC -> filteredArtists.sortedByDescending { it.name.lowercase() }
            }
        }

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var currentScrollLetter by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Artistas",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )

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
                            text = { Text("Título A-Z", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                sortMode = ArtistSortMode.TITLE_ASC
                                sortMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Título Z-A", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                sortMode = ArtistSortMode.TITLE_DESC
                                sortMenuExpanded = false
                            },
                        )
                    }
                }

                IconButton(
                    onClick = {
                        showSearchBar = !showSearchBar
                        if (!showSearchBar) searchQuery = ""
                    },
                ) { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = MaterialTheme.colorScheme.onBackground) }
                IconButton(onClick = {
                    viewAsGrid = !viewAsGrid
                }) {
                    Icon(
                        imageVector = if (viewAsGrid) Icons.Default.ViewList else Icons.Default.ViewModule,
                        contentDescription = "Cambiar vista",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            if (showSearchBar) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    placeholder = { Text("Buscar por artista", color = MaterialTheme.colorScheme.onBackground) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (viewAsGrid) {
                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Adaptive(140.dp),
                        state = gridState,
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(sortedArtists) { artist ->
                            // Aquí puedes mostrar la información del artista
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onArtistClick(artist.name) }
                                        .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                // Puedes agregar imagen o avatar si lo tienes en el modelo Artist
                                Text(
                                    text = artist.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(text = "${artist.songCount} canciones", color = md_textSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(sortedArtists) { artist ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { onArtistClick(artist.name) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = artist.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(text = "${artist.songCount} canciones", color = md_textSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class ArtistSortMode {
    TITLE_ASC,
    TITLE_DESC,
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun ArtistDetailScreen(
    artistViewModel: ArtistViewModel,
    playbackViewModel: PlaybackViewModel,
    playlistViewModel: PlaylistViewModel,
    artistName: String,
    onBack: () -> Unit,
    onAlbumClick: (String, String) -> Unit,
    onViewAllSongs: () -> Unit,
) {
    val artistSongs by artistViewModel.getSongsForArtist(artistName).collectAsState(initial = emptyList())
    val playerState by playbackViewModel.playerState.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()
    val context = LocalContext.current

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onBackground)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artistName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Text(text = "${artistSongs.size} canciones", color = MaterialTheme.extendedColors.textSecondary, fontSize = 12.sp)
            }
        }

        // Mostrar álbumes del artista como grid horizontal (ahora visible antes de la lista de canciones)
        val artistAlbums =
            remember(artistSongs) {
                artistSongs.groupBy { it.album.ifBlank { "Desconocido" } }.toList()
            }
        if (artistAlbums.isNotEmpty()) {
            Text(
                text = "Álbumes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(artistAlbums) { (albumName, albumSongs) ->
                    val firstSong = albumSongs.firstOrNull()
                    var albumArt by remember(firstSong?.uri) { mutableStateOf<Bitmap?>(null) }
                    LaunchedEffect(firstSong?.uri) {
                        withContext(Dispatchers.IO) {
                            try {
                                val uri = firstSong?.uri ?: return@withContext
                                val retriever = MediaMetadataRetriever()
                                retriever.setDataSource(context, uri)
                                retriever.embeddedPicture?.let {
                                    albumArt = BitmapFactory.decodeByteArray(it, 0, it.size)
                                }
                                retriever.release()
                            } catch (_: Exception) {
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.width(120.dp).clickable { onAlbumClick(albumName, artistName) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = albumArt?.let { BitmapPainter(it.asImageBitmap()) } ?: painterResource(R.drawable.ic_default_album),
                            contentDescription = null,
                            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = albumName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${albumSongs.size} canciones",
                            color = md_textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(start = 16.dp, top = 8.dp, bottom = 16.dp, end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(artistSongs) { song ->
                val isCurrent = playerState.currentSong?.id == song.id
                SongItem(
                    song = song,
                    isPlaying = isCurrent,
                    onClick = {
                        // Usar el orden del artista como cola de reproduccion
                        playbackViewModel.updateDisplayOrder(artistSongs)
                        playbackViewModel.play(song)
                    },
                    onQueueNext = { playbackViewModel.addToQueueNext(song) },
                    onQueueEnd = { playbackViewModel.addToQueueEnd(song) },
                    playlists = playlists,
                    onAddToPlaylist = { playlistName, songId ->
                        playlistViewModel.addSongToPlaylist(playlistName, songId)
                    },
                )
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun ArtistSongsScreen(
    artistViewModel: ArtistViewModel,
    artistName: String,
    onBack: () -> Unit,
) {
    val repo = remember { SongRepository(artistViewModel.getApplication<Application>()) }
    val allSongs = remember { repo.loadSongs() }
    val artistSongs = allSongs.filter { it.artist == artistName }
    val context = LocalContext.current

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = artistName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(artistSongs) { song ->
                SongItem(
                    song = song,
                    isPlaying = false,
                    onClick = { /* Aquí puedes agregar lógica de reproducción */ },
                    onQueueNext = {},
                    onQueueEnd = {},
                    playlists = emptyList(),
                    onAddToPlaylist = { _, _ -> },
                )
            }
        }
    }
}
