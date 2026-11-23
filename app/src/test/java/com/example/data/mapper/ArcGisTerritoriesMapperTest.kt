package com.example.data.mapper

import com.example.data.remote.dto.ArcGisFeatureDto
import com.example.data.remote.dto.ArcGisFeatureQueryResponseDto
import com.example.data.remote.dto.IndigenousLandAttributesDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArcGisTerritoriesMapperTest {

    @Test
    fun `splitDelimitedValues separa por virgula ponto e virgula barra e pipe removendo duplicatas`() {
        val result = splitDelimitedValues(" Kaiapó , Kayapó;  Xavante / Kaiapó | Bororo ,  ")

        assertEquals(listOf("Kaiapó", "Kayapó", "Xavante", "Bororo"), result)
    }

    @Test
    fun `splitDelimitedValues retorna lista vazia para nulo ou branco`() {
        assertTrue(splitDelimitedValues(null).isEmpty())
        assertTrue(splitDelimitedValues("").isEmpty())
        assertTrue(splitDelimitedValues("   , ; / ").isEmpty())
    }

    @Test
    fun `toRemoteIndigenousLand ignora superficie negativa ou nao finita e normaliza UFs em maiusculas`() {
        val dto = IndigenousLandAttributesDto(
            objectId = 12L,
            terraiCod = " 777 ",
            terraiNom = " Terra Teste ",
            etniaNome = " Etnia A, Etnia B ",
            municipio = " Cidade 1; Cidade 2 ",
            ufSigla = " pa , mt , PA ",
            superficie = Double.NaN,
            faseTi = "  ",
            modalidade = " Reserva Indígena ",
            dataAtual = null
        )

        val mapped = dto.toRemoteIndigenousLand()

        assertEquals(12L, mapped.objectId)
        assertEquals("777", mapped.territoryCode)
        assertEquals("Terra Teste", mapped.name)
        assertEquals("Etnia A, Etnia B", mapped.rawEthnicity)
        assertEquals(listOf("Etnia A", "Etnia B"), mapped.ethnicities)
        assertEquals(listOf("Cidade 1", "Cidade 2"), mapped.municipalities)
        assertEquals(listOf("PA", "MT"), mapped.statesUf)
        assertNull(mapped.surfaceArea)
        assertNull(mapped.phase)
        assertEquals("Reserva Indígena", mapped.modality)
        assertNull(mapped.updatedAtRaw)

        val negativeAreaMapped = dto.copy(superficie = -10.0).toRemoteIndigenousLand()
        assertNull(negativeAreaMapped.surfaceArea)
    }

    @Test
    fun `toRemoteTerritoriesPage ignora features com attributes nulos e calcula paginacao`() {
        val response = ArcGisFeatureQueryResponseDto(
            features = listOf(
                ArcGisFeatureDto(attributes = null),
                ArcGisFeatureDto(
                    attributes = IndigenousLandAttributesDto(
                        terraiCod = "001",
                        terraiNom = "TI Válida"
                    )
                )
            ),
            exceededTransferLimit = true
        )

        val page = response.toRemoteTerritoriesPage(
            requestedOffset = 200,
            requestedRecordCount = 50
        )

        assertEquals(1, page.items.size)
        assertEquals("001", page.items.single().territoryCode)
        assertTrue(page.hasMorePages)
        assertEquals(202, page.nextOffset)
    }

    @Test
    fun `pagina com cinco features sendo uma com attributes nulo avanca nextOffset em cinco posicoes`() {
        val response = ArcGisFeatureQueryResponseDto(
            features = listOf(
                ArcGisFeatureDto(attributes = IndigenousLandAttributesDto(objectId = 1L, terraiCod = "001")),
                ArcGisFeatureDto(attributes = IndigenousLandAttributesDto(objectId = 2L, terraiCod = "002")),
                ArcGisFeatureDto(attributes = null),
                ArcGisFeatureDto(attributes = IndigenousLandAttributesDto(objectId = 4L, terraiCod = "004")),
                ArcGisFeatureDto(attributes = IndigenousLandAttributesDto(objectId = 5L, terraiCod = "005"))
            ),
            exceededTransferLimit = true
        )

        val page = response.toRemoteTerritoriesPage(
            requestedOffset = 10,
            requestedRecordCount = 5
        )

        assertEquals(4, page.items.size)
        assertTrue(page.hasMorePages)
        assertEquals(15, page.nextOffset)
    }
}
