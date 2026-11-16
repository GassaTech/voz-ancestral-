package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.EncyclopediaDao
import com.example.data.local.database.ParintintinDatabase
import com.example.data.local.entity.CulturalArticleEntity
import com.example.data.remote.api.ParintintinApiService
import com.example.data.remote.datasource.RemoteArchiveDataSource
import com.example.data.remote.dto.ArchiveManifestDto
import com.example.data.remote.dto.CulturalArticleDto
import com.example.data.remote.dto.DictionaryEntryDto
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfflineFirstEncyclopediaRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: ParintintinDatabase
    private lateinit var spyDao: InterceptingEncyclopediaDao
    private lateinit var fakeApiService: FakeEncyclopediaApiService
    private lateinit var repository: OfflineFirstEncyclopediaRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ParintintinDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        spyDao = InterceptingEncyclopediaDao(database.encyclopediaDao())
        fakeApiService = FakeEncyclopediaApiService()

        repository = OfflineFirstEncyclopediaRepository(
            database = database,
            encyclopediaDao = spyDao,
            remoteDataSource = RemoteArchiveDataSource(fakeApiService),
            dispatchers = IoDispatcherProvider
        )
    }

    @After
    fun tearDown() {
        if (database.isOpen) {
            database.close()
        }
    }

    @Test
    fun `syncFromRemote atualiza artigos preservando favoritos existentes`() = runBlocking {
        database.encyclopediaDao().upsertAll(
            listOf(
                sampleEntity(id = "art_1", title = "Território antigo", isBookmarked = true),
                sampleEntity(id = "art_2", title = "Canto antigo", isBookmarked = false)
            )
        )
        fakeApiService.articlesResult = Result.success(
            listOf(
                sampleDto(id = "art_1", title = "Território atualizado", subtitle = "Novo subtítulo"),
                sampleDto(id = "art_2", title = "Canto atualizado", subtitle = "Tradição oral")
            )
        )

        val result = repository.syncFromRemote()

        assertEquals(AppResult.Success(2), result)
        val articlesById = repository.observeArticles().first().associateBy { it.id }
        assertEquals("Território atualizado", articlesById.getValue("art_1").title)
        assertTrue(articlesById.getValue("art_1").isBookmarked)
        assertFalse(articlesById.getValue("art_2").isBookmarked)
    }

    @Test
    fun `syncFromRemote preserva artigos nao favoritados como falso`() = runBlocking {
        database.encyclopediaDao().upsertAll(
            listOf(
                sampleEntity(id = "art_1", title = "Origem", isBookmarked = false),
                sampleEntity(id = "art_2", title = "Rios", isBookmarked = false)
            )
        )
        fakeApiService.articlesResult = Result.success(
            listOf(
                sampleDto(id = "art_1", title = "Origem", subtitle = "Subtítulo 1"),
                sampleDto(id = "art_2", title = "Rios", subtitle = "Subtítulo 2"),
                sampleDto(id = "art_3", title = "Festas", subtitle = "Subtítulo 3")
            )
        )

        val result = repository.syncFromRemote()

        assertEquals(AppResult.Success(3), result)
        val articles = repository.observeArticles().first()
        assertEquals(3, articles.size)
        assertTrue(articles.none { it.isBookmarked })
        assertEquals(0, repository.observeBookmarkedCount().first())
    }

    @Test
    fun `syncFromRemote diante de falha remota preserva artigos e favoritos locais`() = runBlocking {
        database.encyclopediaDao().upsertAll(
            listOf(
                sampleEntity(id = "art_1", title = "Memória oral", isBookmarked = true),
                sampleEntity(id = "art_2", title = "Botânica", isBookmarked = false)
            )
        )
        fakeApiService.articlesResult = Result.failure(IOException("Timeout remoto"))

        val result = repository.syncFromRemote()

        assertTrue(result is AppResult.Error)
        assertEquals(0, spyDao.upsertCallCount)
        val articlesById = repository.observeArticles().first().associateBy { it.id }
        assertEquals(2, articlesById.size)
        assertEquals("Memória oral", articlesById.getValue("art_1").title)
        assertTrue(articlesById.getValue("art_1").isBookmarked)
        assertFalse(articlesById.getValue("art_2").isBookmarked)
    }

    @Test
    fun `syncFromRemote com resposta vazia retorna zero sem excluir artigos locais e sem transacao`() = runBlocking {
        database.encyclopediaDao().upsertAll(
            listOf(
                sampleEntity(id = "art_1", title = "Cosmologia", isBookmarked = true)
            )
        )
        fakeApiService.articlesResult = Result.success(emptyList())

        val result = repository.syncFromRemote()

        assertEquals(AppResult.Success(0), result)
        assertEquals(0, spyDao.getBookmarkedIdsCallCount)
        assertEquals(0, spyDao.upsertCallCount)
        val articles = repository.observeArticles().first()
        assertEquals(1, articles.size)
        assertTrue(articles.single().isBookmarked)
    }

    @Test
    fun `syncFromRemote atualiza corretamente campos e insere novos artigos`() = runBlocking {
        database.encyclopediaDao().upsertAll(
            listOf(
                sampleEntity(
                    id = "art_1",
                    title = "Título antigo",
                    subtitle = "Subtítulo antigo",
                    summary = "Resumo antigo",
                    isBookmarked = true,
                    updatedAt = 100L
                )
            )
        )
        fakeApiService.articlesResult = Result.success(
            listOf(
                sampleDto(
                    id = "art_1",
                    title = "Título revisado",
                    subtitle = "Subtítulo revisado",
                    summary = "Resumo completo atualizado",
                    bodyParagraphs = listOf("Parágrafo 1 revisado", "Parágrafo 2 revisado"),
                    readingTimeMinutes = 7,
                    sourceReference = "Acervo Kagwahiva 2026",
                    updatedAt = 3_000L
                ),
                sampleDto(
                    id = "art_2",
                    title = "Pintura corporal",
                    subtitle = "Grafismos tradicionais",
                    updatedAt = 3_100L
                )
            )
        )

        val result = repository.syncFromRemote()

        assertEquals(AppResult.Success(2), result)
        val updated = repository.observeArticleById("art_1").first()
        assertNotNull(updated)
        assertEquals("Título revisado", updated?.title)
        assertEquals("Subtítulo revisado", updated?.subtitle)
        assertEquals("Resumo completo atualizado", updated?.summary)
        assertEquals(listOf("Parágrafo 1 revisado", "Parágrafo 2 revisado"), updated?.bodyParagraphs)
        assertEquals(7, updated?.readingTimeMinutes)
        assertEquals("Acervo Kagwahiva 2026", updated?.sourceReference)
        assertEquals(3_000L, updated?.updatedAtEpochMillis)
        assertTrue(updated?.isBookmarked == true)

        val inserted = repository.observeArticleById("art_2").first()
        assertNotNull(inserted)
        assertEquals("Pintura corporal", inserted?.title)
        assertFalse(inserted?.isBookmarked == true)
    }

    @Test
    fun `concorrencia entre syncFromRemote e toggleBookmark preserva alteracao do usuario`() = runBlocking {
        withTimeout(5_000L) {
            database.encyclopediaDao().upsertAll(
                listOf(
                    sampleEntity(id = "art_1", title = "Território antigo", isBookmarked = false),
                    sampleEntity(id = "art_2", title = "Canto antigo", isBookmarked = true)
                )
            )
            fakeApiService.articlesResult = Result.success(
                listOf(
                    sampleDto(id = "art_1", title = "Território atualizado", subtitle = "Sub 1"),
                    sampleDto(id = "art_2", title = "Canto atualizado", subtitle = "Sub 2")
                )
            )

            val beforeUpsertReached = CompletableDeferred<Unit>()
            val allowUpsertToFinish = CompletableDeferred<Unit>()
            val toggleMarkEnteredDao = CompletableDeferred<Unit>()
            val toggleUnmarkEnteredDao = CompletableDeferred<Unit>()

            val markCompletedDuringTransaction = AtomicBoolean(false)
            val unmarkCompletedDuringTransaction = AtomicBoolean(false)
            var getBookmarksInTransaction = false
            var upsertInTransaction = false
            var transactionCommitted = false

            spyDao.onAfterGetBookmarkedIds = {
                getBookmarksInTransaction = database.inTransaction()
            }
            spyDao.onBeforeUpsert = {
                upsertInTransaction = database.inTransaction()
                beforeUpsertReached.complete(Unit)
                allowUpsertToFinish.await()
            }
            spyDao.onBeforeToggleBookmark = { articleId ->
                when (articleId) {
                    "art_1" -> toggleMarkEnteredDao.complete(Unit)
                    "art_2" -> toggleUnmarkEnteredDao.complete(Unit)
                }
            }
            spyDao.onAfterToggleBookmark = { articleId ->
                if (!transactionCommitted) {
                    when (articleId) {
                        "art_1" -> markCompletedDuringTransaction.set(true)
                        "art_2" -> unmarkCompletedDuringTransaction.set(true)
                    }
                }
            }

            val syncDeferred = async(Dispatchers.IO) {
                repository.syncFromRemote().also {
                    transactionCommitted = true
                }
            }

            beforeUpsertReached.await()

            val toggleMarkDeferred = async(Dispatchers.IO) { repository.toggleBookmark("art_1") }
            val toggleUnmarkDeferred = async(Dispatchers.IO) { repository.toggleBookmark("art_2") }

            toggleMarkEnteredDao.await()
            toggleUnmarkEnteredDao.await()

            allowUpsertToFinish.complete(Unit)

            val syncResult = syncDeferred.await()
            toggleMarkDeferred.await()
            toggleUnmarkDeferred.await()

            assertEquals(AppResult.Success(2), syncResult)
            assertTrue(getBookmarksInTransaction)
            assertTrue(upsertInTransaction)
            assertFalse(markCompletedDuringTransaction.get())
            assertFalse(unmarkCompletedDuringTransaction.get())

            val articlesById = repository.observeArticles().first().associateBy { it.id }
            assertEquals("Território atualizado", articlesById.getValue("art_1").title)
            assertTrue(articlesById.getValue("art_1").isBookmarked)
            assertEquals("Canto atualizado", articlesById.getValue("art_2").title)
            assertFalse(articlesById.getValue("art_2").isBookmarked)
        }
    }

    private fun sampleEntity(
        id: String,
        title: String,
        subtitle: String = "Subtítulo $id",
        summary: String = "Resumo $id",
        isBookmarked: Boolean,
        updatedAt: Long = 1_000L
    ): CulturalArticleEntity = CulturalArticleEntity(
        id = id,
        title = title,
        subtitle = subtitle,
        summary = summary,
        bodyParagraphs = listOf("Parágrafo inicial $id"),
        topic = "HISTORY",
        readingTimeMinutes = 4,
        sourceReference = "Acervo Local",
        isBookmarked = isBookmarked,
        updatedAtEpochMillis = updatedAt
    )

    private fun sampleDto(
        id: String,
        title: String,
        subtitle: String,
        summary: String = "Resumo atualizado $id",
        bodyParagraphs: List<String> = listOf("Parágrafo atualizado $id"),
        readingTimeMinutes: Int = 5,
        sourceReference: String = "Acervo Remoto",
        updatedAt: Long = 2_000L
    ): CulturalArticleDto = CulturalArticleDto(
        id = id,
        title = title,
        subtitle = subtitle,
        summary = summary,
        bodyParagraphs = bodyParagraphs,
        topic = "HISTORY",
        readingTimeMinutes = readingTimeMinutes,
        sourceReference = sourceReference,
        updatedAtEpochMillis = updatedAt
    )

    private class InterceptingEncyclopediaDao(
        private val delegate: EncyclopediaDao
    ) : EncyclopediaDao by delegate {

        var getBookmarkedIdsCallCount: Int = 0
        var upsertCallCount: Int = 0
        var onAfterGetBookmarkedIds: (() -> Unit)? = null
        var onBeforeUpsert: (suspend () -> Unit)? = null
        var onBeforeToggleBookmark: ((String) -> Unit)? = null
        var onAfterToggleBookmark: ((String) -> Unit)? = null

        override suspend fun getBookmarkedIds(): List<String> {
            getBookmarkedIdsCallCount += 1
            return delegate.getBookmarkedIds().also {
                onAfterGetBookmarkedIds?.invoke()
            }
        }

        override suspend fun upsertAll(articles: List<CulturalArticleEntity>) {
            upsertCallCount += 1
            onBeforeUpsert?.invoke()
            delegate.upsertAll(articles)
        }

        override suspend fun toggleBookmark(articleId: String) {
            onBeforeToggleBookmark?.invoke(articleId)
            delegate.toggleBookmark(articleId)
            onAfterToggleBookmark?.invoke(articleId)
        }
    }

    private class FakeEncyclopediaApiService : ParintintinApiService {
        var articlesResult: Result<List<CulturalArticleDto>> = Result.success(emptyList())

        override suspend fun getArchiveManifest(): ArchiveManifestDto =
            ArchiveManifestDto(version = "1", entriesCount = 0, articlesCount = 0)

        override suspend fun getDictionaryEntries(): List<DictionaryEntryDto> =
            emptyList()

        override suspend fun getCulturalArticles(): List<CulturalArticleDto> =
            articlesResult.getOrThrow()
    }

    private object IoDispatcherProvider : DispatcherProvider {
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val default: CoroutineDispatcher = Dispatchers.Default
        override val main: CoroutineDispatcher = Dispatchers.Main
    }
}
