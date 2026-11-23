package com.example.data.mapper

import com.example.data.local.entity.IndigenousLandEntity
import com.example.data.remote.model.RemoteIndigenousLand
import com.example.domain.model.IndigenousLand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndigenousLandMappersTest {

    @Test
    fun `RemoteIndigenousLand para Domain e Entity preserva todos os campos e multiplos valores`() {
        val remote = RemoteIndigenousLand(
            objectId = 2501L,
            territoryCode = "10201",
            name = "Ipixuna",
            rawEthnicity = "Parintintin; Kagwahiva, Tenharim",
            ethnicities = listOf("Parintintin", "Kagwahiva", "Tenharim"),
            rawMunicipality = "Humaitá / Manicoré",
            municipalities = listOf("Humaitá", "Manicoré"),
            rawStateUf = "am, ro",
            statesUf = listOf("AM", "RO"),
            surfaceArea = 215362.45,
            phase = "Regularizada",
            modality = "Tradicionalmente Ocupada",
            reStudy = null,
            regionalCoordination = "CR Madeira",
            borderStrip = "Não",
            adminUnitCode = "1402",
            adminUnitName = "Coordenação Regional",
            adminUnitAcronym = "CR-MAD",
            unionDomain = "Sim",
            updatedAtRaw = "2024-08-19",
            epsg = "4674"
        )

        val domain = remote.toDomain()
        val entity = remote.toEntity()
        val roundTrip = entity.toDomain()

        assertEquals("terrai:10201", domain.id)
        assertEquals(2501L, domain.objectId)
        assertEquals("10201", domain.territoryCode)
        assertEquals("Ipixuna", domain.name)
        assertEquals("Parintintin; Kagwahiva, Tenharim", domain.rawEthnicity)
        assertEquals(listOf("Parintintin", "Kagwahiva", "Tenharim"), domain.ethnicities)
        assertEquals("Humaitá / Manicoré", domain.rawMunicipality)
        assertEquals(listOf("Humaitá", "Manicoré"), domain.municipalities)
        assertEquals("am, ro", domain.rawStateUf)
        assertEquals(listOf("AM", "RO"), domain.statesUf)
        assertEquals(215362.45, domain.surfaceArea!!, 0.0001)
        assertEquals("Regularizada", domain.phase)
        assertEquals("Tradicionalmente Ocupada", domain.modality)
        assertEquals("2024-08-19", domain.updatedAtRaw)
        assertEquals(domain, roundTrip)
    }

    @Test
    fun `campos ausentes ou em branco permanecem nulos e listas vazias sem dados inventados`() {
        val remote = RemoteIndigenousLand(
            objectId = 99L,
            territoryCode = "   ",
            name = null,
            rawEthnicity = " ",
            ethnicities = emptyList(),
            rawMunicipality = null,
            municipalities = emptyList(),
            rawStateUf = "",
            statesUf = emptyList(),
            surfaceArea = null,
            phase = null,
            modality = "  ",
            reStudy = null,
            regionalCoordination = null,
            borderStrip = null,
            adminUnitCode = null,
            adminUnitName = null,
            adminUnitAcronym = null,
            unionDomain = null,
            updatedAtRaw = null,
            epsg = null
        )

        val domain = remote.toDomain()
        val entity = domain.toEntity()

        assertEquals("oid:99", domain.id)
        assertEquals(99L, domain.objectId)
        assertNull(domain.territoryCode)
        assertNull(domain.name)
        assertNull(domain.rawEthnicity)
        assertTrue(domain.ethnicities.isEmpty())
        assertNull(domain.rawMunicipality)
        assertTrue(domain.municipalities.isEmpty())
        assertNull(domain.rawStateUf)
        assertTrue(domain.statesUf.isEmpty())
        assertNull(domain.surfaceArea)
        assertNull(domain.phase)
        assertNull(domain.modality)
        assertNull(domain.updatedAtRaw)
        assertEquals(domain, entity.toDomain())
    }

    @Test
    fun `id deterministico prioriza terrai_cod depois objectId e por fim hash estavel`() {
        val withSameCodeDifferentObjectIdA = RemoteIndigenousLand(
            objectId = 1L,
            territoryCode = "5001",
            name = "Nove de Janeiro",
            rawEthnicity = null,
            ethnicities = emptyList(),
            rawMunicipality = null,
            municipalities = emptyList(),
            rawStateUf = null,
            statesUf = emptyList(),
            surfaceArea = null,
            phase = null,
            modality = null,
            reStudy = null,
            regionalCoordination = null,
            borderStrip = null,
            adminUnitCode = null,
            adminUnitName = null,
            adminUnitAcronym = null,
            unionDomain = null,
            updatedAtRaw = null,
            epsg = null
        )
        val withSameCodeDifferentObjectIdB = withSameCodeDifferentObjectIdA.copy(objectId = 999L)

        assertEquals(
            withSameCodeDifferentObjectIdA.toDomain().id,
            withSameCodeDifferentObjectIdB.toDomain().id
        )

        val withoutCodeOrObjectIdA = withSameCodeDifferentObjectIdA.copy(
            objectId = null,
            territoryCode = null,
            rawStateUf = "AM"
        )
        val withoutCodeOrObjectIdB = withoutCodeOrObjectIdA.copy(rawStateUf = "RO")

        val domainA = withoutCodeOrObjectIdA.toDomain()
        val domainA2 = withoutCodeOrObjectIdA.toDomain()
        val domainB = withoutCodeOrObjectIdB.toDomain()

        assertTrue(domainA.id.startsWith("anon:"))
        assertEquals(domainA.id, domainA2.id)
        assertNotEquals(domainA.id, domainB.id)
    }

    @Test
    fun `Domain com id vazio deriva id deterministico e faz split de listas a partir dos campos brutos`() {
        val domain = IndigenousLand(
            id = "",
            objectId = null,
            territoryCode = "777",
            name = "Tenharim Marmelos",
            rawEthnicity = "Tenharim, Parintintin",
            ethnicities = emptyList(),
            rawMunicipality = "Humaitá; Manicoré",
            municipalities = emptyList(),
            rawStateUf = "am / ro",
            statesUf = emptyList(),
            surfaceArea = -5.0,
            phase = "Homologada",
            modality = "Tradicionalmente Ocupada",
            updatedAtRaw = null
        )

        val entity: IndigenousLandEntity = domain.toEntity()

        assertEquals("terrai:777", entity.id)
        assertEquals(listOf("Tenharim", "Parintintin"), entity.ethnicities)
        assertEquals(listOf("Humaitá", "Manicoré"), entity.municipalities)
        assertEquals(listOf("AM", "RO"), entity.statesUf)
        assertNull(entity.surfaceArea)
    }
}
