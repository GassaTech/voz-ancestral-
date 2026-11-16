package com.example.domain.model

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data class Synced(
        val lastSyncedEpochMillis: Long,
        val entriesCount: Int? = null,
        val articlesCount: Int? = null
    ) : SyncState

    data class OfflineFallback(
        val reason: String,
        val lastAttemptEpochMillis: Long
    ) : SyncState
}

data class ArchiveStats(
    val totalEntries: Int = 0,
    val favoriteEntries: Int = 0,
    val totalArticles: Int = 0,
    val bookmarkedArticles: Int = 0
)
