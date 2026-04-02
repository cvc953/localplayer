@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.cvc953.localplayer.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.cvc953.localplayer.preferences.AppPrefs
import com.cvc953.localplayer.ui.components.BottomNavigationBar
import com.cvc953.localplayer.ui.navigation.*
import com.cvc953.localplayer.util.StoragePermissionHandler
import com.cvc953.localplayer.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMusicScreenUpdated(onOpenPlayer: () -> Unit) {
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
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()

        val playerState by playbackViewModel.playerState.collectAsState()
        val showSettings by playerViewModel.isSettingsVisible.collectAsState()
        val showEqualizer by equalizerViewModel.isEqualizerVisible.collectAsState()
        val showAbout by playerViewModel.isAboutVisible.collectAsState()
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

        // Peek = MiniPlayer + espacio para que quede arriba del BottomNavBar
        // El BottomNavBar (zIndex 5) tapa la parte de abajo del peek
        val sheetPeekHeight = 76.dp + 80.dp

        // Polling del offset del sheet para animación progresiva
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val peekPx = with(density) { sheetPeekHeight.toPx() }
        val hideOffsetPx = with(density) { (bottomNavHeight + 16.dp).toPx() }

        var bottomNavOffset by remember { mutableStateOf(0.dp) }
        var miniPlayerAlpha by remember { mutableFloatStateOf(1f) }
        var playerBackgroundColor by remember { mutableStateOf(Color.Transparent) }

        val miniPlayerBrush =
            Brush.verticalGradient(
                listOf(
                    lerp(playerBackgroundColor, MaterialTheme.colorScheme.surfaceVariant, miniPlayerAlpha.coerceIn(0f, 1f)),
                    lerp(playerBackgroundColor, MaterialTheme.colorScheme.surfaceVariant, miniPlayerAlpha.coerceIn(0f, 1f)),
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
                scaffoldState = scaffoldState,
                sheetPeekHeight = sheetPeekHeight,
                sheetDragHandle = null,
                sheetContainerColor = MaterialTheme.colorScheme.surface,
                containerColor = MaterialTheme.colorScheme.background,
                sheetShape = RectangleShape,
                sheetContent = {
                    Column {
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
                    )
                }
            }

            // === BOTTOM NAV BAR (independiente, por encima del sheet en Z) ===
            // Se desliza hacia abajo cuando el sheet se expande
            BottomNavigationBar(
                navController = navController,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
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
    }
}
