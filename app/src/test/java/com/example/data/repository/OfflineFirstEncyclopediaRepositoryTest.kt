package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.DispatcherProvider
import com.example.data.local.database.ParintintinDatabase
import com.example.data.local.entity.CulturalArticleEntity
import com.example.domain.model.CulturalTopic
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
class OfflineFirstEncyclopediaRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: ParintintinDatabase
    private lateinit var repository: OfflineFirstEncyclopediaRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ParintintinDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = OfflineFirstEncyclopediaRepository(
            encyclopediaDao = database.encyclopediaDao(),
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
    fun `consulta filtra por topico busca textual e favoritos locais no Room`() = runBlocking {
        database.encyclopediaDao().upsertAll(
            listOf(
                sampleEntity(
                    id = "art_1",
                    title = "Território Kagwahiva",
                    topic = "HISTORY",
                    isBookmarked = true
                ),
                sampleEntity(
                    id = "art_2",
                    title = "Cantos da floresta",
                    topic = "MUSIC_AND_RITUAL",
                    isBookmarked = false
                ),
                sampleEntity(
                    id = "art_3",
                    title = "Rios sagrados",
                    topic = "TERRITORY",
                    isBookmarked = true
                )
            )
        )

        assertEquals(3, repository.observeTotalCount().first())
        assertEquals(2, repository.observeBookmarkedCount().first())

        val bookmarked = repository.observeArticles(onlyBookmarked = true).first()
        assertEquals(setOf("art_1", "art_3"), bookmarked.map { it.id }.toSet())

        val byTopic = repository.observeArticles(topic = CulturalTopic.HISTORY).first()
        assertEquals(listOf("art_1"), byTopic.map { it.id })

        val byQuery = repository.observeArticles(query = "  Cantos  ").first()
        assertEquals(listOf("art_2"), byQuery.map { it.id })
    }

    @Test
    fun `toggleBookmark alterna marcacao local e atualiza observeArticleById`() = runBlocking {
        database.encyclopediaDao().upsertAll(
            listOf(
                sampleEntity(id = "art_1", title = "Memória oral", isBookmarked = false)
            )
        )

        repository.toggleBookmark("art_1")
        val afterMark = repository.observeArticleById("art_1").first()
        assertNotNull(afterMark)
        assertTrue(afterMark?.isBookmarked == true)
        assertEquals(1, repository.observeBookmarkedCount().first())

        repository.toggleBookmark("art_1")
        val afterUnmark = repository.observeArticleById("art_1").first()
        assertFalse(afterUnmark?.isBookmarked == true)
        assertEquals(0, repository.observeBookmarkedCount().first())
    }

    private fun sampleEntity(
        id: String,
        title: String,
        topic: String = "HISTORY",
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
        topic = topic,
        readingTimeMinutes = 4,
        sourceReference = "Acervo Local",
        isBookmarked = isBookmarked,
        updatedAtEpochMillis = updatedAt
    )

    private object IoDispatcherProvider : DispatcherProvider {
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val default: CoroutineDispatcher = Dispatchers.Default
        override val main: CoroutineDispatcher = Dispatchers.Main
    }
}
