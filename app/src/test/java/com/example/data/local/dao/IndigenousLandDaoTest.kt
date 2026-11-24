package com.example.data.local.dao

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.database.ParintintinDatabase
import com.example.data.local.entity.IndigenousLandEntity
import com.example.domain.model.TerritorySortDirection
import com.example.domain.model.TerritorySortField
import com.example.domain.model.TerritorySortOption
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class IndigenousLandDaoTest {

    private lateinit var database: ParintintinDatabase
    private lateinit var dao: IndigenousLandDao

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ParintintinDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.indigenousLandDao()
    }

    @After
    fun tearDown() {
        if (database.isOpen) {
            database.close()
        }
    }

    @Test
    fun `insercao e leitura preservam todos os campos territoriais e multiplas etnias`() = runBlocking {
        val entity = IndigenousLandEntity(
            id = "terrai:101",
            objectId = 10L,
            territoryCode = "101",
            name = "Ipixuna",
            rawEthnicity = "Parintintin, Kagwahiva, Tenharim",
            ethnicities = listOf("Parintintin", "Kagwahiva", "Tenharim"),
            rawMunicipality = "Humaitá; Manicoré",
            municipalities = listOf("Humaitá", "Manicoré"),
            rawStateUf = "AM, RO",
            statesUf = listOf("AM", "RO"),
            surfaceArea = 215362.45,
            phase = "Regularizada",
            modality = "Tradicionalmente Ocupada",
            updatedAtRaw = "2024-08-19"
        )

        dao.insertStrict(entity)

        val loaded = dao.getByIdOnce("terrai:101")
        assertNotNull(loaded)
        assertEquals(entity, loaded)
        assertEquals(entity, dao.findByTerritoryCode("101"))
        assertEquals(1, dao.countOnce())
    }

    @Test
    fun `atualizacao via update e upsert altera registro sem duplicar`() = runBlocking {
        val original = sampleEntity(
            id = "terrai:202",
            territoryCode = "202",
            name = "Nove de Janeiro",
            phase = "Declarada",
            surfaceArea = 10000.0
        )
        dao.upsert(original)

        val updatedByUpdate = original.copy(
            phase = "Homologada",
            surfaceArea = 10500.75
        )
        val rowsUpdated = dao.update(updatedByUpdate)
        assertEquals(1, rowsUpdated)
        assertEquals("Homologada", dao.getByIdOnce("terrai:202")?.phase)
        assertEquals(10500.75, dao.getByIdOnce("terrai:202")?.surfaceArea!!, 0.001)

        val updatedByUpsert = updatedByUpdate.copy(
            name = "Nove de Janeiro Atualizada",
            updatedAtRaw = "2025-01-10"
        )
        dao.upsert(updatedByUpsert)

        val all = dao.observeAll().first()
        assertEquals(1, all.size)
        assertEquals("Nove de Janeiro Atualizada", all.single().name)
        assertEquals("2025-01-10", all.single().updatedAtRaw)
    }

    @Test
    fun `indice unico em terrai_cod rejeita codigo duplicado com id distinto e permite multiplos nulos`() = runBlocking {
        dao.insertStrict(
            sampleEntity(
                id = "terrai:303",
                territoryCode = "303",
                name = "Terra A"
            )
        )

        var constraintViolated = false
        try {
            dao.insertStrict(
                sampleEntity(
                    id = "terrai:outro_id",
                    territoryCode = "303",
                    name = "Terra Duplicada"
                )
            )
        } catch (_: SQLiteConstraintException) {
            constraintViolated = true
        }
        assertTrue(constraintViolated)

        dao.insertStrict(
            sampleEntity(
                id = "oid:1",
                territoryCode = null,
                name = "Sem Codigo 1"
            )
        )
        dao.insertStrict(
            sampleEntity(
                id = "oid:2",
                territoryCode = null,
                name = "Sem Codigo 2"
            )
        )

        assertEquals(3, dao.countOnce())
    }

    @Test
    fun `campos ausentes permanecem nulos no Room sem conversao para strings vazias`() = runBlocking {
        val minimal = IndigenousLandEntity(
            id = "anon:minimal",
            objectId = null,
            territoryCode = null,
            name = null,
            rawEthnicity = null,
            ethnicities = emptyList(),
            rawMunicipality = null,
            municipalities = emptyList(),
            rawStateUf = null,
            statesUf = emptyList(),
            surfaceArea = null,
            phase = null,
            modality = null,
            updatedAtRaw = null
        )

        dao.upsert(minimal)

        val loaded = dao.getByIdOnce("anon:minimal")
        assertNotNull(loaded)
        assertNull(loaded?.objectId)
        assertNull(loaded?.territoryCode)
        assertNull(loaded?.name)
        assertNull(loaded?.rawEthnicity)
        assertTrue(loaded?.ethnicities.orEmpty().isEmpty())
        assertNull(loaded?.rawMunicipality)
        assertTrue(loaded?.municipalities.orEmpty().isEmpty())
        assertNull(loaded?.rawStateUf)
        assertTrue(loaded?.statesUf.orEmpty().isEmpty())
        assertNull(loaded?.surfaceArea)
        assertNull(loaded?.phase)
        assertNull(loaded?.modality)
        assertNull(loaded?.updatedAtRaw)
    }

    @Test
    fun `consultas por nome e por UF retornam resultados esperados sem falsos positivos`() = runBlocking {
        dao.upsertAll(
            listOf(
                sampleEntity(
                    id = "terrai:1",
                    territoryCode = "1",
                    name = "Ipixuna",
                    rawEthnicity = "Parintintin, Kagwahiva",
                    ethnicities = listOf("Parintintin", "Kagwahiva"),
                    rawStateUf = "AM",
                    statesUf = listOf("AM"),
                    phase = "Regularizada",
                    modality = "Tradicionalmente Ocupada"
                ),
                sampleEntity(
                    id = "terrai:2",
                    territoryCode = "2",
                    name = "Tenharim do Igarapé Preto",
                    rawEthnicity = "Tenharim",
                    ethnicities = listOf("Tenharim"),
                    rawStateUf = "AM, RO",
                    statesUf = listOf("AM", "RO"),
                    phase = "Regularizada",
                    modality = "Tradicionalmente Ocupada"
                ),
                sampleEntity(
                    id = "terrai:3",
                    territoryCode = "3",
                    name = "Araribóia",
                    rawEthnicity = "Guajajara, Awá",
                    ethnicities = listOf("Guajajara", "Awá"),
                    rawStateUf = "MA",
                    statesUf = listOf("MA"),
                    phase = "Homologada",
                    modality = "Reserva Indígena"
                )
            )
        )

        val byName = dao.observeTerritories(query = "ipixuna").first()
        assertEquals(listOf("terrai:1"), byName.map { it.id })

        val byEthnicityQuery = dao.observeTerritories(query = "Kagwahiva").first()
        assertEquals(listOf("terrai:1"), byEthnicityQuery.map { it.id })

        val byUfAm = dao.observeByStateUf("AM").first()
        assertEquals(listOf("terrai:1", "terrai:2"), byUfAm.map { it.id })

        val byUfRo = dao.observeTerritories(stateUf = "ro").first()
        assertEquals(listOf("terrai:2"), byUfRo.map { it.id })

        val byUfMa = dao.observeByStateUf("MA").first()
        assertEquals(listOf("terrai:3"), byUfMa.map { it.id })

        val combined = dao.observeTerritories(
            query = "Tenharim",
            stateUf = "AM",
            phase = "Regularizada",
            modality = "Tradicionalmente Ocupada"
        ).first()
        assertEquals(listOf("terrai:2"), combined.map { it.id })
    }

    @Test
    fun `indices esperados existem na tabela indigenous_lands`() {
        val rawDb = database.openHelper.writableDatabase
        val indexes = mutableMapOf<String, Boolean>()
        rawDb.query("PRAGMA index_list(`indigenous_lands`)").use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow("name")
            val uniqueIdx = cursor.getColumnIndexOrThrow("unique")
            while (cursor.moveToNext()) {
                indexes[cursor.getString(nameIdx)] = cursor.getInt(uniqueIdx) == 1
            }
        }

        assertEquals(true, indexes["index_indigenous_lands_terrai_cod"])
        assertEquals(false, indexes["index_indigenous_lands_terrai_nom"])
        assertEquals(false, indexes["index_indigenous_lands_uf_sigla"])
        assertEquals(false, indexes["index_indigenous_lands_fase_ti"])
        assertEquals(false, indexes["index_indigenous_lands_modalidade"])
    }

    @Test
    fun `pesquisa textual trata acentos maiusculas minusculas listas JSON codigo e wildcards LIKE`() = runBlocking {
        dao.upsertAll(
            listOf(
                sampleEntity(
                    id = "terrai:101",
                    territoryCode = "101",
                    name = "Araribóia",
                    rawEthnicity = "Guajajara, Awá",
                    ethnicities = listOf("Guajajara", "Awá"),
                    rawMunicipality = "Amarante do Maranhão",
                    municipalities = listOf("Amarante do Maranhão"),
                    rawStateUf = "MA",
                    statesUf = listOf("MA")
                ),
                sampleEntity(
                    id = "terrai:202",
                    territoryCode = "202",
                    name = "Água Branca",
                    rawEthnicity = null,
                    ethnicities = listOf("Kagwahiva", "Parintintin"),
                    rawMunicipality = null,
                    municipalities = listOf("Humaitá", "Manicoré"),
                    rawStateUf = "AM",
                    statesUf = listOf("AM")
                ),
                sampleEntity(
                    id = "terrai:303",
                    territoryCode = "303",
                    name = null,
                    rawEthnicity = null,
                    ethnicities = emptyList(),
                    rawMunicipality = null,
                    municipalities = emptyList(),
                    rawStateUf = null,
                    statesUf = emptyList()
                )
            )
        )

        // Busca por nome ignorando acento e caixa
        assertEquals(listOf("terrai:101"), dao.observeTerritories(query = "arariboia").first().map { it.id })
        assertEquals(listOf("terrai:202"), dao.observeTerritories(query = "agua branca").first().map { it.id })

        // Busca por etnia com acento e via lista JSON mesmo com rawEthnicity null
        assertEquals(listOf("terrai:101"), dao.observeTerritories(query = "AWA").first().map { it.id })
        assertEquals(listOf("terrai:202"), dao.observeTerritories(query = "kagwahiva").first().map { it.id })

        // Busca por município com acento via lista JSON mesmo com rawMunicipality null
        assertEquals(listOf("terrai:202"), dao.observeTerritories(query = "humaita").first().map { it.id })
        assertEquals(listOf("terrai:101"), dao.observeTerritories(query = "maranhao").first().map { it.id })

        // Busca por código territorial
        assertEquals(listOf("terrai:303"), dao.observeTerritories(query = "303").first().map { it.id })

        // Caracteres especiais LIKE (% e _) e pontuação JSON ([ e ") não geram falsos positivos
        assertTrue(dao.observeTerritories(query = "%").first().isEmpty())
        assertTrue(dao.observeTerritories(query = "_").first().isEmpty())
        assertTrue(dao.observeTerritories(query = "[").first().isEmpty())
        assertTrue(dao.observeTerritories(query = "\"").first().isEmpty())
    }

    @Test
    fun `filtros invalidos e combinados com fase modalidade e UF funcionam sem falhas`() = runBlocking {
        dao.upsertAll(
            listOf(
                sampleEntity(
                    id = "terrai:1",
                    territoryCode = "1",
                    name = "Ipixuna",
                    rawStateUf = "AM, RO",
                    statesUf = listOf("AM", "RO"),
                    phase = "Regularizada",
                    modality = "Tradicionalmente Ocupada"
                ),
                sampleEntity(
                    id = "terrai:2",
                    territoryCode = "2",
                    name = "Cinta Larga",
                    rawStateUf = "RO, MT",
                    statesUf = listOf("RO", "MT"),
                    phase = "Homologada",
                    modality = "Reserva Indígena"
                ),
                sampleEntity(
                    id = "terrai:3",
                    territoryCode = "3",
                    name = "Sem Classificação",
                    rawStateUf = null,
                    statesUf = emptyList(),
                    phase = null,
                    modality = null
                )
            )
        )

        // Valores em branco ignorados retornam todos
        assertEquals(3, dao.observeTerritories(query = "   ", stateUf = "  ", phase = "", modality = null).first().size)

        // Fase e modalidade toleram caixa e acentos
        assertEquals(
            listOf("terrai:2"),
            dao.observeTerritories(
                stateUf = "ro",
                phase = "homologada",
                modality = "reserva indigena"
            ).first().map { it.id }
        )

        // Valores inválidos de UF e inexistentes de fase/modalidade retornam vazio com segurança
        assertTrue(dao.observeTerritories(stateUf = "%").first().isEmpty())
        assertTrue(dao.observeTerritories(stateUf = "123").first().isEmpty())
        assertTrue(dao.observeTerritories(stateUf = "AM; DROP TABLE indigenous_lands").first().isEmpty())
        assertTrue(dao.observeTerritories(stateUf = "XX").first().isEmpty())
        assertTrue(dao.observeTerritories(phase = "Inexistente").first().isEmpty())
        assertTrue(dao.observeTerritories(modality = "Inexistente").first().isEmpty())
    }

    @Test
    fun `ordenacao por nome codigo superficie e data de atualizacao e deterministica com nulos ao final`() = runBlocking {
        dao.upsertAll(
            listOf(
                sampleEntity(
                    id = "terrai:b",
                    territoryCode = "10",
                    name = "Amazonas",
                    surfaceArea = 500.0,
                    updatedAtRaw = "1730000000000" // 2024-10-27
                ),
                sampleEntity(
                    id = "terrai:a",
                    territoryCode = "9",
                    name = "Água Branca",
                    surfaceArea = 1500.0,
                    updatedAtRaw = "2024-08-15"
                ),
                sampleEntity(
                    id = "terrai:c",
                    territoryCode = "TI-A",
                    name = "Zacarias",
                    surfaceArea = 500.0,
                    updatedAtRaw = "10/01/2025" // 2025-01-10
                ),
                sampleEntity(
                    id = "terrai:nulls",
                    territoryCode = null,
                    name = null,
                    surfaceArea = null,
                    updatedAtRaw = "data-invalida"
                ),
                sampleEntity(
                    id = "terrai:nulls_2",
                    territoryCode = null,
                    name = null,
                    surfaceArea = null,
                    updatedAtRaw = null
                )
            )
        )

        // Nome ASC: "Água Branca" antes de "Amazonas", nulos por último desempatados por id ASC
        assertEquals(
            listOf("terrai:a", "terrai:b", "terrai:c", "terrai:nulls", "terrai:nulls_2"),
            dao.observeTerritories(
                sortOption = TerritorySortOption(TerritorySortField.NAME, TerritorySortDirection.ASCENDING)
            ).first().map { it.id }
        )
        // Nome DESC: "Zacarias" -> "Amazonas" -> "Água Branca", nulos por último
        assertEquals(
            listOf("terrai:c", "terrai:b", "terrai:a", "terrai:nulls", "terrai:nulls_2"),
            dao.observeTerritories(
                sortOption = TerritorySortOption(TerritorySortField.NAME, TerritorySortDirection.DESCENDING)
            ).first().map { it.id }
        )

        // Código territorial ASC: numérico 9 < 10, depois alfanumérico TI-A, nulos por último
        assertEquals(
            listOf("terrai:a", "terrai:b", "terrai:c", "terrai:nulls", "terrai:nulls_2"),
            dao.observeTerritories(
                sortOption = TerritorySortOption(TerritorySortField.TERRITORY_CODE, TerritorySortDirection.ASCENDING)
            ).first().map { it.id }
        )
        // Código territorial DESC: numérico 10 > 9, depois alfanumérico TI-A, nulos por último
        assertEquals(
            listOf("terrai:b", "terrai:a", "terrai:c", "terrai:nulls", "terrai:nulls_2"),
            dao.observeTerritories(
                sortOption = TerritorySortOption(TerritorySortField.TERRITORY_CODE, TerritorySortDirection.DESCENDING)
            ).first().map { it.id }
        )

        // Superfície ASC: empate 500.0 desempatado por nome ("Amazonas" antes de "Zacarias"), nulos por último
        assertEquals(
            listOf("terrai:b", "terrai:c", "terrai:a", "terrai:nulls", "terrai:nulls_2"),
            dao.observeTerritories(
                sortOption = TerritorySortOption(TerritorySortField.SURFACE_AREA, TerritorySortDirection.ASCENDING)
            ).first().map { it.id }
        )
        // Superfície DESC: 1500.0 -> empate 500.0 ("Amazonas" antes de "Zacarias"), nulos por último
        assertEquals(
            listOf("terrai:a", "terrai:b", "terrai:c", "terrai:nulls", "terrai:nulls_2"),
            dao.observeTerritories(
                sortOption = TerritorySortOption(TerritorySortField.SURFACE_AREA, TerritorySortDirection.DESCENDING)
            ).first().map { it.id }
        )

        // Data de atualização ASC: 2024-08-15 -> 2024-10-27 (epoch millis) -> 2025-01-10 -> inválidas/nulas por último
        assertEquals(
            listOf("terrai:a", "terrai:b", "terrai:c", "terrai:nulls", "terrai:nulls_2"),
            dao.observeTerritories(
                sortOption = TerritorySortOption(TerritorySortField.UPDATED_AT, TerritorySortDirection.ASCENDING)
            ).first().map { it.id }
        )
        // Data de atualização DESC: 2025-01-10 -> 2024-10-27 -> 2024-08-15 -> inválidas/nulas por último
        assertEquals(
            listOf("terrai:c", "terrai:b", "terrai:a", "terrai:nulls", "terrai:nulls_2"),
            dao.observeTerritories(
                sortOption = TerritorySortOption(TerritorySortField.UPDATED_AT, TerritorySortDirection.DESCENDING)
            ).first().map { it.id }
        )
    }

    @Test
    fun `consultas de opcoes distintas ignoram nulos e vazios`() = runBlocking {
        dao.upsertAll(
            listOf(
                sampleEntity(
                    id = "terrai:1",
                    territoryCode = "1",
                    name = "T1",
                    rawStateUf = "AM, RO",
                    statesUf = listOf("AM", "RO"),
                    phase = "Regularizada",
                    modality = "Tradicionalmente Ocupada"
                ),
                sampleEntity(
                    id = "terrai:2",
                    territoryCode = "2",
                    name = "T2",
                    rawStateUf = null,
                    statesUf = emptyList(),
                    phase = "  ",
                    modality = null
                )
            )
        )

        assertEquals(listOf("[\"AM\",\"RO\"]"), dao.observeDistinctStatesUfJson().first())
        assertEquals(listOf("Regularizada"), dao.observeDistinctPhases().first())
        assertEquals(listOf("Tradicionalmente Ocupada"), dao.observeDistinctModalities().first())
    }

    private fun sampleEntity(
        id: String,
        territoryCode: String?,
        name: String?,
        rawEthnicity: String? = "Parintintin",
        ethnicities: List<String> = listOf("Parintintin"),
        rawMunicipality: String? = "Humaitá",
        municipalities: List<String> = listOf("Humaitá"),
        rawStateUf: String? = "AM",
        statesUf: List<String> = listOf("AM"),
        surfaceArea: Double? = 50000.0,
        phase: String? = "Regularizada",
        modality: String? = "Tradicionalmente Ocupada",
        updatedAtRaw: String? = "2024-01-15"
    ): IndigenousLandEntity = IndigenousLandEntity(
        id = id,
        objectId = null,
        territoryCode = territoryCode,
        name = name,
        rawEthnicity = rawEthnicity,
        ethnicities = ethnicities,
        rawMunicipality = rawMunicipality,
        municipalities = municipalities,
        rawStateUf = rawStateUf,
        statesUf = statesUf,
        surfaceArea = surfaceArea,
        phase = phase,
        modality = modality,
        updatedAtRaw = updatedAtRaw
    )
}
