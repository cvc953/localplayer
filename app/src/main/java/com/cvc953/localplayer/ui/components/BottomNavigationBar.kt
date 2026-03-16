package com.cvc953.localplayer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cvc953.localplayer.ui.navigation.BottomNavItem
import com.cvc953.localplayer.ui.navigation.Screen

@Composable
fun BottomNavigationBar(navController: androidx.navigation.NavHostController) {
    val navItems =
        listOf(
            BottomNavItem.Songs,
            BottomNavItem.Albums,
            BottomNavItem.Artists,
            BottomNavItem.Playlists,
        )

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
            else -> "songs" // Default
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth(),
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            navItems.forEach { item ->
                NavigationBarItem(
                    icon = {
                        Icon(item.icon, contentDescription = item.title)
                    },
                    label = { Text(item.title) },
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
