package com.example.data.remote.api

import com.example.core.common.AppResult
import com.example.core.common.RemoteErrorKind
import com.example.data.remote.datasource.RemoteTerritoriesDataSource
import com.example.data.remote.dto.ArcGisErrorDto
import com.example.data.remote.dto.ArcGisFeatureDto
import com.example.data.remote.dto.ArcGisFeatureQueryResponseDto
import com.example.data.remote.dto.ArcGisFieldDto
import com.example.data.remote.dto.IndigenousLandAttributesDto
import com.example.di.NetworkModule
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import java.io.EOFException
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class IbamaTerritoriesApiContractTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var moshi: Moshi
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var apiService: IbamaTerritoriesApiService
    private lateinit var dataSource: RemoteTerritoriesDataSource

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        moshi = NetworkModule.provideMoshi()
        okHttpClient = NetworkModule.provideOkHttpClient()

        apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(IbamaTerritoriesApiService::class.java)

        dataSource = RemoteTerritoriesDataSource(apiService)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `NetworkModule aponta para o endpoint oficial do IBAMA SIGAGEO camada 25`() {
        val retrofit = NetworkModule.provideRetrofit(okHttpClient, moshi)

        assertEquals(
            "https://pamgia.ibama.gov.br/server/rest/services/SIGAGEO/bases/MapServer/25/",
            retrofit.baseUrl().toString()
        )
    }

    @Test
    fun `adaptadores Moshi gerados pelo KSP estao ativos para os DTOs ArcGIS`() {
        val responseAdapter = moshi.adapter(ArcGisFeatureQueryResponseDto::class.java)
        val fieldAdapter = moshi.adapter(ArcGisFieldDto::class.java)
        val errorAdapter = moshi.adapter(ArcGisErrorDto::class.java)
        val featureAdapter = moshi.adapter(ArcGisFeatureDto::class.java)
        val attributesAdapter = moshi.adapter(IndigenousLandAttributesDto::class.java)

        assertTrue(responseAdapter.toString().contains("GeneratedJsonAdapter(ArcGisFeatureQueryResponseDto)"))
        assertTrue(fieldAdapter.toString().contains("GeneratedJsonAdapter(ArcGisFieldDto)"))
        assertTrue(errorAdapter.toString().contains("GeneratedJsonAdapter(ArcGisErrorDto)"))
        assertTrue(featureAdapter.toString().contains("GeneratedJsonAdapter(ArcGisFeatureDto)"))
        assertTrue(attributesAdapter is com.example.data.remote.dto.IndigenousLandAttributesJsonAdapter)
    }

    @Test
    fun `resposta valida com registros envia parametros padrao e mapeia atributos corretamente`() = runTest {
        mockWebServer.enqueue(jsonResponse(VALID_SINGLE_TERRITORY_JSON))

        val result = dataSource.fetchTerritoriesPage()
        val request = mockWebServer.takeRequest()
        val url = request.requestUrl!!

        assertEquals("GET", request.method)
        assertEquals("/query", url.encodedPath)
        assertEquals("1=1", url.queryParameter("where"))
        assertEquals(
            "objectid,terrai_cod,terrai_nom,etnia_nome,municipio_,uf_sigla,superficie,fase_ti,modalidade,data_atual",
            url.queryParameter("outFields")
        )
        assertTrue(url.queryParameter("outFields")!!.split(",").contains("objectid"))
        assertEquals("false", url.queryParameter("returnGeometry"))
        assertEquals("terrai_cod ASC, objectid ASC", url.queryParameter("orderByFields"))
        assertEquals("0", url.queryParameter("resultOffset"))
        assertEquals("100", url.queryParameter("resultRecordCount"))
        assertEquals("json", url.queryParameter("f"))

        assertTrue(result is AppResult.Success)
        val page = (result as AppResult.Success).data
        assertEquals(0, page.requestedOffset)
        assertEquals(100, page.requestedRecordCount)
        assertFalse(page.exceededTransferLimit)
        assertFalse(page.hasMorePages)
        assertNull(page.nextOffset)
        assertEquals(1, page.items.size)

        val item = page.items.single()
        assertEquals("29701", item.territoryCode)
        assertEquals("Nove de Janeiro", item.name)
        assertEquals("Parintintin", item.rawEthnicity)
        assertEquals(listOf("Parintintin"), item.ethnicities)
        assertEquals("Humaitá", item.rawMunicipality)
        assertEquals(listOf("Humaitá"), item.municipalities)
        assertEquals("AM", item.rawStateUf)
        assertEquals(listOf("AM"), item.statesUf)
        assertEquals(228777.08, item.surfaceArea ?: 0.0, 0.001)
        assertEquals("Regularizada", item.phase)
        assertEquals("Tradicionalmente Ocupada", item.modality)
        assertEquals("2024-08-15", item.updatedAtRaw)
    }

    @Test
    fun `resposta valida sem registros retorna lista vazia sem erro e sem proxima pagina`() = runTest {
        val emptyFeaturesJson = """
            {
              "fields": [],
              "features": [],
              "exceededTransferLimit": false
            }
        """.trimIndent()
        mockWebServer.enqueue(jsonResponse(emptyFeaturesJson))

        val result = dataSource.fetchTerritoriesPage(resultOffset = 0, resultRecordCount = 100)

        assertTrue(result is AppResult.Success)
        val page = (result as AppResult.Success).data
        assertTrue(page.items.isEmpty())
        assertFalse(page.exceededTransferLimit)
        assertFalse(page.hasMorePages)
        assertNull(page.nextOffset)
    }

    @Test
    fun `campos nulos ou vazios sao tratados com seguranca sem inventar valores`() = runTest {
        val sparseJson = """
            {
              "fields": [],
              "features": [
                {
                  "attributes": null
                },
                {
                  "attributes": {
                    "terrai_cod": 45001,
                    "terrai_nom": "   ",
                    "etnia_nome": null,
                    "municipio_": "",
                    "uf_sigla": null,
                    "superficie": "",
                    "fase_ti": null,
                    "modalidade": "   ",
                    "data_atual": 1730000000000
                  }
                }
              ]
            }
        """.trimIndent()
        mockWebServer.enqueue(jsonResponse(sparseJson))

        val result = dataSource.fetchTerritoriesPage()

        assertTrue(result is AppResult.Success)
        val page = (result as AppResult.Success).data
        assertEquals(1, page.items.size)
        val item = page.items.single()
        assertEquals("45001", item.territoryCode)
        assertNull(item.name)
        assertNull(item.rawEthnicity)
        assertTrue(item.ethnicities.isEmpty())
        assertNull(item.rawMunicipality)
        assertTrue(item.municipalities.isEmpty())
        assertNull(item.rawStateUf)
        assertTrue(item.statesUf.isEmpty())
        assertNull(item.surfaceArea)
        assertNull(item.phase)
        assertNull(item.modality)
        assertEquals("1730000000000", item.updatedAtRaw)
    }

    @Test
    fun `resposta contendo multiplas etnias em um unico campo preserva bruto e separa lista normalizada`() = runTest {
        val multiEthnicityJson = """
            {
              "fields": [],
              "features": [
                {
                  "attributes": {
                    "terrai_cod": "10203",
                    "terrai_nom": "Terra Multiétnica Exemplo",
                    "etnia_nome": "Parintintin, Tenharim; Jiahui / Torá | Parintintin",
                    "municipio_": "Humaitá, Manicoré",
                    "uf_sigla": "am / ro",
                    "superficie": "15432,5",
                    "fase_ti": "Declarada",
                    "modalidade": "Tradicionalmente Ocupada",
                    "data_atual": null
                  }
                }
              ],
              "exceededTransferLimit": false
            }
        """.trimIndent()
        mockWebServer.enqueue(jsonResponse(multiEthnicityJson))

        val result = dataSource.fetchTerritoriesPage()

        assertTrue(result is AppResult.Success)
        val item = (result as AppResult.Success).data.items.single()
        assertEquals("Parintintin, Tenharim; Jiahui / Torá | Parintintin", item.rawEthnicity)
        assertEquals(listOf("Parintintin", "Tenharim", "Jiahui", "Torá"), item.ethnicities)
        assertEquals(listOf("Humaitá", "Manicoré"), item.municipalities)
        assertEquals(listOf("AM", "RO"), item.statesUf)
        assertEquals(15432.5, item.surfaceArea ?: 0.0, 0.001)
    }

    @Test
    fun `erro HTTP 404 e 500 retorna AppResult Error com classificacao adequada`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))
        val notFoundResult = dataSource.fetchTerritoriesPage()
        assertTrue(notFoundResult is AppResult.Error)
        val notFoundError = notFoundResult as AppResult.Error
        assertEquals(RemoteErrorKind.HTTP_CLIENT_ERROR, notFoundError.remoteErrorKind)
        assertTrue(notFoundError.cause is HttpException)
        assertEquals(404, (notFoundError.cause as HttpException).code())

        mockWebServer.enqueue(MockResponse().setResponseCode(503).setBody("Service Unavailable"))
        val serverErrorResult = dataSource.fetchTerritoriesPage()
        assertTrue(serverErrorResult is AppResult.Error)
        val serverError = serverErrorResult as AppResult.Error
        assertEquals(RemoteErrorKind.HTTP_SERVER_ERROR, serverError.remoteErrorKind)
        assertTrue(serverError.cause is HttpException)
        assertEquals(503, (serverError.cause as HttpException).code())
    }

    @Test
    fun `erro ArcGIS dentro de resposta HTTP 200 e tratado como AppResult Error`() = runTest {
        val arcGisClientErrorJson = """
            {
              "error": {
                "code": 400,
                "message": "Unable to complete operation.",
                "details": ["'outFields' parameter is invalid."]
              }
            }
        """.trimIndent()
        mockWebServer.enqueue(jsonResponse(arcGisClientErrorJson))

        val clientErrorResult = dataSource.fetchTerritoriesPage()
        assertTrue(clientErrorResult is AppResult.Error)
        val clientError = clientErrorResult as AppResult.Error
        assertEquals(RemoteErrorKind.HTTP_CLIENT_ERROR, clientError.remoteErrorKind)
        assertTrue(clientError.message.contains("400"))
        assertTrue(clientError.message.contains("Unable to complete operation."))
        assertTrue(clientError.message.contains("'outFields' parameter is invalid."))

        val arcGisServerErrorJson = """
            {
              "error": {
                "code": 500,
                "message": "Internal MapServer error.",
                "details": []
              }
            }
        """.trimIndent()
        mockWebServer.enqueue(jsonResponse(arcGisServerErrorJson))

        val serverErrorResult = dataSource.fetchTerritoriesPage()
        assertTrue(serverErrorResult is AppResult.Error)
        val serverError = serverErrorResult as AppResult.Error
        assertEquals(RemoteErrorKind.HTTP_SERVER_ERROR, serverError.remoteErrorKind)
        assertTrue(serverError.message.contains("500"))
    }

    @Test
    fun `primeira pagina envia resultOffset 0 e resultRecordCount configurado`() = runTest {
        mockWebServer.enqueue(jsonResponse(pageJson(codes = listOf("001", "002"), exceeded = true)))

        val result = dataSource.fetchTerritoriesPage(resultOffset = 0, resultRecordCount = 2)
        val request = mockWebServer.takeRequest()

        assertEquals("0", request.requestUrl?.queryParameter("resultOffset"))
        assertEquals("2", request.requestUrl?.queryParameter("resultRecordCount"))
        assertTrue(result is AppResult.Success)
        val page = (result as AppResult.Success).data
        assertEquals(0, page.requestedOffset)
        assertEquals(2, page.requestedRecordCount)
        assertEquals(listOf("001", "002"), page.items.map { it.territoryCode })
    }

    @Test
    fun `segunda pagina envia resultOffset atualizado e retorna proximos registros`() = runTest {
        mockWebServer.enqueue(jsonResponse(pageJson(codes = listOf("001", "002"), exceeded = true)))
        mockWebServer.enqueue(jsonResponse(pageJson(codes = listOf("003"), exceeded = false)))

        val firstPage = (dataSource.fetchTerritoriesPage(resultOffset = 0, resultRecordCount = 2)
            as AppResult.Success).data
        val secondPageResult = dataSource.fetchTerritoriesPage(
            resultOffset = firstPage.nextOffset!!,
            resultRecordCount = 2
        )

        val firstRequest = mockWebServer.takeRequest()
        val secondRequest = mockWebServer.takeRequest()

        assertEquals("0", firstRequest.requestUrl?.queryParameter("resultOffset"))
        assertEquals("2", secondRequest.requestUrl?.queryParameter("resultOffset"))
        assertEquals("2", secondRequest.requestUrl?.queryParameter("resultRecordCount"))

        assertTrue(secondPageResult is AppResult.Success)
        val secondPage = (secondPageResult as AppResult.Success).data
        assertEquals(2, secondPage.requestedOffset)
        assertEquals(listOf("003"), secondPage.items.map { it.territoryCode })
        assertFalse(secondPage.hasMorePages)
        assertNull(secondPage.nextOffset)
    }

    @Test
    fun `resposta com exceededTransferLimit calcula nextOffset e evita loop infinito se features vazio`() = runTest {
        mockWebServer.enqueue(jsonResponse(pageJson(codes = listOf("101", "102", "103"), exceeded = true)))

        val firstResult = dataSource.fetchTerritoriesPage(resultOffset = 10, resultRecordCount = 3)
        assertTrue(firstResult is AppResult.Success)
        val page = (firstResult as AppResult.Success).data
        assertTrue(page.exceededTransferLimit)
        assertTrue(page.hasMorePages)
        assertEquals(13, page.nextOffset)

        val emptyExceededJson = """
            {
              "features": [],
              "exceededTransferLimit": true
            }
        """.trimIndent()
        mockWebServer.enqueue(jsonResponse(emptyExceededJson))

        val emptyExceededResult = dataSource.fetchTerritoriesPage(resultOffset = 13, resultRecordCount = 3)
        assertTrue(emptyExceededResult is AppResult.Success)
        val emptyPage = (emptyExceededResult as AppResult.Success).data
        assertTrue(emptyPage.exceededTransferLimit)
        assertFalse(emptyPage.hasMorePages)
        assertNull(emptyPage.nextOffset)
    }

    @Test
    fun `paginacao com cinco features incluindo uma com attributes nulo avanca offset em cinco posicoes`() = runTest {
        val pageOneWithNullAttributesJson = """
            {
              "fields": [],
              "features": [
                { "attributes": { "objectid": 1, "terrai_cod": "001", "terrai_nom": "TI 001" } },
                { "attributes": null },
                { "attributes": { "objectid": 3, "terrai_cod": "003", "terrai_nom": "TI 003" } },
                { "attributes": { "objectid": 4, "terrai_cod": "004", "terrai_nom": "TI 004" } },
                { "attributes": { "objectid": 5, "terrai_cod": "005", "terrai_nom": "TI 005" } }
              ],
              "exceededTransferLimit": true
            }
        """.trimIndent()
        mockWebServer.enqueue(jsonResponse(pageOneWithNullAttributesJson))
        mockWebServer.enqueue(jsonResponse(pageJson(codes = listOf("006"), exceeded = false)))

        val firstPage = (dataSource.fetchTerritoriesPage(resultOffset = 0, resultRecordCount = 5)
            as AppResult.Success).data
        assertEquals(4, firstPage.items.size)
        assertEquals(1L, firstPage.items.first().objectId)
        assertTrue(firstPage.hasMorePages)
        assertEquals(5, firstPage.nextOffset)

        val secondPage = (dataSource.fetchTerritoriesPage(
            resultOffset = firstPage.nextOffset!!,
            resultRecordCount = 5
        ) as AppResult.Success).data
        assertEquals(listOf("006"), secondPage.items.map { it.territoryCode })
        assertFalse(secondPage.hasMorePages)

        val firstRequest = mockWebServer.takeRequest()
        val secondRequest = mockWebServer.takeRequest()
        assertEquals("0", firstRequest.requestUrl?.queryParameter("resultOffset"))
        assertEquals("5", secondRequest.requestUrl?.queryParameter("resultOffset"))
    }

    @Test
    fun `resposta HTTP 200 sem features e sem error ou JSON malformado gera DESERIALIZATION_ERROR`() = runTest {
        mockWebServer.enqueue(jsonResponse("""{"fields":[]}"""))
        val missingFeaturesResult = dataSource.fetchTerritoriesPage()
        assertTrue(missingFeaturesResult is AppResult.Error)
        assertEquals(
            RemoteErrorKind.DESERIALIZATION_ERROR,
            (missingFeaturesResult as AppResult.Error).remoteErrorKind
        )

        mockWebServer.enqueue(jsonResponse("""{ "features": [ """))
        val truncatedResult = dataSource.fetchTerritoriesPage()
        assertTrue(truncatedResult is AppResult.Error)
        val truncatedError = truncatedResult as AppResult.Error
        assertEquals(RemoteErrorKind.DESERIALIZATION_ERROR, truncatedError.remoteErrorKind)
        assertTrue(truncatedError.cause is EOFException)

        mockWebServer.enqueue(jsonResponse("<html><body>502 Bad Gateway</body></html>"))
        val htmlResult = dataSource.fetchTerritoriesPage()
        assertTrue(htmlResult is AppResult.Error)
        val htmlError = htmlResult as AppResult.Error
        assertEquals(RemoteErrorKind.DESERIALIZATION_ERROR, htmlError.remoteErrorKind)
        assertTrue(htmlError.cause is JsonEncodingException)
    }

    private fun jsonResponse(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json; charset=utf-8")
        .setBody(body)

    private fun pageJson(codes: List<String>, exceeded: Boolean): String {
        val features = codes.joinToString(",") { code ->
            """
            {
              "attributes": {
                "terrai_cod": "$code",
                "terrai_nom": "Terra Indígena $code",
                "etnia_nome": "Etnia $code",
                "municipio_": "Município $code",
                "uf_sigla": "AM",
                "superficie": 1000.0,
                "fase_ti": "Regularizada",
                "modalidade": "Tradicionalmente Ocupada",
                "data_atual": "2025-01-10"
              }
            }
            """.trimIndent()
        }
        return """
            {
              "fields": [],
              "features": [$features],
              "exceededTransferLimit": $exceeded
            }
        """.trimIndent()
    }

    private companion object {
        val VALID_SINGLE_TERRITORY_JSON = """
            {
              "displayFieldName": "terrai_nom",
              "fieldAliases": {
                "terrai_cod": "Código"
              },
              "fields": [
                {
                  "name": "terrai_cod",
                  "type": "esriFieldTypeString",
                  "alias": "Código"
                }
              ],
              "features": [
                {
                  "attributes": {
                    "terrai_cod": "29701",
                    "terrai_nom": "Nove de Janeiro",
                    "etnia_nome": "Parintintin",
                    "municipio_": "Humaitá",
                    "uf_sigla": "AM",
                    "superficie": 228777.08,
                    "fase_ti": "Regularizada",
                    "modalidade": "Tradicionalmente Ocupada",
                    "data_atual": "2024-08-15",
                    "campo_extra_nao_mapeado": "ignorar_sem_erro"
                  }
                }
              ],
              "exceededTransferLimit": false
            }
        """.trimIndent()
    }
}
