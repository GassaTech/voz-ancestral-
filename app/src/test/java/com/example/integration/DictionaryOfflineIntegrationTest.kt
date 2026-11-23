package com.example.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.DictionaryDao
import com.example.data.local.dao.SeedMetadataDao
import com.example.data.local.database.ParintintinDatabase
import com.example.data.local.entity.DictionaryEntryEntity
import com.example.data.local.entity.SeedMetadataEntity
import com.example.data.local.entity.SyncMetadataEntity
import com.example.data.local.seed.AssetDictionarySeedImporter
import com.example.data.local.seed.DictionarySeedAssetSource
import com.example.data.local.seed.DictionarySeedJsonParser
import com.example.data.local.seed.DictionarySeedSpec
import com.example.data.local.seed.SeedClock
import com.example.data.local.seed.SeedSha256Calculator
import com.example.data.local.seed.SeedUpdateOutcomeStatus
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
import com.example.domain.model.GrammaticalClass
import com.example.domain.model.SemanticCategory
import com.example.domain.model.SyncFailureType
import com.example.domain.model.SyncState
import com.example.domain.usecase.ObserveArchiveStatsUseCase
import com.example.domain.usecase.ObserveDictionaryEntriesUseCase
import com.example.presentation.dictionary.DictionaryEntriesState
import com.example.presentation.dictionary.DictionaryViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DictionaryOfflineIntegrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var moshi: Moshi
    private lateinit var mainExecutorDispatcher: kotlinx.coroutines.ExecutorCoroutineDispatcher
    private lateinit var testDispatchers: DispatcherProvider
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
        mainExecutorDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        Dispatchers.setMain(mainExecutorDispatcher)
        testDispatchers = object : DispatcherProvider {
            override val io: CoroutineDispatcher = Dispatchers.IO
            override val default: CoroutineDispatcher = Dispatchers.Default
            override val main: CoroutineDispatcher = mainExecutorDispatcher
        }
    }

    @After
    fun tearDown() {
        createdScopes.forEach { it.cancel() }
        createdScopes.clear()
        openedDatabases.forEach { db ->
            if (db.isOpen) {
                db.close()
            }
        }
        openedDatabases.clear()
        startedServers.forEach { server ->
            runCatching { server.shutdown() }
        }
        startedServers.clear()
        Dispatchers.resetMain()
        mainExecutorDispatcher.close()
    }

    @Test
    fun `1 - importador Room e repositorio importam consultam preservam campos e evitam duplicacao`() = runBlocking {
        val database = createInMemoryDatabase()
        val stack = createRealStack(database = database)

        val seedJson = syntheticSeedArrayJson()

        val firstImport = stack.seedImporter.importIfEmptyFromStream { seedJson.toUtf8Stream() }
        assertEquals(AppResult.Success(4), firstImport)

        val entries = stack.dictionaryRepository.observeEntries().first()
        assertEquals(4, entries.size)
        assertEquals(4, stack.dictionaryRepository.observeTotalCount().first())

        val byId = entries.associateBy { it.id }
        val first = byId.getValue("int_seed_1")
        assertEquals("termo_sintetico_alpha", first.parintintinWord)
        assertEquals("/sintetico_alpha/", first.phoneticIpa)
        assertEquals("significado_sintetico_agua_clara", first.portugueseTranslation)
        assertEquals(GrammaticalClass.NOUN, first.grammaticalClass)
        assertEquals(SemanticCategory.NATURE, first.semanticCategory)
        assertEquals("frase_sintetica_1", first.exampleParintintin)
        assertEquals("traducao_frase_sintetica_1", first.examplePortuguese)
        assertEquals("nota_sintetica_rio", first.culturalNote)
        assertEquals("https://example.invalid/audio/alpha.mp3", first.audioUrl)
        assertFalse(first.isFavorite)
        assertEquals(1_700_000_000_100L, first.updatedAtEpochMillis)

        val third = byId.getValue("int_seed_3")
        assertNull(third.audioUrl)
        assertEquals(GrammaticalClass.ADJECTIVE, third.grammaticalClass)
        assertEquals(SemanticCategory.FOOD, third.semanticCategory)

        // Segunda importação (mesmo com payload diferente) deve ser ignorada de forma idempotente
        val secondSeedJson = """
            [
              ${syntheticJsonEntry(id = "int_seed_1", word = "termo_alterado", translation = "traducao_alterada")},
              ${syntheticJsonEntry(id = "int_seed_extra", word = "termo_extra", translation = "traducao_extra")}
            ]
        """.trimIndent()
        val secondImport = stack.seedImporter.importIfEmptyFromStream { secondSeedJson.toUtf8Stream() }
        assertEquals(AppResult.Success(0), secondImport)

        val afterSecondImport = stack.dictionaryRepository.observeEntries().first()
        assertEquals(4, afterSecondImport.size)
        assertEquals(4, stack.dictionaryRepository.observeTotalCount().first())
        assertEquals(
            "termo_sintetico_alpha",
            afterSecondImport.first { it.id == "int_seed_1" }.parintintinWord
        )
    }

    @Test
    fun `2 - fluxo reativo completo entre Room repositorio e DictionaryViewModel real`() = runBlocking {
        val database = createInMemoryDatabase()
        val stack = createRealStack(database = database)
        val viewModel = DictionaryViewModel(
            observeDictionaryEntries = ObserveDictionaryEntriesUseCase(stack.dictionaryRepository),
            observeArchiveStats = ObserveArchiveStatsUseCase(stack.syncRepository)
        )

        val collectorJob = launch(mainExecutorDispatcher) {
            viewModel.uiState.collect {}
        }

        try {
            // 1. Estado inicial com banco vazio -> Empty(hasActiveFilter = false) e total = 0
            val initialEmpty = withTimeout(5_000L) {
                viewModel.uiState.first {
                    it.contentState == DictionaryEntriesState.Empty(hasActiveFilter = false) &&
                        it.totalLocalEntriesCount == 0
                }
            }
            assertEquals(0, initialEmpty.totalLocalEntriesCount)

            // 2. Executa importIfEmpty() e confirma transição automática para Success + total = 4
            val importResult = stack.seedImporter.importIfEmptyFromStream {
                syntheticSeedArrayJson().toUtf8Stream()
            }
            assertEquals(AppResult.Success(4), importResult)

            val loadedState = withTimeout(5_000L) {
                viewModel.uiState.first {
                    it.contentState is DictionaryEntriesState.Success &&
                        it.totalLocalEntriesCount == 4
                }
            }
            assertEquals(4, (loadedState.contentState as DictionaryEntriesState.Success).entries.size)

            // 3. Pesquisa textual por tradução / nota cultural mantendo totalLocalEntriesCount == 4
            viewModel.onQueryChange("nota_sintetica_rio")
            val searchMatchedState = withTimeout(5_000L) {
                viewModel.uiState.first { state ->
                    state.query == "nota_sintetica_rio" &&
                        state.contentState is DictionaryEntriesState.Success &&
                        state.contentState.entries.map { it.id } == listOf("int_seed_1")
                }
            }
            assertEquals(4, searchMatchedState.totalLocalEntriesCount)

            // 4. Pesquisa sem correspondência mantém totalLocalEntriesCount == 4
            viewModel.onQueryChange("termo_sintetico_inexistente")
            val searchEmptyState = withTimeout(5_000L) {
                viewModel.uiState.first { state ->
                    state.query == "termo_sintetico_inexistente" &&
                        state.contentState == DictionaryEntriesState.Empty(hasActiveFilter = true)
                }
            }
            assertEquals(4, searchEmptyState.totalLocalEntriesCount)

            // 5. Limpa busca e aplica filtros combinados de categoria semântica e classe gramatical
            viewModel.onClearFilters()
            withTimeout(5_000L) {
                viewModel.uiState.first { state ->
                    state.query.isEmpty() &&
                        state.selectedCategory == null &&
                        state.selectedGrammaticalClass == null &&
                        (state.contentState as? DictionaryEntriesState.Success)?.entries?.size == 4
                }
            }

            viewModel.onSelectCategory(SemanticCategory.ANIMALS)
            viewModel.onSelectGrammaticalClass(GrammaticalClass.VERB)
            val filteredState = withTimeout(5_000L) {
                viewModel.uiState.first { state ->
                    state.selectedCategory == SemanticCategory.ANIMALS &&
                        state.selectedGrammaticalClass == GrammaticalClass.VERB &&
                        state.contentState is DictionaryEntriesState.Success &&
                        state.contentState.entries.map { it.id } == listOf("int_seed_2")
                }
            }
            assertEquals(4, filteredState.totalLocalEntriesCount)

            // 6. Combinação sem resultado também mantém totalLocalEntriesCount == 4
            viewModel.onSelectGrammaticalClass(GrammaticalClass.TOPONYM)
            val filteredEmptyState = withTimeout(5_000L) {
                viewModel.uiState.first { state ->
                    state.selectedCategory == SemanticCategory.ANIMALS &&
                        state.selectedGrammaticalClass == GrammaticalClass.TOPONYM &&
                        state.contentState == DictionaryEntriesState.Empty(hasActiveFilter = true)
                }
            }
            assertEquals(4, filteredEmptyState.totalLocalEntriesCount)
        } finally {
            collectorJob.cancel()
        }
    }

    @Test
    fun `3 - persistencia em arquivo SQLite sobrevive ao fechamento e reabertura sem duplicar`() = runBlocking {
        val dbDir = tempFolder.newFolder("sqlite_persistence")
        val dbFile = File(dbDir, "parintintin_archive_integration.db")

        // Instância 1: cria arquivo físico, importa 4 registros e fecha
        val db1 = createFileDatabase(dbFile)
        val stack1 = createRealStack(database = db1)

        val firstImport = stack1.seedImporter.importIfEmptyFromStream {
            syntheticSeedArrayJson().toUtf8Stream()
        }
        assertEquals(AppResult.Success(4), firstImport)
        assertEquals(4, stack1.dictionaryRepository.observeTotalCount().first())
        db1.close()

        // Instância 2: abre novo ParintintinDatabase apontando para o mesmo arquivo
        val db2 = createFileDatabase(dbFile)
        val stack2 = createRealStack(database = db2)

        val persistedEntries = stack2.dictionaryRepository.observeEntries().first()
        assertEquals(4, persistedEntries.size)
        assertEquals(4, stack2.dictionaryRepository.observeTotalCount().first())
        assertEquals(
            setOf("int_seed_1", "int_seed_2", "int_seed_3", "int_seed_4"),
            persistedEntries.map { it.id }.toSet()
        )

        // Nova tentativa de importação na segunda instância não deve duplicar nem sobrescrever
        val secondImport = stack2.seedImporter.importIfEmptyFromStream {
            """
            [
              ${syntheticJsonEntry(id = "int_seed_1", word = "sobrescrito", translation = "sobrescrito")},
              ${syntheticJsonEntry(id = "int_seed_5", word = "novo_verbete", translation = "novo_significado")}
            ]
            """.trimIndent().toUtf8Stream()
        }
        assertEquals(AppResult.Success(0), secondImport)
        assertEquals(4, stack2.dictionaryRepository.observeTotalCount().first())
        assertEquals(
            "termo_sintetico_alpha",
            stack2.dictionaryRepository.observeEntries().first().first { it.id == "int_seed_1" }.parintintinWord
        )
        db2.close()
    }

    @Test
    fun `4 - falha HTTP na sincronizacao preserva dados locais e atualiza DictionaryViewModel`() = runBlocking {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))
            start()
        }
        startedServers += server

        val database = createInMemoryDatabase()
        val stack = createRealStack(
            database = database,
            baseUrl = server.url("/").toString(),
            seedStreamProvider = { syntheticSeedArrayJson().toUtf8Stream() }
        )

        val viewModel = DictionaryViewModel(
            observeDictionaryEntries = ObserveDictionaryEntriesUseCase(stack.dictionaryRepository),
            observeArchiveStats = ObserveArchiveStatsUseCase(stack.syncRepository)
        )
        val collectorJob = launch(mainExecutorDispatcher) {
            viewModel.uiState.collect {}
        }

        try {
            // Sincroniza: seed importa os 4 registros antes da chamada HTTP 500
            val syncResult = stack.syncRepository.syncArchive()
            assertTrue(syncResult is AppResult.Error)
            withTimeout(5_000L) {
                stack.syncRepository.syncState.first { it is SyncState.OfflineFallback }
            }

            val uiStateAfterOfflineSync = withTimeout(5_000L) {
                viewModel.uiState.first {
                    it.contentState is DictionaryEntriesState.Success &&
                        it.totalLocalEntriesCount == 4
                }
            }
            assertEquals(
                4,
                (uiStateAfterOfflineSync.contentState as DictionaryEntriesState.Success).entries.size
            )
            assertEquals(4, uiStateAfterOfflineSync.totalLocalEntriesCount)
        } finally {
            collectorJob.cancel()
        }
    }

    @Test
    fun `5 - fluxo integrado aplica seed inicial evita reescrita idempotente sincroniza oficial e bloqueia alteracoes locais`() = runBlocking {
        var currentSeedJson = syntheticSeedArrayJson()
        val server = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(500).setBody("Offline 1"))
            enqueue(MockResponse().setResponseCode(500).setBody("Offline 2"))
            enqueue(
                MockResponse().setResponseCode(200).setBody(
                    arcGisTerritoriesPageJson(
                        syntheticArcGisFeature(1L, "101", "Ipixuna"),
                        syntheticArcGisFeature(2L, "102", "Nove de Janeiro")
                    )
                )
            )
            enqueue(MockResponse().setResponseCode(500).setBody("Offline 3"))
            start()
        }
        startedServers += server

        val database = createInMemoryDatabase()
        val stack = createRealStack(
            database = database,
            baseUrl = server.url("/").toString(),
            seedStreamProvider = { currentSeedJson.toUtf8Stream() }
        )
        val viewModel = DictionaryViewModel(
            observeDictionaryEntries = ObserveDictionaryEntriesUseCase(stack.dictionaryRepository),
            observeArchiveStats = ObserveArchiveStatsUseCase(stack.syncRepository)
        )
        val collectorJob = launch(mainExecutorDispatcher) {
            viewModel.uiState.collect {}
        }

        try {
            // Execução 1: banco vazio + HTTP 500 -> aplica seed inicial e registra OfflineFallback
            val firstSync = stack.syncRepository.syncArchive()
            assertTrue(firstSync is AppResult.Error)
            withTimeout(5_000L) {
                stack.syncRepository.syncState.first { it is SyncState.OfflineFallback }
            }
            val uiInitialSeed = withTimeout(5_000L) {
                viewModel.uiState.first {
                    it.contentState is DictionaryEntriesState.Success &&
                        it.totalLocalEntriesCount == 4
                }
            }
            assertEquals(4, (uiInitialSeed.contentState as DictionaryEntriesState.Success).entries.size)
            assertEquals(1, stack.spyDictionaryDao.upsertAllCallCount)
            assertNull(database.syncMetadataDao().getMetadataOnce()?.lastSuccessfulSyncEpochMillis)

            val expectedInitialHash = SeedSha256Calculator().calculate(
                currentSeedJson.toByteArray(StandardCharsets.UTF_8)
            )
            val initialSeedMeta = database.seedMetadataDao().getMetadataOnce()
            assertEquals(DictionarySeedSpec.CURRENT_SEED_VERSION, initialSeedMeta?.appliedSeedVersion)
            assertEquals(expectedInitialHash, initialSeedMeta?.appliedSeedHash)

            // Execução 2: mesmo seed + HTTP 500 -> idempotente (não reescreve dictionary_entries)
            val secondSync = stack.syncRepository.syncArchive()
            assertTrue(secondSync is AppResult.Error)
            assertEquals(1, stack.spyDictionaryDao.upsertAllCallCount)
            assertEquals(initialSeedMeta, database.seedMetadataDao().getMetadataOnce())

            // Marca favorito localmente e executa sync oficial HTTP 200 (IBAMA Terras Indígenas)
            stack.dictionaryRepository.toggleFavorite("int_seed_1")
            val thirdSync = stack.syncRepository.syncArchive()
            assertTrue(thirdSync is AppResult.Success)
            withTimeout(5_000L) {
                stack.syncRepository.syncState.first { it is SyncState.Synced }
            }
            assertTrue(database.syncMetadataDao().getMetadataOnce()?.lastSuccessfulSyncEpochMillis != null)
            assertEquals(
                ArchiveSyncRepositoryImpl.IBAMA_TERRITORIES_REMOTE_VERSION,
                database.syncMetadataDao().getMetadataOnce()?.lastSuccessfulRemoteVersion
            )
            assertEquals(2, stack.territoryRepository.countOnce())

            val entriesAfterOfficial = stack.dictionaryRepository.observeEntries().first().associateBy { it.id }
            assertEquals(4, entriesAfterOfficial.size)
            assertEquals("termo_sintetico_alpha", entriesAfterOfficial.getValue("int_seed_1").parintintinWord)
            assertTrue(entriesAfterOfficial.getValue("int_seed_1").isFavorite)

            val uiAfterOfficial = withTimeout(5_000L) {
                viewModel.uiState.first { state ->
                    (state.contentState as? DictionaryEntriesState.Success)?.entries?.size == 4
                }
            }
            assertEquals(4, uiAfterOfficial.totalLocalEntriesCount)
            assertEquals(2, ObserveArchiveStatsUseCase(stack.syncRepository)().first().totalTerritories)

            // Execução 4: seed modificado localmente + HTTP 500 -> bloqueado pelo sync oficial
            currentSeedJson = """
                [
                  ${syntheticJsonEntry(
                    id = "int_seed_1",
                    word = "termo_demo_bloqueado",
                    translation = "significado_demo_bloqueado",
                    updatedAt = 1_700_000_009_000L
                )}
                ]
            """.trimIndent()
            val fourthSync = stack.syncRepository.syncArchive()
            assertTrue(fourthSync is AppResult.Error)
            assertEquals(4, server.requestCount)

            val entriesAfterBlockedSeed = stack.dictionaryRepository.observeEntries().first().associateBy { it.id }
            assertEquals("termo_sintetico_alpha", entriesAfterBlockedSeed.getValue("int_seed_1").parintintinWord)
            assertTrue(entriesAfterBlockedSeed.getValue("int_seed_1").isFavorite)
            assertEquals(initialSeedMeta, database.seedMetadataDao().getMetadataOnce())
        } finally {
            collectorJob.cancel()
        }
    }

    @Test
    fun `6 - atualizacao de seed antigo preserva favoritos antes e depois da sincronizacao oficial`() = runBlocking {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(500).setBody("Offline"))
            enqueue(
                MockResponse().setResponseCode(200).setBody(
                    arcGisTerritoriesPageJson(
                        syntheticArcGisFeature(1L, "101", "Ipixuna")
                    )
                )
            )
            start()
        }
        startedServers += server

        val database = createInMemoryDatabase()
        database.dictionaryDao().upsertAll(
            listOf(
                DictionaryEntryEntity(
                    id = "int_seed_1",
                    parintintinWord = "termo_antigo_v1",
                    phoneticIpa = "/antigo/",
                    portugueseTranslation = "significado_antigo_v1",
                    grammaticalClass = "NOUN",
                    semanticCategory = "NATURE",
                    exampleParintintin = "ex_antigo",
                    examplePortuguese = "ex_antigo_pt",
                    culturalNote = "nota_antiga",
                    audioUrl = null,
                    isFavorite = true,
                    updatedAtEpochMillis = 1_699_999_000_000L
                ),
                DictionaryEntryEntity(
                    id = "custom_entry_1",
                    parintintinWord = "termo_usuario",
                    phoneticIpa = "/usuario/",
                    portugueseTranslation = "significado_usuario",
                    grammaticalClass = "NOUN",
                    semanticCategory = "NATURE",
                    exampleParintintin = "ex_user",
                    examplePortuguese = "ex_user_pt",
                    culturalNote = "nota_user",
                    audioUrl = null,
                    isFavorite = true,
                    updatedAtEpochMillis = 1_699_999_500_000L
                )
            )
        )
        database.seedMetadataDao().upsert(
            SeedMetadataEntity(
                appliedSeedVersion = 1,
                appliedSeedHash = "hash_versao_1",
                appliedSeedIdsJson = "[\"int_seed_1\"]",
                updatedAtEpochMillis = 1_699_999_000_000L
            )
        )

        val seedJson = syntheticSeedArrayJson()
        val stack = createRealStack(
            database = database,
            baseUrl = server.url("/").toString(),
            seedStreamProvider = { seedJson.toUtf8Stream() }
        )

        // Passo 1: atualiza seed de v1 -> CURRENT_SEED_VERSION com backend offline
        val offlineSync = stack.syncRepository.syncArchive()
        assertTrue(offlineSync is AppResult.Error)

        val entriesAfterSeedUpdate = stack.dictionaryRepository.observeEntries().first().associateBy { it.id }
        assertEquals(5, entriesAfterSeedUpdate.size)
        assertEquals("termo_sintetico_alpha", entriesAfterSeedUpdate.getValue("int_seed_1").parintintinWord)
        assertTrue(entriesAfterSeedUpdate.getValue("int_seed_1").isFavorite)
        assertEquals("termo_usuario", entriesAfterSeedUpdate.getValue("custom_entry_1").parintintinWord)
        assertTrue(entriesAfterSeedUpdate.getValue("custom_entry_1").isFavorite)
        assertEquals(
            DictionarySeedSpec.CURRENT_SEED_VERSION,
            database.seedMetadataDao().getMetadataOnce()?.appliedSeedVersion
        )

        // Passo 2: sincronização oficial HTTP 200 persiste territórios e preserva favoritos locais
        val officialSync = stack.syncRepository.syncArchive()
        assertTrue(officialSync is AppResult.Success)
        withTimeout(5_000L) {
            stack.syncRepository.syncState.first { it is SyncState.Synced }
        }

        val entriesAfterOfficial = stack.dictionaryRepository.observeEntries().first().associateBy { it.id }
        assertEquals("termo_sintetico_alpha", entriesAfterOfficial.getValue("int_seed_1").parintintinWord)
        assertTrue(entriesAfterOfficial.getValue("int_seed_1").isFavorite)
        assertTrue(entriesAfterOfficial.getValue("custom_entry_1").isFavorite)
        assertEquals(1, stack.territoryRepository.countOnce())
    }

    @Test
    fun `7 - falha de leitura do seed e falha de persistencia com rollback nao bloqueiam a sincronizacao remota`() = runBlocking {
        // Sub-cenário A: JSON inválido no seed + HTTP 200 bem-sucedido
        val serverInvalidSeed = MockWebServer().apply {
            enqueue(
                MockResponse().setResponseCode(200).setBody(
                    arcGisTerritoriesPageJson(
                        syntheticArcGisFeature(1L, "101", "Ipixuna Resiliente")
                    )
                )
            )
            start()
        }
        startedServers += serverInvalidSeed

        val dbInvalidSeed = createInMemoryDatabase()
        val stackInvalidSeed = createRealStack(
            database = dbInvalidSeed,
            baseUrl = serverInvalidSeed.url("/").toString(),
            seedStreamProvider = { "{ json_invalido".toUtf8Stream() }
        )

        val invalidSeedSync = stackInvalidSeed.syncRepository.syncArchive()
        assertTrue(invalidSeedSync is AppResult.Success)
        assertEquals(1, serverInvalidSeed.requestCount)
        withTimeout(5_000L) {
            stackInvalidSeed.syncRepository.syncState.first { it is SyncState.Synced }
        }
        assertEquals("Ipixuna Resiliente", stackInvalidSeed.territoryRepository.findByTerritoryCode("101")?.name)

        // Sub-cenário B: falha transacional na persistência do seed faz rollback completo e prossegue para HTTP 200
        val serverRollback = MockWebServer().apply {
            enqueue(
                MockResponse().setResponseCode(200).setBody(
                    arcGisTerritoriesPageJson(
                        syntheticArcGisFeature(2L, "202", "Nove de Janeiro Pós Rollback")
                    )
                )
            )
            start()
        }
        startedServers += serverRollback

        val dbRollback = createInMemoryDatabase()
        dbRollback.dictionaryDao().upsertAll(
            listOf(
                DictionaryEntryEntity(
                    id = "int_seed_1",
                    parintintinWord = "termo_preexistente_estavel",
                    phoneticIpa = "/estavel/",
                    portugueseTranslation = "significado_preexistente",
                    grammaticalClass = "NOUN",
                    semanticCategory = "NATURE",
                    exampleParintintin = "ex_estavel",
                    examplePortuguese = "ex_estavel_pt",
                    culturalNote = "nota_estavel",
                    audioUrl = null,
                    isFavorite = true,
                    updatedAtEpochMillis = 1_699_999_000_000L
                )
            )
        )
        val initialRollbackMeta = SeedMetadataEntity(
            appliedSeedVersion = 1,
            appliedSeedHash = "hash_estavel_v1",
            appliedSeedIdsJson = "[\"int_seed_1\"]",
            updatedAtEpochMillis = 1_699_999_000_000L
        )
        dbRollback.seedMetadataDao().upsert(initialRollbackMeta)

        val stackRollback = createRealStack(
            database = dbRollback,
            baseUrl = serverRollback.url("/").toString(),
            seedStreamProvider = { syntheticSeedArrayJson().toUtf8Stream() },
            onBeforeSeedMetadataUpsert = {
                throw IllegalStateException("Falha simulada ao gravar seed_metadata")
            }
        )

        val rollbackSync = stackRollback.syncRepository.syncArchive()
        assertTrue(rollbackSync is AppResult.Success)
        assertEquals(1, serverRollback.requestCount)
        withTimeout(5_000L) {
            stackRollback.syncRepository.syncState.first { it is SyncState.Synced }
        }

        val entriesAfterRollback = stackRollback.dictionaryRepository.observeEntries().first().associateBy { it.id }
        // Rollback garantiu que int_seed_2..4 do seed abortado não foram gravados e int_seed_1 manteve estado pré-existente
        assertEquals(setOf("int_seed_1"), entriesAfterRollback.keys)
        assertEquals("termo_preexistente_estavel", entriesAfterRollback.getValue("int_seed_1").parintintinWord)
        assertTrue(entriesAfterRollback.getValue("int_seed_1").isFavorite)
        assertEquals(initialRollbackMeta, dbRollback.seedMetadataDao().getMetadataOnce())
        assertEquals("Nove de Janeiro Pós Rollback", stackRollback.territoryRepository.findByTerritoryCode("202")?.name)
    }

    @Test
    fun `8 - resposta HTTP 200 vazia nao bloqueia futuras atualizacoes do seed e preserva metadados oficiais anteriores`() = runBlocking {
        var currentSeedJson = syntheticSeedArrayJson()
        val emptyArcGis = arcGisTerritoriesPageJson()
        val server = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(200).setBody(emptyArcGis))
            enqueue(MockResponse().setResponseCode(200).setBody(emptyArcGis))
            enqueue(
                MockResponse().setResponseCode(200).setBody(
                    arcGisTerritoriesPageJson(
                        syntheticArcGisFeature(1L, "101", "Ipixuna"),
                        syntheticArcGisFeature(2L, "102", "Nove de Janeiro")
                    )
                )
            )
            enqueue(MockResponse().setResponseCode(200).setBody(emptyArcGis))
            start()
        }
        startedServers += server

        val database = createInMemoryDatabase()
        val stack = createRealStack(
            database = database,
            baseUrl = server.url("/").toString(),
            seedStreamProvider = { currentSeedJson.toUtf8Stream() }
        )

        // Passo 1: primeira execução com banco vazio + servidor HTTP 200 features=[]
        val firstResult = stack.syncRepository.syncArchive()
        assertTrue(firstResult is AppResult.Success)
        val firstState = withTimeout(5_000L) {
            stack.syncRepository.syncState.first { it is SyncState.RemoteEmpty }
        } as SyncState.RemoteEmpty
        assertNull(firstState.lastSyncedEpochMillis)
        assertNull(firstState.entriesCount)

        val metadataAfterFirst = database.syncMetadataDao().getMetadataOnce()
        assertEquals(SyncMetadataEntity.STATUS_REMOTE_EMPTY, metadataAfterFirst?.lastAttemptStatus)
        assertNull(metadataAfterFirst?.lastSuccessfulSyncEpochMillis)
        assertNull(metadataAfterFirst?.lastSuccessfulEntriesCount)

        val entriesAfterFirst = stack.dictionaryRepository.observeEntries().first().associateBy { it.id }
        assertEquals(4, entriesAfterFirst.size)
        assertEquals("termo_sintetico_alpha", entriesAfterFirst.getValue("int_seed_1").parintintinWord)

        // Marca favorito e altera o Seed local
        stack.dictionaryRepository.toggleFavorite("int_seed_1")
        currentSeedJson = """
            [
              ${syntheticJsonEntry(
                id = "int_seed_1",
                word = "termo_seed_revisado_v2",
                translation = "significado_seed_revisado_v2",
                updatedAt = 1_700_000_005_000L
            )}
            ]
        """.trimIndent()

        // Passo 2: nova chamada HTTP 200 features=[] -> atualizador do Seed NÃO deve ser bloqueado e favorito é preservado
        val secondResult = stack.syncRepository.syncArchive()
        assertTrue(secondResult is AppResult.Success)
        val entriesAfterSecondSeed = stack.dictionaryRepository.observeEntries().first().associateBy { it.id }
        assertEquals("termo_seed_revisado_v2", entriesAfterSecondSeed.getValue("int_seed_1").parintintinWord)
        assertTrue(entriesAfterSecondSeed.getValue("int_seed_1").isFavorite)
        assertNull(database.syncMetadataDao().getMetadataOnce()?.lastSuccessfulSyncEpochMillis)

        // Passo 3: servidor publica camada territorial não vazia -> Synced grava histórico oficial
        val thirdResult = stack.syncRepository.syncArchive()
        assertTrue(thirdResult is AppResult.Success)
        val syncedState = withTimeout(5_000L) {
            stack.syncRepository.syncState.first { it is SyncState.Synced }
        } as SyncState.Synced
        assertEquals(2, syncedState.entriesCount)

        val officialSyncEpoch = database.syncMetadataDao().getMetadataOnce()?.lastSuccessfulSyncEpochMillis
        assertTrue(officialSyncEpoch != null)
        assertEquals(2, stack.territoryRepository.countOnce())
        val entriesAfterOfficial = stack.dictionaryRepository.observeEntries().first().associateBy { it.id }
        assertEquals("termo_seed_revisado_v2", entriesAfterOfficial.getValue("int_seed_1").parintintinWord)
        assertTrue(entriesAfterOfficial.getValue("int_seed_1").isFavorite)

        // Passo 4: novo Seed alterado + HTTP 200 features=[] após sincronização oficial -> Seed fica bloqueado e histórico oficial é preservado
        currentSeedJson = """
            [
              ${syntheticJsonEntry(
                id = "int_seed_1",
                word = "termo_seed_que_nao_deve_sobrescrever",
                translation = "traducao_bloqueada",
                updatedAt = 1_700_000_030_000L
            )}
            ]
        """.trimIndent()

        val directSeedCheck = stack.seedUpdater.updateIfNeeded()
        assertTrue(directSeedCheck is AppResult.Success)
        assertEquals(
            SeedUpdateOutcomeStatus.BLOCKED_BY_OFFICIAL_SYNC,
            (directSeedCheck as AppResult.Success).data.status
        )

        val fourthResult = stack.syncRepository.syncArchive()
        assertTrue(fourthResult is AppResult.Success)
        val stateAfterFourth = withTimeout(5_000L) {
            stack.syncRepository.syncState.first {
                it is SyncState.RemoteEmpty && it.lastSyncedEpochMillis == officialSyncEpoch
            }
        } as SyncState.RemoteEmpty
        assertEquals(2, stateAfterFourth.entriesCount)

        val metadataAfterFourth = database.syncMetadataDao().getMetadataOnce()
        assertEquals(SyncMetadataEntity.STATUS_REMOTE_EMPTY, metadataAfterFourth?.lastAttemptStatus)
        assertEquals(officialSyncEpoch, metadataAfterFourth?.lastSuccessfulSyncEpochMillis)
        assertEquals(2, metadataAfterFourth?.lastSuccessfulEntriesCount)
        assertEquals(
            ArchiveSyncRepositoryImpl.IBAMA_TERRITORIES_REMOTE_VERSION,
            metadataAfterFourth?.lastSuccessfulRemoteVersion
        )

        val finalEntries = stack.dictionaryRepository.observeEntries().first().associateBy { it.id }
        assertEquals("termo_seed_revisado_v2", finalEntries.getValue("int_seed_1").parintintinWord)
        assertTrue(finalEntries.getValue("int_seed_1").isFavorite)
        assertEquals(2, stack.territoryRepository.countOnce())
    }

    @Test
    fun `9 - classificacao de falhas remotas diferencia Offline ConnectedUnvalidated Online com Room e MockWebServer reais`() = runBlocking {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error 1"))
            enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error 2"))
            start()
        }
        startedServers += server

        // Sub-cenário A: dispositivo Offline -> falha de conexão -> NO_INTERNET e Seed preservado
        val dbOffline = createInMemoryDatabase()
        val offlineObserver = FakeNetworkConnectivityObserver(DeviceConnectivityState.Offline)
        val stackOffline = createRealStack(
            database = dbOffline,
            baseUrl = "http://127.0.0.1:1/",
            seedStreamProvider = { syntheticSeedArrayJson().toUtf8Stream() },
            connectivityObserver = offlineObserver
        )
        val offlineResult = stackOffline.syncRepository.syncArchive()
        assertTrue(offlineResult is AppResult.Error)
        val offlineState = withTimeout(5_000L) {
            stackOffline.syncRepository.syncState.first { it is SyncState.OfflineFallback }
        } as SyncState.OfflineFallback
        assertEquals(SyncFailureType.NO_INTERNET, offlineState.failureType)
        assertNull(offlineState.lastSuccessfulSyncEpochMillis)
        assertEquals(4, stackOffline.dictionaryRepository.observeTotalCount().first())

        // Sub-cenário B: ConnectedUnvalidated -> HTTP 500 -> UNVALIDATED_NETWORK e Seed preservado
        val dbUnvalidated = createInMemoryDatabase()
        val unvalidatedObserver = FakeNetworkConnectivityObserver(DeviceConnectivityState.ConnectedUnvalidated)
        val stackUnvalidated = createRealStack(
            database = dbUnvalidated,
            baseUrl = server.url("/").toString(),
            seedStreamProvider = { syntheticSeedArrayJson().toUtf8Stream() },
            connectivityObserver = unvalidatedObserver
        )
        val unvalidatedResult = stackUnvalidated.syncRepository.syncArchive()
        assertTrue(unvalidatedResult is AppResult.Error)
        val unvalidatedState = withTimeout(5_000L) {
            stackUnvalidated.syncRepository.syncState.first { it is SyncState.OfflineFallback }
        } as SyncState.OfflineFallback
        assertEquals(SyncFailureType.UNVALIDATED_NETWORK, unvalidatedState.failureType)
        assertNull(unvalidatedState.lastSuccessfulSyncEpochMillis)
        assertEquals(4, stackUnvalidated.dictionaryRepository.observeTotalCount().first())

        // Sub-cenário C: Online -> HTTP 500 -> SERVER_UNAVAILABLE e Seed preservado
        val dbOnline = createInMemoryDatabase()
        val onlineObserver = FakeNetworkConnectivityObserver(DeviceConnectivityState.Online)
        val stackOnline = createRealStack(
            database = dbOnline,
            baseUrl = server.url("/").toString(),
            seedStreamProvider = { syntheticSeedArrayJson().toUtf8Stream() },
            connectivityObserver = onlineObserver
        )
        val onlineResult = stackOnline.syncRepository.syncArchive()
        assertTrue(onlineResult is AppResult.Error)
        val onlineState = withTimeout(5_000L) {
            stackOnline.syncRepository.syncState.first { it is SyncState.OfflineFallback }
        } as SyncState.OfflineFallback
        assertEquals(SyncFailureType.SERVER_UNAVAILABLE, onlineState.failureType)
        assertNull(onlineState.lastSuccessfulSyncEpochMillis)
        assertEquals(4, stackOnline.dictionaryRepository.observeTotalCount().first())
    }

    @Test
    fun `10 - sincronizacao oficial com favorito preservado seguida de HTTP 500 preserva timestamp oficial favoritos e HTTP 200 vazio segue RemoteEmpty`() = runBlocking {
        val server = MockWebServer().apply {
            enqueue(
                MockResponse().setResponseCode(200).setBody(
                    arcGisTerritoriesPageJson(
                        syntheticArcGisFeature(1L, "101", "Ipixuna Oficial")
                    )
                )
            )
            enqueue(MockResponse().setResponseCode(500).setBody("Server Down"))
            enqueue(MockResponse().setResponseCode(200).setBody(arcGisTerritoriesPageJson()))
            start()
        }
        startedServers += server

        val database = createInMemoryDatabase()
        val connectivityObserver = FakeNetworkConnectivityObserver(DeviceConnectivityState.Online)
        val stack = createRealStack(
            database = database,
            baseUrl = server.url("/").toString(),
            seedStreamProvider = { syntheticSeedArrayJson().toUtf8Stream() },
            connectivityObserver = connectivityObserver
        )

        // Prepara Seed inicial e marca favorito antes da sincronização oficial
        assertEquals(AppResult.Success(4), stack.seedImporter.importIfEmptyFromStream {
            syntheticSeedArrayJson().toUtf8Stream()
        })
        stack.dictionaryRepository.toggleFavorite("int_seed_1")

        // Passo 1: sincronização oficial bem-sucedida preserva favorito e persiste território
        val officialResult = stack.syncRepository.syncArchive()
        assertTrue(officialResult is AppResult.Success)
        val syncedState = withTimeout(5_000L) {
            stack.syncRepository.syncState.first { it is SyncState.Synced }
        } as SyncState.Synced
        val officialTimestamp = syncedState.lastSyncedEpochMillis
        assertEquals("Ipixuna Oficial", stack.territoryRepository.findByTerritoryCode("101")?.name)

        val entriesAfterOfficial = stack.dictionaryRepository.observeEntries().first().associateBy { it.id }
        assertEquals("termo_sintetico_alpha", entriesAfterOfficial.getValue("int_seed_1").parintintinWord)
        assertTrue(entriesAfterOfficial.getValue("int_seed_1").isFavorite)

        // Passo 2: dispositivo Online + HTTP 500 -> SERVER_UNAVAILABLE preservando timestamp oficial, território e favoritos
        val failedResult = stack.syncRepository.syncArchive()
        assertTrue(failedResult is AppResult.Error)
        val fallbackState = withTimeout(5_000L) {
            stack.syncRepository.syncState.first { it is SyncState.OfflineFallback }
        } as SyncState.OfflineFallback
        assertEquals(SyncFailureType.SERVER_UNAVAILABLE, fallbackState.failureType)
        assertEquals(officialTimestamp, fallbackState.lastSuccessfulSyncEpochMillis)
        assertEquals(1, stack.territoryRepository.countOnce())

        val entriesAfterFailure = stack.dictionaryRepository.observeEntries().first().associateBy { it.id }
        assertEquals("termo_sintetico_alpha", entriesAfterFailure.getValue("int_seed_1").parintintinWord)
        assertTrue(entriesAfterFailure.getValue("int_seed_1").isFavorite)

        // Passo 3: HTTP 200 features=[] continua tratado como SyncState.RemoteEmpty e preserva histórico oficial, território e favoritos
        val emptyResult = stack.syncRepository.syncArchive()
        assertTrue(emptyResult is AppResult.Success)
        val remoteEmptyState = withTimeout(5_000L) {
            stack.syncRepository.syncState.first { it is SyncState.RemoteEmpty }
        } as SyncState.RemoteEmpty
        assertEquals(officialTimestamp, remoteEmptyState.lastSyncedEpochMillis)
        assertEquals(1, remoteEmptyState.entriesCount)
        assertEquals(1, stack.territoryRepository.countOnce())
        assertTrue(
            stack.dictionaryRepository.observeEntries().first()
                .first { it.id == "int_seed_1" }
                .isFavorite
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

    private fun createRealStack(
        database: ParintintinDatabase,
        baseUrl: String = "http://127.0.0.1:1/",
        seedStreamProvider: () -> InputStream = { "[]".toUtf8Stream() },
        onBeforeSeedMetadataUpsert: (suspend (SeedMetadataEntity) -> Unit)? = null,
        connectivityObserver: NetworkConnectivityObserver = FakeNetworkConnectivityObserver()
    ): RealIntegrationStack {
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
            dispatchers = testDispatchers
        )
        val dictionaryRepository = OfflineFirstDictionaryRepository(
            dictionaryDao = database.dictionaryDao(),
            dispatchers = testDispatchers
        )
        val encyclopediaRepository = OfflineFirstEncyclopediaRepository(
            encyclopediaDao = database.encyclopediaDao(),
            dispatchers = testDispatchers
        )
        val assetSeeder = AssetDictionarySeedImporter(
            context = context,
            moshi = moshi,
            database = database,
            dictionaryDao = database.dictionaryDao(),
            dispatchers = testDispatchers
        )
        val spyDictionaryDao = SpyDictionaryDao(database.dictionaryDao())
        val spySeedMetadataDao = SpySeedMetadataDao(
            delegate = database.seedMetadataDao(),
            onBeforeUpsert = onBeforeSeedMetadataUpsert
        )
        val seedUpdater = VersionedDictionarySeedUpdater(
            assetSource = DictionarySeedAssetSource { seedStreamProvider() },
            sha256Calculator = SeedSha256Calculator(),
            versionComparator = SeedVersionComparator(),
            jsonParser = DictionarySeedJsonParser(moshi),
            database = database,
            dictionaryDao = spyDictionaryDao,
            seedMetadataDao = spySeedMetadataDao,
            syncMetadataDao = database.syncMetadataDao(),
            dispatchers = testDispatchers,
            clock = SeedClock(System::currentTimeMillis)
        )
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also {
            createdScopes += it
        }
        val syncRepository = ArchiveSyncRepositoryImpl(
            dictionaryRepository = dictionaryRepository,
            encyclopediaRepository = encyclopediaRepository,
            indigenousLandRepository = territoryRepository,
            syncMetadataDao = database.syncMetadataDao(),
            dictionarySeedUpdater = seedUpdater,
            networkConnectivityObserver = connectivityObserver,
            dispatchers = testDispatchers,
            applicationScope = appScope
        )
        return RealIntegrationStack(
            seedImporter = assetSeeder,
            seedUpdater = seedUpdater,
            spyDictionaryDao = spyDictionaryDao,
            spySeedMetadataDao = spySeedMetadataDao,
            dictionaryRepository = dictionaryRepository,
            territoryRepository = territoryRepository,
            syncRepository = syncRepository
        )
    }

    private class FakeNetworkConnectivityObserver(
        initialState: DeviceConnectivityState = DeviceConnectivityState.Online
    ) : NetworkConnectivityObserver {
        private val stateFlow = MutableStateFlow(initialState)
        override val connectivityState: StateFlow<DeviceConnectivityState> = stateFlow.asStateFlow()

        fun setState(state: DeviceConnectivityState) {
            stateFlow.value = state
        }
    }

    private data class RealIntegrationStack(
        val seedImporter: AssetDictionarySeedImporter,
        val seedUpdater: VersionedDictionarySeedUpdater,
        val spyDictionaryDao: SpyDictionaryDao,
        val spySeedMetadataDao: SpySeedMetadataDao,
        val dictionaryRepository: OfflineFirstDictionaryRepository,
        val territoryRepository: RoomIndigenousLandRepository,
        val syncRepository: ArchiveSyncRepositoryImpl
    )

    private class SpyDictionaryDao(
        private val delegate: DictionaryDao
    ) : DictionaryDao by delegate {
        var upsertAllCallCount: Int = 0
            private set

        override suspend fun upsertAll(entries: List<DictionaryEntryEntity>) {
            upsertAllCallCount += 1
            delegate.upsertAll(entries)
        }
    }

    private class SpySeedMetadataDao(
        private val delegate: SeedMetadataDao,
        private val onBeforeUpsert: (suspend (SeedMetadataEntity) -> Unit)? = null
    ) : SeedMetadataDao by delegate {
        override suspend fun upsert(metadata: SeedMetadataEntity) {
            onBeforeUpsert?.invoke(metadata)
            delegate.upsert(metadata)
        }
    }

    private fun syntheticSeedArrayJson(): String = """
        [
          {
            "id": "int_seed_1",
            "parintintin_word": "termo_sintetico_alpha",
            "phonetic_ipa": "/sintetico_alpha/",
            "portuguese_translation": "significado_sintetico_agua_clara",
            "grammatical_class": "NOUN",
            "semantic_category": "NATURE",
            "example_parintintin": "frase_sintetica_1",
            "example_portuguese": "traducao_frase_sintetica_1",
            "cultural_note": "nota_sintetica_rio",
            "audio_url": "https://example.invalid/audio/alpha.mp3",
            "updated_at_epoch_millis": 1700000000100
          },
          ${syntheticJsonEntry(
              id = "int_seed_2",
              word = "termo_sintetico_beta",
              translation = "significado_sintetico_mover",
              grammaticalClass = "VERB",
              semanticCategory = "ANIMALS"
          )},
          ${syntheticJsonEntry(
              id = "int_seed_3",
              word = "termo_sintetico_gama",
              translation = "significado_sintetico_doce",
              grammaticalClass = "ADJECTIVE",
              semanticCategory = "FOOD"
          )},
          ${syntheticJsonEntry(
              id = "int_seed_4",
              word = "termo_sintetico_delta",
              translation = "significado_sintetico_saudacao",
              grammaticalClass = "EXPRESSION",
              semanticCategory = "EVERYDAY"
          )}
        ]
    """.trimIndent()

    private fun syntheticJsonEntry(
        id: String,
        word: String,
        translation: String,
        grammaticalClass: String = "NOUN",
        semanticCategory: String = "NATURE",
        updatedAt: Long = 1_700_000_000_200L
    ): String = """
        {
          "id": "$id",
          "parintintin_word": "$word",
          "phonetic_ipa": "/$id/",
          "portuguese_translation": "$translation",
          "grammatical_class": "$grammaticalClass",
          "semantic_category": "$semanticCategory",
          "example_parintintin": "frase_$id",
          "example_portuguese": "traducao_frase_$id",
          "cultural_note": "nota_$id",
          "audio_url": null,
          "updated_at_epoch_millis": $updatedAt
        }
    """.trimIndent()

    private fun arcGisTerritoriesPageJson(vararg features: String): String = """
        {
          "exceededTransferLimit": false,
          "features": [${features.joinToString(",")}]
        }
    """.trimIndent()

    private fun syntheticArcGisFeature(
        objectId: Long,
        code: String,
        name: String
    ): String = """
        {
          "attributes": {
            "objectid": $objectId,
            "terrai_cod": "$code",
            "terrai_nom": "$name",
            "etnia_nome": "Parintintin",
            "municipio_": "Humaitá",
            "uf_sigla": "AM",
            "superficie": 215362.0,
            "fase_ti": "Regularizada",
            "modalidade": "Tradicionalmente Ocupada",
            "data_atual": "2024-08-19"
          }
        }
    """.trimIndent()

    private fun String.toUtf8Stream(): InputStream =
        ByteArrayInputStream(toByteArray(StandardCharsets.UTF_8))
}
