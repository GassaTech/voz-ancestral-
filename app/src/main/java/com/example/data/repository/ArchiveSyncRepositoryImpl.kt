package com.example.data.repository

import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.SyncMetadataDao
import com.example.data.local.entity.SyncMetadataEntity
import com.example.domain.model.ArchiveStats
import com.example.domain.model.SyncState
import com.example.domain.repository.DictionaryRepository
import com.example.domain.repository.EncyclopediaRepository
import com.example.domain.repository.SyncStatusRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class ArchiveSyncRepositoryImpl @Inject constructor(
    private val dictionaryRepository: DictionaryRepository,
    private val encyclopediaRepository: EncyclopediaRepository,
    private val syncMetadataDao: SyncMetadataDao,
    private val dispatchers: DispatcherProvider
) : SyncStatusRepository {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    override fun observeArchiveStats(): Flow<ArchiveStats> =
        combine(
            dictionaryRepository.observeTotalCount(),
            dictionaryRepository.observeFavoriteCount(),
            encyclopediaRepository.observeTotalCount(),
            encyclopediaRepository.observeBookmarkedCount()
        ) { totalEntries, favoriteEntries, totalArticles, bookmarkedArticles ->
            ArchiveStats(
                totalEntries = totalEntries,
                favoriteEntries = favoriteEntries,
                totalArticles = totalArticles,
                bookmarkedArticles = bookmarkedArticles
            )
        }.flowOn(dispatchers.io)

    override suspend fun syncArchive(): AppResult<Unit> = withContext(dispatchers.io) {
        _syncState.value = SyncState.Syncing
        val entriesResult = dictionaryRepository.syncFromRemote()
        val articlesResult = encyclopediaRepository.syncFromRemote()
        val now = System.currentTimeMillis()

        when {
            entriesResult is AppResult.Success && articlesResult is AppResult.Success -> {
                syncMetadataDao.upsert(
                    SyncMetadataEntity(
                        lastSyncEpochMillis = now,
                        remoteVersion = "api-synced",
                        lastStatus = "SYNCED"
                    )
                )
                _syncState.value = SyncState.Synced(
                    lastSyncedEpochMillis = now,
                    entriesCount = entriesResult.data,
                    articlesCount = articlesResult.data
                )
                AppResult.Success(Unit)
            }
            else -> {
                val reason = (entriesResult as? AppResult.Error)?.message
                    ?: (articlesResult as? AppResult.Error)?.message
                    ?: "Modo offline ativo."
                syncMetadataDao.upsert(
                    SyncMetadataEntity(
                        lastSyncEpochMillis = now,
                        remoteVersion = "offline-cache",
                        lastStatus = "OFFLINE_FALLBACK"
                    )
                )
                _syncState.value = SyncState.OfflineFallback(
                    reason = reason,
                    lastAttemptEpochMillis = now
                )
                AppResult.Error(reason)
            }
        }
    }
}
