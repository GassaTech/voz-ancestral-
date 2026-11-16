package com.example.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.entity.SyncMetadataEntity

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sync_metadata_new` (
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

        db.execSQL(
            """
            INSERT INTO `sync_metadata_new` (
                `id`,
                `last_attempt_epoch_millis`,
                `last_attempt_status`,
                `last_failure_reason`,
                `last_successful_sync_epoch_millis`,
                `last_successful_entries_count`,
                `last_successful_articles_count`,
                `last_successful_remote_version`
            )
            SELECT
                `id`,
                `last_sync_epoch_millis`,
                `last_status`,
                NULL,
                CASE
                    WHEN `last_status` = '${SyncMetadataEntity.STATUS_SYNCED}' THEN `last_sync_epoch_millis`
                    ELSE NULL
                END,
                NULL,
                NULL,
                CASE
                    WHEN `last_status` = '${SyncMetadataEntity.STATUS_SYNCED}' THEN `remote_version`
                    ELSE NULL
                END
            FROM `sync_metadata`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `sync_metadata`")
        db.execSQL("ALTER TABLE `sync_metadata_new` RENAME TO `sync_metadata`")
    }
}
