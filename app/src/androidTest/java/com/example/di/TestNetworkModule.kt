package com.example.di

import com.example.data.remote.api.ParintintinApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class]
)
object TestNetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideMockWebServer(): MockWebServer {
        val executor = Executors.newSingleThreadExecutor()
        return try {
            executor.submit<MockWebServer> {
                MockWebServer().apply {
                    start(InetAddress.getByName("127.0.0.1"), 0)
                }
            }.get()
        } finally {
            executor.shutdown()
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
        mockWebServer: MockWebServer
    ): Retrofit = Retrofit.Builder()
        .baseUrl("http://127.0.0.1:${mockWebServer.port}/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideParintintinApiService(retrofit: Retrofit): ParintintinApiService =
        retrofit.create(ParintintinApiService::class.java)
}
