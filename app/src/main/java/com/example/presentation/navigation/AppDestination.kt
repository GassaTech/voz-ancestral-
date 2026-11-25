package com.example.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Definições tipadas das rotas do aplicativo.
 * A navegação principal expõe [Home], [Dictionary] e [Territories];
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
    data object Territories : AppDestination

    companion object {
        val primaryDestinations: List<AppDestination> = listOf(
            Home,
            Dictionary,
            Territories
        )
    }
}
