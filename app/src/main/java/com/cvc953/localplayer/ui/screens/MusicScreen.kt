package com.cvc953.localplayer.ui.screens

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cvc953.localplayer.Services.MusicService
import com.cvc953.localplayer.model.Album
import com.cvc953.localplayer.preferences.AppPrefs
import com.cvc953.localplayer.ui.MiniPlayer
import com.cvc953.localplayer.ui.PlayerScreen
import com.cvc953.localplayer.ui.PlaylistsScreen
import com.cvc953.localplayer.ui.SettingsScreen
import com.cvc953.localplayer.ui.SongItem
import com.cvc953.localplayer.ui.components.AlphabetScrollerContent
import com.cvc953.localplayer.ui.components.DraggableSwipeRow
import com.cvc953.localplayer.ui.components.ScrollLetterDisplay
import com.cvc953.localplayer.ui.extendedColors
import com.cvc953.localplayer.ui.navigation.BottomNavItem
import com.cvc953.localplayer.ui.theme.LocalExtendedColors
import com.cvc953.localplayer.util.StoragePermissionHandler
import com.cvc953.localplayer.viewmodel.AlbumViewModel
import com.cvc953.localplayer.viewmodel.ArtistViewModel
import com.cvc953.localplayer.viewmodel.EqualizerViewModel
import com.cvc953.localplayer.viewmodel.FolderViewModel
import com.cvc953.localplayer.viewmodel.MainViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlayerViewModel
import com.cvc953.localplayer.viewmodel.PlaylistViewModel
import com.cvc953.localplayer.viewmodel.SongViewModel
import kotlinx.coroutines.launch
import kotlin.text.split

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
            modifier =
                Modifier.Companion
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Companion.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.Companion.height(16.dp))
            Text("Escaneando canciones", color = MaterialTheme.colorScheme.onSurface)
        }
    } else {
        Box(
            modifier =
                Modifier.Companion
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            Column(modifier = Modifier.Companion.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier.Companion.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.Companion.CenterVertically,
                ) {
                    Text(
                        text = "Canciones",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Companion.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.Companion.weight(1f),
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
                                Modifier.Companion.background(
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
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
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
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
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
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
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
                            Modifier.Companion
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
                Box(modifier = Modifier.Companion.fillMaxSize()) {
                    // Lista de canciones
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.Companion.fillMaxSize(),
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

                            DraggableSwipeRow(
                                onSwipeThreshold = {
                                    playbackViewModel.addToQueueNext(song)
                                    Toast
                                        .makeText(
                                            context,
                                            "Añadido como siguiente",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                },
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
                                        // playerViewModel.showPlayerScreen(true)
                                        // Si es necesario, iniciar servicio desde playbackViewModel
                                    },
                                    onQueueNext = {
                                        playbackViewModel.addToQueueNext(song)
                                        Toast
                                            .makeText(
                                                context,
                                                "Añadido como siguiente",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    },
                                    onQueueEnd = {
                                        playbackViewModel.addToQueueEnd(song)
                                        Toast
                                            .makeText(
                                                context,
                                                "Añadido al final de la cola",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    },
                                    playlists = playlists,
                                    onAddToPlaylist = { playlistName, songId ->
                                        playlistViewModel.addSongToPlaylist(playlistName, songId)
                                        Toast
                                            .makeText(
                                                context,
                                                "Añadido a '$playlistName'",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    },
                                )
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
                Box(modifier = Modifier.Companion.fillMaxSize().zIndex(2f)) {
                    AboutScreen(onBack = { playerViewModel.showAbout(false) })
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun MainMusicScreen(onOpenPlayer: () -> Unit) {
    val context = LocalContext.current
    val appPrefs = AppPrefs(context)
    val songViewModel: SongViewModel = viewModel()
    val playbackViewModel: PlaybackViewModel = viewModel()
    val playlistViewModel: PlaylistViewModel = viewModel()
    val playerViewModel: PlayerViewModel = viewModel()
    val artistViewModel: ArtistViewModel = viewModel()
    val albumViewModel: AlbumViewModel = viewModel()
    val mainViewModel: MainViewModel = viewModel()
    val equalizerViewModel: EqualizerViewModel = viewModel()
    val folderViewModel: FolderViewModel = viewModel()

    StoragePermissionHandler(
        isFolderConfiguredInitially = appPrefs.hasMusicFolderUri(),
        onFolderSelected = { uri ->
            folderViewModel.addMusicFolder(uri)
            songViewModel.manualRefreshLibrary()
            albumViewModel.loadAlbums()
            artistViewModel.loadArtists()
        },
        onSetupCompleted = {
            songViewModel.manualRefreshLibrary()
            albumViewModel.loadAlbums()
            artistViewModel.loadArtists()
        },
    ) {
        var selectedArtistSongsView by rememberSaveable { mutableStateOf(false) }
        val showAbout by playerViewModel.isAboutVisible.collectAsState()

        var selectedTab by rememberSaveable { mutableStateOf(BottomNavItem.Songs.route) }
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
                        MusicService::class.java,
                    ).apply {
                        action = MusicService.Companion.ACTION_UPDATE_STATE
                        putExtra("IS_PLAYING", playerState.isPlaying)
                    }
                ContextCompat
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

        Box(modifier = Modifier.Companion.fillMaxSize()) {
            val showEqualizer by equalizerViewModel.isEqualizerVisible.collectAsState()

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    Column(
                        modifier =
                            Modifier.Companion.pointerInput(showPlayerScreen || showSettings || showAbout) {
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
                                modifier = Modifier.Companion,
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
                                        if (item.route != BottomNavItem.Albums.route) {
                                            selectedAlbumName =
                                                null
                                        }
                                        if (item.route != BottomNavItem.Artists.route) {
                                            selectedArtistName =
                                                null
                                        }
                                        if (item.route != BottomNavItem.Playlists.route) {
                                            selectedPlaylistName =
                                                null
                                        }
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
                    modifier =
                        Modifier.Companion
                            .fillMaxSize()
                            .padding(padding)
                            .background(MaterialTheme.colorScheme.onBackground),
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
                                viewModel()
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
                                                    it.artist.equals(
                                                        artistName,
                                                        ignoreCase = true,
                                                    )
                                            }
                                        if (found != null) {
                                            albumViewModel.selectAlbum(found)
                                        } else {
                                            albumViewModel.selectAlbum(
                                                Album(albumName, artistName, 0),
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
                                if (selectedTab == BottomNavItem.Artists.route && selectedArtistSongsView) {
                                    ArtistSongsScreen(
                                        artistViewModel = artistViewModel,
                                        artistName = artistName,
                                        onBack = { selectedArtistSongsView = false },
                                        playbackViewModel = playbackViewModel,
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
                                                        it.artist.equals(
                                                            artistName,
                                                            ignoreCase = true,
                                                        )
                                                }
                                            if (found != null) {
                                                albumViewModel.selectAlbum(found)
                                            } else {
                                                albumViewModel.selectAlbum(
                                                    Album(albumName, artistName, 0),
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
                Box(modifier = Modifier.Companion.fillMaxSize().zIndex(1f)) {
                    val isPlaying = false
                    PlayerScreen(
                        mainViewModel = mainViewModel,
                        onCollapse = { playerViewModel.closePlayerScreen() },
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
                                    Album(albumName, artistName, 0),
                                )
                            }
                            selectedTab = BottomNavItem.Albums.route
                            selectedAlbumName = "$albumName|$artistName"
                        },
                    )
                }
            }

            if (showEqualizer) {
                Box(modifier = Modifier.Companion.fillMaxSize().zIndex(3f)) {
                    EqualizerScreen(
                        viewModel = equalizerViewModel,
                        onClose = { equalizerViewModel.closeEqualizerScreen() },
                    )
                }
            } else if (showSettings) {
                Box(modifier = Modifier.Companion.fillMaxSize().zIndex(2f)) {
                    SettingsScreen(
                        viewModel = mainViewModel,
                        equalizerViewModel = equalizerViewModel,
                        folderViewModel = folderViewModel,
                        onClose = { playerViewModel.closeSettingsScreen() },
                    )
                }
            }
        }
    } // end StoragePermissionHandler
} // end MainMusicScreen

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
