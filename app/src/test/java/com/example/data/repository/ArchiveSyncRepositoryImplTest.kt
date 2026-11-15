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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveSyncRepositoryImplTest {

    @Test
    fun `1 - cancelamento durante a sincronizacao restaura estado anterior e relanca CancellationException`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val syncDao = FakeSyncMetadataDao()
        val repository = ArchiveSyncRepositoryImpl(
            dictionaryRepository = FakeDictionaryRepository(
                throwOnSync = CancellationException("Cancelado durante sincronização")
            ),
            encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(0)),
            syncMetadataDao = syncDao,
            dispatchers = TestDispatcherProvider(dispatcher)
        )

        try {
            repository.syncArchive()
            fail("Era esperado relançar CancellationException")
        } catch (_: CancellationException) {
            assertEquals(SyncState.Idle, repository.syncState.value)
        }
    }

    @Test
    fun `2 - cancelamento durante o registro de OfflineFallback restaura estado anterior e relanca CancellationException`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val syncDao = FakeSyncMetadataDao(throwOnUpsert = CancellationException("Cancelado ao gravar fallback"))
        val repository = ArchiveSyncRepositoryImpl(
            dictionaryRepository = FakeDictionaryRepository(
                throwOnSync = IllegalStateException("Erro inesperado de rede")
            ),
            encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(0)),
            syncMetadataDao = syncDao,
            dispatchers = TestDispatcherProvider(dispatcher)
        )

        try {
            repository.syncArchive()
            fail("Era esperado relançar CancellationException")
        } catch (_: CancellationException) {
            assertEquals(SyncState.Idle, repository.syncState.value)
        }
    }

    @Test
    fun `3 - falha inesperada durante a sincronizacao registra OfflineFallback e preserva dados locais`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val syncDao = FakeSyncMetadataDao()
        val repository = ArchiveSyncRepositoryImpl(
            dictionaryRepository = FakeDictionaryRepository(
                throwOnSync = IllegalStateException("Falha inesperada ao ler resposta")
            ),
            encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(2)),
            syncMetadataDao = syncDao,
            dispatchers = TestDispatcherProvider(dispatcher)
        )

        val result = repository.syncArchive()

        assertTrue(result is AppResult.Error)
        val state = repository.syncState.value as SyncState.OfflineFallback
        assertEquals("Falha inesperada ao ler resposta", state.reason)
        assertEquals(ArchiveSyncRepositoryImpl.STATUS_OFFLINE_FALLBACK, syncDao.lastSaved?.lastStatus)
        assertEquals(
            ArchiveSyncRepositoryImpl.MANIFEST_VERSION_UNAVAILABLE,
            syncDao.lastSaved?.remoteVersion
        )
    }

    @Test
    fun `4 - sucesso das duas sincronizacoes atualiza estado para Synced`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val syncDao = FakeSyncMetadataDao()
        val repository = ArchiveSyncRepositoryImpl(
            dictionaryRepository = FakeDictionaryRepository(syncResult = AppResult.Success(12)),
            encyclopediaRepository = FakeEncyclopediaRepository(syncResult = AppResult.Success(4)),
            syncMetadataDao = syncDao,
            dispatchers = TestDispatcherProvider(dispatcher)
        )

        assertEquals(SyncState.Idle, repository.syncState.value)

        val result = repository.syncArchive()

        assertTrue(result is AppResult.Success)
        val state = repository.syncState.value as SyncState.Synced
        assertEquals(12, state.entriesCount)
        assertEquals(4, state.articlesCount)
        assertEquals(ArchiveSyncRepositoryImpl.STATUS_SYNCED, syncDao.lastSaved?.lastStatus)
        assertEquals(
            ArchiveSyncRepositoryImpl.MANIFEST_VERSION_UNAVAILABLE,
            syncDao.lastSaved?.remoteVersion
        )
    }

    @Test
    fun `5 - falha de apenas um dos repositorios atualiza estado para OfflineFallback`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val syncDao = FakeSyncMetadataDao()
        val repository = ArchiveSyncRepositoryImpl(
            dictionaryRepository = FakeDictionaryRepository(syncResult = AppResult.Success(5)),
            encyclopediaRepository = FakeEncyclopediaRepository(
                syncResult = AppResult.Error("Falha ao consultar API da enciclopédia.")
            ),
            syncMetadataDao = syncDao,
            dispatchers = TestDispatcherProvider(dispatcher)
        )

        val result = repository.syncArchive()

        assertTrue(result is AppResult.Error)
        val state = repository.syncState.value as SyncState.OfflineFallback
        assertEquals("Falha ao consultar API da enciclopédia.", state.reason)
        assertEquals(ArchiveSyncRepositoryImpl.STATUS_OFFLINE_FALLBACK, syncDao.lastSaved?.lastStatus)
    }

    @Test
    fun `observeArchiveStats combina contagens dos repositorios de dicionario e enciclopedia`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val repository = ArchiveSyncRepositoryImpl(
            dictionaryRepository = FakeDictionaryRepository(total = 10, favorites = 3),
            encyclopediaRepository = FakeEncyclopediaRepository(total = 5, bookmarked = 2),
            syncMetadataDao = FakeSyncMetadataDao(),
            dispatchers = TestDispatcherProvider(dispatcher)
        )

        val stats = repository.observeArchiveStats().first()

        assertEquals(10, stats.totalEntries)
        assertEquals(3, stats.favoriteEntries)
        assertEquals(5, stats.totalArticles)
        assertEquals(2, stats.bookmarkedArticles)
    }

    private class TestDispatcherProvider(
        dispatcher: CoroutineDispatcher
    ) : DispatcherProvider {
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
    }

    private class FakeSyncMetadataDao(
        private val throwOnUpsert: Throwable? = null
    ) : SyncMetadataDao {
        private val state = MutableStateFlow<SyncMetadataEntity?>(null)
        var lastSaved: SyncMetadataEntity? = null

        override fun observeMetadata(id: String): Flow<SyncMetadataEntity?> = state

        override suspend fun upsert(metadata: SyncMetadataEntity) {
            throwOnUpsert?.let { throw it }
            lastSaved = metadata
            state.value = metadata
        }
    }

    private class FakeDictionaryRepository(
        private val total: Int = 0,
        private val favorites: Int = 0,
        private val syncResult: AppResult<Int> = AppResult.Success(0),
        private val throwOnSync: Throwable? = null
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
