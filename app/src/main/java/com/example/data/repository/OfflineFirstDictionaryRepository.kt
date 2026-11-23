package com.example.data.repository

import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.DictionaryDao
import com.example.data.mapper.toDomain
import com.example.domain.model.DictionaryEntry
import com.example.domain.model.GrammaticalClass
import com.example.domain.model.SemanticCategory
import com.example.domain.repository.DictionaryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class OfflineFirstDictionaryRepository @Inject constructor(
    private val dictionaryDao: DictionaryDao,
    private val dispatchers: DispatcherProvider
) : DictionaryRepository {

    override fun observeEntries(
        query: String,
        category: SemanticCategory?,
        onlyFavorites: Boolean,
        grammaticalClass: GrammaticalClass?
    ): Flow<List<DictionaryEntry>> =
        dictionaryDao.observeEntries(
            query = query.trim(),
            category = category?.name,
            onlyFavorites = onlyFavorites,
            grammaticalClass = grammaticalClass?.name
        )
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeEntryById(entryId: String): Flow<DictionaryEntry?> =
        dictionaryDao.observeEntryById(entryId)
            .map { it?.toDomain() }
            .flowOn(dispatchers.io)

    override fun observeTotalCount(): Flow<Int> =
        dictionaryDao.observeTotalCount().flowOn(dispatchers.io)

    override fun observeFavoriteCount(): Flow<Int> =
        dictionaryDao.observeFavoriteCount().flowOn(dispatchers.io)

    override suspend fun toggleFavorite(entryId: String) {
        withContext(dispatchers.io) {
            dictionaryDao.toggleFavorite(entryId)
        }
    }
}
