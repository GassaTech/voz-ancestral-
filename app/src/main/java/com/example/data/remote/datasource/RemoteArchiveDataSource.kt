package com.example.data.remote.datasource

import com.example.core.common.AppResult
import com.example.data.remote.api.ParintintinApiService
import com.example.data.remote.dto.CulturalArticleDto
import com.example.data.remote.dto.DictionaryEntryDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteArchiveDataSource @Inject constructor(
    private val apiService: ParintintinApiService
) {
    suspend fun fetchDictionaryEntries(): AppResult<List<DictionaryEntryDto>> =
        runCatching { apiService.getDictionaryEntries() }
            .fold(
                onSuccess = { AppResult.Success(it) },
                onFailure = { error ->
                    AppResult.Error(
                        message = error.localizedMessage ?: "Falha ao consultar API do dicionário.",
                        cause = error
                    )
                }
            )

    suspend fun fetchCulturalArticles(): AppResult<List<CulturalArticleDto>> =
        runCatching { apiService.getCulturalArticles() }
            .fold(
                onSuccess = { AppResult.Success(it) },
                onFailure = { error ->
                    AppResult.Error(
                        message = error.localizedMessage ?: "Falha ao consultar API da enciclopédia.",
                        cause = error
                    )
                }
            )
}
