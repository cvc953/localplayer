package com.cvc953.localplayer.ui

import MiniPlayer
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cvc953.localplayer.viewmodel.MainViewModel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.util.StoragePermissionHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch



@Composable
fun MusicScreen(viewModel: MainViewModel = viewModel(), onOpenPlayer: () -> Unit) {
    val songs by viewModel.songs.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(SortMode.TITLE_ASC) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val filteredSongs = remember(songs, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) songs else songs.filter { song ->
            song.title.lowercase().contains(q) || song.artist.lowercase().contains(q)
        }
    }

    val sortedSongs = remember(filteredSongs, sortMode) {
        when (sortMode) {
            SortMode.TITLE_ASC -> filteredSongs.sortedBy { it.title.lowercase() }
            SortMode.TITLE_DESC -> filteredSongs.sortedByDescending { it.title.lowercase() }
            SortMode.ARTIST_ASC -> filteredSongs.sortedBy { it.artist.lowercase() }
        }
    }
    val playerState by viewModel.playerState.collectAsState()
    var showPlayer by remember { mutableStateOf(false) }
    val showPlayerScreen by viewModel.isPlayerScreenVisible.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var currentScrollLetter by remember { mutableStateOf<String?>(null) }

    // Actualizar el servicio cuando cambia el estado del player
    LaunchedEffect(playerState.isPlaying) {
        val intent = Intent(context, com.cvc953.localplayer.services.MusicService::class.java).apply {
            action = com.cvc953.localplayer.services.MusicService.ACTION_UPDATE_STATE
            putExtra("IS_PLAYING", playerState.isPlaying)
        }
        androidx.core.content.ContextCompat.startForegroundService(context, intent)
    }

    BackHandler {
        activity?.moveTaskToBack(true)
    }

    BackHandler() {
    }

    Scaffold(
        containerColor = Color.Black,
            bottomBar = {
                if (
                    playerState.currentSong != null &&
                    !showPlayerScreen
                ) {
                    MiniPlayer(
                        song = playerState.currentSong!!,
                        isPlaying = playerState.isPlaying,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onClick = { viewModel.openPlayerScreen() },
                        onNext = { viewModel.playNextSong() }
                    )
                }
            }

            ) { padding ->


        val isScanning by viewModel.isScanning

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
        } else {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {

            Column(modifier = Modifier.fillMaxSize()) {


                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Biblioteca",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Ordenar", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Título A-Z") },
                                onClick = {
                                    sortMode = SortMode.TITLE_ASC
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Título Z-A") },
                                onClick = {
                                    sortMode = SortMode.TITLE_DESC
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Artista A-Z") },
                                onClick = {
                                    sortMode = SortMode.ARTIST_ASC
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }

                    IconButton(onClick = {
                        showSearchBar = !showSearchBar
                        if (!showSearchBar) searchQuery = ""
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.White)
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Actualizar biblioteca") },
                                onClick = {
                                    viewModel.manualRefreshLibrary()
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Acerca de") },
                                onClick = {
                                    showAbout = true
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (showSearchBar) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = { Text("Buscar por título o artista") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Black,
                            unfocusedIndicatorColor = Color.Black,
                            cursorColor = Color.Black,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color.Black)
                                }
                            }
                        }
                    )
                }

                // Lista de canciones
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues( start = 16.dp,
                        top = 16.dp,
                        bottom = 16.dp,
                        end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedSongs) { song ->
                        val isCurrent = playerState.currentSong?.id == song.id
                        var dragOffsetX by remember { mutableStateOf(0f) }
                        val density = androidx.compose.ui.platform.LocalDensity.current
                        var rowWidthPx by remember { mutableStateOf(0) }
                        val maxOffsetPx = if (rowWidthPx > 0) rowWidthPx.toFloat() else with(density) { 120.dp.toPx() }
                        val thresholdPx = if (rowWidthPx > 0) (rowWidthPx * 0.4f) else with(density) { 72.dp.toPx() }

                        val dragState = rememberDraggableState { delta ->
                            dragOffsetX = (dragOffsetX + delta).coerceIn(0f, maxOffsetPx)
                        }
                        val progress = (dragOffsetX / maxOffsetPx).coerceIn(0f, 1f)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { rowWidthPx = it.width }
                                .draggable(
                                    state = dragState,
                                    orientation = Orientation.Horizontal,
                                    onDragStopped = {
                                        if (dragOffsetX > thresholdPx) {
                                            viewModel.addToQueueNext(song)
                                        }
                                        dragOffsetX = 0f
                                    }
                                )
                        ) {
                            if (progress > 0f) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 68.dp)
                                            .graphicsLayer(
                                                alpha = progress,
                                                scaleX = 0.9f + 0.1f * progress,
                                                scaleY = 0.9f + 0.1f * progress,
                                                shape = RoundedCornerShape(12.dp),
                                                clip = true
                                            )
                                            .background(Color(0xFF2196F3))
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlaylistAdd,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        //Text("Añadir como siguiente", color = Color.White)
                                    }
                                }
                            }

                            val offsetDp = with(density) { dragOffsetX.toDp() }
                            Box(modifier = Modifier.offset(x = offsetDp)) {
                                SongItem(
                                    song = song,
                                    isPlaying = isCurrent && playerState.isPlaying,
                                    onClick = {
                                        viewModel.playSong(song)
                                        viewModel.startService(context, song)
                                        onOpenPlayer()
                                    },
                                    onQueueNext = { viewModel.addToQueueNext(song) },
                                    onQueueEnd = { viewModel.addToQueueEnd(song) }
                                )
                            }
                        }
                    }
                }
            }

            // Barra de scroll alfabético
            if (sortMode == SortMode.TITLE_ASC || sortMode == SortMode.TITLE_DESC) {
                val alphabet = listOf("#") + ('A'..'Z').map { it.toString() }
                var columnHeight by remember { mutableStateOf(0f) }
                val density = LocalDensity.current

                fun scrollToLetter(letter: String) {
                    currentScrollLetter = letter
                    scope.launch {
                        kotlinx.coroutines.delay(800)
                        currentScrollLetter = null
                    }
                    val index = if (letter == "#") {
                        sortedSongs.indexOfFirst {
                            val firstChar = it.title.firstOrNull()?.uppercaseChar()
                            firstChar == null || !firstChar.isLetter()
                        }
                    } else {
                        sortedSongs.indexOfFirst {
                            it.title.firstOrNull()?.uppercaseChar() == letter[0]
                        }
                    }
                    if (index >= 0) {
                        scope.launch {
                            listState.scrollToItem(index)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .width(28.dp)
                        .fillMaxHeight(0.75f)
                        .onGloballyPositioned { coords ->
                            columnHeight = coords.size.height.toFloat()
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val index = ((offset.y / columnHeight) * alphabet.size)
                                        .toInt()
                                        .coerceIn(0, alphabet.lastIndex)
                                    scrollToLetter(alphabet[index])
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val y = change.position.y.coerceIn(0f, columnHeight)
                                    val index = ((y / columnHeight) * alphabet.size)
                                        .toInt()
                                        .coerceIn(0, alphabet.lastIndex)
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
                            color = if (isActive) Color(0xFF2196F3) else Color.White.copy(alpha = 0.7f),
                            fontSize = if (isActive) 12.sp else 10.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clickable { scrollToLetter(letter) }
                                .padding(vertical = 1.5.dp)
                        )
                    }
                }
            }

            // Overlay de letra grande para feedback
            currentScrollLetter?.let { letter ->
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(100.dp)
                        .background(
                            Color.Black.copy(alpha = 0.8f),
                            RoundedCornerShape(16.dp)
                        )
                        .border(2.dp, Color(0xFF2196F3), RoundedCornerShape(16.dp)),
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

            if (showPlayerScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                ) {
                    PlayerScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closePlayerScreen() }
                    )
                }
            }

            if (showAbout) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                ) {
                    AboutScreen(
                        onBack = { showAbout = false }
                    )
                }
            }
        }
        }
    }
}

@Composable
fun MainMusicScreen(onOpenPlayer: () -> Unit) {
    StoragePermissionHandler {
        val vm: MainViewModel = MainViewModel.instance ?: viewModel()
        MusicScreen(vm, onOpenPlayer)
    }
}



fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return "%02d:%02d".format(minutes, seconds)
}

private enum class SortMode { TITLE_ASC, TITLE_DESC, ARTIST_ASC }

private fun SortMode.next(): SortMode = when (this) {
    SortMode.TITLE_ASC -> SortMode.TITLE_DESC
    SortMode.TITLE_DESC -> SortMode.ARTIST_ASC
    SortMode.ARTIST_ASC -> SortMode.TITLE_ASC
}