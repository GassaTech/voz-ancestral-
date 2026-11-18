package com.example.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.SeedMetadataEntity
import com.example.data.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ParintintinDatabaseMigrationTest {

    private lateinit var context: Context
    private val createdDatabaseNames = mutableListOf<String>()
    private val openedDatabases = mutableListOf<ParintintinDatabase>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        openedDatabases.forEach { db ->
            if (db.isOpen) {
                db.close()
            }
        }
        openedDatabases.clear()
        createdDatabaseNames.forEach { dbName ->
            context.deleteDatabase(dbName)
        }
        createdDatabaseNames.clear()
    }

    @Test
    fun `migracao 1 para 3 preserva dicionario favoritos artigos marcados e converte sync_metadata SYNCED`() = runTest {
        val dbName = newTestDbName("migration_synced")
        val v1SyncEpochMillis = 1_700_000_010_000L
        val v1RemoteVersion = "manifest-v1"

        createV1Database(dbName) { db ->
            insertSampleArchiveRows(db)
            db.execSQL(
                """
                INSERT INTO `sync_metadata` (
                    `id`,
                    `last_sync_epoch_millis`,
                    `last_status`,
                    `remote_version`
                ) VALUES (?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(
                    SyncMetadataEntity.SINGLETON_ID,
                    v1SyncEpochMillis,
                    SyncMetadataEntity.STATUS_SYNCED,
                    v1RemoteVersion
                )
            )
        }

        val databaseV3 = openMigratedDatabase(dbName)
        val rawDb = databaseV3.openHelper.writableDatabase

        assertEquals(3, readUserVersion(rawDb))
        assertSyncMetadataTableStructure(rawDb)
        assertSeedMetadataTableStructure(rawDb)
        assertFalse(tableExists(rawDb, "sync_metadata_new"))

        assertSampleArchiveRowsPreserved(databaseV3)

        val migratedMetadata = databaseV3.syncMetadataDao().getMetadataOnce()
        assertNotNull(migratedMetadata)
        assertEquals(SyncMetadataEntity.SINGLETON_ID, migratedMetadata?.id)
        assertEquals(v1SyncEpochMillis, migratedMetadata?.lastAttemptEpochMillis)
        assertEquals(SyncMetadataEntity.STATUS_SYNCED, migratedMetadata?.lastAttemptStatus)
        assertNull(migratedMetadata?.lastFailureReason)
        assertEquals(v1SyncEpochMillis, migratedMetadata?.lastSuccessfulSyncEpochMillis)
        assertNull(migratedMetadata?.lastSuccessfulEntriesCount)
        assertNull(migratedMetadata?.lastSuccessfulArticlesCount)
        assertEquals(v1RemoteVersion, migratedMetadata?.lastSuccessfulRemoteVersion)

        val seedMetadata = databaseV3.seedMetadataDao().getMetadataOnce()
        assertNotNull(seedMetadata)
        assertEquals(SeedMetadataEntity.SINGLETON_ID, seedMetadata?.id)
        assertEquals(SeedMetadataEntity.INITIAL_SEED_VERSION, seedMetadata?.appliedSeedVersion)
        assertNull(seedMetadata?.appliedSeedHash)
        assertNull(seedMetadata?.appliedSeedIdsJson)
        assertEquals(0L, seedMetadata?.updatedAtEpochMillis)
    }

    @Test
    fun `migracao 1 para 3 converte sync_metadata OFFLINE_FALLBACK mantendo campos de sucesso nulos`() = runTest {
        val dbName = newTestDbName("migration_offline_fallback")
        val v1AttemptEpochMillis = 1_700_000_025_000L

        createV1Database(dbName) { db ->
            insertSampleArchiveRows(db)
            db.execSQL(
                """
                INSERT INTO `sync_metadata` (
                    `id`,
                    `last_sync_epoch_millis`,
                    `last_status`,
                    `remote_version`
                ) VALUES (?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(
                    SyncMetadataEntity.SINGLETON_ID,
                    v1AttemptEpochMillis,
                    SyncMetadataEntity.STATUS_OFFLINE_FALLBACK,
                    "manifest-v1-stale"
                )
            )
        }

        val databaseV3 = openMigratedDatabase(dbName)
        val rawDb = databaseV3.openHelper.writableDatabase

        assertEquals(3, readUserVersion(rawDb))
        assertSyncMetadataTableStructure(rawDb)
        assertSeedMetadataTableStructure(rawDb)
        assertSampleArchiveRowsPreserved(databaseV3)

        val migratedMetadata = databaseV3.syncMetadataDao().getMetadataOnce()
        assertNotNull(migratedMetadata)
        assertEquals(SyncMetadataEntity.SINGLETON_ID, migratedMetadata?.id)
        assertEquals(v1AttemptEpochMillis, migratedMetadata?.lastAttemptEpochMillis)
        assertEquals(SyncMetadataEntity.STATUS_OFFLINE_FALLBACK, migratedMetadata?.lastAttemptStatus)
        assertNull(migratedMetadata?.lastFailureReason)
        assertNull(migratedMetadata?.lastSuccessfulSyncEpochMillis)
        assertNull(migratedMetadata?.lastSuccessfulEntriesCount)
        assertNull(migratedMetadata?.lastSuccessfulArticlesCount)
        assertNull(migratedMetadata?.lastSuccessfulRemoteVersion)
    }

    @Test
    fun `migracao 1 para 3 com sync_metadata vazia preserva dados existentes sem erro`() = runTest {
        val dbName = newTestDbName("migration_empty_metadata")

        createV1Database(dbName) { db ->
            insertSampleArchiveRows(db)
        }

        val databaseV3 = openMigratedDatabase(dbName)
        val rawDb = databaseV3.openHelper.writableDatabase

        assertEquals(3, readUserVersion(rawDb))
        assertSyncMetadataTableStructure(rawDb)
        assertSeedMetadataTableStructure(rawDb)
        assertSampleArchiveRowsPreserved(databaseV3)
        assertNull(databaseV3.syncMetadataDao().getMetadataOnce())
        assertNotNull(databaseV3.seedMetadataDao().getMetadataOnce())
    }

    @Test
    fun `migracao 2 para 3 cria seed_metadata registra apenas IDs DEMO e preserva registros existentes`() = runTest {
        val dbName = newTestDbName("migration_2_to_3_with_demo")
        val v2AttemptEpochMillis = 1_700_000_100_000L

        createV2Database(dbName) { db ->
            insertSampleArchiveRows(db)
            insertDictionaryEntry(
                db = db,
                id = "DEMO-002",
                parintintinWord = "Folhaverde",
                portugueseTranslation = "folha nova após a chuva",
                isFavorite = 1,
                updatedAtEpochMillis = 1_700_000_011_000L
            )
            insertDictionaryEntry(
                db = db,
                id = "DEMO-001",
                parintintinWord = "Riolume",
                portugueseTranslation = "brilho refletido na superfície de um rio",
                isFavorite = 0,
                updatedAtEpochMillis = 1_700_000_010_000L
            )
            insertDictionaryEntry(
                db = db,
                id = "demo-lowercase",
                parintintinWord = "registro_nao_seed",
                portugueseTranslation = "mantido sem entrar no seed",
                isFavorite = 0,
                updatedAtEpochMillis = 1_700_000_012_000L
            )
            db.execSQL(
                """
                INSERT INTO `sync_metadata` (
                    `id`,
                    `last_attempt_epoch_millis`,
                    `last_attempt_status`,
                    `last_failure_reason`,
                    `last_successful_sync_epoch_millis`,
                    `last_successful_entries_count`,
                    `last_successful_articles_count`,
                    `last_successful_remote_version`
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    SyncMetadataEntity.SINGLETON_ID,
                    v2AttemptEpochMillis,
                    SyncMetadataEntity.STATUS_SYNCED,
                    null,
                    v2AttemptEpochMillis,
                    5,
                    2,
                    "manifest-v2"
                )
            )
        }

        val databaseV3 = openMigratedDatabase(dbName)
        val rawDb = databaseV3.openHelper.writableDatabase

        assertEquals(3, readUserVersion(rawDb))
        assertSeedMetadataTableStructure(rawDb)
        assertSyncMetadataTableStructure(rawDb)

        val seedMetadata = databaseV3.seedMetadataDao().getMetadataOnce()
        assertNotNull(seedMetadata)
        assertEquals(SeedMetadataEntity.SINGLETON_ID, seedMetadata?.id)
        assertEquals(SeedMetadataEntity.INITIAL_SEED_VERSION, seedMetadata?.appliedSeedVersion)
        assertNull(seedMetadata?.appliedSeedHash)
        assertEquals("[\"DEMO-001\",\"DEMO-002\"]", seedMetadata?.appliedSeedIdsJson)
        assertEquals(0L, seedMetadata?.updatedAtEpochMillis)

        val dictionaryDao = databaseV3.dictionaryDao()
        val entries = dictionaryDao.observeEntries(
            query = "",
            category = null,
            onlyFavorites = false
        ).first()
        assertEquals(5, entries.size)
        assertEquals(5, dictionaryDao.countOnce())

        val demo1 = entries.first { it.id == "DEMO-001" }
        assertEquals("Riolume", demo1.parintintinWord)
        assertEquals("brilho refletido na superfície de um rio", demo1.portugueseTranslation)
        assertFalse(demo1.isFavorite)

        val demo2 = entries.first { it.id == "DEMO-002" }
        assertEquals("Folhaverde", demo2.parintintinWord)
        assertTrue(demo2.isFavorite)

        val nonDemo1 = entries.first { it.id == "entry_test_1" }
        assertEquals("termo_teste_alfa", nonDemo1.parintintinWord)
        assertTrue(nonDemo1.isFavorite)

        val nonDemoLower = entries.first { it.id == "demo-lowercase" }
        assertEquals("registro_nao_seed", nonDemoLower.parintintinWord)
        assertFalse(nonDemoLower.isFavorite)

        assertEquals(listOf("DEMO-002", "entry_test_1"), dictionaryDao.getFavoriteIds().sorted())
        assertEquals(2, dictionaryDao.observeFavoriteCount().first())

        val articles = databaseV3.encyclopediaDao().observeArticles(
            query = "",
            topic = null,
            onlyBookmarked = false
        ).first()
        assertEquals(2, articles.size)
        assertEquals(listOf("article_test_1"), databaseV3.encyclopediaDao().getBookmarkedIds())

        val syncMetadata = databaseV3.syncMetadataDao().getMetadataOnce()
        assertEquals(v2AttemptEpochMillis, syncMetadata?.lastAttemptEpochMillis)
        assertEquals("manifest-v2", syncMetadata?.lastSuccessfulRemoteVersion)
        assertEquals(5, syncMetadata?.lastSuccessfulEntriesCount)
        assertEquals(2, syncMetadata?.lastSuccessfulArticlesCount)
    }

    @Test
    fun `migracao 2 para 3 sem registros DEMO mantem applied_seed_ids_json nulo e preserva dados`() = runTest {
        val dbName = newTestDbName("migration_2_to_3_without_demo")

        createV2Database(dbName) { db ->
            insertSampleArchiveRows(db)
        }

        val databaseV3 = openMigratedDatabase(dbName)
        val rawDb = databaseV3.openHelper.writableDatabase

        assertEquals(3, readUserVersion(rawDb))
        assertSeedMetadataTableStructure(rawDb)
        assertSampleArchiveRowsPreserved(databaseV3)

        val seedMetadata = databaseV3.seedMetadataDao().getMetadataOnce()
        assertNotNull(seedMetadata)
        assertEquals(SeedMetadataEntity.SINGLETON_ID, seedMetadata?.id)
        assertEquals(SeedMetadataEntity.INITIAL_SEED_VERSION, seedMetadata?.appliedSeedVersion)
        assertNull(seedMetadata?.appliedSeedHash)
        assertNull(seedMetadata?.appliedSeedIdsJson)
        assertEquals(0L, seedMetadata?.updatedAtEpochMillis)
    }

    @Test
    fun `banco novo v3 inicializa seed_metadata singleton e permite upsert e leitura via dao`() = runTest {
        val dbName = newTestDbName("fresh_v3_seed_metadata")
        val database = Room.databaseBuilder(context, ParintintinDatabase::class.java, dbName)
            .addMigrations(*ParintintinDatabase.MIGRATIONS)
            .addCallback(ParintintinDatabase.SEED_INITIALIZATION_CALLBACK)
            .allowMainThreadQueries()
            .build()
        openedDatabases += database

        val rawDb = database.openHelper.writableDatabase
        assertEquals(3, readUserVersion(rawDb))
        assertSeedMetadataTableStructure(rawDb)

        val seedMetadataDao = database.seedMetadataDao()
        val initialMetadata = seedMetadataDao.getMetadataOnce()
        assertNotNull(initialMetadata)
        assertEquals(SeedMetadataEntity.SINGLETON_ID, initialMetadata?.id)
        assertEquals(SeedMetadataEntity.INITIAL_SEED_VERSION, initialMetadata?.appliedSeedVersion)
        assertNull(initialMetadata?.appliedSeedHash)
        assertNull(initialMetadata?.appliedSeedIdsJson)
        assertEquals(0L, initialMetadata?.updatedAtEpochMillis)
        assertEquals(initialMetadata, seedMetadataDao.observeMetadata().first())

        val updatedMetadata = SeedMetadataEntity(
            id = SeedMetadataEntity.SINGLETON_ID,
            appliedSeedVersion = 2,
            appliedSeedHash = "sha256-seed-v2",
            appliedSeedIdsJson = "[\"DEMO-001\",\"DEMO-002\"]",
            updatedAtEpochMillis = 1_700_000_500_000L
        )
        seedMetadataDao.upsert(updatedMetadata)

        assertEquals(updatedMetadata, seedMetadataDao.getMetadataOnce())
        assertEquals(updatedMetadata, seedMetadataDao.observeMetadata().first())
    }

    private fun newTestDbName(prefix: String): String {
        val name = "${prefix}_${System.nanoTime()}.db"
        createdDatabaseNames += name
        context.deleteDatabase(name)
        return name
    }

    private fun createV1Database(
        dbName: String,
        populate: (SupportSQLiteDatabase) -> Unit
    ) {
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dictionary_entries` (
                        `id` TEXT NOT NULL,
                        `parintintin_word` TEXT NOT NULL,
                        `phonetic_ipa` TEXT NOT NULL,
                        `portuguese_translation` TEXT NOT NULL,
                        `grammatical_class` TEXT NOT NULL,
                        `semantic_category` TEXT NOT NULL,
                        `example_parintintin` TEXT NOT NULL,
                        `example_portuguese` TEXT NOT NULL,
                        `cultural_note` TEXT NOT NULL,
                        `audio_url` TEXT,
                        `is_favorite` INTEGER NOT NULL,
                        `updated_at_epoch_millis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cultural_articles` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subtitle` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `body_paragraphs` TEXT NOT NULL,
                        `topic` TEXT NOT NULL,
                        `reading_time_minutes` INTEGER NOT NULL,
                        `source_reference` TEXT NOT NULL,
                        `is_bookmarked` INTEGER NOT NULL,
                        `updated_at_epoch_millis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_metadata` (
                        `id` TEXT NOT NULL,
                        `last_sync_epoch_millis` INTEGER NOT NULL,
                        `last_status` TEXT NOT NULL,
                        `remote_version` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(callback)
            .build()

        FrameworkSQLiteOpenHelperFactory().create(config).use { helper ->
            val db = helper.writableDatabase
            populate(db)
        }
    }

    private fun createV2Database(
        dbName: String,
        populate: (SupportSQLiteDatabase) -> Unit
    ) {
        val callback = object : SupportSQLiteOpenHelper.Callback(2) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dictionary_entries` (
                        `id` TEXT NOT NULL,
                        `parintintin_word` TEXT NOT NULL,
                        `phonetic_ipa` TEXT NOT NULL,
                        `portuguese_translation` TEXT NOT NULL,
                        `grammatical_class` TEXT NOT NULL,
                        `semantic_category` TEXT NOT NULL,
                        `example_parintintin` TEXT NOT NULL,
                        `example_portuguese` TEXT NOT NULL,
                        `cultural_note` TEXT NOT NULL,
                        `audio_url` TEXT,
                        `is_favorite` INTEGER NOT NULL,
                        `updated_at_epoch_millis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cultural_articles` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subtitle` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `body_paragraphs` TEXT NOT NULL,
                        `topic` TEXT NOT NULL,
                        `reading_time_minutes` INTEGER NOT NULL,
                        `source_reference` TEXT NOT NULL,
                        `is_bookmarked` INTEGER NOT NULL,
                        `updated_at_epoch_millis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_metadata` (
                        `id` TEXT NOT NULL,
                        `last_attempt_epoch_millis` INTEGER NOT NULL,
                        `last_attempt_status` TEXT NOT NULL,
                        `last_failure_reason` TEXT,
                        `last_successful_sync_epoch_millis` INTEGER,
                        `last_successful_entries_count` INTEGER,
                        `last_successful_articles_count` INTEGER,
                        `last_successful_remote_version` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(callback)
            .build()

        FrameworkSQLiteOpenHelperFactory().create(config).use { helper ->
            val db = helper.writableDatabase
            populate(db)
        }
    }

    private fun openMigratedDatabase(dbName: String): ParintintinDatabase {
        val db = Room.databaseBuilder(context, ParintintinDatabase::class.java, dbName)
            .addMigrations(*ParintintinDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        openedDatabases += db
        // Força a abertura, execução das migrações aplicáveis e validação de schema do Room v3.
        db.openHelper.writableDatabase
        return db
    }

    private fun insertDictionaryEntry(
        db: SupportSQLiteDatabase,
        id: String,
        parintintinWord: String,
        portugueseTranslation: String,
        isFavorite: Int,
        updatedAtEpochMillis: Long
    ) {
        db.execSQL(
            """
            INSERT INTO `dictionary_entries` (
                `id`, `parintintin_word`, `phonetic_ipa`, `portuguese_translation`,
                `grammatical_class`, `semantic_category`, `example_parintintin`,
                `example_portuguese`, `cultural_note`, `audio_url`,
                `is_favorite`, `updated_at_epoch_millis`
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                id,
                parintintinWord,
                "",
                portugueseTranslation,
                "NOUN",
                "NATURE",
                "",
                "",
                "Nota de teste",
                null,
                isFavorite,
                updatedAtEpochMillis
            )
        )
    }

    private fun insertSampleArchiveRows(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO `dictionary_entries` (
                `id`, `parintintin_word`, `phonetic_ipa`, `portuguese_translation`,
                `grammatical_class`, `semantic_category`, `example_parintintin`,
                `example_portuguese`, `cultural_note`, `audio_url`,
                `is_favorite`, `updated_at_epoch_millis`
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                "entry_test_1",
                "termo_teste_alfa",
                "/tɛɾmu/",
                "tradução teste alfa",
                "NOUN",
                "EVERYDAY",
                "exemplo_teste_alfa",
                "exemplo em português alfa",
                "nota de teste alfa",
                "https://example.test/audio/alfa.mp3",
                1,
                1_700_000_001_000L
            )
        )
        db.execSQL(
            """
            INSERT INTO `dictionary_entries` (
                `id`, `parintintin_word`, `phonetic_ipa`, `portuguese_translation`,
                `grammatical_class`, `semantic_category`, `example_parintintin`,
                `example_portuguese`, `cultural_note`, `audio_url`,
                `is_favorite`, `updated_at_epoch_millis`
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                "entry_test_2",
                "termo_teste_beta",
                "/bɛta/",
                "tradução teste beta",
                "VERB",
                "NATURE",
                "exemplo_teste_beta",
                "exemplo em português beta",
                "nota de teste beta",
                null,
                0,
                1_700_000_002_000L
            )
        )

        db.execSQL(
            """
            INSERT INTO `cultural_articles` (
                `id`, `title`, `subtitle`, `summary`, `body_paragraphs`,
                `topic`, `reading_time_minutes`, `source_reference`,
                `is_bookmarked`, `updated_at_epoch_millis`
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any>(
                "article_test_1",
                "Artigo Teste Um",
                "Subtítulo de teste um",
                "Resumo de teste um",
                "[\"Parágrafo de teste 1\",\"Parágrafo de teste 2\"]",
                "HISTORY",
                4,
                "Fonte de teste 1",
                1,
                1_700_000_003_000L
            )
        )
        db.execSQL(
            """
            INSERT INTO `cultural_articles` (
                `id`, `title`, `subtitle`, `summary`, `body_paragraphs`,
                `topic`, `reading_time_minutes`, `source_reference`,
                `is_bookmarked`, `updated_at_epoch_millis`
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any>(
                "article_test_2",
                "Artigo Teste Dois",
                "Subtítulo de teste dois",
                "Resumo de teste dois",
                "[\"Parágrafo único de teste\"]",
                "TERRITORY",
                6,
                "Fonte de teste 2",
                0,
                1_700_000_004_000L
            )
        )
    }

    private suspend fun assertSampleArchiveRowsPreserved(database: ParintintinDatabase) {
        val dictionaryDao = database.dictionaryDao()
        val entries = dictionaryDao.observeEntries(
            query = "",
            category = null,
            onlyFavorites = false
        ).first()
        assertEquals(2, entries.size)
        assertEquals(2, dictionaryDao.countOnce())

        val firstEntry = entries.first { it.id == "entry_test_1" }
        assertEquals("termo_teste_alfa", firstEntry.parintintinWord)
        assertEquals("/tɛɾmu/", firstEntry.phoneticIpa)
        assertEquals("tradução teste alfa", firstEntry.portugueseTranslation)
        assertEquals("https://example.test/audio/alfa.mp3", firstEntry.audioUrl)
        assertTrue(firstEntry.isFavorite)
        assertEquals(1_700_000_001_000L, firstEntry.updatedAtEpochMillis)

        val secondEntry = entries.first { it.id == "entry_test_2" }
        assertEquals("termo_teste_beta", secondEntry.parintintinWord)
        assertNull(secondEntry.audioUrl)
        assertFalse(secondEntry.isFavorite)

        assertEquals(listOf("entry_test_1"), dictionaryDao.getFavoriteIds())
        assertEquals(1, dictionaryDao.observeFavoriteCount().first())

        val encyclopediaDao = database.encyclopediaDao()
        val articles = encyclopediaDao.observeArticles(
            query = "",
            topic = null,
            onlyBookmarked = false
        ).first()
        assertEquals(2, articles.size)
        assertEquals(2, encyclopediaDao.countOnce())

        val firstArticle = articles.first { it.id == "article_test_1" }
        assertEquals("Artigo Teste Um", firstArticle.title)
        assertEquals(listOf("Parágrafo de teste 1", "Parágrafo de teste 2"), firstArticle.bodyParagraphs)
        assertEquals(4, firstArticle.readingTimeMinutes)
        assertTrue(firstArticle.isBookmarked)
        assertEquals(1_700_000_003_000L, firstArticle.updatedAtEpochMillis)

        val secondArticle = articles.first { it.id == "article_test_2" }
        assertEquals("Artigo Teste Dois", secondArticle.title)
        assertEquals(listOf("Parágrafo único de teste"), secondArticle.bodyParagraphs)
        assertFalse(secondArticle.isBookmarked)

        assertEquals(listOf("article_test_1"), encyclopediaDao.getBookmarkedIds())
        assertEquals(1, encyclopediaDao.observeBookmarkedCount().first())
    }

    private fun assertSyncMetadataTableStructure(db: SupportSQLiteDatabase) {
        val columns = readTableColumns(db, "sync_metadata")

        val expectedColumns = mapOf(
            "id" to ColumnSpec(type = "TEXT", notNull = true, isPrimaryKey = true),
            "last_attempt_epoch_millis" to ColumnSpec(type = "INTEGER", notNull = true, isPrimaryKey = false),
            "last_attempt_status" to ColumnSpec(type = "TEXT", notNull = true, isPrimaryKey = false),
            "last_failure_reason" to ColumnSpec(type = "TEXT", notNull = false, isPrimaryKey = false),
            "last_successful_sync_epoch_millis" to ColumnSpec(type = "INTEGER", notNull = false, isPrimaryKey = false),
            "last_successful_entries_count" to ColumnSpec(type = "INTEGER", notNull = false, isPrimaryKey = false),
            "last_successful_articles_count" to ColumnSpec(type = "INTEGER", notNull = false, isPrimaryKey = false),
            "last_successful_remote_version" to ColumnSpec(type = "TEXT", notNull = false, isPrimaryKey = false)
        )

        assertEquals(expectedColumns, columns)
    }

    private fun assertSeedMetadataTableStructure(db: SupportSQLiteDatabase) {
        val columns = readTableColumns(db, "seed_metadata")

        val expectedColumns = mapOf(
            "id" to ColumnSpec(type = "TEXT", notNull = true, isPrimaryKey = true),
            "applied_seed_version" to ColumnSpec(type = "INTEGER", notNull = true, isPrimaryKey = false),
            "applied_seed_hash" to ColumnSpec(type = "TEXT", notNull = false, isPrimaryKey = false),
            "applied_seed_ids_json" to ColumnSpec(type = "TEXT", notNull = false, isPrimaryKey = false),
            "updated_at_epoch_millis" to ColumnSpec(type = "INTEGER", notNull = true, isPrimaryKey = false)
        )

        assertEquals(expectedColumns, columns)
    }

    private fun readTableColumns(
        db: SupportSQLiteDatabase,
        tableName: String
    ): Map<String, ColumnSpec> {
        val columns = mutableMapOf<String, ColumnSpec>()
        db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow("name")
            val typeIdx = cursor.getColumnIndexOrThrow("type")
            val notNullIdx = cursor.getColumnIndexOrThrow("notnull")
            val pkIdx = cursor.getColumnIndexOrThrow("pk")
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIdx)
                columns[name] = ColumnSpec(
                    type = cursor.getString(typeIdx),
                    notNull = cursor.getInt(notNullIdx) == 1,
                    isPrimaryKey = cursor.getInt(pkIdx) > 0
                )
            }
        }
        return columns
    }

    private fun readUserVersion(db: SupportSQLiteDatabase): Int =
        db.query("PRAGMA user_version").use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getInt(0)
        }

    private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean =
        db.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(tableName)
        ).use { cursor ->
            cursor.moveToFirst()
        }

    private data class ColumnSpec(
        val type: String,
        val notNull: Boolean,
        val isPrimaryKey: Boolean
    )
}
