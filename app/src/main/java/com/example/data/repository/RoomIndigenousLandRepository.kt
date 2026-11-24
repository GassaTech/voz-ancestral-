package com.example.data.repository

import androidx.room.withTransaction
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.core.common.RemoteErrorKind
import com.example.data.local.dao.IndigenousLandDao
import com.example.data.local.database.ParintintinDatabase
import com.example.data.mapper.splitDelimitedValues
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.data.remote.api.ArcGisTerritoriesQuerySpec
import com.example.data.remote.datasource.RemoteTerritoriesDataSource
import com.example.data.remote.model.RemoteIndigenousLand
import com.example.domain.model.IndigenousLand
import com.example.domain.model.RemoteSyncOutcome
import com.example.domain.model.TerritoryFilterOptions
import com.example.domain.model.TerritorySortOption
import com.example.domain.repository.IndigenousLandRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Singleton
class RoomIndigenousLandRepository @Inject constructor(
    private val database: ParintintinDatabase,
    private val indigenousLandDao: IndigenousLandDao,
    private val remoteDataSource: RemoteTerritoriesDataSource,
    private val dispatchers: DispatcherProvider
) : IndigenousLandRepository {

    private val syncMutex = Mutex()
    private val ufListJson = Json { ignoreUnknownKeys = true }

    override fun observeAll(): Flow<List<IndigenousLand>> =
        indigenousLandDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeTerritories(
        query: String,
        stateUf: String?,
        phase: String?,
        modality: String?,
        sortOption: TerritorySortOption
    ): Flow<List<IndigenousLand>> =
        indigenousLandDao.observeTerritories(
            query = query.trim(),
            stateUf = stateUf?.trim()?.takeIf { it.isNotEmpty() },
            phase = phase?.trim()?.takeIf { it.isNotEmpty() },
            modality = modality?.trim()?.takeIf { it.isNotEmpty() },
            sortOption = sortOption
        )
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeFilterOptions(): Flow<TerritoryFilterOptions> =
        combine(
            indigenousLandDao.observeDistinctStatesUfJson(),
            indigenousLandDao.observeDistinctRawStateUfs(),
            indigenousLandDao.observeDistinctPhases(),
            indigenousLandDao.observeDistinctModalities()
        ) { stateJsonList, rawStates, phases, modalities ->
            val extractedUfs = buildSet {
                stateJsonList.forEach { encoded ->
                    runCatching { ufListJson.decodeFromString<List<String>>(encoded) }
                        .getOrDefault(emptyList())
                        .forEach { uf ->
                            val normalized = uf.trim().uppercase(Locale.ROOT)
                            if (VALID_UF_REGEX.matches(normalized)) add(normalized)
                        }
                }
                rawStates.forEach { raw ->
                    splitDelimitedValues(raw).forEach { uf ->
                        val normalized = uf.trim().uppercase(Locale.ROOT)
                        if (VALID_UF_REGEX.matches(normalized)) add(normalized)
                    }
                }
            }.sorted()

            TerritoryFilterOptions(
                statesUf = extractedUfs,
                phases = phases.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sortedBy { it.lowercase(Locale.ROOT) },
                modalities = modalities.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sortedBy { it.lowercase(Locale.ROOT) }
            )
        }.flowOn(dispatchers.io)

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
            val deduplicated = territories
                .map { it.toEntity() }
                .associateBy { it.id }
                .values
                .toList()
            indigenousLandDao.upsertAll(deduplicated)
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

    override suspend fun syncFromRemote(): AppResult<RemoteSyncOutcome> = withContext(dispatchers.io) {
        syncMutex.withLock {
            val visitedOffsets = hashSetOf<Int>()
            val collectedRemoteItems = mutableListOf<RemoteIndigenousLand>()
            var currentOffset = 0

            while (true) {
                if (!visitedOffsets.add(currentOffset)) {
                    return@withLock AppResult.Error(
                        message = "Loop de paginação detectado no offset $currentOffset.",
                        cause = IllegalStateException("Offset repetido durante paginação: $currentOffset"),
                        remoteErrorKind = RemoteErrorKind.UNEXPECTED_ERROR
                    )
                }

                when (
                    val pageResult = remoteDataSource.fetchTerritoriesPage(
                        resultOffset = currentOffset,
                        resultRecordCount = ArcGisTerritoriesQuerySpec.DEFAULT_PAGE_SIZE
                    )
                ) {
                    is AppResult.Error -> return@withLock pageResult
                    is AppResult.Success -> {
                        val page = pageResult.data
                        collectedRemoteItems += page.items

                        if (!page.hasMorePages) {
                            break
                        }

                        val nextOffset = page.nextOffset
                        if (nextOffset == null || nextOffset <= currentOffset || nextOffset in visitedOffsets) {
                            return@withLock AppResult.Error(
                                message = "Paginação ArcGIS inconsistente: próximo offset inválido ($nextOffset após $currentOffset).",
                                cause = IllegalStateException(
                                    "Paginação ArcGIS não avançou corretamente: current=$currentOffset, next=$nextOffset"
                                ),
                                remoteErrorKind = RemoteErrorKind.UNEXPECTED_ERROR
                            )
                        }
                        currentOffset = nextOffset
                    }
                }
            }

            if (collectedRemoteItems.isEmpty()) {
                return@withLock AppResult.Success(RemoteSyncOutcome.Empty)
            }

            val deduplicatedEntities = collectedRemoteItems
                .map { it.toEntity() }
                .associateBy { it.id }
                .values
                .toList()

            if (deduplicatedEntities.isEmpty()) {
                return@withLock AppResult.Success(RemoteSyncOutcome.Empty)
            }

            val persistedCount = database.withTransaction {
                indigenousLandDao.upsertAll(deduplicatedEntities)
                deduplicatedEntities.size
            }

            AppResult.Success(RemoteSyncOutcome.Synced(persistedCount))
        }
    }

    private companion object {
        val VALID_UF_REGEX = Regex("^[A-Z]{2}$")
    }
}
