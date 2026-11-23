package com.example.domain.model

sealed interface SyncState {
    data object Loading : SyncState
    data object Idle : SyncState
    data object Syncing : SyncState
    data class Synced(
        val lastSyncedEpochMillis: Long,
        val entriesCount: Int? = null,
        val articlesCount: Int? = null
    ) : SyncState

    data class RemoteEmpty(
        val lastAttemptEpochMillis: Long,
        val lastSyncedEpochMillis: Long? = null,
        val entriesCount: Int? = null,
        val articlesCount: Int? = null
    ) : SyncState

    data class OfflineFallback(
        val reason: String,
        val lastAttemptEpochMillis: Long,
        val failureType: SyncFailureType = SyncFailureType.NO_INTERNET,
        val lastSuccessfulSyncEpochMillis: Long? = null
    ) : SyncState
}

data class ArchiveStats(
    val totalEntries: Int = 0,
    val favoriteEntries: Int = 0,
    val totalArticles: Int = 0,
    val bookmarkedArticles: Int = 0,
    val totalTerritories: Int = 0
)
