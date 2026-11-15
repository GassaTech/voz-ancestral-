package com.example.domain.repository

import com.example.core.common.AppResult
import com.example.domain.model.ArchiveStats
import com.example.domain.model.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SyncStatusRepository {
    val syncState: StateFlow<SyncState>
    fun observeArchiveStats(): Flow<ArchiveStats>
    suspend fun syncArchive(): AppResult<Unit>
}
