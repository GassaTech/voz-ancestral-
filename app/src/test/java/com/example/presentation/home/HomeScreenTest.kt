package com.example.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.example.domain.model.ArchiveStats
import com.example.domain.model.SyncState
import com.example.ui.theme.ParintintinTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `aviso MODO DEMONSTRATIVO aparece na HomeScreen sem exigir interacao e informa registros ficticios`() {
        composeTestRule.setContent {
            ParintintinTheme {
                HomeContent(
                    uiState = HomeUiState(
                        archiveStats = ArchiveStats(totalEntries = 20, totalArticles = 0),
                        syncState = SyncState.Idle
                    ),
                    onManualSync = {},
                    onToggleStageDetails = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_hero_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("demo_mode_notice_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("demo_mode_notice_title")
            .assertIsDisplayed()
            .assertTextEquals("MODO DEMONSTRATIVO")
        composeTestRule.onNodeWithTag("demo_mode_notice_body")
            .assertIsDisplayed()
            .assertTextEquals(
                "Este aplicativo utiliza registros fictícios para demonstrar suas funcionalidades. " +
                    "As palavras e traduções exibidas não representam vocabulário autêntico da língua Parintintin."
            )
        composeTestRule.onNodeWithText("registros fictícios", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "não representam vocabulário autêntico da língua Parintintin",
            substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun `aviso na HomeScreen preserva hierarquia abaixo do banner e sem overflow em 280dp`() {
        composeTestRule.setContent {
            ParintintinTheme {
                Box(modifier = Modifier.width(280.dp)) {
                    HomeContent(
                        uiState = HomeUiState(
                            archiveStats = ArchiveStats(totalEntries = 20, totalArticles = 0),
                            syncState = SyncState.Idle
                        ),
                        onManualSync = {},
                        onToggleStageDetails = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val rootBounds = composeTestRule.onNodeWithTag("home_screen_root")
            .fetchSemanticsNode().boundsInRoot
        val heroBounds = composeTestRule.onNodeWithTag("home_hero_banner")
            .fetchSemanticsNode().boundsInRoot
        val noticeBounds = composeTestRule.onNodeWithTag("demo_mode_notice_card")
            .fetchSemanticsNode().boundsInRoot

        assertTrue(noticeBounds.top >= heroBounds.bottom)
        assertTrue(noticeBounds.right <= rootBounds.right)
    }
}
