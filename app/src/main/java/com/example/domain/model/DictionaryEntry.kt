package com.example.domain.model

enum class GrammaticalClass(val labelPt: String) {
    NOUN("Substantivo"),
    VERB("Verbo"),
    ADJECTIVE("Adjetivo"),
    PRONOUN("Pronome"),
    ADVERB("Advérbio"),
    EXPRESSION("Expressão"),
    TOPONYM("Topônimo")
}

enum class SemanticCategory(val labelPt: String) {
    NATURE("Natureza e Território"),
    FAMILY_SOCIETY("Pessoas e Parentesco"),
    ANIMALS("Fauna Amazônica"),
    FOOD("Alimentação e Roça"),
    CULTURE_SPIRIT("Cultura e Espiritualidade"),
    EVERYDAY("Comunicação Cotidiana")
}

data class DictionaryEntry(
    val id: String,
    val parintintinWord: String,
    val phoneticIpa: String,
    val portugueseTranslation: String,
    val grammaticalClass: GrammaticalClass,
    val semanticCategory: SemanticCategory,
    val exampleParintintin: String,
    val examplePortuguese: String,
    val culturalNote: String,
    val audioUrl: String?,
    val isFavorite: Boolean,
    val updatedAtEpochMillis: Long
)
