package com.example.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.example.domain.model.ArchiveStats
import com.example.domain.model.SyncFailureType
import com.example.domain.model.SyncState
import com.example.presentation.components.BRAZILIAN_PORTUGUESE_LOCALE
import com.example.presentation.components.formatSyncTimestamp
import com.example.ui.theme.ParintintinTheme
import org.junit.Assert.assertEquals
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
    fun `HomeScreen apresenta identidade Voz Ancestral e nao exibe aviso MODO DEMONSTRATIVO na tela inicial`() {
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
        composeTestRule.onNodeWithTag("home_hero_badge")
            .assertIsDisplayed()
            .assertTextEquals("CATÁLOGO DIGITAL • OFFLINE-FIRST")
        composeTestRule.onNodeWithTag("home_hero_title")
            .assertIsDisplayed()
            .assertTextEquals("Terras e Povos Indígenas do Brasil")
        composeTestRule.onNodeWithTag("home_hero_subtitle")
            .assertIsDisplayed()
            .assertTextEquals(
                "Plataforma offline-first para consulta de terras indígenas, saberes culturais e preservação linguística."
            )
        composeTestRule.onNodeWithTag("demo_mode_notice_card").assertDoesNotExist()
        composeTestRule.onNodeWithText("MODO DEMONSTRATIVO").assertDoesNotExist()
    }

    @Test
    fun `introducao na HomeScreen preserva hierarquia abaixo do banner e sem overflow em 280dp`() {
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
        val introBounds = composeTestRule.onNodeWithTag("intro_notice_card")
            .fetchSemanticsNode().boundsInRoot

        assertTrue(introBounds.top >= heroBounds.bottom)
        assertTrue(introBounds.right <= rootBounds.right)
    }

    @Test
    fun `home exibe badge do catalogo digital e esclarece limites da API IBAMA e do Dicionario`() {
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

        composeTestRule.onNodeWithText("CATÁLOGO DIGITAL • OFFLINE-FIRST").assertIsDisplayed()
        composeTestRule.onNodeWithText("VERSÃO 1.0 • DEMONSTRATIVA").assertDoesNotExist()
        composeTestRule.onNodeWithTag("demo_mode_notice_card").assertDoesNotExist()

        composeTestRule.onNodeWithTag("home_list")
            .performScrollToNode(hasText("Sobre o catálogo e suas fontes"))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            "A aba Territórios reúne dados territoriais oficiais e etnias associadas da API " +
                "ArcGIS REST do IBAMA/SIGAGEO (Camada 25), que não representam perfis culturais, " +
                "históricos ou linguísticos completos. A aba Dicionário exibe registros demonstrativos " +
                "em português enquanto o acervo linguístico autêntico aguarda curadoria."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Acervo em construção").assertDoesNotExist()
    }

    @Test
    fun `modulos planejados omitem Dicionario e preservam os modulos futuros`() {
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

        composeTestRule.onNodeWithTag("home_list")
            .performScrollToNode(hasText("Módulos Planejados"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Dicionário Parintintin–Português").assertDoesNotExist()

        composeTestRule.onNodeWithTag("home_list")
            .performScrollToNode(hasText("Enciclopédia Cultural e Linguística"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Enciclopédia Cultural e Linguística").assertIsDisplayed()

        composeTestRule.onNodeWithTag("home_list")
            .performScrollToNode(hasText("Pronúncias em Áudio"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Pronúncias em Áudio").assertIsDisplayed()

        composeTestRule.onNodeWithTag("home_list")
            .performScrollToNode(hasText("Pesquisa e Favoritos"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Pesquisa e Favoritos").assertIsDisplayed()

        composeTestRule.onNodeWithText("Dicionário Parintintin–Português").assertDoesNotExist()
    }

    @Test
    fun `cartao expansivel anuncia estado recolhido e atualiza semantica ao expandir e recolher`() {
        var expanded by mutableStateOf(false)

        composeTestRule.setContent {
            ParintintinTheme {
                HomeContent(
                    uiState = HomeUiState(
                        archiveStats = ArchiveStats(totalEntries = 20, totalArticles = 0),
                        syncState = SyncState.Idle,
                        isStageDetailsExpanded = expanded
                    ),
                    onManualSync = {},
                    onToggleStageDetails = { expanded = !expanded }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_list")
            .performScrollToNode(hasTestTag("stage_details_toggle"))
        composeTestRule.waitForIdle()

        // Estado inicial recolhido
        composeTestRule.onNodeWithTag("stage_details_toggle")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Recolhido"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertEquals(
            "Mostrar detalhes",
            composeTestRule.onNodeWithTag("stage_details_toggle")
                .fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)?.label
        )
        composeTestRule.onNodeWithText("Estrutura em Clean Architecture", substring = true)
            .assertDoesNotExist()

        // Expande ao clicar
        composeTestRule.onNodeWithTag("stage_details_toggle").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("stage_details_toggle")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expandido"))
        assertEquals(
            "Ocultar detalhes",
            composeTestRule.onNodeWithTag("stage_details_toggle")
                .fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)?.label
        )
        composeTestRule.onNodeWithText("Estrutura em Clean Architecture", substring = true)
            .assertIsDisplayed()

        // Recolhe novamente ao clicar
        composeTestRule.onNodeWithTag("stage_details_toggle").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("stage_details_toggle")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Recolhido"))
        assertEquals(
            "Mostrar detalhes",
            composeTestRule.onNodeWithTag("stage_details_toggle")
                .fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)?.label
        )
    }

    @Test
    fun `titulos estruturais da Home possuem semantica de heading e textos comuns nao`() {
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

        composeTestRule.onNodeWithText("Terras e Povos Indígenas do Brasil").assert(isHeading())
        composeTestRule.onNodeWithTag("home_hero_subtitle").assert(isHeading().not())

        composeTestRule.onNodeWithTag("home_list")
            .performScrollToNode(hasText("Sobre o catálogo e suas fontes"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Sobre o catálogo e suas fontes").assert(isHeading())
        composeTestRule.onNodeWithText(
            "A aba Territórios reúne dados territoriais oficiais",
            substring = true
        ).assert(isHeading().not())

        composeTestRule.onNodeWithTag("home_list")
            .performScrollToNode(hasText("Armazenamento Local (Offline-First)"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Armazenamento Local (Offline-First)").assert(isHeading())

        composeTestRule.onNodeWithTag("home_list")
            .performScrollToNode(hasText("Módulos Planejados"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Módulos Planejados").assert(isHeading())
    }

    @Test
    fun `home exibe estados diferenciados de falha e sincronizacao mantendo botao manual`() {
        var currentSyncState by mutableStateOf<SyncState>(
            SyncState.OfflineFallback(
                reason = "Offline",
                lastAttemptEpochMillis = 1_700_000_010_000L,
                failureType = SyncFailureType.NO_INTERNET,
                lastSuccessfulSyncEpochMillis = null
            )
        )
        var manualSyncCount = 0

        composeTestRule.setContent {
            ParintintinTheme {
                HomeContent(
                    uiState = HomeUiState(
                        archiveStats = ArchiveStats(totalEntries = 20, totalArticles = 0),
                        syncState = currentSyncState
                    ),
                    onManualSync = { manualSyncCount += 1 },
                    onToggleStageDetails = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // NO_INTERNET
        composeTestRule.onNodeWithTag("home_list")
            .performScrollToNode(hasTestTag("home_offline_status_card"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Sem conexão com a internet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ainda não houve sincronização oficial concluída.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_sync_button").performClick()
        assertEquals(1, manualSyncCount)

        // UNVALIDATED_NETWORK
        currentSyncState = SyncState.OfflineFallback(
            reason = "Captive portal",
            lastAttemptEpochMillis = 1_700_000_020_000L,
            failureType = SyncFailureType.UNVALIDATED_NETWORK,
            lastSuccessfulSyncEpochMillis = null
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Conexão ainda não validada").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_sync_button").performClick()
        assertEquals(2, manualSyncCount)

        // SERVER_UNAVAILABLE com histórico oficial
        val officialEpoch = 1_700_000_000_000L
        val expectedOfficial = formatSyncTimestamp(
            epochMillis = officialEpoch,
            locale = BRAZILIAN_PORTUGUESE_LOCALE
        )
        currentSyncState = SyncState.OfflineFallback(
            reason = "HTTP 500",
            lastAttemptEpochMillis = 1_700_000_030_000L,
            failureType = SyncFailureType.SERVER_UNAVAILABLE,
            lastSuccessfulSyncEpochMillis = officialEpoch
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Não foi possível sincronizar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sem internet", substring = true, ignoreCase = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("Última sincronização oficial em $expectedOfficial.")
            .assertIsDisplayed()

        // RemoteEmpty sem histórico
        currentSyncState = SyncState.RemoteEmpty(
            lastAttemptEpochMillis = 1_700_000_040_000L,
            lastSyncedEpochMillis = null
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Catálogo oficial ainda indisponível").assertIsDisplayed()

        // Synced exibe confirmacao de catalogo sincronizado
        currentSyncState = SyncState.Synced(
            lastSyncedEpochMillis = officialEpoch,
            territoriesCount = 627
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Catálogo sincronizado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sincronizadas: 627 Terras Indígenas.").assertIsDisplayed()
    }
}
