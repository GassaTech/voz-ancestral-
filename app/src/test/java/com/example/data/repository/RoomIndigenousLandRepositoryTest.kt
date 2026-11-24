package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.core.common.RemoteErrorKind
import com.example.data.local.database.ParintintinDatabase
import com.example.data.remote.api.IbamaTerritoriesApiService
import com.example.data.remote.datasource.RemoteTerritoriesDataSource
import com.example.data.remote.dto.IndigenousLandAttributesDto
import com.example.data.remote.dto.IndigenousLandAttributesJsonAdapter
import com.example.domain.model.IndigenousLand
import com.example.domain.model.RemoteSyncOutcome
import com.example.domain.model.TerritoryFilter
import com.example.domain.model.TerritorySortDirection
import com.example.domain.model.TerritorySortField
import com.example.domain.model.TerritorySortOption
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomIndigenousLandRepositoryTest {

    private lateinit var database: ParintintinDatabase
    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: RoomIndigenousLandRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ParintintinDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mockWebServer = MockWebServer().apply { start() }
        val moshi = Moshi.Builder()
            .add(IndigenousLandAttributesDto::class.java, IndigenousLandAttributesJsonAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(IbamaTerritoriesApiService::class.java)

        repository = RoomIndigenousLandRepository(
            database = database,
            indigenousLandDao = database.indigenousLandDao(),
            remoteDataSource = RemoteTerritoriesDataSource(apiService),
            dispatchers = IoDispatcherProvider
        )
    }

    @After
    fun tearDown() {
        runCatching { mockWebServer.shutdown() }
        if (database.isOpen) {
            database.close()
        }
    }

    @Test
    fun `repositorio persiste consulta filtra e atualiza territorios via Room`() = runBlocking {
        val ipixuna = IndigenousLand(
            id = "terrai:101",
            objectId = 1L,
            territoryCode = "101",
            name = "Ipixuna",
            rawEthnicity = "Parintintin, Kagwahiva",
            ethnicities = listOf("Parintintin", "Kagwahiva"),
            rawMunicipality = "Humaitá",
            municipalities = listOf("Humaitá"),
            rawStateUf = "AM",
            statesUf = listOf("AM"),
            surfaceArea = 215362.0,
            phase = "Regularizada",
            modality = "Tradicionalmente Ocupada",
            updatedAtRaw = "2024-08-19"
        )
        val semCampos = IndigenousLand(
            id = "oid:999",
            objectId = 999L,
            territoryCode = null,
            name = "Área Sem Código",
            rawEthnicity = null,
            ethnicities = emptyList(),
            rawMunicipality = null,
            municipalities = emptyList(),
            rawStateUf = "RO",
            statesUf = listOf("RO"),
            surfaceArea = null,
            phase = null,
            modality = null,
            updatedAtRaw = null
        )

        repository.upsertAll(listOf(ipixuna, semCampos))

        assertEquals(2, repository.observeTotalCount().first())
        assertEquals(2, repository.countOnce())

        val filteredByNameAndUf = repository.observeTerritories(
            query = "  Ipixuna  ",
            stateUf = " am "
        ).first()
        assertEquals(listOf("terrai:101"), filteredByNameAndUf.map { it.id })

        val foundByCode = repository.findByTerritoryCode(" 101 ")
        assertNotNull(foundByCode)
        assertEquals("Ipixuna", foundByCode?.name)

        val updated = repository.update(ipixuna.copy(phase = "Homologada"))
        assertTrue(updated)
        assertEquals("Homologada", repository.observeById("terrai:101").first()?.phase)

        val missingUpdate = repository.update(ipixuna.copy(id = "inexistente", territoryCode = "999"))
        assertFalse(missingUpdate)

        assertNull(repository.findByTerritoryCode("   "))

        repository.deleteAll()
        assertEquals(0, repository.observeAll().first().size)
    }

    @Test
    fun `syncFromRemote percorre todas as paginas sem contagem fixa deduplica e persiste no Room`() = runBlocking {
        // Página 1 (offset 0, 2 registros brutos sendo 1 com attributes null + exceededTransferLimit=true)
        // Testa avanço pelo tamanho bruto (requestedOffset + 2 = 2)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "exceededTransferLimit": true,
                      "features": [
                        {
                          "attributes": {
                            "objectid": 10,
                            "terrai_cod": "101",
                            "terrai_nom": "Ipixuna",
                            "etnia_nome": "Parintintin",
                            "municipio_": "Humaitá",
                            "uf_sigla": "AM",
                            "superficie": 215362.0,
                            "fase_ti": "Regularizada",
                            "modalidade": "Tradicionalmente Ocupada",
                            "data_atual": "2024-08-19"
                          }
                        },
                        {
                          "attributes": null
                        }
                      ]
                    }
                    """.trimIndent()
                )
        )
        // Página 2 (offset 2, 1 registro + exceededTransferLimit=true -> nextOffset = 3)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "exceededTransferLimit": true,
                      "features": [
                        {
                          "attributes": {
                            "objectid": 20,
                            "terrai_cod": "202",
                            "terrai_nom": "Nove de Janeiro",
                            "etnia_nome": "Parintintin",
                            "municipio_": "Humaitá",
                            "uf_sigla": "AM",
                            "superficie": 228777.0,
                            "fase_ti": "Regularizada",
                            "modalidade": "Tradicionalmente Ocupada",
                            "data_atual": "2024-08-19"
                          }
                        }
                      ]
                    }
                    """.trimIndent()
                )
        )
        // Página 3 (offset 3, inclui duplicata de terrai_cod=101 atualizada e novo registro 303, exceededTransferLimit=false)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "exceededTransferLimit": false,
                      "features": [
                        {
                          "attributes": {
                            "objectid": 10,
                            "terrai_cod": "101",
                            "terrai_nom": "Ipixuna Atualizada",
                            "etnia_nome": "Parintintin, Kagwahiva",
                            "municipio_": "Humaitá, Manicoré",
                            "uf_sigla": "AM",
                            "superficie": 215362.5,
                            "fase_ti": "Homologada",
                            "modalidade": "Tradicionalmente Ocupada",
                            "data_atual": "2025-01-10"
                          }
                        },
                        {
                          "attributes": {
                            "objectid": 30,
                            "terrai_cod": "303",
                            "terrai_nom": "Tenharim do Igarapé Preto",
                            "etnia_nome": "Tenharim",
                            "municipio_": "Novo Aripuanã",
                            "uf_sigla": "AM",
                            "superficie": 87413.0,
                            "fase_ti": "Regularizada",
                            "modalidade": "Tradicionalmente Ocupada",
                            "data_atual": "2024-05-01"
                          }
                        }
                      ]
                    }
                    """.trimIndent()
                )
        )

        val result = repository.syncFromRemote()

        assertEquals(AppResult.Success(RemoteSyncOutcome.Synced(3)), result)
        assertEquals(3, mockWebServer.requestCount)

        val req1 = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
        val req2 = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
        val req3 = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
        assertEquals("0", req1?.requestUrl?.queryParameter("resultOffset"))
        assertEquals("2", req2?.requestUrl?.queryParameter("resultOffset"))
        assertEquals("3", req3?.requestUrl?.queryParameter("resultOffset"))

        val stored = repository.observeAll().first()
        assertEquals(3, stored.size)
        val ipixuna = repository.findByTerritoryCode("101")
        assertEquals("Ipixuna Atualizada", ipixuna?.name)
        assertEquals("Homologada", ipixuna?.phase)
        assertEquals(listOf("Parintintin", "Kagwahiva"), ipixuna?.ethnicities)
    }

    @Test
    fun `syncFromRemote preserva registros locais quando falha HTTP ou ArcGIS ocorre na segunda pagina`() = runBlocking {
        repository.upsert(
            IndigenousLand(
                id = "terrai:101",
                objectId = 1L,
                territoryCode = "101",
                name = "Ipixuna Local Preservada",
                rawEthnicity = "Parintintin",
                ethnicities = listOf("Parintintin"),
                rawMunicipality = "Humaitá",
                municipalities = listOf("Humaitá"),
                rawStateUf = "AM",
                statesUf = listOf("AM"),
                surfaceArea = 215362.0,
                phase = "Regularizada",
                modality = "Tradicionalmente Ocupada",
                updatedAtRaw = "2024-08-19"
            )
        )

        // Página 1 bem-sucedida com outro registro, página 2 retorna erro ArcGIS em HTTP 200
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "exceededTransferLimit": true,
                      "features": [
                        {
                          "attributes": {
                            "objectid": 99,
                            "terrai_cod": "999",
                            "terrai_nom": "Registro Parcial Que Não Deve Persistir"
                          }
                        }
                      ]
                    }
                    """.trimIndent()
                )
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "error": {
                        "code": 500,
                        "message": "Erro interno SIGAGEO",
                        "details": ["Falha ao consultar pagina 2"]
                      }
                    }
                    """.trimIndent()
                )
        )

        val result = repository.syncFromRemote()

        assertTrue(result is AppResult.Error)
        assertEquals(RemoteErrorKind.HTTP_SERVER_ERROR, (result as AppResult.Error).remoteErrorKind)
        assertEquals(1, repository.countOnce())
        assertEquals("Ipixuna Local Preservada", repository.findByTerritoryCode("101")?.name)
        assertNull(repository.findByTerritoryCode("999"))
    }

    @Test
    fun `syncFromRemote com resposta vazia retorna Empty e preserva registros locais`() = runBlocking {
        repository.upsert(
            IndigenousLand(
                id = "terrai:101",
                objectId = 1L,
                territoryCode = "101",
                name = "Ipixuna Local",
                rawEthnicity = "Parintintin",
                ethnicities = listOf("Parintintin"),
                rawMunicipality = "Humaitá",
                municipalities = listOf("Humaitá"),
                rawStateUf = "AM",
                statesUf = listOf("AM"),
                surfaceArea = 215362.0,
                phase = "Regularizada",
                modality = "Tradicionalmente Ocupada",
                updatedAtRaw = "2024-08-19"
            )
        )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"exceededTransferLimit":false,"features":[]}""")
        )

        val result = repository.syncFromRemote()

        assertEquals(AppResult.Success(RemoteSyncOutcome.Empty), result)
        assertEquals(1, repository.countOnce())
        assertEquals("Ipixuna Local", repository.findByTerritoryCode("101")?.name)
    }

    @Test
    fun `resincronizacao dos mesmos territorios nao cria duplicatas e atualiza campos alterados`() = runBlocking {
        val pageOne = """
            {
              "exceededTransferLimit": false,
              "features": [
                {
                  "attributes": {
                    "objectid": 1,
                    "terrai_cod": "101",
                    "terrai_nom": "Ipixuna",
                    "etnia_nome": "Parintintin",
                    "municipio_": "Humaitá",
                    "uf_sigla": "AM",
                    "superficie": 215362.0,
                    "fase_ti": "Declarada",
                    "modalidade": "Tradicionalmente Ocupada",
                    "data_atual": "2024-01-01"
                  }
                }
              ]
            }
        """.trimIndent()
        val pageTwo = """
            {
              "exceededTransferLimit": false,
              "features": [
                {
                  "attributes": {
                    "objectid": 1,
                    "terrai_cod": "101",
                    "terrai_nom": "Ipixuna",
                    "etnia_nome": "Parintintin",
                    "municipio_": "Humaitá",
                    "uf_sigla": "AM",
                    "superficie": 215362.0,
                    "fase_ti": "Regularizada",
                    "modalidade": "Tradicionalmente Ocupada",
                    "data_atual": "2025-02-01"
                  }
                }
              ]
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(pageOne))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(pageTwo))

        assertEquals(AppResult.Success(RemoteSyncOutcome.Synced(1)), repository.syncFromRemote())
        assertEquals("Declarada", repository.findByTerritoryCode("101")?.phase)

        assertEquals(AppResult.Success(RemoteSyncOutcome.Synced(1)), repository.syncFromRemote())
        assertEquals(1, repository.countOnce())
        assertEquals("Regularizada", repository.findByTerritoryCode("101")?.phase)
        assertEquals("2025-02-01", repository.findByTerritoryCode("101")?.updatedAtRaw)
    }

    @Test
    fun `syncFromRemote diante de HTTP 500 na primeira pagina preserva dados locais`() = runBlocking {
        repository.upsert(
            IndigenousLand(
                id = "terrai:101",
                objectId = 1L,
                territoryCode = "101",
                name = "Ipixuna Preservada",
                rawEthnicity = "Parintintin",
                ethnicities = listOf("Parintintin"),
                rawMunicipality = "Humaitá",
                municipalities = listOf("Humaitá"),
                rawStateUf = "AM",
                statesUf = listOf("AM"),
                surfaceArea = 215362.0,
                phase = "Regularizada",
                modality = "Tradicionalmente Ocupada",
                updatedAtRaw = "2024-08-19"
            )
        )
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Server Error"))

        val result = repository.syncFromRemote()

        assertTrue(result is AppResult.Error)
        assertEquals(RemoteErrorKind.HTTP_SERVER_ERROR, (result as AppResult.Error).remoteErrorKind)
        assertEquals(1, repository.countOnce())
        assertEquals("Ipixuna Preservada", repository.findByTerritoryCode("101")?.name)
    }

    @Test
    fun `syncFromRemote com features apenas com attributes null retorna Empty sem alterar Room`() = runBlocking {
        repository.upsert(
            IndigenousLand(
                id = "terrai:101",
                objectId = 1L,
                territoryCode = "101",
                name = "Ipixuna Existente",
                rawEthnicity = "Parintintin",
                ethnicities = listOf("Parintintin"),
                rawMunicipality = "Humaitá",
                municipalities = listOf("Humaitá"),
                rawStateUf = "AM",
                statesUf = listOf("AM"),
                surfaceArea = 215362.0,
                phase = "Regularizada",
                modality = "Tradicionalmente Ocupada",
                updatedAtRaw = "2024-08-19"
            )
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"exceededTransferLimit":false,"features":[{"attributes":null}]}""")
        )

        val result = repository.syncFromRemote()

        assertEquals(AppResult.Success(RemoteSyncOutcome.Empty), result)
        assertEquals(1, repository.countOnce())
        assertEquals("Ipixuna Existente", repository.findByTerritoryCode("101")?.name)
    }

    @Test
    fun `chamadas concorrentes de syncFromRemote sao serializadas sem duplicar registros`() = runBlocking {
        withTimeout(5_000L) {
            val firstRequestArrived = CompletableDeferred<Unit>()
            val releaseFirstRequest = CompletableDeferred<Unit>()
            var requestCounter = 0

            mockWebServer.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val index = ++requestCounter
                    if (index == 1) {
                        firstRequestArrived.complete(Unit)
                        runBlocking { releaseFirstRequest.await() }
                    }
                    return MockResponse()
                        .setResponseCode(200)
                        .setBody(
                            """
                            {
                              "exceededTransferLimit": false,
                              "features": [
                                {
                                  "attributes": {
                                    "objectid": 1,
                                    "terrai_cod": "101",
                                    "terrai_nom": "Ipixuna Sync $index",
                                    "uf_sigla": "AM"
                                  }
                                }
                              ]
                            }
                            """.trimIndent()
                        )
                }
            }

            val firstSync = async(Dispatchers.IO) { repository.syncFromRemote() }
            firstRequestArrived.await()
            val secondSync = async(Dispatchers.IO) { repository.syncFromRemote() }

            releaseFirstRequest.complete(Unit)

            assertEquals(AppResult.Success(RemoteSyncOutcome.Synced(1)), firstSync.await())
            assertEquals(AppResult.Success(RemoteSyncOutcome.Synced(1)), secondSync.await())
            assertEquals(2, requestCounter)
            assertEquals(1, repository.countOnce())
            assertEquals("Ipixuna Sync 2", repository.findByTerritoryCode("101")?.name)
        }
    }

    @Test
    fun `observeTerritories combina filtros offline de busca UF fase e modalidade`() = runBlocking {
        repository.upsertAll(
            listOf(
                IndigenousLand(
                    id = "terrai:101",
                    objectId = 1L,
                    territoryCode = "101",
                    name = "Ipixuna",
                    rawEthnicity = "Parintintin",
                    ethnicities = listOf("Parintintin"),
                    rawMunicipality = "Humaitá",
                    municipalities = listOf("Humaitá"),
                    rawStateUf = "AM",
                    statesUf = listOf("AM"),
                    surfaceArea = 215362.0,
                    phase = "Regularizada",
                    modality = "Tradicionalmente Ocupada",
                    updatedAtRaw = "2024-08-19"
                ),
                IndigenousLand(
                    id = "terrai:202",
                    objectId = 2L,
                    territoryCode = "202",
                    name = "Araribóia",
                    rawEthnicity = "Guajajara",
                    ethnicities = listOf("Guajajara"),
                    rawMunicipality = "Amarante",
                    municipalities = listOf("Amarante"),
                    rawStateUf = "MA",
                    statesUf = listOf("MA"),
                    surfaceArea = 413288.0,
                    phase = "Homologada",
                    modality = "Reserva Indígena",
                    updatedAtRaw = "2024-08-19"
                )
            )
        )

        val matched = repository.observeTerritories(
            query = "Humaitá",
            stateUf = "AM",
            phase = "Regularizada",
            modality = "Tradicionalmente Ocupada"
        ).first()
        assertEquals(listOf("terrai:101"), matched.map { it.id })

        val unmatched = repository.observeTerritories(
            query = "Humaitá",
            stateUf = "MA"
        ).first()
        assertTrue(unmatched.isEmpty())
    }

    @Test
    fun `observeTerritories por TerritoryFilter ordena e trata acentos nulos filtros invalidos e opcoes distintas`() = runBlocking {
        repository.upsertAll(
            listOf(
                IndigenousLand(
                    id = "terrai:101",
                    objectId = 1L,
                    territoryCode = "101",
                    name = "Ipixuna",
                    rawEthnicity = "Parintintin, Kagwahiva",
                    ethnicities = listOf("Parintintin", "Kagwahiva"),
                    rawMunicipality = "Humaitá",
                    municipalities = listOf("Humaitá"),
                    rawStateUf = "AM",
                    statesUf = listOf("AM"),
                    surfaceArea = 215362.0,
                    phase = "Regularizada",
                    modality = "Tradicionalmente Ocupada",
                    updatedAtRaw = "2024-08-19"
                ),
                IndigenousLand(
                    id = "terrai:202",
                    objectId = 2L,
                    territoryCode = "9",
                    name = "Araribóia",
                    rawEthnicity = "Guajajara, Awá",
                    ethnicities = listOf("Guajajara", "Awá"),
                    rawMunicipality = "Amarante",
                    municipalities = listOf("Amarante"),
                    rawStateUf = "MA, TO",
                    statesUf = listOf("MA", "TO"),
                    surfaceArea = 413288.0,
                    phase = "Homologada",
                    modality = "Reserva Indígena",
                    updatedAtRaw = "1730000000000"
                ),
                IndigenousLand(
                    id = "oid:999",
                    objectId = 999L,
                    territoryCode = null,
                    name = null,
                    rawEthnicity = null,
                    ethnicities = emptyList(),
                    rawMunicipality = null,
                    municipalities = emptyList(),
                    rawStateUf = null,
                    statesUf = emptyList(),
                    surfaceArea = null,
                    phase = null,
                    modality = null,
                    updatedAtRaw = null
                )
            )
        )

        val byAreaDesc = repository.observeTerritories(
            TerritoryFilter(
                sortOption = TerritorySortOption(
                    field = TerritorySortField.SURFACE_AREA,
                    direction = TerritorySortDirection.DESCENDING
                )
            )
        ).first()
        assertEquals(listOf("terrai:202", "terrai:101", "oid:999"), byAreaDesc.map { it.id })

        val byCodeAsc = repository.observeTerritories(
            TerritoryFilter(
                sortOption = TerritorySortOption(
                    field = TerritorySortField.TERRITORY_CODE,
                    direction = TerritorySortDirection.ASCENDING
                )
            )
        ).first()
        assertEquals(listOf("terrai:202", "terrai:101", "oid:999"), byCodeAsc.map { it.id })

        val accentAndModality = repository.observeTerritories(
            TerritoryFilter(
                query = "awa",
                stateUf = "to",
                phase = "homologada",
                modality = "reserva indigena"
            )
        ).first()
        assertEquals(listOf("terrai:202"), accentAndModality.map { it.id })

        assertTrue(repository.observeTerritories(TerritoryFilter(stateUf = "%")).first().isEmpty())

        val filterOptions = repository.observeFilterOptions().first()
        assertEquals(listOf("AM", "MA", "TO"), filterOptions.statesUf)
        assertEquals(listOf("Homologada", "Regularizada"), filterOptions.phases)
        assertEquals(listOf("Reserva Indígena", "Tradicionalmente Ocupada"), filterOptions.modalities)
    }

    private object IoDispatcherProvider : DispatcherProvider {
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val default: CoroutineDispatcher = Dispatchers.Default
        override val main: CoroutineDispatcher = Dispatchers.Main
    }
}
