package com.cvc953.localplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cvc953.localplayer.ui.AlbumDetailScreen
import com.cvc953.localplayer.ui.AlbumsScreen
import com.cvc953.localplayer.ui.ArtistDetailScreen
import com.cvc953.localplayer.ui.ArtistSongsScreen
import com.cvc953.localplayer.ui.ArtistsScreen
import com.cvc953.localplayer.ui.PlaylistDetailScreen
import com.cvc953.localplayer.ui.PlaylistsScreen
import com.cvc953.localplayer.ui.SongsContent
import com.cvc953.localplayer.viewmodel.AlbumViewModel
import com.cvc953.localplayer.viewmodel.ArtistViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlayerViewModel
import com.cvc953.localplayer.viewmodel.PlaylistViewModel
import com.cvc953.localplayer.viewmodel.SongViewModel

@Suppress("ktlint:standard:function-naming")
/*
 * Componente principal de navegación
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    songViewModel: SongViewModel = viewModel(),
    playbackViewModel: PlaybackViewModel = viewModel(),
    playlistViewModel: PlaylistViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    artistViewModel: ArtistViewModel = viewModel(),
    albumViewModel: AlbumViewModel = viewModel(),
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Screen.Songs.route,
    ) {
        // Pantalla de Canciones
        composable(Screen.Songs.route) {
            SongsContent(
                songViewModel = songViewModel,
                playbackViewModel = playbackViewModel,
                playlistViewModel = playlistViewModel,
                playerViewModel = playerViewModel,
            )
        }

        // Pantalla de Álbumes
        composable(Screen.Albums.route) {
            AlbumsScreen(
                albumViewModel = albumViewModel,
                playbackViewModel = playbackViewModel,
                onAlbumClick = { albumName, artistName ->
                    navController.navigateAlbumDetail(albumName, artistName)
                },
            )
        }

        // Detalle de Álbum
        composable(
            route = Screen.AlbumDetail.route,
            arguments = albumDetailArguments,
            deepLinks = albumDeepLinks,
        ) { backStackEntry ->
            val albumName = RouteParams.decode(backStackEntry.arguments?.getString(Screen.AlbumDetail.ARG_ALBUM_NAME))
            val artistName = RouteParams.decode(backStackEntry.arguments?.getString(Screen.AlbumDetail.ARG_ARTIST_NAME))

            AlbumDetailScreen(
                albumViewModel = albumViewModel,
                playbackViewModel = playbackViewModel,
                playlistViewModel = playlistViewModel,
                albumName = albumName,
                artistName = artistName,
                onBack = { navController.navigateBack() },
            )
        }

        // Pantalla de Artistas
        composable(Screen.Artists.route) {
            ArtistsScreen(
                artistViewModel = artistViewModel,
                playbackViewModel = playbackViewModel,
                onArtistClick = { artistName ->
                    navController.navigateArtistDetail(artistName)
                },
            )
        }

        // Detalle de Artista
        composable(
            route = Screen.ArtistDetail.route,
            arguments = artistDetailArguments,
            deepLinks = artistDeepLinks,
        ) { backStackEntry ->
            val artistName = RouteParams.decode(backStackEntry.arguments?.getString(Screen.ArtistDetail.ARG_ARTIST_NAME))

            ArtistDetailScreen(
                artistViewModel = artistViewModel,
                playbackViewModel = playbackViewModel,
                playlistViewModel = playlistViewModel,
                artistName = artistName,
                onBack = { navController.navigateBack() },
                onAlbumClick = { albumName, artistName ->
                    navController.navigateAlbumDetail(albumName, artistName)
                },
                onViewAllSongs = {
                    navController.navigateArtistSongs(artistName)
                },
            )
        }

        // Canciones de Artista
        composable(
            route = Screen.ArtistSongs.route,
            arguments = artistSongsArguments,
        ) { backStackEntry ->
            val artistName = RouteParams.decode(backStackEntry.arguments?.getString(Screen.ArtistSongs.ARG_ARTIST_NAME))

            ArtistSongsScreen(
                artistViewModel = artistViewModel,
                artistName = artistName,
                onBack = { navController.navigateBack() },
                playbackViewModel = playbackViewModel,
            )
        }

        // Pantalla de Playlists
        composable(Screen.Playlists.route) {
            PlaylistsScreen(
                playlistViewModel = playlistViewModel,
                onPlaylistClick = { playlistName ->
                    navController.navigatePlaylistDetail(playlistName)
                },
                playbackViewModel = playbackViewModel,
            )
        }

        // Detalle de Playlist
        composable(
            route = Screen.PlaylistDetail.route,
            arguments = playlistDetailArguments,
            deepLinks = playlistDeepLinks,
        ) { backStackEntry ->
            val playlistName = backStackEntry.arguments?.getString(Screen.PlaylistDetail.ARG_PLAYLIST_NAME) ?: ""

            PlaylistDetailScreen(
                playlistViewModel = playlistViewModel,
                playlistName = playlistName,
                onBack = { navController.navigateBack() },
                playbackViewModel = playbackViewModel,
            )
        }
    }
}
