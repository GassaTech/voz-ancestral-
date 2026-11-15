package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val id: String = SINGLETON_ID,
    @ColumnInfo(name = "last_sync_epoch_millis")
    val lastSyncEpochMillis: Long,
    @ColumnInfo(name = "remote_version")
    val remoteVersion: String,
    @ColumnInfo(name = "last_status")
    val lastStatus: String
) {
    companion object {
        const val SINGLETON_ID = "archive_sync_singleton"
    }
}
