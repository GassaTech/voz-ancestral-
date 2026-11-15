package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dictionary_entries")
data class DictionaryEntryEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "parintintin_word")
    val parintintinWord: String,
    @ColumnInfo(name = "phonetic_ipa")
    val phoneticIpa: String,
    @ColumnInfo(name = "portuguese_translation")
    val portugueseTranslation: String,
    @ColumnInfo(name = "grammatical_class")
    val grammaticalClass: String,
    @ColumnInfo(name = "semantic_category")
    val semanticCategory: String,
    @ColumnInfo(name = "example_parintintin")
    val exampleParintintin: String,
    @ColumnInfo(name = "example_portuguese")
    val examplePortuguese: String,
    @ColumnInfo(name = "cultural_note")
    val culturalNote: String,
    @ColumnInfo(name = "audio_url")
    val audioUrl: String?,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long
)
