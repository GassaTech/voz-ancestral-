package com.example.data.local.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.data.local.database.ParintintinDatabase
import com.example.data.local.entity.SeedMetadataEntity
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DictionarySeedChangeDetectorTest {

    private lateinit var context: Context
    private lateinit var database: ParintintinDatabase
    private lateinit var assetSource: DictionarySeedAssetSource
    private val sha256Calculator = SeedSha256Calculator()
    private val versionComparator = SeedVersionComparator()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ParintintinDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        assetSource = AndroidDictionarySeedAssetSource(context)
    }

    @After
    fun tearDown() {
        if (database.isOpen) {
            database.close()
        }
    }

    @Test
    fun `inspecao do asset real com banco vazio e metadata inicial retorna SEED_NOT_APPLIED sem escrever no banco`() = runBlocking {
        val detector = createDetector(assetSource)

        val expectedHash = sha256Calculator.calculateFromSource(assetSource)
        assertTrue(expectedHash.matches(Regex("^[0-9a-f]{64}$")))

        // Cenário 1: nenhuma linha em seed_metadata ainda
        val missingMetadataResult = detector.inspectSeedStatus()
        assertTrue(missingMetadataResult is AppResult.Success)
        val missingInspection = (missingMetadataResult as AppResult.Success).data
        assertEquals(SeedChangeStatus.SEED_NOT_APPLIED, missingInspection.status)
        assertEquals(expectedHash, missingInspection.currentSeedHash)
        assertNull(missingInspection.appliedSeedVersion)
        assertEquals(0, database.dictionaryDao().countOnce())
        assertNull(database.seedMetadataDao().getMetadataOnce())

        // Cenário 2: registro singleton inicial (versão 0, hash nulo)
        database.seedMetadataDao().upsert(
            SeedMetadataEntity(
                id = SeedMetadataEntity.SINGLETON_ID,
                appliedSeedVersion = SeedMetadataEntity.INITIAL_SEED_VERSION,
                appliedSeedHash = null,
                appliedSeedIdsJson = null,
                updatedAtEpochMillis = 0L
            )
        )
        val initialResult = detector.inspectSeedStatus()
        val initialInspection = (initialResult as AppResult.Success).data
        assertEquals(SeedChangeStatus.SEED_NOT_APPLIED, initialInspection.status)
        assertEquals(0, initialInspection.appliedSeedVersion)
        assertEquals(0, database.dictionaryDao().countOnce())
    }

    @Test
    fun `inspecao identifica SEED_UP_TO_DATE SEED_UPDATED e INCONSISTENT_VERSION lendo SeedMetadataDao`() = runBlocking {
        val detector = createDetector(assetSource)
        val currentHash = sha256Calculator.calculateFromSource(assetSource)

        // SEED_UP_TO_DATE
        database.seedMetadataDao().upsert(
            SeedMetadataEntity(
                id = SeedMetadataEntity.SINGLETON_ID,
                appliedSeedVersion = DictionarySeedSpec.CURRENT_SEED_VERSION,
                appliedSeedHash = currentHash,
                appliedSeedIdsJson = "[\"DEMO-001\"]",
                updatedAtEpochMillis = 1_700_000_000_000L
            )
        )
        val upToDate = (detector.inspectSeedStatus() as AppResult.Success).data
        assertEquals(SeedChangeStatus.SEED_UP_TO_DATE, upToDate.status)
        assertEquals(currentHash, upToDate.currentSeedHash)

        // SEED_UPDATED
        database.seedMetadataDao().upsert(
            SeedMetadataEntity(
                id = SeedMetadataEntity.SINGLETON_ID,
                appliedSeedVersion = DictionarySeedSpec.CURRENT_SEED_VERSION,
                appliedSeedHash = "0".repeat(64),
                appliedSeedIdsJson = "[\"DEMO-001\"]",
                updatedAtEpochMillis = 1_700_000_000_000L
            )
        )
        val updated = (detector.inspectSeedStatus() as AppResult.Success).data
        assertEquals(SeedChangeStatus.SEED_UPDATED, updated.status)
        assertEquals("0".repeat(64), updated.appliedSeedHash)

        // INCONSISTENT_VERSION
        database.seedMetadataDao().upsert(
            SeedMetadataEntity(
                id = SeedMetadataEntity.SINGLETON_ID,
                appliedSeedVersion = DictionarySeedSpec.CURRENT_SEED_VERSION + 1,
                appliedSeedHash = currentHash,
                updatedAtEpochMillis = 1_700_000_000_000L
            )
        )
        val inconsistent = (detector.inspectSeedStatus() as AppResult.Success).data
        assertEquals(SeedChangeStatus.INCONSISTENT_VERSION, inconsistent.status)
        assertTrue(inconsistent.isInconsistentVersion)

        // Garante que nenhuma leitura escreveu em dictionary_entries
        assertEquals(0, database.dictionaryDao().countOnce())
    }

    @Test
    fun `arquivo vazio ou falha de leitura retornam AppResult Error e CancellationException e relancada`() = runBlocking {
        val emptyDetector = createDetector { ByteArrayInputStream(ByteArray(0)) }
        val emptyResult = emptyDetector.inspectSeedStatus()
        assertTrue(emptyResult is AppResult.Error)
        assertTrue((emptyResult as AppResult.Error).cause is EmptySeedFileException)

        val missingFileDetector = createDetector { throw FileNotFoundException("dictionary_seed.json ausente") }
        val missingResult = missingFileDetector.inspectSeedStatus()
        assertTrue(missingResult is AppResult.Error)
        assertTrue((missingResult as AppResult.Error).cause is FileNotFoundException)

        val ioErrorDetector = createDetector { throw IOException("Falha de E/S simulada") }
        val ioResult = ioErrorDetector.inspectSeedStatus()
        assertTrue(ioResult is AppResult.Error)
        assertTrue((ioResult as AppResult.Error).cause is IOException)

        val cancelledDetector = createDetector { throw CancellationException("Inspeção cancelada") }
        try {
            cancelledDetector.inspectSeedStatus()
            fail("Era esperado relançar CancellationException.")
        } catch (_: CancellationException) {
            // Comportamento esperado
        }
    }

    private fun createDetector(source: DictionarySeedAssetSource): DefaultDictionarySeedChangeDetector =
        DefaultDictionarySeedChangeDetector(
            assetSource = source,
            sha256Calculator = sha256Calculator,
            seedMetadataDao = database.seedMetadataDao(),
            versionComparator = versionComparator,
            dispatchers = IoDispatcherProvider
        )

    private object IoDispatcherProvider : DispatcherProvider {
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val default: CoroutineDispatcher = Dispatchers.Default
        override val main: CoroutineDispatcher = Dispatchers.Main
    }
}
