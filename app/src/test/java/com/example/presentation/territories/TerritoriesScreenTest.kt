package com.example.presentation.territories

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.example.domain.model.IndigenousLand
import com.example.domain.model.SyncFailureType
import com.example.domain.model.SyncState
import com.example.domain.model.TerritoryFilterOptions
import com.example.domain.model.TerritorySortDirection
import com.example.domain.model.TerritorySortField
import com.example.domain.model.TerritorySortOption
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
class TerritoriesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `campo de pesquisa encaminha digitacao e botao limpar zera consulta`() {
        val queries = mutableListOf<String>()
        var currentQuery by mutableStateOf("")

        composeTestRule.setContent {
            ParintintinTheme {
                TerritoriesContent(
                    uiState = TerritoriesUiState(
                        query = currentQuery,
                        syncState = SyncState.Idle,
                        contentState = TerritoriesContentState.Success(emptyList())
                    ),
                    onQueryChange = { newValue ->
                        queries += newValue
                        currentQuery = newValue
                    },
                    onSelectStateUf = {},
                    onSelectPhase = {},
                    onSelectModality = {},
                    onSelectSortField = {},
                    onToggleSortDirection = {},
                    onClearFilters = {},
                    onRetry = {},
                    onManualSync = {},
                    onDismissManualSyncError = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("territories_clear_search").assertDoesNotExist()
        scrollListToTag("territories_search_input")
        composeTestRule.onNodeWithTag("territories_search_input")
            .assertIsDisplayed()
            .performTextInput("Kagwahiva")
        composeTestRule.waitForIdle()

        assertEquals(listOf("Kagwahiva"), queries)

        composeTestRule.onNodeWithTag("territories_clear_search")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("Kagwahiva", ""), queries)
    }

    @Test
    fun `filtros combinaveis de UF fase e modalidade atualizam selecao e acessibilidade`() {
        val ufs = mutableListOf<String?>()
        val phases = mutableListOf<String?>()
        val modalities = mutableListOf<String?>()

        composeTestRule.setContent {
            ParintintinTheme {
                TerritoriesContent(
                    uiState = TerritoriesUiState(
                        selectedStateUf = "AM",
                        selectedPhase = "Homologada",
                        selectedModality = "Tradicionalmente ocupada",
                        filterOptions = TerritoryFilterOptions(
                            statesUf = listOf("AM", "RO"),
                            phases = listOf("Declarada", "Homologada"),
                            modalities = listOf("Reserva Indígena", "Tradicionalmente ocupada")
                        ),
                        syncState = SyncState.Idle,
                        contentState = TerritoriesContentState.Success(emptyList())
                    ),
                    onQueryChange = {},
                    onSelectStateUf = { ufs += it },
                    onSelectPhase = { phases += it },
                    onSelectModality = { modalities += it },
                    onSelectSortField = {},
                    onToggleSortDirection = {},
                    onClearFilters = {},
                    onRetry = {},
                    onManualSync = {},
                    onDismissManualSyncError = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        scrollListToTag("territories_uf_filters")
        composeTestRule.onNodeWithTag("territories_uf_all")
            .assertIsNotSelected()
            .assertContentDescriptionEquals("UF: Todas")
        composeTestRule.onNodeWithTag("territories_uf_AM")
            .assertIsSelected()
            .assertContentDescriptionEquals("UF: AM")
        composeTestRule.onNodeWithTag("territories_uf_RO")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("territories_uf_AM")
            .performScrollTo()
            .performClick()

        scrollListToTag("territories_phase_filters")
        composeTestRule.onNodeWithTag("territories_phase_all").assertIsNotSelected()
        composeTestRule.onNodeWithTag("territories_phase_Homologada")
            .assertIsSelected()
            .assertContentDescriptionEquals("Fase do processo: Homologada")
        composeTestRule.onNodeWithTag("territories_phase_Declarada")
            .performScrollTo()
            .performClick()

        scrollListToTag("territories_modality_filters")
        composeTestRule.onNodeWithTag("territories_modality_all").assertIsNotSelected()
        composeTestRule.onNodeWithTag("territories_modality_Tradicionalmente ocupada")
            .assertIsSelected()
            .assertContentDescriptionEquals("Modalidade: Tradicionalmente ocupada")
        composeTestRule.onNodeWithTag("territories_modality_Reserva Indígena")
            .performScrollTo()
            .performClick()

        assertEquals(listOf("RO", null), ufs)
        assertEquals(listOf("Declarada"), phases)
        assertEquals(listOf("Reserva Indígena"), modalities)
    }

    @Test
    fun `ordenacao exibe os quatro criterios e alterna campo e direcao`() {
        val selectedFields = mutableListOf<TerritorySortField>()
        var toggleCount = 0

        composeTestRule.setContent {
            ParintintinTheme {
                TerritoriesContent(
                    uiState = TerritoriesUiState(
                        sortOption = TerritorySortOption(
                            field = TerritorySortField.SURFACE_AREA,
                            direction = TerritorySortDirection.DESCENDING
                        ),
                        syncState = SyncState.Idle,
                        contentState = TerritoriesContentState.Success(emptyList())
                    ),
                    onQueryChange = {},
                    onSelectStateUf = {},
                    onSelectPhase = {},
                    onSelectModality = {},
                    onSelectSortField = { selectedFields += it },
                    onToggleSortDirection = { toggleCount += 1 },
                    onClearFilters = {},
                    onRetry = {},
                    onManualSync = {},
                    onDismissManualSyncError = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        scrollListToTag("territories_sort_direction_button")
        composeTestRule.onNodeWithTag("territories_sort_direction_button")
            .assertIsDisplayed()
            .assert(hasText("Decrescente"))
            .assertContentDescriptionEquals("Ordem decrescente. Toque para alternar para crescente")
            .performClick()

        scrollListToTag("territories_sort_fields")
        composeTestRule.onNodeWithTag("territories_sort_field_SURFACE_AREA").assertIsSelected()
        composeTestRule.onNodeWithTag("territories_sort_field_NAME")
            .assertIsNotSelected()
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("territories_sort_field_TERRITORY_CODE")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("territories_sort_field_UPDATED_AT")
            .performScrollTo()
            .performClick()

        assertEquals(1, toggleCount)
        assertEquals(
            listOf(
                TerritorySortField.NAME,
                TerritorySortField.TERRITORY_CODE,
                TerritorySortField.UPDATED_AT
            ),
            selectedFields
        )
    }

    @Test
    fun `estado Loading exibe indicador e mensagem de consulta local`() {
        composeTestRule.setContent {
            ParintintinTheme {
                TerritoriesContent(
                    uiState = TerritoriesUiState(
                        syncState = SyncState.Idle,
                        contentState = TerritoriesContentState.Loading
                    ),
                    onQueryChange = {},
                    onSelectStateUf = {},
                    onSelectPhase = {},
                    onSelectModality = {},
                    onSelectSortField = {},
                    onToggleSortDirection = {},
                    onClearFilters = {},
                    onRetry = {},
                    onManualSync = {},
                    onDismissManualSyncError = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        scrollListToTag("territories_loading_state")
        composeTestRule.onNodeWithTag("territories_loading_state").assertIsDisplayed()
        composeTestRule.onNodeWithTag("territories_loading_indicator")
            .performScrollTo()
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Carregando Terras Indígenas do banco local")
        composeTestRule.onNodeWithText("Consultando Terras Indígenas locais…")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `estado Empty sem filtros orienta sincronizacao e oculta limpar filtros`() {
        var syncClicks = 0

        composeTestRule.setContent {
            ParintintinTheme {
                TerritoriesContent(
                    uiState = TerritoriesUiState(
                        syncState = SyncState.Idle,
                        contentState = TerritoriesContentState.Empty(hasActiveFilter = false)
                    ),
                    onQueryChange = {},
                    onSelectStateUf = {},
                    onSelectPhase = {},
                    onSelectModality = {},
                    onSelectSortField = {},
                    onToggleSortDirection = {},
                    onClearFilters = {},
                    onRetry = {},
                    onManualSync = { syncClicks += 1 },
                    onDismissManualSyncError = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        scrollListToTag("territories_empty_state")
        composeTestRule.onNodeWithText("Nenhuma Terra Indígena cadastrada ainda")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("territories_clear_filters_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("territories_empty_sync_button")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals(1, syncClicks)
    }

    @Test
    fun `estado Empty com filtros ativos oferece limpeza e aciona callback`() {
        var cleared = false

        composeTestRule.setContent {
            ParintintinTheme {
                TerritoriesContent(
                    uiState = TerritoriesUiState(
                        query = "Inexistente",
                        selectedStateUf = "AM",
                        filterOptions = TerritoryFilterOptions(statesUf = listOf("AM")),
                        syncState = SyncState.Idle,
                        contentState = TerritoriesContentState.Empty(hasActiveFilter = true)
                    ),
                    onQueryChange = {},
                    onSelectStateUf = {},
                    onSelectPhase = {},
                    onSelectModality = {},
                    onSelectSortField = {},
                    onToggleSortDirection = {},
                    onClearFilters = { cleared = true },
                    onRetry = {},
                    onManualSync = {},
                    onDismissManualSyncError = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        scrollListToTag("territories_empty_state")
        composeTestRule.onNodeWithText("Nenhuma Terra Indígena encontrada")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("territories_clear_filters_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertTrue(cleared)
    }

    @Test
    fun `estado Error exibe mensagem e aciona retry`() {
        var retried = false

        composeTestRule.setContent {
            ParintintinTheme {
                TerritoriesContent(
                    uiState = TerritoriesUiState(
                        syncState = SyncState.Idle,
                        contentState = TerritoriesContentState.Error("Falha de leitura local")
                    ),
                    onQueryChange = {},
                    onSelectStateUf = {},
                    onSelectPhase = {},
                    onSelectModality = {},
                    onSelectSortField = {},
                    onToggleSortDirection = {},
                    onClearFilters = {},
                    onRetry = { retried = true },
                    onManualSync = {},
                    onDismissManualSyncError = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        scrollListToTag("territories_error_state")
        composeTestRule.onNodeWithText("Não foi possível carregar os territórios")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Falha de leitura local")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("territories_retry_button")
            .performScrollTo()
            .performClick()

        assertTrue(retried)
    }

    @Test
    fun `estado Success exibe dados completos e trata campos nulos ou ausentes com seguranca`() {
        val completeLand = IndigenousLand(
            id = "ti_1",
            territoryCode = "12345",
            name = "Tenharim Marmelos",
            ethnicities = listOf("Tenharim", "Parintintin"),
            municipalities = listOf("Humaitá", "Manicoré"),
            statesUf = listOf("AM"),
            surfaceArea = 497521.5,
            phase = "Homologada",
            modality = "Tradicionalmente ocupada",
            updatedAtRaw = "2024-06-15"
        )
        val minimalLand = IndigenousLand(
            id = "ti_2",
            territoryCode = null,
            name = "   ",
            rawEthnicity = null,
            ethnicities = emptyList(),
            rawMunicipality = null,
            municipalities = emptyList(),
            rawStateUf = null,
            statesUf = emptyList(),
            surfaceArea = null,
            phase = null,
            modality = "",
            updatedAtRaw = null
        )

        composeTestRule.setContent {
            ParintintinTheme {
                TerritoriesContent(
                    uiState = TerritoriesUiState(
                        totalLocalTerritoriesCount = 2,
                        syncState = SyncState.Synced(lastSyncedEpochMillis = 1_700_000_000_000L),
                        contentState = TerritoriesContentState.Success(
                            listOf(completeLand, minimalLand)
                        )
                    ),
                    onQueryChange = {},
                    onSelectStateUf = {},
                    onSelectPhase = {},
                    onSelectModality = {},
                    onSelectSortField = {},
                    onToggleSortDirection = {},
                    onClearFilters = {},
                    onRetry = {},
                    onManualSync = {},
                    onDismissManualSyncError = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("territories_total_count_badge")
            .assert(hasText("2 Terras Indígenas locais"))

        scrollListToTag("territory_card_ti_1")
        composeTestRule.onNodeWithTag("territory_name_ti_1")
            .assertIsDisplayed()
            .assert(hasText("Tenharim Marmelos"))
        composeTestRule.onNode(
            hasText("UF: AM") and
                hasContentDescription("UF: AM") and
                hasAnyAncestor(hasTestTag("territory_card_ti_1"))
        ).assertIsDisplayed()
        composeTestRule.onNode(
            hasText("Homologada") and
                hasContentDescription("Fase do processo: Homologada") and
                hasAnyAncestor(hasTestTag("territory_card_ti_1"))
        ).assertIsDisplayed()
        composeTestRule.onNode(
            hasText("Tradicionalmente ocupada") and
                hasContentDescription("Modalidade: Tradicionalmente ocupada") and
                hasAnyAncestor(hasTestTag("territory_card_ti_1"))
        ).assertIsDisplayed()
        composeTestRule.onNode(
            hasText("Tenharim, Parintintin") and
                hasAnyAncestor(hasTestTag("territory_card_ti_1"))
        ).assertIsDisplayed()
        composeTestRule.onNodeWithTag("territory_code_ti_1").assertIsDisplayed()

        scrollListToTag("territory_card_ti_2")
        composeTestRule.onNodeWithTag("territory_name_ti_2")
            .assertIsDisplayed()
            .assert(hasText("Terra Indígena sem nome informado"))
        composeTestRule.onNodeWithTag("territory_uf_ti_2").assertDoesNotExist()
        composeTestRule.onNodeWithTag("territory_phase_ti_2").assertDoesNotExist()
        composeTestRule.onNodeWithTag("territory_modality_ti_2").assertDoesNotExist()
        composeTestRule.onNodeWithTag("territory_ethnicities_ti_2").assertDoesNotExist()
        composeTestRule.onNodeWithTag("territory_code_ti_2").assertDoesNotExist()
    }

    @Test
    fun `barra de sincronizacao respeita Syncing OfflineFallback e erro manual dispensavel`() {
        var currentSyncState by mutableStateOf<SyncState>(SyncState.Syncing)
        var manualError by mutableStateOf<String?>("Tempo limite excedido")
        var syncClicks = 0
        var dismissed = false

        composeTestRule.setContent {
            ParintintinTheme {
                TerritoriesContent(
                    uiState = TerritoriesUiState(
                        syncState = currentSyncState,
                        manualSyncErrorMessage = manualError,
                        contentState = TerritoriesContentState.Empty(hasActiveFilter = false)
                    ),
                    onQueryChange = {},
                    onSelectStateUf = {},
                    onSelectPhase = {},
                    onSelectModality = {},
                    onSelectSortField = {},
                    onToggleSortDirection = {},
                    onClearFilters = {},
                    onRetry = {},
                    onManualSync = { syncClicks += 1 },
                    onDismissManualSyncError = {
                        dismissed = true
                        manualError = null
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("territories_sync_linear_progress").assertIsDisplayed()
        composeTestRule.onNodeWithTag("territories_sync_button")
            .assertIsNotEnabled()
            .assert(hasText("Sincronizando…"))

        composeTestRule.onNodeWithTag("territories_manual_sync_error_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tempo limite excedido").assertIsDisplayed()
        composeTestRule.onNodeWithTag("territories_dismiss_sync_error_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(dismissed)
        composeTestRule.onNodeWithTag("territories_manual_sync_error_banner").assertDoesNotExist()

        currentSyncState = SyncState.OfflineFallback(
            reason = "Sem internet",
            lastAttemptEpochMillis = 1_700_000_000_000L,
            failureType = SyncFailureType.NO_INTERNET
        )
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("territories_sync_button")
            .assertIsEnabled()
            .assert(hasText("Tentar novamente"))
            .performClick()

        assertEquals(1, syncClicks)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w720dp-h900dp")
    fun `layout em tela larga exibe dois cards lado a lado e headings acessiveis`() {
        val lands = listOf(
            IndigenousLand(id = "a1", name = "Terra Alfa", statesUf = listOf("AM")),
            IndigenousLand(id = "a2", name = "Terra Beta", statesUf = listOf("RO"))
        )

        composeTestRule.setContent {
            ParintintinTheme {
                Box(modifier = Modifier.width(720.dp)) {
                    TerritoriesContent(
                        uiState = TerritoriesUiState(
                            totalLocalTerritoriesCount = 2,
                            filterOptions = TerritoryFilterOptions(statesUf = listOf("AM", "RO")),
                            syncState = SyncState.Idle,
                            contentState = TerritoriesContentState.Success(lands)
                        ),
                        onQueryChange = {},
                        onSelectStateUf = {},
                        onSelectPhase = {},
                        onSelectModality = {},
                        onSelectSortField = {},
                        onToggleSortDirection = {},
                        onClearFilters = {},
                        onRetry = {},
                        onManualSync = {},
                        onDismissManualSyncError = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Terras Indígenas").assert(isHeading())
        composeTestRule.onNodeWithText("Unidade Federativa (UF)").assert(isHeading())
        composeTestRule.onNodeWithText("Ordenar por").assert(isHeading())
        composeTestRule.onNodeWithText("Terras Indígenas disponíveis").assert(isHeading())
        composeTestRule.onNodeWithTag("territories_total_count_badge").assert(isHeading().not())

        scrollListToTag("territory_card_a1")
        val firstBounds = composeTestRule.onNodeWithTag("territory_card_a1")
            .fetchSemanticsNode()
            .boundsInRoot
        val secondBounds = composeTestRule.onNodeWithTag("territory_card_a2")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(
            "Em largura de 720dp, os dois cards devem ficar lado a lado",
            secondBounds.left > firstBounds.left
        )
    }

    @Test
    fun `clicar em um territorio encaminha seu ID para selecao`() {
        val selectedIds = mutableListOf<String>()

        val land = IndigenousLand(
            id = "ti_tenharim_001",
            territoryCode = "12345",
            name = "Tenharim Marmelos",
            statesUf = listOf("AM")
        )

        composeTestRule.setContent {
            ParintintinTheme {
                TerritoriesContent(
                    uiState = TerritoriesUiState(
                        totalLocalTerritoriesCount = 1,
                        syncState = SyncState.Idle,
                        contentState = TerritoriesContentState.Success(listOf(land))
                    ),
                    onQueryChange = {},
                    onSelectStateUf = {},
                    onSelectPhase = {},
                    onSelectModality = {},
                    onSelectSortField = {},
                    onToggleSortDirection = {},
                    onClearFilters = {},
                    onRetry = {},
                    onManualSync = {},
                    onDismissManualSyncError = {},
                    onSelectTerritory = { selectedIds += it }
                )
            }
        }

        composeTestRule.waitForIdle()
        scrollListToTag("territory_card_ti_tenharim_001")

        composeTestRule.onNodeWithTag("territory_card_ti_tenharim_001")
            .assertIsDisplayed()
            .performClick()

        assertEquals(listOf("ti_tenharim_001"), selectedIds)
    }
    private fun scrollListToTag(tag: String) {
        composeTestRule.onNodeWithTag("territories_list")
            .performScrollToNode(hasTestTag(tag))
        composeTestRule.waitForIdle()
    }
}
