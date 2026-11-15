package com.example.domain.model

enum class CulturalTopic(val labelPt: String) {
    HISTORY("História e Identidade"),
    LINGUISTICS("Língua e Fonologia"),
    TERRITORY("Território e Rios"),
    ORAL_TRADITION("Tradição Oral e Cantos"),
    MATERIAL_CULTURE("Cultura Material")
}

data class CulturalArticle(
    val id: String,
    val title: String,
    val subtitle: String,
    val summary: String,
    val bodyParagraphs: List<String>,
    val topic: CulturalTopic,
    val readingTimeMinutes: Int,
    val sourceReference: String,
    val isBookmarked: Boolean,
    val updatedAtEpochMillis: Long
)
