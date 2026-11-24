package com.example.data.local.dao

import com.example.domain.model.TerritoryFilter
import com.example.domain.model.TerritorySortDirection
import com.example.domain.model.TerritorySortField
import com.example.domain.model.TerritorySortOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IndigenousLandQueryBuilderTest {

    @Test
    fun `normalizeText remove acentos normaliza caixa e colapsa espacos`() {
        assertEquals("arariboia", IndigenousLandQueryBuilder.normalizeText("  Araribóia "))
        assertEquals("agua branca", IndigenousLandQueryBuilder.normalizeText("ÁGUA   BRANCA"))
        assertEquals("humaita", IndigenousLandQueryBuilder.normalizeText("Humaitá"))
        assertEquals("awa guajajara", IndigenousLandQueryBuilder.normalizeText("Awá  Guajajára"))
        assertEquals("maraiwatsede", IndigenousLandQueryBuilder.normalizeText("Marãiwatsédé"))
        assertEquals("", IndigenousLandQueryBuilder.normalizeText("   \t\n  "))
    }

    @Test
    fun `escapeLikeWildcards escapa barra porcentagem e sublinhado`() {
        assertEquals("100\\%\\_teste\\\\fim", IndigenousLandQueryBuilder.escapeLikeWildcards("100%_teste\\fim"))
    }

    @Test
    fun `consulta sem filtros nao gera clausula WHERE e ordena por nome com desempate deterministico`() {
        val query = IndigenousLandQueryBuilder.build()
        val sql = query.sql

        assertFalse(sql.contains(" WHERE "))
        assertEquals(0, query.argCount)
        assertTrue(sql.contains("ORDER BY CASE WHEN terrai_nom IS NULL"))
        assertTrue(sql.endsWith("id ASC"))
    }

    @Test
    fun `UF invalida gera clausula segura 1 = 0 sem wildcard LIKE`() {
        val wildcardUf = IndigenousLandQueryBuilder.build(stateUf = "%")
        assertTrue(wildcardUf.sql.contains("1 = 0"))
        assertEquals(0, wildcardUf.argCount)

        val numericUf = IndigenousLandQueryBuilder.build(stateUf = "12")
        assertTrue(numericUf.sql.contains("1 = 0"))

        val injectionUf = IndigenousLandQueryBuilder.build(stateUf = "AM'; DROP TABLE indigenous_lands;--")
        assertTrue(injectionUf.sql.contains("1 = 0"))
        assertEquals(0, injectionUf.argCount)
    }

    @Test
    fun `combinacao completa de pesquisa filtros e ordenacao gera placeholders parametrizados`() {
        val query = IndigenousLandQueryBuilder.build(
            TerritoryFilter(
                query = "  Araribóia_100% ",
                stateUf = " ma ",
                phase = " Homologada ",
                modality = " Reserva Indígena ",
                sortOption = TerritorySortOption(
                    field = TerritorySortField.SURFACE_AREA,
                    direction = TerritorySortDirection.DESCENDING
                )
            )
        )

        val sql = query.sql
        assertTrue(sql.contains(" WHERE "))
        assertTrue(sql.contains("LIKE ? ESCAPE '\\'"))
        assertTrue(sql.contains("superficie DESC"))
        assertTrue(sql.endsWith("id ASC"))
        // 6 placeholders de busca + 2 de UF + 1 de fase + 1 de modalidade = 10 argumentos
        assertEquals(10, query.argCount)
    }

    @Test
    fun `todas as opcoes de ordenacao incluem desempate final por id ASC`() {
        TerritorySortField.entries.forEach { field ->
            TerritorySortDirection.entries.forEach { direction ->
                val sql = IndigenousLandQueryBuilder.build(
                    sortOption = TerritorySortOption(field = field, direction = direction)
                ).sql
                assertTrue("Faltou id ASC para $field/$direction", sql.endsWith("id ASC"))
            }
        }
    }

    @Test
    fun `expressao SQL limita aninhamento de REPLACE por estagio evitando parser stack overflow`() {
        val sql = IndigenousLandQueryBuilder.build(
            TerritoryFilter(
                query = "Araribóia",
                stateUf = "MA",
                phase = "Homologada",
                modality = "Reserva Indígena",
                sortOption = TerritorySortOption(
                    field = TerritorySortField.NAME,
                    direction = TerritorySortDirection.ASCENDING
                )
            )
        ).sql

        // Verifica que nenhum encadeamento contínuo de REPLACE( ultrapassa 10 níveis por expressão.
        val maxConsecutiveReplaceChain = Regex("(?:REPLACE\\()+")
            .findAll(sql)
            .maxOf { match -> match.value.count { it == '(' } }

        assertTrue(
            "Aninhamento excessivo de REPLACE detectado: $maxConsecutiveReplaceChain",
            maxConsecutiveReplaceChain <= 10
        )
    }
}
