package com.example.domain.usecase

import com.example.core.common.AppResult
import com.example.domain.model.IndigenousLand
import com.example.domain.model.RemoteSyncOutcome
import com.example.domain.model.TerritoryFilter
import com.example.domain.model.TerritoryFilterOptions
import com.example.domain.model.TerritorySortOption
import com.example.domain.repository.IndigenousLandRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveIndigenousLandsUseCase @Inject constructor(
    private val repository: IndigenousLandRepository
) {
    operator fun invoke(filter: TerritoryFilter): Flow<List<IndigenousLand>> =
        repository.observeTerritories(filter)

    operator fun invoke(
        query: String = "",
        stateUf: String? = null,
        phase: String? = null,
        modality: String? = null,
        sortOption: TerritorySortOption = TerritorySortOption()
    ): Flow<List<IndigenousLand>> =
        repository.observeTerritories(
            query = query,
            stateUf = stateUf,
            phase = phase,
            modality = modality,
            sortOption = sortOption
        )
}

class ObserveIndigenousLandFilterOptionsUseCase @Inject constructor(
    private val repository: IndigenousLandRepository
) {
    operator fun invoke(): Flow<TerritoryFilterOptions> =
        repository.observeFilterOptions()
}

class ObserveIndigenousLandByIdUseCase @Inject constructor(
    private val repository: IndigenousLandRepository
) {
    operator fun invoke(id: String): Flow<IndigenousLand?> =
        repository.observeById(id)
}

class ObserveIndigenousLandsCountUseCase @Inject constructor(
    private val repository: IndigenousLandRepository
) {
    operator fun invoke(): Flow<Int> = repository.observeTotalCount()
}

class SyncIndigenousLandsUseCase @Inject constructor(
    private val repository: IndigenousLandRepository
) {
    suspend operator fun invoke(): AppResult<RemoteSyncOutcome> = repository.syncFromRemote()
}
