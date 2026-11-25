package com.example.presentation.navigation

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ParintintinNavHostTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `navegacao principal apresenta exatamente Inicio Dicionario e Territorios na barra inferior`() {
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(
                AppDestination.Home,
                AppDestination.Dictionary,
                AppDestination.Territories
            ),
            AppDestination.primaryDestinations
        )

        composeTestRule.onNodeWithTag("bottom_nav_home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Início").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_dictionary").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dicionário").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_territories").assertIsDisplayed()
        composeTestRule.onNodeWithText("Territórios").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bottom_nav_encyclopedia").assertDoesNotExist()
    }

    @Test
    fun `navegacao alterna entre Inicio Dicionario e Territorios com selecao visual correta`() {
        composeTestRule.waitForIdle()

        // 1. Inicialização na Home
        composeTestRule.onNodeWithTag("home_hero_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_home").assertIsSelected()
        composeTestRule.onNodeWithTag("bottom_nav_dictionary").assertIsNotSelected()
        composeTestRule.onNodeWithTag("bottom_nav_territories").assertIsNotSelected()

        // 2. Navegação para Dicionário
        composeTestRule.onNodeWithTag("bottom_nav_dictionary")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dictionary_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dictionary_header_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_dictionary").assertIsSelected()
        composeTestRule.onNodeWithTag("bottom_nav_home").assertIsNotSelected()
        composeTestRule.onNodeWithTag("bottom_nav_territories").assertIsNotSelected()

        // 3. Navegação para Territórios
        composeTestRule.onNodeWithTag("bottom_nav_territories")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("territories_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("territories_header_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dictionary_screen_root").assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_nav_territories").assertIsSelected()
        composeTestRule.onNodeWithTag("bottom_nav_home").assertIsNotSelected()
        composeTestRule.onNodeWithTag("bottom_nav_dictionary").assertIsNotSelected()

        // 4. Retorno à Home
        composeTestRule.onNodeWithTag("bottom_nav_home")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_hero_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("territories_screen_root").assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_nav_home").assertIsSelected()
        composeTestRule.onNodeWithTag("bottom_nav_dictionary").assertIsNotSelected()
        composeTestRule.onNodeWithTag("bottom_nav_territories").assertIsNotSelected()
    }

    @Test
    fun `alternancia entre abas preserva estado da pesquisa em Territorios e Dicionario`() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("bottom_nav_territories").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("territories_list")
            .performScrollToNode(hasTestTag("territories_search_input"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("territories_search_input")
            .performClick()
            .performTextInput("Tenharim")
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeTestRule.onNodeWithTag("territories_search_input")
                    .assert(hasText("Tenharim"))
                true
            }.getOrDefault(false)
        }

        composeTestRule.onNodeWithTag("bottom_nav_dictionary").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dictionary_list")
            .performScrollToNode(hasTestTag("dictionary_search_input"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dictionary_search_input")
            .performClick()
            .performTextInput("Riolume")
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeTestRule.onNodeWithTag("dictionary_search_input")
                    .assert(hasText("Riolume"))
                true
            }.getOrDefault(false)
        }

        composeTestRule.onNodeWithTag("bottom_nav_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_hero_banner").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bottom_nav_territories").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("territories_list")
            .performScrollToNode(hasTestTag("territories_search_input"))
        composeTestRule.onNodeWithTag("territories_search_input")
            .assert(hasText("Tenharim"))

        composeTestRule.onNodeWithTag("bottom_nav_dictionary").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dictionary_list")
            .performScrollToNode(hasTestTag("dictionary_search_input"))
        composeTestRule.onNodeWithTag("dictionary_search_input")
            .assert(hasText("Riolume"))
    }

    @Test
    fun `botao voltar a partir de Territorios retorna ao destino inicial Inicio`() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("bottom_nav_dictionary").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("bottom_nav_territories").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("territories_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_territories").assertIsSelected()

        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_hero_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("territories_screen_root").assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_nav_home").assertIsSelected()
        composeTestRule.onNodeWithTag("bottom_nav_territories").assertIsNotSelected()
    }
}
