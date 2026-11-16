package com.example.di

import com.example.data.sync.ArchiveSyncCoordinatorImpl
import com.example.domain.sync.ArchiveSyncCoordinator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindArchiveSyncCoordinator(
        impl: ArchiveSyncCoordinatorImpl
    ): ArchiveSyncCoordinator
}
