package com.example.data.local.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.DictionaryDao
import com.example.data.local.dao.SeedMetadataDao
import com.example.data.local.database.ParintintinDatabase
import com.example.data.local.entity.CulturalArticleEntity
import com.example.data.local.entity.DictionaryEntryEntity
import com.example.data.local.entity.SeedMetadataEntity
import com.example.data.local.entity.SyncMetadataEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
class VersionedDictionarySeedUpdaterTest {

    private lateinit var context: Context
    private lateinit var database: ParintintinDatabase
    private lateinit var spyDictionaryDao: SpyDictionaryDao
    private lateinit var spySeedMetadataDao: SpySeedMetadataDao
    private lateinit var moshi: Moshi
    private lateinit var jsonParser: DictionarySeedJsonParser
    private val sha256Calculator = SeedSha256Calculator()
    private val versionComparator = SeedVersionComparator()
    private var fixedClockMillis: Long = 1_700_000_999_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ParintintinDatabase::class.java)
            .addCallback(ParintintinDatabase.SEED_INITIALIZATION_CALLBACK)
            .allowMainThreadQueries()
            .build()
        spyDictionaryDao = SpyDictionaryDao(database.dictionaryDao())
        spySeedMetadataDao = SpySeedMetadataDao(database.seedMetadataDao())
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        jsonParser = DictionarySeedJsonParser(moshi)
    }

    @After
    fun tearDown() {
        if (database.isOpen) {
            database.close()
        }
    }

    @Test
    fun `A - atualizacao de instalacao antiga com 20 registros DEMO aplica todos os 20 termos e significados atuais`() = runBlocking {
        val legacyDemoEntries = (1..20).map { idx ->
            val id = "DEMO-${idx.toString().padStart(3, '0')}"
            sampleEntity(
                id = id,
                word = "Termo legado $idx",
                translation = "Significado legado $idx",
                culturalNote = "Nota antiga $idx",
                isFavorite = false
            )
        }
        database.dictionaryDao().upsertAll(legacyDemoEntries)
        database.seedMetadataDao().upsert(
            SeedMetadataEntity(
                id = SeedMetadataEntity.SINGLETON_ID,
                appliedSeedVersion = 0,
                appliedSeedHash = null,
                appliedSeedIdsJson = "[\"DEMO-001\",\"DEMO-020\"]",
                updatedAtEpochMillis = 0L
            )
        )

        val realAssetSource = AndroidDictionarySeedAssetSource(context)
        val expectedHash = sha256Calculator.calculateFromSource(realAssetSource)
        val updater = createUpdater(realAssetSource)

        val result = updater.updateIfNeeded()

        assertTrue(result is AppResult.Success)
        val outcome = (result as AppResult.Success).data
        assertEquals(SeedUpdateOutcomeStatus.UPDATED, outcome.status)
        assertEquals(20, outcome.entriesProcessedCount)
        assertEquals(DictionarySeedSpec.CURRENT_SEED_VERSION, outcome.appliedSeedVersion)
        assertEquals(expectedHash, outcome.appliedSeedHash)

        val entriesById = database.dictionaryDao()
            .observeEntries(query = "", category = null, onlyFavorites = false)
            .first()
            .associateBy { it.id }
        assertEquals(20, entriesById.size)

        val expectedPairs = mapOf(
            "DEMO-001" to ("Riolume" to "brilho refletido na superfície de um rio."),
            "DEMO-002" to ("Folhaverde" to "folha nova após a chuva."),
            "DEMO-003" to ("Brisamata" to "vento leve entre as árvores."),
            "DEMO-004" to ("Chuvafina" to "chuva suave sobre a floresta."),
            "DEMO-005" to ("Raizforte" to "raízes que sustentam o solo."),
            "DEMO-006" to ("Orvaluz" to "gota de orvalho iluminada."),
            "DEMO-007" to ("Ventocopa" to "movimento do vento nas copas das árvores."),
            "DEMO-008" to ("Pedralisa" to "pedra suavizada pela água."),
            "DEMO-009" to ("Nuvemrio" to "nuvem refletida na água."),
            "DEMO-010" to ("Solnascente" to "primeira luz da manhã."),
            "DEMO-011" to ("Flor d'água" to "flor que cresce em ambiente aquático."),
            "DEMO-012" to ("Sementeluz" to "semente que representa renovação."),
            "DEMO-013" to ("Águaclara" to "água transparente de um igarapé."),
            "DEMO-014" to ("Sombramata" to "sombra formada pelas árvores."),
            "DEMO-015" to ("Frutaviva" to "fruto encontrado na floresta."),
            "DEMO-016" to ("Cantodamata" to "sons dos animais e da floresta."),
            "DEMO-017" to ("Barrovermelho" to "solo avermelhado da região amazônica."),
            "DEMO-018" to ("Trilhaverde" to "caminho entre a vegetação."),
            "DEMO-019" to ("Céuespelhado" to "reflexo do céu na superfície da água."),
            "DEMO-020" to ("Noitedegrilo" to "sons dos grilos durante a noite.")
        )

        expectedPairs.forEach { (id, wordAndTranslation) ->
            val persisted = entriesById.getValue(id)
            assertEquals(wordAndTranslation.first, persisted.parintintinWord)
            assertEquals(wordAndTranslation.second, persisted.portugueseTranslation)
        }
    }

    @Test
    fun `B - atualizacao preserva favoritos em registros DEMO existentes e inicia novos sem favorito`() = runBlocking {
        database.dictionaryDao().upsertAll(
            listOf(
                sampleEntity(id = "DEMO-001", word = "Antigo 1", translation = "Trad 1", isFavorite = true),
                sampleEntity(id = "DEMO-002", word = "Antigo 2", translation = "Trad 2", isFavorite = false),
                sampleEntity(id = "DEMO-005", word = "Antigo 5", translation = "Trad 5", isFavorite = true)
            )
        )
        val newSeedJson = """
            [
              ${jsonEntry("DEMO-001", "Riolume atualizado", "brilho atualizado")},
              ${jsonEntry("DEMO-002", "Folhaverde atualizado", "folha atualizada")},
              ${jsonEntry("DEMO-005", "Raizforte atualizado", "raiz atualizada")},
              ${jsonEntry("DEMO-021", "Novoverbete", "novo registro de demonstração")}
            ]
        """.trimIndent()
        val updater = createUpdater { newSeedJson.toUtf8Stream() }

        val result = updater.updateIfNeeded()

        assertTrue(result is AppResult.Success)
        assertEquals(SeedUpdateOutcomeStatus.UPDATED, (result as AppResult.Success).data.status)

        val entriesById = database.dictionaryDao()
            .observeEntries(query = "", category = null, onlyFavorites = false)
            .first()
            .associateBy { it.id }

        assertEquals("Riolume atualizado", entriesById.getValue("DEMO-001").parintintinWord)
        assertTrue(entriesById.getValue("DEMO-001").isFavorite)
        assertFalse(entriesById.getValue("DEMO-002").isFavorite)
        assertTrue(entriesById.getValue("DEMO-005").isFavorite)
        assertFalse(entriesById.getValue("DEMO-021").isFavorite)
        assertEquals(listOf("DEMO-001", "DEMO-005"), database.dictionaryDao().getFavoriteIds().sorted())
    }

    @Test
    fun `C - preservacao integral de registros nao-DEMO artigos culturais e sync_metadata`() = runBlocking {
        database.dictionaryDao().upsertAll(
            listOf(
                sampleEntity(id = "DEMO-001", word = "Legado", translation = "Legado", isFavorite = false),
                sampleEntity(
                    id = "custom_entry_1",
                    word = "Registro pessoal 1",
                    translation = "Tradução pessoal 1",
                    culturalNote = "Nota pessoal",
                    isFavorite = true,
                    updatedAt = 1_234L
                ),
                sampleEntity(
                    id = "custom_entry_2",
                    word = "Registro pessoal 2",
                    translation = "Tradução pessoal 2",
                    isFavorite = false,
                    updatedAt = 5_678L
                )
            )
        )
        val article = CulturalArticleEntity(
            id = "article_preserve_1",
            title = "História preservada",
            subtitle = "Subtítulo",
            summary = "Resumo",
            bodyParagraphs = listOf("Parágrafo 1"),
            topic = "HISTORY",
            readingTimeMinutes = 3,
            sourceReference = "Fonte",
            isBookmarked = true,
            updatedAtEpochMillis = 9_999L
        )
        database.encyclopediaDao().upsertAll(listOf(article))
        val offlineSyncMetadata = SyncMetadataEntity(
            id = SyncMetadataEntity.SINGLETON_ID,
            lastAttemptEpochMillis = 55_000L,
            lastAttemptStatus = SyncMetadataEntity.STATUS_OFFLINE_FALLBACK,
            lastFailureReason = "Sem rede",
            lastSuccessfulSyncEpochMillis = null,
            lastSuccessfulEntriesCount = null,
            lastSuccessfulArticlesCount = null,
            lastSuccessfulRemoteVersion = null
        )
        database.syncMetadataDao().upsert(offlineSyncMetadata)

        val updater = createUpdater(AndroidDictionarySeedAssetSource(context))
        val result = updater.updateIfNeeded()

        assertTrue(result is AppResult.Success)
        assertEquals(SeedUpdateOutcomeStatus.UPDATED, (result as AppResult.Success).data.status)

        val entriesById = database.dictionaryDao()
            .observeEntries(query = "", category = null, onlyFavorites = false)
            .first()
            .associateBy { it.id }
        assertEquals(22, entriesById.size)

        val custom1 = entriesById.getValue("custom_entry_1")
        assertEquals("Registro pessoal 1", custom1.parintintinWord)
        assertEquals("Nota pessoal", custom1.culturalNote)
        assertTrue(custom1.isFavorite)
        assertEquals(1_234L, custom1.updatedAtEpochMillis)

        val custom2 = entriesById.getValue("custom_entry_2")
        assertEquals("Registro pessoal 2", custom2.parintintinWord)
        assertFalse(custom2.isFavorite)
        assertEquals(5_678L, custom2.updatedAtEpochMillis)

        val preservedArticles = database.encyclopediaDao()
            .observeArticles(query = "", topic = null, onlyBookmarked = false)
            .first()
        assertEquals(listOf(article), preservedArticles)
        assertEquals(offlineSyncMetadata, database.syncMetadataDao().getMetadataOnce())
    }

    @Test
    fun `D - instalacao nova com banco vazio importa 20 registros e grava metadata corretamente`() = runBlocking {
        // Força criação do banco com callback de versão inicial 0
        assertEquals(0, database.dictionaryDao().countOnce())
        val initialMetadata = database.seedMetadataDao().getMetadataOnce()
        assertNotNull(initialMetadata)
        assertEquals(0, initialMetadata?.appliedSeedVersion)
        assertNull(initialMetadata?.appliedSeedHash)

        val realAssetSource = AndroidDictionarySeedAssetSource(context)
        val expectedHash = sha256Calculator.calculateFromSource(realAssetSource)
        fixedClockMillis = 1_700_123_456_789L
        val updater = createUpdater(realAssetSource)

        val result = updater.updateIfNeeded()

        assertTrue(result is AppResult.Success)
        val outcome = (result as AppResult.Success).data
        assertEquals(SeedUpdateOutcomeStatus.APPLIED_INITIAL, outcome.status)
        assertEquals(20, outcome.entriesProcessedCount)
        assertEquals(DictionarySeedSpec.CURRENT_SEED_VERSION, outcome.appliedSeedVersion)
        assertEquals(expectedHash, outcome.appliedSeedHash)

        assertEquals(20, database.dictionaryDao().countOnce())
        val persistedMetadata = database.seedMetadataDao().getMetadataOnce()
        assertNotNull(persistedMetadata)
        assertEquals(DictionarySeedSpec.CURRENT_SEED_VERSION, persistedMetadata?.appliedSeedVersion)
        assertEquals(expectedHash, persistedMetadata?.appliedSeedHash)
        assertEquals(fixedClockMillis, persistedMetadata?.updatedAtEpochMillis)
        assertTrue(persistedMetadata?.appliedSeedIdsJson?.contains("DEMO-001") == true)
        assertTrue(persistedMetadata?.appliedSeedIdsJson?.contains("DEMO-020") == true)
    }

    @Test
    fun `E - idempotencia garante que segunda execucao com mesmo JSON nao executa upsert nem altera metadata`() = runBlocking {
        val realAssetSource = AndroidDictionarySeedAssetSource(context)
        val updater = createUpdater(realAssetSource)

        fixedClockMillis = 1_700_000_100_000L
        val firstResult = updater.updateIfNeeded()
        assertTrue(firstResult is AppResult.Success)
        assertEquals(SeedUpdateOutcomeStatus.APPLIED_INITIAL, (firstResult as AppResult.Success).data.status)
        assertEquals(1, spyDictionaryDao.upsertCallCount)
        assertEquals(1, spySeedMetadataDao.upsertCallCount)

        val metadataAfterFirst = database.seedMetadataDao().getMetadataOnce()

        fixedClockMillis = 1_700_000_900_000L
        val secondResult = updater.updateIfNeeded()

        assertTrue(secondResult is AppResult.Success)
        val secondOutcome = (secondResult as AppResult.Success).data
        assertEquals(SeedUpdateOutcomeStatus.ALREADY_UP_TO_DATE, secondOutcome.status)
        assertEquals(0, secondOutcome.entriesProcessedCount)
        assertEquals(1, spyDictionaryDao.upsertCallCount)
        assertEquals(1, spySeedMetadataDao.upsertCallCount)
        assertEquals(metadataAfterFirst, database.seedMetadataDao().getMetadataOnce())

        // Se o conteúdo mudar na mesma versão, o hash diferente deve disparar UPDATED
        val modifiedJson = "[${jsonEntry("DEMO-001", "Riolume revisado", "nova tradução")}]"
        val modifiedUpdater = createUpdater { modifiedJson.toUtf8Stream() }
        val thirdResult = modifiedUpdater.updateIfNeeded()
        assertTrue(thirdResult is AppResult.Success)
        assertEquals(SeedUpdateOutcomeStatus.UPDATED, (thirdResult as AppResult.Success).data.status)
    }

    @Test
    fun `F - protecao contra sobrescrita bloqueia atualizacao quando ha sincronizacao oficial bem sucedida`() = runBlocking {
        val officialEntry = sampleEntity(
            id = "DEMO-001",
            word = "Palavra oficial sincronizada",
            translation = "Tradução oficial",
            isFavorite = true
        )
        database.dictionaryDao().upsertAll(listOf(officialEntry))
        val initialSeedMetadata = SeedMetadataEntity(
            appliedSeedVersion = 0,
            appliedSeedHash = null,
            appliedSeedIdsJson = null,
            updatedAtEpochMillis = 0L
        )
        database.seedMetadataDao().upsert(initialSeedMetadata)
        database.syncMetadataDao().upsert(
            SyncMetadataEntity(
                id = SyncMetadataEntity.SINGLETON_ID,
                lastAttemptEpochMillis = 1_700_000_500_000L,
                lastAttemptStatus = SyncMetadataEntity.STATUS_SYNCED,
                lastFailureReason = null,
                lastSuccessfulSyncEpochMillis = 1_700_000_500_000L,
                lastSuccessfulEntriesCount = 1,
                lastSuccessfulArticlesCount = 0,
                lastSuccessfulRemoteVersion = "official-v1"
            )
        )
        spyDictionaryDao.upsertCallCount = 0
        spySeedMetadataDao.upsertCallCount = 0

        val updater = createUpdater(AndroidDictionarySeedAssetSource(context))
        val result = updater.updateIfNeeded()

        assertTrue(result is AppResult.Success)
        val outcome = (result as AppResult.Success).data
        assertEquals(SeedUpdateOutcomeStatus.BLOCKED_BY_OFFICIAL_SYNC, outcome.status)
        assertEquals(0, outcome.entriesProcessedCount)
        assertEquals(0, spyDictionaryDao.upsertCallCount)
        assertEquals(0, spySeedMetadataDao.upsertCallCount)
        assertEquals(1, database.dictionaryDao().countOnce())
        assertEquals(officialEntry, database.dictionaryDao().observeEntryById("DEMO-001").first())
        assertEquals(initialSeedMetadata, database.seedMetadataDao().getMetadataOnce())
    }

    @Test
    fun `G - JSON invalido ou arquivo vazio retorna AppResult Error sem modificar banco nem favoritos`() = runBlocking {
        val existing = sampleEntity(
            id = "DEMO-001",
            word = "Preservado",
            translation = "Preservado",
            isFavorite = true
        )
        database.dictionaryDao().upsertAll(listOf(existing))
        val existingMetadata = SeedMetadataEntity(
            appliedSeedVersion = 1,
            appliedSeedHash = "hash-v1",
            appliedSeedIdsJson = "[\"DEMO-001\"]",
            updatedAtEpochMillis = 10_000L
        )
        database.seedMetadataDao().upsert(existingMetadata)
        spyDictionaryDao.upsertCallCount = 0
        spySeedMetadataDao.upsertCallCount = 0

        val invalidPayloads = listOf(
            "",
            "{ invalid json",
            "[]",
            """[{"id":"DEMO-001","parintintin_word":"","portuguese_translation":"x","grammatical_class":"NOUN","semantic_category":"NATURE","updated_at_epoch_millis":1}]""",
            """[{"id":"DEMO-001","parintintin_word":"A","portuguese_translation":"B","grammatical_class":"DESCONHECIDA","semantic_category":"NATURE","updated_at_epoch_millis":1}]""",
            """[${jsonEntry("DEMO-001", "A", "B")},${jsonEntry("DEMO-001", "C", "D")}]""",
            """[{"id":"DEMO-001","parintintin_word":"A","portuguese_translation":"B","grammatical_class":"NOUN","semantic_category":"NATURE","updated_at_epoch_millis":-5}]"""
        )

        invalidPayloads.forEach { payload ->
            val updater = createUpdater { payload.toUtf8Stream() }
            val result = updater.updateIfNeeded()
            assertTrue("Esperado AppResult.Error para payload '$payload'", result is AppResult.Error)
        }

        assertEquals(0, spyDictionaryDao.upsertCallCount)
        assertEquals(0, spySeedMetadataDao.upsertCallCount)
        assertEquals(existing, database.dictionaryDao().observeEntryById("DEMO-001").first())
        assertEquals(existingMetadata, database.seedMetadataDao().getMetadataOnce())
    }

    @Test
    fun `H - atomicidade garante rollback integral quando ocorre falha durante persistencia da metadata`() = runBlocking {
        val existingEntry = sampleEntity(
            id = "DEMO-001",
            word = "Valor anterior",
            translation = "Significado anterior",
            isFavorite = true
        )
        database.dictionaryDao().upsertAll(listOf(existingEntry))
        val previousMetadata = SeedMetadataEntity(
            appliedSeedVersion = 0,
            appliedSeedHash = null,
            appliedSeedIdsJson = "[\"DEMO-001\"]",
            updatedAtEpochMillis = 0L
        )
        database.seedMetadataDao().upsert(previousMetadata)

        spySeedMetadataDao.onBeforeUpsert = {
            throw IllegalStateException("Falha simulada ao gravar seed_metadata")
        }

        val updater = createUpdater(AndroidDictionarySeedAssetSource(context))
        val result = updater.updateIfNeeded()

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).cause is IllegalStateException)
        // Verifica que o upsertAll em dictionary_entries foi revertido pela transação
        assertEquals(1, database.dictionaryDao().countOnce())
        assertEquals(existingEntry, database.dictionaryDao().observeEntryById("DEMO-001").first())
        assertEquals(previousMetadata, database.seedMetadataDao().getMetadataOnce())
    }

    @Test
    fun `I - versao aplicada superior a versao atual bloqueia por inconsistencia sem alterar dados`() = runBlocking {
        val existing = sampleEntity(id = "DEMO-001", word = "Existente", translation = "Existente", isFavorite = true)
        database.dictionaryDao().upsertAll(listOf(existing))
        val higherVersionMetadata = SeedMetadataEntity(
            appliedSeedVersion = DictionarySeedSpec.CURRENT_SEED_VERSION + 1,
            appliedSeedHash = "future-hash",
            appliedSeedIdsJson = "[\"DEMO-001\"]",
            updatedAtEpochMillis = 88_888L
        )
        database.seedMetadataDao().upsert(higherVersionMetadata)
        spyDictionaryDao.upsertCallCount = 0
        spySeedMetadataDao.upsertCallCount = 0

        val updater = createUpdater(AndroidDictionarySeedAssetSource(context))
        val result = updater.updateIfNeeded()

        assertTrue(result is AppResult.Success)
        val outcome = (result as AppResult.Success).data
        assertEquals(SeedUpdateOutcomeStatus.BLOCKED_INCONSISTENT_VERSION, outcome.status)
        assertEquals(0, outcome.entriesProcessedCount)
        assertEquals(0, spyDictionaryDao.upsertCallCount)
        assertEquals(0, spySeedMetadataDao.upsertCallCount)
        assertEquals(existing, database.dictionaryDao().observeEntryById("DEMO-001").first())
        assertEquals(higherVersionMetadata, database.seedMetadataDao().getMetadataOnce())
    }

    @Test
    fun `J - execucao concorrente serializa atualizacoes garantindo apenas uma escrita efetiva`() = runBlocking {
        val realBytes = AndroidDictionarySeedAssetSource(context).openSeedStream().use { it.readBytes() }
        val openCount = AtomicInteger(0)
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()

        val gatedSource = DictionarySeedAssetSource {
            if (openCount.incrementAndGet() == 1) {
                firstEntered.complete(Unit)
                while (!releaseFirst.isCompleted) {
                    Thread.sleep(10)
                }
            }
            ByteArrayInputStream(realBytes)
        }
        val updater = createUpdater(gatedSource)

        val results = withTimeout(5_000L) {
            val first = async(Dispatchers.IO) { updater.updateIfNeeded() }
            firstEntered.await()
            val second = async(Dispatchers.IO) { updater.updateIfNeeded() }
            val third = async(Dispatchers.IO) { updater.updateIfNeeded() }
            releaseFirst.complete(Unit)
            awaitAll(first, second, third)
        }

        val outcomes = results.map { (it as AppResult.Success).data.status }
        assertEquals(1, outcomes.count { it == SeedUpdateOutcomeStatus.APPLIED_INITIAL })
        assertEquals(2, outcomes.count { it == SeedUpdateOutcomeStatus.ALREADY_UP_TO_DATE })
        assertEquals(1, spyDictionaryDao.upsertCallCount)
        assertEquals(1, spySeedMetadataDao.upsertCallCount)
        assertEquals(20, database.dictionaryDao().countOnce())
    }

    @Test
    fun `K - IDs antigos ausentes no novo JSON nao sao excluidos automaticamente`() = runBlocking {
        val obsoleteDemo = sampleEntity(
            id = "DEMO-999",
            word = "Legado descontinuado",
            translation = "Mantido sem exclusão",
            isFavorite = true
        )
        val updatedDemo = sampleEntity(
            id = "DEMO-001",
            word = "Antigo 1",
            translation = "Trad antiga 1",
            isFavorite = false
        )
        database.dictionaryDao().upsertAll(listOf(obsoleteDemo, updatedDemo))

        val subsetJson = """
            [
              ${jsonEntry("DEMO-001", "Riolume", "brilho refletido na superfície de um rio.")},
              ${jsonEntry("DEMO-002", "Folhaverde", "folha nova após a chuva.")}
            ]
        """.trimIndent()
        val updater = createUpdater { subsetJson.toUtf8Stream() }

        val result = updater.updateIfNeeded()

        assertTrue(result is AppResult.Success)
        assertEquals(SeedUpdateOutcomeStatus.UPDATED, (result as AppResult.Success).data.status)

        val entriesById = database.dictionaryDao()
            .observeEntries(query = "", category = null, onlyFavorites = false)
            .first()
            .associateBy { it.id }
        assertEquals(3, entriesById.size)
        assertEquals(obsoleteDemo, entriesById.getValue("DEMO-999"))
        assertEquals("Riolume", entriesById.getValue("DEMO-001").parintintinWord)
        assertEquals("Folhaverde", entriesById.getValue("DEMO-002").parintintinWord)
    }

    @Test
    fun `CancellationException durante leitura e relancada`() = runBlocking {
        val updater = createUpdater { throw CancellationException("Cancelado") }
        try {
            updater.updateIfNeeded()
            fail("Era esperado relançar CancellationException.")
        } catch (_: CancellationException) {
            assertEquals(0, database.dictionaryDao().countOnce())
        }
    }

    private fun createUpdater(assetSource: DictionarySeedAssetSource): VersionedDictionarySeedUpdater =
        VersionedDictionarySeedUpdater(
            assetSource = assetSource,
            sha256Calculator = sha256Calculator,
            versionComparator = versionComparator,
            jsonParser = jsonParser,
            database = database,
            dictionaryDao = spyDictionaryDao,
            seedMetadataDao = spySeedMetadataDao,
            syncMetadataDao = database.syncMetadataDao(),
            dispatchers = IoDispatcherProvider,
            clock = SeedClock { fixedClockMillis }
        )

    private fun String.toUtf8Stream(): InputStream =
        ByteArrayInputStream(toByteArray(StandardCharsets.UTF_8))

    private fun jsonEntry(
        id: String,
        word: String,
        translation: String
    ): String = """
        {
          "id": "$id",
          "parintintin_word": "$word",
          "phonetic_ipa": "",
          "portuguese_translation": "$translation",
          "grammatical_class": "NOUN",
          "semantic_category": "NATURE",
          "example_parintintin": "",
          "example_portuguese": "",
          "cultural_note": "Conteúdo fictício exclusivo para demonstração do aplicativo.",
          "audio_url": null,
          "updated_at_epoch_millis": 1700000000000
        }
    """.trimIndent()

    private fun sampleEntity(
        id: String,
        word: String,
        translation: String,
        culturalNote: String = "Nota $id",
        isFavorite: Boolean,
        updatedAt: Long = 1_000L
    ): DictionaryEntryEntity = DictionaryEntryEntity(
        id = id,
        parintintinWord = word,
        phoneticIpa = "",
        portugueseTranslation = translation,
        grammaticalClass = "NOUN",
        semanticCategory = "NATURE",
        exampleParintintin = "",
        examplePortuguese = "",
        culturalNote = culturalNote,
        audioUrl = null,
        isFavorite = isFavorite,
        updatedAtEpochMillis = updatedAt
    )

    private class SpyDictionaryDao(
        private val delegate: DictionaryDao
    ) : DictionaryDao by delegate {
        var upsertCallCount: Int = 0

        override suspend fun upsertAll(entries: List<DictionaryEntryEntity>) {
            upsertCallCount += 1
            delegate.upsertAll(entries)
        }
    }

    private class SpySeedMetadataDao(
        private val delegate: SeedMetadataDao
    ) : SeedMetadataDao by delegate {
        var upsertCallCount: Int = 0
        var onBeforeUpsert: (() -> Unit)? = null

        override suspend fun upsert(metadata: SeedMetadataEntity) {
            upsertCallCount += 1
            onBeforeUpsert?.invoke()
            delegate.upsert(metadata)
        }
    }

    private object IoDispatcherProvider : DispatcherProvider {
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val default: CoroutineDispatcher = Dispatchers.Default
        override val main: CoroutineDispatcher = Dispatchers.Main
    }
}
