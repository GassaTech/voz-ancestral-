package com.example.data.remote.api

import com.example.core.common.AppResult
import com.example.data.remote.datasource.RemoteArchiveDataSource
import com.example.data.remote.dto.ArchiveManifestDto
import com.example.data.remote.dto.CulturalArticleDto
import com.example.data.remote.dto.DictionaryEntryDto
import com.example.di.NetworkModule
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import java.io.EOFException
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ParintintinApiContractTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var moshi: Moshi
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var apiService: ParintintinApiService
    private lateinit var dataSource: RemoteArchiveDataSource

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
            .create(ParintintinApiService::class.java)

        dataSource = RemoteArchiveDataSource(apiService)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `adaptadores Moshi gerados pelo KSP estao ativos para todos os DTOs`() {
        val codegenOnlyMoshi = Moshi.Builder().build()

        val manifestAdapter = codegenOnlyMoshi.adapter(ArchiveManifestDto::class.java)
        val dictionaryAdapter = codegenOnlyMoshi.adapter(DictionaryEntryDto::class.java)
        val articleAdapter = codegenOnlyMoshi.adapter(CulturalArticleDto::class.java)

        assertTrue(manifestAdapter.toString().contains("GeneratedJsonAdapter(ArchiveManifestDto)"))
        assertTrue(dictionaryAdapter.toString().contains("GeneratedJsonAdapter(DictionaryEntryDto)"))
        assertTrue(articleAdapter.toString().contains("GeneratedJsonAdapter(CulturalArticleDto)"))
    }

    // --- Manifesto: GET /v1/manifest.json ---

    @Test
    fun `manifesto - resposta HTTP 200 com JSON valido desserializa os tres campos e usa caminho correto`() = runTest {
        mockWebServer.enqueue(jsonResponse(VALID_MANIFEST_JSON))

        val manifest = apiService.getArchiveManifest()
        val request = mockWebServer.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/v1/manifest.json", request.path)
        assertEquals("1.0.0", manifest.version)
        assertEquals(150, manifest.entriesCount)
        assertEquals(25, manifest.articlesCount)
    }

    @Test
    fun `manifesto - rejeita campos obrigatorios ausentes`() = runTest {
        val missingVersionJson = """
            {
              "entries_count": 150,
              "articles_count": 25
            }
        """.trimIndent()
        val missingEntriesCountJson = """
            {
              "version": "1.0.0",
              "articles_count": 25
            }
        """.trimIndent()
        val missingArticlesCountJson = """
            {
              "version": "1.0.0",
              "entries_count": 150
            }
        """.trimIndent()

        listOf(missingVersionJson, missingEntriesCountJson, missingArticlesCountJson).forEach { json ->
            mockWebServer.enqueue(jsonResponse(json))
            assertThrows<JsonDataException> {
                apiService.getArchiveManifest()
            }
        }
    }

    @Test
    fun `manifesto - rejeita valores e tipos incompativeis`() = runTest {
        val invalidEntriesCountTypeJson = """
            {
              "version": "1.0.0",
              "entries_count": "cento e cinquenta",
              "articles_count": 25
            }
        """.trimIndent()
        val fractionalArticlesCountJson = """
            {
              "version": "1.0.0",
              "entries_count": 150,
              "articles_count": 25.5
            }
        """.trimIndent()
        val invalidVersionTypeJson = """
            {
              "version": {"major": 1},
              "entries_count": 150,
              "articles_count": 25
            }
        """.trimIndent()

        listOf(
            invalidEntriesCountTypeJson,
            fractionalArticlesCountJson,
            invalidVersionTypeJson
        ).forEach { json ->
            mockWebServer.enqueue(jsonResponse(json))
            assertThrows<JsonDataException> {
                apiService.getArchiveManifest()
            }
        }
    }

    // --- Dicionário: GET /v1/dictionary.json ---

    @Test
    fun `dicionario - resposta HTTP 200 com JSON valido desserializa lista e usa caminho correto`() = runTest {
        mockWebServer.enqueue(jsonResponse(VALID_DICTIONARY_JSON))

        val entries = apiService.getDictionaryEntries()
        val request = mockWebServer.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/v1/dictionary.json", request.path)
        assertEquals(1, entries.size)
        val entry = entries.single()
        assertEquals("entry_001", entry.id)
        assertEquals("EXEMPLO", entry.parintintinWord)
        assertEquals("exemplo", entry.phoneticIpa)
        assertEquals("Tradução ilustrativa", entry.portugueseTranslation)
        assertEquals("NOUN", entry.grammaticalClass)
        assertEquals("NATURE", entry.semanticCategory)
        assertEquals("", entry.exampleParintintin)
        assertEquals("", entry.examplePortuguese)
        assertEquals("", entry.culturalNote)
        assertNull(entry.audioUrl)
        assertEquals(1794787200000L, entry.updatedAtEpochMillis)
    }

    @Test
    fun `dicionario - aceita audio_url null e audio_url preenchido`() = runTest {
        val json = """
            [
              {
                "id": "entry_001",
                "parintintin_word": "EXEMPLO",
                "phonetic_ipa": "exemplo",
                "portuguese_translation": "Tradução ilustrativa",
                "grammatical_class": "NOUN",
                "semantic_category": "NATURE",
                "example_parintintin": "",
                "example_portuguese": "",
                "cultural_note": "",
                "audio_url": null,
                "updated_at_epoch_millis": 1794787200000
              },
              {
                "id": "entry_002",
                "parintintin_word": "EXEMPLO_2",
                "phonetic_ipa": "exemplo2",
                "portuguese_translation": "Segunda tradução",
                "grammatical_class": "VERB",
                "semantic_category": "EVERYDAY",
                "example_parintintin": "",
                "example_portuguese": "",
                "cultural_note": "",
                "audio_url": "https://archive.parintintin.org/v1/audio/entry_002.mp3",
                "updated_at_epoch_millis": 1794787200001
              }
            ]
        """.trimIndent()
        mockWebServer.enqueue(jsonResponse(json))

        val entries = apiService.getDictionaryEntries()

        assertEquals(2, entries.size)
        assertNull(entries[0].audioUrl)
        assertEquals("https://archive.parintintin.org/v1/audio/entry_002.mp3", entries[1].audioUrl)
    }

    @Test
    fun `dicionario - aceita audio_url ausente usando valor padrao null`() = runTest {
        val jsonWithoutAudioUrl = """
            [
              {
                "id": "entry_001",
                "parintintin_word": "EXEMPLO",
                "phonetic_ipa": "exemplo",
                "portuguese_translation": "Tradução ilustrativa",
                "grammatical_class": "NOUN",
                "semantic_category": "NATURE",
                "example_parintintin": "",
                "example_portuguese": "",
                "cultural_note": "",
                "updated_at_epoch_millis": 1794787200000
              }
            ]
        """.trimIndent()
        mockWebServer.enqueue(jsonResponse(jsonWithoutAudioUrl))

        val entries = apiService.getDictionaryEntries()

        assertEquals(1, entries.size)
        assertNull(entries.single().audioUrl)
    }

    @Test
    fun `dicionario - aceita campos adicionais desconhecidos`() = runTest {
        val jsonWithExtraFields = """
            [
              {
                "id": "entry_001",
                "parintintin_word": "EXEMPLO",
                "phonetic_ipa": "exemplo",
                "portuguese_translation": "Tradução ilustrativa",
                "grammatical_class": "NOUN",
                "semantic_category": "NATURE",
                "example_parintintin": "",
                "example_portuguese": "",
                "cultural_note": "",
                "audio_url": null,
                "updated_at_epoch_millis": 1794787200000,
                "dialect_variant": "Upper Madeira",
                "contributors": ["Pesquisador A"],
                "schema_revision": 2
              }
            ]
        """.trimIndent()
        mockWebServer.enqueue(jsonResponse(jsonWithExtraFields))

        val entries = apiService.getDictionaryEntries()

        assertEquals(1, entries.size)
        assertEquals("entry_001", entries.single().id)
        assertEquals("EXEMPLO", entries.single().parintintinWord)
    }

    @Test
    fun `dicionario - rejeita campos obrigatorios ausentes ou nulos`() = runTest {
        val missingWordJson = """
            [
              {
                "id": "entry_001",
                "phonetic_ipa": "exemplo",
                "portuguese_translation": "Tradução ilustrativa",
                "grammatical_class": "NOUN",
                "semantic_category": "NATURE",
                "example_parintintin": "",
                "example_portuguese": "",
                "cultural_note": "",
                "audio_url": null,
                "updated_at_epoch_millis": 1794787200000
              }
            ]
        """.trimIndent()
        val nullCulturalNoteJson = """
            [
              {
                "id": "entry_001",
                "parintintin_word": "EXEMPLO",
                "phonetic_ipa": "exemplo",
                "portuguese_translation": "Tradução ilustrativa",
                "grammatical_class": "NOUN",
                "semantic_category": "NATURE",
                "example_parintintin": "",
                "example_portuguese": "",
                "cultural_note": null,
                "audio_url": null,
                "updated_at_epoch_millis": 1794787200000
              }
            ]
        """.trimIndent()
        val missingTimestampJson = """
            [
              {
                "id": "entry_001",
                "parintintin_word": "EXEMPLO",
                "phonetic_ipa": "exemplo",
                "portuguese_translation": "Tradução ilustrativa",
                "grammatical_class": "NOUN",
                "semantic_category": "NATURE",
                "example_parintintin": "",
                "example_portuguese": "",
                "cultural_note": "",
                "audio_url": null
              }
            ]
        """.trimIndent()

        listOf(missingWordJson, nullCulturalNoteJson, missingTimestampJson).forEach { json ->
            mockWebServer.enqueue(jsonResponse(json))
            assertThrows<JsonDataException> {
                apiService.getDictionaryEntries()
            }
        }
    }

    @Test
    fun `dicionario - rejeita tipos incompativeis`() = runTest {
        val isoStringTimestampJson = """
            [
              {
                "id": "entry_001",
                "parintintin_word": "EXEMPLO",
                "phonetic_ipa": "exemplo",
                "portuguese_translation": "Tradução ilustrativa",
                "grammatical_class": "NOUN",
                "semantic_category": "NATURE",
                "example_parintintin": "",
                "example_portuguese": "",
                "cultural_note": "",
                "audio_url": null,
                "updated_at_epoch_millis": "2026-11-16T00:00:00Z"
              }
            ]
        """.trimIndent()
        val objectInsteadOfStringJson = """
            [
              {
                "id": "entry_001",
                "parintintin_word": {"text": "EXEMPLO"},
                "phonetic_ipa": "exemplo",
                "portuguese_translation": "Tradução ilustrativa",
                "grammatical_class": "NOUN",
                "semantic_category": "NATURE",
                "example_parintintin": "",
                "example_portuguese": "",
                "cultural_note": "",
                "audio_url": null,
                "updated_at_epoch_millis": 1794787200000
              }
            ]
        """.trimIndent()

        listOf(isoStringTimestampJson, objectInsteadOfStringJson).forEach { json ->
            mockWebServer.enqueue(jsonResponse(json))
            assertThrows<JsonDataException> {
                apiService.getDictionaryEntries()
            }
        }
    }

    // --- Enciclopédia: GET /v1/articles.json ---

    @Test
    fun `enciclopedia - resposta HTTP 200 com JSON valido desserializa artigos e paragrafos e usa caminho correto`() = runTest {
        mockWebServer.enqueue(jsonResponse(VALID_ARTICLES_JSON))

        val articles = apiService.getCulturalArticles()
        val request = mockWebServer.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/v1/articles.json", request.path)
        assertEquals(1, articles.size)
        val article = articles.single()
        assertEquals("article_001", article.id)
        assertEquals("Título ilustrativo", article.title)
        assertEquals("Subtítulo ilustrativo", article.subtitle)
        assertEquals("Resumo ilustrativo", article.summary)
        assertEquals(
            listOf(
                "Primeiro parágrafo ilustrativo.",
                "Segundo parágrafo ilustrativo."
            ),
            article.bodyParagraphs
        )
        assertEquals("HISTORY", article.topic)
        assertEquals(5, article.readingTimeMinutes)
        assertEquals("Referência ilustrativa", article.sourceReference)
        assertEquals(1794787200000L, article.updatedAtEpochMillis)
    }

    @Test
    fun `enciclopedia - aceita campos adicionais desconhecidos`() = runTest {
        val jsonWithExtraFields = """
            [
              {
                "id": "article_001",
                "title": "Título ilustrativo",
                "subtitle": "Subtítulo ilustrativo",
                "summary": "Resumo ilustrativo",
                "body_paragraphs": [
                  "Primeiro parágrafo ilustrativo.",
                  "Segundo parágrafo ilustrativo."
                ],
                "topic": "HISTORY",
                "reading_time_minutes": 5,
                "source_reference": "Referência ilustrativa",
                "updated_at_epoch_millis": 1794787200000,
                "cover_image_url": "https://archive.parintintin.org/v1/images/art_001.jpg",
                "reviewed_by_elders": true
              }
            ]
        """.trimIndent()
        mockWebServer.enqueue(jsonResponse(jsonWithExtraFields))

        val articles = apiService.getCulturalArticles()

        assertEquals(1, articles.size)
        assertEquals("article_001", articles.single().id)
        assertEquals(2, articles.single().bodyParagraphs.size)
    }

    @Test
    fun `enciclopedia - rejeita campos obrigatorios ausentes ou nulos`() = runTest {
        val missingSourceReferenceJson = """
            [
              {
                "id": "article_001",
                "title": "Título ilustrativo",
                "subtitle": "Subtítulo ilustrativo",
                "summary": "Resumo ilustrativo",
                "body_paragraphs": ["Primeiro parágrafo ilustrativo."],
                "topic": "HISTORY",
                "reading_time_minutes": 5,
                "updated_at_epoch_millis": 1794787200000
              }
            ]
        """.trimIndent()
        val nullParagraphsListJson = """
            [
              {
                "id": "article_001",
                "title": "Título ilustrativo",
                "subtitle": "Subtítulo ilustrativo",
                "summary": "Resumo ilustrativo",
                "body_paragraphs": null,
                "topic": "HISTORY",
                "reading_time_minutes": 5,
                "source_reference": "Referência ilustrativa",
                "updated_at_epoch_millis": 1794787200000
              }
            ]
        """.trimIndent()
        val nullSummaryJson = """
            [
              {
                "id": "article_001",
                "title": "Título ilustrativo",
                "subtitle": "Subtítulo ilustrativo",
                "summary": null,
                "body_paragraphs": ["Primeiro parágrafo ilustrativo."],
                "topic": "HISTORY",
                "reading_time_minutes": 5,
                "source_reference": "Referência ilustrativa",
                "updated_at_epoch_millis": 1794787200000
              }
            ]
        """.trimIndent()
        val missingTitleJson = """
            [
              {
                "id": "article_001",
                "subtitle": "Subtítulo ilustrativo",
                "summary": "Resumo ilustrativo",
                "body_paragraphs": ["Primeiro parágrafo ilustrativo."],
                "topic": "HISTORY",
                "reading_time_minutes": 5,
                "source_reference": "Referência ilustrativa",
                "updated_at_epoch_millis": 1794787200000
              }
            ]
        """.trimIndent()

        listOf(
            missingSourceReferenceJson,
            nullParagraphsListJson,
            nullSummaryJson,
            missingTitleJson
        ).forEach { json ->
            mockWebServer.enqueue(jsonResponse(json))
            assertThrows<JsonDataException> {
                apiService.getCulturalArticles()
            }
        }
    }

    @Test
    fun `enciclopedia - rejeita tipos incompativeis`() = runTest {
        val stringReadingTimeJson = """
            [
              {
                "id": "article_001",
                "title": "Título ilustrativo",
                "subtitle": "Subtítulo ilustrativo",
                "summary": "Resumo ilustrativo",
                "body_paragraphs": ["Primeiro parágrafo ilustrativo."],
                "topic": "HISTORY",
                "reading_time_minutes": "cinco",
                "source_reference": "Referência ilustrativa",
                "updated_at_epoch_millis": 1794787200000
              }
            ]
        """.trimIndent()
        val stringInsteadOfArrayParagraphsJson = """
            [
              {
                "id": "article_001",
                "title": "Título ilustrativo",
                "subtitle": "Subtítulo ilustrativo",
                "summary": "Resumo ilustrativo",
                "body_paragraphs": "Texto único em string",
                "topic": "HISTORY",
                "reading_time_minutes": 5,
                "source_reference": "Referência ilustrativa",
                "updated_at_epoch_millis": 1794787200000
              }
            ]
        """.trimIndent()

        listOf(stringReadingTimeJson, stringInsteadOfArrayParagraphsJson).forEach { json ->
            mockWebServer.enqueue(jsonResponse(json))
            assertThrows<JsonDataException> {
                apiService.getCulturalArticles()
            }
        }
    }

    // --- HTTP e Retrofit: 404, 500 e JSON malformado ---

    @Test
    fun `http 404 gera HttpException no Retrofit e AppResult Error no RemoteArchiveDataSource`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"not_found"}"""))
        val manifestException = assertThrows<HttpException> {
            apiService.getArchiveManifest()
        }
        assertEquals(404, manifestException.code())
        assertEquals("/v1/manifest.json", mockWebServer.takeRequest().path)

        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"not_found"}"""))
        val dictionaryResult = dataSource.fetchDictionaryEntries()
        assertTrue(dictionaryResult is AppResult.Error)
        assertTrue((dictionaryResult as AppResult.Error).cause is HttpException)
        assertEquals(404, (dictionaryResult.cause as HttpException).code())
        assertEquals("/v1/dictionary.json", mockWebServer.takeRequest().path)

        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"not_found"}"""))
        val articlesResult = dataSource.fetchCulturalArticles()
        assertTrue(articlesResult is AppResult.Error)
        assertTrue((articlesResult as AppResult.Error).cause is HttpException)
        assertEquals(404, (articlesResult.cause as HttpException).code())
        assertEquals("/v1/articles.json", mockWebServer.takeRequest().path)
    }

    @Test
    fun `http 500 gera HttpException no Retrofit e AppResult Error no RemoteArchiveDataSource`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"internal_error"}"""))
        val manifestException = assertThrows<HttpException> {
            apiService.getArchiveManifest()
        }
        assertEquals(500, manifestException.code())

        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"internal_error"}"""))
        val dictionaryResult = dataSource.fetchDictionaryEntries()
        assertTrue(dictionaryResult is AppResult.Error)
        assertTrue((dictionaryResult as AppResult.Error).cause is HttpException)
        assertEquals(500, (dictionaryResult.cause as HttpException).code())

        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"internal_error"}"""))
        val articlesResult = dataSource.fetchCulturalArticles()
        assertTrue(articlesResult is AppResult.Error)
        assertTrue((articlesResult as AppResult.Error).cause is HttpException)
        assertEquals(500, (articlesResult.cause as HttpException).code())
    }

    @Test
    fun `json malformado gera erro de leitura no Retrofit e AppResult Error no RemoteArchiveDataSource`() = runTest {
        mockWebServer.enqueue(jsonResponse("""{ "version": "1.0.0", invalid }"""))
        assertThrows<JsonEncodingException> {
            apiService.getArchiveManifest()
        }

        mockWebServer.enqueue(jsonResponse("""{ "version": "1.0.0", """))
        assertThrows<EOFException> {
            apiService.getArchiveManifest()
        }

        mockWebServer.enqueue(jsonResponse("[{\"id\":\"entry_001\""))
        val dictionaryResult = dataSource.fetchDictionaryEntries()
        assertTrue(dictionaryResult is AppResult.Error)
        assertTrue((dictionaryResult as AppResult.Error).cause is EOFException)

        mockWebServer.enqueue(jsonResponse("<html><body>erro</body></html>"))
        val articlesResult = dataSource.fetchCulturalArticles()
        assertTrue(articlesResult is AppResult.Error)
        assertTrue((articlesResult as AppResult.Error).cause is JsonEncodingException)
    }

    private fun jsonResponse(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json; charset=utf-8")
        .setBody(body)

    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit): T {
        try {
            block()
        } catch (t: Throwable) {
            if (t is T) {
                return t
            }
            fail("Esperada exceção ${T::class.java.simpleName}, mas lançou ${t::class.java.simpleName}: ${t.message}")
        }
        fail("Esperada exceção ${T::class.java.simpleName}, mas nenhuma exceção foi lançada.")
        error("Inalcançável")
    }

    private companion object {
        val VALID_MANIFEST_JSON = """
            {
              "version": "1.0.0",
              "entries_count": 150,
              "articles_count": 25
            }
        """.trimIndent()

        val VALID_DICTIONARY_JSON = """
            [
              {
                "id": "entry_001",
                "parintintin_word": "EXEMPLO",
                "phonetic_ipa": "exemplo",
                "portuguese_translation": "Tradução ilustrativa",
                "grammatical_class": "NOUN",
                "semantic_category": "NATURE",
                "example_parintintin": "",
                "example_portuguese": "",
                "cultural_note": "",
                "audio_url": null,
                "updated_at_epoch_millis": 1794787200000
              }
            ]
        """.trimIndent()

        val VALID_ARTICLES_JSON = """
            [
              {
                "id": "article_001",
                "title": "Título ilustrativo",
                "subtitle": "Subtítulo ilustrativo",
                "summary": "Resumo ilustrativo",
                "body_paragraphs": [
                  "Primeiro parágrafo ilustrativo.",
                  "Segundo parágrafo ilustrativo."
                ],
                "topic": "HISTORY",
                "reading_time_minutes": 5,
                "source_reference": "Referência ilustrativa",
                "updated_at_epoch_millis": 1794787200000
              }
            ]
        """.trimIndent()
    }
}
