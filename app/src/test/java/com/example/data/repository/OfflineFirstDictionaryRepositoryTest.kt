package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.DictionaryDao
import com.example.data.local.database.ParintintinDatabase
import com.example.data.local.entity.DictionaryEntryEntity
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
class OfflineFirstDictionaryRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: ParintintinDatabase
    private lateinit var spyDao: InterceptingDictionaryDao
    private lateinit var fakeApiService: FakeDictionaryApiService
    private lateinit var repository: OfflineFirstDictionaryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ParintintinDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        spyDao = InterceptingDictionaryDao(database.dictionaryDao())
        fakeApiService = FakeDictionaryApiService()

        repository = OfflineFirstDictionaryRepository(
            database = database,
            dictionaryDao = spyDao,
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
    fun `syncFromRemote atualiza dados remotos preservando favoritos existentes`() = runBlocking {
        database.dictionaryDao().upsertAll(
            listOf(
                sampleEntity(id = "dict_1", word = "Ywy antigo", isFavorite = true),
                sampleEntity(id = "dict_2", word = "Kaa antigo", isFavorite = false)
            )
        )
        fakeApiService.entriesResult = Result.success(
            listOf(
                sampleDto(id = "dict_1", word = "Ywy atualizado", translation = "Terra sagrada"),
                sampleDto(id = "dict_2", word = "Kaa atualizado", translation = "Mata")
            )
        )

        val result = repository.syncFromRemote()

        assertEquals(AppResult.Success(2), result)
        val entriesById = repository.observeEntries().first().associateBy { it.id }
        assertEquals("Ywy atualizado", entriesById.getValue("dict_1").parintintinWord)
        assertTrue(entriesById.getValue("dict_1").isFavorite)
        assertFalse(entriesById.getValue("dict_2").isFavorite)
    }

    @Test
    fun `syncFromRemote preserva registros nao favoritados como falso`() = runBlocking {
        database.dictionaryDao().upsertAll(
            listOf(
                sampleEntity(id = "dict_1", word = "Pira", isFavorite = false),
                sampleEntity(id = "dict_2", word = "Jasy", isFavorite = false)
            )
        )
        fakeApiService.entriesResult = Result.success(
            listOf(
                sampleDto(id = "dict_1", word = "Pira", translation = "Peixe"),
                sampleDto(id = "dict_2", word = "Jasy", translation = "Lua"),
                sampleDto(id = "dict_3", word = "Kwarahy", translation = "Sol")
            )
        )

        val result = repository.syncFromRemote()

        assertEquals(AppResult.Success(3), result)
        val entries = repository.observeEntries().first()
        assertEquals(3, entries.size)
        assertTrue(entries.none { it.isFavorite })
        assertEquals(0, repository.observeFavoriteCount().first())
    }

    @Test
    fun `syncFromRemote diante de falha remota preserva dados e favoritos locais`() = runBlocking {
        database.dictionaryDao().upsertAll(
            listOf(
                sampleEntity(id = "dict_1", word = "Iga", isFavorite = true),
                sampleEntity(id = "dict_2", word = "Tata", isFavorite = false)
            )
        )
        fakeApiService.entriesResult = Result.failure(IOException("Sem conexão"))

        val result = repository.syncFromRemote()

        assertTrue(result is AppResult.Error)
        assertEquals(0, spyDao.upsertCallCount)
        val entriesById = repository.observeEntries().first().associateBy { it.id }
        assertEquals(2, entriesById.size)
        assertEquals("Iga", entriesById.getValue("dict_1").parintintinWord)
        assertTrue(entriesById.getValue("dict_1").isFavorite)
        assertFalse(entriesById.getValue("dict_2").isFavorite)
    }

    @Test
    fun `syncFromRemote com resposta vazia retorna zero sem excluir dados locais e sem transacao`() = runBlocking {
        database.dictionaryDao().upsertAll(
            listOf(
                sampleEntity(id = "dict_1", word = "Kurumi", isFavorite = true)
            )
        )
        fakeApiService.entriesResult = Result.success(emptyList())

        val result = repository.syncFromRemote()

        assertEquals(AppResult.Success(0), result)
        assertEquals(0, spyDao.getFavoriteIdsCallCount)
        assertEquals(0, spyDao.upsertCallCount)
        val entries = repository.observeEntries().first()
        assertEquals(1, entries.size)
        assertTrue(entries.single().isFavorite)
    }

    @Test
    fun `syncFromRemote atualiza corretamente campos e insere novos verbetes`() = runBlocking {
        database.dictionaryDao().upsertAll(
            listOf(
                sampleEntity(
                    id = "dict_1",
                    word = "Tupã antigo",
                    translation = "Tradução antiga",
                    culturalNote = "Nota antiga",
                    isFavorite = true,
                    updatedAt = 100L
                )
            )
        )
        fakeApiService.entriesResult = Result.success(
            listOf(
                sampleDto(
                    id = "dict_1",
                    word = "Tupã novo",
                    translation = "Trovão / força espiritual",
                    culturalNote = "Nota revisada pelos anciãos",
                    audioUrl = "https://archive.parintintin.org/audio/tupa.mp3",
                    updatedAt = 2_000L
                ),
                sampleDto(
                    id = "dict_2",
                    word = "Paraná",
                    translation = "Rio grande",
                    culturalNote = "Registro novo",
                    updatedAt = 2_100L
                )
            )
        )

        val result = repository.syncFromRemote()

        assertEquals(AppResult.Success(2), result)
        val updated = repository.observeEntryById("dict_1").first()
        assertNotNull(updated)
        assertEquals("Tupã novo", updated?.parintintinWord)
        assertEquals("Trovão / força espiritual", updated?.portugueseTranslation)
        assertEquals("Nota revisada pelos anciãos", updated?.culturalNote)
        assertEquals("https://archive.parintintin.org/audio/tupa.mp3", updated?.audioUrl)
        assertEquals(2_000L, updated?.updatedAtEpochMillis)
        assertTrue(updated?.isFavorite == true)

        val inserted = repository.observeEntryById("dict_2").first()
        assertNotNull(inserted)
        assertEquals("Paraná", inserted?.parintintinWord)
        assertFalse(inserted?.isFavorite == true)
    }

    @Test
    fun `concorrencia entre syncFromRemote e toggleFavorite preserva marcacao do usuario`() = runBlocking {
        withTimeout(5_000L) {
            database.dictionaryDao().upsertAll(
                listOf(
                    sampleEntity(id = "dict_1", word = "Ywy antigo", isFavorite = false),
                    sampleEntity(id = "dict_2", word = "Kaa antigo", isFavorite = true)
                )
            )
            fakeApiService.entriesResult = Result.success(
                listOf(
                    sampleDto(id = "dict_1", word = "Ywy atualizado", translation = "Terra"),
                    sampleDto(id = "dict_2", word = "Kaa atualizado", translation = "Floresta")
                )
            )

            val beforeUpsertReached = CompletableDeferred<Unit>()
            val allowUpsertToFinish = CompletableDeferred<Unit>()
            val toggleMarkEnteredDao = CompletableDeferred<Unit>()
            val toggleUnmarkEnteredDao = CompletableDeferred<Unit>()

            val markCompletedDuringTransaction = AtomicBoolean(false)
            val unmarkCompletedDuringTransaction = AtomicBoolean(false)
            var getFavoritesInTransaction = false
            var upsertInTransaction = false
            var transactionCommitted = false

            spyDao.onAfterGetFavoriteIds = {
                getFavoritesInTransaction = database.inTransaction()
            }
            spyDao.onBeforeUpsert = {
                upsertInTransaction = database.inTransaction()
                beforeUpsertReached.complete(Unit)
                allowUpsertToFinish.await()
            }
            spyDao.onBeforeToggleFavorite = { entryId ->
                when (entryId) {
                    "dict_1" -> toggleMarkEnteredDao.complete(Unit)
                    "dict_2" -> toggleUnmarkEnteredDao.complete(Unit)
                }
            }
            spyDao.onAfterToggleFavorite = { entryId ->
                if (!transactionCommitted) {
                    when (entryId) {
                        "dict_1" -> markCompletedDuringTransaction.set(true)
                        "dict_2" -> unmarkCompletedDuringTransaction.set(true)
                    }
                }
            }

            val syncDeferred = async(Dispatchers.IO) {
                repository.syncFromRemote().also {
                    transactionCommitted = true
                }
            }

            beforeUpsertReached.await()

            val toggleMarkDeferred = async(Dispatchers.IO) { repository.toggleFavorite("dict_1") }
            val toggleUnmarkDeferred = async(Dispatchers.IO) { repository.toggleFavorite("dict_2") }

            toggleMarkEnteredDao.await()
            toggleUnmarkEnteredDao.await()

            allowUpsertToFinish.complete(Unit)

            val syncResult = syncDeferred.await()
            toggleMarkDeferred.await()
            toggleUnmarkDeferred.await()

            assertEquals(AppResult.Success(2), syncResult)
            assertTrue(getFavoritesInTransaction)
            assertTrue(upsertInTransaction)
            assertFalse(markCompletedDuringTransaction.get())
            assertFalse(unmarkCompletedDuringTransaction.get())

            val entriesById = repository.observeEntries().first().associateBy { it.id }
            assertEquals("Ywy atualizado", entriesById.getValue("dict_1").parintintinWord)
            assertTrue(entriesById.getValue("dict_1").isFavorite)
            assertEquals("Kaa atualizado", entriesById.getValue("dict_2").parintintinWord)
            assertFalse(entriesById.getValue("dict_2").isFavorite)
        }
    }

    private fun sampleEntity(
        id: String,
        word: String,
        translation: String = "Tradução $id",
        culturalNote: String = "Nota $id",
        isFavorite: Boolean,
        updatedAt: Long = 1_000L
    ): DictionaryEntryEntity = DictionaryEntryEntity(
        id = id,
        parintintinWord = word,
        phoneticIpa = "/$id/",
        portugueseTranslation = translation,
        grammaticalClass = "NOUN",
        semanticCategory = "NATURE",
        exampleParintintin = "Exemplo $word",
        examplePortuguese = "Exemplo $translation",
        culturalNote = culturalNote,
        audioUrl = null,
        isFavorite = isFavorite,
        updatedAtEpochMillis = updatedAt
    )

    private fun sampleDto(
        id: String,
        word: String,
        translation: String,
        culturalNote: String = "Nota atualizada $id",
        audioUrl: String? = null,
        updatedAt: Long = 2_000L
    ): DictionaryEntryDto = DictionaryEntryDto(
        id = id,
        parintintinWord = word,
        phoneticIpa = "/$id/",
        portugueseTranslation = translation,
        grammaticalClass = "NOUN",
        semanticCategory = "NATURE",
        exampleParintintin = "Exemplo $word",
        examplePortuguese = "Exemplo $translation",
        culturalNote = culturalNote,
        audioUrl = audioUrl,
        updatedAtEpochMillis = updatedAt
    )

    private class InterceptingDictionaryDao(
        private val delegate: DictionaryDao
    ) : DictionaryDao by delegate {

        var getFavoriteIdsCallCount: Int = 0
        var upsertCallCount: Int = 0
        var onAfterGetFavoriteIds: (() -> Unit)? = null
        var onBeforeUpsert: (suspend () -> Unit)? = null
        var onBeforeToggleFavorite: ((String) -> Unit)? = null
        var onAfterToggleFavorite: ((String) -> Unit)? = null

        override suspend fun getFavoriteIds(): List<String> {
            getFavoriteIdsCallCount += 1
            return delegate.getFavoriteIds().also {
                onAfterGetFavoriteIds?.invoke()
            }
        }

        override suspend fun upsertAll(entries: List<DictionaryEntryEntity>) {
            upsertCallCount += 1
            onBeforeUpsert?.invoke()
            delegate.upsertAll(entries)
        }

        override suspend fun toggleFavorite(entryId: String) {
            onBeforeToggleFavorite?.invoke(entryId)
            delegate.toggleFavorite(entryId)
            onAfterToggleFavorite?.invoke(entryId)
        }
    }

    private class FakeDictionaryApiService : ParintintinApiService {
        var entriesResult: Result<List<DictionaryEntryDto>> = Result.success(emptyList())

        override suspend fun getArchiveManifest(): ArchiveManifestDto =
            ArchiveManifestDto(version = "1", entriesCount = 0, articlesCount = 0)

        override suspend fun getDictionaryEntries(): List<DictionaryEntryDto> =
            entriesResult.getOrThrow()

        override suspend fun getCulturalArticles(): List<CulturalArticleDto> =
            emptyList()
    }

    private object IoDispatcherProvider : DispatcherProvider {
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val default: CoroutineDispatcher = Dispatchers.Default
        override val main: CoroutineDispatcher = Dispatchers.Main
    }
}
