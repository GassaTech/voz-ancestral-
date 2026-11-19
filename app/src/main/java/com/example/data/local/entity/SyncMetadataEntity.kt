package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val id: String = SINGLETON_ID,
    @ColumnInfo(name = "last_attempt_epoch_millis")
    val lastAttemptEpochMillis: Long,
    @ColumnInfo(name = "last_attempt_status")
    val lastAttemptStatus: String,
    @ColumnInfo(name = "last_failure_reason")
    val lastFailureReason: String?,
    @ColumnInfo(name = "last_successful_sync_epoch_millis")
    val lastSuccessfulSyncEpochMillis: Long?,
    @ColumnInfo(name = "last_successful_entries_count")
    val lastSuccessfulEntriesCount: Int?,
    @ColumnInfo(name = "last_successful_articles_count")
    val lastSuccessfulArticlesCount: Int?,
    @ColumnInfo(name = "last_successful_remote_version")
    val lastSuccessfulRemoteVersion: String?
) {
    companion object {
        const val SINGLETON_ID = "archive_sync_singleton"
        const val STATUS_SYNCED = "SYNCED"
        const val STATUS_REMOTE_EMPTY = "REMOTE_EMPTY"
        const val STATUS_OFFLINE_FALLBACK = "OFFLINE_FALLBACK"
    }
}
