package com.example.data.sync

import com.example.core.common.AppResult
import com.example.domain.model.ArchiveStats
import com.example.domain.model.SyncState
import com.example.domain.repository.SyncStatusRepository
import com.example.domain.sync.ArchiveSyncOutcome
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveSyncCoordinatorImplTest {

    private val createdScopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        createdScopes.forEach { it.cancel() }
        createdScopes.clear()
    }

    @Test
    fun `1 - primeira solicitacao automatica executa a sincronizacao`() = runTest {
        val repository = FakeSyncStatusRepository()
        val coordinator = createCoordinator(repository)

        val outcome = coordinator.requestAutoSync()

        assertEquals(ArchiveSyncOutcome.Executed(AppResult.Success(Unit)), outcome)
        assertEquals(1, repository.syncCallCount)
    }

    @Test
    fun `2 - solicitacoes automaticas repetidas nao executam novamente`() = runTest {
        val gate = CompletableDeferred<AppResult<Unit>>()
        val started = CompletableDeferred<Unit>()
        val repository = FakeSyncStatusRepository(
            onSync = {
                started.complete(Unit)
                gate.await()
            }
        )
        val coordinator = createCoordinator(repository)

        val firstAutoDeferred = async { coordinator.requestAutoSync() }
        started.await()

        val duringInFlightOutcome = coordinator.requestAutoSync()
        assertEquals(ArchiveSyncOutcome.AlreadyTriggeredForLaunch, duringInFlightOutcome)
        assertEquals(1, repository.syncCallCount)

        gate.complete(AppResult.Success(Unit))
        assertEquals(
            ArchiveSyncOutcome.Executed(AppResult.Success(Unit)),
            firstAutoDeferred.await()
        )

        val afterCompletionOutcome = coordinator.requestAutoSync()
        assertEquals(ArchiveSyncOutcome.AlreadyTriggeredForLaunch, afterCompletionOutcome)
        assertEquals(1, repository.syncCallCount)
    }

    @Test
    fun `3 - solicitacao manual durante sincronizacao automatica nao inicia outra execucao`() = runTest {
        val gate = CompletableDeferred<AppResult<Unit>>()
        val started = CompletableDeferred<Unit>()
        val repository = FakeSyncStatusRepository(
            onSync = {
                started.complete(Unit)
                gate.await()
            }
        )
        val coordinator = createCoordinator(repository)

        val autoDeferred = async { coordinator.requestAutoSync() }
        started.await()

        val manualDeferred = async { coordinator.requestManualSync() }
        advanceUntilIdle()
        assertEquals(1, repository.syncCallCount)

        val expected = AppResult.Success(Unit)
        gate.complete(expected)

        assertEquals(ArchiveSyncOutcome.Executed(expected), autoDeferred.await())
        assertEquals(ArchiveSyncOutcome.JoinedInFlight(expected), manualDeferred.await())
        assertEquals(1, repository.syncCallCount)
    }

    @Test
    fun `4 - solicitacao automatica durante sincronizacao manual nao inicia outra execucao`() = runTest {
        val gate = CompletableDeferred<AppResult<Unit>>()
        val started = CompletableDeferred<Unit>()
        val repository = FakeSyncStatusRepository(
            onSync = {
                started.complete(Unit)
                gate.await()
            }
        )
        val coordinator = createCoordinator(repository)

        val manualDeferred = async { coordinator.requestManualSync() }
        started.await()

        val autoDeferred = async { coordinator.requestAutoSync() }
        advanceUntilIdle()
        assertEquals(1, repository.syncCallCount)

        val expected = AppResult.Success(Unit)
        gate.complete(expected)

        assertEquals(ArchiveSyncOutcome.Executed(expected), manualDeferred.await())
        assertEquals(ArchiveSyncOutcome.JoinedInFlight(expected), autoDeferred.await())
        assertEquals(1, repository.syncCallCount)

        // Abertura já consumida durante a sincronização manual em andamento.
        assertEquals(ArchiveSyncOutcome.AlreadyTriggeredForLaunch, coordinator.requestAutoSync())
        assertEquals(1, repository.syncCallCount)
    }

    @Test
    fun `5 - apos conclusao uma nova solicitacao manual pode executar`() = runTest {
        val repository = FakeSyncStatusRepository()
        val coordinator = createCoordinator(repository)

        val first = coordinator.requestAutoSync()
        val second = coordinator.requestManualSync()
        val third = coordinator.requestManualSync()

        assertEquals(ArchiveSyncOutcome.Executed(AppResult.Success(Unit)), first)
        assertEquals(ArchiveSyncOutcome.Executed(AppResult.Success(Unit)), second)
        assertEquals(ArchiveSyncOutcome.Executed(AppResult.Success(Unit)), third)
        assertEquals(3, repository.syncCallCount)
    }

    @Test
    fun `6a - erro retornado pelo repositorio nao deixa o coordenador bloqueado`() = runTest {
        var attempt = 0
        val repository = FakeSyncStatusRepository(
            onSync = {
                attempt += 1
                if (attempt == 1) {
                    AppResult.Error("Sem conexão com a internet")
                } else {
                    AppResult.Success(Unit)
                }
            }
        )
        val coordinator = createCoordinator(repository)

        val failedOutcome = coordinator.requestAutoSync()
        assertTrue(failedOutcome is ArchiveSyncOutcome.Executed)
        assertTrue((failedOutcome as ArchiveSyncOutcome.Executed).result is AppResult.Error)
        assertEquals(1, repository.syncCallCount)

        val retryOutcome = coordinator.requestManualSync()
        assertEquals(ArchiveSyncOutcome.Executed(AppResult.Success(Unit)), retryOutcome)
        assertEquals(2, repository.syncCallCount)
    }

    @Test
    fun `6b - excecao inesperada durante sincronizacao nao bloqueia o coordenador nem quebra ApplicationScope`() = runTest {
        var attempt = 0
        val repository = FakeSyncStatusRepository(
            onSync = {
                attempt += 1
                if (attempt == 1) {
                    throw IllegalStateException("Falha inesperada na leitura")
                } else {
                    AppResult.Success(Unit)
                }
            }
        )
        val coordinator = createCoordinator(repository)

        val failedOutcome = coordinator.requestManualSync()
        assertTrue(failedOutcome is ArchiveSyncOutcome.Executed)
        val error = (failedOutcome as ArchiveSyncOutcome.Executed).result as AppResult.Error
        assertEquals("Falha inesperada na leitura", error.message)
        assertEquals(1, repository.syncCallCount)

        val nextOutcome = coordinator.requestManualSync()
        assertEquals(ArchiveSyncOutcome.Executed(AppResult.Success(Unit)), nextOutcome)
        assertEquals(2, repository.syncCallCount)
    }

    @Test
    fun `7a - cancelamento da execucao libera o coordenador para novas solicitacoes`() = runTest {
        var attempt = 0
        val repository = FakeSyncStatusRepository(
            onSync = {
                attempt += 1
                if (attempt == 1) {
                    throw CancellationException("Sincronização cancelada")
                } else {
                    AppResult.Success(Unit)
                }
            }
        )
        val coordinator = createCoordinator(repository)

        try {
            coordinator.requestManualSync()
            fail("Era esperado relançar CancellationException")
        } catch (_: CancellationException) {
            // Esperado
        }

        val afterCancelOutcome = coordinator.requestManualSync()
        assertEquals(ArchiveSyncOutcome.Executed(AppResult.Success(Unit)), afterCancelOutcome)
        assertEquals(2, repository.syncCallCount)
    }

    @Test
    fun `7b - cancelamento do chamador preserva execucao em ApplicationScope e nao bloqueia novas chamadas`() = runTest {
        val gate = CompletableDeferred<AppResult<Unit>>()
        val started = CompletableDeferred<Unit>()
        val completedInRepo = CompletableDeferred<Unit>()
        var attempt = 0
        val repository = FakeSyncStatusRepository(
            onSync = {
                attempt += 1
                if (attempt == 1) {
                    started.complete(Unit)
                    try {
                        gate.await()
                    } finally {
                        completedInRepo.complete(Unit)
                    }
                } else {
                    AppResult.Success(Unit)
                }
            }
        )
        val coordinator = createCoordinator(repository)

        val callerJob = launch {
            coordinator.requestAutoSync()
        }
        started.await()

        // Simula destruição de Activity/ViewModel do chamador durante a sincronização.
        callerJob.cancel()
        advanceUntilIdle()
        assertEquals(1, repository.syncCallCount)

        // Execução em ApplicationScope segue viva até concluir.
        gate.complete(AppResult.Success(Unit))
        completedInRepo.await()
        advanceUntilIdle()

        val nextManual = coordinator.requestManualSync()
        assertEquals(ArchiveSyncOutcome.Executed(AppResult.Success(Unit)), nextManual)
        assertEquals(2, repository.syncCallCount)
    }

    @Test
    fun `8 - chamadas concorrentes em multiplas threads nao provocam execucoes duplicadas`() {
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also {
            createdScopes += it
        }
        val callerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also {
            createdScopes += it
        }
        val startedLatch = CountDownLatch(1)
        val gate = CompletableDeferred<AppResult<Unit>>()
        val repository = FakeSyncStatusRepository(
            onSync = {
                startedLatch.countDown()
                gate.await()
            }
        )
        val coordinator = ArchiveSyncCoordinatorImpl(
            syncStatusRepository = repository,
            applicationScope = appScope
        )

        val threadCount = 8
        val executor = Executors.newFixedThreadPool(threadCount)
        val startBarrier = CyclicBarrier(threadCount)
        val dispatchedLatch = CountDownLatch(threadCount)
        val deferredOutcomes = Collections.synchronizedList(
            mutableListOf<kotlinx.coroutines.Deferred<ArchiveSyncOutcome>>()
        )

        try {
            repeat(threadCount) { index ->
                executor.execute {
                    try {
                        startBarrier.await(5, TimeUnit.SECONDS)
                        val deferred = callerScope.async(
                            start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED
                        ) {
                            if (index % 2 == 0) {
                                coordinator.requestAutoSync()
                            } else {
                                coordinator.requestManualSync()
                            }
                        }
                        deferredOutcomes += deferred
                    } finally {
                        dispatchedLatch.countDown()
                    }
                }
            }

            assertTrue(dispatchedLatch.await(5, TimeUnit.SECONDS))
            assertTrue(startedLatch.await(5, TimeUnit.SECONDS))
            assertEquals(1, repository.syncCallCount)

            gate.complete(AppResult.Success(Unit))
            val outcomes = runBlocking { deferredOutcomes.map { it.await() } }

            assertEquals(1, repository.syncCallCount)
            assertEquals(threadCount, outcomes.size)
            assertEquals(1, outcomes.count { it is ArchiveSyncOutcome.Executed })
            assertTrue(
                outcomes.all {
                    it is ArchiveSyncOutcome.Executed ||
                        it is ArchiveSyncOutcome.JoinedInFlight ||
                        it is ArchiveSyncOutcome.AlreadyTriggeredForLaunch
                }
            )

            // Após concluir o lote concorrente, uma nova chamada manual pode executar normalmente.
            val nextOutcome = runBlocking { coordinator.requestManualSync() }
            assertEquals(ArchiveSyncOutcome.Executed(AppResult.Success(Unit)), nextOutcome)
            assertEquals(2, repository.syncCallCount)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun TestScope.createCoordinator(
        repository: SyncStatusRepository
    ): ArchiveSyncCoordinatorImpl {
        val appScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)).also {
            createdScopes += it
        }
        return ArchiveSyncCoordinatorImpl(
            syncStatusRepository = repository,
            applicationScope = appScope
        )
    }

    private class FakeSyncStatusRepository(
        private val onSync: suspend () -> AppResult<Unit> = { AppResult.Success(Unit) }
    ) : SyncStatusRepository {
        private val calls = AtomicInteger(0)
        val syncCallCount: Int
            get() = calls.get()

        override val syncState: StateFlow<SyncState> = MutableStateFlow(SyncState.Idle)

        override fun observeArchiveStats(): Flow<ArchiveStats> = MutableStateFlow(ArchiveStats())

        override suspend fun syncArchive(): AppResult<Unit> {
            calls.incrementAndGet()
            return onSync()
        }
    }
}
