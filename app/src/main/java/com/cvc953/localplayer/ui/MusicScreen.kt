@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.cvc953.localplayer.ui

import MiniPlayer
import android.app.Activity
import android.content.Intent
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cvc953.localplayer.preferences.AppPrefs
import com.cvc953.localplayer.ui.navigation.BottomNavItem
import com.cvc953.localplayer.ui.theme.LocalExtendedColors
import com.cvc953.localplayer.ui.theme.md_overlay
import com.cvc953.localplayer.util.StoragePermissionHandler
import com.cvc953.localplayer.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Suppress("ktlint:standard:function-naming")
@Composable
fun SongsContent(viewModel: MainViewModel) {
    val songs by viewModel.songs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(SortMode.TITLE_ASC) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    // About visibility moved to ViewModel so shell UI can react
    var menuExpanded by remember { mutableStateOf(false) }

    val filteredSongs =
        remember(songs, searchQuery) {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) {
                songs
            } else {
                songs.filter { song ->
                    song.title.lowercase().contains(q) ||
                        song.artist.lowercase().contains(q)
                }
            }
        }

    val sortedSongs =
        remember(filteredSongs, sortMode) {
            when (sortMode) {
                SortMode.TITLE_ASC -> {
                    filteredSongs.sortedBy { it.title.lowercase() }
                }

                SortMode.TITLE_DESC -> {
                    filteredSongs.sortedByDescending { it.title.lowercase() }
                }

                SortMode.ARTIST_ASC -> {
                    filteredSongs.sortedBy { it.artist.lowercase() }
                }
            }
        }

    val playerState by viewModel.playerState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var currentScrollLetter by remember { mutableStateOf<String?>(null) }

    val isScanning by viewModel.isScanning

    if (isScanning) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Escaneando canciones", color = MaterialTheme.colorScheme.onSurface)
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Canciones",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )

                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(
                                Icons.Default.Sort,
                                contentDescription = "Ordenar",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = {
                                sortMenuExpanded = false
                            },
                            containerColor = MaterialTheme.extendedColors.surfaceSheet,
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Título A-Z",
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    sortMode =
                                        SortMode.TITLE_ASC
                                    sortMenuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Título Z-A",
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    sortMode =
                                        SortMode.TITLE_DESC
                                    sortMenuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Artista A-Z",
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    sortMode =
                                        SortMode.ARTIST_ASC
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
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }

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
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier =
                                Modifier.background(
                                    MaterialTheme.extendedColors.surfaceSheet,
                                ),
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Actualizar biblioteca",
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    viewModel
                                        .manualRefreshLibrary()
                                    menuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Ajustes",
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    viewModel.openSettingsScreen()
                                    menuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Acerca de",
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    viewModel.openAboutScreen()
                                    menuExpanded = false
                                },
                            )
                        }
                    }
                }

                if (showSearchBar) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = {
                            Text(
                                "Buscar por título o artista",
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor =
                                    MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor =
                                    MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor =
                                    MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor =
                                    MaterialTheme.colorScheme.outline,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                focusedLabelColor =
                                    MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription =
                                            "Limpiar",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        },
                    )
                }

                // Lista de canciones
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = 16.dp,
                            top = 16.dp,
                            bottom = 16.dp,
                            end = 4.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(sortedSongs) { song ->
                        val isCurrent =
                            playerState.currentSong?.id == song.id
                        var dragOffsetX by remember { mutableStateOf(0f) }
                        val density =
                            androidx.compose.ui.platform.LocalDensity
                                .current
                        var rowWidthPx by remember { mutableStateOf(0) }
                        val maxOffsetPx =
                            if (rowWidthPx > 0) {
                                rowWidthPx.toFloat()
                            } else {
                                with(density) { 120.dp.toPx() }
                            }
                        val thresholdPx =
                            if (rowWidthPx > 0) {
                                (rowWidthPx * 0.4f)
                            } else {
                                with(density) { 72.dp.toPx() }
                            }

                        val dragState =
                            rememberDraggableState { delta ->
                                dragOffsetX =
                                    (dragOffsetX + delta).coerceIn(
                                        0f,
                                        maxOffsetPx,
                                    )
                            }
                        val progress =
                            (dragOffsetX / maxOffsetPx).coerceIn(0f, 1f)

                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged {
                                        rowWidthPx =
                                            it.width
                                    }.draggable(
                                        state = dragState,
                                        orientation =
                                            Orientation
                                                .Horizontal,
                                        onDragStopped = {
                                            if (dragOffsetX >
                                                thresholdPx
                                            ) {
                                                viewModel
                                                    .addToQueueNext(
                                                        song,
                                                    )
                                            }
                                            dragOffsetX =
                                                0f
                                        },
                                    ),
                        ) {
                            if (progress > 0f) {
                                Box(
                                    modifier =
                                        Modifier
                                            .matchParentSize()
                                            .padding(
                                                horizontal =
                                                    8.dp,
                                                vertical =
                                                    4.dp,
                                            ),
                                    contentAlignment =
                                        Alignment
                                            .CenterStart,
                                ) {
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .heightIn(
                                                    min =
                                                        68.dp,
                                                ).graphicsLayer(
                                                    alpha =
                                                    progress,
                                                    scaleX =
                                                        0.9f +
                                                            0.1f *
                                                            progress,
                                                    scaleY =
                                                        0.9f +
                                                            0.1f *
                                                            progress,
                                                    shape =
                                                        RoundedCornerShape(
                                                            12.dp,
                                                        ),
                                                    clip =
                                                    true,
                                                ).background(
                                                    MaterialTheme.colorScheme.primary,
                                                ).padding(
                                                    horizontal =
                                                        16.dp,
                                                    vertical =
                                                        12.dp,
                                                ),
                                        verticalAlignment =
                                            Alignment
                                                .CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector =
                                                Icons.Default
                                                    .PlaylistAdd,
                                            contentDescription =
                                            null,
                                            tint =
                                                MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(
                                            Modifier.width(
                                                10.dp,
                                            ),
                                        )
                                        // Text("Añadir como
                                        // siguiente", color
                                        // = Color.White)
                                    }
                                }
                            }

                            val offsetDp =
                                with(density) { dragOffsetX.toDp() }
                            Box(
                                modifier =
                                    Modifier.offset(
                                        x = offsetDp,
                                    ),
                            ) {
                                SongItem(
                                    song = song,
                                    isPlaying =
                                        isCurrent &&
                                            playerState
                                                .isPlaying,
                                    onClick = {
                                        // Usar el orden
                                        // actual solo al
                                        // reproducir desde
                                        // esta vista
                                        viewModel
                                            .updateDisplayOrder(
                                                sortedSongs,
                                            )
                                        viewModel.playSong(
                                            song,
                                        )
                                        viewModel
                                            .startService(
                                                context,
                                                song,
                                            )
                                    },
                                    onQueueNext = {
                                        viewModel
                                            .addToQueueNext(
                                                song,
                                            )
                                    },
                                    onQueueEnd = {
                                        viewModel
                                            .addToQueueEnd(
                                                song,
                                            )
                                    },
                                    playlists = playlists,
                                    onAddToPlaylist = {
                                        playlistName,
                                        songId,
                                        ->
                                        viewModel
                                            .addSongToPlaylist(
                                                playlistName,
                                                songId,
                                            )
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Barra de scroll alfabético
            if (sortMode == SortMode.TITLE_ASC ||
                sortMode == SortMode.TITLE_DESC ||
                sortMode == SortMode.ARTIST_ASC
            ) {
                val alphabet = listOf("#") + ('A'..'Z').map { it.toString() }
                var columnHeight by remember { mutableStateOf(0f) }
                val density = LocalDensity.current

                fun scrollToLetter(letter: String) {
                    currentScrollLetter = letter
                    scope.launch {
                        kotlinx.coroutines.delay(800)
                        currentScrollLetter = null
                    }
                    val index =
                        if (letter == "#") {
                            sortedSongs.indexOfFirst {
                                val firstChar =
                                    when (sortMode) {
                                        SortMode.ARTIST_ASC -> {
                                            it.artist
                                                .firstOrNull()
                                                ?.uppercaseChar()
                                        }

                                        else -> {
                                            it.title
                                                .firstOrNull()
                                                ?.uppercaseChar()
                                        }
                                    }
                                firstChar == null ||
                                    !firstChar.isLetter()
                            }
                        } else {
                            sortedSongs.indexOfFirst {
                                val firstChar =
                                    when (sortMode) {
                                        SortMode.ARTIST_ASC -> {
                                            it.artist
                                                .firstOrNull()
                                                ?.uppercaseChar()
                                        }

                                        else -> {
                                            it.title
                                                .firstOrNull()
                                                ?.uppercaseChar()
                                        }
                                    }
                                firstChar == letter[0]
                            }
                        }
                    if (index >= 0) {
                        scope.launch { listState.scrollToItem(index) }
                    }
                }

                Column(
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .width(28.dp)
                            .fillMaxHeight(0.8f)
                            .onGloballyPositioned { coords ->
                                columnHeight =
                                    coords.size.height.toFloat()
                            }.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val index =
                                            (
                                                (
                                                    offset.y /
                                                        columnHeight
                                                ) *
                                                    alphabet.size
                                            ).toInt()
                                                .coerceIn(
                                                    0,
                                                    alphabet.lastIndex,
                                                )
                                        scrollToLetter(
                                            alphabet[
                                                index,
                                            ],
                                        )
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val y =
                                            change.position
                                                .y
                                                .coerceIn(
                                                    0f,
                                                    columnHeight,
                                                )
                                        val index =
                                            (
                                                (
                                                    y /
                                                        columnHeight
                                                ) *
                                                    alphabet.size
                                            ).toInt()
                                                .coerceIn(
                                                    0,
                                                    alphabet.lastIndex,
                                                )
                                        scrollToLetter(
                                            alphabet[
                                                index,
                                            ],
                                        )
                                    },
                                )
                            },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
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
                            fontSize = if (isActive) 11.sp else 9.sp,
                            fontWeight =
                                if (isActive) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Medium
                                },
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier
                                    .clickable {
                                        scrollToLetter(
                                            letter,
                                        )
                                    }, // .padding(vertical = 10.dp),
                        )
                    }
                }
            }

            // Overlay de letra grande para feedback
            currentScrollLetter?.let { letter ->
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .size(
                                with(LocalDensity.current) {
                                    (
                                        LocalConfiguration.current
                                            .screenWidthDp
                                            .dp * 0.25f
                                    )
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

            val showAbout by viewModel.isAboutVisible.collectAsState()
            if (showAbout) {
                Box(modifier = Modifier.fillMaxSize().zIndex(2f)) {
                    AboutScreen(onBack = { viewModel.closeAboutScreen() })
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun MainMusicScreen(onOpenPlayer: () -> Unit) {
    StoragePermissionHandler {
        val vm: MainViewModel = MainViewModel.instance ?: viewModel()
        val context = LocalContext.current
        val appPrefs = AppPrefs(context)

        // If user hasn't picked a music folder yet, show a chooser overlay
        var needPicker by remember { mutableStateOf(!appPrefs.hasMusicFolderUri()) }
        val launcher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree(),
            ) { uri ->
                android.util.Log.d("MainMusicScreen", "OpenDocumentTree returned: $uri")
                if (uri != null) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                    } catch (e: Exception) {
                        android.util.Log.w("MainMusicScreen", "takePersistableUriPermission failed", e)
                    }
                    android.util.Log.d("MainMusicScreen", "Storing music folder uri and starting scan: $uri")
                    appPrefs.setMusicFolderUri(uri.toString())
                    // hide picker overlay and trigger scan now that folder is set
                    needPicker = false
                    try {
                        vm.manualRefreshLibrary()
                    } catch (e: Exception) {
                        android.util.Log.e("MainMusicScreen", "manualRefreshLibrary error", e)
                    }
                    try {
                        android.widget.Toast
                            .makeText(
                                context,
                                "Carpeta seleccionada",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                    } catch (_: Exception) {
                    }
                }
            }

        var selectedTab by rememberSaveable { mutableStateOf(BottomNavItem.Songs.route) }
        if (needPicker) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Selecciona la carpeta donde está tu música",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { launcher.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) { Text("Elegir carpeta") }
                }
            }
        } else {
            var selectedAlbumName by remember { mutableStateOf<String?>(null) }
            var selectedArtistName by remember { mutableStateOf<String?>(null) }
            var selectedPlaylistName by remember { mutableStateOf<String?>(null) }
            val playerState by vm.playerState.collectAsState()
            val showPlayerScreen by vm.isPlayerScreenVisible.collectAsState()
            val showSettings by vm.isSettingsVisible.collectAsState()
            val showAbout by vm.isAboutVisible.collectAsState()
            val showEqualizer by vm.isEqualizerVisible.collectAsState()
            val activity = context as? Activity
            var lastBackPressTime by remember { mutableStateOf(0L) }

            // Actualizar el servicio cuando cambia el estado del player
            LaunchedEffect(playerState.isPlaying) {
                val intent =
                    Intent(
                        context,
                        com.cvc953.localplayer.services.MusicService::class
                            .java,
                    ).apply {
                        action =
                            com.cvc953.localplayer.services.MusicService
                                .ACTION_UPDATE_STATE
                        putExtra("IS_PLAYING", playerState.isPlaying)
                    }
                androidx.core.content.ContextCompat
                    .startForegroundService(context, intent)
            }

            BackHandler {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < 1000) {
                    // Segunda pulsación dentro de 1 segundo
                    activity?.finish()
                } else {
                    // Primera pulsación
                    lastBackPressTime = currentTime
                    Toast
                        .makeText(
                            context,
                            "Presiona de nuevo para salir",
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }

            val navItems =
                listOf(
                    BottomNavItem.Songs,
                    BottomNavItem.Albums,
                    BottomNavItem.Artists,
                    BottomNavItem.Playlists,
                )

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (!showPlayerScreen && !showSettings && !showAbout && !showEqualizer) {
                        Column {
                            // MiniPlayer arriba del BottomNavigationBar
                            if (playerState.currentSong != null) {
                                MiniPlayer(
                                    song = playerState.currentSong!!,
                                    isPlaying = playerState.isPlaying,
                                    onPlayPause = {
                                        vm.togglePlayPause()
                                    },
                                    onClick = { vm.openPlayerScreen() },
                                    onNext = { vm.playNextSong() },
                                )
                            }
                            // Bottom Navigation Bar
                            NavigationBar(
                                containerColor = LocalExtendedColors.current.surfaceSheet,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ) {
                                navItems.forEach { item ->
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                item.icon,
                                                contentDescription =
                                                    item.title,
                                            )
                                        },
                                        label = {
                                            Text(item.title)
                                        },
                                        selected =
                                            selectedTab ==
                                                item.route,
                                        onClick = {
                                            selectedTab =
                                                item.route
                                            if (item.route !=
                                                BottomNavItem
                                                    .Albums
                                                    .route
                                            ) {
                                                selectedAlbumName =
                                                    null
                                            }
                                            if (item.route !=
                                                BottomNavItem
                                                    .Artists
                                                    .route
                                            ) {
                                                selectedArtistName =
                                                    null
                                            }
                                            if (item.route !=
                                                BottomNavItem
                                                    .Playlists
                                                    .route
                                            ) {
                                                selectedPlaylistName =
                                                    null
                                            }
                                        },
                                        colors =
                                            NavigationBarItemDefaults
                                                .colors(
                                                    selectedIconColor =
                                                        MaterialTheme.colorScheme.background,
                                                    selectedTextColor =
                                                        MaterialTheme.colorScheme.primary,
                                                    unselectedIconColor =
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                    unselectedTextColor =
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                    indicatorColor =
                                                        MaterialTheme.colorScheme.primary,
                                                ),
                                    )
                                }
                            }
                        }
                    }
                },
            ) { padding ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(MaterialTheme.colorScheme.onBackground),
                ) {
                    // Contenido según la pestaña seleccionada
                    when (selectedTab) {
                        BottomNavItem.Songs.route -> {
                            SongsContent(vm)
                        }

                        BottomNavItem.Albums.route -> {
                            val albumName = selectedAlbumName
                            if (albumName == null) {
                                AlbumsScreen(
                                    viewModel = vm,
                                    onAlbumClick = {
                                        selectedAlbumName = it
                                    },
                                )
                            } else {
                                AlbumDetailScreen(
                                    viewModel = vm,
                                    albumName = albumName,
                                    onBack = {
                                        selectedAlbumName = null
                                    },
                                )
                            }
                        }

                        BottomNavItem.Artists.route -> {
                            val artistName = selectedArtistName
                            if (artistName == null) {
                                ArtistsScreen(
                                    viewModel = vm,
                                    onArtistClick = {
                                        selectedArtistName = it
                                    },
                                )
                            } else {
                                ArtistDetailScreen(
                                    viewModel = vm,
                                    artistName = artistName,
                                    onBack = {
                                        selectedArtistName = null
                                    },
                                )
                            }
                        }

                        BottomNavItem.Playlists.route -> {
                            val playlistName = selectedPlaylistName
                            if (playlistName == null) {
                                PlaylistsScreen(
                                    viewModel = vm,
                                    onPlaylistClick = {
                                        selectedPlaylistName = it
                                    },
                                )
                            } else {
                                PlaylistDetailScreen(
                                    viewModel = vm,
                                    playlistName = playlistName,
                                    onBack = {
                                        selectedPlaylistName = null
                                    },
                                )
                            }
                        }
                    }

                    // PlayerScreen sobre todo el contenido
                    if (showPlayerScreen) {
                        Box(modifier = Modifier.fillMaxSize().zIndex(1f)) {
                            PlayerScreen(
                                viewModel = vm,
                                onBack = { vm.closePlayerScreen() },
                                onNavigateToArtist = { artistName ->
                                    vm.closePlayerScreen()
                                    selectedTab =
                                        BottomNavItem.Artists.route
                                    selectedArtistName = artistName
                                },
                                onNavigateToAlbum = { albumName ->
                                    vm.closePlayerScreen()
                                    selectedTab =
                                        BottomNavItem.Albums.route
                                    selectedAlbumName = albumName
                                },
                            )
                        }
                    }
                    // Settings screen overlay
                    if (showSettings) {
                        Box(modifier = Modifier.fillMaxSize().zIndex(2f)) {
                            SettingsScreen(
                                viewModel = vm,
                                onClose = {
                                    vm
                                        .closeSettingsScreen()
                                },
                            )
                            if (vm.isEqualizerVisible
                                    .collectAsState()
                                    .value
                            ) {
                                EqualizerScreen(
                                    viewModel = vm,
                                    onClose = {
                                        vm
                                            .closeEqualizerScreen()
                                    },
                                )
                            }
                        }
                    }
                }
            } // end Box container
        } // end Scaffold
    } // end else branch
} // end StoragePermissionHandler

fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return "%02d:%02d".format(minutes, seconds)
}

private enum class SortMode {
    TITLE_ASC,
    TITLE_DESC,
    ARTIST_ASC,
}

private fun SortMode.next(): SortMode =
    when (this) {
        SortMode.TITLE_ASC -> SortMode.TITLE_DESC
        SortMode.TITLE_DESC -> SortMode.ARTIST_ASC
        SortMode.ARTIST_ASC -> SortMode.TITLE_ASC
    }
