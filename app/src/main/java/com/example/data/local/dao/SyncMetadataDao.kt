package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE id = :id LIMIT 1")
    fun observeMetadata(id: String = SyncMetadataEntity.SINGLETON_ID): Flow<SyncMetadataEntity?>

    @Query("SELECT * FROM sync_metadata WHERE id = :id LIMIT 1")
    suspend fun getMetadataOnce(id: String = SyncMetadataEntity.SINGLETON_ID): SyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: SyncMetadataEntity)

    @Transaction
    suspend fun recordAttemptFailure(
        attemptEpochMillis: Long,
        failureReason: String?
    ) {
        val current = getMetadataOnce(SyncMetadataEntity.SINGLETON_ID)
        val updated = current?.copy(
            lastAttemptEpochMillis = attemptEpochMillis,
            lastAttemptStatus = SyncMetadataEntity.STATUS_OFFLINE_FALLBACK,
            lastFailureReason = failureReason
        ) ?: SyncMetadataEntity(
            id = SyncMetadataEntity.SINGLETON_ID,
            lastAttemptEpochMillis = attemptEpochMillis,
            lastAttemptStatus = SyncMetadataEntity.STATUS_OFFLINE_FALLBACK,
            lastFailureReason = failureReason,
            lastSuccessfulSyncEpochMillis = null,
            lastSuccessfulEntriesCount = null,
            lastSuccessfulArticlesCount = null,
            lastSuccessfulRemoteVersion = null
        )
        upsert(updated)
    }
}
