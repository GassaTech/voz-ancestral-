package com.example.data.local.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.DictionaryDao
import com.example.data.local.database.ParintintinDatabase
import com.example.data.local.entity.DictionaryEntryEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException
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
class DictionarySeedImporterTest {

    private lateinit var context: Context
    private lateinit var database: ParintintinDatabase
    private lateinit var spyDao: SpyDictionaryDao
    private lateinit var moshi: Moshi
    private lateinit var importer: AssetDictionarySeedImporter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ParintintinDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        spyDao = SpyDictionaryDao(database.dictionaryDao())
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        importer = AssetDictionarySeedImporter(
            context = context,
            moshi = moshi,
            database = database,
            dictionaryDao = spyDao,
            dispatchers = IoDispatcherProvider
        )
    }

    @After
    fun tearDown() {
        if (database.isOpen) {
            database.close()
        }
    }

    @Test
    fun `1 - importacao bem sucedida com banco vazio insere verbetes e retorna contagem`() = runBlocking {
        val json = """
            [
              ${syntheticJsonEntry(id = "seed_test_1", word = "palavra_sintetica_1", translation = "traducao_sintetica_1")},
              ${syntheticJsonEntry(id = "seed_test_2", word = "palavra_sintetica_2", translation = "traducao_sintetica_2")}
            ]
        """.trimIndent()

        val result = importer.importIfEmptyFromStream { json.toUtf8Stream() }

        assertEquals(AppResult.Success(2), result)
        assertEquals(1, spyDao.upsertCallCount)
        assertEquals(2, database.dictionaryDao().countOnce())
    }

    @Test
    fun `2 - seed vazio retorna sucesso zero sem erro e sem escrita`() = runBlocking {
        val result = importer.importIfEmptyFromStream { "[]".toUtf8Stream() }

        assertEquals(AppResult.Success(0), result)
        assertEquals(0, spyDao.upsertCallCount)
        assertEquals(0, database.dictionaryDao().countOnce())
    }

    @Test
    fun `3 - leitura do asset real dictionary_seed json retorna sucesso zero sem erro`() = runBlocking {
        val result = importer.importIfEmpty()

        assertEquals(AppResult.Success(0), result)
        assertEquals(0, spyDao.upsertCallCount)
        assertEquals(0, database.dictionaryDao().countOnce())
    }

    @Test
    fun `4 - preservacao de todos os campos e isFavorite false nos verbetes importados`() = runBlocking {
        val json = """
            [
              {
                "id": "seed_test_fields_1",
                "parintintin_word": "termo_sintetico_alpha",
                "phonetic_ipa": "/sintetico_alpha/",
                "portuguese_translation": "significado_sintetico_alpha",
                "grammatical_class": "VERB",
                "semantic_category": "CULTURE_SPIRIT",
                "example_parintintin": "frase_sintetica_alpha",
                "example_portuguese": "traducao_frase_sintetica_alpha",
                "cultural_note": "nota_sintetica_alpha",
                "audio_url": "https://example.invalid/audio/alpha.mp3",
                "updated_at_epoch_millis": 1700000000123
              },
              {
                "id": "seed_test_fields_2",
                "parintintin_word": "termo_sintetico_beta",
                "phonetic_ipa": "/sintetico_beta/",
                "portuguese_translation": "significado_sintetico_beta",
                "grammatical_class": "TOPONYM",
                "semantic_category": "EVERYDAY",
                "example_parintintin": "",
                "example_portuguese": "",
                "cultural_note": "",
                "updated_at_epoch_millis": 0
              }
            ]
        """.trimIndent()

        val result = importer.importIfEmptyFromStream { json.toUtf8Stream() }

        assertEquals(AppResult.Success(2), result)
        val entriesById = database.dictionaryDao()
            .observeEntries(query = "", category = null, onlyFavorites = false)
            .first()
            .associateBy { it.id }

        val first = entriesById.getValue("seed_test_fields_1")
        assertEquals("termo_sintetico_alpha", first.parintintinWord)
        assertEquals("/sintetico_alpha/", first.phoneticIpa)
        assertEquals("significado_sintetico_alpha", first.portugueseTranslation)
        assertEquals("VERB", first.grammaticalClass)
        assertEquals("CULTURE_SPIRIT", first.semanticCategory)
        assertEquals("frase_sintetica_alpha", first.exampleParintintin)
        assertEquals("traducao_frase_sintetica_alpha", first.examplePortuguese)
        assertEquals("nota_sintetica_alpha", first.culturalNote)
        assertEquals("https://example.invalid/audio/alpha.mp3", first.audioUrl)
        assertEquals(1_700_000_000_123L, first.updatedAtEpochMillis)
        assertFalse(first.isFavorite)

        val second = entriesById.getValue("seed_test_fields_2")
        assertNull(second.audioUrl)
        assertEquals("TOPONYM", second.grammaticalClass)
        assertEquals("EVERYDAY", second.semanticCategory)
        assertEquals(0L, second.updatedAtEpochMillis)
        assertFalse(second.isFavorite)
    }

    @Test
    fun `5 - idempotencia com banco ja populado retorna zero sem sobrescrever nem duplicar`() = runBlocking {
        database.dictionaryDao().upsertAll(
            listOf(
                syntheticEntity(
                    id = "existing_1",
                    word = "registro_existente_1",
                    translation = "traducao_existente_1",
                    isFavorite = false
                )
            )
        )
        spyDao.upsertCallCount = 0

        val json = """
            [
              ${syntheticJsonEntry(id = "existing_1", word = "registro_sobrescrito", translation = "outra_traducao")},
              ${syntheticJsonEntry(id = "seed_test_new", word = "novo_registro_sintetico", translation = "nova_traducao")}
            ]
        """.trimIndent()

        val result = importer.importIfEmptyFromStream { json.toUtf8Stream() }

        assertEquals(AppResult.Success(0), result)
        assertEquals(0, spyDao.upsertCallCount)
        assertEquals(1, database.dictionaryDao().countOnce())
        val persisted = database.dictionaryDao().observeEntryById("existing_1").first()
        assertEquals("registro_existente_1", persisted?.parintintinWord)
    }

    @Test
    fun `6 - json malformado retorna AppResult Error sem inserir registros`() = runBlocking {
        val malformedPayloads = listOf(
            "{ invalid json",
            """{"id": "objeto_na_raiz"}""",
            "",
            """[{"id":"a"}] trailing"""
        )

        malformedPayloads.forEach { payload ->
            val result = importer.importIfEmptyFromStream { payload.toUtf8Stream() }
            assertTrue("Era esperado AppResult.Error para payload '$payload'", result is AppResult.Error)
            assertEquals(0, database.dictionaryDao().countOnce())
        }
        assertEquals(0, spyDao.upsertCallCount)
    }

    @Test
    fun `7 - campos obrigatorios ausentes ou em branco retornam AppResult Error sem persistir`() = runBlocking {
        val missingRequiredField = """
            [
              {
                "id": "seed_missing_word",
                "phonetic_ipa": "/x/",
                "portuguese_translation": "traducao_sintetica",
                "grammatical_class": "NOUN",
                "semantic_category": "NATURE",
                "example_parintintin": "",
                "example_portuguese": "",
                "cultural_note": "",
                "updated_at_epoch_millis": 100
              }
            ]
        """.trimIndent()

        val blankId = """
            [
              ${syntheticJsonEntry(id = "   ", word = "palavra_sintetica", translation = "traducao_sintetica")}
            ]
        """.trimIndent()

        val blankWord = """
            [
              ${syntheticJsonEntry(id = "seed_blank_word", word = "  ", translation = "traducao_sintetica")}
            ]
        """.trimIndent()

        val blankTranslation = """
            [
              ${syntheticJsonEntry(id = "seed_blank_trans", word = "palavra_sintetica", translation = "")}
            ]
        """.trimIndent()

        listOf(missingRequiredField, blankId, blankWord, blankTranslation).forEach { json ->
            val result = importer.importIfEmptyFromStream { json.toUtf8Stream() }
            assertTrue(result is AppResult.Error)
            assertEquals(0, database.dictionaryDao().countOnce())
        }
        assertEquals(0, spyDao.upsertCallCount)
    }

    @Test
    fun `8 - grammatical_class invalido retorna AppResult Error sem inserir`() = runBlocking {
        val json = """
            [
              ${syntheticJsonEntry(id = "seed_bad_class", word = "palavra_sintetica", translation = "traducao_sintetica", grammaticalClass = "INVALID_CLASS")}
            ]
        """.trimIndent()

        val result = importer.importIfEmptyFromStream { json.toUtf8Stream() }

        assertTrue(result is AppResult.Error)
        assertEquals(0, spyDao.upsertCallCount)
        assertEquals(0, database.dictionaryDao().countOnce())
    }

    @Test
    fun `9 - semantic_category invalido retorna AppResult Error sem inserir`() = runBlocking {
        val json = """
            [
              ${syntheticJsonEntry(id = "seed_bad_cat", word = "palavra_sintetica", translation = "traducao_sintetica", semanticCategory = "UNKNOWN_CATEGORY")}
            ]
        """.trimIndent()

        val result = importer.importIfEmptyFromStream { json.toUtf8Stream() }

        assertTrue(result is AppResult.Error)
        assertEquals(0, spyDao.upsertCallCount)
        assertEquals(0, database.dictionaryDao().countOnce())
    }

    @Test
    fun `10 - updated_at_epoch_millis negativo retorna AppResult Error sem inserir`() = runBlocking {
        val json = """
            [
              ${syntheticJsonEntry(id = "seed_neg_ts", word = "palavra_sintetica", translation = "traducao_sintetica", updatedAt = -1L)}
            ]
        """.trimIndent()

        val result = importer.importIfEmptyFromStream { json.toUtf8Stream() }

        assertTrue(result is AppResult.Error)
        assertEquals(0, spyDao.upsertCallCount)
        assertEquals(0, database.dictionaryDao().countOnce())
    }

    @Test
    fun `11 - ids duplicados no json retornam AppResult Error e garantem zero persistencia parcial`() = runBlocking {
        val json = """
            [
              ${syntheticJsonEntry(id = "seed_dup", word = "palavra_sintetica_1", translation = "traducao_sintetica_1")},
              ${syntheticJsonEntry(id = "seed_dup", word = "palavra_sintetica_2", translation = "traducao_sintetica_2")}
            ]
        """.trimIndent()

        val result = importer.importIfEmptyFromStream { json.toUtf8Stream() }

        assertTrue(result is AppResult.Error)
        assertEquals(0, spyDao.upsertCallCount)
        assertEquals(0, database.dictionaryDao().countOnce())
    }

    @Test
    fun `12 - elemento nulo no array json retorna AppResult Error e garante zero persistencia parcial`() = runBlocking {
        val json = """
            [
              ${syntheticJsonEntry(id = "seed_valid_first", word = "palavra_sintetica_1", translation = "traducao_sintetica_1")},
              null
            ]
        """.trimIndent()

        val result = importer.importIfEmptyFromStream { json.toUtf8Stream() }

        assertTrue(result is AppResult.Error)
        assertEquals(0, spyDao.upsertCallCount)
        assertEquals(0, database.dictionaryDao().countOnce())
    }

    @Test
    fun `13 - arquivo ausente ou erro de leitura retorna AppResult Error sem corromper o banco`() = runBlocking {
        val missingResult = importer.importIfEmptyFromStream {
            throw FileNotFoundException("dictionary_seed.json não encontrado")
        }
        val ioResult = importer.importIfEmptyFromStream {
            throw IOException("Falha de E/S ao ler asset")
        }

        assertTrue(missingResult is AppResult.Error)
        assertTrue((missingResult as AppResult.Error).cause is FileNotFoundException)
        assertTrue(ioResult is AppResult.Error)
        assertTrue((ioResult as AppResult.Error).cause is IOException)
        assertEquals(0, database.dictionaryDao().countOnce())
    }

    @Test
    fun `14 - preserva favoritos existentes e garante importacao concorrente segura e cancelamento`() = runBlocking {
        // Parte A: preservação de favoritos existentes quando banco já está populado
        database.dictionaryDao().upsertAll(
            listOf(
                syntheticEntity(
                    id = "fav_existing_1",
                    word = "palavra_favorita_existente",
                    translation = "traducao_favorita_existente",
                    isFavorite = true
                )
            )
        )
        spyDao.upsertCallCount = 0

        val skipJson = """
            [
              ${syntheticJsonEntry(id = "fav_existing_1", word = "palavra_alterada", translation = "traducao_alterada")}
            ]
        """.trimIndent()
        val skipResult = importer.importIfEmptyFromStream { skipJson.toUtf8Stream() }
        assertEquals(AppResult.Success(0), skipResult)
        val preserved = database.dictionaryDao().observeEntryById("fav_existing_1").first()
        assertTrue(preserved?.isFavorite == true)
        assertEquals("palavra_favorita_existente", preserved?.parintintinWord)

        // Parte B: importação concorrente segura com banco vazio
        database.clearAllTables()
        spyDao.upsertCallCount = 0
        val streamOpenCount = AtomicInteger(0)
        val firstStreamEntered = CompletableDeferred<Unit>()
        val releaseFirstStream = CompletableDeferred<Unit>()
        val concurrentJson = """
            [
              ${syntheticJsonEntry(id = "concurrent_1", word = "palavra_concorrente_1", translation = "traducao_concorrente_1")},
              ${syntheticJsonEntry(id = "concurrent_2", word = "palavra_concorrente_2", translation = "traducao_concorrente_2")}
            ]
        """.trimIndent()

        val results = withTimeout(5_000L) {
            val first = async(Dispatchers.IO) {
                importer.importIfEmptyFromStream {
                    if (streamOpenCount.incrementAndGet() == 1) {
                        firstStreamEntered.complete(Unit)
                        while (!releaseFirstStream.isCompleted) {
                            Thread.sleep(10)
                        }
                    }
                    concurrentJson.toUtf8Stream()
                }
            }
            firstStreamEntered.await()
            val second = async(Dispatchers.IO) {
                importer.importIfEmptyFromStream {
                    streamOpenCount.incrementAndGet()
                    concurrentJson.toUtf8Stream()
                }
            }
            val third = async(Dispatchers.IO) {
                importer.importIfEmptyFromStream {
                    streamOpenCount.incrementAndGet()
                    concurrentJson.toUtf8Stream()
                }
            }
            releaseFirstStream.complete(Unit)
            awaitAll(first, second, third)
        }

        assertEquals(1, results.count { it == AppResult.Success(2) })
        assertEquals(2, results.count { it == AppResult.Success(0) })
        assertEquals(1, streamOpenCount.get())
        assertEquals(1, spyDao.upsertCallCount)
        assertEquals(2, database.dictionaryDao().countOnce())

        // Parte C: CancellationException deve ser relançada e não convertida em AppResult.Error
        database.clearAllTables()
        try {
            importer.importIfEmptyFromStream {
                throw CancellationException("Cancelado durante leitura")
            }
            fail("Era esperado relançar CancellationException")
        } catch (_: CancellationException) {
            assertEquals(0, database.dictionaryDao().countOnce())
        }
    }

    private fun String.toUtf8Stream(): InputStream =
        ByteArrayInputStream(toByteArray(StandardCharsets.UTF_8))

    private fun syntheticJsonEntry(
        id: String,
        word: String,
        translation: String,
        grammaticalClass: String = "NOUN",
        semanticCategory: String = "NATURE",
        updatedAt: Long = 1_700_000_000_000L
    ): String = """
        {
          "id": "$id",
          "parintintin_word": "$word",
          "phonetic_ipa": "/$id/",
          "portuguese_translation": "$translation",
          "grammatical_class": "$grammaticalClass",
          "semantic_category": "$semanticCategory",
          "example_parintintin": "exemplo_$id",
          "example_portuguese": "exemplo_pt_$id",
          "cultural_note": "nota_$id",
          "audio_url": null,
          "updated_at_epoch_millis": $updatedAt
        }
    """.trimIndent()

    private fun syntheticEntity(
        id: String,
        word: String,
        translation: String,
        isFavorite: Boolean
    ): DictionaryEntryEntity = DictionaryEntryEntity(
        id = id,
        parintintinWord = word,
        phoneticIpa = "/$id/",
        portugueseTranslation = translation,
        grammaticalClass = "NOUN",
        semanticCategory = "NATURE",
        exampleParintintin = "exemplo_$id",
        examplePortuguese = "exemplo_pt_$id",
        culturalNote = "nota_$id",
        audioUrl = null,
        isFavorite = isFavorite,
        updatedAtEpochMillis = 1_000L
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

    private object IoDispatcherProvider : DispatcherProvider {
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val default: CoroutineDispatcher = Dispatchers.Default
        override val main: CoroutineDispatcher = Dispatchers.Main
    }
}
