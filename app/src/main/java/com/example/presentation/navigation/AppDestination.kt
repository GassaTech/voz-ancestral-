package com.example.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Definições tipadas das rotas principais do aplicativo.
 * Apenas [Home] possui tela funcional nesta etapa.
 */
@Serializable
sealed interface AppDestination {

    @Serializable
    data object Home : AppDestination

    @Serializable
    data object Encyclopedia : AppDestination

    @Serializable
    data object Dictionary : AppDestination

    @Serializable
    data object Favorites : AppDestination

    @Serializable
    data object Archive : AppDestination

    companion object {
        val primaryDestinations: List<AppDestination> = listOf(
            Home,
            Encyclopedia,
            Dictionary,
            Favorites,
            Archive
        )
    }
}
