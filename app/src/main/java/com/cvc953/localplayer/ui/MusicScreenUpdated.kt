@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.cvc953.localplayer.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cvc953.localplayer.preferences.AppPrefs
import com.cvc953.localplayer.ui.components.BottomNavigationBar
import com.cvc953.localplayer.ui.navigation.*
import com.cvc953.localplayer.util.StoragePermissionHandler
import com.cvc953.localplayer.viewmodel.*

/**
 * Versión actualizada de MainMusicScreen usando Navigation Compose
 */
@Suppress("ktlint:standard:function-naming")
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
    val mainViewModel: com.cvc953.localplayer.viewmodel.MainViewModel = viewModel()
    val equalizerViewModel: com.cvc953.localplayer.viewmodel.EqualizerViewModel = viewModel()
    val folderViewModel: com.cvc953.localplayer.viewmodel.FolderViewModel = viewModel()

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
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val playerState by playbackViewModel.playerState.collectAsState()
        val showPlayerScreen by playerViewModel.isPlayerScreenVisible.collectAsState()
        val showSettings by playerViewModel.isSettingsVisible.collectAsState()
        val showEqualizer by equalizerViewModel.isEqualizerVisible.collectAsState()
        val activity = context as? Activity
        var lastBackPressTime by remember { mutableStateOf(0L) }

        // Determine current tab based on first route segment
        val firstSegment = currentRoute?.substringBefore("/")
        val currentTab =
            when (firstSegment) {
                "songs" -> BottomNavItem.Songs.route
                "album" -> BottomNavItem.Albums.route
                "artists" -> BottomNavItem.Artists.route
                "artist" -> BottomNavItem.Artists.route
                "playlists" -> BottomNavItem.Playlists.route
                "playlist" -> BottomNavItem.Playlists.route
                else -> BottomNavItem.Songs.route
            }

        val navItems =
            listOf(
                BottomNavItem.Songs,
                BottomNavItem.Albums,
                BottomNavItem.Artists,
                BottomNavItem.Playlists,
            )

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    BottomNavigationBar(
                        navController = navController,
                    )
                },
            ) { padding ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(MaterialTheme.colorScheme.onBackground),
                ) {
                    // Main content - takes remaining space
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .weight(1f),
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

                    // MiniPlayer - above BottomNavigationBar
                    if (playerState.currentSong != null) {
                        MiniPlayer(
                            song = playerState.currentSong!!,
                            isPlaying = playerState.isPlaying,
                            onPlayPause = { playbackViewModel.togglePlayPause() },
                            onClick = { playerViewModel.openPlayerScreen() },
                            onNext = { playbackViewModel.playNextSong() },
                            modifier = Modifier,
                        )
                    }
                }
            }

            if (showPlayerScreen) {
                Box(modifier = Modifier.fillMaxSize().zIndex(1f)) {
                    PlayerScreen(
                        mainViewModel = mainViewModel,
                        onBack = { playerViewModel.closePlayerScreen() },
                        onNavigateToArtist = { artistName ->
                            playerViewModel.closePlayerScreen()
                            navController.navigate(Screen.ArtistDetail.createRoute(artistName))
                        },
                        onNavigateToAlbum = { albumName, artistName ->
                            playerViewModel.closePlayerScreen()
                            navController.navigate(Screen.AlbumDetail.createRoute(albumName, artistName))
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
                        folderViewModel = folderViewModel,
                        onClose = { playerViewModel.closeSettingsScreen() },
                    )
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
