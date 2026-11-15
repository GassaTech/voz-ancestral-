package com.example.data.repository

import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.EncyclopediaDao
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.data.remote.datasource.RemoteArchiveDataSource
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
    private val remoteDataSource: RemoteArchiveDataSource,
    private val dispatchers: DispatcherProvider
) : EncyclopediaRepository {

    override fun observeArticles(
        topic: CulturalTopic?,
        onlyBookmarked: Boolean
    ): Flow<List<CulturalArticle>> =
        encyclopediaDao.observeArticles(
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

    override suspend fun syncFromRemote(): AppResult<Int> = withContext(dispatchers.io) {
        when (val result = remoteDataSource.fetchCulturalArticles()) {
            is AppResult.Success -> {
                val bookmarkedIds = encyclopediaDao.getBookmarkedIds().toSet()
                val entities = result.data.map { dto ->
                    dto.toEntity(isBookmarked = bookmarkedIds.contains(dto.id))
                }
                if (entities.isNotEmpty()) {
                    encyclopediaDao.upsertAll(entities)
                }
                AppResult.Success(entities.size)
            }
            is AppResult.Error -> result
        }
    }
}
