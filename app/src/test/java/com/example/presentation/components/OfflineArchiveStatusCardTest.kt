package com.example.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.domain.model.ArchiveStats
import com.example.domain.model.SyncState
import com.example.ui.theme.ParintintinTheme
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfflineArchiveStatusCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleStats = ArchiveStats(
        totalEntries = 42,
        favoriteEntries = 5,
        totalArticles = 12,
        bookmarkedArticles = 3
    )

    @Test
    fun `estado Loading exibe consulta local chips e botao Sincronizar agora habilitado`() {
        var manualClicks = 0

        composeTestRule.setContent {
            ParintintinTheme {
                OfflineArchiveStatusCard(
                    stats = sampleStats,
                    syncState = SyncState.Loading,
                    onManualSync = { manualClicks += 1 }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_offline_status_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Consultando histórico local…").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_sync_progress_indicator")
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Consultando histórico de sincronização local")
        assertLocalChipsDisplayed()

        composeTestRule.onNodeWithTag("home_sync_button")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertTextEquals("Sincronizar agora")
            .performClick()

        assertEquals(1, manualClicks)
    }

    @Test
    fun `estado Idle exibe ausencia de sincronizacao remota chips e botao habilitado`() {
        var manualClicks = 0

        composeTestRule.setContent {
            ParintintinTheme {
                OfflineArchiveStatusCard(
                    stats = sampleStats,
                    syncState = SyncState.Idle,
                    onManualSync = { manualClicks += 1 }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Pronto para sincronizar").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Base local preparada. Ainda não houve sincronização remota neste dispositivo."
        ).assertIsDisplayed()
        assertLocalChipsDisplayed()

        composeTestRule.onNodeWithTag("home_sync_button")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertTextEquals("Sincronizar agora")
            .performClick()

        assertEquals(1, manualClicks)
    }

    @Test
    fun `estado Syncing exibe progresso chips locais e mantem botao visivel e desabilitado`() {
        var manualClicks = 0

        composeTestRule.setContent {
            ParintintinTheme {
                OfflineArchiveStatusCard(
                    stats = sampleStats,
                    syncState = SyncState.Syncing,
                    onManualSync = { manualClicks += 1 }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sincronizando acervo…").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_sync_progress_indicator")
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Sincronização remota do acervo em andamento")
        composeTestRule.onNodeWithTag("home_sync_linear_progress").assertIsDisplayed()
        assertLocalChipsDisplayed()

        composeTestRule.onNodeWithTag("home_sync_button")
            .assertIsDisplayed()
            .assertIsNotEnabled()
            .assertTextEquals("Sincronizando…")
            .performClick()

        assertEquals(0, manualClicks)
    }

    @Test
    fun `estado Synced exibe confirmacao data formatada chips locais e botao Sincronizar agora habilitado`() {
        val epochMillis = 1_700_000_000_000L
        val expectedFormatted = formatSyncTimestamp(
            epochMillis = epochMillis,
            locale = Locale.getDefault(),
            timeZone = TimeZone.getDefault()
        )
        var manualClicks = 0

        composeTestRule.setContent {
            ParintintinTheme {
                OfflineArchiveStatusCard(
                    stats = sampleStats,
                    syncState = SyncState.Synced(
                        lastSyncedEpochMillis = epochMillis,
                        entriesCount = 42,
                        articlesCount = 12
                    ),
                    onManualSync = { manualClicks += 1 }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Acervo sincronizado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Última sincronização em $expectedFormatted.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sincronizados: 42 verbetes e 12 artigos.").assertIsDisplayed()
        assertLocalChipsDisplayed()

        composeTestRule.onNodeWithTag("home_sync_button")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertTextEquals("Sincronizar agora")
            .performClick()

        assertEquals(1, manualClicks)
    }

    @Test
    fun `estado OfflineFallback oculta mensagem tecnica exibe texto amigavel chips locais e botao Tentar novamente`() {
        val epochMillis = 1_700_000_050_000L
        val technicalError = "java.net.UnknownHostException: api.internal.test"
        var manualClicks = 0

        composeTestRule.setContent {
            ParintintinTheme {
                OfflineArchiveStatusCard(
                    stats = sampleStats,
                    syncState = SyncState.OfflineFallback(
                        reason = technicalError,
                        lastAttemptEpochMillis = epochMillis
                    ),
                    onManualSync = { manualClicks += 1 }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Modo offline ativo").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Não foi possível atualizar agora, mas os dados locais continuam disponíveis para consulta."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(technicalError, substring = true).assertDoesNotExist()
        assertLocalChipsDisplayed()

        composeTestRule.onNodeWithTag("home_sync_button")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertTextEquals("Tentar novamente")
            .performClick()

        assertEquals(1, manualClicks)
    }

    @Test
    fun `cartao nao sofre overflow horizontal em tela estreita e mantem chips dentro da largura`() {
        val narrowWidth = 280.dp

        composeTestRule.setContent {
            ParintintinTheme {
                Box(modifier = Modifier.width(narrowWidth)) {
                    OfflineArchiveStatusCard(
                        stats = ArchiveStats(
                            totalEntries = 12345,
                            totalArticles = 6789
                        ),
                        syncState = SyncState.OfflineFallback(
                            reason = "Timeout",
                            lastAttemptEpochMillis = 1_700_000_050_000L
                        ),
                        onManualSync = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val cardBounds = composeTestRule.onNodeWithTag("home_offline_status_card")
            .fetchSemanticsNode().boundsInRoot
        val entriesBounds = composeTestRule.onNodeWithTag("home_offline_entries_chip")
            .fetchSemanticsNode().boundsInRoot
        val articlesBounds = composeTestRule.onNodeWithTag("home_offline_articles_chip")
            .fetchSemanticsNode().boundsInRoot
        val buttonBounds = composeTestRule.onNodeWithTag("home_sync_button")
            .fetchSemanticsNode().boundsInRoot

        assert(entriesBounds.right <= cardBounds.right)
        assert(articlesBounds.right <= cardBounds.right)
        assert(buttonBounds.right <= cardBounds.right)
    }

    private fun assertLocalChipsDisplayed() {
        composeTestRule.onNodeWithTag("home_offline_entries_chip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_offline_articles_chip").assertIsDisplayed()
        composeTestRule.onNodeWithText("42 verbetes locais").assertIsDisplayed()
        composeTestRule.onNodeWithText("12 artigos locais").assertIsDisplayed()
    }

    @Test
    fun `titulo estrutural do cartao offline possui semantica de heading e corpo comum nao`() {
        composeTestRule.setContent {
            ParintintinTheme {
                OfflineArchiveStatusCard(
                    stats = sampleStats,
                    syncState = SyncState.Idle,
                    onManualSync = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Armazenamento Local (Offline-First)").assert(isHeading())
        composeTestRule.onNodeWithText(
            "Base local preparada. Ainda não houve sincronização remota neste dispositivo."
        ).assert(isHeading().not())
    }
}
