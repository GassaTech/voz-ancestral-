package com.example.di

import com.example.core.common.DefaultDispatcherProvider
import com.example.core.common.DispatcherProvider
import com.example.data.repository.ArchiveSyncRepositoryImpl
import com.example.data.repository.OfflineFirstDictionaryRepository
import com.example.data.repository.OfflineFirstEncyclopediaRepository
import com.example.domain.repository.DictionaryRepository
import com.example.domain.repository.EncyclopediaRepository
import com.example.domain.repository.SyncStatusRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDictionaryRepository(
        impl: OfflineFirstDictionaryRepository
    ): DictionaryRepository

    @Binds
    @Singleton
    abstract fun bindEncyclopediaRepository(
        impl: OfflineFirstEncyclopediaRepository
    ): EncyclopediaRepository

    @Binds
    @Singleton
    abstract fun bindSyncStatusRepository(
        impl: ArchiveSyncRepositoryImpl
    ): SyncStatusRepository

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(
        impl: DefaultDispatcherProvider
    ): DispatcherProvider
}
