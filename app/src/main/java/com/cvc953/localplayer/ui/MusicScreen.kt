@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.cvc953.localplayer.ui

import MiniPlayer
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.cvc953.localplayer.ui.components.AlphabetScrollerContent
import com.cvc953.localplayer.ui.components.ScrollLetterDisplay
import com.cvc953.localplayer.ui.navigation.BottomNavItem
import com.cvc953.localplayer.ui.theme.LocalExtendedColors
import com.cvc953.localplayer.util.StoragePermissionHandler
import com.cvc953.localplayer.viewmodel.AlbumViewModel
import com.cvc953.localplayer.viewmodel.ArtistViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlayerViewModel
import com.cvc953.localplayer.viewmodel.PlaylistViewModel
import com.cvc953.localplayer.viewmodel.SongViewModel
import kotlinx.coroutines.launch

@Suppress("ktlint:standard:function-naming")
@Composable
fun SongsContent(
    songViewModel: SongViewModel,
    playbackViewModel: PlaybackViewModel,
    playlistViewModel: PlaylistViewModel,
    playerViewModel: PlayerViewModel,
) {
    val songs by songViewModel.songs.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()
    val playerState by playbackViewModel.playerState.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(SortMode.TITLE_ASC) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    // About visibility moved to ViewModel so shell UI can react
    var menuExpanded by remember { mutableStateOf(false) }
    val showAbout by playerViewModel.isAboutVisible.collectAsState()
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

    // val playerState by playerViewModel.playerState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var currentScrollLetter by remember { mutableStateOf<String?>(null) }

    val isScanning by songViewModel.isScanning.collectAsState()

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
                                    songViewModel.manualRefreshLibrary()
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
                                    // Redirigir a navegación de settings si es necesario
                                    menuExpanded = false
                                    playerViewModel.showSettings(true)
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
                                    // Redirigir a navegación de about si es necesario
                                    menuExpanded = false
                                    playerViewModel.showAbout(true)
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
                Box(modifier = Modifier.fillMaxSize()) {
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
                            val dragOffsetX = remember { Animatable(0f) }
                            val itemScope = rememberCoroutineScope()
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
                                    itemScope.launch {
                                        dragOffsetX.snapTo(
                                            (dragOffsetX.value + delta).coerceIn(
                                                0f,
                                                maxOffsetPx,
                                            ),
                                        )
                                    }
                                }
                            val progress =
                                (dragOffsetX.value / maxOffsetPx).coerceIn(0f, 1f)

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
                                                itemScope.launch {
                                                    if (dragOffsetX.value > thresholdPx) {
                                                        playbackViewModel.addToQueueNext(song)
                                                        dragOffsetX.animateTo(
                                                            maxOffsetPx,
                                                            animationSpec = tween(300),
                                                        )
                                                        dragOffsetX.snapTo(0f)
                                                    } else {
                                                        dragOffsetX.animateTo(
                                                            0f,
                                                            animationSpec = tween(200),
                                                        )
                                                    }
                                                }
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
                                        val iconWidth = with(density) { 24.dp.toPx() }
                                        val spacerWidth = with(density) { 40.dp.toPx() }
                                        val iconTriggerOffset = iconWidth + spacerWidth
                                        val iconOffsetPx =
                                            if (dragOffsetX.value > iconTriggerOffset) {
                                                dragOffsetX.value - iconTriggerOffset
                                            } else {
                                                0f
                                            }
                                        val iconOffsetDp = with(density) { iconOffsetPx.toDp() }

                                        Row(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(
                                                        min =
                                                            68.dp,
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
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .offset(x = iconOffsetDp),
                                            ) {
                                                Icon(
                                                    imageVector =
                                                        Icons.AutoMirrored.Filled.QueueMusic,
                                                    contentDescription =
                                                    null,
                                                    tint =
                                                        Color.White,
                                                    modifier = Modifier.size(24.dp),
                                                )
                                            }
                                        }
                                    }
                                }

                                val offsetDp =
                                    with(density) { dragOffsetX.value.toDp() }
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
                                            playbackViewModel.updateDisplayOrder(sortedSongs)
                                            playbackViewModel.play(song)
                                            // Ensure player UI is shown
                                            // playerViewModel.showPlayerScreen(true)
                                            // Si es necesario, iniciar servicio desde playbackViewModel
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
                    // Barra de scroll alfabético
                    if (sortMode == SortMode.TITLE_ASC ||
                        sortMode == SortMode.TITLE_DESC ||
                        sortMode == SortMode.ARTIST_ASC
                    ) {
                        AlphabetScrollerContent(
                            items = sortedSongs,
                            getItemName = { song ->
                                when (sortMode) {
                                    SortMode.ARTIST_ASC -> song.artist
                                    else -> song.title
                                }
                            },
                            currentScrollLetter = currentScrollLetter,
                            onLetterSelected = { letter ->
                                currentScrollLetter = letter
                            },
                            onScrollToIndex = { index, _ ->
                                scope.launch {
                                    listState.scrollToItem(index)
                                }
                            },
                            viewAsGrid = false,
                            scope = scope,
                        )
                    }

                    // Overlay de letra grande para feedback
                    currentScrollLetter?.let { letter ->
                        ScrollLetterDisplay(letter = letter)
                    }
                }
            }

            if (showAbout) {
                Box(modifier = Modifier.fillMaxSize().zIndex(2f)) {
                    AboutScreen(onBack = { playerViewModel.showAbout(false) })
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun MainMusicScreen(onOpenPlayer: () -> Unit) {
    StoragePermissionHandler {
        val context = LocalContext.current
        val appPrefs = AppPrefs(context)
        val songViewModel: SongViewModel = viewModel()
        val playbackViewModel: PlaybackViewModel = viewModel()
        val playlistViewModel: PlaylistViewModel = viewModel()
        val playerViewModel: PlayerViewModel = viewModel()
        val artistViewModel: ArtistViewModel = viewModel()
        val albumViewModel: AlbumViewModel = viewModel()
        val mainViewModel: com.cvc953.localplayer.viewmodel.MainViewModel = viewModel()
        val equalizerViewModel: com.cvc953.localplayer.viewmodel.EqualizerViewModel = viewModel()

        var needPicker by remember { mutableStateOf(!appPrefs.hasMusicFolderUri()) }
        var selectedArtistSongsView by rememberSaveable { mutableStateOf(false) }
        val showAbout by playerViewModel.isAboutVisible.collectAsState()
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
                    // Usar mainViewModel para que actualice la lista de carpetas configuradas
                    mainViewModel.addMusicFolder(uri.toString())
                    needPicker = false
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
            var selectedAlbumName by rememberSaveable { mutableStateOf<String?>(null) }
            var selectedArtistName by rememberSaveable { mutableStateOf<String?>(null) }
            var selectedPlaylistName by rememberSaveable { mutableStateOf<String?>(null) }
            val playerState by playbackViewModel.playerState.collectAsState()
            val showPlayerScreen by playerViewModel.isPlayerScreenVisible.collectAsState()
            val showSettings by playerViewModel.isSettingsVisible.collectAsState()
            val activity = context as? Activity
            var lastBackPressTime by remember { mutableStateOf(0L) }

            var hasSentPlaybackIntent by remember { mutableStateOf(false) }
            LaunchedEffect(playerState.isPlaying) {
                if (hasSentPlaybackIntent) {
                    val intent =
                        Intent(
                            context,
                            com.cvc953.localplayer.services.MusicService::class.java,
                        ).apply {
                            action = com.cvc953.localplayer.services.MusicService.ACTION_UPDATE_STATE
                            putExtra("IS_PLAYING", playerState.isPlaying)
                        }
                    androidx.core.content.ContextCompat
                        .startForegroundService(context, intent)
                } else {
                    hasSentPlaybackIntent = true
                }
            }

            BackHandler {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < 1000) {
                    activity?.finish()
                } else {
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

            Box(modifier = Modifier.fillMaxSize()) {
                val showEqualizer by equalizerViewModel.isEqualizerVisible.collectAsState()

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        Column(
                            modifier =
                                Modifier.pointerInput(showPlayerScreen || showSettings || showAbout) {
                                    if (showPlayerScreen || showSettings || showAbout) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                awaitPointerEvent()
                                            }
                                        }
                                    }
                                },
                        ) {
                            if (playerState.currentSong != null) {
                                MiniPlayer(
                                    song = playerState.currentSong!!,
                                    isPlaying = playerState.isPlaying,
                                    onPlayPause = { playbackViewModel.togglePlayPause() },
                                    onClick = { playerViewModel.openPlayerScreen() },
                                    onNext = { playbackViewModel.playNextSong() },
                                )
                            }
                            NavigationBar(
                                containerColor = LocalExtendedColors.current.surfaceSheet,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ) {
                                navItems.forEach { item ->
                                    NavigationBarItem(
                                        icon = {
                                            Icon(item.icon, contentDescription = item.title)
                                        },
                                        label = { Text(item.title) },
                                        selected = selectedTab == item.route,
                                        onClick = {
                                            selectedTab = item.route
                                            if (item.route != BottomNavItem.Albums.route) selectedAlbumName = null
                                            if (item.route != BottomNavItem.Artists.route) selectedArtistName = null
                                            if (item.route != BottomNavItem.Playlists.route) selectedPlaylistName = null
                                        },
                                        colors =
                                            NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.background,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                indicatorColor = MaterialTheme.colorScheme.primary,
                                            ),
                                    )
                                }
                            }
                        }
                    },
                ) { padding ->
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.onBackground),
                    ) {
                        when (selectedTab) {
                            BottomNavItem.Songs.route -> {
                                SongsContent(
                                    songViewModel = songViewModel,
                                    playbackViewModel = playbackViewModel,
                                    playlistViewModel = playlistViewModel,
                                    playerViewModel = playerViewModel,
                                )
                            }

                            BottomNavItem.Albums.route -> {
                                // Usar SIEMPRE la misma instancia de AlbumViewModel
                                val albumViewModel: AlbumViewModel =
                                    androidx.lifecycle.viewmodel.compose
                                        .viewModel()
                                val albumKey = selectedAlbumName
                                if (albumKey == null) {
                                    AlbumsScreen(
                                        albumViewModel = albumViewModel,
                                        playbackViewModel = playbackViewModel,
                                        onAlbumClick = { albumName, artistName ->
                                            // Ensure AlbumViewModel loads songs for this album
                                            val found =
                                                albumViewModel.albums.value.find {
                                                    it.name.equals(albumName, ignoreCase = true) &&
                                                        it.artist.equals(artistName, ignoreCase = true)
                                                }
                                            if (found != null) {
                                                albumViewModel.selectAlbum(found)
                                            } else {
                                                albumViewModel.selectAlbum(
                                                    com.cvc953.localplayer.model
                                                        .Album(albumName, artistName, 0),
                                                )
                                            }
                                            selectedAlbumName = "$albumName|$artistName"
                                        },
                                    )
                                } else {
                                    val parts = albumKey.split("|")
                                    val albumName = parts[0]
                                    val artistName = parts.getOrElse(1) { "Desconocido" }
                                    AlbumDetailScreen(
                                        albumViewModel = albumViewModel,
                                        playbackViewModel = playbackViewModel,
                                        playlistViewModel = playlistViewModel,
                                        albumName = albumName,
                                        artistName = artistName,
                                        onBack = { selectedAlbumName = null },
                                    )
                                }
                            }

                            BottomNavItem.Artists.route -> {
                                val artistName = selectedArtistName
                                if (artistName == null) {
                                    ArtistsScreen(
                                        artistViewModel = artistViewModel,
                                        playbackViewModel = playbackViewModel,
                                        onArtistClick = { selectedArtistName = it },
                                    )
                                } else {
                                    if (selectedArtistName != null && selectedTab == BottomNavItem.Artists.route &&
                                        selectedArtistSongsView
                                    ) {
                                        ArtistSongsScreen(
                                            artistViewModel = artistViewModel,
                                            artistName = artistName,
                                            onBack = { selectedArtistSongsView = false },
                                        )
                                    } else {
                                        ArtistDetailScreen(
                                            artistViewModel = artistViewModel,
                                            playbackViewModel = playbackViewModel,
                                            playlistViewModel = playlistViewModel,
                                            artistName = artistName,
                                            onBack = { selectedArtistName = null },
                                            onAlbumClick = { albumName, artistName ->
                                                val found =
                                                    albumViewModel.albums.value.find {
                                                        it.name.equals(albumName, ignoreCase = true) &&
                                                            it.artist.equals(artistName, ignoreCase = true)
                                                    }
                                                if (found != null) {
                                                    albumViewModel.selectAlbum(found)
                                                } else {
                                                    albumViewModel.selectAlbum(
                                                        com.cvc953.localplayer.model
                                                            .Album(albumName, artistName, 0),
                                                    )
                                                }
                                                selectedAlbumName = "$albumName|$artistName"
                                                selectedTab = BottomNavItem.Albums.route
                                            },
                                            onViewAllSongs = {
                                                selectedArtistSongsView = true
                                            },
                                        )
                                    }
                                }
                            }

                            BottomNavItem.Playlists.route -> {
                                val playlistName = selectedPlaylistName
                                if (playlistName == null) {
                                    PlaylistsScreen(
                                        playlistViewModel = playlistViewModel,
                                        onPlaylistClick = { selectedPlaylistName = it },
                                        playbackViewModel = playbackViewModel,
                                    )
                                } else {
                                    PlaylistDetailScreen(
                                        playlistViewModel = playlistViewModel,
                                        playlistName = playlistName,
                                        onBack = { selectedPlaylistName = null },
                                        playbackViewModel = playbackViewModel,
                                    )
                                }
                            }
                        }
                    }
                } // end Box container

                if (showPlayerScreen) {
                    Box(modifier = Modifier.fillMaxSize().zIndex(1f)) {
                        PlayerScreen(
                            onBack = { playerViewModel.closePlayerScreen() },
                            onNavigateToArtist = { artistName ->
                                playerViewModel.closePlayerScreen()
                                selectedTab = BottomNavItem.Artists.route
                                selectedArtistName = artistName
                            },
                            onNavigateToAlbum = { albumName, artistName ->
                                playerViewModel.closePlayerScreen()
                                val found =
                                    albumViewModel.albums.value.find {
                                        it.name.equals(albumName, ignoreCase = true) &&
                                            it.artist.equals(artistName, ignoreCase = true)
                                    }
                                if (found != null) {
                                    albumViewModel.selectAlbum(found)
                                } else {
                                    albumViewModel.selectAlbum(
                                        com.cvc953.localplayer.model
                                            .Album(albumName, artistName, 0),
                                    )
                                }
                                selectedTab = BottomNavItem.Albums.route
                                selectedAlbumName = "$albumName|$artistName"
                            },
                        )
                    }
                }

                if (showEqualizer) {
                    Box(modifier = Modifier.fillMaxSize().zIndex(3f)) {
                        EqualizerScreen(viewModel = equalizerViewModel, onClose = { equalizerViewModel.closeEqualizerScreen() })
                    }
                } else if (showSettings) {
                    Box(modifier = Modifier.fillMaxSize().zIndex(2f)) {
                        SettingsScreen(
                            viewModel = mainViewModel,
                            equalizerViewModel = equalizerViewModel,
                            onClose = { playerViewModel.closeSettingsScreen() },
                        )
                    }
                }
            }
        } // end else branch
    } // end StoragePermissionHandler
} // end MainMusicScreen

/*@Suppress("ktlint:standard:function-naming")
@Composable
fun PlaylistDetailScreen(
    playlistViewModel: PlaylistViewModel,
    playlistName: String,
    onBack: () -> Unit,
    playbackViewModel: PlaybackViewModel,
) {
    TODO("Not yet implemented")
}*/

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
