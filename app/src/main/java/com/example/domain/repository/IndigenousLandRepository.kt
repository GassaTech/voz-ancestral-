package com.example.domain.repository

import com.example.core.common.AppResult
import com.example.domain.model.IndigenousLand
import com.example.domain.model.RemoteSyncOutcome
import kotlinx.coroutines.flow.Flow

interface IndigenousLandRepository {

    fun observeAll(): Flow<List<IndigenousLand>>

    fun observeTerritories(
        query: String = "",
        stateUf: String? = null,
        phase: String? = null,
        modality: String? = null
    ): Flow<List<IndigenousLand>>

    fun observeById(id: String): Flow<IndigenousLand?>

    suspend fun findByTerritoryCode(territoryCode: String): IndigenousLand?

    fun observeTotalCount(): Flow<Int>

    suspend fun countOnce(): Int

    suspend fun upsert(territory: IndigenousLand)

    suspend fun upsertAll(territories: List<IndigenousLand>)

    suspend fun update(territory: IndigenousLand): Boolean

    suspend fun deleteAll()

    suspend fun syncFromRemote(): AppResult<RemoteSyncOutcome>
}
