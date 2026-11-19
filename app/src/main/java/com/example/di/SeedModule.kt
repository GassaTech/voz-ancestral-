package com.example.di

import com.example.data.local.seed.AndroidDictionarySeedAssetSource
import com.example.data.local.seed.DefaultDictionarySeedChangeDetector
import com.example.data.local.seed.DictionarySeedAssetSource
import com.example.data.local.seed.DictionarySeedChangeDetector
import com.example.data.local.seed.DictionarySeedUpdater
import com.example.data.local.seed.SeedClock
import com.example.data.local.seed.VersionedDictionarySeedUpdater
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SeedModule {

    @Binds
    @Singleton
    abstract fun bindDictionarySeedAssetSource(
        impl: AndroidDictionarySeedAssetSource
    ): DictionarySeedAssetSource

    @Binds
    @Singleton
    abstract fun bindDictionarySeedChangeDetector(
        impl: DefaultDictionarySeedChangeDetector
    ): DictionarySeedChangeDetector

    @Binds
    @Singleton
    abstract fun bindDictionarySeedUpdater(
        impl: VersionedDictionarySeedUpdater
    ): DictionarySeedUpdater

    companion object {
        @Provides
        @Singleton
        fun provideSeedClock(): SeedClock = SeedClock(System::currentTimeMillis)
    }
}
