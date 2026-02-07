package com.cvc953.localplayer.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.R
import com.cvc953.localplayer.model.Playlist
import com.cvc953.localplayer.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AlbumsScreen(viewModel: MainViewModel, onAlbumClick: (String) -> Unit) {
    val songs by viewModel.songs.collectAsState()
    val isScanning by viewModel.isScanning
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(AlbumSortMode.TITLE_ASC) }
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

    val albums = remember(songs) { songs.groupBy { it.album.ifBlank { "Desconocido" } }.toList() }

    val filteredAlbums =
            remember(albums, searchQuery) {
                val q = searchQuery.trim().lowercase()
                if (q.isEmpty()) albums else albums.filter { it.first.lowercase().contains(q) }
            }

    val sortedAlbums =
            remember(filteredAlbums, sortMode) {
                when (sortMode) {
                    AlbumSortMode.TITLE_ASC -> filteredAlbums.sortedBy { it.first.lowercase() }
                    AlbumSortMode.TITLE_DESC ->
                            filteredAlbums.sortedByDescending { it.first.lowercase() }
                }
            }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var currentScrollLetter by remember { mutableStateOf<String?>(null) }

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
                        text = "Álbumes",
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
                                    sortMode = AlbumSortMode.TITLE_ASC
                                    sortMenuExpanded = false
                                }
                        )
                        DropdownMenuItem(
                                text = { Text("Título Z-A", color = Color.White) },
                                onClick = {
                                    sortMode = AlbumSortMode.TITLE_DESC
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
                        placeholder = { Text("Buscar por álbum", color = Color(0xFF808080)) },
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

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding =
                                PaddingValues(
                                        start = 16.dp,
                                        top = 16.dp,
                                        bottom = 16.dp,
                                        end = 4.dp
                                ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedAlbums) { (albumName, albumSongs) ->
                        val context = LocalContext.current
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
                                } catch (_: Exception) {}
                            }
                        }

                        Row(
                                modifier =
                                        Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable {
                                            onAlbumClick(albumName)
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
                                        text = albumName,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                        text = "${albumSongs.size} canciones",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                if (sortedAlbums.isNotEmpty()) {
                    val alphabet = listOf("#") + ('A'..'Z').map { it.toString() }
                    var columnHeight by remember { mutableStateOf(0f) }

                    fun scrollToLetter(letter: String) {
                        currentScrollLetter = letter
                        scope.launch {
                            delay(800)
                            currentScrollLetter = null
                        }
                        val index =
                                if (letter == "#") {
                                    sortedAlbums.indexOfFirst { (albumName, _) ->
                                        val firstChar = albumName.firstOrNull()?.uppercaseChar()
                                        firstChar == null || !firstChar.isLetter()
                                    }
                                } else {
                                    sortedAlbums.indexOfFirst { (albumName, _) ->
                                        albumName.firstOrNull()?.uppercaseChar() == letter[0]
                                    }
                                }
                        if (index >= 0) {
                            scope.launch { listState.scrollToItem(index) }
                        }
                    }

                    Column(
                            modifier =
                                    Modifier.align(Alignment.CenterEnd)
                                            .padding(end = 4.dp)
                                            .width(28.dp)
                                            .fillMaxHeight(0.75f)
                                            .onGloballyPositioned { coords ->
                                                columnHeight = coords.size.height.toFloat()
                                            }
                                            .pointerInput(Unit) {
                                                detectDragGestures(
                                                        onDragStart = { offset ->
                                                            val index =
                                                                    ((offset.y / columnHeight) *
                                                                                    alphabet.size)
                                                                            .toInt()
                                                                            .coerceIn(
                                                                                    0,
                                                                                    alphabet.lastIndex
                                                                            )
                                                            scrollToLetter(alphabet[index])
                                                        },
                                                        onDrag = { change, _ ->
                                                            change.consume()
                                                            val y =
                                                                    change.position.y.coerceIn(
                                                                            0f,
                                                                            columnHeight
                                                                    )
                                                            val index =
                                                                    ((y / columnHeight) *
                                                                                    alphabet.size)
                                                                            .toInt()
                                                                            .coerceIn(
                                                                                    0,
                                                                                    alphabet.lastIndex
                                                                            )
                                                            scrollToLetter(alphabet[index])
                                                        }
                                                )
                                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        alphabet.forEach { letter ->
                            val isActive = currentScrollLetter == letter
                            Text(
                                    text = letter,
                                    color =
                                            if (isActive) Color(0xFF2196F3)
                                            else Color.White.copy(alpha = 0.7f),
                                    fontSize = if (isActive) 12.sp else 10.sp,
                                    fontWeight =
                                            if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier =
                                            Modifier.clickable { scrollToLetter(letter) }
                                                    .padding(vertical = 1.5.dp)
                            )
                        }
                    }
                }

                currentScrollLetter?.let { letter ->
                    Box(
                            modifier =
                                    Modifier.align(Alignment.Center)
                                            .size(
                                                    with(LocalDensity.current) {
                                                        LocalConfiguration.current
                                                                .screenWidthDp
                                                                .dp * 0.25f
                                                    }
                                            )
                                            .background(
                                                    Color.Black.copy(alpha = 0.8f),
                                                    RoundedCornerShape(16.dp)
                                            )
                                            .border(
                                                    2.dp,
                                                    Color(0xFF2196F3),
                                                    RoundedCornerShape(16.dp)
                                            ),
                            contentAlignment = Alignment.Center
                    ) {
                        Text(
                                text = letter,
                                color = Color.White,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumDetailScreen(viewModel: MainViewModel, albumName: String, onBack: () -> Unit) {
    val songs by viewModel.songs.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val playlists: List<Playlist> by viewModel.playlists.collectAsState()
    val albumSongs = remember(songs, albumName) { songs.filter { it.album == albumName } }
    val context = LocalContext.current

    BackHandler { onBack() }

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
                        text = albumName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
                Text(text = "${albumSongs.size} canciones", color = Color.Gray, fontSize = 12.sp)
            }
        }

        LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                        PaddingValues(start = 16.dp, top = 8.dp, bottom = 16.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(albumSongs) { song ->
                val isCurrent = playerState.currentSong?.id == song.id
                SongItem(
                        song = song,
                        isPlaying = isCurrent,
                        onClick = {
                            // Usar el orden del album como cola de reproduccion
                            viewModel.updateDisplayOrder(albumSongs)
                            viewModel.playSong(song)
                            viewModel.startService(context, song)
                        },
                        onQueueNext = { viewModel.addToQueueNext(song) },
                        onQueueEnd = { viewModel.addToQueueEnd(song) },
                        playlists = playlists,
                        onAddToPlaylist = { playlistName, songId ->
                            viewModel.addSongToPlaylist(playlistName, songId)
                        }
                )
            }
        }
    }
}

private enum class AlbumSortMode {
    TITLE_ASC,
    TITLE_DESC
}
