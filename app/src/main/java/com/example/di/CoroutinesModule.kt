package com.example.di

import com.example.core.common.ApplicationScope
import com.example.core.common.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(
        dispatchers: DispatcherProvider
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.io)
}
