package com.example.di

import com.example.BuildConfig
import com.example.data.remote.api.ArcGisTerritoriesQuerySpec
import com.example.data.remote.api.IbamaTerritoriesApiService
import com.example.data.remote.api.ParintintinApiService
import com.example.data.remote.dto.IndigenousLandAttributesDto
import com.example.data.remote.dto.IndigenousLandAttributesJsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(IndigenousLandAttributesDto::class.java, IndigenousLandAttributesJsonAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit = Retrofit.Builder()
        .baseUrl(ArcGisTerritoriesQuerySpec.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideIbamaTerritoriesApiService(retrofit: Retrofit): IbamaTerritoriesApiService =
        retrofit.create(IbamaTerritoriesApiService::class.java)

    @Provides
    @Singleton
    fun provideParintintinApiService(retrofit: Retrofit): ParintintinApiService =
        retrofit.create(ParintintinApiService::class.java)
}
