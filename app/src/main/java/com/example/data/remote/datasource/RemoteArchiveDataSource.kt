package com.example.data.remote.datasource

import com.example.core.common.AppResult
import com.example.data.remote.api.ParintintinApiService
import com.example.data.remote.dto.CulturalArticleDto
import com.example.data.remote.dto.DictionaryEntryDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class RemoteArchiveDataSource @Inject constructor(
    private val apiService: ParintintinApiService
) {
    suspend fun fetchDictionaryEntries(): AppResult<List<DictionaryEntryDto>> =
        try {
            AppResult.Success(apiService.getDictionaryEntries())
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            AppResult.Error(
                message = e.localizedMessage ?: "Falha ao consultar API do dicionário.",
                cause = e
            )
        }

    suspend fun fetchCulturalArticles(): AppResult<List<CulturalArticleDto>> =
        try {
            AppResult.Success(apiService.getCulturalArticles())
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            AppResult.Error(
                message = e.localizedMessage ?: "Falha ao consultar API da enciclopédia.",
                cause = e
            )
        }
}
