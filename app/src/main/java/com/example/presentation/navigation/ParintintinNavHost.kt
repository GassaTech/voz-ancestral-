package com.example.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.presentation.dictionary.DictionaryScreen
import com.example.presentation.encyclopedia.EncyclopediaScreen
import com.example.presentation.home.HomeScreen
import com.example.presentation.territories.TerritoriesScreen
import com.example.presentation.territories.TerritoryDetailsScreen

@Composable
fun ParintintinAppShell(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            val isDetailsScreen = currentDestination?.hierarchy?.any {
                it.hasRoute(AppDestination.TerritoryDetails::class)
            } == true

            if (!isDetailsScreen) {
                ParintintinBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = navController::navigateSingleTopTo
                )
            }
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
            DictionaryScreen()
        }
        composable<AppDestination.Territories> {
            TerritoriesScreen(
                onSelectTerritory = { landId ->
                    navController.navigate(
                        AppDestination.TerritoryDetails(landId = landId)
                    )
                }
            )
        }

        composable<AppDestination.TerritoryDetails> { backStackEntry ->
            backStackEntry.toRoute<AppDestination.TerritoryDetails>()

            TerritoryDetailsScreen(
                onBack = { navController.popBackStack() }
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
