package com.cvc953.localplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cvc953.localplayer.ui.navigation.BottomNavItem

@Composable
fun BottomNavigationBar(
    navController: androidx.navigation.NavHostController,
    songsTabEnabled: Boolean,
    albumsTabEnabled: Boolean,
    artistsTabEnabled: Boolean,
    playlistsTabEnabled: Boolean,
    genresTabEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val navItems =
        buildList {
            if (songsTabEnabled) add(BottomNavItem.Songs)
            if (albumsTabEnabled) add(BottomNavItem.Albums)
            if (artistsTabEnabled) add(BottomNavItem.Artists)
            if (playlistsTabEnabled) add(BottomNavItem.Playlists)
            if (genresTabEnabled) add(BottomNavItem.Genres)
        }

    // Use currentBackStackEntryAsState to ensure recomposition when destination changes
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    // Map routes to their corresponding tab
    val currentTab =
        when {
            currentRoute == "songs" -> "songs"

            currentRoute == "albums" -> "albums"

            currentRoute.startsWith("album/") -> "albums"

            // album detail -> albums tab
            currentRoute == "artists" -> "artists"

            currentRoute.startsWith("artist/") -> "artists"

            // artist detail -> artists tab
            currentRoute == "playlists" -> "playlists"

            currentRoute.startsWith("playlist/") -> "playlists"

            // playlist detail -> playlists tab
            currentRoute == "genres" -> "genres"

            currentRoute.startsWith("genre/") -> "genres"

            // genre detail -> genres tab
            else -> "songs" // Default
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth(),
    ) {
        // Fill strip to close gap between NavBar and MiniPlayer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            windowInsets = WindowInsets.navigationBars,
        ) {
            navItems.forEach { item ->
                val title = stringResource(item.titleResId)
                NavigationBarItem(
                    icon = {
                        Icon(item.icon, contentDescription = title)
                    },
                    label = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false,
                        )
                    },
                    selected = currentTab == item.route,
                    onClick = {
                        // Navegación principal
                        navController.navigate(item.route) {
                            // Evitar duplicados en la pila
                            launchSingleTop = true
                            // Restaurar estado si existe
                            restoreState = true
                            // Hacer pop hasta el inicio si navegamos desde profundidades
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                        }
                    },
                    colors =
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.background,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                        ),
                )
            }
        }
    }
}
