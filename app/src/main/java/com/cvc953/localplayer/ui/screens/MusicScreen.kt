package com.cvc953.localplayer.ui.screens

import android.app.Activity
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.cvc953.localplayer.R
import com.cvc953.localplayer.preferences.AppPrefs
import com.cvc953.localplayer.ui.MiniPlayer
import com.cvc953.localplayer.ui.components.BottomNavigationBar
import com.cvc953.localplayer.ui.navigation.AppNavigation
import com.cvc953.localplayer.ui.navigation.Screen
import com.cvc953.localplayer.viewmodel.AlbumViewModel
import com.cvc953.localplayer.viewmodel.ArtistViewModel
import com.cvc953.localplayer.viewmodel.EqualizerViewModel
import com.cvc953.localplayer.viewmodel.FolderViewModel
import com.cvc953.localplayer.viewmodel.GenreViewModel
import com.cvc953.localplayer.viewmodel.MainViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlayerViewModel
import com.cvc953.localplayer.viewmodel.PlaylistViewModel
import com.cvc953.localplayer.viewmodel.SongViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(audioFileUri: String? = null, onOpenPlayer: () -> Unit) {
    val context = LocalContext.current
    val appPrefs = AppPrefs(context)
    val songViewModel: SongViewModel = viewModel()
    val playbackViewModel: PlaybackViewModel = viewModel()
    val playlistViewModel: PlaylistViewModel = viewModel()
    val playerViewModel: PlayerViewModel = viewModel()
    val artistViewModel: ArtistViewModel = viewModel()
    val albumViewModel: AlbumViewModel = viewModel()
    val genreViewModel: GenreViewModel = viewModel()
    val mainViewModel: MainViewModel = viewModel()
    val equalizerViewModel: EqualizerViewModel = viewModel()
    val folderViewModel: FolderViewModel = viewModel()

    // Handle audio file opened from external file manager
    LaunchedEffect(audioFileUri) {
        if (audioFileUri != null) {
            try {
                val uri = android.net.Uri.parse(audioFileUri)

                // --- Strategy 1: resolve to file path and match against loaded songs ---
                val filePath = resolveAudioFilePath(context, uri)
                var song =
                    filePath?.let { path ->
                        songViewModel.songs.value.find { it.filePath == path }
                    }

                // --- Strategy 2: direct MediaStore query (works for content://media URIs) ---
                if (song == null) {
                    song = querySongFromMediaStore(context, uri)
                }

                // --- Strategy 3: fallback — minimal Song from URI, ExoPlayer can play it anyway ---
                if (song == null) {
                    val title =
                        uri.lastPathSegment
                            ?.substringAfterLast('/')
                            ?.substringBeforeLast('.')
                            ?: uri.toString().substringAfterLast('/').substringBeforeLast('.')
                    song =
                        com.cvc953.localplayer.model.Song(
                            id = -1L,
                            title = title,
                            artist = "",
                            album = "",
                            uri = uri,
                            duration = 0L,
                            filePath = filePath,
                            year = null,
                            trackNumber = 0,
                            discNumber = 0,
                        )
                }

                playbackViewModel.play(song)
            } catch (_: Exception) {
            }
        }
    }

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
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()

        val playerState by playbackViewModel.playerState.collectAsState()
        val showSettings by playerViewModel.isSettingsVisible.collectAsState()
        val showEqualizer by equalizerViewModel.isEqualizerVisible.collectAsState()
        val showAbout by playerViewModel.isAboutVisible.collectAsState()
        val defaultStartTab by mainViewModel.defaultStartTab.collectAsState()
        val songsTabEnabled by mainViewModel.songsTabEnabled.collectAsState()
        val albumsTabEnabled by mainViewModel.albumsTabEnabled.collectAsState()
        val artistsTabEnabled by mainViewModel.artistsTabEnabled.collectAsState()
        val playlistsTabEnabled by mainViewModel.playlistsTabEnabled.collectAsState()
        val genresTabEnabled by mainViewModel.genresTabEnabled.collectAsState()
        val activity = context as? Activity
        var lastBackPressTime by remember { mutableLongStateOf(0L) }

        val sheetState =
            rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded,
                skipHiddenState = true,
            )
        val scaffoldState =
            rememberBottomSheetScaffoldState(
                bottomSheetState = sheetState,
            )

        val bottomNavHeight = 80.dp

        // Polling del offset del sheet para animación progresiva
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current

        val navBarBottomHeightDp = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }

        // Peek = MiniPlayer + BottomNavBar + navigation bar padding
        val sheetPeekHeight = 76.dp + bottomNavHeight + navBarBottomHeightDp
        val peekPx = with(density) { sheetPeekHeight.toPx() }
        val hideOffsetPx = peekPx

        var bottomNavOffset by remember { mutableStateOf(0.dp) }
        var miniPlayerAlpha by remember { mutableFloatStateOf(1f) }
        var playerBackgroundColor by remember { mutableStateOf(Color.Transparent) }
        val toastPressBackAgain = stringResource(R.string.toast_press_back_again)

        val miniPlayerBrush =
            Brush.Companion.verticalGradient(
                listOf(
                    lerp(
                        playerBackgroundColor,
                        MaterialTheme.colorScheme.surfaceVariant,
                        miniPlayerAlpha.coerceIn(0f, 1f),
                    ),
                    lerp(
                        playerBackgroundColor,
                        MaterialTheme.colorScheme.surfaceVariant,
                        miniPlayerAlpha.coerceIn(0f, 1f),
                    ),
                ),
            )

        LaunchedEffect(Unit) {
            while (isActive) {
                try {
                    val sheetOffset = sheetState.requireOffset()
                    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
                    val maxSheetOffset = screenHeightPx - peekPx
                    if (maxSheetOffset > 0f) {
                        val progress = (1f - sheetOffset / maxSheetOffset).coerceIn(0f, 1f)
                        bottomNavOffset = with(density) { (hideOffsetPx * progress).toDp() }
                        miniPlayerAlpha = 1f - progress
                    }
                } catch (_: IllegalStateException) {
                }
                delay(16)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // === BOTTOM SHEET (debajo del BottomNavBar) ===
            BottomSheetScaffold(
                modifier = Modifier.fillMaxSize(),
                scaffoldState = scaffoldState,
                sheetPeekHeight = sheetPeekHeight,
                sheetDragHandle = null,
                sheetMaxWidth = 10000.dp,
                sheetContainerColor = MaterialTheme.colorScheme.surface,
                containerColor = MaterialTheme.colorScheme.background,
                sheetShape = RectangleShape,
                sheetContent = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // MiniPlayer — se desvanece al expandir
                        if (playerState.currentSong != null) {
                            MiniPlayer(
                                song = playerState.currentSong!!,
                                isPlaying = playerState.isPlaying,
                                onPlayPause = {
                                    if (sheetState.currentValue !=
                                        SheetValue.Expanded
                                    ) {
                                        playbackViewModel.togglePlayPause()
                                    } else {
                                        null
                                    }
                                },
                                onClick = {
                                    scope.launch {
                                        if (sheetState.currentValue == SheetValue.PartiallyExpanded) {
                                            sheetState.expand()
                                        } else {
                                            sheetState.partialExpand()
                                        }
                                    }
                                },
                                onNext = { if (sheetState.currentValue != SheetValue.Expanded) playbackViewModel.playNextSong() else null },
                                backgroundBrush = miniPlayerBrush,
                                contentAlpha = miniPlayerAlpha,
                            )
                        }

                        // PlayerScreen — visible cuando se expande
                        if (playerState.currentSong != null) {
                            PlayerScreen(
                                mainViewModel = mainViewModel,
                                onCollapse = {
                                    scope.launch { sheetState.partialExpand() }
                                },
                                onNavigateToArtist = { artistName ->
                                    scope.launch { sheetState.partialExpand() }
                                    navController.navigate(
                                        Screen.ArtistDetail.createRoute(artistName),
                                    )
                                },
                                onNavigateToAlbum = { albumName, artistName ->
                                    scope.launch { sheetState.partialExpand() }
                                    navController.navigate(
                                        Screen.AlbumDetail.createRoute(albumName, artistName),
                                    )
                                },
                                onBackgroundColorChanged = { color ->
                                    playerBackgroundColor = color
                                },
                            )
                        }
                    }
                },
            ) {
                // Padding manual: statusBarsPadding arriba, sheetPeekHeight abajo
                // El BottomNavBar es componente independiente, no afecta el content
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(bottom = sheetPeekHeight),
                ) {
                    AppNavigation(
                        navController = navController,
                        songViewModel = songViewModel,
                        playbackViewModel = playbackViewModel,
                        playlistViewModel = playlistViewModel,
                        playerViewModel = playerViewModel,
                        artistViewModel = artistViewModel,
                        albumViewModel = albumViewModel,
                        genreViewModel = genreViewModel,
                        startDestination = defaultStartTab,
                    )
                }
            }

            // === BOTTOM NAV BAR (independiente, por encima del sheet en Z) ===
            // Se desliza hacia abajo cuando el sheet se expande
            BottomNavigationBar(
                navController = navController,
                songsTabEnabled = songsTabEnabled,
                albumsTabEnabled = albumsTabEnabled,
                artistsTabEnabled = artistsTabEnabled,
                playlistsTabEnabled = playlistsTabEnabled,
                genresTabEnabled = genresTabEnabled,
                modifier =
                    Modifier
                        .align(Alignment.Companion.BottomCenter)
                        .offset(y = bottomNavOffset)
                        .zIndex(5f),
            )

            // === OVERLAYS (por encima de todo, incluido sheet y bottom nav) ===
            if (showEqualizer) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .zIndex(10f),
                ) {
                    EqualizerScreen(
                        viewModel = equalizerViewModel,
                        onClose = { equalizerViewModel.closeEqualizerScreen() },
                    )
                }
            } else if (showSettings) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .zIndex(10f),
                ) {
                    SettingsScreen(
                        viewModel = mainViewModel,
                        equalizerViewModel = equalizerViewModel,
                        folderViewModel = folderViewModel,
                        playlistViewModel = playlistViewModel,
                        onClose = { playerViewModel.closeSettingsScreen() },
                    )
                }
            }

            if (showAbout) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .zIndex(11f),
                ) {
                    AboutScreen(onBack = { playerViewModel.showAbout(false) })
                }
            }
        }

        BackHandler {
            // 1. Si el PlayerScreen está expandido, colapsarlo
            if (sheetState.currentValue == SheetValue.Expanded) {
                scope.launch { sheetState.partialExpand() }
                return@BackHandler
            }

            // 2. Si el NavHost puede navegar hacia atrás, hacerlo
            if (navController.popBackStack().not()) {
                // 3. No hay más pantallas atrás — mostrar "presiona de nuevo para salir"
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < 1000) {
                    activity?.finish()
                } else {
                    lastBackPressTime = currentTime
                    Toast
                        .makeText(
                            context,
                            toastPressBackAgain,
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }
        }
    }
}

/**
 * Resolve a content/file URI to an absolute file path, or null if it can't be resolved.
 * Handles DocumentsProvider URIs (Google Files, etc.), file:// URIs, and MediaStore content URIs.
 */
private fun resolveAudioFilePath(context: android.content.Context, uri: android.net.Uri): String? {
    // DocumentsProvider URIs (e.g. from Google Files)
    if (DocumentsContract.isDocumentUri(context, uri) &&
        "com.android.externalstorage.documents" == uri.authority
    ) {
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(":")
        if (split.size >= 2) {
            val type = split[0] // "primary", "XXXX-XXXX" (SD card)
            val path = split[1]
            return "/storage/$type/$path"
        }
    }
    // file:// URIs
    if (uri.scheme == "file") {
        return uri.path
    }
    // content:// — try querying the DATA column directly
    if (uri.scheme == "content") {
        try {
            val cursor =
                context.contentResolver.query(uri, arrayOf(MediaStore.Audio.Media.DATA), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getString(0)
                }
            }
        } catch (_: Exception) {
        }
    }
    return null
}

/**
 * Query a Song from MediaStore by URI. Returns null if the URI is not a MediaStore audio URI
 * or the query fails.
 */
private fun querySongFromMediaStore(
    context: android.content.Context,
    uri: android.net.Uri,
): com.cvc953.localplayer.model.Song? {
    val projection =
        arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
        )
    val cursor =
        try {
            context.contentResolver.query(uri, projection, null, null, null)
        } catch (_: Exception) {
            null
        }
    cursor?.use {
        if (it.moveToFirst()) {
            val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
            val title = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)) ?: ""
            val artist = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)) ?: ""
            val album = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)) ?: ""
            val duration = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
            val filePath = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
            return com.cvc953.localplayer.model.Song(
                id = id,
                title = title,
                artist = artist,
                album = album,
                uri = uri,
                duration = duration,
                filePath = filePath,
                year = null,
                trackNumber = 0,
                discNumber = 0,
            )
        }
    }
    return null
}
