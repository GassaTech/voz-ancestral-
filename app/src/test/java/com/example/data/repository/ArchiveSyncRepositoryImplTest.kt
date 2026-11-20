package com.example.data.repository

import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.core.common.RemoteErrorKind
import com.example.data.local.dao.SyncMetadataDao
import com.example.data.local.entity.SyncMetadataEntity
import com.example.data.local.seed.DictionarySeedSpec
import com.example.data.local.seed.DictionarySeedUpdater
import com.example.data.local.seed.SeedUpdateOutcome
import com.example.data.local.seed.SeedUpdateOutcomeStatus
import com.example.domain.connectivity.NetworkConnectivityObserver
import com.example.domain.model.CulturalArticle
import com.example.domain.model.CulturalTopic
import com.example.domain.model.DeviceConnectivityState
import com.example.domain.model.DictionaryEntry
import com.example.domain.model.GrammaticalClass
import com.example.domain.model.RemoteSyncOutcome
import com.example.domain.model.SemanticCategory
import com.example.domain.model.SyncFailureType
import com.example.domain.model.SyncState
import com.example.domain.repository.DictionaryRepository
import com.example.domain.repository.EncyclopediaRepository
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveSyncRepositoryImplTest {

    private val createdScopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        createdScopes.forEach { it.cancel() }
        createdScopes.clear()
    }

    @Test
    fun `1 - cancelamento durante a sincronizacao restaura estado anterior e relanca CancellationException`() = runTest {
        val syncDao = FakeSyncMetadataDao()
        val encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(0))
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                throwOnSync = CancellationException("Cancelado durante sincronização")
            ),
            encyclopediaRepository = encyclopediaRepository,
            syncDao = syncDao
        )
        advanceUntilIdle()

        try {
            repository.syncArchive()
            fail("Era esperado relançar CancellationException")
        } catch (_: CancellationException) {
            advanceUntilIdle()
            assertEquals(SyncState.Idle, repository.syncState.value)
            assertEquals(0, encyclopediaRepository.syncCallCount)
        }
    }

    @Test
    fun `2 - cancelamento durante o registro de OfflineFallback restaura estado anterior e relanca CancellationException`() = runTest {
        val syncDao = FakeSyncMetadataDao(
            throwOnRecordFailure = CancellationException("Cancelado ao gravar fallback")
        )
        val encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(0))
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                throwOnSync = IllegalStateException("Erro inesperado de rede")
            ),
            encyclopediaRepository = encyclopediaRepository,
            syncDao = syncDao
        )
        advanceUntilIdle()

        try {
            repository.syncArchive()
            fail("Era esperado relançar CancellationException")
        } catch (_: CancellationException) {
            advanceUntilIdle()
            assertEquals(SyncState.Idle, repository.syncState.value)
            assertEquals(0, encyclopediaRepository.syncCallCount)
        }
    }

    @Test
    fun `3 - falha inesperada durante a sincronizacao registra OfflineFallback e preserva dados locais`() = runTest {
        val syncDao = FakeSyncMetadataDao()
        val encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(2))
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                throwOnSync = IllegalStateException("Falha inesperada ao ler resposta")
            ),
            encyclopediaRepository = encyclopediaRepository,
            syncDao = syncDao
        )
        advanceUntilIdle()

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Error)
        assertEquals(0, encyclopediaRepository.syncCallCount)
        val state = repository.syncState.value as SyncState.OfflineFallback
        assertEquals("Falha inesperada ao ler resposta", state.reason)
        assertEquals(SyncMetadataEntity.STATUS_OFFLINE_FALLBACK, syncDao.lastSaved?.lastAttemptStatus)
        assertEquals("Falha inesperada ao ler resposta", syncDao.lastSaved?.lastFailureReason)
        assertNull(syncDao.lastSaved?.lastSuccessfulSyncEpochMillis)
    }

    @Test
    fun `4 - sucesso da sincronizacao do dicionario atualiza estado para Synced sem executar enciclopedia e com articlesCount null`() = runTest {
        val syncDao = FakeSyncMetadataDao()
        val encyclopediaRepository = FakeEncyclopediaRepository(
            throwOnSync = AssertionError("Enciclopédia não deve ser sincronizada na versão 1.0")
        )
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                syncResult = AppResult.Success(RemoteSyncOutcome.Synced(12))
            ),
            encyclopediaRepository = encyclopediaRepository,
            syncDao = syncDao
        )
        advanceUntilIdle()

        assertEquals(SyncState.Idle, repository.syncState.value)

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Success)
        assertEquals(0, encyclopediaRepository.syncCallCount)
        val state = repository.syncState.value as SyncState.Synced
        assertEquals(12, state.entriesCount)
        assertNull(state.articlesCount)
        assertEquals(SyncMetadataEntity.STATUS_SYNCED, syncDao.lastSaved?.lastAttemptStatus)
        assertEquals(12, syncDao.lastSaved?.lastSuccessfulEntriesCount)
        assertNull(syncDao.lastSaved?.lastSuccessfulArticlesCount)
        assertEquals(
            ArchiveSyncRepositoryImpl.MANIFEST_VERSION_UNAVAILABLE,
            syncDao.lastSaved?.lastSuccessfulRemoteVersion
        )
    }

    @Test
    fun `5 - falha do dicionario atualiza estado para OfflineFallback e preserva causa estruturada`() = runTest {
        val syncDao = FakeSyncMetadataDao()
        val cause = IllegalStateException("Timeout na rede")
        val encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(4))
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                syncResult = AppResult.Error(message = "Erro no dicionário", cause = cause)
            ),
            encyclopediaRepository = encyclopediaRepository,
            syncDao = syncDao
        )
        advanceUntilIdle()

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Error)
        assertEquals("Erro no dicionário", (result as AppResult.Error).message)
        assertEquals(cause, result.cause)
        assertEquals(0, encyclopediaRepository.syncCallCount)
        val state = repository.syncState.value as SyncState.OfflineFallback
        assertEquals("Erro no dicionário", state.reason)
        assertEquals(SyncMetadataEntity.STATUS_OFFLINE_FALLBACK, syncDao.lastSaved?.lastAttemptStatus)
    }

    @Test
    fun `6 - falha ou indisponibilidade da enciclopedia nao impede Synced do dicionario nem chama syncFromRemote`() = runTest {
        val syncDao = FakeSyncMetadataDao()
        val encyclopediaRepository = FakeEncyclopediaRepository(
            syncResult = AppResult.Error("Falha ao consultar API da enciclopédia.")
        )
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                syncResult = AppResult.Success(RemoteSyncOutcome.Synced(5))
            ),
            encyclopediaRepository = encyclopediaRepository,
            syncDao = syncDao
        )
        advanceUntilIdle()

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Success)
        assertEquals(0, encyclopediaRepository.syncCallCount)
        val state = repository.syncState.value as SyncState.Synced
        assertEquals(5, state.entriesCount)
        assertNull(state.articlesCount)
        assertEquals(SyncMetadataEntity.STATUS_SYNCED, syncDao.lastSaved?.lastAttemptStatus)
        assertEquals(5, syncDao.lastSaved?.lastSuccessfulEntriesCount)
        assertNull(syncDao.lastSaved?.lastSuccessfulArticlesCount)
    }

    @Test
    fun `observeArchiveStats combina contagens dos repositorios de dicionario e enciclopedia`() = runTest {
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(total = 10, favorites = 3),
            encyclopediaRepository = FakeEncyclopediaRepository(total = 5, bookmarked = 2),
            syncDao = FakeSyncMetadataDao()
        )

        val stats = repository.observeArchiveStats().first()

        assertEquals(10, stats.totalEntries)
        assertEquals(3, stats.favoriteEntries)
        assertEquals(5, stats.totalArticles)
        assertEquals(2, stats.bookmarkedArticles)
    }

    @Test
    fun `estado inicial e Loading antes da primeira emissao do Room`() = runTest {
        val syncDao = FakeSyncMetadataDao(
            initialEntity = null,
            deferInitialEmission = true
        )
        val repository = createRepository(syncDao = syncDao)

        assertEquals(SyncState.Loading, repository.syncState.value)
        advanceUntilIdle()
        assertTrue(syncDao.observedCount > 0)
        assertEquals(SyncState.Loading, repository.syncState.value)
    }

    @Test
    fun `recuperacao inicial transita de Loading para Idle quando DAO emite null`() = runTest {
        val syncDao = FakeSyncMetadataDao(
            initialEntity = null,
            deferInitialEmission = true
        )
        val repository = createRepository(syncDao = syncDao)

        advanceUntilIdle()
        assertEquals(SyncState.Loading, repository.syncState.value)

        syncDao.emitInitialMetadata()
        advanceUntilIdle()

        assertTrue(syncDao.observedCount > 0)
        assertEquals(SyncState.Idle, repository.syncState.value)
    }

    @Test
    fun `recuperacao inicial transita de Loading para Synced incluindo contadores nulos`() = runTest {
        val persistedSynced = SyncMetadataEntity(
            lastAttemptEpochMillis = 1_700_000_010_000L,
            lastAttemptStatus = SyncMetadataEntity.STATUS_SYNCED,
            lastFailureReason = null,
            lastSuccessfulSyncEpochMillis = 1_700_000_010_000L,
            lastSuccessfulEntriesCount = null,
            lastSuccessfulArticlesCount = null,
            lastSuccessfulRemoteVersion = ArchiveSyncRepositoryImpl.MANIFEST_VERSION_UNAVAILABLE
        )
        val syncDao = FakeSyncMetadataDao(
            initialEntity = persistedSynced,
            deferInitialEmission = true
        )
        val repository = createRepository(syncDao = syncDao)

        advanceUntilIdle()
        assertEquals(SyncState.Loading, repository.syncState.value)

        syncDao.emitInitialMetadata()
        advanceUntilIdle()

        val state = repository.syncState.value as SyncState.Synced
        assertEquals(1_700_000_010_000L, state.lastSyncedEpochMillis)
        assertNull(state.entriesCount)
        assertNull(state.articlesCount)
    }

    @Test
    fun `recuperacao inicial transita de Loading para OfflineFallback quando ha falha persistida`() = runTest {
        val persistedFallback = SyncMetadataEntity(
            lastAttemptEpochMillis = 1_700_000_020_000L,
            lastAttemptStatus = SyncMetadataEntity.STATUS_OFFLINE_FALLBACK,
            lastFailureReason = "Sem conectividade na última tentativa",
            lastSuccessfulSyncEpochMillis = 1_700_000_000_000L,
            lastSuccessfulEntriesCount = 20,
            lastSuccessfulArticlesCount = 8,
            lastSuccessfulRemoteVersion = ArchiveSyncRepositoryImpl.MANIFEST_VERSION_UNAVAILABLE
        )
        val syncDao = FakeSyncMetadataDao(
            initialEntity = persistedFallback,
            deferInitialEmission = true
        )
        val repository = createRepository(syncDao = syncDao)

        advanceUntilIdle()
        assertEquals(SyncState.Loading, repository.syncState.value)

        syncDao.emitInitialMetadata()
        advanceUntilIdle()

        val state = repository.syncState.value as SyncState.OfflineFallback
        assertEquals("Sem conectividade na última tentativa", state.reason)
        assertEquals(1_700_000_020_000L, state.lastAttemptEpochMillis)
    }

    @Test
    fun `apos primeira emissao do Room estado nao regride para Loading em erro cancelamento ou sucesso`() = runTest {
        var attempt = 0
        val syncDao = FakeSyncMetadataDao(
            initialEntity = null,
            deferInitialEmission = true
        )
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                customSync = {
                    attempt += 1
                    when (attempt) {
                        1 -> AppResult.Error("Sem internet")
                        2 -> throw CancellationException("Cancelado")
                        else -> AppResult.Success(RemoteSyncOutcome.Synced(10))
                    }
                }
            ),
            encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(4)),
            syncDao = syncDao
        )

        val emittedStates = mutableListOf<SyncState>()
        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.syncState.collect { emittedStates += it }
        }

        advanceUntilIdle()
        assertEquals(SyncState.Loading, repository.syncState.value)

        // Primeira emissão do Room: banco vazio -> Idle
        syncDao.emitInitialMetadata()
        advanceUntilIdle()
        assertEquals(SyncState.Idle, repository.syncState.value)

        // Tentativa 1 (erro) -> OfflineFallback
        repository.syncArchive()
        advanceUntilIdle()
        assertTrue(repository.syncState.value is SyncState.OfflineFallback)

        // Tentativa 2 (cancelamento) -> restaura OfflineFallback, nunca Loading
        try {
            repository.syncArchive()
            fail("Era esperado relançar CancellationException")
        } catch (_: CancellationException) {
            advanceUntilIdle()
        }
        assertTrue(repository.syncState.value is SyncState.OfflineFallback)

        // Tentativa 3 (sucesso) -> Synced
        repository.syncArchive()
        advanceUntilIdle()
        assertTrue(repository.syncState.value is SyncState.Synced)

        assertEquals(SyncState.Loading, emittedStates.first())
        assertEquals(1, emittedStates.count { it is SyncState.Loading })
        assertFalse(emittedStates.drop(1).any { it is SyncState.Loading })

        collectorJob.cancel()
    }

    @Test
    fun `falha apos sincronizacao bem-sucedida preserva dados da ultima sincronizacao no Room`() = runTest {
        val initialSynced = SyncMetadataEntity(
            lastAttemptEpochMillis = 1_700_000_000_000L,
            lastAttemptStatus = SyncMetadataEntity.STATUS_SYNCED,
            lastFailureReason = null,
            lastSuccessfulSyncEpochMillis = 1_700_000_000_000L,
            lastSuccessfulEntriesCount = 30,
            lastSuccessfulArticlesCount = 9,
            lastSuccessfulRemoteVersion = "v1.2.3"
        )
        val syncDao = FakeSyncMetadataDao(initialEntity = initialSynced)
        val encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(9))
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                syncResult = AppResult.Error("Tempo limite excedido")
            ),
            encyclopediaRepository = encyclopediaRepository,
            syncDao = syncDao
        )

        val recovered = repository.syncState.first { it is SyncState.Synced } as SyncState.Synced
        assertEquals(30, recovered.entriesCount)

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Error)
        assertEquals(0, encyclopediaRepository.syncCallCount)
        val currentState = repository.syncState.value as SyncState.OfflineFallback
        assertEquals("Tempo limite excedido", currentState.reason)

        val saved = syncDao.lastSaved
        assertNotNull(saved)
        assertEquals(SyncMetadataEntity.STATUS_OFFLINE_FALLBACK, saved?.lastAttemptStatus)
        assertEquals("Tempo limite excedido", saved?.lastFailureReason)
        assertTrue((saved?.lastAttemptEpochMillis ?: 0L) > initialSynced.lastAttemptEpochMillis)
        assertEquals(1_700_000_000_000L, saved?.lastSuccessfulSyncEpochMillis)
        assertEquals(30, saved?.lastSuccessfulEntriesCount)
        assertEquals(9, saved?.lastSuccessfulArticlesCount)
        assertEquals("v1.2.3", saved?.lastSuccessfulRemoteVersion)
    }

    @Test
    fun `cancelamento apos estado recuperado restaura o estado Synced persistido no Room`() = runTest {
        val initialSynced = SyncMetadataEntity(
            lastAttemptEpochMillis = 1_700_000_000_000L,
            lastAttemptStatus = SyncMetadataEntity.STATUS_SYNCED,
            lastFailureReason = null,
            lastSuccessfulSyncEpochMillis = 1_700_000_000_000L,
            lastSuccessfulEntriesCount = 14,
            lastSuccessfulArticlesCount = 6,
            lastSuccessfulRemoteVersion = ArchiveSyncRepositoryImpl.MANIFEST_VERSION_UNAVAILABLE
        )
        val syncDao = FakeSyncMetadataDao(initialEntity = initialSynced)
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                throwOnSync = CancellationException("Cancelado pelo chamador")
            ),
            encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(6)),
            syncDao = syncDao
        )
        repository.syncState.first { it is SyncState.Synced }

        try {
            repository.syncArchive()
            fail("Era esperado relançar CancellationException")
        } catch (_: CancellationException) {
            advanceUntilIdle()
            val restored = repository.syncState.value as SyncState.Synced
            assertEquals(1_700_000_000_000L, restored.lastSyncedEpochMillis)
            assertEquals(14, restored.entriesCount)
            assertEquals(6, restored.articlesCount)
        }
    }

    @Test
    fun `falha ao consultar metadados no Room nao trava em Syncing nem sobrescreve o banco`() = runTest {
        val initialSynced = SyncMetadataEntity(
            lastAttemptEpochMillis = 1_700_000_000_000L,
            lastAttemptStatus = SyncMetadataEntity.STATUS_SYNCED,
            lastFailureReason = null,
            lastSuccessfulSyncEpochMillis = 1_700_000_000_000L,
            lastSuccessfulEntriesCount = 11,
            lastSuccessfulArticlesCount = 4,
            lastSuccessfulRemoteVersion = ArchiveSyncRepositoryImpl.MANIFEST_VERSION_UNAVAILABLE
        )
        val syncDao = FakeSyncMetadataDao(
            initialEntity = initialSynced,
            throwOnRead = IllegalStateException("Erro ao consultar metadados no SQLite")
        )
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                syncResult = AppResult.Success(RemoteSyncOutcome.Synced(18))
            ),
            encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(7)),
            syncDao = syncDao
        )
        repository.syncState.first { it is SyncState.Synced }

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Error)
        val state = repository.syncState.value as SyncState.OfflineFallback
        assertEquals("Erro ao consultar metadados no SQLite", state.reason)
        assertEquals(initialSynced, syncDao.lastSaved)
    }

    @Test
    fun `falha ao persistir metadados mantem OfflineFallback em memoria e preserva registro anterior`() = runTest {
        val initialSynced = SyncMetadataEntity(
            lastAttemptEpochMillis = 1_700_000_000_000L,
            lastAttemptStatus = SyncMetadataEntity.STATUS_SYNCED,
            lastFailureReason = null,
            lastSuccessfulSyncEpochMillis = 1_700_000_000_000L,
            lastSuccessfulEntriesCount = 10,
            lastSuccessfulArticlesCount = 3,
            lastSuccessfulRemoteVersion = ArchiveSyncRepositoryImpl.MANIFEST_VERSION_UNAVAILABLE
        )
        val syncDao = FakeSyncMetadataDao(
            initialEntity = initialSynced,
            throwOnUpsert = IllegalStateException("Erro de escrita no SQLite")
        )
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                syncResult = AppResult.Success(RemoteSyncOutcome.Synced(25))
            ),
            encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(9)),
            syncDao = syncDao
        )
        repository.syncState.first { it is SyncState.Synced }

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Error)
        val state = repository.syncState.value as SyncState.OfflineFallback
        assertEquals("Erro de escrita no SQLite", state.reason)
        assertEquals(initialSynced, syncDao.lastSaved)
    }

    @Test
    fun `timestamps de tentativas consecutivas sao estritamente crescentes em relacao ao Room`() = runTest {
        val futureReference = System.currentTimeMillis() + 120_000L
        val persisted = SyncMetadataEntity(
            lastAttemptEpochMillis = futureReference,
            lastAttemptStatus = SyncMetadataEntity.STATUS_SYNCED,
            lastFailureReason = null,
            lastSuccessfulSyncEpochMillis = futureReference,
            lastSuccessfulEntriesCount = 1,
            lastSuccessfulArticlesCount = 1,
            lastSuccessfulRemoteVersion = ArchiveSyncRepositoryImpl.MANIFEST_VERSION_UNAVAILABLE
        )
        val syncDao = FakeSyncMetadataDao(initialEntity = persisted)
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                syncResult = AppResult.Success(RemoteSyncOutcome.Synced(2))
            ),
            encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(3)),
            syncDao = syncDao
        )
        repository.syncState.first { it is SyncState.Synced }

        val firstResult = repository.syncArchive()
        advanceUntilIdle()
        assertTrue(firstResult is AppResult.Success)
        val firstAttemptTimestamp = syncDao.lastSaved?.lastAttemptEpochMillis ?: 0L
        assertEquals(futureReference + 1L, firstAttemptTimestamp)

        val secondResult = repository.syncArchive()
        advanceUntilIdle()
        assertTrue(secondResult is AppResult.Success)
        val secondAttemptTimestamp = syncDao.lastSaved?.lastAttemptEpochMillis ?: 0L
        assertEquals(firstAttemptTimestamp + 1L, secondAttemptTimestamp)
    }

    @Test
    fun `estados transitorios Syncing e PendingRoomEmission protegem contra atraso e emissao antiga do Room`() = runTest {
        val firstGate = CompletableDeferred<AppResult<RemoteSyncOutcome>>()
        val secondGate = CompletableDeferred<AppResult<RemoteSyncOutcome>>()
        var syncCallCount = 0
        val syncDao = FakeSyncMetadataDao(deferFlowEmission = true)

        val dictionaryRepository = FakeDictionaryRepository(
            customSync = {
                syncCallCount += 1
                if (syncCallCount == 1) firstGate.await() else secondGate.await()
            }
        )
        val encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(5))
        val repository = createRepository(
            dictionaryRepository = dictionaryRepository,
            encyclopediaRepository = encyclopediaRepository,
            syncDao = syncDao
        )
        advanceUntilIdle()

        val firstJob = async { repository.syncArchive() }
        advanceUntilIdle()
        assertEquals(SyncState.Syncing, repository.syncState.value)

        firstGate.complete(AppResult.Success(RemoteSyncOutcome.Synced(15)))
        assertTrue(firstJob.await() is AppResult.Success)
        advanceUntilIdle()

        // Mesmo antes de o Flow do Room emitir, PendingRoomEmission já expõe Synced.
        val resolvedBeforeRoomEmit = repository.syncState.value as SyncState.Synced
        assertEquals(15, resolvedBeforeRoomEmit.entriesCount)
        assertNull(resolvedBeforeRoomEmit.articlesCount)

        // Inicia segunda sincronização antes da emissão atrasada do Room da primeira tentativa.
        val secondJob = async { repository.syncArchive() }
        advanceUntilIdle()
        assertEquals(SyncState.Syncing, repository.syncState.value)

        // Emissão antiga do Room da primeira tentativa não pode sobrescrever Syncing ativo.
        syncDao.emitLatestToObservers()
        advanceUntilIdle()
        assertEquals(SyncState.Syncing, repository.syncState.value)

        secondGate.complete(AppResult.Success(RemoteSyncOutcome.Synced(22)))
        assertTrue(secondJob.await() is AppResult.Success)
        syncDao.emitLatestToObservers()
        advanceUntilIdle()

        val finalState = repository.syncState.value as SyncState.Synced
        assertEquals(22, finalState.entriesCount)
        assertNull(finalState.articlesCount)
        assertEquals(0, encyclopediaRepository.syncCallCount)
    }

    @Test
    fun `15 - falha na atualizacao do seed nao impede tentativa de sincronizacao remota nem grava erro do seed em sync_metadata`() = runTest {
        val callOrder = mutableListOf<String>()
        val syncDao = FakeSyncMetadataDao()
        val seedUpdater = FakeDictionarySeedUpdater(
            customUpdate = {
                callOrder += "seed"
                AppResult.Error("JSON local malformado")
            }
        )
        val errorResultRepository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                customSync = {
                    callOrder += "remote"
                    AppResult.Success(RemoteSyncOutcome.Synced(7))
                }
            ),
            syncDao = syncDao,
            dictionarySeedUpdater = seedUpdater
        )
        advanceUntilIdle()

        val result = errorResultRepository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Success)
        assertEquals(1, seedUpdater.updateCallCount)
        assertEquals(listOf("seed", "remote"), callOrder)
        val synced = errorResultRepository.syncState.value as SyncState.Synced
        assertEquals(7, synced.entriesCount)
        assertEquals(SyncMetadataEntity.STATUS_SYNCED, syncDao.lastSaved?.lastAttemptStatus)
        assertNull(syncDao.lastSaved?.lastFailureReason)

        // Também não deve bloquear caso o atualizador lance exceção inesperada não-cancelamento
        val exceptionCallOrder = mutableListOf<String>()
        val throwingSyncDao = FakeSyncMetadataDao()
        val throwingUpdater = FakeDictionarySeedUpdater(
            customUpdate = {
                exceptionCallOrder += "seed"
                throw IllegalStateException("Erro inesperado no asset")
            }
        )
        val throwingSeederRepository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                customSync = {
                    exceptionCallOrder += "remote"
                    AppResult.Error("Sem internet")
                }
            ),
            syncDao = throwingSyncDao,
            dictionarySeedUpdater = throwingUpdater
        )
        advanceUntilIdle()

        val fallbackResult = throwingSeederRepository.syncArchive()
        advanceUntilIdle()

        assertTrue(fallbackResult is AppResult.Error)
        assertEquals(1, throwingUpdater.updateCallCount)
        assertEquals(listOf("seed", "remote"), exceptionCallOrder)
        val fallbackState = throwingSeederRepository.syncState.value as SyncState.OfflineFallback
        assertEquals("Sem internet", fallbackState.reason)
        assertEquals("Sem internet", throwingSyncDao.lastSaved?.lastFailureReason)
    }

    @Test
    fun `seed local atualizado com sucesso nao registra STATUS_SYNCED quando chamada remota falha`() = runTest {
        val syncDao = FakeSyncMetadataDao()
        val callOrder = mutableListOf<String>()
        val seedUpdater = FakeDictionarySeedUpdater(
            customUpdate = { version ->
                callOrder += "seed"
                AppResult.Success(
                    SeedUpdateOutcome(
                        status = SeedUpdateOutcomeStatus.UPDATED,
                        entriesProcessedCount = 10,
                        appliedSeedVersion = version,
                        appliedSeedHash = "hash_v$version",
                        currentSeedVersion = version,
                        currentSeedHash = "hash_v$version",
                        message = "Seed atualizado"
                    )
                )
            }
        )
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                customSync = {
                    callOrder += "remote"
                    AppResult.Error("Backend indisponível")
                }
            ),
            syncDao = syncDao,
            dictionarySeedUpdater = seedUpdater
        )
        advanceUntilIdle()

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Error)
        assertEquals(1, seedUpdater.updateCallCount)
        assertEquals(listOf("seed", "remote"), callOrder)
        assertEquals(
            SyncMetadataEntity.STATUS_OFFLINE_FALLBACK,
            syncDao.lastSaved?.lastAttemptStatus
        )
        assertNull(syncDao.lastSaved?.lastSuccessfulSyncEpochMillis)
        assertTrue(repository.syncState.value is SyncState.OfflineFallback)
    }

    @Test
    fun `cancelamento durante atualizacao do seed restaura estado anterior nao chama remoto e relanca CancellationException`() = runTest {
        val syncDao = FakeSyncMetadataDao()
        var remoteCallCount = 0
        val seedUpdater = FakeDictionarySeedUpdater(
            customUpdate = {
                throw CancellationException("Cancelado durante seed")
            }
        )
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                customSync = {
                    remoteCallCount += 1
                    AppResult.Success(RemoteSyncOutcome.Synced(3))
                }
            ),
            syncDao = syncDao,
            dictionarySeedUpdater = seedUpdater
        )
        advanceUntilIdle()

        try {
            repository.syncArchive()
            fail("Era esperado relançar CancellationException")
        } catch (_: CancellationException) {
            advanceUntilIdle()
            assertEquals(1, seedUpdater.updateCallCount)
            assertEquals(0, remoteCallCount)
            assertNull(syncDao.lastSaved)
            assertEquals(SyncState.Idle, repository.syncState.value)
        }
    }

    @Test
    fun `atualizacao do seed e executada exatamente uma vez por syncArchive e antes da sincronizacao remota`() = runTest {
        val callOrder = mutableListOf<String>()
        val seedUpdater = FakeDictionarySeedUpdater(
            customUpdate = { version ->
                callOrder += "seed"
                AppResult.Success(
                    SeedUpdateOutcome(
                        status = SeedUpdateOutcomeStatus.APPLIED_INITIAL,
                        entriesProcessedCount = 20,
                        appliedSeedVersion = version,
                        appliedSeedHash = "hash_initial",
                        currentSeedVersion = version,
                        currentSeedHash = "hash_initial",
                        message = "Aplicado"
                    )
                )
            }
        )
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                customSync = {
                    callOrder += "remote"
                    AppResult.Success(RemoteSyncOutcome.Synced(20))
                }
            ),
            syncDao = FakeSyncMetadataDao(),
            dictionarySeedUpdater = seedUpdater
        )
        advanceUntilIdle()

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Success)
        assertEquals(1, seedUpdater.updateCallCount)
        assertEquals(listOf(DictionarySeedSpec.CURRENT_SEED_VERSION), seedUpdater.capturedVersions)
        assertEquals(listOf("seed", "remote"), callOrder)
    }

    @Test
    fun `todos os status de SeedUpdateOutcomeStatus permitem prosseguir para a sincronizacao remota`() = runTest {
        SeedUpdateOutcomeStatus.entries.forEach { status ->
            val callOrder = mutableListOf<String>()
            val syncDao = FakeSyncMetadataDao()
            val seedUpdater = FakeDictionarySeedUpdater(
                customUpdate = { version ->
                    callOrder += "seed"
                    AppResult.Success(
                        SeedUpdateOutcome(
                            status = status,
                            entriesProcessedCount = 0,
                            appliedSeedVersion = version,
                            appliedSeedHash = "hash_$status",
                            currentSeedVersion = version,
                            currentSeedHash = "hash_$status",
                            message = "Status $status"
                        )
                    )
                }
            )
            val repository = createRepository(
                dictionaryRepository = FakeDictionaryRepository(
                    customSync = {
                        callOrder += "remote"
                        AppResult.Success(RemoteSyncOutcome.Synced(14))
                    }
                ),
                syncDao = syncDao,
                dictionarySeedUpdater = seedUpdater
            )
            advanceUntilIdle()

            val result = repository.syncArchive()
            advanceUntilIdle()

            assertTrue("Falha para status $status", result is AppResult.Success)
            assertEquals("Contagem incorreta para status $status", 1, seedUpdater.updateCallCount)
            assertEquals("Ordem incorreta para status $status", listOf("seed", "remote"), callOrder)
            val state = repository.syncState.value as SyncState.Synced
            assertEquals(14, state.entriesCount)
            assertNotNull(syncDao.lastSaved?.lastSuccessfulSyncEpochMillis)
        }
    }

    @Test
    fun `RemoteSyncOutcome Empty sem historico oficial grava STATUS_REMOTE_EMPTY preserva nulls e emite RemoteEmpty`() = runTest {
        val syncDao = FakeSyncMetadataDao()
        val encyclopediaRepository = FakeEncyclopediaRepository(
            throwOnSync = AssertionError("Enciclopédia não deve ser chamada")
        )
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                syncResult = AppResult.Success(RemoteSyncOutcome.Empty)
            ),
            encyclopediaRepository = encyclopediaRepository,
            syncDao = syncDao
        )
        advanceUntilIdle()

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Success)
        assertEquals(0, encyclopediaRepository.syncCallCount)

        val state = repository.syncState.value as SyncState.RemoteEmpty
        assertNull(state.lastSyncedEpochMillis)
        assertNull(state.entriesCount)
        assertNull(state.articlesCount)

        val saved = syncDao.lastSaved
        assertNotNull(saved)
        assertEquals(SyncMetadataEntity.STATUS_REMOTE_EMPTY, saved?.lastAttemptStatus)
        assertNull(saved?.lastFailureReason)
        assertNull(saved?.lastSuccessfulSyncEpochMillis)
        assertNull(saved?.lastSuccessfulEntriesCount)
        assertNull(saved?.lastSuccessfulArticlesCount)
        assertNull(saved?.lastSuccessfulRemoteVersion)
    }

    @Test
    fun `RemoteSyncOutcome Empty apos sincronizacao oficial preserva historico no Room e no SyncState`() = runTest {
        val initialSynced = SyncMetadataEntity(
            lastAttemptEpochMillis = 1_700_000_000_000L,
            lastAttemptStatus = SyncMetadataEntity.STATUS_SYNCED,
            lastFailureReason = null,
            lastSuccessfulSyncEpochMillis = 1_700_000_000_000L,
            lastSuccessfulEntriesCount = 24,
            lastSuccessfulArticlesCount = 6,
            lastSuccessfulRemoteVersion = "v1.4.0"
        )
        val syncDao = FakeSyncMetadataDao(initialEntity = initialSynced)
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                syncResult = AppResult.Success(RemoteSyncOutcome.Empty)
            ),
            syncDao = syncDao
        )
        repository.syncState.first { it is SyncState.Synced }

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Success)
        val state = repository.syncState.value as SyncState.RemoteEmpty
        assertTrue(state.lastAttemptEpochMillis > initialSynced.lastAttemptEpochMillis)
        assertEquals(1_700_000_000_000L, state.lastSyncedEpochMillis)
        assertEquals(24, state.entriesCount)
        assertEquals(6, state.articlesCount)

        val saved = syncDao.lastSaved
        assertNotNull(saved)
        assertEquals(SyncMetadataEntity.STATUS_REMOTE_EMPTY, saved?.lastAttemptStatus)
        assertNull(saved?.lastFailureReason)
        assertTrue((saved?.lastAttemptEpochMillis ?: 0L) > initialSynced.lastAttemptEpochMillis)
        assertEquals(1_700_000_000_000L, saved?.lastSuccessfulSyncEpochMillis)
        assertEquals(24, saved?.lastSuccessfulEntriesCount)
        assertEquals(6, saved?.lastSuccessfulArticlesCount)
        assertEquals("v1.4.0", saved?.lastSuccessfulRemoteVersion)
    }

    @Test
    fun `recuperacao inicial transita de Loading para RemoteEmpty com e sem historico oficial persistido`() = runTest {
        val withoutHistory = SyncMetadataEntity(
            lastAttemptEpochMillis = 1_700_000_030_000L,
            lastAttemptStatus = SyncMetadataEntity.STATUS_REMOTE_EMPTY,
            lastFailureReason = null,
            lastSuccessfulSyncEpochMillis = null,
            lastSuccessfulEntriesCount = null,
            lastSuccessfulArticlesCount = null,
            lastSuccessfulRemoteVersion = null
        )
        val daoWithoutHistory = FakeSyncMetadataDao(
            initialEntity = withoutHistory,
            deferInitialEmission = true
        )
        val repoWithoutHistory = createRepository(syncDao = daoWithoutHistory)

        advanceUntilIdle()
        assertEquals(SyncState.Loading, repoWithoutHistory.syncState.value)

        daoWithoutHistory.emitInitialMetadata()
        advanceUntilIdle()

        val recoveredNoHistory = repoWithoutHistory.syncState.value as SyncState.RemoteEmpty
        assertEquals(1_700_000_030_000L, recoveredNoHistory.lastAttemptEpochMillis)
        assertNull(recoveredNoHistory.lastSyncedEpochMillis)
        assertNull(recoveredNoHistory.entriesCount)

        val withHistory = SyncMetadataEntity(
            lastAttemptEpochMillis = 1_700_000_050_000L,
            lastAttemptStatus = SyncMetadataEntity.STATUS_REMOTE_EMPTY,
            lastFailureReason = null,
            lastSuccessfulSyncEpochMillis = 1_700_000_010_000L,
            lastSuccessfulEntriesCount = 35,
            lastSuccessfulArticlesCount = 8,
            lastSuccessfulRemoteVersion = "v2.0.0"
        )
        val daoWithHistory = FakeSyncMetadataDao(
            initialEntity = withHistory,
            deferInitialEmission = true
        )
        val repoWithHistory = createRepository(syncDao = daoWithHistory)

        advanceUntilIdle()
        assertEquals(SyncState.Loading, repoWithHistory.syncState.value)

        daoWithHistory.emitInitialMetadata()
        advanceUntilIdle()

        val recoveredWithHistory = repoWithHistory.syncState.value as SyncState.RemoteEmpty
        assertEquals(1_700_000_050_000L, recoveredWithHistory.lastAttemptEpochMillis)
        assertEquals(1_700_000_010_000L, recoveredWithHistory.lastSyncedEpochMillis)
        assertEquals(35, recoveredWithHistory.entriesCount)
        assertEquals(8, recoveredWithHistory.articlesCount)
    }

    @Test
    fun `cancelamento durante registro de RemoteEmpty restaura estado anterior e relanca CancellationException`() = runTest {
        val syncDao = FakeSyncMetadataDao(
            throwOnRecordFailure = CancellationException("Cancelado ao gravar RemoteEmpty")
        )
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                syncResult = AppResult.Success(RemoteSyncOutcome.Empty)
            ),
            syncDao = syncDao
        )
        advanceUntilIdle()

        try {
            repository.syncArchive()
            fail("Era esperado relançar CancellationException")
        } catch (_: CancellationException) {
            advanceUntilIdle()
            assertEquals(SyncState.Idle, repository.syncState.value)
            assertNull(syncDao.lastSaved)
        }
    }

    @Test
    fun `dispositivo Offline com falha remota classifica como NO_INTERNET e mantem timestamp nulo sem sync anterior`() = runTest {
        val syncDao = FakeSyncMetadataDao()
        val connectivityObserver = FakeNetworkConnectivityObserver(DeviceConnectivityState.Offline)
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                syncResult = AppResult.Error(
                    message = "Sem rede",
                    cause = UnknownHostException("archive.parintintin.org"),
                    remoteErrorKind = RemoteErrorKind.DNS_FAILURE
                )
            ),
            syncDao = syncDao,
            connectivityObserver = connectivityObserver
        )
        advanceUntilIdle()

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Error)
        val state = repository.syncState.value as SyncState.OfflineFallback
        assertEquals(SyncFailureType.NO_INTERNET, state.failureType)
        assertNull(state.lastSuccessfulSyncEpochMillis)
        assertNull(syncDao.lastSaved?.lastSuccessfulSyncEpochMillis)
    }

    @Test
    fun `rede ConnectedUnvalidated com falha remota classifica como UNVALIDATED_NETWORK`() = runTest {
        val connectivityObserver = FakeNetworkConnectivityObserver(
            DeviceConnectivityState.ConnectedUnvalidated
        )
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                syncResult = AppResult.Error(
                    message = "Rede sem acesso validado",
                    cause = SocketTimeoutException("timeout"),
                    remoteErrorKind = RemoteErrorKind.TIMEOUT
                )
            ),
            connectivityObserver = connectivityObserver
        )
        advanceUntilIdle()

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Error)
        val state = repository.syncState.value as SyncState.OfflineFallback
        assertEquals(SyncFailureType.UNVALIDATED_NETWORK, state.failureType)
        assertNull(state.lastSuccessfulSyncEpochMillis)
    }

    @Test
    fun `dispositivo Online com HTTP 500 timeout e erro DNS classifica como SERVER_UNAVAILABLE`() = runTest {
        val http500 = HttpException(
            Response.error<Unit>(500, "{}".toResponseBody("application/json".toMediaType()))
        )
        val failures = listOf(
            Triple(http500, RemoteErrorKind.HTTP_SERVER_ERROR, "HTTP 500"),
            Triple(SocketTimeoutException("timeout"), RemoteErrorKind.TIMEOUT, "Timeout"),
            Triple(UnknownHostException("archive.parintintin.org"), RemoteErrorKind.DNS_FAILURE, "DNS")
        )

        failures.forEach { (cause, kind, label) ->
            val connectivityObserver = FakeNetworkConnectivityObserver(DeviceConnectivityState.Online)
            val repository = createRepository(
                dictionaryRepository = FakeDictionaryRepository(
                    syncResult = AppResult.Error(
                        message = label,
                        cause = cause,
                        remoteErrorKind = kind
                    )
                ),
                connectivityObserver = connectivityObserver
            )
            advanceUntilIdle()

            val result = repository.syncArchive()
            advanceUntilIdle()

            assertTrue(result is AppResult.Error)
            val state = repository.syncState.value as SyncState.OfflineFallback
            assertEquals(SyncFailureType.SERVER_UNAVAILABLE, state.failureType)
            assertNull(state.lastSuccessfulSyncEpochMillis)
        }
    }

    @Test
    fun `mudanca de conectividade durante tentativa HTTP usa estado atual no momento da falha sem usar cache antigo`() = runTest {
        val connectivityObserver = FakeNetworkConnectivityObserver(DeviceConnectivityState.Online)
        var attempt = 0
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                customSync = {
                    attempt += 1
                    if (attempt == 1) {
                        // Falha enquanto dispositivo permanece Online -> SERVER_UNAVAILABLE
                        AppResult.Error(
                            message = "Erro servidor",
                            remoteErrorKind = RemoteErrorKind.HTTP_SERVER_ERROR
                        )
                    } else {
                        // Durante a tentativa 2, conectividade cai para Offline antes do tratamento da falha
                        connectivityObserver.setState(DeviceConnectivityState.Offline)
                        AppResult.Error(
                            message = "Conexão perdida",
                            remoteErrorKind = RemoteErrorKind.IO_FAILURE
                        )
                    }
                }
            ),
            connectivityObserver = connectivityObserver
        )
        advanceUntilIdle()

        repository.syncArchive()
        advanceUntilIdle()
        assertEquals(
            SyncFailureType.SERVER_UNAVAILABLE,
            (repository.syncState.value as SyncState.OfflineFallback).failureType
        )

        repository.syncArchive()
        advanceUntilIdle()
        assertEquals(
            SyncFailureType.NO_INTERNET,
            (repository.syncState.value as SyncState.OfflineFallback).failureType
        )
    }

    @Test
    fun `falha apos sincronizacao oficial anterior preserva lastSuccessfulSyncEpochMillis em OfflineFallback e em falhas subsequentes`() = runTest {
        val officialEpoch = 1_700_000_000_000L
        val initialSynced = SyncMetadataEntity(
            lastAttemptEpochMillis = officialEpoch,
            lastAttemptStatus = SyncMetadataEntity.STATUS_SYNCED,
            lastFailureReason = null,
            lastSuccessfulSyncEpochMillis = officialEpoch,
            lastSuccessfulEntriesCount = 30,
            lastSuccessfulArticlesCount = null,
            lastSuccessfulRemoteVersion = ArchiveSyncRepositoryImpl.MANIFEST_VERSION_UNAVAILABLE
        )
        val syncDao = FakeSyncMetadataDao(initialEntity = initialSynced)
        val connectivityObserver = FakeNetworkConnectivityObserver(DeviceConnectivityState.Online)
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                syncResult = AppResult.Error(
                    message = "Servidor indisponível",
                    remoteErrorKind = RemoteErrorKind.HTTP_SERVER_ERROR
                )
            ),
            syncDao = syncDao,
            connectivityObserver = connectivityObserver
        )
        repository.syncState.first { it is SyncState.Synced }

        repository.syncArchive()
        advanceUntilIdle()

        val firstFallback = repository.syncState.value as SyncState.OfflineFallback
        assertEquals(SyncFailureType.SERVER_UNAVAILABLE, firstFallback.failureType)
        assertEquals(officialEpoch, firstFallback.lastSuccessfulSyncEpochMillis)
        assertEquals(officialEpoch, syncDao.lastSaved?.lastSuccessfulSyncEpochMillis)

        // Segunda falha consecutiva (agora Offline) não pode perder o timestamp oficial nem manter cache SERVER_UNAVAILABLE
        connectivityObserver.setState(DeviceConnectivityState.Offline)
        repository.syncArchive()
        advanceUntilIdle()

        val secondFallback = repository.syncState.value as SyncState.OfflineFallback
        assertEquals(SyncFailureType.NO_INTERNET, secondFallback.failureType)
        assertEquals(officialEpoch, secondFallback.lastSuccessfulSyncEpochMillis)
        assertEquals(officialEpoch, syncDao.lastSaved?.lastSuccessfulSyncEpochMillis)
    }

    @Test
    fun `HTTP 200 com lista vazia continua produzindo RemoteEmpty mesmo com observador Online sem classificar como falha`() = runTest {
        val syncDao = FakeSyncMetadataDao()
        val connectivityObserver = FakeNetworkConnectivityObserver(DeviceConnectivityState.Online)
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                syncResult = AppResult.Success(RemoteSyncOutcome.Empty)
            ),
            syncDao = syncDao,
            connectivityObserver = connectivityObserver
        )
        advanceUntilIdle()

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Success)
        assertTrue(repository.syncState.value is SyncState.RemoteEmpty)
        assertEquals(SyncMetadataEntity.STATUS_REMOTE_EMPTY, syncDao.lastSaved?.lastAttemptStatus)
        assertNull(syncDao.lastSaved?.lastFailureReason)
    }

    private fun TestScope.createRepository(
        dictionaryRepository: DictionaryRepository = FakeDictionaryRepository(),
        encyclopediaRepository: EncyclopediaRepository = FakeEncyclopediaRepository(),
        syncDao: SyncMetadataDao = FakeSyncMetadataDao(),
        dictionarySeedUpdater: DictionarySeedUpdater = FakeDictionarySeedUpdater(),
        connectivityObserver: NetworkConnectivityObserver = FakeNetworkConnectivityObserver()
    ): ArchiveSyncRepositoryImpl {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val applicationScope = CoroutineScope(SupervisorJob() + testDispatcher)
        createdScopes += applicationScope
        return ArchiveSyncRepositoryImpl(
            dictionaryRepository = dictionaryRepository,
            encyclopediaRepository = encyclopediaRepository,
            syncMetadataDao = syncDao,
            dictionarySeedUpdater = dictionarySeedUpdater,
            networkConnectivityObserver = connectivityObserver,
            dispatchers = TestDispatcherProvider(testDispatcher),
            applicationScope = applicationScope
        )
    }

    private class TestDispatcherProvider(
        dispatcher: CoroutineDispatcher
    ) : DispatcherProvider {
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
    }

    private class FakeSyncMetadataDao(
        initialEntity: SyncMetadataEntity? = null,
        private val throwOnRead: Throwable? = null,
        private val throwOnUpsert: Throwable? = null,
        private val throwOnRecordFailure: Throwable? = null,
        private val deferFlowEmission: Boolean = false,
        private val deferInitialEmission: Boolean = false
    ) : SyncMetadataDao {
        private val immediateState = MutableStateFlow(initialEntity)
        private val controlledState = MutableSharedFlow<SyncMetadataEntity?>(
            replay = 1,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        var lastSaved: SyncMetadataEntity? = initialEntity
            private set
        var observedCount: Int = 0
            private set

        override fun observeMetadata(id: String): Flow<SyncMetadataEntity?> {
            observedCount += 1
            return if (deferInitialEmission) {
                controlledState.asSharedFlow()
            } else {
                immediateState
            }
        }

        override suspend fun getMetadataOnce(id: String): SyncMetadataEntity? {
            throwOnRead?.let { throw it }
            return lastSaved
        }

        override suspend fun upsert(metadata: SyncMetadataEntity) {
            throwOnUpsert?.let { throw it }
            lastSaved = metadata
            if (!deferFlowEmission) {
                emitToObservers(metadata)
            }
        }

        override suspend fun recordAttemptFailure(
            attemptEpochMillis: Long,
            failureReason: String?
        ) {
            throwOnRecordFailure?.let { throw it }
            super.recordAttemptFailure(attemptEpochMillis, failureReason)
        }

        override suspend fun recordRemoteEmptyAttempt(attemptEpochMillis: Long) {
            throwOnRecordFailure?.let { throw it }
            super.recordRemoteEmptyAttempt(attemptEpochMillis)
        }

        fun emitInitialMetadata() {
            emitToObservers(lastSaved)
        }

        fun emitLatestToObservers() {
            emitToObservers(lastSaved)
        }

        private fun emitToObservers(metadata: SyncMetadataEntity?) {
            if (deferInitialEmission) {
                check(controlledState.tryEmit(metadata))
            } else {
                immediateState.value = metadata
            }
        }
    }

    private class FakeDictionaryRepository(
        private val total: Int = 0,
        private val favorites: Int = 0,
        private val syncResult: AppResult<RemoteSyncOutcome> = AppResult.Success(RemoteSyncOutcome.Empty),
        private val throwOnSync: Throwable? = null,
        private val customSync: (suspend () -> AppResult<RemoteSyncOutcome>)? = null
    ) : DictionaryRepository {
        override fun observeEntries(
            query: String,
            category: SemanticCategory?,
            onlyFavorites: Boolean,
            grammaticalClass: GrammaticalClass?
        ): Flow<List<DictionaryEntry>> = flowOf(emptyList())

        override fun observeEntryById(entryId: String): Flow<DictionaryEntry?> = flowOf(null)
        override fun observeTotalCount(): Flow<Int> = flowOf(total)
        override fun observeFavoriteCount(): Flow<Int> = flowOf(favorites)
        override suspend fun toggleFavorite(entryId: String) = Unit
        override suspend fun syncFromRemote(): AppResult<RemoteSyncOutcome> {
            customSync?.let { return it() }
            throwOnSync?.let { throw it }
            return syncResult
        }
    }

    private class FakeEncyclopediaRepository(
        private val total: Int = 0,
        private val bookmarked: Int = 0,
        private val syncResult: AppResult<Int> = AppResult.Success(0),
        private val throwOnSync: Throwable? = null
    ) : EncyclopediaRepository {
        var syncCallCount: Int = 0
            private set

        override fun observeArticles(
            query: String,
            topic: CulturalTopic?,
            onlyBookmarked: Boolean
        ): Flow<List<CulturalArticle>> = flowOf(emptyList())

        override fun observeArticleById(articleId: String): Flow<CulturalArticle?> = flowOf(null)
        override fun observeTotalCount(): Flow<Int> = flowOf(total)
        override fun observeBookmarkedCount(): Flow<Int> = flowOf(bookmarked)
        override suspend fun toggleBookmark(articleId: String) = Unit
        override suspend fun syncFromRemote(): AppResult<Int> {
            syncCallCount += 1
            throwOnSync?.let { throw it }
            return syncResult
        }
    }

    private class FakeDictionarySeedUpdater(
        private val outcomeStatus: SeedUpdateOutcomeStatus = SeedUpdateOutcomeStatus.ALREADY_UP_TO_DATE,
        private val customUpdate: (suspend (Int) -> AppResult<SeedUpdateOutcome>)? = null
    ) : DictionarySeedUpdater {
        var updateCallCount: Int = 0
            private set
        val capturedVersions = mutableListOf<Int>()

        override suspend fun updateIfNeeded(currentVersion: Int): AppResult<SeedUpdateOutcome> {
            updateCallCount += 1
            capturedVersions += currentVersion
            customUpdate?.let { return it(currentVersion) }
            return AppResult.Success(
                SeedUpdateOutcome(
                    status = outcomeStatus,
                    entriesProcessedCount = when (outcomeStatus) {
                        SeedUpdateOutcomeStatus.APPLIED_INITIAL,
                        SeedUpdateOutcomeStatus.UPDATED -> 20
                        SeedUpdateOutcomeStatus.ALREADY_UP_TO_DATE,
                        SeedUpdateOutcomeStatus.BLOCKED_BY_OFFICIAL_SYNC,
                        SeedUpdateOutcomeStatus.BLOCKED_INCONSISTENT_VERSION -> 0
                    },
                    appliedSeedVersion = currentVersion,
                    appliedSeedHash = "fake_hash_$currentVersion",
                    currentSeedVersion = currentVersion,
                    currentSeedHash = "fake_hash_$currentVersion",
                    message = "Outcome simulado: $outcomeStatus"
                )
            )
        }
    }

    private class FakeNetworkConnectivityObserver(
        initialState: DeviceConnectivityState = DeviceConnectivityState.Offline
    ) : NetworkConnectivityObserver {
        private val stateFlow = MutableStateFlow(initialState)
        override val connectivityState: StateFlow<DeviceConnectivityState> = stateFlow.asStateFlow()

        fun setState(newState: DeviceConnectivityState) {
            stateFlow.value = newState
        }
    }
}
