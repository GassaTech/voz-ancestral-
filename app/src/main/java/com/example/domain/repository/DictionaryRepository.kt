package com.example.domain.repository

import com.example.core.common.AppResult
import com.example.domain.model.DictionaryEntry
import com.example.domain.model.GrammaticalClass
import com.example.domain.model.RemoteSyncOutcome
import com.example.domain.model.SemanticCategory
import kotlinx.coroutines.flow.Flow

interface DictionaryRepository {
    fun observeEntries(
        query: String = "",
        category: SemanticCategory? = null,
        onlyFavorites: Boolean = false,
        grammaticalClass: GrammaticalClass? = null
    ): Flow<List<DictionaryEntry>>

    fun observeEntryById(entryId: String): Flow<DictionaryEntry?>

    fun observeTotalCount(): Flow<Int>

    fun observeFavoriteCount(): Flow<Int>

    suspend fun toggleFavorite(entryId: String)

    suspend fun syncFromRemote(): AppResult<RemoteSyncOutcome>
}
