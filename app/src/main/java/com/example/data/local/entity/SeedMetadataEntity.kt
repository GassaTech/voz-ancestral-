package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "seed_metadata")
data class SeedMetadataEntity(
    @PrimaryKey
    val id: String = SINGLETON_ID,
    @ColumnInfo(name = "applied_seed_version")
    val appliedSeedVersion: Int = INITIAL_SEED_VERSION,
    @ColumnInfo(name = "applied_seed_hash")
    val appliedSeedHash: String? = null,
    @ColumnInfo(name = "applied_seed_ids_json")
    val appliedSeedIdsJson: String? = null,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long = 0L
) {
    companion object {
        const val SINGLETON_ID = "dictionary_seed_singleton"
        const val INITIAL_SEED_VERSION = 0
    }
}
