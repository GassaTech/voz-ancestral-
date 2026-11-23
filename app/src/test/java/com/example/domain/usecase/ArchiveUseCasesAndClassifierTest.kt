package com.example.domain.usecase

import com.example.core.common.AppResult
import com.example.domain.model.ArchiveStats
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
import com.example.domain.repository.SyncStatusRepository
import com.example.domain.sync.SyncFailureClassifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ArchiveUseCasesAndClassifierTest {

    @Test
    fun `SyncFailureClassifier mapeia todos os estados de conectividade do dispositivo`() {
        assertEquals(
            SyncFailureType.NO_INTERNET,
            SyncFailureClassifier.classify(DeviceConnectivityState.Offline)
        )
        assertEquals(
            SyncFailureType.UNVALIDATED_NETWORK,
            SyncFailureClassifier.classify(DeviceConnectivityState.ConnectedUnvalidated)
        )
        assertEquals(
            SyncFailureType.SERVER_UNAVAILABLE,
            SyncFailureClassifier.classify(DeviceConnectivityState.Online)
        )
    }

    @Test
    fun `casos de uso de dicionario enciclopedia e sincronizacao delegam parametros corretamente`() = runTest {
        val sampleEntry = DictionaryEntry(
            id = "e1",
            parintintinWord = "Termo",
            phoneticIpa = "/t/",
            portugueseTranslation = "Significado",
            grammaticalClass = GrammaticalClass.VERB,
            semanticCategory = SemanticCategory.NATURE,
            exampleParintintin = "Ex",
            examplePortuguese = "Ex pt",
            culturalNote = "Nota",
            audioUrl = null,
            isFavorite = false,
            updatedAtEpochMillis = 1_700_000_000_000L
        )
        val sampleArticle = CulturalArticle(
            id = "a1",
            title = "Título",
            subtitle = "Sub",
            summary = "Resumo",
            bodyParagraphs = listOf("Parágrafo"),
            topic = CulturalTopic.ORAL_TRADITION,
            readingTimeMinutes = 3,
            sourceReference = "Fonte",
            isBookmarked = true,
            updatedAtEpochMillis = 1_700_000_000_100L
        )
        val fakeDictionaryRepo = object : DictionaryRepository {
            var lastQuery: String? = null
            var lastCategory: SemanticCategory? = null
            var lastOnlyFavorites: Boolean? = null
            var lastGrammaticalClass: GrammaticalClass? = null
            var toggledId: String? = null

            override fun observeEntries(
                query: String,
                category: SemanticCategory?,
                onlyFavorites: Boolean,
                grammaticalClass: GrammaticalClass?
            ): Flow<List<DictionaryEntry>> {
                lastQuery = query
                lastCategory = category
                lastOnlyFavorites = onlyFavorites
                lastGrammaticalClass = grammaticalClass
                return flowOf(listOf(sampleEntry))
            }

            override fun observeEntryById(entryId: String): Flow<DictionaryEntry?> =
                flowOf(sampleEntry.takeIf { it.id == entryId })

            override fun observeTotalCount(): Flow<Int> = flowOf(1)
            override fun observeFavoriteCount(): Flow<Int> = flowOf(0)
            override suspend fun toggleFavorite(entryId: String) {
                toggledId = entryId
            }

            override suspend fun syncFromRemote(): AppResult<RemoteSyncOutcome> =
                AppResult.Success(RemoteSyncOutcome.Empty)
        }
        val fakeEncyclopediaRepo = object : EncyclopediaRepository {
            var bookmarkedId: String? = null

            override fun observeArticles(
                query: String,
                topic: CulturalTopic?,
                onlyBookmarked: Boolean
            ): Flow<List<CulturalArticle>> = flowOf(listOf(sampleArticle))

            override fun observeArticleById(articleId: String): Flow<CulturalArticle?> =
                flowOf(sampleArticle.takeIf { it.id == articleId })

            override fun observeTotalCount(): Flow<Int> = flowOf(1)
            override fun observeBookmarkedCount(): Flow<Int> = flowOf(1)
            override suspend fun toggleBookmark(articleId: String) {
                bookmarkedId = articleId
            }

            override suspend fun syncFromRemote(): AppResult<Int> = AppResult.Success(0)
        }
        val expectedStats = ArchiveStats(totalEntries = 20, favoriteEntries = 2)
        val syncStateFlow = MutableStateFlow<SyncState>(SyncState.Idle)
        val fakeSyncRepo = object : SyncStatusRepository {
            var syncCalls = 0
            override val syncState: StateFlow<SyncState> = syncStateFlow.asStateFlow()
            override fun observeArchiveStats(): Flow<ArchiveStats> = flowOf(expectedStats)
            override suspend fun syncArchive(): AppResult<Unit> {
                syncCalls += 1
                return AppResult.Success(Unit)
            }
        }

        val entries = ObserveDictionaryEntriesUseCase(fakeDictionaryRepo)(
            query = "rio",
            category = SemanticCategory.NATURE,
            onlyFavorites = true,
            grammaticalClass = GrammaticalClass.VERB
        ).first()
        assertEquals(listOf(sampleEntry), entries)
        assertEquals("rio", fakeDictionaryRepo.lastQuery)
        assertEquals(SemanticCategory.NATURE, fakeDictionaryRepo.lastCategory)
        assertEquals(true, fakeDictionaryRepo.lastOnlyFavorites)
        assertEquals(GrammaticalClass.VERB, fakeDictionaryRepo.lastGrammaticalClass)

        assertEquals(sampleEntry, ObserveDictionaryEntryByIdUseCase(fakeDictionaryRepo)("e1").first())
        ToggleFavoriteEntryUseCase(fakeDictionaryRepo)("e1")
        assertEquals("e1", fakeDictionaryRepo.toggledId)

        assertEquals(listOf(sampleArticle), ObserveCulturalArticlesUseCase(fakeEncyclopediaRepo)().first())
        assertEquals(sampleArticle, ObserveCulturalArticleByIdUseCase(fakeEncyclopediaRepo)("a1").first())
        ToggleArticleBookmarkUseCase(fakeEncyclopediaRepo)("a1")
        assertEquals("a1", fakeEncyclopediaRepo.bookmarkedId)

        assertSame(fakeSyncRepo.syncState, ObserveSyncStateUseCase(fakeSyncRepo)())
        assertEquals(expectedStats, ObserveArchiveStatsUseCase(fakeSyncRepo)().first())
        assertEquals(AppResult.Success(Unit), SyncArchiveUseCase(fakeSyncRepo)())
        assertEquals(1, fakeSyncRepo.syncCalls)
    }
}
