package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SeedMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeedMetadataDao {
    @Query("SELECT * FROM seed_metadata WHERE id = :id LIMIT 1")
    fun observeMetadata(id: String = SeedMetadataEntity.SINGLETON_ID): Flow<SeedMetadataEntity?>

    @Query("SELECT * FROM seed_metadata WHERE id = :id LIMIT 1")
    suspend fun getMetadataOnce(id: String = SeedMetadataEntity.SINGLETON_ID): SeedMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: SeedMetadataEntity)
}
