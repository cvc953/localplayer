package com.cvc953.localplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cvc953.localplayer.ui.components.SongsContent
import com.cvc953.localplayer.ui.navigation.Screen
import com.cvc953.localplayer.ui.screens.AlbumDetailScreen
import com.cvc953.localplayer.ui.screens.AlbumEditScreen
import com.cvc953.localplayer.ui.screens.AlbumsScreen
import com.cvc953.localplayer.ui.screens.ArtistDetailScreen
import com.cvc953.localplayer.ui.screens.ArtistSongsScreen
import com.cvc953.localplayer.ui.screens.ArtistsScreen
import com.cvc953.localplayer.ui.screens.GenreDetailScreen
import com.cvc953.localplayer.ui.screens.GenresScreen
import com.cvc953.localplayer.ui.screens.PlaylistDetailScreen
import com.cvc953.localplayer.ui.screens.PlaylistsScreen
import com.cvc953.localplayer.ui.screens.SongEditScreen
import com.cvc953.localplayer.viewmodel.AlbumEditViewModel
import com.cvc953.localplayer.viewmodel.AlbumViewModel
import com.cvc953.localplayer.viewmodel.ArtistViewModel
import com.cvc953.localplayer.viewmodel.GenreViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlayerViewModel
import com.cvc953.localplayer.viewmodel.PlaylistViewModel
import com.cvc953.localplayer.viewmodel.SongEditViewModel
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
    genreViewModel: GenreViewModel = viewModel(),
    startDestination: String = Screen.Songs.route,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination,
    ) {
        // Pantalla de Canciones
        composable(Screen.Songs.route) {
            SongsContent(
                songViewModel = songViewModel,
                playbackViewModel = playbackViewModel,
                playlistViewModel = playlistViewModel,
                playerViewModel = playerViewModel,
                onEditSong = { song ->
                    navController.navigateSongEdit(song.id)
                },
            )
        }

        // Pantalla de Álbumes
        composable(Screen.Albums.route) {
            AlbumsScreen(
                albumViewModel = albumViewModel,
                playbackViewModel = playbackViewModel,
                playerViewModel = playerViewModel,
                onAlbumClick = { albumName, artistName ->
                    navController.navigateAlbumDetail(albumName, artistName)
                },
                onEditAlbum = { albumName, artistName ->
                    navController.navigateAlbumEdit(albumName, artistName)
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
                onNavigateToAlbumEdit = { aN, aN2 ->
                    navController.navigateAlbumEdit(aN, aN2)
                },
            )
        }

        // Pantalla de Artistas
        composable(Screen.Artists.route) {
            ArtistsScreen(
                artistViewModel = artistViewModel,
                playbackViewModel = playbackViewModel,
                playerViewModel = playerViewModel,
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

        // Pantalla de Géneros
        composable(Screen.Genres.route) {
            GenresScreen(
                genreViewModel = genreViewModel,
                playbackViewModel = playbackViewModel,
                playerViewModel = playerViewModel,
                songViewModel = songViewModel,
                onGenreClick = { genreName ->
                    navController.navigateGenreDetail(genreName)
                },
            )
        }

        // Detalle de Género
        composable(
            route = Screen.GenreDetail.route,
            arguments = genreDetailArguments,
        ) { backStackEntry ->
            val genreName = RouteParams.decode(backStackEntry.arguments?.getString(Screen.GenreDetail.ARG_GENRE_NAME))

            GenreDetailScreen(
                genreName = genreName,
                playbackViewModel = playbackViewModel,
                onBack = { navController.navigateBack() },
            )
        }

        // Pantalla de Playlists
        composable(Screen.Playlists.route) {
            PlaylistsScreen(
                playlistViewModel = playlistViewModel,
                playbackViewModel = playbackViewModel,
                playerViewModel = playerViewModel,
                onPlaylistClick = { playlistName ->
                    navController.navigatePlaylistDetail(playlistName)
                },
            )
        }

        // Detalle de Playlist
        composable(
            route = Screen.PlaylistDetail.route,
            arguments = playlistDetailArguments,
            deepLinks = playlistDeepLinks,
        ) { backStackEntry ->
            val playlistName = RouteParams.decode(backStackEntry.arguments?.getString(Screen.PlaylistDetail.ARG_PLAYLIST_NAME))

            PlaylistDetailScreen(
                playlistViewModel = playlistViewModel,
                playlistName = playlistName,
                onBack = { navController.navigateBack() },
                playbackViewModel = playbackViewModel,
            )
        }

        // Editar canción
        composable(
            route = Screen.SongEdit.route,
            arguments = songEditArguments,
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getLong(Screen.SongEdit.ARG_SONG_ID) ?: return@composable

            SongEditScreen(
                viewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            SongEditViewModel(songViewModel.getApplication(), songId) as T
                    },
                ),
                onNavigateBack = { navController.navigateBack() },
            )
        }

        // Editar álbum
        composable(
            route = Screen.AlbumEdit.route,
            arguments = albumEditArguments,
        ) { backStackEntry ->
            val albumName = RouteParams.decode(backStackEntry.arguments?.getString(Screen.AlbumEdit.ARG_ALBUM_NAME))
            val artistName = RouteParams.decode(backStackEntry.arguments?.getString(Screen.AlbumEdit.ARG_ARTIST_NAME))

            AlbumEditScreen(
                viewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            AlbumEditViewModel(songViewModel.getApplication(), albumName, artistName) as T
                    },
                ),
                onNavigateBack = { navController.navigateBack() },
            )
        }
    }
}
