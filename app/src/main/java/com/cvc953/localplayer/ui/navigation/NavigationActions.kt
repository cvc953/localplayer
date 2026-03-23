package com.cvc953.localplayer.ui.navigation

import androidx.navigation.NavController

/**
 * Extensiones para NavController que facilitan la navegación
 */
fun NavController.navigateSongs() {
    navigate(Screen.Songs.route)
}

fun NavController.navigateAlbums() {
    navigate(Screen.Albums.route)
}

fun NavController.navigateAlbumDetail(
    albumName: String,
    artistName: String,
) {
    navigate(Screen.AlbumDetail.createRoute(albumName, artistName))
}

fun NavController.navigateArtists() {
    navigate(Screen.Artists.route)
}

fun NavController.navigateArtistDetail(artistName: String) {
    navigate(Screen.ArtistDetail.createRoute(artistName))
}

fun NavController.navigateArtistSongs(artistName: String) {
    navigate(Screen.ArtistSongs.createRoute(artistName))
}

fun NavController.navigatePlaylists() {
    navigate(Screen.Playlists.route)
}

fun NavController.navigatePlaylistDetail(playlistName: String) {
    navigate(Screen.PlaylistDetail.createRoute(playlistName))
}

fun NavController.navigatePlayer() {
    navigate(Screen.Player.route)
}

fun NavController.navigateSettings() {
    navigate(Screen.Settings.route)
}

fun NavController.navigateEqualizer() {
    navigate(Screen.Equalizer.route)
}

fun NavController.navigateAbout() {
    navigate(Screen.About.route)
}

/**
 * Navegación hacia atrás
 */
fun NavController.navigateBack() {
    popBackStack()
}
