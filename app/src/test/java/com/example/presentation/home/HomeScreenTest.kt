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
import com.example.domain.model.SyncState
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
        composeTestRule.onNode(
            hasTestTag("demo_mode_notice_body") and hasText("registros fictícios", substring = true)
        ).assertIsDisplayed()
        composeTestRule.onNode(
            hasTestTag("demo_mode_notice_body") and hasText(
                "não representam vocabulário autêntico da língua Parintintin",
                substring = true
            )
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

    @Test
    fun `home exibe versao demonstrativa e texto atualizado do acervo sem afirmar ausencia de dados`() {
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

        composeTestRule.onNodeWithText("VERSÃO 1.0 • DEMONSTRATIVA").assertIsDisplayed()
        composeTestRule.onNodeWithText("NOME PROVISÓRIO • ETAPA 1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("demo_mode_notice_card").assertIsDisplayed()

        composeTestRule.onNodeWithTag("home_list")
            .performScrollToNode(hasText("Acervo em construção"))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            "O acervo linguístico autêntico ainda está em preparação. " +
                "Esta versão utiliza exclusivamente registros fictícios para demonstrar o " +
                "funcionamento do aplicativo, e a inclusão de conteúdo autêntico depende de " +
                "validação e autorização adequadas."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Nenhum dado linguístico será exibido", substring = true)
            .assertDoesNotExist()
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
        composeTestRule.onNodeWithText("Estrutura organizada em Clean Architecture", substring = true)
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
        composeTestRule.onNodeWithText("Estrutura organizada em Clean Architecture", substring = true)
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

        composeTestRule.onNodeWithText("Preservação da Língua Parintintin").assert(isHeading())
        composeTestRule.onNodeWithText("MODO DEMONSTRATIVO").assert(isHeading())
        composeTestRule.onNodeWithTag("demo_mode_notice_body").assert(isHeading().not())

        composeTestRule.onNodeWithTag("home_list")
            .performScrollToNode(hasText("Acervo em construção"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Acervo em construção").assert(isHeading())
        composeTestRule.onNodeWithText(
            "O acervo linguístico autêntico ainda está em preparação.",
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
}
