package com.cvc953.localplayer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Songs : BottomNavItem("songs", "Canciones", Icons.Default.MusicNote)
    object Albums : BottomNavItem("albums", "Álbumes", Icons.Default.Album)
    object Artists : BottomNavItem("artists", "Artistas", Icons.Default.Person)
    object Playlists : BottomNavItem("playlists", "Listas", Icons.Default.PlaylistPlay)
}
