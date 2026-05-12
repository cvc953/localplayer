package com.cvc953.localplayer.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.cvc953.localplayer.R

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleResId: Int,
    val icon: ImageVector,
) {
    object Songs : BottomNavItem("songs", R.string.songs_title, Icons.Default.MusicNote)

    object Albums : BottomNavItem("albums", R.string.albums_title, Icons.Default.Album)

    object Artists : BottomNavItem("artists", R.string.artists_title, Icons.Default.Person)

    object Playlists : BottomNavItem("playlists", R.string.playlists_title, Icons.AutoMirrored.Filled.PlaylistPlay)
}
