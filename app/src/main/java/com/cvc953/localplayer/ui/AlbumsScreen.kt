@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.cvc953.localplayer.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import com.cvc953.localplayer.R
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.theme.md_textSecondary
import com.cvc953.localplayer.viewmodel.AlbumViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlaylistViewModel
import com.cvc953.localplayer.viewmodel.SongViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("ktlint:standard:function-naming")
@Composable
fun AlbumsScreen(
    albumViewModel: AlbumViewModel,
    playbackViewModel: PlaybackViewModel,
    onAlbumClick: (albumName: String, artistName: String) -> Unit,
) {
    // Use the global SongViewModel to get the full songs list so album thumbnails
    // are available for all albums, not only the currently selected one.
    val songViewModel: SongViewModel =
        androidx.lifecycle.viewmodel.compose
            .viewModel()
    val songs by songViewModel.songs.collectAsState()
    val albums by albumViewModel.albums.collectAsState()
    val isScanning by albumViewModel.isScanning
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(AlbumSortMode.TITLE_ASC) }
    var viewAsGrid by rememberSaveable { mutableStateOf(albumViewModel.isGridViewPreferred()) }
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

    // Agrupar álbumes por artista usando normalización
    data class AlbumKey(
        val name: String,
        val artist: String,
    )
    val expandedAlbums =
        remember(albums) {
            val seen = mutableSetOf<AlbumKey>()
            val result = mutableListOf<com.cvc953.localplayer.model.Album>()
            for (album in albums) {
                val artistNames = normalizeArtistName(album.artist)
                val mainArtist = artistNames.firstOrNull()?.trim() ?: album.artist.trim()
                for (albumName in normalizeAlbumName(album.name)) {
                    val normAlbum = albumName.trim()
                    val normArtist = mainArtist
                    val key = AlbumKey(normAlbum, normArtist)
                    if (normAlbum.isNotEmpty() && normArtist.isNotEmpty() && seen.add(key)) {
                        result.add(album.copy(name = normAlbum, artist = normArtist))
                    }
                }
            }
            result
        }

    val filteredAlbums =
        remember(expandedAlbums, searchQuery) {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) expandedAlbums else expandedAlbums.filter { it.name.lowercase().contains(q) }
        }

    val sortedAlbums =
        remember(filteredAlbums, sortMode) {
            when (sortMode) {
                AlbumSortMode.TITLE_ASC -> filteredAlbums.sortedBy { it.name.lowercase() }
                AlbumSortMode.TITLE_DESC -> filteredAlbums.sortedByDescending { it.name.lowercase() }
            }
        }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var currentScrollLetter by remember { mutableStateOf<String?>(null) }

    if (isScanning) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Escaneando canciones", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Álbumes",
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
                                sortMode = AlbumSortMode.TITLE_ASC
                                sortMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Título Z-A", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                sortMode = AlbumSortMode.TITLE_DESC
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
                    albumViewModel.setGridViewPreferred(viewAsGrid)
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
                    placeholder = { Text("Buscar por álbum", color = MaterialTheme.colorScheme.onBackground) },
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
                        items(sortedAlbums) { album ->
                            val context = LocalContext.current
                            // Buscar la primera canción usando normalización para coincidencia real
                            val firstSong =
                                songs.firstOrNull { song ->
                                    normalizeAlbumName(song.album).any { it.equals(album.name.trim(), ignoreCase = true) } &&
                                        normalizeArtistName(song.artist).any { it.equals(album.artist.trim(), ignoreCase = true) }
                                }
                            var albumArt by remember(firstSong?.uri) { mutableStateOf<Bitmap?>(null) }

                            LaunchedEffect(firstSong?.uri, firstSong?.filePath) {
                                withContext(Dispatchers.IO) {
                                    try {
                                        val uri = firstSong?.uri ?: return@withContext
                                        Log.d(
                                            "AlbumsScreen",
                                            "Loading art for album='${album.name}' artist='${album.artist}' uri=$uri filePath=${firstSong?.filePath}",
                                        )
                                        val retriever = MediaMetadataRetriever()
                                        retriever.setDataSource(context, uri)
                                        val embedded = retriever.embeddedPicture
                                        if (embedded != null && embedded.isNotEmpty()) {
                                            albumArt = BitmapFactory.decodeByteArray(embedded, 0, embedded.size)
                                            Log.d("AlbumsScreen", "Embedded art found for uri=$uri size=${embedded.size}")
                                        } else {
                                            Log.d("AlbumsScreen", "No embedded art for uri=$uri")
                                        }
                                        retriever.release()

                                        if (albumArt == null) {
                                            Log.d("AlbumsScreen", "Trying contentResolver fallback for uri=$uri")
                                            try {
                                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                                    albumArt = BitmapFactory.decodeStream(stream)
                                                }
                                            } catch (e: Exception) {
                                                Log.d("AlbumsScreen", "contentResolver fallback failed: ${e.message}")
                                            }
                                        }

                                        if (albumArt == null) {
                                            Log.d("AlbumsScreen", "Trying file fallback for filePath=${firstSong?.filePath}")
                                            val path = firstSong?.filePath
                                            if (!path.isNullOrBlank()) {
                                                try {
                                                    val dir = java.io.File(path).parentFile
                                                    val candidates =
                                                        listOf(
                                                            "cover.jpg",
                                                            "folder.jpg",
                                                            "album.jpg",
                                                            "front.jpg",
                                                            "cover.png",
                                                            "folder.png",
                                                        )
                                                    for (name in candidates) {
                                                        val f = java.io.File(dir, name)
                                                        if (f.exists() && f.length() > 0) {
                                                            albumArt = BitmapFactory.decodeFile(f.absolutePath)
                                                            if (albumArt != null) {
                                                                Log.d("AlbumsScreen", "Found external cover file=${f.absolutePath}")
                                                                break
                                                            }
                                                        }
                                                    }
                                                } catch (_: Exception) {
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.d("AlbumsScreen", "Unexpected error loading art: ${e.message}")
                                    }
                                }
                            }

                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onAlbumClick(album.name, album.artist) }
                                        .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(modifier = Modifier.size(120.dp)) {
                                    Image(
                                        painter =
                                            albumArt?.let { BitmapPainter(it.asImageBitmap()) }
                                                ?: painterResource(R.drawable.ic_default_album),
                                        contentDescription = null,
                                        modifier = Modifier.matchParentSize().clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                    var menuExpanded by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                                        IconButton(onClick = { menuExpanded = true }) {
                                            Icon(
                                                Icons.Default.MoreVert,
                                                contentDescription = "Más opciones",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = menuExpanded,
                                            onDismissRequest = { menuExpanded = false },
                                            containerColor = MaterialTheme.extendedColors.surfaceSheet,
                                            modifier = Modifier.background(MaterialTheme.extendedColors.surfaceSheet),
                                        ) {
                                            // Solo usar las canciones del álbum actual para el dropdown (más rápido)
                                            val albumSongs =
                                                songs
                                                    .filter { song ->
                                                        normalizeAlbumName(
                                                            song.album,
                                                        ).any { it.equals(album.name.trim(), ignoreCase = true) } &&
                                                            normalizeArtistName(
                                                                song.artist,
                                                            ).any { it.equals(album.artist.trim(), ignoreCase = true) }
                                                    }.sortedWith(compareBy<Song>({ it.discNumber }, { it.trackNumber }))
                                            val firstSongOfAlbum = albumSongs.firstOrNull()

                                            DropdownMenuItem(
                                                text = { Text("Reproducir ahora", color = MaterialTheme.colorScheme.onSurface) },
                                                onClick = {
                                                    menuExpanded = false
                                                    if (albumSongs.isNotEmpty()) {
                                                        playbackViewModel.updateDisplayOrder(albumSongs)
                                                        playbackViewModel.play(albumSongs[0])
                                                    }
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Añadir como siguiente", color = MaterialTheme.colorScheme.onSurface) },
                                                onClick = {
                                                    menuExpanded = false
                                                    val currentQueue = playbackViewModel.queue.value
                                                    val toAdd = albumSongs.filter { song -> currentQueue.none { it.id == song.id } }
                                                    toAdd.reversed().forEach { playbackViewModel.addToQueueNext(it) }
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Añadir al final", color = MaterialTheme.colorScheme.onSurface) },
                                                onClick = {
                                                    menuExpanded = false
                                                    val currentQueue = playbackViewModel.queue.value
                                                    val toAdd = albumSongs.filter { song -> currentQueue.none { it.id == song.id } }
                                                    toAdd.forEach { playbackViewModel.addToQueueEnd(it) }
                                                },
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = album.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                // Contar canciones igual que en AlbumDetailScreen (solo primer artista normalizado)
                                val mainArtist = normalizeArtistName(album.artist).firstOrNull() ?: album.artist
                                val songCount =
                                    songs.count { song ->
                                        normalizeAlbumName(song.album).any { it.equals(album.name.trim(), ignoreCase = true) } &&
                                            normalizeArtistName(song.artist).firstOrNull()?.equals(mainArtist, ignoreCase = true) == true
                                    }
                                Text(
                                    text = "$songCount canciones",
                                    color = MaterialTheme.extendedColors.textSecondary,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding =
                            PaddingValues(
                                start = 16.dp,
                                top = 16.dp,
                                bottom = 16.dp,
                                end = 16.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(sortedAlbums) { album ->
                            val context = LocalContext.current
                            val firstSong =
                                songs.firstOrNull {
                                    it.album.trim().equals(album.name.trim(), ignoreCase = true) &&
                                        it.artist.trim().equals(album.artist.trim(), ignoreCase = true)
                                }
                            var albumArt by remember(firstSong?.uri) { mutableStateOf<Bitmap?>(null) }

                            LaunchedEffect(firstSong?.uri, firstSong?.filePath) {
                                withContext(Dispatchers.IO) {
                                    try {
                                        val uri = firstSong?.uri ?: return@withContext
                                        val retriever = MediaMetadataRetriever()
                                        retriever.setDataSource(context, uri)
                                        val embedded = retriever.embeddedPicture
                                        if (embedded != null && embedded.isNotEmpty()) {
                                            albumArt = BitmapFactory.decodeByteArray(embedded, 0, embedded.size)
                                        }
                                        retriever.release()

                                        // Fallback: try content resolver stream (some providers expose album art this way)
                                        if (albumArt == null) {
                                            try {
                                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                                    albumArt = BitmapFactory.decodeStream(stream)
                                                }
                                            } catch (_: Exception) {
                                            }
                                        }

                                        // Fallback: check for common cover files next to the audio file
                                        if (albumArt == null) {
                                            val path = firstSong.filePath
                                            if (!path.isNullOrBlank()) {
                                                try {
                                                    val dir = java.io.File(path).parentFile
                                                    val candidates =
                                                        listOf(
                                                            "cover.jpg",
                                                            "folder.jpg",
                                                            "album.jpg",
                                                            "front.jpg",
                                                            "cover.png",
                                                            "folder.png",
                                                        )
                                                    for (name in candidates) {
                                                        val f = java.io.File(dir, name)
                                                        if (f.exists() && f.length() > 0) {
                                                            albumArt = BitmapFactory.decodeFile(f.absolutePath)
                                                            if (albumArt != null) break
                                                        }
                                                    }
                                                } catch (_: Exception) {
                                                }
                                            }
                                        }
                                    } catch (_: Exception) {
                                    }
                                }
                            }

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth().padding(8.dp).clickable {
                                        onAlbumClick(album.name, album.artist)
                                    },
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
                                        text = album.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${album.songCount} canciones",
                                        color = md_textSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                    )
                                }

                                var menuExpanded by remember { mutableStateOf(false) }
                                var showPlaylistDialog by remember { mutableStateOf(false) }

                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "Más opciones",
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
                                            text = { Text("Reproducir ahora", color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = {
                                                menuExpanded = false
                                                // Obtener canciones del álbum
                                                val albumSongs =
                                                    songs
                                                        .filter {
                                                            it.album.trim().equals(album.name.trim(), ignoreCase = true) &&
                                                                it.artist.trim().equals(album.artist.trim(), ignoreCase = true)
                                                        }.sortedWith(compareBy<Song>({ it.discNumber }, { it.trackNumber }))
                                                if (albumSongs.isNotEmpty()) {
                                                    playbackViewModel.updateDisplayOrder(albumSongs)
                                                    playbackViewModel.play(albumSongs.first())
                                                }
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Añadir como siguiente", color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = {
                                                menuExpanded = false
                                                // Añadir todas las canciones del álbum como siguientes (sin duplicar)
                                                val albumSongs =
                                                    songs
                                                        .filter {
                                                            it.album.trim().equals(album.name.trim(), ignoreCase = true) &&
                                                                it.artist.trim().equals(album.artist.trim(), ignoreCase = true)
                                                        }.sortedWith(compareBy<Song>({ it.discNumber }, { it.trackNumber }))
                                                val currentQueue = playbackViewModel.queue.value
                                                val toAdd = albumSongs.filter { song -> currentQueue.none { it.id == song.id } }
                                                toAdd.reversed().forEach { playbackViewModel.addToQueueNext(it) }
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Añadir al final", color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = {
                                                menuExpanded = false
                                                // Añadir todas las canciones del álbum al final (sin duplicar)
                                                val albumSongs =
                                                    songs
                                                        .filter {
                                                            it.album.trim().equals(album.name.trim(), ignoreCase = true) &&
                                                                it.artist.trim().equals(album.artist.trim(), ignoreCase = true)
                                                        }.sortedWith(compareBy<Song>({ it.discNumber }, { it.trackNumber }))
                                                val currentQueue = playbackViewModel.queue.value
                                                val toAdd = albumSongs.filter { song -> currentQueue.none { it.id == song.id } }
                                                toAdd.forEach { playbackViewModel.addToQueueEnd(it) }
                                            },
                                        )
                                    }
                                }
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
                            if (viewAsGrid) {
                                scope.launch { gridState.scrollToItem(index) }
                            } else {
                                scope.launch { listState.scrollToItem(index) }
                            }
                        }
                    }

                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                // .padding(end = 4.dp)
                                .width(28.dp)
                                .fillMaxHeight()
                                .onGloballyPositioned { coords ->
                                    columnHeight = coords.size.height.toFloat()
                                }.pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val index =
                                                (
                                                    (offset.y / columnHeight) *
                                                        alphabet.size
                                                ).toInt()
                                                    .coerceIn(
                                                        0,
                                                        alphabet.lastIndex,
                                                    )
                                            scrollToLetter(alphabet[index])
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val y =
                                                change.position.y.coerceIn(
                                                    0f,
                                                    columnHeight,
                                                )
                                            val index =
                                                (
                                                    (y / columnHeight) *
                                                        alphabet.size
                                                ).toInt()
                                                    .coerceIn(
                                                        0,
                                                        alphabet.lastIndex,
                                                    )
                                            scrollToLetter(alphabet[index])
                                        },
                                    )
                                },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        alphabet.forEach { letter ->
                            val isActive = currentScrollLetter == letter
                            Text(
                                text = letter,
                                color =
                                    if (isActive) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    },
                                fontSize = if (isActive) 12.sp else 10.sp,
                                fontWeight =
                                    if (isActive) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .wrapContentHeight(Alignment.CenterVertically)
                                        .clickable { scrollToLetter(letter) },
                                // .padding(vertical = 1.5.dp),
                            )
                        }
                    }
                }

                currentScrollLetter?.let { letter ->
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .size(
                                    with(LocalDensity.current) {
                                        LocalConfiguration.current
                                            .screenWidthDp
                                            .dp * 0.25f
                                    },
                                ).background(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                    RoundedCornerShape(16.dp),
                                ).border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(16.dp),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = letter,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

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
    // Solo considerar el primer nombre de artista normalizado para filtrar canciones
    val mainArtist = normalizeArtistName(artistName).firstOrNull() ?: artistName
    val albumSongs =
        remember(songs, albumName, mainArtist) {
            songs
                .filter { song ->
                    normalizeAlbumName(song.album).any { it.equals(albumName.trim(), ignoreCase = true) } &&
                        normalizeArtistName(song.artist).firstOrNull()?.equals(mainArtist, ignoreCase = true) == true
                }.sortedWith(compareBy<Song>({ it.discNumber }, { it.trackNumber }))
        }
    val context = LocalContext.current

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onBackground)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = albumName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Left,
                )
                // Text(text = "${albumSongs.size} canciones", color = MaterialTheme.extendedColors.texMeta, fontSize = 12.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { AlbumHeader(albumViewModel, albumName, artistName, playbackViewModel) }
            items(albumSongs) { song ->
                val isCurrent = playerState.currentSong?.id == song.id

                SongItem(
                    song = song,
                    isPlaying = isCurrent,
                    onClick = {
                        // Usar el orden del album como cola de reproduccion
                        playbackViewModel.updateDisplayOrder(albumSongs)
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

private enum class AlbumSortMode {
    TITLE_ASC,
    TITLE_DESC,
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun AlbumHeader(
    viewModel: AlbumViewModel,
    albumName: String,
    artistName: String,
    playbackViewModel: PlaybackViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(8.dp),
    ) {
        val songs by viewModel.songs.collectAsState()
        val mainArtist = normalizeArtistName(artistName).firstOrNull() ?: artistName
        val albumSongs =
            remember(songs, albumName, mainArtist) {
                songs
                    .filter { song ->
                        normalizeAlbumName(song.album).any { it.equals(albumName.trim(), ignoreCase = true) } &&
                            normalizeArtistName(song.artist).firstOrNull()?.equals(mainArtist, ignoreCase = true) == true
                    }.sortedWith(compareBy<Song>({ it.discNumber }, { it.trackNumber }))
            }
        var albumArt by remember { mutableStateOf<Bitmap?>(null) }
        val firstSong =
            songs.firstOrNull { song ->
                normalizeAlbumName(song.album).any { it.equals(albumName.trim(), ignoreCase = true) } &&
                    normalizeArtistName(song.artist).firstOrNull()?.equals(mainArtist, ignoreCase = true) == true
            }
        val context = LocalContext.current

        LaunchedEffect(firstSong?.uri, firstSong?.filePath) {
            withContext(Dispatchers.IO) {
                try {
                    val uri = firstSong?.uri ?: return@withContext
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val picture = retriever.embeddedPicture
                    retriever.release()
                    if (picture != null && picture.isNotEmpty()) {
                        albumArt = BitmapFactory.decodeByteArray(picture, 0, picture.size)
                    }

                    if (albumArt == null) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                val bmp = BitmapFactory.decodeStream(stream)
                                if (bmp != null) albumArt = bmp
                            }
                        } catch (_: Exception) {
                        }
                    }

                    if (albumArt == null) {
                        val path = firstSong.filePath
                        if (!path.isNullOrBlank()) {
                            try {
                                val dir = java.io.File(path).parentFile
                                val candidates = listOf("cover.jpg", "folder.jpg", "album.jpg", "front.jpg", "cover.png", "folder.png")
                                for (name in candidates) {
                                    val f = java.io.File(dir, name)
                                    if (f.exists() && f.length() > 0) {
                                        val bmp = BitmapFactory.decodeFile(f.absolutePath)
                                        if (bmp != null) {
                                            albumArt = bmp
                                            break
                                        }
                                    }
                                }
                            } catch (_: Exception) {
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
        Image(
            painter = albumArt?.asImageBitmap()?.let { BitmapPainter(it) } ?: painterResource(R.drawable.ic_default_album),
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxWidth(1f)
                    .aspectRatio(1f)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = albumName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Botones igual que PlaylistHeader
        val buttonColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)

        Text(text = "${albumSongs.size} canciones", fontSize = 16.sp, color = MaterialTheme.extendedColors.textSecondary)
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(32.dp),
            ) {
                Button(
                    onClick = {
                        if (albumSongs.isNotEmpty()) {
                            playbackViewModel.setShuffle(false)
                            playbackViewModel.playAlbum(albumName, mainArtist, albumSongs)
                            playbackViewModel.updateDisplayOrder(albumSongs)
                            playbackViewModel.play(albumSongs.first())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Reproducir ahora",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text("Reproducir", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(32.dp),
            ) {
                Button(
                    onClick = {
                        if (albumSongs.isNotEmpty()) {
                            val shuffled = albumSongs.shuffled()
                            playbackViewModel.setShuffle(true)
                            playbackViewModel.playAlbum(albumName, mainArtist, shuffled)
                            playbackViewModel.updateDisplayOrder(shuffled)
                            playbackViewModel.play(shuffled.first())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Aleatorio", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("Aleatorio", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// Normaliza nombres de álbumes, separando por ',' y '/' (puedes ajustar si hay excepciones)
fun normalizeAlbumName(albumName: String): List<String> {
    // Los álbumes NO deben dividirse por comas, solo trimear
    val trimmed = albumName.trim()
    return if (trimmed.isNotEmpty()) listOf(trimmed) else emptyList()
}
