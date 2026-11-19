package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.dao.DictionaryDao
import com.example.data.local.dao.EncyclopediaDao
import com.example.data.local.dao.SeedMetadataDao
import com.example.data.local.dao.SyncMetadataDao
import com.example.data.local.database.ParintintinDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideParintintinDatabase(
        @ApplicationContext context: Context
    ): ParintintinDatabase = Room.inMemoryDatabaseBuilder(
        context,
        ParintintinDatabase::class.java
    )
        .addMigrations(*ParintintinDatabase.MIGRATIONS)
        .addCallback(ParintintinDatabase.SEED_INITIALIZATION_CALLBACK)
        .allowMainThreadQueries()
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
}
