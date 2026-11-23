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

class RemoteArchiveDataSourceTest {

    @Test
    fun `distingue UnknownHostException ConnectException SocketTimeoutException SSLHandshakeException e IOException`() = runTest {
        val cases = listOf(
            UnknownHostException("archive.parintintin.org") to RemoteErrorKind.DNS_FAILURE,
            ConnectException("Connection refused") to RemoteErrorKind.CONNECTION_FAILURE,
            SocketTimeoutException("timeout") to RemoteErrorKind.TIMEOUT,
            SSLHandshakeException("handshake failed") to RemoteErrorKind.SSL_HANDSHAKE_FAILURE,
            IOException("stream reset") to RemoteErrorKind.IO_FAILURE
        )

        cases.forEach { (exception, expectedKind) ->
            val dataSource = RemoteArchiveDataSource(
                FailingApiService(exception)
            )
            val result = dataSource.fetchDictionaryEntries()
            assertTrue(result is AppResult.Error)
            val error = result as AppResult.Error
            assertEquals(expectedKind, error.remoteErrorKind)
            assertEquals(exception, error.cause)
        }
    }

    @Test
    fun `distingue HttpException 4xx e 5xx e erros de desserializacao`() = runTest {
        val http404 = HttpException(
            Response.error<List<DictionaryEntryDto>>(
                404,
                "{}".toResponseBody("application/json".toMediaType())
            )
        )
        val http500 = HttpException(
            Response.error<List<DictionaryEntryDto>>(
                500,
                "{}".toResponseBody("application/json".toMediaType())
            )
        )
        val cases = listOf(
            http404 to RemoteErrorKind.HTTP_CLIENT_ERROR,
            http500 to RemoteErrorKind.HTTP_SERVER_ERROR,
            JsonDataException("campo obrigatório ausente") to RemoteErrorKind.DESERIALIZATION_ERROR,
            JsonEncodingException("json inválido") to RemoteErrorKind.DESERIALIZATION_ERROR
        )

        cases.forEach { (exception, expectedKind) ->
            val dataSource = RemoteArchiveDataSource(FailingApiService(exception))
            val result = dataSource.fetchDictionaryEntries()
            assertTrue(result is AppResult.Error)
            assertEquals(expectedKind, (result as AppResult.Error).remoteErrorKind)
        }
    }

    @Test
    fun `relanca CancellationException imediatamente sem converter em AppResult Error`() = runTest {
        val dataSource = RemoteArchiveDataSource(
            FailingApiService(CancellationException("Cancelado"))
        )

        try {
            dataSource.fetchDictionaryEntries()
            fail("Era esperado relançar CancellationException")
        } catch (_: CancellationException) {
            // Esperado
        }
    }

    @Test
    fun `fetchArchiveManifest retorna sucesso para manifesto valido`() = runTest {
        val expected = ArchiveManifestDto(version = "1.4.2", entriesCount = 40, articlesCount = 0)
        val dataSource = RemoteArchiveDataSource(
            ConfigurableApiService(manifestProvider = { expected })
        )

        val result = dataSource.fetchArchiveManifest()

        assertEquals(AppResult.Success(expected), result)
    }

    @Test
    fun `fetchArchiveManifest rejeita versao em branco ou contagens negativas como DESERIALIZATION_ERROR`() = runTest {
        val invalidManifests = listOf(
            ArchiveManifestDto(version = "", entriesCount = 10, articlesCount = 0),
            ArchiveManifestDto(version = "   ", entriesCount = 10, articlesCount = 0),
            ArchiveManifestDto(version = "1.0.0", entriesCount = -1, articlesCount = 0),
            ArchiveManifestDto(version = "1.0.0", entriesCount = 10, articlesCount = -2)
        )

        invalidManifests.forEach { invalid ->
            val dataSource = RemoteArchiveDataSource(
                ConfigurableApiService(manifestProvider = { invalid })
            )
            val result = dataSource.fetchArchiveManifest()
            assertTrue(result is AppResult.Error)
            assertEquals(RemoteErrorKind.DESERIALIZATION_ERROR, (result as AppResult.Error).remoteErrorKind)
        }
    }

    @Test
    fun `fetchArchiveManifest classifica falhas de rede HTTP e relanca CancellationException`() = runTest {
        val networkResult = RemoteArchiveDataSource(
            FailingApiService(SocketTimeoutException("timeout manifesto"))
        ).fetchArchiveManifest()
        assertTrue(networkResult is AppResult.Error)
        assertEquals(RemoteErrorKind.TIMEOUT, (networkResult as AppResult.Error).remoteErrorKind)

        val cancelSource = RemoteArchiveDataSource(
            FailingApiService(CancellationException("Cancelado no manifesto"))
        )
        try {
            cancelSource.fetchArchiveManifest()
            fail("Era esperado relançar CancellationException")
        } catch (_: CancellationException) {
            // Esperado
        }
    }

    @Test
    fun `fetchCulturalArticles classifica falhas e relanca CancellationException`() = runTest {
        val httpError = HttpException(
            Response.error<List<CulturalArticleDto>>(
                503,
                "{}".toResponseBody("application/json".toMediaType())
            )
        )
        val errorResult = RemoteArchiveDataSource(FailingApiService(httpError)).fetchCulturalArticles()
        assertTrue(errorResult is AppResult.Error)
        assertEquals(RemoteErrorKind.HTTP_SERVER_ERROR, (errorResult as AppResult.Error).remoteErrorKind)

        val cancelSource = RemoteArchiveDataSource(
            FailingApiService(CancellationException("Cancelado nos artigos"))
        )
        try {
            cancelSource.fetchCulturalArticles()
            fail("Era esperado relançar CancellationException")
        } catch (_: CancellationException) {
            // Esperado
        }
    }

    private class FailingApiService(
        private val failure: Throwable
    ) : ParintintinApiService {
        override suspend fun getArchiveManifest(): ArchiveManifestDto = throw failure
        override suspend fun getDictionaryEntries(): List<DictionaryEntryDto> = throw failure
        override suspend fun getCulturalArticles(): List<CulturalArticleDto> = throw failure
    }

    private class ConfigurableApiService(
        private val manifestProvider: suspend () -> ArchiveManifestDto
    ) : ParintintinApiService {
        override suspend fun getArchiveManifest(): ArchiveManifestDto = manifestProvider()
        override suspend fun getDictionaryEntries(): List<DictionaryEntryDto> = emptyList()
        override suspend fun getCulturalArticles(): List<CulturalArticleDto> = emptyList()
    }
}
