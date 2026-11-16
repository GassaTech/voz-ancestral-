package com.example.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.example.data.local.dao.DictionaryDao
import com.example.data.local.dao.EncyclopediaDao
import com.example.data.local.dao.SyncMetadataDao
import com.example.data.local.entity.CulturalArticleEntity
import com.example.data.local.entity.DictionaryEntryEntity
import com.example.data.local.entity.SyncMetadataEntity

@Database(
    entities = [
        DictionaryEntryEntity::class,
        CulturalArticleEntity::class,
        SyncMetadataEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(ParintintinConverters::class)
abstract class ParintintinDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun encyclopediaDao(): EncyclopediaDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
    }
}
