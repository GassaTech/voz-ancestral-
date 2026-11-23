package com.example.domain.usecase

import com.example.core.common.AppResult
import com.example.domain.model.ArchiveStats
import com.example.domain.model.CulturalArticle
import com.example.domain.model.CulturalTopic
import com.example.domain.model.DeviceConnectivityState
import com.example.domain.model.DictionaryEntry
import com.example.domain.model.GrammaticalClass
import com.example.domain.model.IndigenousLand
import com.example.domain.model.RemoteSyncOutcome
import com.example.domain.model.SemanticCategory
import com.example.domain.model.SyncFailureType
import com.example.domain.model.SyncState
import com.example.domain.repository.DictionaryRepository
import com.example.domain.repository.EncyclopediaRepository
import com.example.domain.repository.IndigenousLandRepository
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
    fun `casos de uso de dicionario enciclopedia territorios e sincronizacao delegam parametros corretamente`() = runTest {
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
        val sampleTerritory = IndigenousLand(
            id = "terrai:101",
            objectId = 1L,
            territoryCode = "101",
            name = "Ipixuna",
            rawEthnicity = "Parintintin",
            ethnicities = listOf("Parintintin"),
            rawMunicipality = "Humaitá",
            municipalities = listOf("Humaitá"),
            rawStateUf = "AM",
            statesUf = listOf("AM"),
            surfaceArea = 215362.0,
            phase = "Regularizada",
            modality = "Tradicionalmente Ocupada",
            updatedAtRaw = "2024-08-19"
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
        }
        val fakeTerritoryRepo = object : IndigenousLandRepository {
            var lastQuery: String? = null
            var lastUf: String? = null
            var lastPhase: String? = null
            var lastModality: String? = null
            var syncCalls = 0

            override fun observeAll(): Flow<List<IndigenousLand>> = flowOf(listOf(sampleTerritory))

            override fun observeTerritories(
                query: String,
                stateUf: String?,
                phase: String?,
                modality: String?
            ): Flow<List<IndigenousLand>> {
                lastQuery = query
                lastUf = stateUf
                lastPhase = phase
                lastModality = modality
                return flowOf(listOf(sampleTerritory))
            }

            override fun observeById(id: String): Flow<IndigenousLand?> =
                flowOf(sampleTerritory.takeIf { it.id == id })

            override suspend fun findByTerritoryCode(territoryCode: String): IndigenousLand? =
                sampleTerritory.takeIf { it.territoryCode == territoryCode }

            override fun observeTotalCount(): Flow<Int> = flowOf(627)
            override suspend fun countOnce(): Int = 627
            override suspend fun upsert(territory: IndigenousLand) = Unit
            override suspend fun upsertAll(territories: List<IndigenousLand>) = Unit
            override suspend fun update(territory: IndigenousLand): Boolean = false
            override suspend fun deleteAll() = Unit
            override suspend fun syncFromRemote(): AppResult<RemoteSyncOutcome> {
                syncCalls += 1
                return AppResult.Success(RemoteSyncOutcome.Synced(627))
            }
        }
        val expectedStats = ArchiveStats(totalEntries = 20, favoriteEntries = 2, totalTerritories = 627)
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

        val territories = ObserveIndigenousLandsUseCase(fakeTerritoryRepo)(
            query = "Ipixuna",
            stateUf = "AM",
            phase = "Regularizada",
            modality = "Tradicionalmente Ocupada"
        ).first()
        assertEquals(listOf(sampleTerritory), territories)
        assertEquals("Ipixuna", fakeTerritoryRepo.lastQuery)
        assertEquals("AM", fakeTerritoryRepo.lastUf)
        assertEquals("Regularizada", fakeTerritoryRepo.lastPhase)
        assertEquals("Tradicionalmente Ocupada", fakeTerritoryRepo.lastModality)
        assertEquals(sampleTerritory, ObserveIndigenousLandByIdUseCase(fakeTerritoryRepo)("terrai:101").first())
        assertEquals(627, ObserveIndigenousLandsCountUseCase(fakeTerritoryRepo)().first())
        assertEquals(AppResult.Success(RemoteSyncOutcome.Synced(627)), SyncIndigenousLandsUseCase(fakeTerritoryRepo)())
        assertEquals(1, fakeTerritoryRepo.syncCalls)

        assertSame(fakeSyncRepo.syncState, ObserveSyncStateUseCase(fakeSyncRepo)())
        assertEquals(expectedStats, ObserveArchiveStatsUseCase(fakeSyncRepo)().first())
        assertEquals(AppResult.Success(Unit), SyncArchiveUseCase(fakeSyncRepo)())
        assertEquals(1, fakeSyncRepo.syncCalls)
    }
}
