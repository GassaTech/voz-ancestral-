package com.example.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.DictionaryDao
import com.example.data.local.dao.EncyclopediaDao
import com.example.data.local.dao.IndigenousLandDao
import com.example.data.local.dao.SeedMetadataDao
import com.example.data.local.dao.SyncMetadataDao
import com.example.data.local.entity.CulturalArticleEntity
import com.example.data.local.entity.DictionaryEntryEntity
import com.example.data.local.entity.IndigenousLandEntity
import com.example.data.local.entity.SeedMetadataEntity
import com.example.data.local.entity.SyncMetadataEntity

@Database(
    entities = [
        DictionaryEntryEntity::class,
        CulturalArticleEntity::class,
        SyncMetadataEntity::class,
        SeedMetadataEntity::class,
        IndigenousLandEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(ParintintinConverters::class)
abstract class ParintintinDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun encyclopediaDao(): EncyclopediaDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun seedMetadataDao(): SeedMetadataDao
    abstract fun indigenousLandDao(): IndigenousLandDao

    companion object {
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)

        val SEED_INITIALIZATION_CALLBACK: Callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `seed_metadata` (
                        `id`,
                        `applied_seed_version`,
                        `applied_seed_hash`,
                        `applied_seed_ids_json`,
                        `updated_at_epoch_millis`
                    ) VALUES (?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any?>(
                        SeedMetadataEntity.SINGLETON_ID,
                        SeedMetadataEntity.INITIAL_SEED_VERSION,
                        null,
                        null,
                        0L
                    )
                )
            }
        }
    }
}
