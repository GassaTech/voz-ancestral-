package com.example.domain.model

enum class TerritorySortField {
    NAME,
    TERRITORY_CODE,
    SURFACE_AREA,
    UPDATED_AT
}

enum class TerritorySortDirection {
    ASCENDING,
    DESCENDING
}

data class TerritorySortOption(
    val field: TerritorySortField = TerritorySortField.NAME,
    val direction: TerritorySortDirection = TerritorySortDirection.ASCENDING
)

data class TerritoryFilter(
    val query: String = "",
    val stateUf: String? = null,
    val phase: String? = null,
    val modality: String? = null,
    val sortOption: TerritorySortOption = TerritorySortOption()
)

data class TerritoryFilterOptions(
    val statesUf: List<String> = emptyList(),
    val phases: List<String> = emptyList(),
    val modalities: List<String> = emptyList()
)
