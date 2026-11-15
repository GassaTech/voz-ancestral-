package com.example.data.remote.api

import com.example.data.remote.dto.ArchiveManifestDto
import com.example.data.remote.dto.CulturalArticleDto
import com.example.data.remote.dto.DictionaryEntryDto
import retrofit2.http.GET

interface ParintintinApiService {
    @GET("v1/manifest.json")
    suspend fun getArchiveManifest(): ArchiveManifestDto

    @GET("v1/dictionary.json")
    suspend fun getDictionaryEntries(): List<DictionaryEntryDto>

    @GET("v1/articles.json")
    suspend fun getCulturalArticles(): List<CulturalArticleDto>
}
