package com.example.presentation.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    fun `navegacao principal apresenta somente Home e Dictionary na barra inferior`() {
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(AppDestination.Home, AppDestination.Dictionary),
            AppDestination.primaryDestinations
        )

        composeTestRule.onNodeWithTag("bottom_nav_home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Início").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_dictionary").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dicionário").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bottom_nav_encyclopedia").assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_nav_favorites").assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_nav_archive").assertDoesNotExist()
    }

    @Test
    fun `navegacao alterna entre Home e Dictionary exibindo DictionaryScreen sem placeholder`() {
        composeTestRule.waitForIdle()

        // 1. Inicialização na Home
        composeTestRule.onNodeWithTag("home_hero_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_home").assertIsSelected()
        composeTestRule.onNodeWithTag("bottom_nav_dictionary").assertIsNotSelected()

        // 2. Clique no item Dicionário
        composeTestRule.onNodeWithTag("bottom_nav_dictionary")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        // 3. Exibição da DictionaryScreen e ausência do placeholder
        composeTestRule.onNodeWithTag("dictionary_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dictionary_header_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("provisional_screen_dictionary").assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_nav_dictionary").assertIsSelected()
        composeTestRule.onNodeWithTag("bottom_nav_home").assertIsNotSelected()

        // 4. Retorno à Home
        composeTestRule.onNodeWithTag("bottom_nav_home")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_hero_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dictionary_screen_root").assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_nav_home").assertIsSelected()
        composeTestRule.onNodeWithTag("bottom_nav_dictionary").assertIsNotSelected()

        // 5. Nova abertura do Dicionário
        composeTestRule.onNodeWithTag("bottom_nav_dictionary")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dictionary_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("provisional_screen_dictionary").assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_nav_dictionary").assertIsSelected()
    }
}
