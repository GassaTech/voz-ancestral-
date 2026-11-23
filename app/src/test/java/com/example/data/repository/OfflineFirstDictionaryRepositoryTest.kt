package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.DispatcherProvider
import com.example.data.local.database.ParintintinDatabase
import com.example.data.local.entity.DictionaryEntryEntity
import com.example.domain.model.GrammaticalClass
import com.example.domain.model.RemoteSyncOutcome
import com.example.domain.model.SemanticCategory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
    private lateinit var repository: OfflineFirstDictionaryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ParintintinDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = OfflineFirstDictionaryRepository(
            dictionaryDao = database.dictionaryDao(),
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
    fun `toggleFavorite alterna marcacao local e atualiza contagem de favoritos`() = runBlocking {
        database.dictionaryDao().upsertAll(
            listOf(
                sampleEntity(id = "dict_1", word = "Ywy", isFavorite = false),
                sampleEntity(id = "dict_2", word = "Kaa", isFavorite = true)
            )
        )

        assertEquals(2, repository.observeTotalCount().first())
        assertEquals(1, repository.observeFavoriteCount().first())

        repository.toggleFavorite("dict_1")
        repository.toggleFavorite("dict_2")

        assertEquals(1, repository.observeFavoriteCount().first())
        assertTrue(repository.observeEntryById("dict_1").first()?.isFavorite == true)
        assertFalse(repository.observeEntryById("dict_2").first()?.isFavorite == true)
        assertNotNull(repository.observeEntryById("dict_1").first())
    }

    @Test
    fun `observeEntries busca por palavra traducao nota e filtra favoritos localmente`() = runBlocking {
        database.dictionaryDao().upsertAll(
            listOf(
                sampleEntity(
                    id = "dict_1",
                    word = "Ywy",
                    translation = "Terra firme",
                    culturalNote = "Território ancestral",
                    isFavorite = true
                ),
                sampleEntity(
                    id = "dict_2",
                    word = "Kaa",
                    translation = "Floresta alta",
                    culturalNote = "Mata fechada",
                    isFavorite = false
                )
            )
        )

        val favorites = repository.observeEntries(onlyFavorites = true).first()
        assertEquals(listOf("dict_1"), favorites.map { it.id })

        val byNote = repository.observeEntries(query = "  ancestral  ").first()
        assertEquals(listOf("dict_1"), byNote.map { it.id })

        assertTrue(repository.observeEntryById("inexistente").first() == null)
    }

    @Test
    fun `RemoteSyncOutcome Synced rejeita contagem menor ou igual a zero`() {
        var zeroFailed = false
        try {
            RemoteSyncOutcome.Synced(0)
        } catch (_: IllegalArgumentException) {
            zeroFailed = true
        }
        assertTrue(zeroFailed)

        var negativeFailed = false
        try {
            RemoteSyncOutcome.Synced(-1)
        } catch (_: IllegalArgumentException) {
            negativeFailed = true
        }
        assertTrue(negativeFailed)
    }

    @Test
    fun `observeEntries filtra por classe gramatical individualmente e mantem ordenacao alfabetica`() = runBlocking {
        database.dictionaryDao().upsertAll(
            listOf(
                sampleEntity(
                    id = "dict_1",
                    word = "Ywy",
                    grammaticalClass = "NOUN",
                    isFavorite = false
                ),
                sampleEntity(
                    id = "dict_2",
                    word = "pyhyk",
                    grammaticalClass = "VERB",
                    isFavorite = false
                ),
                sampleEntity(
                    id = "dict_3",
                    word = "Awa",
                    grammaticalClass = "VERB",
                    isFavorite = false
                ),
                sampleEntity(
                    id = "dict_4",
                    word = "Porang",
                    grammaticalClass = "ADJECTIVE",
                    isFavorite = false
                )
            )
        )

        val verbs = repository.observeEntries(grammaticalClass = GrammaticalClass.VERB).first()
        val nouns = repository.observeEntries(grammaticalClass = GrammaticalClass.NOUN).first()
        val all = repository.observeEntries(grammaticalClass = null).first()

        assertEquals(listOf("dict_3", "dict_2"), verbs.map { it.id })
        assertTrue(verbs.all { it.grammaticalClass == GrammaticalClass.VERB })
        assertEquals(listOf("dict_1"), nouns.map { it.id })
        assertEquals(4, all.size)
    }

    @Test
    fun `observeEntries combina classe gramatical com busca textual categoria semantica e favoritos`() = runBlocking {
        database.dictionaryDao().upsertAll(
            listOf(
                sampleEntity(
                    id = "dict_1",
                    word = "Kaa",
                    translation = "Mata verde",
                    grammaticalClass = "NOUN",
                    semanticCategory = "NATURE",
                    isFavorite = true
                ),
                sampleEntity(
                    id = "dict_2",
                    word = "Kaa-ete",
                    translation = "Floresta alta",
                    grammaticalClass = "ADJECTIVE",
                    semanticCategory = "NATURE",
                    isFavorite = true
                ),
                sampleEntity(
                    id = "dict_3",
                    word = "Pira",
                    translation = "Peixe de rio",
                    grammaticalClass = "NOUN",
                    semanticCategory = "ANIMALS",
                    isFavorite = true
                ),
                sampleEntity(
                    id = "dict_4",
                    word = "Ywy",
                    translation = "Terra de mata",
                    grammaticalClass = "NOUN",
                    semanticCategory = "NATURE",
                    isFavorite = false
                )
            )
        )

        val byCategoryAndClass = repository.observeEntries(
            category = SemanticCategory.NATURE,
            grammaticalClass = GrammaticalClass.NOUN
        ).first()
        assertEquals(listOf("dict_1", "dict_4"), byCategoryAndClass.map { it.id })

        val byAllFilters = repository.observeEntries(
            query = "  mata  ",
            category = SemanticCategory.NATURE,
            onlyFavorites = true,
            grammaticalClass = GrammaticalClass.NOUN
        ).first()
        assertEquals(listOf("dict_1"), byAllFilters.map { it.id })

        val withoutMatches = repository.observeEntries(
            query = "mata",
            category = SemanticCategory.ANIMALS,
            grammaticalClass = GrammaticalClass.VERB
        ).first()
        assertTrue(withoutMatches.isEmpty())
    }

    private fun sampleEntity(
        id: String,
        word: String,
        translation: String = "Tradução $id",
        culturalNote: String = "Nota $id",
        grammaticalClass: String = "NOUN",
        semanticCategory: String = "NATURE",
        isFavorite: Boolean,
        updatedAt: Long = 1_000L
    ): DictionaryEntryEntity = DictionaryEntryEntity(
        id = id,
        parintintinWord = word,
        phoneticIpa = "/$id/",
        portugueseTranslation = translation,
        grammaticalClass = grammaticalClass,
        semanticCategory = semanticCategory,
        exampleParintintin = "Exemplo $word",
        examplePortuguese = "Exemplo $translation",
        culturalNote = culturalNote,
        audioUrl = null,
        isFavorite = isFavorite,
        updatedAtEpochMillis = updatedAt
    )

    private object IoDispatcherProvider : DispatcherProvider {
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val default: CoroutineDispatcher = Dispatchers.Default
        override val main: CoroutineDispatcher = Dispatchers.Main
    }
}
