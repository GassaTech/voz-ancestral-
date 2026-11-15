package com.example.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.presentation.components.ProvisionalDestinationScreen
import com.example.presentation.encyclopedia.EncyclopediaScreen
import com.example.presentation.home.HomeScreen

@Composable
fun ParintintinAppShell(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            ParintintinBottomBar(
                currentDestination = currentDestination,
                onNavigate = navController::navigateSingleTopTo
            )
        }
    ) { innerPadding ->
        ParintintinNavHost(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
fun ParintintinNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: AppDestination = AppDestination.Home
) {
    val backToHome: () -> Unit = {
        navController.navigateSingleTopTo(AppDestination.Home)
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<AppDestination.Home> {
            HomeScreen()
        }
        composable<AppDestination.Encyclopedia> {
            EncyclopediaScreen()
        }
        composable<AppDestination.Dictionary> {
            ProvisionalDestinationScreen(
                icon = Icons.Outlined.MenuBook,
                title = stringResource(R.string.nav_dictionary),
                description = stringResource(R.string.provisional_dictionary_desc),
                tagSuffix = "dictionary",
                onBackToHome = backToHome
            )
        }
        composable<AppDestination.Favorites> {
            ProvisionalDestinationScreen(
                icon = Icons.Outlined.FavoriteBorder,
                title = stringResource(R.string.nav_favorites),
                description = stringResource(R.string.provisional_favorites_desc),
                tagSuffix = "favorites",
                onBackToHome = backToHome
            )
        }
        composable<AppDestination.Archive> {
            ProvisionalDestinationScreen(
                icon = Icons.Outlined.Storage,
                title = stringResource(R.string.nav_archive),
                description = stringResource(R.string.provisional_archive_desc),
                tagSuffix = "archive",
                onBackToHome = backToHome
            )
        }
    }
}

fun NavHostController.navigateSingleTopTo(destination: AppDestination) {
    navigate(destination) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
