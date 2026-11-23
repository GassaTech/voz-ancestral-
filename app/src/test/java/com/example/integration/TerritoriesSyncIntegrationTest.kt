package com.example.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.core.common.RemoteErrorKind
import com.example.data.local.database.ParintintinDatabase
import com.example.data.local.entity.SyncMetadataEntity
import com.example.data.local.seed.DictionarySeedAssetSource
import com.example.data.local.seed.DictionarySeedJsonParser
import com.example.data.local.seed.SeedClock
import com.example.data.local.seed.SeedSha256Calculator
import com.example.data.local.seed.SeedVersionComparator
import com.example.data.local.seed.VersionedDictionarySeedUpdater
import com.example.data.remote.api.IbamaTerritoriesApiService
import com.example.data.remote.datasource.RemoteTerritoriesDataSource
import com.example.data.remote.dto.IndigenousLandAttributesDto
import com.example.data.remote.dto.IndigenousLandAttributesJsonAdapter
import com.example.data.repository.ArchiveSyncRepositoryImpl
import com.example.data.repository.OfflineFirstDictionaryRepository
import com.example.data.repository.OfflineFirstEncyclopediaRepository
import com.example.data.repository.RoomIndigenousLandRepository
import com.example.domain.connectivity.NetworkConnectivityObserver
import com.example.domain.model.DeviceConnectivityState
import com.example.domain.model.SyncFailureType
import com.example.domain.model.SyncState
import com.example.domain.usecase.ObserveArchiveStatsUseCase
import com.example.domain.usecase.ObserveIndigenousLandByIdUseCase
import com.example.domain.usecase.ObserveIndigenousLandsCountUseCase
import com.example.domain.usecase.ObserveIndigenousLandsUseCase
import com.example.domain.usecase.SyncArchiveUseCase
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TerritoriesSyncIntegrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var moshi: Moshi
    private val openedDatabases = mutableListOf<ParintintinDatabase>()
    private val createdScopes = mutableListOf<CoroutineScope>()
    private val startedServers = mutableListOf<MockWebServer>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        moshi = Moshi.Builder()
            .add(IndigenousLandAttributesDto::class.java, IndigenousLandAttributesJsonAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @After
    fun tearDown() {
        createdScopes.forEach { it.cancel() }
        createdScopes.clear()
        openedDatabases.forEach { db ->
            if (db.isOpen) db.close()
        }
        openedDatabases.clear()
        startedServers.forEach { server ->
            runCatching { server.shutdown() }
        }
        startedServers.clear()
    }

    @Test
    fun `sincronizacao paginada completa persiste no SQLite e permite consulta offline apos encerrar servidor`() = runBlocking {
        val server = MockWebServer().apply {
            enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        arcGisPageJson(
                            exceededTransferLimit = true,
                            features = listOf(
                                arcGisFeatureJson(1L, "101", "Ipixuna", "Parintintin", "Humaitá", "AM", 215362.0, "Regularizada"),
                                arcGisFeatureJson(2L, "102", "Nove de Janeiro", "Parintintin", "Humaitá", "AM", 228777.0, "Regularizada")
                            )
                        )
                    )
            )
            enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        arcGisPageJson(
                            exceededTransferLimit = false,
                            features = listOf(
                                arcGisFeatureJson(3L, "103", "Yanomami", "Yanomami", "Alto Alegre, Mucajaí", "RR, AM", 9664975.0, "Homologada")
                            )
                        )
                    )
            )
            start()
        }
        startedServers += server

        val dbFile = File(tempFolder.root, "territories_sync_offline.db")
        val firstDb = createFileDatabase(dbFile)
        val connectivityObserver = FakeNetworkConnectivityObserver(DeviceConnectivityState.Online)
        val firstStack = createStack(
            database = firstDb,
            baseUrl = server.url("/").toString(),
            connectivityObserver = connectivityObserver
        )

        val syncResult = SyncArchiveUseCase(firstStack.syncRepository)()
        assertTrue(syncResult is AppResult.Success)
        assertEquals(2, server.requestCount)

        val req1 = server.takeRequest(1, TimeUnit.SECONDS)
        val req2 = server.takeRequest(1, TimeUnit.SECONDS)
        assertEquals("0", req1?.requestUrl?.queryParameter("resultOffset"))
        assertEquals("2", req2?.requestUrl?.queryParameter("resultOffset"))

        val syncedState = withTimeout(5_000L) {
            firstStack.syncRepository.syncState.first { it is SyncState.Synced }
        } as SyncState.Synced
        assertEquals(3, syncedState.entriesCount)
        assertEquals(3, ObserveArchiveStatsUseCase(firstStack.syncRepository)().first().totalTerritories)
        assertEquals(3, ObserveIndigenousLandsCountUseCase(firstStack.territoryRepository)().first())

        firstDb.close()
        server.shutdown()

        // Reabre o banco SQLite sem servidor ativo (simula dispositivo totalmente offline após reinício)
        val offlineDb = createFileDatabase(dbFile)
        val offlineStack = createStack(
            database = offlineDb,
            baseUrl = "http://127.0.0.1:1/",
            connectivityObserver = FakeNetworkConnectivityObserver(DeviceConnectivityState.Offline)
        )

        val recoveredState = withTimeout(5_000L) {
            offlineStack.syncRepository.syncState.first { it is SyncState.Synced }
        } as SyncState.Synced
        assertEquals(3, recoveredState.entriesCount)

        val allOffline = ObserveIndigenousLandsUseCase(offlineStack.territoryRepository)().first()
        assertEquals(3, allOffline.size)

        val filteredByUf = ObserveIndigenousLandsUseCase(offlineStack.territoryRepository)(stateUf = "RR").first()
        assertEquals(listOf("terrai:103"), filteredByUf.map { it.id })

        val detail = ObserveIndigenousLandByIdUseCase(offlineStack.territoryRepository)("terrai:101").first()
        assertNotNull(detail)
        assertEquals("Ipixuna", detail?.name)
    }

    @Test
    fun `falha de rede ou JSON invalido preserva territorios locais e diferencia OfflineFallback de RemoteEmpty`() = runBlocking {
        val server = MockWebServer().apply {
            // 1. Sync inicial bem-sucedida com 2 territórios
            enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        arcGisPageJson(
                            exceededTransferLimit = false,
                            features = listOf(
                                arcGisFeatureJson(1L, "101", "Ipixuna", "Parintintin", "Humaitá", "AM", 215362.0, "Regularizada"),
                                arcGisFeatureJson(2L, "102", "Nove de Janeiro", "Parintintin", "Humaitá", "AM", 228777.0, "Regularizada")
                            )
                        )
                    )
            )
            // 2. Resposta inválida (JSON corrompido)
            enqueue(MockResponse().setResponseCode(200).setBody("{json_corrompido"))
            // 3. HTTP 500
            enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))
            // 4. Resposta vazia válida (features = [])
            enqueue(MockResponse().setResponseCode(200).setBody("""{"exceededTransferLimit":false,"features":[]}"""))
            start()
        }
        startedServers += server

        val database = createInMemoryDatabase()
        val connectivityObserver = FakeNetworkConnectivityObserver(DeviceConnectivityState.Online)
        val stack = createStack(
            database = database,
            baseUrl = server.url("/").toString(),
            connectivityObserver = connectivityObserver
        )

        // Passo 1: sincronização inicial
        assertTrue(stack.syncRepository.syncArchive() is AppResult.Success)
        val synced = withTimeout(5_000L) {
            stack.syncRepository.syncState.first { it is SyncState.Synced }
        } as SyncState.Synced
        val officialEpoch = synced.lastSyncedEpochMillis
        assertEquals(2, stack.territoryRepository.countOnce())

        // Passo 2: JSON inválido com rede Online -> OfflineFallback(SERVER_UNAVAILABLE) e Room preservado
        val invalidResult = stack.syncRepository.syncArchive()
        assertTrue(invalidResult is AppResult.Error)
        assertEquals(RemoteErrorKind.DESERIALIZATION_ERROR, (invalidResult as AppResult.Error).remoteErrorKind)
        val fallbackInvalid = withTimeout(5_000L) {
            stack.syncRepository.syncState.first { it is SyncState.OfflineFallback }
        } as SyncState.OfflineFallback
        assertEquals(SyncFailureType.SERVER_UNAVAILABLE, fallbackInvalid.failureType)
        assertEquals(officialEpoch, fallbackInvalid.lastSuccessfulSyncEpochMillis)
        assertEquals(2, stack.territoryRepository.countOnce())

        // Passo 3: HTTP 500 com dispositivo Offline -> OfflineFallback(NO_INTERNET) e Room preservado
        connectivityObserver.setState(DeviceConnectivityState.Offline)
        val httpErrorResult = stack.syncRepository.syncArchive()
        assertTrue(httpErrorResult is AppResult.Error)
        val fallbackOffline = withTimeout(5_000L) {
            stack.syncRepository.syncState.first {
                it is SyncState.OfflineFallback && it.failureType == SyncFailureType.NO_INTERNET
            }
        } as SyncState.OfflineFallback
        assertEquals(officialEpoch, fallbackOffline.lastSuccessfulSyncEpochMillis)
        assertEquals(2, stack.territoryRepository.countOnce())

        // Passo 4: resposta vazia válida -> RemoteEmpty (distinto de erro) preservando Room e histórico oficial
        connectivityObserver.setState(DeviceConnectivityState.Online)
        val emptyResult = stack.syncRepository.syncArchive()
        assertTrue(emptyResult is AppResult.Success)
        val emptyState = withTimeout(5_000L) {
            stack.syncRepository.syncState.first { it is SyncState.RemoteEmpty }
        } as SyncState.RemoteEmpty
        assertEquals(officialEpoch, emptyState.lastSyncedEpochMillis)
        assertEquals(2, emptyState.entriesCount)
        assertEquals(2, stack.territoryRepository.countOnce())
        assertEquals(
            SyncMetadataEntity.STATUS_REMOTE_EMPTY,
            database.syncMetadataDao().getMetadataOnce()?.lastAttemptStatus
        )
    }

    private fun createInMemoryDatabase(): ParintintinDatabase =
        Room.inMemoryDatabaseBuilder(context, ParintintinDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            .also { openedDatabases += it }

    private fun createFileDatabase(dbFile: File): ParintintinDatabase =
        Room.databaseBuilder(context, ParintintinDatabase::class.java, dbFile.absolutePath)
            .addMigrations(*ParintintinDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()
            .also { openedDatabases += it }

    private fun createStack(
        database: ParintintinDatabase,
        baseUrl: String,
        connectivityObserver: NetworkConnectivityObserver
    ): TerritoryStack {
        val dispatchers = object : DispatcherProvider {
            override val io: CoroutineDispatcher = Dispatchers.IO
            override val default: CoroutineDispatcher = Dispatchers.Default
            override val main: CoroutineDispatcher = Dispatchers.Main
        }
        val apiService = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(OkHttpClient())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(IbamaTerritoriesApiService::class.java)
        val remoteDataSource = RemoteTerritoriesDataSource(apiService)
        val territoryRepository = RoomIndigenousLandRepository(
            database = database,
            indigenousLandDao = database.indigenousLandDao(),
            remoteDataSource = remoteDataSource,
            dispatchers = dispatchers
        )
        val dictionaryRepository = OfflineFirstDictionaryRepository(
            dictionaryDao = database.dictionaryDao(),
            dispatchers = dispatchers
        )
        val encyclopediaRepository = OfflineFirstEncyclopediaRepository(
            encyclopediaDao = database.encyclopediaDao(),
            dispatchers = dispatchers
        )
        val seedUpdater = VersionedDictionarySeedUpdater(
            assetSource = DictionarySeedAssetSource {
                ByteArrayInputStream("[]".toByteArray(StandardCharsets.UTF_8))
            },
            sha256Calculator = SeedSha256Calculator(),
            versionComparator = SeedVersionComparator(),
            jsonParser = DictionarySeedJsonParser(moshi),
            database = database,
            dictionaryDao = database.dictionaryDao(),
            seedMetadataDao = database.seedMetadataDao(),
            syncMetadataDao = database.syncMetadataDao(),
            dispatchers = dispatchers,
            clock = SeedClock(System::currentTimeMillis)
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also {
            createdScopes += it
        }
        val syncRepository = ArchiveSyncRepositoryImpl(
            dictionaryRepository = dictionaryRepository,
            encyclopediaRepository = encyclopediaRepository,
            indigenousLandRepository = territoryRepository,
            syncMetadataDao = database.syncMetadataDao(),
            dictionarySeedUpdater = seedUpdater,
            networkConnectivityObserver = connectivityObserver,
            dispatchers = dispatchers,
            applicationScope = scope
        )
        return TerritoryStack(
            territoryRepository = territoryRepository,
            syncRepository = syncRepository
        )
    }

    private fun arcGisPageJson(
        exceededTransferLimit: Boolean,
        features: List<String>
    ): String = """
        {
          "exceededTransferLimit": $exceededTransferLimit,
          "features": [${features.joinToString(",")}]
        }
    """.trimIndent()

    private fun arcGisFeatureJson(
        objectId: Long,
        code: String,
        name: String,
        ethnicity: String,
        municipality: String,
        uf: String,
        area: Double,
        phase: String
    ): String = """
        {
          "attributes": {
            "objectid": $objectId,
            "terrai_cod": "$code",
            "terrai_nom": "$name",
            "etnia_nome": "$ethnicity",
            "municipio_": "$municipality",
            "uf_sigla": "$uf",
            "superficie": $area,
            "fase_ti": "$phase",
            "modalidade": "Tradicionalmente Ocupada",
            "data_atual": "2024-08-19"
          }
        }
    """.trimIndent()

    private data class TerritoryStack(
        val territoryRepository: RoomIndigenousLandRepository,
        val syncRepository: ArchiveSyncRepositoryImpl
    )

    private class FakeNetworkConnectivityObserver(
        initialState: DeviceConnectivityState
    ) : NetworkConnectivityObserver {
        private val stateFlow = MutableStateFlow(initialState)
        override val connectivityState: StateFlow<DeviceConnectivityState> = stateFlow.asStateFlow()

        fun setState(state: DeviceConnectivityState) {
            stateFlow.value = state
        }
    }
}
