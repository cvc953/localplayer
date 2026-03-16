package com.cvc953.localplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

/**
 * Wrapper para mantener compatibilidad temporal con estado de navegación actual
 */
class NavigationState(
    val navController: NavHostController,
) {
    val currentRoute: String?
        get() = navController.currentDestination?.route

    fun navigateTo(route: String) {
        navController.navigate(route)
    }

    fun goBack() {
        navController.popBackStack()
    }
}

/**
 * Remember extension para NavigationState
 */
@Composable
fun rememberNavigationState(): NavigationState {
    val navController = rememberNavController()
    return remember(navController) {
        NavigationState(navController)
    }
}
