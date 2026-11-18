package com.example.presentation.dictionary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
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
import com.example.domain.model.DictionaryEntry
import com.example.domain.model.GrammaticalClass
import com.example.domain.model.SemanticCategory
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
class DictionaryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `campo de pesquisa encaminha digitacao e botao limpar zera consulta`() {
        val recordedQueries = mutableListOf<String>()
        var currentQuery by mutableStateOf("")

        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        query = currentQuery,
                        contentState = DictionaryEntriesState.Success(emptyList())
                    ),
                    onQueryChange = { newValue ->
                        recordedQueries += newValue
                        currentQuery = newValue
                    },
                    onSelectCategory = {},
                    onSelectGrammaticalClass = {},
                    onClearFilters = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dictionary_clear_search").assertDoesNotExist()
        composeTestRule.onNodeWithTag("dictionary_search_input")
            .assertIsDisplayed()
            .performTextInput("TERMO_TESTE")
        composeTestRule.waitForIdle()

        assertEquals(listOf("TERMO_TESTE"), recordedQueries)

        composeTestRule.onNodeWithTag("dictionary_clear_search")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("TERMO_TESTE", ""), recordedQueries)
    }

    @Test
    fun `selecao e remocao de categoria semantica atualizam estado visual e callbacks`() {
        val selectedCategories = mutableListOf<SemanticCategory?>()

        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        selectedCategory = SemanticCategory.FOOD,
                        contentState = DictionaryEntriesState.Success(emptyList())
                    ),
                    onQueryChange = {},
                    onSelectCategory = { selectedCategories += it },
                    onSelectGrammaticalClass = {},
                    onClearFilters = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dictionary_category_all").assertIsNotSelected()
        composeTestRule.onNodeWithTag("dictionary_category_${SemanticCategory.FOOD.name}")
            .assertIsSelected()

        composeTestRule.onNodeWithTag("dictionary_category_${SemanticCategory.ANIMALS.name}")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("dictionary_category_${SemanticCategory.FOOD.name}")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("dictionary_category_all")
            .performScrollTo()
            .performClick()

        assertEquals(
            listOf(SemanticCategory.ANIMALS, null, null),
            selectedCategories
        )
    }

    @Test
    fun `selecao e remocao de classe gramatical atualizam estado visual e callbacks`() {
        val selectedClasses = mutableListOf<GrammaticalClass?>()

        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        selectedGrammaticalClass = GrammaticalClass.VERB,
                        contentState = DictionaryEntriesState.Success(emptyList())
                    ),
                    onQueryChange = {},
                    onSelectCategory = {},
                    onSelectGrammaticalClass = { selectedClasses += it },
                    onClearFilters = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dictionary_grammatical_class_all").assertIsNotSelected()
        composeTestRule.onNodeWithTag("dictionary_grammatical_class_${GrammaticalClass.VERB.name}")
            .assertIsSelected()

        composeTestRule.onNodeWithTag("dictionary_grammatical_class_${GrammaticalClass.NOUN.name}")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("dictionary_grammatical_class_${GrammaticalClass.VERB.name}")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("dictionary_grammatical_class_all")
            .performScrollTo()
            .performClick()

        assertEquals(
            listOf(GrammaticalClass.NOUN, null, null),
            selectedClasses
        )
    }

    @Test
    fun `estado Loading exibe indicador e mensagem de consulta local`() {
        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        contentState = DictionaryEntriesState.Loading
                    ),
                    onQueryChange = {},
                    onSelectCategory = {},
                    onSelectGrammaticalClass = {},
                    onClearFilters = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        scrollListToTag("dictionary_loading_state")
        composeTestRule.onNodeWithTag("dictionary_loading_state").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dictionary_loading_indicator")
            .performScrollTo()
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Carregando verbetes do dicionário local")
        composeTestRule.onNodeWithText("Consultando verbetes locais…")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `estado Empty sem filtros informa ausencia de cadastro e oculta limpar filtros`() {
        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        contentState = DictionaryEntriesState.Empty(hasActiveFilter = false)
                    ),
                    onQueryChange = {},
                    onSelectCategory = {},
                    onSelectGrammaticalClass = {},
                    onClearFilters = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        scrollListToTag("dictionary_empty_state")
        composeTestRule.onNodeWithTag("dictionary_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nenhum verbete cadastrado ainda")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("dictionary_clear_filters_button").assertDoesNotExist()
    }

    @Test
    fun `estado Empty com filtros exibe botao Limpar filtros e aciona callback`() {
        var clearClicks = 0

        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        query = "TERMO_INEXISTENTE",
                        contentState = DictionaryEntriesState.Empty(hasActiveFilter = true)
                    ),
                    onQueryChange = {},
                    onSelectCategory = {},
                    onSelectGrammaticalClass = {},
                    onClearFilters = { clearClicks += 1 },
                    onRetry = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        scrollListToTag("dictionary_empty_state")
        composeTestRule.onNodeWithTag("dictionary_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nenhum verbete encontrado")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("dictionary_clear_filters_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, clearClicks)
    }

    @Test
    fun `estado Error exibe mensagem e aciona onRetry ao clicar em Tentar novamente`() {
        var retryClicks = 0
        val errorMessage = "Falha simulada ao consultar banco local"

        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        contentState = DictionaryEntriesState.Error(errorMessage)
                    ),
                    onQueryChange = {},
                    onSelectCategory = {},
                    onSelectGrammaticalClass = {},
                    onClearFilters = {},
                    onRetry = { retryClicks += 1 }
                )
            }
        }
        composeTestRule.waitForIdle()

        scrollListToTag("dictionary_error_state")
        composeTestRule.onNodeWithTag("dictionary_error_state").assertIsDisplayed()
        composeTestRule.onNodeWithText("Não foi possível carregar os verbetes")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(errorMessage)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("dictionary_retry_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, retryClicks)
    }

    @Test
    fun `estado Success exibe dados completos dos verbetes`() {
        val completeEntry = sampleEntry(
            id = "e1",
            word = "VERBETE_COMPLETO",
            phoneticIpa = "[ipa.1]",
            translation = "Tradução ilustrativa 1",
            grammaticalClass = GrammaticalClass.NOUN,
            semanticCategory = SemanticCategory.NATURE,
            exampleParintintin = "Exemplo ilustrativo na língua",
            examplePortuguese = "Exemplo ilustrativo em português",
            culturalNote = "Nota cultural ilustrativa"
        )
        val minimalEntry = sampleEntry(
            id = "e2",
            word = "VERBETE_MINIMO",
            phoneticIpa = "",
            translation = "Tradução ilustrativa 2",
            grammaticalClass = GrammaticalClass.VERB,
            semanticCategory = SemanticCategory.EVERYDAY,
            exampleParintintin = "",
            examplePortuguese = "",
            culturalNote = ""
        )

        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        contentState = DictionaryEntriesState.Success(
                            listOf(completeEntry, minimalEntry)
                        )
                    ),
                    onQueryChange = {},
                    onSelectCategory = {},
                    onSelectGrammaticalClass = {},
                    onClearFilters = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        scrollListToTag("dictionary_entry_card_e1")
        composeTestRule.onNodeWithTag("dictionary_entry_card_e1").assertIsDisplayed()
        composeTestRule.onNodeWithText("VERBETE_COMPLETO").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("[ipa.1]").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Tradução ilustrativa 1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNode(
            hasText("Substantivo") and hasAnyAncestor(hasTestTag("dictionary_entry_card_e1"))
        ).performScrollTo().assertIsDisplayed()
        composeTestRule.onNode(
            hasText("Natureza e Território") and hasAnyAncestor(hasTestTag("dictionary_entry_card_e1"))
        ).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Exemplo de uso").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Exemplo ilustrativo na língua").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Exemplo ilustrativo em português").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Nota cultural").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Nota cultural ilustrativa").performScrollTo().assertIsDisplayed()

        scrollListToTag("dictionary_entry_card_e2")
        composeTestRule.onNodeWithTag("dictionary_entry_card_e2").assertIsDisplayed()
        composeTestRule.onNodeWithText("VERBETE_MINIMO").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Tradução ilustrativa 2").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `estado Success oculta exemplo e nota cultural quando campos estao vazios`() {
        val minimalEntry = sampleEntry(
            id = "e2",
            word = "VERBETE_MINIMO",
            phoneticIpa = "",
            translation = "Tradução ilustrativa 2",
            grammaticalClass = GrammaticalClass.VERB,
            semanticCategory = SemanticCategory.EVERYDAY,
            exampleParintintin = "",
            examplePortuguese = "",
            culturalNote = ""
        )

        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        contentState = DictionaryEntriesState.Success(listOf(minimalEntry))
                    ),
                    onQueryChange = {},
                    onSelectCategory = {},
                    onSelectGrammaticalClass = {},
                    onClearFilters = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        scrollListToTag("dictionary_entry_card_e2")
        composeTestRule.onNodeWithTag("dictionary_entry_card_e2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exemplo de uso").assertDoesNotExist()
        composeTestRule.onNodeWithText("Nota cultural").assertDoesNotExist()
    }

    @Test
    fun `layout estreito nao sofre overflow horizontal dentro de 280dp`() {
        val sample = sampleEntry(
            id = "narrow_1",
            word = "VERBETE_EXTENSO_ILUSTRATIVO",
            phoneticIpa = "[ipa.extenso]",
            translation = "Tradução ilustrativa suficientemente longa para validar quebra de linha",
            grammaticalClass = GrammaticalClass.EXPRESSION,
            semanticCategory = SemanticCategory.CULTURE_SPIRIT,
            exampleParintintin = "Frase de exemplo ilustrativa para validar largura estreita",
            examplePortuguese = "Tradução do exemplo ilustrativo",
            culturalNote = "Nota cultural ilustrativa extensa"
        )

        composeTestRule.setContent {
            ParintintinTheme {
                Box(modifier = Modifier.width(280.dp)) {
                    DictionaryContent(
                        uiState = DictionaryUiState(
                            query = "termo",
                            selectedCategory = SemanticCategory.CULTURE_SPIRIT,
                            contentState = DictionaryEntriesState.Success(listOf(sample))
                        ),
                        onQueryChange = {},
                        onSelectCategory = {},
                        onSelectGrammaticalClass = {},
                        onClearFilters = {},
                        onRetry = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val rootBounds = composeTestRule.onNodeWithTag("dictionary_screen_root")
            .fetchSemanticsNode().boundsInRoot
        val headerBounds = composeTestRule.onNodeWithTag("dictionary_header_card")
            .fetchSemanticsNode().boundsInRoot
        val searchBounds = composeTestRule.onNodeWithTag("dictionary_search_input")
            .fetchSemanticsNode().boundsInRoot

        assertTrue(headerBounds.right <= rootBounds.right)
        assertTrue(searchBounds.right <= rootBounds.right)

        scrollListToTag("dictionary_entry_card_narrow_1")
        val cardBounds = composeTestRule.onNodeWithTag("dictionary_entry_card_narrow_1")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(cardBounds.right <= rootBounds.right)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w720dp-h900dp")
    fun `layout em tela larga a partir de 600dp organiza verbetes em duas colunas`() {
        val entries = listOf(
            sampleEntry(id = "col_1", word = "VERBETE_A"),
            sampleEntry(id = "col_2", word = "VERBETE_B")
        )

        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        contentState = DictionaryEntriesState.Success(entries)
                    ),
                    onQueryChange = {},
                    onSelectCategory = {},
                    onSelectGrammaticalClass = {},
                    onClearFilters = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        scrollListToTag("dictionary_entry_card_col_1")
        scrollListToTag("dictionary_entry_card_col_2")

        val firstBounds = composeTestRule.onNodeWithTag("dictionary_entry_card_col_1")
            .fetchSemanticsNode().boundsInRoot
        val secondBounds = composeTestRule.onNodeWithTag("dictionary_entry_card_col_2")
            .fetchSemanticsNode().boundsInRoot

        assertTrue(secondBounds.left > firstBounds.left)
        assertEquals(firstBounds.top, secondBounds.top, 2f)
    }

    @Test
    fun `cabecalho exibe zero verbetes locais quando banco esta vazio`() {
        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        totalLocalEntriesCount = 0,
                        contentState = DictionaryEntriesState.Empty(hasActiveFilter = false)
                    ),
                    onQueryChange = {},
                    onSelectCategory = {},
                    onSelectGrammaticalClass = {},
                    onClearFilters = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dictionary_total_entries_count")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("0 verbetes locais").assertIsDisplayed()
    }

    @Test
    fun `cabecalho mantem total local quando pesquisa nao encontra resultados`() {
        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        query = "TERMO_INEXISTENTE",
                        totalLocalEntriesCount = 12,
                        contentState = DictionaryEntriesState.Empty(hasActiveFilter = true)
                    ),
                    onQueryChange = {},
                    onSelectCategory = {},
                    onSelectGrammaticalClass = {},
                    onClearFilters = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dictionary_total_entries_count")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("12 verbetes locais").assertIsDisplayed()
        scrollListToTag("dictionary_empty_state")
        composeTestRule.onNodeWithText("Nenhum verbete encontrado").assertIsDisplayed()
    }

    @Test
    fun `cabecalho exibe total local separado da contagem de resultados filtrados`() {
        val filteredEntries = listOf(
            sampleEntry(id = "f1", word = "VERBETE_1"),
            sampleEntry(id = "f2", word = "VERBETE_2")
        )

        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        selectedCategory = SemanticCategory.NATURE,
                        totalLocalEntriesCount = 15,
                        contentState = DictionaryEntriesState.Success(filteredEntries)
                    ),
                    onQueryChange = {},
                    onSelectCategory = {},
                    onSelectGrammaticalClass = {},
                    onClearFilters = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dictionary_total_entries_count")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("15 verbetes locais").assertIsDisplayed()
        scrollListToTag("dictionary_entries_count")
        composeTestRule.onNodeWithTag("dictionary_entries_count").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 encontrados").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w412dp-h915dp")
    fun `aviso MODO DEMONSTRATIVO aparece na DictionaryScreen e esclarece ausencia de vocabulario autentico`() {
        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        totalLocalEntriesCount = 20,
                        contentState = DictionaryEntriesState.Success(
                            listOf(sampleEntry(id = "DEMO-001", word = "DEMO-001"))
                        )
                    ),
                    onQueryChange = {},
                    onSelectCategory = {},
                    onSelectGrammaticalClass = {},
                    onClearFilters = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dictionary_header_card").assertIsDisplayed()
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
    fun `busca e filtros continuam funcionando com o aviso de modo demonstrativo visivel`() {
        val recordedQueries = mutableListOf<String>()
        val recordedCategories = mutableListOf<SemanticCategory?>()
        val recordedClasses = mutableListOf<GrammaticalClass?>()
        var currentQuery by mutableStateOf("")

        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        query = currentQuery,
                        totalLocalEntriesCount = 20,
                        contentState = DictionaryEntriesState.Success(
                            listOf(sampleEntry(id = "DEMO-001", word = "DEMO-001"))
                        )
                    ),
                    onQueryChange = { newValue ->
                        recordedQueries += newValue
                        currentQuery = newValue
                    },
                    onSelectCategory = { recordedCategories += it },
                    onSelectGrammaticalClass = { recordedClasses += it },
                    onClearFilters = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dictionary_total_entries_count").assertIsDisplayed()

        composeTestRule.onNodeWithTag("dictionary_search_input")
            .assertIsDisplayed()
            .performTextInput("DEMO-001")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dictionary_category_${SemanticCategory.NATURE.name}")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("dictionary_grammatical_class_${GrammaticalClass.NOUN.name}")
            .performScrollTo()
            .performClick()

        scrollListToTag("demo_mode_notice_card")
        composeTestRule.onNodeWithTag("demo_mode_notice_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("demo_mode_notice_title").assertIsDisplayed()

        assertEquals(listOf("DEMO-001"), recordedQueries)
        assertEquals(listOf(SemanticCategory.NATURE), recordedCategories)
        assertEquals(listOf(GrammaticalClass.NOUN), recordedClasses)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w412dp-h732dp")
    fun `aviso MODO DEMONSTRATIVO permanece fixado no topo ao rolar lista extensa de verbetes`() {
        val twentyEntries = (1..20).map { index ->
            val id = "DEMO-${index.toString().padStart(3, '0')}"
            sampleEntry(
                id = id,
                word = id,
                translation = "Elemento fictício de demonstração ${index.toString().padStart(2, '0')}"
            )
        }

        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        totalLocalEntriesCount = 20,
                        contentState = DictionaryEntriesState.Success(twentyEntries)
                    ),
                    onQueryChange = {},
                    onSelectCategory = {},
                    onSelectGrammaticalClass = {},
                    onClearFilters = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Estado inicial antes da rolagem
        composeTestRule.onNodeWithTag("dictionary_header_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("demo_mode_notice_card").assertIsDisplayed()

        // Rola até os últimos verbetes da lista
        scrollListToTag("dictionary_entry_card_DEMO-020")

        // Cabeçalho inicial saiu da área visível, mas o aviso fixo permanece visível no topo
        composeTestRule.onNodeWithTag("dictionary_header_card").assertIsNotDisplayed()
        composeTestRule.onNodeWithTag("dictionary_sticky_demo_notice_container").assertIsDisplayed()
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

        // O último cartão exibido não fica encoberto pelo aviso fixo
        composeTestRule.onNodeWithTag("dictionary_entry_card_DEMO-020").assertIsDisplayed()
        val rootBounds = composeTestRule.onNodeWithTag("dictionary_screen_root")
            .fetchSemanticsNode().boundsInRoot
        val stickyBounds = composeTestRule.onNodeWithTag("dictionary_sticky_demo_notice_container")
            .fetchSemanticsNode().boundsInRoot
        val lastCardBounds = composeTestRule.onNodeWithTag("dictionary_entry_card_DEMO-020")
            .fetchSemanticsNode().boundsInRoot

        assertEquals(rootBounds.top, stickyBounds.top, 2f)
        assertTrue(lastCardBounds.top >= stickyBounds.bottom - 1f)
    }

    @Test
    fun `aviso fixo permanece visivel durante rolagem em tela estreita e busca e filtros continuam operando`() {
        val recordedQueries = mutableListOf<String>()
        val recordedCategories = mutableListOf<SemanticCategory?>()
        val recordedClasses = mutableListOf<GrammaticalClass?>()
        var currentQuery by mutableStateOf("")
        val entries = (1..12).map { index ->
            val id = "DEMO-${index.toString().padStart(3, '0')}"
            sampleEntry(id = id, word = id)
        }

        composeTestRule.setContent {
            ParintintinTheme {
                Box(modifier = Modifier.width(280.dp)) {
                    DictionaryContent(
                        uiState = DictionaryUiState(
                            query = currentQuery,
                            totalLocalEntriesCount = 12,
                            contentState = DictionaryEntriesState.Success(entries)
                        ),
                        onQueryChange = { newValue ->
                            recordedQueries += newValue
                            currentQuery = newValue
                        },
                        onSelectCategory = { recordedCategories += it },
                        onSelectGrammaticalClass = { recordedClasses += it },
                        onClearFilters = {},
                        onRetry = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Rola para baixo até o 12º verbete em largura de 280dp
        scrollListToTag("dictionary_entry_card_DEMO-012")
        composeTestRule.onNodeWithTag("demo_mode_notice_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dictionary_entry_card_DEMO-012").assertIsDisplayed()

        val rootBounds = composeTestRule.onNodeWithTag("dictionary_screen_root")
            .fetchSemanticsNode().boundsInRoot
        val noticeBounds = composeTestRule.onNodeWithTag("demo_mode_notice_card")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(noticeBounds.right <= rootBounds.right)

        // Volta ao topo e valida que busca e filtros continuam funcionando normalmente
        scrollListToTag("dictionary_search_input")
        composeTestRule.onNodeWithTag("dictionary_search_input")
            .assertIsDisplayed()
            .performTextInput("DEMO")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dictionary_category_${SemanticCategory.NATURE.name}")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("dictionary_grammatical_class_${GrammaticalClass.NOUN.name}")
            .performScrollTo()
            .performClick()

        assertEquals(listOf("DEMO"), recordedQueries)
        assertEquals(listOf(SemanticCategory.NATURE), recordedCategories)
        assertEquals(listOf(GrammaticalClass.NOUN), recordedClasses)
    }

    @Test
    fun `campo de pesquisa possui label persistente mesmo apos digitacao`() {
        var currentQuery by mutableStateOf("")
        val recordedQueries = mutableListOf<String>()

        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        query = currentQuery,
                        totalLocalEntriesCount = 20,
                        contentState = DictionaryEntriesState.Success(emptyList())
                    ),
                    onQueryChange = { newValue ->
                        recordedQueries += newValue
                        currentQuery = newValue
                    },
                    onSelectCategory = {},
                    onSelectGrammaticalClass = {},
                    onClearFilters = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Pesquisar verbetes").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dictionary_search_input")
            .assert(hasText("Pesquisar verbetes"))
            .performTextInput("DEMO-001")
        composeTestRule.waitForIdle()

        assertEquals(listOf("DEMO-001"), recordedQueries)
        composeTestRule.onNodeWithText("Pesquisar verbetes").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dictionary_search_input")
            .assert(hasText("Pesquisar verbetes"))
            .assert(hasText("DEMO-001"))
    }

    @Test
    fun `chips de classificacao e filtros fornecem contexto semantico sem anuncios duplicados`() {
        val entry = sampleEntry(
            id = "sem_1",
            word = "DEMO-001",
            grammaticalClass = GrammaticalClass.NOUN,
            semanticCategory = SemanticCategory.NATURE
        )

        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        totalLocalEntriesCount = 1,
                        contentState = DictionaryEntriesState.Success(listOf(entry))
                    ),
                    onQueryChange = {},
                    onSelectCategory = {},
                    onSelectGrammaticalClass = {},
                    onClearFilters = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // FilterChips possuem contentDescription contextualizada
        composeTestRule.onNodeWithTag("dictionary_category_all")
            .assertContentDescriptionEquals("Categoria semântica: Todas")
        composeTestRule.onNodeWithTag("dictionary_category_${SemanticCategory.NATURE.name}")
            .performScrollTo()
            .assertContentDescriptionEquals("Categoria semântica: Natureza e Território")
        composeTestRule.onNodeWithTag("dictionary_grammatical_class_all")
            .assertContentDescriptionEquals("Classe gramatical: Todas")
        composeTestRule.onNodeWithTag("dictionary_grammatical_class_${GrammaticalClass.NOUN.name}")
            .performScrollTo()
            .assertContentDescriptionEquals("Classe gramatical: Substantivo")

        // Chips do DictionaryEntryCard preservam o texto visual e expõem descrição contextualizada
        scrollListToTag("dictionary_entry_card_sem_1")
        composeTestRule.onNode(
            hasText("Substantivo") and
                hasContentDescription("Classe gramatical: Substantivo") and
                hasAnyAncestor(hasTestTag("dictionary_entry_card_sem_1"))
        ).assertIsDisplayed()
        composeTestRule.onNode(
            hasText("Natureza e Território") and
                hasContentDescription("Categoria semântica: Natureza e Território") and
                hasAnyAncestor(hasTestTag("dictionary_entry_card_sem_1"))
        ).assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w412dp-h915dp")
    fun `titulos estruturais do DictionaryScreen possuem semantica de heading e textos comuns nao`() {
        composeTestRule.setContent {
            ParintintinTheme {
                DictionaryContent(
                    uiState = DictionaryUiState(
                        query = "INEXISTENTE",
                        totalLocalEntriesCount = 20,
                        contentState = DictionaryEntriesState.Empty(hasActiveFilter = true)
                    ),
                    onQueryChange = {},
                    onSelectCategory = {},
                    onSelectGrammaticalClass = {},
                    onClearFilters = {},
                    onRetry = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Dicionário").assert(isHeading())
        composeTestRule.onNodeWithText("Categorias semânticas").assert(isHeading())
        composeTestRule.onNodeWithText("Classes gramaticais").assert(isHeading())
        composeTestRule.onNodeWithText("MODO DEMONSTRATIVO").assert(isHeading())
        composeTestRule.onNodeWithText("Verbetes disponíveis").assert(isHeading())
        composeTestRule.onNodeWithText("Nenhum verbete encontrado").assert(isHeading())

        composeTestRule.onNodeWithTag("dictionary_total_entries_count").assert(isHeading().not())
        composeTestRule.onNodeWithText("Consulta offline via Room").assert(isHeading().not())
        composeTestRule.onNodeWithTag("demo_mode_notice_body").assert(isHeading().not())
    }

    private fun scrollListToTag(tag: String) {
        composeTestRule.onNodeWithTag("dictionary_list")
            .performScrollToNode(hasTestTag(tag))
        composeTestRule.waitForIdle()
    }

    private fun sampleEntry(
        id: String,
        word: String,
        phoneticIpa: String = "[$id]",
        translation: String = "Tradução ilustrativa $id",
        grammaticalClass: GrammaticalClass = GrammaticalClass.NOUN,
        semanticCategory: SemanticCategory = SemanticCategory.NATURE,
        exampleParintintin: String = "",
        examplePortuguese: String = "",
        culturalNote: String = ""
    ): DictionaryEntry = DictionaryEntry(
        id = id,
        parintintinWord = word,
        phoneticIpa = phoneticIpa,
        portugueseTranslation = translation,
        grammaticalClass = grammaticalClass,
        semanticCategory = semanticCategory,
        exampleParintintin = exampleParintintin,
        examplePortuguese = examplePortuguese,
        culturalNote = culturalNote,
        audioUrl = null,
        isFavorite = false,
        updatedAtEpochMillis = 1_700_000_000_000L
    )
}
