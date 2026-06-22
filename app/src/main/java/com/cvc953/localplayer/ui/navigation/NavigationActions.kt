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

fun NavController.navigateGenres() {
    navigate(Screen.Genres.route)
}

fun NavController.navigateGenreDetail(genreName: String) {
    navigate(Screen.GenreDetail.createRoute(genreName))
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

fun NavController.navigateSongEdit(songId: Long) {
    navigate(Screen.SongEdit.createRoute(songId))
}

fun NavController.navigateAlbumEdit(albumName: String, artistName: String) {
    navigate(Screen.AlbumEdit.createRoute(albumName, artistName))
}

/**
 * Navegación hacia atrás
 */
fun NavController.navigateBack() {
    if (!navigateUp()) {
        popBackStack()
    }
}
