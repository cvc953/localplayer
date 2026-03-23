package com.cvc953.localplayer.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Definiciones de destinos de navegación para Navigation Compose
 */
sealed class Screen(
    val route: String,
) {
    object Songs : Screen("songs")

    object Albums : Screen("albums")

    object AlbumDetail : Screen("album/{albumName}/{artistName}") {
        const val ARG_ALBUM_NAME = "albumName"
        const val ARG_ARTIST_NAME = "artistName"

        fun createRoute(
            albumName: String,
            artistName: String,
        ): String {
            val encodedAlbum = URLEncoder.encode(albumName, StandardCharsets.UTF_8.toString())
            val encodedArtist = URLEncoder.encode(artistName, StandardCharsets.UTF_8.toString())
            return "album/$encodedAlbum/$encodedArtist"
        }
    }

    object Artists : Screen("artists")

    object ArtistDetail : Screen("artist/{artistName}") {
        const val ARG_ARTIST_NAME = "artistName"

        fun createRoute(artistName: String): String {
            val encodedArtist = URLEncoder.encode(artistName, StandardCharsets.UTF_8.toString())
            return "artist/$encodedArtist"
        }
    }

    object ArtistSongs : Screen("artist/{artistName}/songs") {
        const val ARG_ARTIST_NAME = "artistName"

        fun createRoute(artistName: String): String {
            val encodedArtist = URLEncoder.encode(artistName, StandardCharsets.UTF_8.toString())
            return "artist/$encodedArtist/songs"
        }
    }

    object Playlists : Screen("playlists")

    object PlaylistDetail : Screen("playlist/{playlistName}") {
        const val ARG_PLAYLIST_NAME = "playlistName"

        fun createRoute(playlistName: String): String {
            val encodedPlaylist = URLEncoder.encode(playlistName, StandardCharsets.UTF_8.toString())
            return "playlist/$encodedPlaylist"
        }
    }

    object Player : Screen("player")

    object Equalizer : Screen("equalizer")

    object Settings : Screen("Settings")

    object About : Screen("about")
}

/**
 * Argumentos de navegación para cada destino
 */
val albumDetailArguments =
    listOf(
        navArgument(Screen.AlbumDetail.ARG_ALBUM_NAME) { type = NavType.StringType },
        navArgument(Screen.AlbumDetail.ARG_ARTIST_NAME) { type = NavType.StringType },
    )

val artistDetailArguments =
    listOf(
        navArgument(Screen.ArtistDetail.ARG_ARTIST_NAME) { type = NavType.StringType },
    )

val artistSongsArguments =
    listOf(
        navArgument(Screen.ArtistSongs.ARG_ARTIST_NAME) { type = NavType.StringType },
    )

val playlistDetailArguments =
    listOf(
        navArgument(Screen.PlaylistDetail.ARG_PLAYLIST_NAME) { type = NavType.StringType },
    )

/**
 * Deep links para cada destino
 */
val albumDeepLinks =
    listOf(
        navDeepLink { uriPattern = "localplayer://album/{albumName}/{artistName}" },
    )

val artistDeepLinks =
    listOf(
        navDeepLink { uriPattern = "localplayer://artist/{artistName}" },
    )

val playlistDeepLinks =
    listOf(
        navDeepLink { uriPattern = "localplayer://playlist/{playlistName}" },
    )

/**
 * Utility functions for encoding/decoding route parameters
 */
object RouteParams {
    fun decode(value: String?): String =
        if (value == null || value.isEmpty()) {
            ""
        } else {
            URLDecoder.decode(value.replace("+", " "), StandardCharsets.UTF_8)
        }
}
