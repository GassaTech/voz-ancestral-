package com.example.data.remote.datasource

import com.example.core.common.AppResult
import com.example.core.common.RemoteErrorKind
import com.example.data.mapper.toRemoteTerritoriesPage
import com.example.data.remote.api.ArcGisTerritoriesQuerySpec
import com.example.data.remote.api.IbamaTerritoriesApiService
import com.example.data.remote.dto.ArcGisErrorDto
import com.example.data.remote.model.RemoteTerritoriesPage
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import java.io.EOFException
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
class RemoteTerritoriesDataSource @Inject constructor(
    private val apiService: IbamaTerritoriesApiService
) {

    suspend fun fetchTerritoriesPage(
        resultOffset: Int = 0,
        resultRecordCount: Int = ArcGisTerritoriesQuerySpec.DEFAULT_PAGE_SIZE,
        where: String = ArcGisTerritoriesQuerySpec.DEFAULT_WHERE,
        outFields: String = ArcGisTerritoriesQuerySpec.DEFAULT_OUT_FIELDS,
        orderByFields: String = ArcGisTerritoriesQuerySpec.DEFAULT_ORDER_BY_FIELDS,
        returnGeometry: Boolean = ArcGisTerritoriesQuerySpec.DEFAULT_RETURN_GEOMETRY
    ): AppResult<RemoteTerritoriesPage> {
        if (resultOffset < 0) {
            return AppResult.Error(
                message = "Parâmetro resultOffset inválido: $resultOffset.",
                cause = IllegalArgumentException("resultOffset deve ser >= 0"),
                remoteErrorKind = RemoteErrorKind.UNEXPECTED_ERROR
            )
        }
        if (resultRecordCount <= 0) {
            return AppResult.Error(
                message = "Parâmetro resultRecordCount inválido: $resultRecordCount.",
                cause = IllegalArgumentException("resultRecordCount deve ser > 0"),
                remoteErrorKind = RemoteErrorKind.UNEXPECTED_ERROR
            )
        }

        return try {
            val response = apiService.queryIndigenousLands(
                where = where,
                outFields = outFields,
                returnGeometry = returnGeometry,
                orderByFields = orderByFields,
                resultOffset = resultOffset,
                resultRecordCount = resultRecordCount,
                format = ArcGisTerritoriesQuerySpec.DEFAULT_FORMAT
            )

            val arcGisError = response.error
            if (arcGisError != null) {
                return AppResult.Error(
                    message = formatArcGisErrorMessage(arcGisError),
                    remoteErrorKind = classifyArcGisErrorCode(arcGisError.code)
                )
            }

            if (response.features == null) {
                return AppResult.Error(
                    message = "Resposta ArcGIS inválida: campo 'features' ausente.",
                    remoteErrorKind = RemoteErrorKind.DESERIALIZATION_ERROR
                )
            }

            AppResult.Success(
                response.toRemoteTerritoriesPage(
                    requestedOffset = resultOffset,
                    requestedRecordCount = resultRecordCount
                )
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            AppResult.Error(
                message = e.localizedMessage ?: "Falha ao consultar camada territorial do IBAMA.",
                cause = e,
                remoteErrorKind = classifyException(e)
            )
        }
    }

    internal fun classifyArcGisErrorCode(code: Int?): RemoteErrorKind = when (code) {
        in 400..499 -> RemoteErrorKind.HTTP_CLIENT_ERROR
        in 500..599 -> RemoteErrorKind.HTTP_SERVER_ERROR
        else -> RemoteErrorKind.HTTP_OTHER_ERROR
    }

    internal fun classifyException(exception: Exception): RemoteErrorKind = when (exception) {
        is JsonDataException,
        is JsonEncodingException,
        is EOFException -> RemoteErrorKind.DESERIALIZATION_ERROR
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

    private fun formatArcGisErrorMessage(error: ArcGisErrorDto): String {
        val codePart = error.code?.let { "código $it" }
        val messagePart = error.message?.trim()?.takeIf { it.isNotEmpty() }
        val detailsPart = error.details
            ?.mapNotNull { it?.trim()?.takeIf { item -> item.isNotEmpty() } }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString("; ")

        val parts = listOfNotNull(codePart, messagePart, detailsPart)
        return if (parts.isEmpty()) {
            "Erro retornado pela API ArcGIS."
        } else {
            "Erro da API ArcGIS (${parts.joinToString(" - ")})."
        }
    }
}
