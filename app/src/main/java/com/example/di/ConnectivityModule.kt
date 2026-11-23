package com.example.di

import android.content.Context
import android.net.ConnectivityManager
import com.example.core.common.ApplicationScope
import com.example.data.connectivity.AndroidNetworkConnectivityObserver
import com.example.domain.connectivity.NetworkConnectivityObserver
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityModule {

    @Binds
    @Singleton
    abstract fun bindNetworkConnectivityObserver(
        impl: AndroidNetworkConnectivityObserver
    ): NetworkConnectivityObserver

    companion object {
        @Provides
        @Singleton
        fun provideConnectivityManager(
            @ApplicationContext context: Context
        ): ConnectivityManager = requireNotNull(
            context.getSystemService(ConnectivityManager::class.java)
        ) {
            "ConnectivityManager indisponível no Context da aplicação."
        }

        @Provides
        @Singleton
        fun provideAndroidNetworkConnectivityObserver(
            connectivityManager: ConnectivityManager,
            @ApplicationScope applicationScope: CoroutineScope
        ): AndroidNetworkConnectivityObserver = AndroidNetworkConnectivityObserver(
            connectivityManager = connectivityManager,
            applicationScope = applicationScope
        )
    }
}
