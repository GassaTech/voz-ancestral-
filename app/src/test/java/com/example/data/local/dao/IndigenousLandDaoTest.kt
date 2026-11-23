package com.example.data.local.dao

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.database.ParintintinDatabase
import com.example.data.local.entity.IndigenousLandEntity
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
