package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DictionaryEntryDto(
    @Json(name = "id") val id: String,
    @Json(name = "parintintin_word") val parintintinWord: String,
    @Json(name = "phonetic_ipa") val phoneticIpa: String,
    @Json(name = "portuguese_translation") val portugueseTranslation: String,
    @Json(name = "grammatical_class") val grammaticalClass: String,
    @Json(name = "semantic_category") val semanticCategory: String,
    @Json(name = "example_parintintin") val exampleParintintin: String,
    @Json(name = "example_portuguese") val examplePortuguese: String,
    @Json(name = "cultural_note") val culturalNote: String,
    @Json(name = "audio_url") val audioUrl: String? = null,
    @Json(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long
)

@JsonClass(generateAdapter = true)
data class CulturalArticleDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "subtitle") val subtitle: String,
    @Json(name = "summary") val summary: String,
    @Json(name = "body_paragraphs") val bodyParagraphs: List<String>,
    @Json(name = "topic") val topic: String,
    @Json(name = "reading_time_minutes") val readingTimeMinutes: Int,
    @Json(name = "source_reference") val sourceReference: String,
    @Json(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long
)

@JsonClass(generateAdapter = true)
data class ArchiveManifestDto(
    @Json(name = "version") val version: String,
    @Json(name = "entries_count") val entriesCount: Int,
    @Json(name = "articles_count") val articlesCount: Int
)
