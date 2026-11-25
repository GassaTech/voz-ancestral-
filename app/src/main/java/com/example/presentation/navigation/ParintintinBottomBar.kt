package com.example.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.example.R

private data class BottomNavItem(
    val destination: AppDestination,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val bottomNavItems: List<BottomNavItem> = AppDestination.primaryDestinations.map { destination ->
    when (destination) {
        AppDestination.Home -> BottomNavItem(
            destination = destination,
            labelRes = R.string.nav_home,
            selectedIcon = Icons.Rounded.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "bottom_nav_home"
        )
        AppDestination.Encyclopedia -> BottomNavItem(
            destination = destination,
            labelRes = R.string.nav_encyclopedia,
            selectedIcon = Icons.Rounded.AutoStories,
            unselectedIcon = Icons.Outlined.AutoStories,
            testTag = "bottom_nav_encyclopedia"
        )
        AppDestination.Dictionary -> BottomNavItem(
            destination = destination,
            labelRes = R.string.nav_dictionary,
            selectedIcon = Icons.Rounded.MenuBook,
            unselectedIcon = Icons.Outlined.MenuBook,
            testTag = "bottom_nav_dictionary"
        )
        AppDestination.Territories -> BottomNavItem(
            destination = destination,
            labelRes = R.string.nav_territories,
            selectedIcon = Icons.Rounded.Public,
            unselectedIcon = Icons.Outlined.Public,
            testTag = "bottom_nav_territories"
        )
        AppDestination.Favorites -> BottomNavItem(
            destination = destination,
            labelRes = R.string.nav_favorites,
            selectedIcon = Icons.Rounded.Favorite,
            unselectedIcon = Icons.Outlined.FavoriteBorder,
            testTag = "bottom_nav_favorites"
        )
        AppDestination.Archive -> BottomNavItem(
            destination = destination,
            labelRes = R.string.nav_archive,
            selectedIcon = Icons.Rounded.Storage,
            unselectedIcon = Icons.Outlined.Storage,
            testTag = "bottom_nav_archive"
        )
    }
}

@Composable
fun ParintintinBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any {
                it.hasRoute(item.destination::class)
            } == true
            val label = stringResource(item.labelRes)

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}
