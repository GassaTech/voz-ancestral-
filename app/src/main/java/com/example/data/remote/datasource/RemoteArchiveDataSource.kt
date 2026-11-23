package com.example.data.remote.datasource

import com.example.core.common.AppResult
import com.example.core.common.RemoteErrorKind
import com.example.data.remote.api.ParintintinApiService
import com.example.data.remote.dto.ArchiveManifestDto
import com.example.data.remote.dto.CulturalArticleDto
import com.example.data.remote.dto.DictionaryEntryDto
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

@Singleton
class RemoteArchiveDataSource @Inject constructor(
    private val apiService: ParintintinApiService
) {
    suspend fun fetchArchiveManifest(): AppResult<ArchiveManifestDto> =
        try {
            val manifest = apiService.getArchiveManifest()
            when {
                manifest.version.isBlank() -> AppResult.Error(
                    message = "Manifesto remoto inválido: versão ausente ou vazia.",
                    remoteErrorKind = RemoteErrorKind.DESERIALIZATION_ERROR
                )
                manifest.entriesCount < 0 || manifest.articlesCount < 0 -> AppResult.Error(
                    message = "Manifesto remoto inválido: contagens não podem ser negativas.",
                    remoteErrorKind = RemoteErrorKind.DESERIALIZATION_ERROR
                )
                else -> AppResult.Success(manifest)
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            AppResult.Error(
                message = e.localizedMessage ?: "Falha ao consultar manifesto remoto do acervo.",
                cause = e,
                remoteErrorKind = classifyException(e)
            )
        }

    suspend fun fetchDictionaryEntries(): AppResult<List<DictionaryEntryDto>> =
        try {
            AppResult.Success(apiService.getDictionaryEntries())
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            AppResult.Error(
                message = e.localizedMessage ?: "Falha ao consultar API do dicionário.",
                cause = e,
                remoteErrorKind = classifyException(e)
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
                cause = e,
                remoteErrorKind = classifyException(e)
            )
        }

    internal fun classifyException(exception: Exception): RemoteErrorKind = when (exception) {
        is JsonDataException,
        is JsonEncodingException -> RemoteErrorKind.DESERIALIZATION_ERROR
        is UnknownHostException -> RemoteErrorKind.DNS_FAILURE
        is ConnectException -> RemoteErrorKind.CONNECTION_FAILURE
        is SocketTimeoutException -> RemoteErrorKind.TIMEOUT
        is SSLHandshakeException -> RemoteErrorKind.SSL_HANDSHAKE_FAILURE
        is HttpException -> when (exception.code()) {
            in 400..499 -> RemoteErrorKind.HTTP_CLIENT_ERROR
            in 500..599 -> RemoteErrorKind.HTTP_SERVER_ERROR
            else -> RemoteErrorKind.HTTP_OTHER_ERROR
        }
        is IOException -> RemoteErrorKind.IO_FAILURE
        else -> RemoteErrorKind.UNEXPECTED_ERROR
    }
}
