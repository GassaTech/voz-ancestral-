package com.example.data.local.dao

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.domain.model.TerritoryFilter
import com.example.domain.model.TerritorySortDirection
import com.example.domain.model.TerritorySortField
import com.example.domain.model.TerritorySortOption
import java.text.Normalizer
import java.util.Locale

internal object IndigenousLandQueryBuilder {

    private val COMBINING_MARKS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val MULTIPLE_WHITESPACE_REGEX = Regex("\\s+")
    private val VALID_UF_REGEX = Regex("^[A-Z]{2}$")

    private const val REPLACEMENTS_PER_CTE_STAGE = 10

    private val ENTITY_COLUMNS = listOf(
        "id",
        "objectid",
        "terrai_cod",
        "terrai_nom",
        "etnia_nome",
        "ethnicities",
        "municipio_",
        "municipalities",
        "uf_sigla",
        "states_uf",
        "superficie",
        "fase_ti",
        "modalidade",
        "data_atual"
    )
    private val ENTITY_PROJECTION_SQL = ENTITY_COLUMNS.joinToString(separator = ", ")

    private val ACCENT_FOLDING_REPLACEMENTS: List<Pair<String, String>> = listOf(
        "á" to "a", "à" to "a", "â" to "a", "ã" to "a", "ä" to "a",
        "Á" to "a", "À" to "a", "Â" to "a", "Ã" to "a", "Ä" to "a",
        "é" to "e", "è" to "e", "ê" to "e", "ë" to "e", "ẽ" to "e",
        "É" to "e", "È" to "e", "Ê" to "e", "Ë" to "e", "Ẽ" to "e",
        "í" to "i", "ì" to "i", "î" to "i", "ï" to "i", "ĩ" to "i",
        "Í" to "i", "Ì" to "i", "Î" to "i", "Ï" to "i", "Ĩ" to "i",
        "ó" to "o", "ò" to "o", "ô" to "o", "õ" to "o", "ö" to "o",
        "Ó" to "o", "Ò" to "o", "Ô" to "o", "Õ" to "o", "Ö" to "o",
        "ú" to "u", "ù" to "u", "û" to "u", "ü" to "u", "ũ" to "u",
        "Ú" to "u", "Ù" to "u", "Û" to "u", "Ü" to "u", "Ũ" to "u",
        "ç" to "c", "Ç" to "c",
        "ñ" to "n", "Ñ" to "n",
        "ý" to "y", "ÿ" to "y", "ỹ" to "y",
        "Ý" to "y", "Ÿ" to "y", "Ỹ" to "y"
    )

    private val ACCENT_FOLDING_CHUNKS: List<List<Pair<String, String>>> =
        ACCENT_FOLDING_REPLACEMENTS.chunked(REPLACEMENTS_PER_CTE_STAGE)

    private const val ALIAS_NORM_NAME = "norm_name"
    private const val ALIAS_NORM_RAW_ETH = "norm_raw_eth"
    private const val ALIAS_NORM_LIST_ETH = "norm_list_eth"
    private const val ALIAS_NORM_RAW_MUNI = "norm_raw_muni"
    private const val ALIAS_NORM_LIST_MUNI = "norm_list_muni"
    private const val ALIAS_NORM_CODE = "norm_code"
    private const val ALIAS_NORM_PHASE = "norm_phase"
    private const val ALIAS_NORM_MODALITY = "norm_modality"

    private val PARSED_UPDATED_AT_JULIAN_SQL = """
        CASE
            WHEN data_atual IS NULL OR TRIM(data_atual) = '' THEN NULL
            WHEN TRIM(data_atual) GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]*'
                THEN COALESCE(julianday(TRIM(data_atual)), julianday(SUBSTR(TRIM(data_atual), 1, 10)))
            WHEN TRIM(data_atual) GLOB '[0-9][0-9]/[0-9][0-9]/[0-9][0-9][0-9][0-9]*'
                THEN julianday(
                    SUBSTR(TRIM(data_atual), 7, 4) || '-' ||
                    SUBSTR(TRIM(data_atual), 4, 2) || '-' ||
                    SUBSTR(TRIM(data_atual), 1, 2)
                )
            WHEN TRIM(data_atual) GLOB '[0-9]*' AND TRIM(data_atual) NOT GLOB '*[^0-9]*'
                THEN CASE
                    WHEN LENGTH(TRIM(data_atual)) >= 11 THEN (CAST(TRIM(data_atual) AS REAL) / 86400000.0) + 2440587.5
                    ELSE (CAST(TRIM(data_atual) AS REAL) / 86400.0) + 2440587.5
                END
            ELSE NULL
        END
    """.trimIndent()

    fun build(
        query: String = "",
        stateUf: String? = null,
        phase: String? = null,
        modality: String? = null,
        sortOption: TerritorySortOption = TerritorySortOption()
    ): SupportSQLiteQuery {
        val bindArgs = mutableListOf<Any>()

        val normalizedQuery = normalizeText(query)
        val trimmedStateUf = stateUf?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }
        val normalizedPhase = phase?.let(::normalizeText)?.takeIf { it.isNotEmpty() }
        val normalizedModality = modality?.let(::normalizeText)?.takeIf { it.isNotEmpty() }

        val baseWhereClauses = mutableListOf<String>()
        if (trimmedStateUf != null) {
            if (!VALID_UF_REGEX.matches(trimmedStateUf)) {
                // Valor de UF inválido (não vazio e fora do padrão de 2 letras): retorna conjunto vazio com segurança.
                baseWhereClauses += "1 = 0"
            } else {
                baseWhereClauses += """
                    (
                        UPPER(TRIM(COALESCE(uf_sigla, ''))) = ?
                        OR UPPER(COALESCE(states_uf, '')) LIKE ? ESCAPE '\'
                    )
                """.trimIndent()
                bindArgs += trimmedStateUf
                bindArgs += "%\"$trimmedStateUf\"%"
            }
        }

        val foldTargets = buildList {
            add(FoldTarget(ALIAS_NORM_NAME, "TRIM(COALESCE(terrai_nom, ''))"))
            if (normalizedQuery.isNotEmpty()) {
                add(FoldTarget(ALIAS_NORM_RAW_ETH, "COALESCE(etnia_nome, '')"))
                add(FoldTarget(ALIAS_NORM_LIST_ETH, jsonStringArrayToTextSql("ethnicities")))
                add(FoldTarget(ALIAS_NORM_RAW_MUNI, "COALESCE(municipio_, '')"))
                add(FoldTarget(ALIAS_NORM_LIST_MUNI, jsonStringArrayToTextSql("municipalities")))
                add(FoldTarget(ALIAS_NORM_CODE, "COALESCE(terrai_cod, '')"))
            }
            if (normalizedPhase != null) {
                add(FoldTarget(ALIAS_NORM_PHASE, "TRIM(COALESCE(fase_ti, ''))"))
            }
            if (normalizedModality != null) {
                add(FoldTarget(ALIAS_NORM_MODALITY, "TRIM(COALESCE(modalidade, ''))"))
            }
        }

        val outerWhereClauses = mutableListOf<String>()
        if (normalizedQuery.isNotEmpty()) {
            val likePattern = "%${escapeLikeWildcards(normalizedQuery)}%"
            outerWhereClauses += """
                (
                    $ALIAS_NORM_NAME LIKE ? ESCAPE '\'
                    OR $ALIAS_NORM_RAW_ETH LIKE ? ESCAPE '\'
                    OR $ALIAS_NORM_LIST_ETH LIKE ? ESCAPE '\'
                    OR $ALIAS_NORM_RAW_MUNI LIKE ? ESCAPE '\'
                    OR $ALIAS_NORM_LIST_MUNI LIKE ? ESCAPE '\'
                    OR $ALIAS_NORM_CODE LIKE ? ESCAPE '\'
                )
            """.trimIndent()
            repeat(6) { bindArgs += likePattern }
        }

        if (normalizedPhase != null) {
            outerWhereClauses += "(fase_ti IS NOT NULL AND $ALIAS_NORM_PHASE = ?)"
            bindArgs += normalizedPhase
        }

        if (normalizedModality != null) {
            outerWhereClauses += "(modalidade IS NOT NULL AND $ALIAS_NORM_MODALITY = ?)"
            bindArgs += normalizedModality
        }

        // Encadeia CTEs com no máximo 10 REPLACE() por estágio para manter a pilha do parser SQLite bem abaixo do limite.
        val cteDefinitions = mutableListOf<String>()
        val baseProjections = foldTargets.joinToString(separator = ", ") { target ->
            "${target.sourceSql} AS ${target.alias}"
        }
        val baseSql = buildString {
            append("SELECT ")
            append(ENTITY_PROJECTION_SQL)
            append(", ")
            append(baseProjections)
            append(" FROM indigenous_lands")
            if (baseWhereClauses.isNotEmpty()) {
                append(" WHERE ")
                append(baseWhereClauses.joinToString(separator = " AND "))
            }
        }
        cteDefinitions += "stage_0 AS ($baseSql)"

        var previousStageName = "stage_0"
        ACCENT_FOLDING_CHUNKS.forEachIndexed { index, chunk ->
            val currentStageName = "stage_${index + 1}"
            val isLastChunk = index == ACCENT_FOLDING_CHUNKS.lastIndex
            val stageProjections = foldTargets.joinToString(separator = ", ") { target ->
                val replaced = applyReplacementChunk("$previousStageName.${target.alias}", chunk)
                val finalExpr = if (isLastChunk) "LOWER($replaced)" else replaced
                "$finalExpr AS ${target.alias}"
            }
            val qualifiedEntityCols = ENTITY_COLUMNS.joinToString(separator = ", ") { col ->
                "$previousStageName.$col"
            }
            cteDefinitions += "$currentStageName AS (SELECT $qualifiedEntityCols, $stageProjections FROM $previousStageName)"
            previousStageName = currentStageName
        }

        val sql = buildString {
            append("WITH ")
            append(cteDefinitions.joinToString(separator = ", "))
            append(" SELECT ")
            append(ENTITY_PROJECTION_SQL)
            append(" FROM ")
            append(previousStageName)
            if (outerWhereClauses.isNotEmpty()) {
                append(" WHERE ")
                append(outerWhereClauses.joinToString(separator = " AND "))
            }
            append(" ORDER BY ")
            append(buildOrderByClause(sortOption))
        }

        return SimpleSQLiteQuery(sql, bindArgs.toTypedArray())
    }

    fun build(filter: TerritoryFilter): SupportSQLiteQuery = build(
        query = filter.query,
        stateUf = filter.stateUf,
        phase = filter.phase,
        modality = filter.modality,
        sortOption = filter.sortOption
    )

    internal fun normalizeText(raw: String): String {
        val collapsed = raw.trim().replace(MULTIPLE_WHITESPACE_REGEX, " ")
        if (collapsed.isEmpty()) return ""
        val decomposed = Normalizer.normalize(collapsed, Normalizer.Form.NFD)
        return COMBINING_MARKS_REGEX.replace(decomposed, "").lowercase(Locale.ROOT)
    }

    internal fun escapeLikeWildcards(value: String): String = buildString(value.length) {
        for (ch in value) {
            when (ch) {
                '\\', '%', '_' -> {
                    append('\\')
                    append(ch)
                }
                else -> append(ch)
            }
        }
    }

    private fun buildOrderByClause(sortOption: TerritorySortOption): String {
        val directionSql = when (sortOption.direction) {
            TerritorySortDirection.ASCENDING -> "ASC"
            TerritorySortDirection.DESCENDING -> "DESC"
        }

        val tieBreakerByNameAndCode =
            "CASE WHEN terrai_nom IS NULL OR TRIM(terrai_nom) = '' THEN 1 ELSE 0 END ASC, " +
                "$ALIAS_NORM_NAME ASC, " +
                "COALESCE(terrai_nom, '') COLLATE NOCASE ASC, " +
                "CASE WHEN terrai_cod IS NULL OR TRIM(terrai_cod) = '' THEN 1 ELSE 0 END ASC, " +
                "COALESCE(terrai_cod, '') COLLATE NOCASE ASC, " +
                "id ASC"

        return when (sortOption.field) {
            TerritorySortField.NAME ->
                "CASE WHEN terrai_nom IS NULL OR TRIM(terrai_nom) = '' THEN 1 ELSE 0 END ASC, " +
                    "$ALIAS_NORM_NAME $directionSql, " +
                    "COALESCE(terrai_nom, '') COLLATE NOCASE $directionSql, " +
                    "CASE WHEN terrai_cod IS NULL OR TRIM(terrai_cod) = '' THEN 1 ELSE 0 END ASC, " +
                    "COALESCE(terrai_cod, '') COLLATE NOCASE ASC, " +
                    "id ASC"

            TerritorySortField.TERRITORY_CODE ->
                "CASE WHEN terrai_cod IS NULL OR TRIM(terrai_cod) = '' THEN 1 ELSE 0 END ASC, " +
                    "CASE WHEN TRIM(terrai_cod) GLOB '[0-9]*' AND TRIM(terrai_cod) NOT GLOB '*[^0-9]*' THEN 0 ELSE 1 END ASC, " +
                    "CASE WHEN TRIM(terrai_cod) GLOB '[0-9]*' AND TRIM(terrai_cod) NOT GLOB '*[^0-9]*' THEN CAST(TRIM(terrai_cod) AS INTEGER) ELSE 0 END $directionSql, " +
                    "TRIM(COALESCE(terrai_cod, '')) COLLATE NOCASE $directionSql, " +
                    tieBreakerByNameAndCode

            TerritorySortField.SURFACE_AREA ->
                "CASE WHEN superficie IS NULL THEN 1 ELSE 0 END ASC, " +
                    "superficie $directionSql, " +
                    tieBreakerByNameAndCode

            TerritorySortField.UPDATED_AT ->
                "CASE WHEN ($PARSED_UPDATED_AT_JULIAN_SQL) IS NULL THEN 1 ELSE 0 END ASC, " +
                    "($PARSED_UPDATED_AT_JULIAN_SQL) $directionSql, " +
                    tieBreakerByNameAndCode
        }
    }

    private fun jsonStringArrayToTextSql(column: String): String =
        "REPLACE(REPLACE(REPLACE(REPLACE(COALESCE($column, ''), '[]', ''), '[\"', ' '), '\"]', ' '), '\",\"', ' ')"

    private fun applyReplacementChunk(
        expression: String,
        chunk: List<Pair<String, String>>
    ): String {
        var wrapped = expression
        for ((from, to) in chunk) {
            wrapped = "REPLACE($wrapped, '$from', '$to')"
        }
        return wrapped
    }

    private data class FoldTarget(
        val alias: String,
        val sourceSql: String
    )
}
