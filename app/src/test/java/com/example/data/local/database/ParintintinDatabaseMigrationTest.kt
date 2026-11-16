package com.example.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
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
    fun `migracao 1 para 2 preserva dicionario favoritos artigos marcados e converte sync_metadata SYNCED`() = runTest {
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

        val databaseV2 = openMigratedV2Database(dbName)
        val rawDb = databaseV2.openHelper.writableDatabase

        assertEquals(2, readUserVersion(rawDb))
        assertSyncMetadataTableStructure(rawDb)
        assertFalse(tableExists(rawDb, "sync_metadata_new"))

        assertSampleArchiveRowsPreserved(databaseV2)

        val migratedMetadata = databaseV2.syncMetadataDao().getMetadataOnce()
        assertNotNull(migratedMetadata)
        assertEquals(SyncMetadataEntity.SINGLETON_ID, migratedMetadata?.id)
        assertEquals(v1SyncEpochMillis, migratedMetadata?.lastAttemptEpochMillis)
        assertEquals(SyncMetadataEntity.STATUS_SYNCED, migratedMetadata?.lastAttemptStatus)
        assertNull(migratedMetadata?.lastFailureReason)
        assertEquals(v1SyncEpochMillis, migratedMetadata?.lastSuccessfulSyncEpochMillis)
        assertNull(migratedMetadata?.lastSuccessfulEntriesCount)
        assertNull(migratedMetadata?.lastSuccessfulArticlesCount)
        assertEquals(v1RemoteVersion, migratedMetadata?.lastSuccessfulRemoteVersion)
    }

    @Test
    fun `migracao 1 para 2 converte sync_metadata OFFLINE_FALLBACK mantendo campos de sucesso nulos`() = runTest {
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

        val databaseV2 = openMigratedV2Database(dbName)
        val rawDb = databaseV2.openHelper.writableDatabase

        assertEquals(2, readUserVersion(rawDb))
        assertSyncMetadataTableStructure(rawDb)
        assertSampleArchiveRowsPreserved(databaseV2)

        val migratedMetadata = databaseV2.syncMetadataDao().getMetadataOnce()
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
    fun `migracao 1 para 2 com sync_metadata vazia preserva dados existentes sem erro`() = runTest {
        val dbName = newTestDbName("migration_empty_metadata")

        createV1Database(dbName) { db ->
            insertSampleArchiveRows(db)
        }

        val databaseV2 = openMigratedV2Database(dbName)
        val rawDb = databaseV2.openHelper.writableDatabase

        assertEquals(2, readUserVersion(rawDb))
        assertSyncMetadataTableStructure(rawDb)
        assertSampleArchiveRowsPreserved(databaseV2)
        assertNull(databaseV2.syncMetadataDao().getMetadataOnce())
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

    private fun openMigratedV2Database(dbName: String): ParintintinDatabase {
        val db = Room.databaseBuilder(context, ParintintinDatabase::class.java, dbName)
            .addMigrations(*ParintintinDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        openedDatabases += db
        // Força a abertura, execução da MIGRATION_1_2 e validação de schema do Room v2.
        db.openHelper.writableDatabase
        return db
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
        val columns = mutableMapOf<String, ColumnSpec>()
        db.query("PRAGMA table_info(`sync_metadata`)").use { cursor ->
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
