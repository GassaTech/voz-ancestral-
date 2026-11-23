package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.core.common.AppResult
import com.example.data.local.dao.DictionaryDao
import com.example.data.local.dao.SeedMetadataDao
import com.example.data.local.dao.SyncMetadataDao
import com.example.data.local.database.ParintintinDatabase
import com.example.data.local.entity.SyncMetadataEntity
import com.example.data.local.seed.DictionarySeedAssetSource
import com.example.data.local.seed.DictionarySeedSpec
import com.example.data.local.seed.DictionarySeedUpdater
import com.example.data.local.seed.SeedSha256Calculator
import com.example.data.local.seed.SeedUpdateOutcomeStatus
import com.example.core.common.ApplicationScope
import com.example.domain.model.SyncState
import com.example.domain.repository.SyncStatusRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class StartupSeedHiltInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createEmptyComposeRule()

    @Inject
    lateinit var database: ParintintinDatabase

    @Inject
    lateinit var dictionaryDao: DictionaryDao

    @Inject
    lateinit var seedMetadataDao: SeedMetadataDao

    @Inject
    lateinit var syncMetadataDao: SyncMetadataDao

    @Inject
    lateinit var syncStatusRepository: SyncStatusRepository

    @Inject
    lateinit var dictionarySeedUpdater: DictionarySeedUpdater

    @Inject
    lateinit var seedAssetSource: DictionarySeedAssetSource

    @Inject
    lateinit var sha256Calculator: SeedSha256Calculator

    @Inject
    lateinit var mockWebServer: MockWebServer

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        if (::applicationScope.isInitialized) {
            applicationScope.cancel()
        }
        if (::database.isInitialized && database.isOpen) {
            database.close()
        }
        if (::mockWebServer.isInitialized) {
            runBlocking(Dispatchers.IO) {
                runCatching { mockWebServer.shutdown() }
            }
        }
    }

    @Test
    fun inicializacaoComHiltRealAplicaSeedPersisteNoRoomEExibeNaDictionaryScreen() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody("""{"fields":[],"features":[],"exceededTransferLimit":false}""")
        )

        runBlocking {
            assertEquals(0, dictionaryDao.countOnce())
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("home_hero_banner").assertIsDisplayed()

            // Aguarda deterministicamente a conclusão do fluxo automático:
            // HomeViewModel.init -> ArchiveSyncCoordinator -> ArchiveSyncRepository -> Seed -> ArcGIS /query vazio -> RemoteEmpty
            val resolvedSyncState = runBlocking {
                withTimeout(SYNC_TIMEOUT_MILLIS) {
                    syncStatusRepository.syncState.first { it is SyncState.RemoteEmpty }
                }
            } as SyncState.RemoteEmpty

            assertNull(resolvedSyncState.lastSyncedEpochMillis)
            assertNull(resolvedSyncState.territoriesCount)
            assertEquals(1, mockWebServer.requestCount)
            val territoriesRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
            assertTrue(territoriesRequest?.path?.startsWith("/query?") == true)

            // Valida persistência direta no Room via DAOs injetados pelo Hilt
            runBlocking {
                assertEquals(EXPECTED_SEED_COUNT, dictionaryDao.countOnce())

                val persistedEntries = dictionaryDao.observeEntries(
                    query = "",
                    category = null,
                    onlyFavorites = false
                ).first()
                assertEquals(EXPECTED_SEED_COUNT, persistedEntries.size)

                val expectedIds = (1..EXPECTED_SEED_COUNT)
                    .map { index -> "DEMO-${index.toString().padStart(3, '0')}" }
                    .toSet()
                assertEquals(expectedIds, persistedEntries.map { it.id }.toSet())

                val expectedSeedHash = sha256Calculator.calculateFromSource(seedAssetSource)
                val seedMetadata = seedMetadataDao.getMetadataOnce()
                assertNotNull(seedMetadata)
                assertEquals(DictionarySeedSpec.CURRENT_SEED_VERSION, seedMetadata?.appliedSeedVersion)
                assertEquals(expectedSeedHash, seedMetadata?.appliedSeedHash)

                val syncMetadata = syncMetadataDao.getMetadataOnce()
                assertNotNull(syncMetadata)
                assertEquals(SyncMetadataEntity.STATUS_REMOTE_EMPTY, syncMetadata?.lastAttemptStatus)
                assertNull(syncMetadata?.lastSuccessfulSyncEpochMillis)
                assertNull(syncMetadata?.lastSuccessfulEntriesCount)
            }

            // Navega pela UI real da Home até a tela Dictionary
            composeTestRule.onNodeWithTag("bottom_nav_dictionary")
                .assertIsDisplayed()
                .performClick()

            composeTestRule.waitUntil(timeoutMillis = UI_TIMEOUT_MILLIS) {
                composeTestRule
                    .onAllNodes(
                        hasTestTag("dictionary_total_entries_count") and hasText("20 verbetes locais")
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeTestRule.onNodeWithTag("dictionary_screen_root").assertIsDisplayed()
            composeTestRule.onNodeWithTag("dictionary_total_entries_count")
                .assertIsDisplayed()
                .assertTextEquals("20 verbetes locais")

            composeTestRule.onNodeWithTag("dictionary_list")
                .performScrollToNode(hasTestTag("demo_mode_notice_card"))
            composeTestRule.onNodeWithTag("demo_mode_notice_card").assertIsDisplayed()
            composeTestRule.onNodeWithTag("demo_mode_notice_title")
                .assertIsDisplayed()
                .assertTextEquals("MODO DEMONSTRATIVO")

            composeTestRule.waitUntil(timeoutMillis = UI_TIMEOUT_MILLIS) {
                composeTestRule
                    .onAllNodesWithTag("dictionary_entries_count")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithTag("dictionary_list")
                .performScrollToNode(hasTestTag("dictionary_entries_count"))
            composeTestRule.onNodeWithTag("dictionary_entries_count")
                .assertIsDisplayed()
                .assertTextEquals("20 verbetes locais")

            // Confirma exibição de registros demonstrativos vindos do Room
            composeTestRule.onNodeWithTag("dictionary_list")
                .performScrollToNode(hasTestTag("dictionary_entry_card_DEMO-001"))
            composeTestRule.onNodeWithTag("dictionary_entry_card_DEMO-001").assertIsDisplayed()
            composeTestRule.onNodeWithText("Riolume").assertIsDisplayed()

            composeTestRule.onNodeWithTag("dictionary_list")
                .performScrollToNode(hasTestTag("dictionary_entry_card_DEMO-020"))
            composeTestRule.onNodeWithTag("dictionary_entry_card_DEMO-020").assertIsDisplayed()
            composeTestRule.onNodeWithText("Noitedegrilo").assertIsDisplayed()

            // Confirma integridade após exibição e resposta remota vazia
            runBlocking {
                assertEquals(EXPECTED_SEED_COUNT, dictionaryDao.countOnce())
                assertNull(syncMetadataDao.getMetadataOnce()?.lastSuccessfulSyncEpochMillis)
            }
        }
    }

    @Test
    fun idempotenciaComSeedJaAplicadoRetornaAlreadyUpToDateSemDuplicarRegistros() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody("""{"fields":[],"features":[],"exceededTransferLimit":false}""")
        )

        ActivityScenario.launch(MainActivity::class.java).use {
            composeTestRule.waitForIdle()

            runBlocking {
                withTimeout(SYNC_TIMEOUT_MILLIS) {
                    syncStatusRepository.syncState.first { it is SyncState.RemoteEmpty }
                }
            }

            val initialMetadata = runBlocking {
                assertEquals(EXPECTED_SEED_COUNT, dictionaryDao.countOnce())
                seedMetadataDao.getMetadataOnce()
            }
            assertNotNull(initialMetadata)

            // Nova execução do fluxo de atualização do Seed deve ser idempotente (ALREADY_UP_TO_DATE)
            val secondSeedOutcome = runBlocking {
                dictionarySeedUpdater.updateIfNeeded()
            }
            assertTrue(secondSeedOutcome is AppResult.Success)
            val outcome = (secondSeedOutcome as AppResult.Success).data
            assertEquals(SeedUpdateOutcomeStatus.ALREADY_UP_TO_DATE, outcome.status)
            assertEquals(0, outcome.entriesProcessedCount)

            runBlocking {
                assertEquals(EXPECTED_SEED_COUNT, dictionaryDao.countOnce())
                assertEquals(initialMetadata, seedMetadataDao.getMetadataOnce())
            }

            // Navega ao Dicionário e confirma contagem única sem duplicação
            composeTestRule.onNodeWithTag("bottom_nav_dictionary").performClick()
            composeTestRule.waitUntil(timeoutMillis = UI_TIMEOUT_MILLIS) {
                composeTestRule
                    .onAllNodes(
                        hasTestTag("dictionary_total_entries_count") and hasText("20 verbetes locais")
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeTestRule.onNodeWithTag("dictionary_total_entries_count")
                .assertIsDisplayed()
                .assertTextEquals("20 verbetes locais")
        }
    }

    companion object {
        private const val EXPECTED_SEED_COUNT = 20
        private const val SYNC_TIMEOUT_MILLIS = 10_000L
        private const val UI_TIMEOUT_MILLIS = 5_000L
    }
}
