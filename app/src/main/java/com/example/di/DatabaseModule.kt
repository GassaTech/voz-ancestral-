package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.dao.DictionaryDao
import com.example.data.local.dao.EncyclopediaDao
import com.example.data.local.dao.IndigenousLandDao
import com.example.data.local.dao.SeedMetadataDao
import com.example.data.local.dao.SyncMetadataDao
import com.example.data.local.database.ParintintinDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideParintintinDatabase(
        @ApplicationContext context: Context
    ): ParintintinDatabase = Room.databaseBuilder(
        context,
        ParintintinDatabase::class.java,
        "parintintin_archive.db"
    )
        .addMigrations(*ParintintinDatabase.MIGRATIONS)
        .addCallback(ParintintinDatabase.SEED_INITIALIZATION_CALLBACK)
        .build()

    @Provides
    fun provideDictionaryDao(database: ParintintinDatabase): DictionaryDao =
        database.dictionaryDao()

    @Provides
    fun provideEncyclopediaDao(database: ParintintinDatabase): EncyclopediaDao =
        database.encyclopediaDao()

    @Provides
    fun provideSyncMetadataDao(database: ParintintinDatabase): SyncMetadataDao =
        database.syncMetadataDao()

    @Provides
    fun provideSeedMetadataDao(database: ParintintinDatabase): SeedMetadataDao =
        database.seedMetadataDao()

    @Provides
    fun provideIndigenousLandDao(database: ParintintinDatabase): IndigenousLandDao =
        database.indigenousLandDao()
}
