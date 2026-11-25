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
import com.example.domain.model.SyncFailureType
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
        bookmarkedArticles = 3,
        totalTerritories = 627
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

        composeTestRule.onNodeWithText("Sincronizando catálogo…").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_sync_progress_indicator")
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Sincronização remota do catálogo em andamento")
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
    fun `estado Synced exibe confirmacao data formatada contagem territorial e botao Sincronizar agora habilitado`() {
        val epochMillis = 1_700_000_000_000L
        val expectedFormatted = formatSyncTimestamp(
            epochMillis = epochMillis,
            locale = BRAZILIAN_PORTUGUESE_LOCALE,
            timeZone = TimeZone.getDefault()
        )
        var manualClicks = 0

        composeTestRule.setContent {
            ParintintinTheme {
                OfflineArchiveStatusCard(
                    stats = sampleStats,
                    syncState = SyncState.Synced(
                        lastSyncedEpochMillis = epochMillis,
                        territoriesCount = 627
                    ),
                    onManualSync = { manualClicks += 1 }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Catálogo sincronizado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Última sincronização em $expectedFormatted.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sincronizadas: 627 Terras Indígenas.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sincronizados:", substring = true).assertDoesNotExist()
        assertLocalChipsDisplayed()

        composeTestRule.onNodeWithTag("home_sync_button")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertTextEquals("Sincronizar agora")
            .performClick()

        assertEquals(1, manualClicks)
    }

    @Test
    fun `estado Synced com 1 Terra Indigena exibe texto no singular sem mencionar verbetes`() {
        val epochMillis = 1_700_000_000_000L
        val expectedFormatted = formatSyncTimestamp(
            epochMillis = epochMillis,
            locale = BRAZILIAN_PORTUGUESE_LOCALE,
            timeZone = TimeZone.getDefault()
        )

        composeTestRule.setContent {
            ParintintinTheme {
                OfflineArchiveStatusCard(
                    stats = sampleStats.copy(totalTerritories = 1),
                    syncState = SyncState.Synced(
                        lastSyncedEpochMillis = epochMillis,
                        territoriesCount = 1
                    ),
                    onManualSync = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Catálogo sincronizado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Última sincronização em $expectedFormatted.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sincronizada: 1 Terra Indígena.").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 Terra Indígena local").assertIsDisplayed()
    }

    @Test
    fun `estado OfflineFallback NO_INTERNET oculta mensagem tecnica exibe texto especifico chips locais e botao Tentar novamente`() {
        val epochMillis = 1_700_000_050_000L
        val expectedAttempt = formatSyncTimestamp(
            epochMillis = epochMillis,
            locale = BRAZILIAN_PORTUGUESE_LOCALE,
            timeZone = TimeZone.getDefault()
        )
        val technicalError = "java.net.UnknownHostException: api.internal.test"
        var manualClicks = 0

        composeTestRule.setContent {
            ParintintinTheme {
                OfflineArchiveStatusCard(
                    stats = sampleStats,
                    syncState = SyncState.OfflineFallback(
                        reason = technicalError,
                        lastAttemptEpochMillis = epochMillis,
                        failureType = SyncFailureType.NO_INTERNET,
                        lastSuccessfulSyncEpochMillis = null
                    ),
                    onManualSync = { manualClicks += 1 }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sem conexão com a internet").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "O aplicativo continua utilizando os dados disponíveis localmente. " +
                "A sincronização poderá ser tentada novamente quando houver conexão."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Ainda não houve sincronização oficial concluída.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Última tentativa em $expectedAttempt.").assertIsDisplayed()
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
    fun `estado OfflineFallback UNVALIDATED_NETWORK exibe rede disponivel sem internet validada e ausencia de historico`() {
        val attemptEpochMillis = 1_700_000_055_000L
        val expectedAttempt = formatSyncTimestamp(
            epochMillis = attemptEpochMillis,
            locale = BRAZILIAN_PORTUGUESE_LOCALE,
            timeZone = TimeZone.getDefault()
        )
        var manualClicks = 0

        composeTestRule.setContent {
            ParintintinTheme {
                OfflineArchiveStatusCard(
                    stats = sampleStats,
                    syncState = SyncState.OfflineFallback(
                        reason = "SocketTimeoutException",
                        lastAttemptEpochMillis = attemptEpochMillis,
                        failureType = SyncFailureType.UNVALIDATED_NETWORK,
                        lastSuccessfulSyncEpochMillis = null
                    ),
                    onManualSync = { manualClicks += 1 }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Conexão ainda não validada").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "O dispositivo possui uma rede disponível, mas ainda não foi possível " +
                "confirmar o acesso à internet. Os dados locais continuam disponíveis."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Ainda não houve sincronização oficial concluída.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Última tentativa em $expectedAttempt.").assertIsDisplayed()
        assertLocalChipsDisplayed()

        composeTestRule.onNodeWithTag("home_sync_button")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertTextEquals("Tentar novamente")
            .performClick()

        assertEquals(1, manualClicks)
    }

    @Test
    fun `estado OfflineFallback SERVER_UNAVAILABLE nao afirma ausencia de internet e informa falha remota`() {
        val attemptEpochMillis = 1_700_000_058_000L
        val expectedAttempt = formatSyncTimestamp(
            epochMillis = attemptEpochMillis,
            locale = BRAZILIAN_PORTUGUESE_LOCALE,
            timeZone = TimeZone.getDefault()
        )
        var manualClicks = 0

        composeTestRule.setContent {
            ParintintinTheme {
                OfflineArchiveStatusCard(
                    stats = sampleStats,
                    syncState = SyncState.OfflineFallback(
                        reason = "HTTP 500",
                        lastAttemptEpochMillis = attemptEpochMillis,
                        failureType = SyncFailureType.SERVER_UNAVAILABLE,
                        lastSuccessfulSyncEpochMillis = null
                    ),
                    onManualSync = { manualClicks += 1 }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Não foi possível sincronizar").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "O dispositivo possui conexão, mas o serviço remoto não respondeu " +
                "adequadamente ou apresentou uma falha. Os dados locais continuam disponíveis."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Sem internet", substring = true, ignoreCase = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("Sem conexão", substring = true, ignoreCase = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("Ainda não houve sincronização oficial concluída.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Última tentativa em $expectedAttempt.").assertIsDisplayed()

        composeTestRule.onNodeWithTag("home_sync_button")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertTextEquals("Tentar novamente")
            .performClick()

        assertEquals(1, manualClicks)
    }

    @Test
    fun `estado OfflineFallback com historico oficial exibe timestamp pt_BR sem substituir pela tentativa malsucedida`() {
        val officialEpochMillis = 1_700_000_000_000L
        val failedAttemptEpochMillis = 1_700_120_000_000L
        val expectedOfficial = formatSyncTimestamp(
            epochMillis = officialEpochMillis,
            locale = BRAZILIAN_PORTUGUESE_LOCALE,
            timeZone = TimeZone.getDefault()
        )
        val expectedAttempt = formatSyncTimestamp(
            epochMillis = failedAttemptEpochMillis,
            locale = BRAZILIAN_PORTUGUESE_LOCALE,
            timeZone = TimeZone.getDefault()
        )

        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            composeTestRule.setContent {
                ParintintinTheme {
                    OfflineArchiveStatusCard(
                        stats = sampleStats,
                        syncState = SyncState.OfflineFallback(
                            reason = "HTTP 503",
                            lastAttemptEpochMillis = failedAttemptEpochMillis,
                            failureType = SyncFailureType.SERVER_UNAVAILABLE,
                            lastSuccessfulSyncEpochMillis = officialEpochMillis
                        ),
                        onManualSync = {}
                    )
                }
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Última sincronização oficial em $expectedOfficial.")
                .assertIsDisplayed()
            composeTestRule.onNodeWithText("Última tentativa em $expectedAttempt.")
                .assertIsDisplayed()
            composeTestRule.onNodeWithText("Ainda não houve sincronização oficial concluída.")
                .assertDoesNotExist()
            composeTestRule.onNodeWithText("Última sincronização oficial em $expectedAttempt.")
                .assertDoesNotExist()
            composeTestRule.onNodeWithText("Catálogo sincronizado").assertDoesNotExist()
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun `estado RemoteEmpty sem historico informa ausencia de catalogo publicado nao mostra Catalogo sincronizado e habilita botao`() {
        val attemptEpochMillis = 1_700_000_060_000L
        val expectedAttempt = formatSyncTimestamp(
            epochMillis = attemptEpochMillis,
            locale = BRAZILIAN_PORTUGUESE_LOCALE,
            timeZone = TimeZone.getDefault()
        )
        var manualClicks = 0

        composeTestRule.setContent {
            ParintintinTheme {
                OfflineArchiveStatusCard(
                    stats = sampleStats,
                    syncState = SyncState.RemoteEmpty(
                        lastAttemptEpochMillis = attemptEpochMillis,
                        lastSyncedEpochMillis = null,
                        territoriesCount = null
                    ),
                    onManualSync = { manualClicks += 1 }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Catálogo oficial ainda indisponível").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "O servidor respondeu, mas não retornou registros territoriais publicados. Os dados locais continuam disponíveis."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Última verificação em $expectedAttempt.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Catálogo sincronizado").assertDoesNotExist()
        assertLocalChipsDisplayed()

        composeTestRule.onNodeWithTag("home_sync_button")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertTextEquals("Sincronizar agora")
            .performClick()

        assertEquals(1, manualClicks)
    }

    @Test
    fun `estado RemoteEmpty com historico preserva data oficial anterior sem exibir Catalogo sincronizado`() {
        val officialEpochMillis = 1_700_000_000_000L
        val attemptEpochMillis = 1_700_000_090_000L
        val expectedOfficial = formatSyncTimestamp(
            epochMillis = officialEpochMillis,
            locale = BRAZILIAN_PORTUGUESE_LOCALE,
            timeZone = TimeZone.getDefault()
        )
        val expectedAttempt = formatSyncTimestamp(
            epochMillis = attemptEpochMillis,
            locale = BRAZILIAN_PORTUGUESE_LOCALE,
            timeZone = TimeZone.getDefault()
        )
        var manualClicks = 0

        composeTestRule.setContent {
            ParintintinTheme {
                OfflineArchiveStatusCard(
                    stats = sampleStats,
                    syncState = SyncState.RemoteEmpty(
                        lastAttemptEpochMillis = attemptEpochMillis,
                        lastSyncedEpochMillis = officialEpochMillis,
                        territoriesCount = 627
                    ),
                    onManualSync = { manualClicks += 1 }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sem novos registros publicados").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "O servidor respondeu sem novos registros territoriais. A sincronização oficial anterior continua preservada neste dispositivo."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Última sincronização oficial em $expectedOfficial • Verificado em $expectedAttempt."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Catálogo sincronizado").assertDoesNotExist()
        assertLocalChipsDisplayed()

        composeTestRule.onNodeWithTag("home_sync_button")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertTextEquals("Sincronizar agora")
            .performClick()

        assertEquals(1, manualClicks)
    }

    @Test
    fun `cartao nao sofre overflow horizontal em tela estreita e mantem textos e chips dentro da largura`() {
        val narrowWidth = 280.dp

        composeTestRule.setContent {
            ParintintinTheme {
                Box(modifier = Modifier.width(narrowWidth)) {
                    OfflineArchiveStatusCard(
                        stats = ArchiveStats(
                            totalEntries = 12345,
                            totalArticles = 6789,
                            totalTerritories = 1234
                        ),
                        syncState = SyncState.OfflineFallback(
                            reason = "Timeout",
                            lastAttemptEpochMillis = 1_700_100_050_000L,
                            failureType = SyncFailureType.SERVER_UNAVAILABLE,
                            lastSuccessfulSyncEpochMillis = 1_700_000_000_000L
                        ),
                        onManualSync = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val cardBounds = composeTestRule.onNodeWithTag("home_offline_status_card")
            .fetchSemanticsNode().boundsInRoot
        val titleBounds = composeTestRule.onNodeWithTag("home_sync_status_title")
            .fetchSemanticsNode().boundsInRoot
        val bodyBounds = composeTestRule.onNodeWithTag("home_sync_status_body")
            .fetchSemanticsNode().boundsInRoot
        val historyBounds = composeTestRule.onNodeWithTag("home_sync_official_history")
            .fetchSemanticsNode().boundsInRoot
        val detailBounds = composeTestRule.onNodeWithTag("home_sync_secondary_detail")
            .fetchSemanticsNode().boundsInRoot
        val territoriesBounds = composeTestRule.onNodeWithTag("home_offline_territories_chip")
            .fetchSemanticsNode().boundsInRoot
        val entriesBounds = composeTestRule.onNodeWithTag("home_offline_entries_chip")
            .fetchSemanticsNode().boundsInRoot
        val articlesBounds = composeTestRule.onNodeWithTag("home_offline_articles_chip")
            .fetchSemanticsNode().boundsInRoot
        val buttonBounds = composeTestRule.onNodeWithTag("home_sync_button")
            .fetchSemanticsNode().boundsInRoot

        assert(titleBounds.right <= cardBounds.right)
        assert(bodyBounds.right <= cardBounds.right)
        assert(historyBounds.right <= cardBounds.right)
        assert(detailBounds.right <= cardBounds.right)
        assert(territoriesBounds.right <= cardBounds.right)
        assert(entriesBounds.right <= cardBounds.right)
        assert(articlesBounds.right <= cardBounds.right)
        assert(buttonBounds.right <= cardBounds.right)
    }

    private fun assertLocalChipsDisplayed() {
        composeTestRule.onNodeWithTag("home_offline_territories_chip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_offline_entries_chip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_offline_articles_chip").assertIsDisplayed()
        composeTestRule.onNodeWithText("627 Terras Indígenas locais").assertIsDisplayed()
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
