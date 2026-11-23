package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.DispatcherProvider
import com.example.data.local.database.ParintintinDatabase
import com.example.domain.model.IndigenousLand
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class RoomIndigenousLandRepositoryTest {

    private lateinit var database: ParintintinDatabase
    private lateinit var repository: RoomIndigenousLandRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ParintintinDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomIndigenousLandRepository(
            indigenousLandDao = database.indigenousLandDao(),
            dispatchers = IoDispatcherProvider
        )
    }

    @After
    fun tearDown() {
        if (database.isOpen) {
            database.close()
        }
    }

    @Test
    fun `repositorio persiste consulta filtra e atualiza territorios via Room`() = runBlocking {
        val ipixuna = IndigenousLand(
            id = "terrai:101",
            objectId = 1L,
            territoryCode = "101",
            name = "Ipixuna",
            rawEthnicity = "Parintintin, Kagwahiva",
            ethnicities = listOf("Parintintin", "Kagwahiva"),
            rawMunicipality = "Humaitá",
            municipalities = listOf("Humaitá"),
            rawStateUf = "AM",
            statesUf = listOf("AM"),
            surfaceArea = 215362.0,
            phase = "Regularizada",
            modality = "Tradicionalmente Ocupada",
            updatedAtRaw = "2024-08-19"
        )
        val semCampos = IndigenousLand(
            id = "oid:999",
            objectId = 999L,
            territoryCode = null,
            name = "Área Sem Código",
            rawEthnicity = null,
            ethnicities = emptyList(),
            rawMunicipality = null,
            municipalities = emptyList(),
            rawStateUf = "RO",
            statesUf = listOf("RO"),
            surfaceArea = null,
            phase = null,
            modality = null,
            updatedAtRaw = null
        )

        repository.upsertAll(listOf(ipixuna, semCampos))

        assertEquals(2, repository.observeTotalCount().first())
        assertEquals(2, repository.countOnce())

        val filteredByNameAndUf = repository.observeTerritories(
            query = "  Ipixuna  ",
            stateUf = " am "
        ).first()
        assertEquals(listOf("terrai:101"), filteredByNameAndUf.map { it.id })

        val foundByCode = repository.findByTerritoryCode(" 101 ")
        assertNotNull(foundByCode)
        assertEquals("Ipixuna", foundByCode?.name)

        val updated = repository.update(ipixuna.copy(phase = "Homologada"))
        assertTrue(updated)
        assertEquals("Homologada", repository.observeById("terrai:101").first()?.phase)

        val missingUpdate = repository.update(ipixuna.copy(id = "inexistente", territoryCode = "999"))
        assertFalse(missingUpdate)

        assertNull(repository.findByTerritoryCode("   "))

        repository.deleteAll()
        assertEquals(0, repository.observeAll().first().size)
    }

    private object IoDispatcherProvider : DispatcherProvider {
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val default: CoroutineDispatcher = Dispatchers.Default
        override val main: CoroutineDispatcher = Dispatchers.Main
    }
}
