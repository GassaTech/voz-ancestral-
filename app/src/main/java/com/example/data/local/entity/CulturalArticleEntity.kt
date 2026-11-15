package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cultural_articles")
data class CulturalArticleEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val subtitle: String,
    val summary: String,
    @ColumnInfo(name = "body_paragraphs")
    val bodyParagraphs: List<String>,
    val topic: String,
    @ColumnInfo(name = "reading_time_minutes")
    val readingTimeMinutes: Int,
    @ColumnInfo(name = "source_reference")
    val sourceReference: String,
    @ColumnInfo(name = "is_bookmarked")
    val isBookmarked: Boolean = false,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long
)
