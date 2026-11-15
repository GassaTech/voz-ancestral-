package com.example.presentation.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.presentation.archive.ArchiveStatusScreen
import com.example.presentation.dictionary.DictionaryScreen
import com.example.presentation.dictionary.EntryDetailScreen
import com.example.presentation.encyclopedia.ArticleDetailScreen
import com.example.presentation.encyclopedia.EncyclopediaScreen
import com.example.presentation.favorites.FavoritesScreen

private data class TopLevelNavItem<T : Any>(
    val route: T,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelItems = listOf(
    TopLevelNavItem(
        route = EncyclopediaRoute,
        labelRes = R.string.nav_encyclopedia,
        selectedIcon = Icons.Rounded.AutoStories,
        unselectedIcon = Icons.Outlined.AutoStories,
        testTag = "nav_encyclopedia"
    ),
    TopLevelNavItem(
        route = DictionaryRoute,
        labelRes = R.string.nav_dictionary,
        selectedIcon = Icons.Rounded.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook,
        testTag = "nav_dictionary"
    ),
    TopLevelNavItem(
        route = FavoritesRoute,
        labelRes = R.string.nav_favorites,
        selectedIcon = Icons.Rounded.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        testTag = "nav_favorites"
    ),
    TopLevelNavItem(
        route = ArchiveStatusRoute,
        labelRes = R.string.nav_archive,
        selectedIcon = Icons.Rounded.Storage,
        unselectedIcon = Icons.Outlined.Storage,
        testTag = "nav_archive"
    )
)

@Composable
fun ParintintinAppShell(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val showTopLevelChrome = topLevelItems.any { item ->
        currentDestination?.hasRoute(item.route::class) == true
    }

    val navigateToTopLevel: (Any) -> Unit = { destination ->
        navController.navigate(destination) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showTopLevelChrome && !useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        topLevelItems.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTopLevel(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelChrome && useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        topLevelItems.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTopLevel(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = EncyclopediaRoute,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<EncyclopediaRoute> {
                        EncyclopediaScreen(
                            onOpenArticle = { articleId ->
                                navController.navigate(ArticleDetailRoute(articleId))
                            },
                            onOpenDictionarySearch = {
                                navigateToTopLevel(DictionaryRoute)
                            }
                        )
                    }
                    composable<DictionaryRoute> {
                        DictionaryScreen(
                            onOpenEntry = { entryId ->
                                navController.navigate(EntryDetailRoute(entryId))
                            }
                        )
                    }
                    composable<FavoritesRoute> {
                        FavoritesScreen(
                            onOpenEntry = { entryId ->
                                navController.navigate(EntryDetailRoute(entryId))
                            },
                            onOpenArticle = { articleId ->
                                navController.navigate(ArticleDetailRoute(articleId))
                            }
                        )
                    }
                    composable<ArchiveStatusRoute> {
                        ArchiveStatusScreen()
                    }
                    composable<EntryDetailRoute> {
                        EntryDetailScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<ArticleDetailRoute> {
                        ArticleDetailScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
