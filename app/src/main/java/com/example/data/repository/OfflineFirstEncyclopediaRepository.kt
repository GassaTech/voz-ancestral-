package com.example.data.repository

import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.EncyclopediaDao
import com.example.data.mapper.toDomain
import com.example.domain.model.CulturalArticle
import com.example.domain.model.CulturalTopic
import com.example.domain.repository.EncyclopediaRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class OfflineFirstEncyclopediaRepository @Inject constructor(
    private val encyclopediaDao: EncyclopediaDao,
    private val dispatchers: DispatcherProvider
) : EncyclopediaRepository {

    override fun observeArticles(
        query: String,
        topic: CulturalTopic?,
        onlyBookmarked: Boolean
    ): Flow<List<CulturalArticle>> =
        encyclopediaDao.observeArticles(
            query = query.trim(),
            topic = topic?.name,
            onlyBookmarked = onlyBookmarked
        )
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeArticleById(articleId: String): Flow<CulturalArticle?> =
        encyclopediaDao.observeArticleById(articleId)
            .map { it?.toDomain() }
            .flowOn(dispatchers.io)

    override fun observeTotalCount(): Flow<Int> =
        encyclopediaDao.observeTotalCount().flowOn(dispatchers.io)

    override fun observeBookmarkedCount(): Flow<Int> =
        encyclopediaDao.observeBookmarkedCount().flowOn(dispatchers.io)

    override suspend fun toggleBookmark(articleId: String) {
        withContext(dispatchers.io) {
            encyclopediaDao.toggleBookmark(articleId)
        }
    }
}
