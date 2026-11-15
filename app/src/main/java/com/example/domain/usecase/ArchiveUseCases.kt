package com.example.domain.usecase

import com.example.core.common.AppResult
import com.example.domain.model.ArchiveStats
import com.example.domain.model.CulturalArticle
import com.example.domain.model.CulturalTopic
import com.example.domain.model.DictionaryEntry
import com.example.domain.model.SemanticCategory
import com.example.domain.model.SyncState
import com.example.domain.repository.DictionaryRepository
import com.example.domain.repository.EncyclopediaRepository
import com.example.domain.repository.SyncStatusRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class ObserveDictionaryEntriesUseCase @Inject constructor(
    private val repository: DictionaryRepository
) {
    operator fun invoke(
        query: String = "",
        category: SemanticCategory? = null,
        onlyFavorites: Boolean = false
    ): Flow<List<DictionaryEntry>> =
        repository.observeEntries(query = query, category = category, onlyFavorites = onlyFavorites)
}

class ObserveDictionaryEntryByIdUseCase @Inject constructor(
    private val repository: DictionaryRepository
) {
    operator fun invoke(entryId: String): Flow<DictionaryEntry?> =
        repository.observeEntryById(entryId)
}

class ToggleFavoriteEntryUseCase @Inject constructor(
    private val repository: DictionaryRepository
) {
    suspend operator fun invoke(entryId: String) = repository.toggleFavorite(entryId)
}

class ObserveCulturalArticlesUseCase @Inject constructor(
    private val repository: EncyclopediaRepository
) {
    operator fun invoke(
        topic: CulturalTopic? = null,
        onlyBookmarked: Boolean = false
    ): Flow<List<CulturalArticle>> =
        repository.observeArticles(topic = topic, onlyBookmarked = onlyBookmarked)
}

class ObserveCulturalArticleByIdUseCase @Inject constructor(
    private val repository: EncyclopediaRepository
) {
    operator fun invoke(articleId: String): Flow<CulturalArticle?> =
        repository.observeArticleById(articleId)
}

class ToggleArticleBookmarkUseCase @Inject constructor(
    private val repository: EncyclopediaRepository
) {
    suspend operator fun invoke(articleId: String) = repository.toggleBookmark(articleId)
}

class ObserveSyncStateUseCase @Inject constructor(
    private val repository: SyncStatusRepository
) {
    operator fun invoke(): StateFlow<SyncState> = repository.syncState
}

class ObserveArchiveStatsUseCase @Inject constructor(
    private val repository: SyncStatusRepository
) {
    operator fun invoke(): Flow<ArchiveStats> = repository.observeArchiveStats()
}

class SyncArchiveUseCase @Inject constructor(
    private val repository: SyncStatusRepository
) {
    suspend operator fun invoke(): AppResult<Unit> = repository.syncArchive()
}
