package com.example.data.repository

import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.IndigenousLandDao
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.IndigenousLand
import com.example.domain.repository.IndigenousLandRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class RoomIndigenousLandRepository @Inject constructor(
    private val indigenousLandDao: IndigenousLandDao,
    private val dispatchers: DispatcherProvider
) : IndigenousLandRepository {

    override fun observeAll(): Flow<List<IndigenousLand>> =
        indigenousLandDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeTerritories(
        query: String,
        stateUf: String?,
        phase: String?,
        modality: String?
    ): Flow<List<IndigenousLand>> =
        indigenousLandDao.observeTerritories(
            query = query.trim(),
            stateUf = stateUf?.trim()?.uppercase()?.takeIf { it.isNotEmpty() },
            phase = phase?.trim()?.takeIf { it.isNotEmpty() },
            modality = modality?.trim()?.takeIf { it.isNotEmpty() }
        )
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeById(id: String): Flow<IndigenousLand?> =
        indigenousLandDao.observeById(id)
            .map { it?.toDomain() }
            .flowOn(dispatchers.io)

    override suspend fun findByTerritoryCode(territoryCode: String): IndigenousLand? =
        withContext(dispatchers.io) {
            val normalizedCode = territoryCode.trim()
            if (normalizedCode.isEmpty()) return@withContext null
            indigenousLandDao.findByTerritoryCode(normalizedCode)?.toDomain()
        }

    override fun observeTotalCount(): Flow<Int> =
        indigenousLandDao.observeTotalCount().flowOn(dispatchers.io)

    override suspend fun countOnce(): Int =
        withContext(dispatchers.io) {
            indigenousLandDao.countOnce()
        }

    override suspend fun upsert(territory: IndigenousLand) {
        withContext(dispatchers.io) {
            indigenousLandDao.upsert(territory.toEntity())
        }
    }

    override suspend fun upsertAll(territories: List<IndigenousLand>) {
        if (territories.isEmpty()) return
        withContext(dispatchers.io) {
            indigenousLandDao.upsertAll(territories.map { it.toEntity() })
        }
    }

    override suspend fun update(territory: IndigenousLand): Boolean =
        withContext(dispatchers.io) {
            indigenousLandDao.update(territory.toEntity()) > 0
        }

    override suspend fun deleteAll() {
        withContext(dispatchers.io) {
            indigenousLandDao.deleteAll()
        }
    }
}
