package com.example.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    version = 1,
    exportSchema = false
)
@TypeConverters(ParintintinConverters::class)
abstract class ParintintinDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun encyclopediaDao(): EncyclopediaDao
    abstract fun syncMetadataDao(): SyncMetadataDao
}
