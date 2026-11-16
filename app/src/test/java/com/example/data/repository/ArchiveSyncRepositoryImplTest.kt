package com.example.data.repository

import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.SyncMetadataDao
import com.example.data.local.entity.SyncMetadataEntity
import com.example.domain.model.CulturalArticle
import com.example.domain.model.CulturalTopic
import com.example.domain.model.DictionaryEntry
import com.example.domain.model.SemanticCategory
import com.example.domain.model.SyncState
import com.example.domain.repository.DictionaryRepository
import com.example.domain.repository.EncyclopediaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

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
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                throwOnSync = CancellationException("Cancelado durante sincronização")
            ),
            encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(0)),
            syncDao = syncDao
        )
        advanceUntilIdle()

        try {
            repository.syncArchive()
            fail("Era esperado relançar CancellationException")
        } catch (_: CancellationException) {
            advanceUntilIdle()
            assertEquals(SyncState.Idle, repository.syncState.value)
        }
    }

    @Test
    fun `2 - cancelamento durante o registro de OfflineFallback restaura estado anterior e relanca CancellationException`() = runTest {
        val syncDao = FakeSyncMetadataDao(
            throwOnRecordFailure = CancellationException("Cancelado ao gravar fallback")
        )
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                throwOnSync = IllegalStateException("Erro inesperado de rede")
            ),
            encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(0)),
            syncDao = syncDao
        )
        advanceUntilIdle()

        try {
            repository.syncArchive()
            fail("Era esperado relançar CancellationException")
        } catch (_: CancellationException) {
            advanceUntilIdle()
            assertEquals(SyncState.Idle, repository.syncState.value)
        }
    }

    @Test
    fun `3 - falha inesperada durante a sincronizacao registra OfflineFallback e preserva dados locais`() = runTest {
        val syncDao = FakeSyncMetadataDao()
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                throwOnSync = IllegalStateException("Falha inesperada ao ler resposta")
            ),
            encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(2)),
            syncDao = syncDao
        )
        advanceUntilIdle()

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Error)
        val state = repository.syncState.value as SyncState.OfflineFallback
        assertEquals("Falha inesperada ao ler resposta", state.reason)
        assertEquals(SyncMetadataEntity.STATUS_OFFLINE_FALLBACK, syncDao.lastSaved?.lastAttemptStatus)
        assertEquals("Falha inesperada ao ler resposta", syncDao.lastSaved?.lastFailureReason)
        assertNull(syncDao.lastSaved?.lastSuccessfulSyncEpochMillis)
    }

    @Test
    fun `4 - sucesso das duas sincronizacoes atualiza estado para Synced`() = runTest {
        val syncDao = FakeSyncMetadataDao()
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(syncResult = AppResult.Success(12)),
            encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(4)),
            syncDao = syncDao
        )
        advanceUntilIdle()

        assertEquals(SyncState.Idle, repository.syncState.value)

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Success)
        val state = repository.syncState.value as SyncState.Synced
        assertEquals(12, state.entriesCount)
        assertEquals(4, state.articlesCount)
        assertEquals(SyncMetadataEntity.STATUS_SYNCED, syncDao.lastSaved?.lastAttemptStatus)
        assertEquals(12, syncDao.lastSaved?.lastSuccessfulEntriesCount)
        assertEquals(4, syncDao.lastSaved?.lastSuccessfulArticlesCount)
        assertEquals(
            ArchiveSyncRepositoryImpl.MANIFEST_VERSION_UNAVAILABLE,
            syncDao.lastSaved?.lastSuccessfulRemoteVersion
        )
    }

    @Test
    fun `5 - falha de apenas um dos repositorios atualiza estado para OfflineFallback`() = runTest {
        val syncDao = FakeSyncMetadataDao()
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(syncResult = AppResult.Success(5)),
            encyclopediaRepository = FakeEncyclopediaRepository(
                syncResult = AppResult.Error("Falha ao consultar API da enciclopédia.")
            ),
            syncDao = syncDao
        )
        advanceUntilIdle()

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Error)
        val state = repository.syncState.value as SyncState.OfflineFallback
        assertEquals("Falha ao consultar API da enciclopédia.", state.reason)
        assertEquals(SyncMetadataEntity.STATUS_OFFLINE_FALLBACK, syncDao.lastSaved?.lastAttemptStatus)
    }

    @Test
    fun `6 - falha das duas fontes combina motivos distintos em OfflineFallback`() = runTest {
        val syncDao = FakeSyncMetadataDao()
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                syncResult = AppResult.Error("Erro no dicionário")
            ),
            encyclopediaRepository = FakeEncyclopediaRepository(
                syncResult = AppResult.Error("Erro na enciclopédia")
            ),
            syncDao = syncDao
        )
        advanceUntilIdle()

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Error)
        val state = repository.syncState.value as SyncState.OfflineFallback
        assertEquals("Erro no dicionário | Erro na enciclopédia", state.reason)
        assertEquals(SyncMetadataEntity.STATUS_OFFLINE_FALLBACK, syncDao.lastSaved?.lastAttemptStatus)
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
    fun `recuperacao inicial com banco vazio permanece em Idle apos emissao do Flow`() = runTest {
        val syncDao = FakeSyncMetadataDao(initialEntity = null)
        val repository = createRepository(syncDao = syncDao)

        advanceUntilIdle()

        assertTrue(syncDao.observedCount > 0)
        assertEquals(SyncState.Idle, repository.syncState.value)
    }

    @Test
    fun `recuperacao inicial restaura SyncState Synced persistido no Room incluindo contadores nulos`() = runTest {
        val persistedSynced = SyncMetadataEntity(
            lastAttemptEpochMillis = 1_700_000_010_000L,
            lastAttemptStatus = SyncMetadataEntity.STATUS_SYNCED,
            lastFailureReason = null,
            lastSuccessfulSyncEpochMillis = 1_700_000_010_000L,
            lastSuccessfulEntriesCount = null,
            lastSuccessfulArticlesCount = null,
            lastSuccessfulRemoteVersion = ArchiveSyncRepositoryImpl.MANIFEST_VERSION_UNAVAILABLE
        )
        val syncDao = FakeSyncMetadataDao(initialEntity = persistedSynced)
        val repository = createRepository(syncDao = syncDao)

        val recovered = repository.syncState.first { it !is SyncState.Idle }

        val state = recovered as SyncState.Synced
        assertEquals(1_700_000_010_000L, state.lastSyncedEpochMillis)
        assertNull(state.entriesCount)
        assertNull(state.articlesCount)
    }

    @Test
    fun `recuperacao inicial restaura SyncState OfflineFallback persistido no Room`() = runTest {
        val persistedFallback = SyncMetadataEntity(
            lastAttemptEpochMillis = 1_700_000_020_000L,
            lastAttemptStatus = SyncMetadataEntity.STATUS_OFFLINE_FALLBACK,
            lastFailureReason = "Sem conectividade na última tentativa",
            lastSuccessfulSyncEpochMillis = 1_700_000_000_000L,
            lastSuccessfulEntriesCount = 20,
            lastSuccessfulArticlesCount = 8,
            lastSuccessfulRemoteVersion = ArchiveSyncRepositoryImpl.MANIFEST_VERSION_UNAVAILABLE
        )
        val syncDao = FakeSyncMetadataDao(initialEntity = persistedFallback)
        val repository = createRepository(syncDao = syncDao)

        val recovered = repository.syncState.first { it !is SyncState.Idle }

        val state = recovered as SyncState.OfflineFallback
        assertEquals("Sem conectividade na última tentativa", state.reason)
        assertEquals(1_700_000_020_000L, state.lastAttemptEpochMillis)
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
        val repository = createRepository(
            dictionaryRepository = FakeDictionaryRepository(
                syncResult = AppResult.Error("Tempo limite excedido")
            ),
            encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(9)),
            syncDao = syncDao
        )

        val recovered = repository.syncState.first { it is SyncState.Synced } as SyncState.Synced
        assertEquals(30, recovered.entriesCount)

        val result = repository.syncArchive()
        advanceUntilIdle()

        assertTrue(result is AppResult.Error)
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
            dictionaryRepository = FakeDictionaryRepository(syncResult = AppResult.Success(18)),
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
            dictionaryRepository = FakeDictionaryRepository(syncResult = AppResult.Success(25)),
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
            dictionaryRepository = FakeDictionaryRepository(syncResult = AppResult.Success(2)),
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
        val firstGate = CompletableDeferred<AppResult<Int>>()
        val secondGate = CompletableDeferred<AppResult<Int>>()
        var syncCallCount = 0
        val syncDao = FakeSyncMetadataDao(deferFlowEmission = true)

        val dictionaryRepository = FakeDictionaryRepository(
            customSync = {
                syncCallCount += 1
                if (syncCallCount == 1) firstGate.await() else secondGate.await()
            }
        )
        val repository = createRepository(
            dictionaryRepository = dictionaryRepository,
            encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(5)),
            syncDao = syncDao
        )
        advanceUntilIdle()

        val firstJob = async { repository.syncArchive() }
        advanceUntilIdle()
        assertEquals(SyncState.Syncing, repository.syncState.value)

        firstGate.complete(AppResult.Success(15))
        assertTrue(firstJob.await() is AppResult.Success)
        advanceUntilIdle()

        // Mesmo antes de o Flow do Room emitir, PendingRoomEmission já expõe Synced.
        val resolvedBeforeRoomEmit = repository.syncState.value as SyncState.Synced
        assertEquals(15, resolvedBeforeRoomEmit.entriesCount)

        // Inicia segunda sincronização antes da emissão atrasada do Room da primeira tentativa.
        val secondJob = async { repository.syncArchive() }
        advanceUntilIdle()
        assertEquals(SyncState.Syncing, repository.syncState.value)

        // Emissão antiga do Room da primeira tentativa não pode sobrescrever Syncing ativo.
        syncDao.emitLatestToObservers()
        advanceUntilIdle()
        assertEquals(SyncState.Syncing, repository.syncState.value)

        secondGate.complete(AppResult.Success(22))
        assertTrue(secondJob.await() is AppResult.Success)
        syncDao.emitLatestToObservers()
        advanceUntilIdle()

        val finalState = repository.syncState.value as SyncState.Synced
        assertEquals(22, finalState.entriesCount)
        assertEquals(5, finalState.articlesCount)
    }

    private fun TestScope.createRepository(
        dictionaryRepository: DictionaryRepository = FakeDictionaryRepository(),
        encyclopediaRepository: EncyclopediaRepository = FakeEncyclopediaRepository(),
        syncDao: SyncMetadataDao = FakeSyncMetadataDao()
    ): ArchiveSyncRepositoryImpl {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val applicationScope = CoroutineScope(SupervisorJob() + testDispatcher)
        createdScopes += applicationScope
        return ArchiveSyncRepositoryImpl(
            dictionaryRepository = dictionaryRepository,
            encyclopediaRepository = encyclopediaRepository,
            syncMetadataDao = syncDao,
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
        private val deferFlowEmission: Boolean = false
    ) : SyncMetadataDao {
        private val state = MutableStateFlow(initialEntity)
        var lastSaved: SyncMetadataEntity? = initialEntity
            private set
        var observedCount: Int = 0
            private set

        override fun observeMetadata(id: String): Flow<SyncMetadataEntity?> {
            observedCount += 1
            return state
        }

        override suspend fun getMetadataOnce(id: String): SyncMetadataEntity? {
            throwOnRead?.let { throw it }
            return lastSaved
        }

        override suspend fun upsert(metadata: SyncMetadataEntity) {
            throwOnUpsert?.let { throw it }
            lastSaved = metadata
            if (!deferFlowEmission) {
                state.value = metadata
            }
        }

        override suspend fun recordAttemptFailure(
            attemptEpochMillis: Long,
            failureReason: String?
        ) {
            throwOnRecordFailure?.let { throw it }
            super.recordAttemptFailure(attemptEpochMillis, failureReason)
        }

        fun emitLatestToObservers() {
            state.value = lastSaved
        }
    }

    private class FakeDictionaryRepository(
        private val total: Int = 0,
        private val favorites: Int = 0,
        private val syncResult: AppResult<Int> = AppResult.Success(0),
        private val throwOnSync: Throwable? = null,
        private val customSync: (suspend () -> AppResult<Int>)? = null
    ) : DictionaryRepository {
        override fun observeEntries(
            query: String,
            category: SemanticCategory?,
            onlyFavorites: Boolean
        ): Flow<List<DictionaryEntry>> = flowOf(emptyList())

        override fun observeEntryById(entryId: String): Flow<DictionaryEntry?> = flowOf(null)
        override fun observeTotalCount(): Flow<Int> = flowOf(total)
        override fun observeFavoriteCount(): Flow<Int> = flowOf(favorites)
        override suspend fun toggleFavorite(entryId: String) = Unit
        override suspend fun syncFromRemote(): AppResult<Int> {
            customSync?.let { return it() }
            throwOnSync?.let { throw it }
            return syncResult
        }
    }

    private class FakeEncyclopediaRepository(
        private val total: Int = 0,
        private val bookmarked: Int = 0,
        private val syncResult: AppResult<Int> = AppResult.Success(0)
    ) : EncyclopediaRepository {
        override fun observeArticles(
            query: String,
            topic: CulturalTopic?,
            onlyBookmarked: Boolean
        ): Flow<List<CulturalArticle>> = flowOf(emptyList())

        override fun observeArticleById(articleId: String): Flow<CulturalArticle?> = flowOf(null)
        override fun observeTotalCount(): Flow<Int> = flowOf(total)
        override fun observeBookmarkedCount(): Flow<Int> = flowOf(bookmarked)
        override suspend fun toggleBookmark(articleId: String) = Unit
        override suspend fun syncFromRemote(): AppResult<Int> = syncResult
    }
}
