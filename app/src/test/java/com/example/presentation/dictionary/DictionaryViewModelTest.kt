package com.example.presentation.dictionary

import com.example.core.common.AppResult
import com.example.domain.model.ArchiveStats
import com.example.domain.model.DictionaryEntry
import com.example.domain.model.GrammaticalClass
import com.example.domain.model.RemoteSyncOutcome
import com.example.domain.model.SemanticCategory
import com.example.domain.model.SyncState
import com.example.domain.repository.DictionaryRepository
import com.example.domain.repository.SyncStatusRepository
import com.example.domain.usecase.ObserveArchiveStatsUseCase
import com.example.domain.usecase.ObserveDictionaryEntriesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DictionaryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `1 - estado inicial do ViewModel possui filtros vazios e contentState Loading`() = runTest(testDispatcher) {
        val fakeRepository = FakeDictionaryRepository()
        val viewModel = createViewModel(fakeRepository)

        assertEquals(
            DictionaryUiState(
                query = "",
                selectedCategory = null,
                selectedGrammaticalClass = null,
                totalLocalEntriesCount = 0,
                contentState = DictionaryEntriesState.Loading
            ),
            viewModel.uiState.value
        )
        assertTrue(fakeRepository.recordedCalls.isEmpty())
    }

    @Test
    fun `2 - carregamento inicial emite Success com os verbetes do repositorio`() = runTest(testDispatcher) {
        val sample = listOf(
            sampleEntry(id = "entry_1", word = "TERMO_ALFA"),
            sampleEntry(id = "entry_2", word = "TERMO_BETA", grammaticalClass = GrammaticalClass.VERB)
        )
        val fakeRepository = FakeDictionaryRepository(initialEntries = sample)
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DictionaryEntriesState.Success(sample), state.contentState)
        assertEquals(1, fakeRepository.recordedCalls.size)
        assertEquals(
            ObserveCall(
                query = "",
                category = null,
                onlyFavorites = false,
                grammaticalClass = null
            ),
            fakeRepository.recordedCalls.single()
        )

        collectorJob.cancel()
    }

    @Test
    fun `3 - estado Empty sem filtros informa hasActiveFilter false`() = runTest(testDispatcher) {
        val fakeRepository = FakeDictionaryRepository(initialEntries = emptyList())
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()

        assertEquals(
            DictionaryEntriesState.Empty(hasActiveFilter = false),
            viewModel.uiState.value.contentState
        )

        collectorJob.cancel()
    }

    @Test
    fun `4 - estado Empty com filtros ativos informa hasActiveFilter true`() = runTest(testDispatcher) {
        val fakeRepository = FakeDictionaryRepository(
            initialEntries = listOf(sampleEntry(id = "entry_1", word = "TERMO_ALFA"))
        )
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.contentState is DictionaryEntriesState.Success)

        viewModel.onQueryChange("inexistente")
        advanceUntilIdle()
        assertEquals(
            DictionaryEntriesState.Empty(hasActiveFilter = true),
            viewModel.uiState.value.contentState
        )

        viewModel.onQueryChange("")
        viewModel.onSelectCategory(SemanticCategory.ANIMALS)
        advanceUntilIdle()
        assertEquals(
            DictionaryEntriesState.Empty(hasActiveFilter = true),
            viewModel.uiState.value.contentState
        )

        viewModel.onSelectCategory(null)
        viewModel.onSelectGrammaticalClass(GrammaticalClass.TOPONYM)
        advanceUntilIdle()
        assertEquals(
            DictionaryEntriesState.Empty(hasActiveFilter = true),
            viewModel.uiState.value.contentState
        )

        collectorJob.cancel()
    }

    @Test
    fun `5 - onQueryChange atualiza query na UI imediatamente antes do debounce`() = runTest(testDispatcher) {
        val fakeRepository = FakeDictionaryRepository()
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()
        assertEquals(1, fakeRepository.recordedCalls.size)

        viewModel.onQueryChange("termo")
        advanceTimeBy(50L)
        runCurrent()

        assertEquals("termo", viewModel.uiState.value.query)
        assertEquals(1, fakeRepository.recordedCalls.size)

        advanceTimeBy(100L)
        advanceUntilIdle()
        assertEquals(2, fakeRepository.recordedCalls.size)
        assertEquals("termo", fakeRepository.recordedCalls.last().query)

        collectorJob.cancel()
    }

    @Test
    fun `6 - debounce de 150 ms evita consultas intermediarias durante digitacao rapida`() = runTest(testDispatcher) {
        val fakeRepository = FakeDictionaryRepository()
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()
        assertEquals(1, fakeRepository.recordedCalls.size)

        viewModel.onQueryChange("t")
        advanceTimeBy(80L)
        runCurrent()
        viewModel.onQueryChange("te")
        advanceTimeBy(80L)
        runCurrent()
        viewModel.onQueryChange("termo")

        advanceTimeBy(149L)
        runCurrent()
        assertEquals(1, fakeRepository.recordedCalls.size)
        assertEquals("termo", viewModel.uiState.value.query)

        advanceTimeBy(1L)
        advanceUntilIdle()
        assertEquals(2, fakeRepository.recordedCalls.size)
        assertEquals("termo", fakeRepository.recordedCalls.last().query)

        collectorJob.cancel()
    }

    @Test
    fun `7 - selecao e remocao de categoria semantica atualizam estado e consulta`() = runTest(testDispatcher) {
        val natureEntry = sampleEntry(id = "e1", word = "TERMO_NATUREZA", category = SemanticCategory.NATURE)
        val foodEntry = sampleEntry(id = "e2", word = "TERMO_COMIDA", category = SemanticCategory.FOOD)
        val fakeRepository = FakeDictionaryRepository(initialEntries = listOf(natureEntry, foodEntry))
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()
        assertEquals(DictionaryEntriesState.Success(listOf(natureEntry, foodEntry)), viewModel.uiState.value.contentState)

        viewModel.onSelectCategory(SemanticCategory.FOOD)
        advanceUntilIdle()

        assertEquals(SemanticCategory.FOOD, viewModel.uiState.value.selectedCategory)
        assertEquals(DictionaryEntriesState.Success(listOf(foodEntry)), viewModel.uiState.value.contentState)
        assertEquals(SemanticCategory.FOOD, fakeRepository.recordedCalls.last().category)

        viewModel.onSelectCategory(null)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedCategory)
        assertEquals(DictionaryEntriesState.Success(listOf(natureEntry, foodEntry)), viewModel.uiState.value.contentState)
        assertNull(fakeRepository.recordedCalls.last().category)

        collectorJob.cancel()
    }

    @Test
    fun `8 - selecao e remocao de classe gramatical atualizam estado e consulta`() = runTest(testDispatcher) {
        val nounEntry = sampleEntry(id = "e1", word = "TERMO_SUBST", grammaticalClass = GrammaticalClass.NOUN)
        val verbEntry = sampleEntry(id = "e2", word = "TERMO_VERBO", grammaticalClass = GrammaticalClass.VERB)
        val fakeRepository = FakeDictionaryRepository(initialEntries = listOf(nounEntry, verbEntry))
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()

        viewModel.onSelectGrammaticalClass(GrammaticalClass.VERB)
        advanceUntilIdle()

        assertEquals(GrammaticalClass.VERB, viewModel.uiState.value.selectedGrammaticalClass)
        assertEquals(DictionaryEntriesState.Success(listOf(verbEntry)), viewModel.uiState.value.contentState)
        assertEquals(GrammaticalClass.VERB, fakeRepository.recordedCalls.last().grammaticalClass)

        viewModel.onSelectGrammaticalClass(null)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedGrammaticalClass)
        assertEquals(DictionaryEntriesState.Success(listOf(nounEntry, verbEntry)), viewModel.uiState.value.contentState)
        assertNull(fakeRepository.recordedCalls.last().grammaticalClass)

        collectorJob.cancel()
    }

    @Test
    fun `9 - combinacao simultanea de query categoria e classe gramatical encaminha todos os criterios`() = runTest(testDispatcher) {
        val match = sampleEntry(
            id = "match",
            word = "TERMO_ESPECIFICO",
            translation = "Tradução específica",
            category = SemanticCategory.CULTURE_SPIRIT,
            grammaticalClass = GrammaticalClass.EXPRESSION
        )
        val wrongClass = sampleEntry(
            id = "wrong_class",
            word = "TERMO_ESPECIFICO_2",
            translation = "Tradução específica",
            category = SemanticCategory.CULTURE_SPIRIT,
            grammaticalClass = GrammaticalClass.NOUN
        )
        val wrongCategory = sampleEntry(
            id = "wrong_cat",
            word = "TERMO_ESPECIFICO_3",
            translation = "Tradução específica",
            category = SemanticCategory.EVERYDAY,
            grammaticalClass = GrammaticalClass.EXPRESSION
        )
        val fakeRepository = FakeDictionaryRepository(
            initialEntries = listOf(match, wrongClass, wrongCategory)
        )
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()

        viewModel.onQueryChange("específica")
        viewModel.onSelectCategory(SemanticCategory.CULTURE_SPIRIT)
        viewModel.onSelectGrammaticalClass(GrammaticalClass.EXPRESSION)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("específica", state.query)
        assertEquals(SemanticCategory.CULTURE_SPIRIT, state.selectedCategory)
        assertEquals(GrammaticalClass.EXPRESSION, state.selectedGrammaticalClass)
        assertEquals(DictionaryEntriesState.Success(listOf(match)), state.contentState)
        assertEquals(
            ObserveCall(
                query = "específica",
                category = SemanticCategory.CULTURE_SPIRIT,
                onlyFavorites = false,
                grammaticalClass = GrammaticalClass.EXPRESSION
            ),
            fakeRepository.recordedCalls.last()
        )

        collectorJob.cancel()
    }

    @Test
    fun `10 - onClearFilters limpa busca categoria e classe gramatical restaurando acervo completo`() = runTest(testDispatcher) {
        val first = sampleEntry(id = "e1", word = "TERMO_1", category = SemanticCategory.NATURE, grammaticalClass = GrammaticalClass.NOUN)
        val second = sampleEntry(id = "e2", word = "TERMO_2", category = SemanticCategory.ANIMALS, grammaticalClass = GrammaticalClass.VERB)
        val fakeRepository = FakeDictionaryRepository(initialEntries = listOf(first, second))
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()

        viewModel.onQueryChange("TERMO_1")
        viewModel.onSelectCategory(SemanticCategory.NATURE)
        viewModel.onSelectGrammaticalClass(GrammaticalClass.NOUN)
        advanceUntilIdle()
        assertEquals(DictionaryEntriesState.Success(listOf(first)), viewModel.uiState.value.contentState)

        viewModel.onClearFilters()
        runCurrent()
        assertEquals("", viewModel.uiState.value.query)
        assertNull(viewModel.uiState.value.selectedCategory)
        assertNull(viewModel.uiState.value.selectedGrammaticalClass)

        advanceUntilIdle()

        assertEquals(DictionaryEntriesState.Success(listOf(first, second)), viewModel.uiState.value.contentState)
        assertEquals(
            ObserveCall(
                query = "",
                category = null,
                onlyFavorites = false,
                grammaticalClass = null
            ),
            fakeRepository.recordedCalls.last()
        )

        collectorJob.cancel()
    }

    @Test
    fun `11 - falha do repositorio emite estado Error e nao converte CancellationException em Error`() = runTest(testDispatcher) {
        val fakeRepository = FakeDictionaryRepository(
            customObserve = { _, _, _, _ ->
                flow {
                    throw IllegalStateException("Falha ao consultar SQLite")
                }
            }
        )
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()

        assertEquals(
            DictionaryEntriesState.Error("Falha ao consultar SQLite"),
            viewModel.uiState.value.contentState
        )
        collectorJob.cancel()

        val cancellingRepository = FakeDictionaryRepository(
            customObserve = { _, _, _, _ ->
                flow {
                    throw CancellationException("Cancelamento estruturado")
                }
            }
        )
        val cancellingViewModel = createViewModel(cancellingRepository)
        val cancellingCollector = launch(UnconfinedTestDispatcher(testScheduler)) {
            cancellingViewModel.uiState.collect {}
        }

        advanceUntilIdle()

        assertFalse(cancellingViewModel.uiState.value.contentState is DictionaryEntriesState.Error)
        assertEquals(DictionaryEntriesState.Loading, cancellingViewModel.uiState.value.contentState)
        cancellingCollector.cancel()
    }

    @Test
    fun `12 - onRetry apos falha executa exatamente uma nova coleta e transita para Success`() = runTest(testDispatcher) {
        var shouldFail = true
        val expectedEntries = listOf(sampleEntry(id = "e1", word = "TERMO_RECUPERADO"))
        val fakeRepository = FakeDictionaryRepository(
            customObserve = { _, _, _, _ ->
                flow {
                    if (shouldFail) {
                        throw IllegalStateException("Erro transitório de leitura")
                    }
                    emit(expectedEntries)
                }
            }
        )
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()
        assertEquals(1, fakeRepository.recordedCalls.size)
        assertEquals(
            DictionaryEntriesState.Error("Erro transitório de leitura"),
            viewModel.uiState.value.contentState
        )

        shouldFail = false
        viewModel.onRetry()
        advanceUntilIdle()

        assertEquals(2, fakeRepository.recordedCalls.size)
        assertEquals(
            DictionaryEntriesState.Success(expectedEntries),
            viewModel.uiState.value.contentState
        )

        collectorJob.cancel()
    }

    @Test
    fun `13 - alteracao de filtros cancela coleta anterior via flatMapLatest antes da nova consulta`() = runTest(testDispatcher) {
        var startedFlows = 0
        var cancelledFlows = 0
        val events = mutableListOf<String>()

        val fakeRepository = FakeDictionaryRepository(
            customObserve = { _, category, _, grammaticalClass ->
                callbackFlow {
                    val flowIndex = ++startedFlows
                    events += "start:$flowIndex:${category?.name}:${grammaticalClass?.name}"
                    trySend(
                        listOf(
                            sampleEntry(
                                id = "entry_$flowIndex",
                                word = "TERMO_$flowIndex",
                                category = category ?: SemanticCategory.EVERYDAY,
                                grammaticalClass = grammaticalClass ?: GrammaticalClass.NOUN
                            )
                        )
                    )
                    awaitClose {
                        cancelledFlows += 1
                        events += "cancel:$flowIndex"
                    }
                }
            }
        )
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()
        assertEquals(1, startedFlows)
        assertEquals(0, cancelledFlows)

        viewModel.onSelectCategory(SemanticCategory.NATURE)
        advanceUntilIdle()
        assertEquals(2, startedFlows)
        assertEquals(1, cancelledFlows)

        viewModel.onSelectGrammaticalClass(GrammaticalClass.VERB)
        advanceUntilIdle()
        assertEquals(3, startedFlows)
        assertEquals(2, cancelledFlows)

        assertEquals(
            listOf(
                "start:1:null:null",
                "cancel:1",
                "start:2:NATURE:null",
                "cancel:2",
                "start:3:NATURE:VERB"
            ),
            events
        )

        collectorJob.cancel()
        advanceUntilIdle()
        assertEquals(3, cancelledFlows)
    }

    @Test
    fun `14 - banco vazio e banco populado expoem totalLocalEntriesCount correto`() = runTest(testDispatcher) {
        val emptyRepository = FakeDictionaryRepository(initialEntries = emptyList())
        val emptyViewModel = createViewModel(emptyRepository)
        val emptyCollector = launch(UnconfinedTestDispatcher(testScheduler)) {
            emptyViewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertEquals(0, emptyViewModel.uiState.value.totalLocalEntriesCount)
        assertEquals(
            DictionaryEntriesState.Empty(hasActiveFilter = false),
            emptyViewModel.uiState.value.contentState
        )
        emptyCollector.cancel()

        val entries = listOf(
            sampleEntry(id = "e1", word = "TERMO_1"),
            sampleEntry(id = "e2", word = "TERMO_2"),
            sampleEntry(id = "e3", word = "TERMO_3")
        )
        val populatedRepository = FakeDictionaryRepository(initialEntries = entries)
        val populatedViewModel = createViewModel(populatedRepository)
        val populatedCollector = launch(UnconfinedTestDispatcher(testScheduler)) {
            populatedViewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertEquals(3, populatedViewModel.uiState.value.totalLocalEntriesCount)
        assertEquals(
            DictionaryEntriesState.Success(entries),
            populatedViewModel.uiState.value.contentState
        )
        populatedCollector.cancel()
    }

    @Test
    fun `15 - pesquisa com e sem resultados preserva totalLocalEntriesCount real do banco`() = runTest(testDispatcher) {
        val first = sampleEntry(id = "e1", word = "TERMO_ALPHA")
        val second = sampleEntry(id = "e2", word = "TERMO_BETA")
        val third = sampleEntry(id = "e3", word = "TERMO_ALPHA_DOIS")
        val fakeRepository = FakeDictionaryRepository(initialEntries = listOf(first, second, third))
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.totalLocalEntriesCount)

        // Pesquisa com resultados (2 de 3)
        viewModel.onQueryChange("ALPHA")
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.totalLocalEntriesCount)
        assertEquals(
            DictionaryEntriesState.Success(listOf(first, third)),
            viewModel.uiState.value.contentState
        )

        // Pesquisa sem resultados (0 de 3)
        viewModel.onQueryChange("SEM_CORRESPONDENCIA")
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.totalLocalEntriesCount)
        assertEquals(
            DictionaryEntriesState.Empty(hasActiveFilter = true),
            viewModel.uiState.value.contentState
        )

        collectorJob.cancel()
    }

    @Test
    fun `16 - aplicacao de filtros semanticos e gramaticais mantem totalLocalEntriesCount separado`() = runTest(testDispatcher) {
        val entries = listOf(
            sampleEntry(
                id = "e1",
                word = "TERMO_1",
                category = SemanticCategory.NATURE,
                grammaticalClass = GrammaticalClass.NOUN
            ),
            sampleEntry(
                id = "e2",
                word = "TERMO_2",
                category = SemanticCategory.FOOD,
                grammaticalClass = GrammaticalClass.VERB
            ),
            sampleEntry(
                id = "e3",
                word = "TERMO_3",
                category = SemanticCategory.ANIMALS,
                grammaticalClass = GrammaticalClass.ADJECTIVE
            ),
            sampleEntry(
                id = "e4",
                word = "TERMO_4",
                category = SemanticCategory.EVERYDAY,
                grammaticalClass = GrammaticalClass.EXPRESSION
            )
        )
        val fakeRepository = FakeDictionaryRepository(initialEntries = entries)
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onSelectCategory(SemanticCategory.FOOD)
        advanceUntilIdle()
        assertEquals(4, viewModel.uiState.value.totalLocalEntriesCount)
        assertEquals(
            DictionaryEntriesState.Success(listOf(entries[1])),
            viewModel.uiState.value.contentState
        )

        viewModel.onSelectGrammaticalClass(GrammaticalClass.TOPONYM)
        advanceUntilIdle()
        assertEquals(4, viewModel.uiState.value.totalLocalEntriesCount)
        assertEquals(
            DictionaryEntriesState.Empty(hasActiveFilter = true),
            viewModel.uiState.value.contentState
        )

        collectorJob.cancel()
    }

    @Test
    fun `17 - insercao reativa de novos registros atualiza totalLocalEntriesCount automaticamente`() = runTest(testDispatcher) {
        val fakeRepository = FakeDictionaryRepository(initialEntries = emptyList())
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.totalLocalEntriesCount)
        assertEquals(
            DictionaryEntriesState.Empty(hasActiveFilter = false),
            viewModel.uiState.value.contentState
        )

        val initialEntries = listOf(
            sampleEntry(id = "e1", word = "TERMO_INICIAL_1"),
            sampleEntry(id = "e2", word = "TERMO_INICIAL_2")
        )
        fakeRepository.replaceEntries(initialEntries)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.totalLocalEntriesCount)
        assertEquals(
            DictionaryEntriesState.Success(initialEntries),
            viewModel.uiState.value.contentState
        )

        // Mesmo com filtro sem resultado ativo, inserção de novo registro atualiza o total real
        viewModel.onQueryChange("INEXISTENTE")
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.totalLocalEntriesCount)
        assertEquals(
            DictionaryEntriesState.Empty(hasActiveFilter = true),
            viewModel.uiState.value.contentState
        )

        val updatedEntries = initialEntries + sampleEntry(id = "e3", word = "TERMO_NOVO_3")
        fakeRepository.replaceEntries(updatedEntries)
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.totalLocalEntriesCount)
        assertEquals(
            DictionaryEntriesState.Empty(hasActiveFilter = true),
            viewModel.uiState.value.contentState
        )

        collectorJob.cancel()
    }

    private fun createViewModel(repository: DictionaryRepository): DictionaryViewModel =
        DictionaryViewModel(
            observeDictionaryEntries = ObserveDictionaryEntriesUseCase(repository),
            observeArchiveStats = ObserveArchiveStatsUseCase(FakeSyncStatusRepository(repository))
        )

    private fun sampleEntry(
        id: String,
        word: String,
        translation: String = "Tradução ilustrativa $id",
        category: SemanticCategory = SemanticCategory.NATURE,
        grammaticalClass: GrammaticalClass = GrammaticalClass.NOUN
    ): DictionaryEntry = DictionaryEntry(
        id = id,
        parintintinWord = word,
        phoneticIpa = "[$id]",
        portugueseTranslation = translation,
        grammaticalClass = grammaticalClass,
        semanticCategory = category,
        exampleParintintin = "",
        examplePortuguese = "",
        culturalNote = "",
        audioUrl = null,
        isFavorite = false,
        updatedAtEpochMillis = 1_700_000_000_000L
    )

    private data class ObserveCall(
        val query: String,
        val category: SemanticCategory?,
        val onlyFavorites: Boolean,
        val grammaticalClass: GrammaticalClass?
    )

    private class FakeDictionaryRepository(
        initialEntries: List<DictionaryEntry> = emptyList(),
        private val customObserve: ((String, SemanticCategory?, Boolean, GrammaticalClass?) -> Flow<List<DictionaryEntry>>)? = null
    ) : DictionaryRepository {

        private val entriesFlow = MutableStateFlow(initialEntries)
        val recordedCalls = mutableListOf<ObserveCall>()

        override fun observeEntries(
            query: String,
            category: SemanticCategory?,
            onlyFavorites: Boolean,
            grammaticalClass: GrammaticalClass?
        ): Flow<List<DictionaryEntry>> {
            recordedCalls += ObserveCall(
                query = query,
                category = category,
                onlyFavorites = onlyFavorites,
                grammaticalClass = grammaticalClass
            )
            customObserve?.let { return it(query, category, onlyFavorites, grammaticalClass) }

            val normalizedQuery = query.trim()
            return entriesFlow.map { entries ->
                entries.filter { entry ->
                    (!onlyFavorites || entry.isFavorite) &&
                        (category == null || entry.semanticCategory == category) &&
                        (grammaticalClass == null || entry.grammaticalClass == grammaticalClass) &&
                        (normalizedQuery.isEmpty() ||
                            entry.parintintinWord.contains(normalizedQuery, ignoreCase = true) ||
                            entry.portugueseTranslation.contains(normalizedQuery, ignoreCase = true) ||
                            entry.culturalNote.contains(normalizedQuery, ignoreCase = true))
                }
            }
        }

        fun replaceEntries(entries: List<DictionaryEntry>) {
            entriesFlow.value = entries
        }

        override fun observeEntryById(entryId: String): Flow<DictionaryEntry?> = flowOf(null)
        override fun observeTotalCount(): Flow<Int> = entriesFlow.map { it.size }
        override fun observeFavoriteCount(): Flow<Int> = flowOf(0)
        override suspend fun toggleFavorite(entryId: String) = Unit
    }

    private class FakeSyncStatusRepository(
        private val dictionaryRepository: DictionaryRepository
    ) : SyncStatusRepository {
        override val syncState: StateFlow<SyncState> = MutableStateFlow(SyncState.Idle)

        override fun observeArchiveStats(): Flow<ArchiveStats> =
            dictionaryRepository.observeTotalCount().map { totalEntries ->
                ArchiveStats(totalEntries = totalEntries)
            }

        override suspend fun syncArchive(): AppResult<Unit> = AppResult.Success(Unit)
    }
}
