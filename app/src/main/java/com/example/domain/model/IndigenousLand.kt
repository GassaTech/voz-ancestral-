package com.example.domain.model

/**
 * Modelo de domínio para uma Terra Indígena proveniente da base territorial IBAMA / SIGAGEO.
 *
 * Campos ausentes na origem permanecem explicitamente `null` (ou listas vazias),
 * preservando tanto os textos brutos quanto as coleções derivadas de valores múltiplos.
 */
data class IndigenousLand(
    val id: String,
    val objectId: Long? = null,
    val territoryCode: String? = null,
    val name: String? = null,
    val rawEthnicity: String? = null,
    val ethnicities: List<String> = emptyList(),
    val rawMunicipality: String? = null,
    val municipalities: List<String> = emptyList(),
    val rawStateUf: String? = null,
    val statesUf: List<String> = emptyList(),
    val surfaceArea: Double? = null,
    val phase: String? = null,
    val modality: String? = null,
    val updatedAtRaw: String? = null
)
