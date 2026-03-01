@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.cvc953.localplayer.ui

import android.app.Activity
import android.app.Application
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
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cvc953.localplayer.R
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.model.SongRepository
import com.cvc953.localplayer.ui.theme.md_textSecondary
import com.cvc953.localplayer.viewmodel.ArtistViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlaylistViewModel
import com.cvc953.localplayer.viewmodel.SongViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("ktlint:standard:function-naming")
@Composable
fun ArtistsScreen(
    artistViewModel: ArtistViewModel,
    playbackViewModel: PlaybackViewModel,
    onArtistClick: (artistName: String) -> Unit,
) {
    // Use the global SongViewModel to fetch all songs so we can build artist thumbnails
    val songViewModel: SongViewModel = viewModel()
    val songs by songViewModel.songs.collectAsState()
    val artists by artistViewModel.artists.collectAsState()

    // Expand and deduplicate artist names using normalization
    val expandedArtists =
        remember(artists) {
            val seen = mutableSetOf<String>()
            val result = mutableListOf<com.cvc953.localplayer.model.Artist>()
            for (artist in artists) {
                for (name in normalizeArtistName(artist.name)) {
                    val norm = name.trim()
                    if (norm.isNotEmpty() && seen.add(norm.lowercase())) {
                        result.add(artist.copy(name = norm))
                    }
                }
            }
            result
        }
    val isScanning by songViewModel.isScanning.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(ArtistSortMode.TITLE_ASC) }
    var viewAsGrid by rememberSaveable { mutableStateOf(artistViewModel.isGridViewPreferred()) }
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
        remember(expandedArtists, searchQuery) {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) expandedArtists else expandedArtists.filter { it.name.lowercase().contains(q) }
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
                    artistViewModel.setGridViewPreferred(viewAsGrid)
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
                            val context = LocalContext.current
                            // Buscar cualquier canción donde el artista normalizado coincida
                            val firstSong =
                                songs.firstOrNull { song ->
                                    normalizeArtistName(song.artist).any { it.equals(artist.name.trim(), ignoreCase = true) }
                                }
                            var artistArt by remember(firstSong?.uri) { mutableStateOf<Bitmap?>(null) }

                            LaunchedEffect(firstSong?.uri, firstSong?.filePath) {
                                withContext(Dispatchers.IO) {
                                    try {
                                        val uri = firstSong?.uri ?: return@withContext
                                        Log.d(
                                            "ArtistsScreen",
                                            "Loading art for artist='${artist.name}' uri=$uri filePath=${firstSong?.filePath}",
                                        )
                                        val retriever = MediaMetadataRetriever()
                                        retriever.setDataSource(context, uri)
                                        val embedded = retriever.embeddedPicture
                                        if (embedded != null && embedded.isNotEmpty()) {
                                            artistArt = BitmapFactory.decodeByteArray(embedded, 0, embedded.size)
                                            Log.d("ArtistsScreen", "Embedded art found for uri=$uri size=${embedded.size}")
                                        } else {
                                            Log.d("ArtistsScreen", "No embedded art for uri=$uri")
                                        }
                                        retriever.release()

                                        if (artistArt == null) {
                                            Log.d("ArtistsScreen", "Trying contentResolver fallback for uri=$uri")
                                            try {
                                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                                    artistArt = BitmapFactory.decodeStream(stream)
                                                }
                                            } catch (e: Exception) {
                                                Log.d("ArtistsScreen", "contentResolver fallback failed: ${e.message}")
                                            }
                                        }

                                        if (artistArt == null) {
                                            Log.d("ArtistsScreen", "Trying file fallback for filePath=${firstSong?.filePath}")
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
                                                            artistArt = BitmapFactory.decodeFile(f.absolutePath)
                                                            if (artistArt != null) {
                                                                Log.d("ArtistsScreen", "Found external cover file=${f.absolutePath}")
                                                                break
                                                            }
                                                        }
                                                    }
                                                } catch (_: Exception) {
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.d("ArtistsScreen", "Unexpected error loading art: ${e.message}")
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth().clickable { onArtistClick(artist.name) }.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(modifier = Modifier.size(120.dp)) {
                                    Image(
                                        painter =
                                            artistArt?.let { BitmapPainter(it.asImageBitmap()) }
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
                                            // Solo usar las canciones del artista actual para el dropdown (más rápido)
                                            val artistSongs =
                                                songs
                                                    .filter {
                                                        it.artist.trim().equals(artist.name.trim(), ignoreCase = true)
                                                    }.sortedWith(compareBy<Song>({ it.album }, { it.discNumber }, { it.trackNumber }))
                                            val firstSongOfArtist = artistSongs.firstOrNull()
                                            DropdownMenuItem(
                                                text = { Text("Reproducir ahora", color = MaterialTheme.colorScheme.onSurface) },
                                                onClick = {
                                                    menuExpanded = false
                                                    if (artistSongs.isNotEmpty()) {
                                                        playbackViewModel.updateDisplayOrder(artistSongs)
                                                        playbackViewModel.play(artistSongs[0])
                                                    }
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Añadir como siguiente", color = MaterialTheme.colorScheme.onSurface) },
                                                onClick = {
                                                    menuExpanded = false
                                                    val currentQueue = playbackViewModel.queue.value
                                                    val toAdd = artistSongs.filter { song -> currentQueue.none { it.id == song.id } }
                                                    toAdd.reversed().forEach { playbackViewModel.addToQueueNext(it) }
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Añadir al final", color = MaterialTheme.colorScheme.onSurface) },
                                                onClick = {
                                                    menuExpanded = false
                                                    val currentQueue = playbackViewModel.queue.value
                                                    val toAdd = artistSongs.filter { song -> currentQueue.none { it.id == song.id } }
                                                    toAdd.forEach { playbackViewModel.addToQueueEnd(it) }
                                                },
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = artist.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                // Contar todas las canciones donde el artista aparece (normalizado)
                                val songCount =
                                    songs.count { song ->
                                        normalizeArtistName(song.artist).any { it.equals(artist.name.trim(), ignoreCase = true) }
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
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(sortedArtists) { artist ->
                            val context = LocalContext.current
                            val firstSong = songs.firstOrNull { it.artist.trim().equals(artist.name.trim(), ignoreCase = true) }
                            var artistArt by remember(firstSong?.uri) { mutableStateOf<Bitmap?>(null) }

                            LaunchedEffect(firstSong?.uri, firstSong?.filePath) {
                                withContext(Dispatchers.IO) {
                                    try {
                                        val uri = firstSong?.uri ?: return@withContext
                                        val retriever = MediaMetadataRetriever()
                                        retriever.setDataSource(context, uri)
                                        val embedded = retriever.embeddedPicture
                                        if (embedded != null && embedded.isNotEmpty()) {
                                            artistArt = BitmapFactory.decodeByteArray(embedded, 0, embedded.size)
                                        }
                                        retriever.release()

                                        if (artistArt == null) {
                                            try {
                                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                                    artistArt = BitmapFactory.decodeStream(stream)
                                                }
                                            } catch (_: Exception) {
                                            }
                                        }

                                        if (artistArt == null) {
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
                                                            artistArt = BitmapFactory.decodeFile(f.absolutePath)
                                                            if (artistArt != null) break
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
                                        onArtistClick(artist.name)
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Image(
                                    painter =
                                        artistArt?.let {
                                            BitmapPainter(
                                                it.asImageBitmap(),
                                            )
                                        } ?: painterResource(R.drawable.ic_default_album),
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop,
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = artist.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(text = "${artist.songCount} canciones", color = md_textSecondary, fontSize = 12.sp, maxLines = 1)
                                }

                                var menuExpanded by remember { mutableStateOf(false) }

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
                                        // Agrupar todas las canciones por artista, orden alfabético
                                        val artistsOrdered = sortedArtists.map { it.name.trim() }
                                        val artistSongs =
                                            songs
                                                .filter { song ->
                                                    normalizeArtistName(
                                                        song.artist,
                                                    ).any { it.equals(artist.name.trim(), ignoreCase = true) }
                                                }.sortedWith(compareBy<Song>({ it.album }, { it.discNumber }, { it.trackNumber }))
                                        val firstSongOfArtist = artistSongs.firstOrNull()
                                        val startIndex = 0
                                        DropdownMenuItem(
                                            text = { Text("Reproducir ahora", color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = {
                                                menuExpanded = false
                                                if (artistSongs.isNotEmpty()) {
                                                    playbackViewModel.updateDisplayOrder(artistSongs)
                                                    playbackViewModel.play(artistSongs[0])
                                                }
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Añadir como siguiente", color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = {
                                                menuExpanded = false
                                                val currentQueue = playbackViewModel.queue.value
                                                val toAdd = artistSongs.filter { song -> currentQueue.none { it.id == song.id } }
                                                toAdd.reversed().forEach { playbackViewModel.addToQueueNext(it) }
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Añadir al final", color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = {
                                                menuExpanded = false
                                                val currentQueue = playbackViewModel.queue.value
                                                val toAdd = artistSongs.filter { song -> currentQueue.none { it.id == song.id } }
                                                toAdd.forEach { playbackViewModel.addToQueueEnd(it) }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (sortedArtists.isNotEmpty()) {
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
                                sortedArtists.indexOfFirst { artist ->
                                    val firstChar = artist.name.firstOrNull()?.uppercaseChar()
                                    firstChar == null || !firstChar.isLetter()
                                }
                            } else {
                                sortedArtists.indexOfFirst { artist ->
                                    artist.name.firstOrNull()?.uppercaseChar() == letter[0]
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
                                    columnHeight =
                                        coords.size.height.toFloat()
                                }.pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val index = ((offset.y / columnHeight) * alphabet.size).toInt().coerceIn(0, alphabet.lastIndex)
                                            scrollToLetter(alphabet[index])
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val y = change.position.y.coerceIn(0f, columnHeight)
                                            val index = ((y / columnHeight) * alphabet.size).toInt().coerceIn(0, alphabet.lastIndex)
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
                                        MaterialTheme.colorScheme.onBackground.copy(
                                            alpha = 0.7f,
                                        )
                                    },
                                fontSize = if (isActive) 12.sp else 10.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier =
                                    Modifier
                                        .weight(
                                            1f,
                                        ).wrapContentHeight(Alignment.CenterVertically)
                                        .clickable { scrollToLetter(letter) },
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
                                        LocalConfiguration.current.screenWidthDp.dp *
                                            0.25f
                                    },
                                ).background(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                    RoundedCornerShape(16.dp),
                                ).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = letter, color = MaterialTheme.colorScheme.onBackground, fontSize = 48.sp, fontWeight = FontWeight.Bold)
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
    onAlbumClick: (albumName: String, artistName: String) -> Unit,
    onViewAllSongs: () -> Unit,
    onBack: () -> Unit,
) {
    val repo = remember { SongRepository(artistViewModel.getApplication<Application>()) }
    val allSongs = remember { repo.loadSongs() }
    val playerState by playbackViewModel.playerState.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()
    val artistSongs =
        remember(allSongs, artistName) {
            allSongs.filter { song -> normalizeArtistName(song.artist).any { it.equals(artistName, ignoreCase = true) } }
        }
    val artistSongsSorted = remember(artistSongs) { artistSongs.sortedWith(compareBy({ it.album }, { it.discNumber }, { it.trackNumber })) }
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
            }
        }

        val maxItems = 6

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { ArtistHeader(artistViewModel, artistName, playbackViewModel, modifier = Modifier, onViewAllSongs) }
            items(artistSongsSorted.take(maxItems)) { song ->
                val isCurrent = playerState.currentSong?.id == song.id

                SongItem(
                    song = song,
                    isPlaying = isCurrent,
                    onClick = {
                        playbackViewModel.updateDisplayOrder(artistSongsSorted)
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
            // LazyRow de álbumes del artista
            item {
                val albums =
                    remember(artistSongs) {
                        artistSongs
                            .groupBy { it.album.ifBlank { "Desconocido" } }
                            .filterKeys { it.isNotBlank() }
                            .toList()
                    }
                if (albums.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Álbumes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                        )
                        LazyRow(
                            // contentPadding = PaddingValues(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(albums) { (albumName, albumSongs) ->
                                val context = LocalContext.current
                                val representativeSong = albumSongs.firstOrNull()
                                var albumArt by remember(representativeSong?.uri) { mutableStateOf<Bitmap?>(null) }
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
                                        } catch (_: Exception) {
                                        }
                                    }
                                }
                                Column(
                                    modifier =
                                        Modifier
                                            .width(120.dp)
                                            .clickable { onAlbumClick(albumName, artistName) },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Image(
                                        painter =
                                            albumArt?.let { BitmapPainter(it.asImageBitmap()) }
                                                ?: painterResource(R.drawable.ic_default_album),
                                        contentDescription = null,
                                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = albumName,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${albumSongs.size} canciones",
                                        color = md_textSecondary,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun ArtistHeader(
    artistViewModel: ArtistViewModel,
    artistName: String,
    playbackViewModel: PlaybackViewModel,
    modifier: Modifier,
    onViewAllSongs: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        val songs by artistViewModel.getSongsForArtist(artistName).collectAsState(initial = emptyList())
        val repo = remember { SongRepository(artistViewModel.getApplication<Application>()) }
        val allSongs = remember { repo.loadSongs() }
        val artistSongs =
            remember(allSongs, artistName) {
                allSongs.filter { song -> normalizeArtistName(song.artist).any { it.equals(artistName, ignoreCase = true) } }
            }
        var artistArt by remember { mutableStateOf<Bitmap?>(null) }
        val firstSong = artistSongs.firstOrNull()
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
                        artistArt = BitmapFactory.decodeByteArray(picture, 0, picture.size)
                    }

                    if (artistArt == null) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                val bmp = BitmapFactory.decodeStream(stream)
                                if (bmp != null) artistArt = bmp
                            }
                        } catch (_: Exception) {
                        }
                    }

                    if (artistArt == null) {
                        val path = firstSong?.filePath
                        if (!path.isNullOrBlank()) {
                            try {
                                val dir = java.io.File(path).parentFile
                                val candidates = listOf("cover.jpg", "folder.jpg", "album.jpg", "front.jpg", "cover.png", "folder.png")
                                for (name in candidates) {
                                    val f = java.io.File(dir, name)
                                    if (f.exists() && f.length() > 0) {
                                        val bmp = BitmapFactory.decodeFile(f.absolutePath)
                                        if (bmp != null) {
                                            artistArt = bmp
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
            painter = artistArt?.asImageBitmap()?.let { BitmapPainter(it) } ?: painterResource(R.drawable.ic_default_album),
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
            text = artistName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "${artistSongs.size} canciones", fontSize = 16.sp, color = MaterialTheme.extendedColors.textSecondary)

        Spacer(modifier = Modifier.height(8.dp))

        val buttonColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Canciones", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(
                text = "Ver todas",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.extendedColors.textSecondary,
                modifier = Modifier.clickable { onViewAllSongs() },
            )
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
    val artistSongs = allSongs.filter { song -> normalizeArtistName(song.artist).any { it.equals(artistName, ignoreCase = true) } }
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

// Normaliza nombres de artistas, separando por ',' y '/' excepto 'AC/DC'
fun normalizeArtistName(artist: String): List<String> {
    val trimmed = artist.trim()
    return if (trimmed.equals("AC/DC", ignoreCase = true)) {
        listOf("AC/DC")
    } else {
        trimmed.split(',', '/').map { it.trim() }.filter { it.isNotEmpty() }
    }
}
