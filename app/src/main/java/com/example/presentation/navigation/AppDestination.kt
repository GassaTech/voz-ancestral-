package com.example.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Definições tipadas das rotas do aplicativo.
 * Na versão 1.0, a navegação principal expõe [Home] e [Dictionary];
 * os demais destinos permanecem preservados para evoluções futuras.
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
            Dictionary
        )
    }
}
