package com.example.presentation.encyclopedia

import com.example.core.common.AppResult
import com.example.domain.model.CulturalArticle
import com.example.domain.model.CulturalTopic
import com.example.domain.repository.EncyclopediaRepository
import com.example.domain.usecase.ObserveCulturalArticlesUseCase
import com.example.domain.usecase.ToggleArticleBookmarkUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EncyclopediaViewModelTest {

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
    fun `1 - falha comum do repositorio emite estado Error com mensagem da excecao`() = runTest(testDispatcher) {
        val fakeRepository = FakeEncyclopediaRepository(
            customObserve = { _, _, _ ->
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
            EncyclopediaArticlesState.Error("Falha ao consultar SQLite"),
            viewModel.uiState.value.contentState
        )

        collectorJob.cancel()
    }

    @Test
    fun `2 - falha comum sem mensagem utiliza mensagem padrao de fallback`() = runTest(testDispatcher) {
        val fakeRepository = FakeEncyclopediaRepository(
            customObserve = { _, _, _ ->
                flow {
                    throw IllegalStateException()
                }
            }
        )
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()

        assertEquals(
            EncyclopediaArticlesState.Error("Erro ao consultar artigos no banco de dados local."),
            viewModel.uiState.value.contentState
        )

        collectorJob.cancel()
    }

    @Test
    fun `3 - CancellationException lancada na observacao nao e convertida em estado Error`() = runTest(testDispatcher) {
        val cancellingRepository = FakeEncyclopediaRepository(
            customObserve = { _, _, _ ->
                flow {
                    throw CancellationException("Cancelamento estruturado")
                }
            }
        )
        val viewModel = createViewModel(cancellingRepository)
        val emittedStates = mutableListOf<EncyclopediaArticlesState>()

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { state ->
                emittedStates += state.contentState
            }
        }

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.contentState is EncyclopediaArticlesState.Error)
        assertFalse(emittedStates.any { it is EncyclopediaArticlesState.Error })
        assertEquals(EncyclopediaArticlesState.Loading, viewModel.uiState.value.contentState)

        collectorJob.cancel()
    }

    @Test
    fun `4 - troca de filtro cancela fluxo anterior sem emitir Error e carrega novo topico`() = runTest(testDispatcher) {
        val historyArticle = sampleArticle(id = "art_1", title = "Artigo Histórico", topic = CulturalTopic.HISTORY)
        val fakeRepository = FakeEncyclopediaRepository(
            customObserve = { _, topic, _ ->
                flow {
                    if (topic == null) {
                        try {
                            awaitCancellation()
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        }
                    } else {
                        emit(listOf(historyArticle))
                    }
                }
            }
        )
        val viewModel = createViewModel(fakeRepository)
        val emittedStates = mutableListOf<EncyclopediaArticlesState>()

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { state ->
                emittedStates += state.contentState
            }
        }

        advanceUntilIdle()
        assertEquals(EncyclopediaArticlesState.Loading, viewModel.uiState.value.contentState)

        viewModel.onSelectTopic(CulturalTopic.HISTORY)
        advanceUntilIdle()

        assertFalse(emittedStates.any { it is EncyclopediaArticlesState.Error })
        assertEquals(
            EncyclopediaArticlesState.Success(listOf(historyArticle)),
            viewModel.uiState.value.contentState
        )

        collectorJob.cancel()
    }

    @Test
    fun `5 - onRetry apos falha comum realiza nova consulta e transita para Success`() = runTest(testDispatcher) {
        var shouldFail = true
        val expectedArticle = sampleArticle(id = "art_rec", title = "Artigo Recuperado")
        val fakeRepository = FakeEncyclopediaRepository(
            customObserve = { _, _, _ ->
                flow {
                    if (shouldFail) {
                        throw IllegalStateException("Erro transitório de leitura")
                    }
                    emit(listOf(expectedArticle))
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
            EncyclopediaArticlesState.Error("Erro transitório de leitura"),
            viewModel.uiState.value.contentState
        )

        shouldFail = false
        viewModel.onRetry()
        advanceUntilIdle()

        assertEquals(2, fakeRepository.recordedCalls.size)
        assertEquals(
            EncyclopediaArticlesState.Success(listOf(expectedArticle)),
            viewModel.uiState.value.contentState
        )

        collectorJob.cancel()
    }

    private fun createViewModel(repository: EncyclopediaRepository): EncyclopediaViewModel =
        EncyclopediaViewModel(
            observeCulturalArticles = ObserveCulturalArticlesUseCase(repository),
            toggleArticleBookmark = ToggleArticleBookmarkUseCase(repository)
        )

    private fun sampleArticle(
        id: String,
        title: String,
        topic: CulturalTopic = CulturalTopic.HISTORY
    ): CulturalArticle = CulturalArticle(
        id = id,
        title = title,
        subtitle = "Subtítulo $id",
        summary = "Resumo $id",
        bodyParagraphs = listOf("Parágrafo 1 de $id"),
        topic = topic,
        readingTimeMinutes = 3,
        sourceReference = "Referência $id",
        isBookmarked = false,
        updatedAtEpochMillis = 1_700_000_000_000L
    )

    private data class ObserveCall(
        val query: String,
        val topic: CulturalTopic?,
        val onlyBookmarked: Boolean
    )

    private class FakeEncyclopediaRepository(
        initialArticles: List<CulturalArticle> = emptyList(),
        private val customObserve: ((String, CulturalTopic?, Boolean) -> Flow<List<CulturalArticle>>)? = null
    ) : EncyclopediaRepository {

        private val articlesFlow = MutableStateFlow(initialArticles)
        val recordedCalls = mutableListOf<ObserveCall>()

        override fun observeArticles(
            query: String,
            topic: CulturalTopic?,
            onlyBookmarked: Boolean
        ): Flow<List<CulturalArticle>> {
            recordedCalls += ObserveCall(
                query = query,
                topic = topic,
                onlyBookmarked = onlyBookmarked
            )
            customObserve?.let { return it(query, topic, onlyBookmarked) }

            val normalizedQuery = query.trim()
            return articlesFlow.map { articles ->
                articles.filter { article ->
                    (!onlyBookmarked || article.isBookmarked) &&
                        (topic == null || article.topic == topic) &&
                        (normalizedQuery.isEmpty() ||
                            article.title.contains(normalizedQuery, ignoreCase = true) ||
                            article.summary.contains(normalizedQuery, ignoreCase = true))
                }
            }
        }

        override fun observeArticleById(articleId: String): Flow<CulturalArticle?> = flowOf(null)
        override fun observeTotalCount(): Flow<Int> = articlesFlow.map { it.size }
        override fun observeBookmarkedCount(): Flow<Int> = flowOf(0)
        override suspend fun toggleBookmark(articleId: String) = Unit
        override suspend fun syncFromRemote(): AppResult<Int> = AppResult.Success(0)
    }
}
