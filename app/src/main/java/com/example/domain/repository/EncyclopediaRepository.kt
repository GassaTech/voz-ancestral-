package com.example.domain.repository

import com.example.domain.model.CulturalArticle
import com.example.domain.model.CulturalTopic
import kotlinx.coroutines.flow.Flow

interface EncyclopediaRepository {
    fun observeArticles(
        query: String = "",
        topic: CulturalTopic? = null,
        onlyBookmarked: Boolean = false
    ): Flow<List<CulturalArticle>>

    fun observeArticleById(articleId: String): Flow<CulturalArticle?>

    fun observeTotalCount(): Flow<Int>

    fun observeBookmarkedCount(): Flow<Int>

    suspend fun toggleBookmark(articleId: String)
}
