package com.example.presentation.home

import androidx.lifecycle.ViewModelStore
import com.example.core.common.AppResult
import com.example.data.sync.ArchiveSyncCoordinatorImpl
import com.example.domain.model.ArchiveStats
import com.example.domain.model.SyncFailureType
import com.example.domain.model.SyncState
import com.example.domain.repository.SyncStatusRepository
import com.example.domain.sync.ArchiveSyncCoordinator
import com.example.domain.sync.ArchiveSyncOutcome
import com.example.domain.usecase.ObserveArchiveStatsUseCase
import com.example.domain.usecase.ObserveSyncStateUseCase
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

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
    fun `estado inicial do HomeUiState possui valores padrao esperados`() = runTest(testDispatcher) {
        val fakeRepository = FakeSyncStatusRepository(initialSyncState = SyncState.Loading)
        val viewModel = createViewModel(fakeRepository)

        assertEquals(
            HomeUiState(
                archiveStats = ArchiveStats(),
                syncState = SyncState.Loading,
                isStageDetailsExpanded = false
            ),
            viewModel.uiState.value
        )
    }

    @Test
    fun `criacao do HomeViewModel solicita sincronizacao automatica uma vez mesmo em Loading sem acionar manual`() = runTest(testDispatcher) {
        val fakeRepository = FakeSyncStatusRepository(initialSyncState = SyncState.Loading)
        val fakeCoordinator = FakeArchiveSyncCoordinator()

        val viewModel = createViewModel(fakeRepository, fakeCoordinator)
        advanceUntilIdle()

        assertEquals(SyncState.Loading, viewModel.uiState.value.syncState)
        assertEquals(1, fakeCoordinator.autoSyncCallCount)
        assertEquals(0, fakeCoordinator.manualSyncCallCount)
    }

    @Test
    fun `coleta de uiState toggleStageDetails e syncManually nao provocam novas solicitacoes automaticas`() = runTest(testDispatcher) {
        val fakeRepository = FakeSyncStatusRepository(initialSyncState = SyncState.Loading)
        val fakeCoordinator = FakeArchiveSyncCoordinator()
        val viewModel = createViewModel(fakeRepository, fakeCoordinator)
        advanceUntilIdle()
        assertEquals(1, fakeCoordinator.autoSyncCallCount)

        val firstCollector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        viewModel.toggleStageDetails()
        viewModel.toggleStageDetails()
        advanceUntilIdle()
        firstCollector.cancel()

        // Simula nova coleta após recomposição/retorno à tela.
        val secondCollector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        viewModel.syncManually()
        advanceUntilIdle()
        secondCollector.cancel()

        assertEquals(1, fakeCoordinator.autoSyncCallCount)
        assertEquals(1, fakeCoordinator.manualSyncCallCount)
    }

    @Test
    fun `cancelamento do viewModelScope nao cancela sincronizacao automatica em andamento no ApplicationScope`() = runTest(testDispatcher) {
        val syncStarted = CompletableDeferred<Unit>()
        val syncGate = CompletableDeferred<AppResult<Unit>>()
        val syncCompletedNormally = CompletableDeferred<Boolean>()
        val fakeRepository = FakeSyncStatusRepository(
            initialSyncState = SyncState.Loading,
            onSyncArchive = {
                syncStarted.complete(Unit)
                try {
                    val result = syncGate.await()
                    syncCompletedNormally.complete(true)
                    result
                } catch (ce: CancellationException) {
                    syncCompletedNormally.complete(false)
                    throw ce
                }
            }
        )
        val applicationScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        try {
            val realCoordinator = ArchiveSyncCoordinatorImpl(
                syncStatusRepository = fakeRepository,
                applicationScope = applicationScope
            )
            val viewModel = createViewModel(fakeRepository, realCoordinator)

            syncStarted.await()
            assertEquals(1, fakeRepository.syncArchiveCallCount)

            // Destrói o ViewModel (cancela viewModelScope) enquanto a sync ainda está suspensa.
            ViewModelStore().apply {
                put("home", viewModel)
                clear()
            }
            advanceUntilIdle()

            // A sincronização no ApplicationScope deve continuar ativa e concluir normalmente.
            assertFalse(syncCompletedNormally.isCompleted)
            syncGate.complete(AppResult.Success(Unit))
            advanceUntilIdle()

            assertTrue(syncCompletedNormally.await())
            assertEquals(1, fakeRepository.syncArchiveCallCount)
        } finally {
            applicationScope.cancel()
        }
    }

    @Test
    fun `atualizacao das estatisticas do acervo reflete no HomeUiState`() = runTest(testDispatcher) {
        val fakeRepository = FakeSyncStatusRepository()
        val viewModel = createViewModel(fakeRepository)
        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val updatedStats = ArchiveStats(
            totalEntries = 48,
            favoriteEntries = 9,
            totalArticles = 14,
            bookmarkedArticles = 4
        )
        fakeRepository.statsFlow.value = updatedStats
        advanceUntilIdle()

        assertEquals(updatedStats, viewModel.uiState.value.archiveStats)
        collectorJob.cancel()
    }

    @Test
    fun `atualizacao do SyncState propaga Loading Idle Syncing Synced e OfflineFallback no HomeUiState`() = runTest(testDispatcher) {
        val fakeRepository = FakeSyncStatusRepository(initialSyncState = SyncState.Loading)
        val viewModel = createViewModel(fakeRepository)
        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()
        assertEquals(SyncState.Loading, viewModel.uiState.value.syncState)

        fakeRepository.syncStateFlow.value = SyncState.Idle
        advanceUntilIdle()
        assertEquals(SyncState.Idle, viewModel.uiState.value.syncState)

        fakeRepository.syncStateFlow.value = SyncState.Syncing
        advanceUntilIdle()
        assertEquals(SyncState.Syncing, viewModel.uiState.value.syncState)

        val synced = SyncState.Synced(
            lastSyncedEpochMillis = 1_700_000_050_000L,
            territoriesCount = 627
        )
        fakeRepository.syncStateFlow.value = synced
        advanceUntilIdle()
        assertEquals(synced, viewModel.uiState.value.syncState)

        val remoteEmpty = SyncState.RemoteEmpty(
            lastAttemptEpochMillis = 1_700_000_075_000L,
            lastSyncedEpochMillis = 1_700_000_050_000L,
            territoriesCount = 627
        )
        fakeRepository.syncStateFlow.value = remoteEmpty
        advanceUntilIdle()
        assertEquals(remoteEmpty, viewModel.uiState.value.syncState)

        val fallback = SyncState.OfflineFallback(
            reason = "Sem conexão",
            lastAttemptEpochMillis = 1_700_000_090_000L,
            failureType = SyncFailureType.SERVER_UNAVAILABLE,
            lastSuccessfulSyncEpochMillis = 1_700_000_050_000L
        )
        fakeRepository.syncStateFlow.value = fallback
        advanceUntilIdle()
        assertEquals(fallback, viewModel.uiState.value.syncState)
        val propagatedFallback = viewModel.uiState.value.syncState as SyncState.OfflineFallback
        assertEquals(SyncFailureType.SERVER_UNAVAILABLE, propagatedFallback.failureType)
        assertEquals(1_700_000_050_000L, propagatedFallback.lastSuccessfulSyncEpochMillis)

        collectorJob.cancel()
    }

    @Test
    fun `toggleStageDetails alterna estado expandido no HomeUiState`() = runTest(testDispatcher) {
        val fakeRepository = FakeSyncStatusRepository()
        val viewModel = createViewModel(fakeRepository)
        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isStageDetailsExpanded)

        viewModel.toggleStageDetails()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isStageDetailsExpanded)

        viewModel.toggleStageDetails()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isStageDetailsExpanded)

        collectorJob.cancel()
    }

    @Test
    fun `syncManually executa o coordenador de sincronizacao e atualiza o estado`() = runTest(testDispatcher) {
        val fakeRepository = FakeSyncStatusRepository()
        val fakeCoordinator = FakeArchiveSyncCoordinator(
            onManualSync = {
                val synced = SyncState.Synced(
                    lastSyncedEpochMillis = 1_700_000_100_000L,
                    territoriesCount = 12
                )
                fakeRepository.syncStateFlow.value = synced
                ArchiveSyncOutcome.Executed(AppResult.Success(Unit))
            }
        )
        val viewModel = createViewModel(fakeRepository, fakeCoordinator)
        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.syncManually()
        advanceUntilIdle()

        assertEquals(1, fakeCoordinator.manualSyncCallCount)
        assertEquals(1, fakeCoordinator.autoSyncCallCount)
        assertTrue(viewModel.uiState.value.syncState is SyncState.Synced)
        collectorJob.cancel()
    }

    @Test
    fun `syncManually bloqueia chamadas repetidas enquanto sincronizacao anterior esta ativa`() = runTest(testDispatcher) {
        val syncGate = CompletableDeferred<ArchiveSyncOutcome>()
        val fakeRepository = FakeSyncStatusRepository()
        val fakeCoordinator = FakeArchiveSyncCoordinator(
            onManualSync = {
                fakeRepository.syncStateFlow.value = SyncState.Syncing
                syncGate.await()
            }
        )
        val viewModel = createViewModel(fakeRepository, fakeCoordinator)
        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // Cliques consecutivos antes mesmo de a coroutine iniciar.
        viewModel.syncManually()
        viewModel.syncManually()
        viewModel.syncManually()
        advanceUntilIdle()

        assertEquals(1, fakeCoordinator.manualSyncCallCount)
        assertEquals(SyncState.Syncing, viewModel.uiState.value.syncState)

        // Novo clique enquanto aguarda conclusão do gate.
        viewModel.syncManually()
        advanceUntilIdle()
        assertEquals(1, fakeCoordinator.manualSyncCallCount)

        fakeRepository.syncStateFlow.value = SyncState.Synced(
            lastSyncedEpochMillis = 1_700_000_200_000L,
            territoriesCount = 10
        )
        syncGate.complete(ArchiveSyncOutcome.Executed(AppResult.Success(Unit)))
        advanceUntilIdle()

        assertEquals(1, fakeCoordinator.manualSyncCallCount)
        collectorJob.cancel()
    }

    @Test
    fun `syncManually e permitida enquanto estado observado e Loading e bloqueia duplicadas com Job ativo`() = runTest(testDispatcher) {
        val syncGate = CompletableDeferred<ArchiveSyncOutcome>()
        val fakeRepository = FakeSyncStatusRepository(initialSyncState = SyncState.Loading)
        val fakeCoordinator = FakeArchiveSyncCoordinator(
            onManualSync = { syncGate.await() }
        )
        val viewModel = createViewModel(fakeRepository, fakeCoordinator)
        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()
        assertEquals(SyncState.Loading, viewModel.uiState.value.syncState)

        viewModel.syncManually()
        viewModel.syncManually()
        advanceUntilIdle()
        assertEquals(1, fakeCoordinator.manualSyncCallCount)

        fakeRepository.syncStateFlow.value = SyncState.Synced(
            lastSyncedEpochMillis = 1_700_000_250_000L,
            territoriesCount = 7
        )
        syncGate.complete(ArchiveSyncOutcome.Executed(AppResult.Success(Unit)))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.syncState is SyncState.Synced)
        collectorJob.cancel()
    }

    @Test
    fun `syncManually bloqueia apenas quando estado e Syncing ou ha Job ativo`() = runTest(testDispatcher) {
        val fakeRepository = FakeSyncStatusRepository(
            initialSyncState = SyncState.Syncing
        )
        val fakeCoordinator = FakeArchiveSyncCoordinator()
        val viewModel = createViewModel(fakeRepository, fakeCoordinator)

        viewModel.syncManually()
        advanceUntilIdle()
        assertEquals(0, fakeCoordinator.manualSyncCallCount)

        fakeRepository.syncStateFlow.value = SyncState.Loading
        viewModel.syncManually()
        advanceUntilIdle()
        assertEquals(1, fakeCoordinator.manualSyncCallCount)

        fakeRepository.syncStateFlow.value = SyncState.Idle
        viewModel.syncManually()
        advanceUntilIdle()
        assertEquals(2, fakeCoordinator.manualSyncCallCount)

        fakeRepository.syncStateFlow.value = SyncState.RemoteEmpty(
            lastAttemptEpochMillis = 1_700_000_270_000L
        )
        viewModel.syncManually()
        advanceUntilIdle()
        assertEquals(3, fakeCoordinator.manualSyncCallCount)

        fakeRepository.syncStateFlow.value = SyncState.OfflineFallback(
            reason = "Offline",
            lastAttemptEpochMillis = 1_700_000_280_000L
        )
        viewModel.syncManually()
        advanceUntilIdle()
        assertEquals(4, fakeCoordinator.manualSyncCallCount)
    }

    @Test
    fun `syncManually libera controle apos conclusao permitindo nova sincronizacao`() = runTest(testDispatcher) {
        var callNumber = 0
        val fakeRepository = FakeSyncStatusRepository()
        val fakeCoordinator = FakeArchiveSyncCoordinator(
            onManualSync = {
                callNumber += 1
                if (callNumber == 1) {
                    fakeRepository.syncStateFlow.value = SyncState.OfflineFallback(
                        reason = "Falha temporária",
                        lastAttemptEpochMillis = 1_700_000_300_000L
                    )
                    ArchiveSyncOutcome.Executed(AppResult.Error("Falha temporária"))
                } else {
                    fakeRepository.syncStateFlow.value = SyncState.Synced(
                        lastSyncedEpochMillis = 1_700_000_310_000L,
                        territoriesCount = 15
                    )
                    ArchiveSyncOutcome.Executed(AppResult.Success(Unit))
                }
            }
        )
        val viewModel = createViewModel(fakeRepository, fakeCoordinator)
        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.syncManually()
        advanceUntilIdle()
        assertEquals(1, fakeCoordinator.manualSyncCallCount)
        assertTrue(viewModel.uiState.value.syncState is SyncState.OfflineFallback)

        viewModel.syncManually()
        advanceUntilIdle()
        assertEquals(2, fakeCoordinator.manualSyncCallCount)
        assertTrue(viewModel.uiState.value.syncState is SyncState.Synced)

        collectorJob.cancel()
    }

    @Test
    fun `syncManually libera controle e preserva viewModelScope apos cancelamento`() = runTest(testDispatcher) {
        val firstAttemptGate = CompletableDeferred<ArchiveSyncOutcome>()
        var attempt = 0
        val fakeRepository = FakeSyncStatusRepository()
        val fakeCoordinator = FakeArchiveSyncCoordinator(
            onManualSync = {
                attempt += 1
                if (attempt == 1) {
                    fakeRepository.syncStateFlow.value = SyncState.Syncing
                    firstAttemptGate.await()
                } else {
                    val synced = SyncState.Synced(
                        lastSyncedEpochMillis = 1_700_000_400_000L,
                        territoriesCount = 18
                    )
                    fakeRepository.syncStateFlow.value = synced
                    ArchiveSyncOutcome.Executed(AppResult.Success(Unit))
                }
            }
        )
        val viewModel = createViewModel(fakeRepository, fakeCoordinator)
        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.syncManually()
        advanceUntilIdle()
        assertEquals(1, fakeCoordinator.manualSyncCallCount)

        // Simula cancelamento durante a sincronização e restauração do estado anterior.
        fakeRepository.syncStateFlow.value = SyncState.Idle
        firstAttemptGate.completeExceptionally(CancellationException("Sincronização cancelada"))
        advanceUntilIdle()

        assertEquals(SyncState.Idle, viewModel.uiState.value.syncState)

        // Controle deve estar liberado e viewModelScope ativo para nova chamada.
        viewModel.syncManually()
        advanceUntilIdle()

        assertEquals(2, fakeCoordinator.manualSyncCallCount)
        assertTrue(viewModel.uiState.value.syncState is SyncState.Synced)
        collectorJob.cancel()
    }

    @Test
    fun `chamadas concorrentes a syncManually em multiplas threads iniciam apenas uma sincronizacao`() = runTest(testDispatcher) {
        val syncGate = CompletableDeferred<ArchiveSyncOutcome>()
        val fakeRepository = FakeSyncStatusRepository()
        val fakeCoordinator = FakeArchiveSyncCoordinator(
            onManualSync = {
                fakeRepository.syncStateFlow.value = SyncState.Syncing
                syncGate.await()
            }
        )
        val viewModel = createViewModel(fakeRepository, fakeCoordinator)

        val threadCount = 8
        val executor = Executors.newFixedThreadPool(threadCount)
        val startBarrier = CyclicBarrier(threadCount)
        val finishedLatch = CountDownLatch(threadCount)

        try {
            repeat(threadCount) {
                executor.execute {
                    try {
                        startBarrier.await(5, TimeUnit.SECONDS)
                        viewModel.syncManually()
                    } finally {
                        finishedLatch.countDown()
                    }
                }
            }

            assertTrue(finishedLatch.await(5, TimeUnit.SECONDS))
            advanceUntilIdle()
            assertEquals(1, fakeCoordinator.manualSyncCallCount)

            fakeRepository.syncStateFlow.value = SyncState.Synced(
                lastSyncedEpochMillis = 1_700_000_500_000L,
                territoriesCount = 5
            )
            syncGate.complete(ArchiveSyncOutcome.Executed(AppResult.Success(Unit)))
            advanceUntilIdle()

            assertEquals(1, fakeCoordinator.manualSyncCallCount)

            // Após a conclusão da única execução, uma nova chamada é permitida.
            viewModel.syncManually()
            advanceUntilIdle()
            assertEquals(2, fakeCoordinator.manualSyncCallCount)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun createViewModel(
        repository: SyncStatusRepository,
        coordinator: ArchiveSyncCoordinator = FakeArchiveSyncCoordinator()
    ): HomeViewModel = HomeViewModel(
        observeArchiveStats = ObserveArchiveStatsUseCase(repository),
        observeSyncState = ObserveSyncStateUseCase(repository),
        syncCoordinator = coordinator
    )

    private class FakeSyncStatusRepository(
        initialStats: ArchiveStats = ArchiveStats(),
        initialSyncState: SyncState = SyncState.Idle,
        private val onSyncArchive: (suspend () -> AppResult<Unit>)? = null
    ) : SyncStatusRepository {
        val statsFlow = MutableStateFlow(initialStats)
        val syncStateFlow = MutableStateFlow(initialSyncState)
        var syncArchiveCallCount: Int = 0
            private set

        override val syncState: StateFlow<SyncState> = syncStateFlow

        override fun observeArchiveStats(): Flow<ArchiveStats> = statsFlow

        override suspend fun syncArchive(): AppResult<Unit> {
            val handler = onSyncArchive
                ?: throw AssertionError("syncArchive() não deve ser chamado diretamente pelo HomeViewModel")
            syncArchiveCallCount += 1
            return handler()
        }
    }

    private class FakeArchiveSyncCoordinator(
        private val onAutoSync: suspend () -> ArchiveSyncOutcome = {
            ArchiveSyncOutcome.Executed(AppResult.Success(Unit))
        },
        private val onManualSync: suspend () -> ArchiveSyncOutcome = {
            ArchiveSyncOutcome.Executed(AppResult.Success(Unit))
        }
    ) : ArchiveSyncCoordinator {
        var autoSyncCallCount: Int = 0
            private set
        var manualSyncCallCount: Int = 0
            private set

        override suspend fun requestAutoSync(): ArchiveSyncOutcome {
            autoSyncCallCount += 1
            return onAutoSync()
        }

        override suspend fun requestManualSync(): ArchiveSyncOutcome {
            manualSyncCallCount += 1
            return onManualSync()
        }
    }
}
