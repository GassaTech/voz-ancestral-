package com.example.data.remote.datasource

import com.example.core.common.AppResult
import com.example.core.common.RemoteErrorKind
import com.example.data.remote.api.IbamaTerritoriesApiService
import com.example.data.remote.dto.ArcGisErrorDto
import com.example.data.remote.dto.ArcGisFeatureQueryResponseDto
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RemoteTerritoriesDataSourceTest {

    @Test
    fun `classifica corretamente excecoes de rede SSL timeout e desserializacao`() = runTest {
        val cases = listOf(
            UnknownHostException("pamgia.ibama.gov.br") to RemoteErrorKind.DNS_FAILURE,
            ConnectException("Connection refused") to RemoteErrorKind.CONNECTION_FAILURE,
            SocketTimeoutException("timeout") to RemoteErrorKind.TIMEOUT,
            SSLHandshakeException("handshake failed") to RemoteErrorKind.SSL_HANDSHAKE_FAILURE,
            EOFException("unexpected end of stream") to RemoteErrorKind.DESERIALIZATION_ERROR,
            IOException("stream reset") to RemoteErrorKind.IO_FAILURE,
            JsonDataException("tipo inválido") to RemoteErrorKind.DESERIALIZATION_ERROR,
            JsonEncodingException("json malformado") to RemoteErrorKind.DESERIALIZATION_ERROR
        )

        cases.forEach { (exception, expectedKind) ->
            val dataSource = RemoteTerritoriesDataSource(FailingTerritoriesApiService(exception))
            val result = dataSource.fetchTerritoriesPage()

            assertTrue(result is AppResult.Error)
            val error = result as AppResult.Error
            assertEquals(expectedKind, error.remoteErrorKind)
            assertEquals(exception, error.cause)
        }
    }

    @Test
    fun `classifica HttpException 4xx 5xx e codigos ArcGIS desconhecidos`() = runTest {
        val http400 = HttpException(
            Response.error<ArcGisFeatureQueryResponseDto>(
                400,
                "{}".toResponseBody("application/json".toMediaType())
            )
        )
        val http502 = HttpException(
            Response.error<ArcGisFeatureQueryResponseDto>(
                502,
                "{}".toResponseBody("application/json".toMediaType())
            )
        )

        val err400 = RemoteTerritoriesDataSource(FailingTerritoriesApiService(http400))
            .fetchTerritoriesPage() as AppResult.Error
        assertEquals(RemoteErrorKind.HTTP_CLIENT_ERROR, err400.remoteErrorKind)

        val err502 = RemoteTerritoriesDataSource(FailingTerritoriesApiService(http502))
            .fetchTerritoriesPage() as AppResult.Error
        assertEquals(RemoteErrorKind.HTTP_SERVER_ERROR, err502.remoteErrorKind)

        val unknownArcGisErrorSource = RemoteTerritoriesDataSource(
            object : IbamaTerritoriesApiService {
                override suspend fun queryIndigenousLands(
                    where: String,
                    outFields: String,
                    returnGeometry: Boolean,
                    orderByFields: String,
                    resultOffset: Int,
                    resultRecordCount: Int,
                    format: String
                ) = ArcGisFeatureQueryResponseDto(
                    error = ArcGisErrorDto(code = 999, message = "Token error")
                )
            }
        )
        val unknownResult = unknownArcGisErrorSource.fetchTerritoriesPage() as AppResult.Error
        assertEquals(RemoteErrorKind.HTTP_OTHER_ERROR, unknownResult.remoteErrorKind)
    }

    @Test
    fun `rejeita parametros de paginacao invalidos sem chamar a API`() = runTest {
        var called = false
        val spyService = object : IbamaTerritoriesApiService {
            override suspend fun queryIndigenousLands(
                where: String,
                outFields: String,
                returnGeometry: Boolean,
                orderByFields: String,
                resultOffset: Int,
                resultRecordCount: Int,
                format: String
            ): ArcGisFeatureQueryResponseDto {
                called = true
                return ArcGisFeatureQueryResponseDto(features = emptyList())
            }
        }
        val dataSource = RemoteTerritoriesDataSource(spyService)

        val negativeOffset = dataSource.fetchTerritoriesPage(resultOffset = -1)
        val zeroLimit = dataSource.fetchTerritoriesPage(resultRecordCount = 0)

        assertTrue(negativeOffset is AppResult.Error)
        assertTrue(zeroLimit is AppResult.Error)
        assertTrue(!called)
    }

    @Test
    fun `relanca CancellationException imediatamente sem converter em AppResult Error`() = runTest {
        val dataSource = RemoteTerritoriesDataSource(
            FailingTerritoriesApiService(CancellationException("Operação cancelada"))
        )

        try {
            dataSource.fetchTerritoriesPage()
            fail("Era esperado relançar CancellationException")
        } catch (_: CancellationException) {
            // Esperado
        }
    }

    private class FailingTerritoriesApiService(
        private val failure: Throwable
    ) : IbamaTerritoriesApiService {
        override suspend fun queryIndigenousLands(
            where: String,
            outFields: String,
            returnGeometry: Boolean,
            orderByFields: String,
            resultOffset: Int,
            resultRecordCount: Int,
            format: String
        ): ArcGisFeatureQueryResponseDto = throw failure
    }
}
