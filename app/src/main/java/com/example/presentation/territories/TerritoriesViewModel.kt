package com.example.presentation.territories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.AppResult
import com.example.domain.model.IndigenousLand
import com.example.domain.model.SyncState
import com.example.domain.model.TerritoryFilter
import com.example.domain.model.TerritoryFilterOptions
import com.example.domain.model.TerritorySortDirection
import com.example.domain.model.TerritorySortField
import com.example.domain.model.TerritorySortOption
import com.example.domain.sync.ArchiveSyncCoordinator
import com.example.domain.sync.ArchiveSyncOutcome
import com.example.domain.usecase.ObserveIndigenousLandFilterOptionsUseCase
import com.example.domain.usecase.ObserveIndigenousLandsCountUseCase
import com.example.domain.usecase.ObserveIndigenousLandsUseCase
import com.example.domain.usecase.ObserveSyncStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TerritoriesContentState {
    data object Loading : TerritoriesContentState
    data class Empty(val hasActiveFilter: Boolean) : TerritoriesContentState
    data class Success(val territories: List<IndigenousLand>) : TerritoriesContentState
    data class Error(val message: String) : TerritoriesContentState
}

data class TerritoriesUiState(
    val query: String = "",
    val selectedStateUf: String? = null,
    val selectedPhase: String? = null,
    val selectedModality: String? = null,
    val sortOption: TerritorySortOption = TerritorySortOption(),
    val filterOptions: TerritoryFilterOptions = TerritoryFilterOptions(),
    val totalLocalTerritoriesCount: Int = 0,
    val syncState: SyncState = SyncState.Loading,
    val manualSyncErrorMessage: String? = null,
    val contentState: TerritoriesContentState = TerritoriesContentState.Loading
) {
    val hasActiveFilter: Boolean
        get() = query.isNotBlank() ||
            selectedStateUf != null ||
            selectedPhase != null ||
            selectedModality != null
}

@HiltViewModel
class TerritoriesViewModel @Inject constructor(
    private val observeIndigenousLands: ObserveIndigenousLandsUseCase,
    private val observeFilterOptions: ObserveIndigenousLandFilterOptionsUseCase,
    private val observeTotalCount: ObserveIndigenousLandsCountUseCase,
    observeSyncState: ObserveSyncStateUseCase,
    private val syncCoordinator: ArchiveSyncCoordinator
) : ViewModel() {

    private data class CriteriaSnapshot(
        val query: String,
        val stateUf: String?,
        val phase: String?,
        val modality: String?,
        val sortOption: TerritorySortOption,
        val filterOptions: TerritoryFilterOptions
    )

    private val searchQuery = MutableStateFlow("")
    private val selectedStateUf = MutableStateFlow<String?>(null)
    private val selectedPhase = MutableStateFlow<String?>(null)
    private val selectedModality = MutableStateFlow<String?>(null)
    private val sortOption = MutableStateFlow(TerritorySortOption())
    private val retryTrigger = MutableStateFlow(0)

    private val syncStateFlow: StateFlow<SyncState> = observeSyncState()
    private val manualSyncErrorMessage = MutableStateFlow<String?>(null)
    private val syncJobLock = Any()
    private var activeSyncJob: Job? = null

    private val filterSelectionsFlow = combine(
        selectedStateUf,
        selectedPhase,
        selectedModality
    ) { stateUf, phase, modality ->
        Triple(stateUf, phase, modality)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val totalLocalCountFlow: Flow<Int> = retryTrigger
        .flatMapLatest {
            observeTotalCount()
                .catch { throwable ->
                    if (throwable is CancellationException) {
                        throw throwable
                    }
                    emit(0)
                }
        }
        .onStart { emit(0) }
        .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val filterOptionsFlow: Flow<TerritoryFilterOptions> = retryTrigger
        .flatMapLatest {
            observeFilterOptions()
                .catch { throwable ->
                    if (throwable is CancellationException) {
                        throw throwable
                    }
                    emit(TerritoryFilterOptions())
                }
        }
        .onStart { emit(TerritoryFilterOptions()) }
        .distinctUntilChanged()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val territoriesStateFlow: Flow<TerritoriesContentState> = combine(
        searchQuery.debounce(SEARCH_DEBOUNCE_MILLIS),
        filterSelectionsFlow,
        sortOption,
        retryTrigger
    ) { query, (stateUf, phase, modality), sort, retryCount ->
        TerritoryFilter(
            query = query,
            stateUf = stateUf,
            phase = phase,
            modality = modality,
            sortOption = sort
        ) to retryCount
    }
        .distinctUntilChanged()
        .flatMapLatest { (filter, _) ->
            observeIndigenousLands(filter)
                .map<List<IndigenousLand>, TerritoriesContentState> { territories ->
                    if (territories.isEmpty()) {
                        TerritoriesContentState.Empty(
                            hasActiveFilter = filter.query.isNotBlank() ||
                                filter.stateUf != null ||
                                filter.phase != null ||
                                filter.modality != null
                        )
                    } else {
                        TerritoriesContentState.Success(territories)
                    }
                }
                .onStart { emit(TerritoriesContentState.Loading) }
                .catch { throwable ->
                    if (throwable is CancellationException) {
                        throw throwable
                    }
                    emit(
                        TerritoriesContentState.Error(
                            throwable.localizedMessage?.takeIf { it.isNotBlank() }
                                ?: DEFAULT_ERROR_MESSAGE
                        )
                    )
                }
        }

    private val criteriaSnapshotFlow = combine(
        searchQuery,
        filterSelectionsFlow,
        sortOption,
        filterOptionsFlow
    ) { query, (stateUf, phase, modality), sort, options ->
        CriteriaSnapshot(
            query = query,
            stateUf = stateUf,
            phase = phase,
            modality = modality,
            sortOption = sort,
            filterOptions = options
        )
    }

    private val syncSnapshotFlow = combine(
        syncStateFlow,
        manualSyncErrorMessage
    ) { syncState, syncError ->
        syncState to syncError
    }

    val uiState: StateFlow<TerritoriesUiState> = combine(
        criteriaSnapshotFlow,
        totalLocalCountFlow,
        syncSnapshotFlow,
        territoriesStateFlow
    ) { snapshot, totalCount, (syncState, syncError), contentState ->
        TerritoriesUiState(
            query = snapshot.query,
            selectedStateUf = snapshot.stateUf,
            selectedPhase = snapshot.phase,
            selectedModality = snapshot.modality,
            sortOption = snapshot.sortOption,
            filterOptions = snapshot.filterOptions,
            totalLocalTerritoriesCount = totalCount,
            syncState = syncState,
            manualSyncErrorMessage = syncError,
            contentState = contentState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TerritoriesUiState()
    )

    fun onQueryChange(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun onSelectStateUf(stateUf: String?) {
        selectedStateUf.value = stateUf?.takeIf { it.isNotBlank() }
    }

    fun onSelectPhase(phase: String?) {
        selectedPhase.value = phase?.takeIf { it.isNotBlank() }
    }

    fun onSelectModality(modality: String?) {
        selectedModality.value = modality?.takeIf { it.isNotBlank() }
    }

    fun onSelectSortOption(newSortOption: TerritorySortOption) {
        sortOption.value = newSortOption
    }

    fun onSelectSortField(field: TerritorySortField) {
        val current = sortOption.value
        sortOption.value = if (current.field == field) {
            current.copy(direction = current.direction.toggled())
        } else {
            TerritorySortOption(
                field = field,
                direction = TerritorySortDirection.ASCENDING
            )
        }
    }

    fun onToggleSortDirection() {
        val current = sortOption.value
        sortOption.value = current.copy(direction = current.direction.toggled())
    }

    fun onClearFilters() {
        searchQuery.value = ""
        selectedStateUf.value = null
        selectedPhase.value = null
        selectedModality.value = null
    }

    fun onRetry() {
        retryTrigger.value += 1
    }

    fun onDismissManualSyncError() {
        manualSyncErrorMessage.value = null
    }

    fun syncManually() {
        val jobToStart = synchronized(syncJobLock) {
            val currentJob = activeSyncJob
            val hasReservedOrActiveJob = currentJob != null && !currentJob.isCompleted
            if (hasReservedOrActiveJob || syncStateFlow.value is SyncState.Syncing) {
                return
            }

            manualSyncErrorMessage.value = null
            val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val outcome = syncCoordinator.requestManualSync()
                    val errorResult = when (outcome) {
                        is ArchiveSyncOutcome.Executed -> outcome.result as? AppResult.Error
                        is ArchiveSyncOutcome.JoinedInFlight -> outcome.result as? AppResult.Error
                        ArchiveSyncOutcome.AlreadyTriggeredForLaunch -> null
                    }
                    if (errorResult != null && syncStateFlow.value !is SyncState.OfflineFallback) {
                        manualSyncErrorMessage.value = errorResult.message
                            .takeIf { it.isNotBlank() }
                            ?: DEFAULT_MANUAL_SYNC_ERROR_MESSAGE
                    } else if (errorResult == null && outcome !is ArchiveSyncOutcome.AlreadyTriggeredForLaunch) {
                        manualSyncErrorMessage.value = null
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    manualSyncErrorMessage.value = e.localizedMessage
                        ?.takeIf { it.isNotBlank() }
                        ?: DEFAULT_MANUAL_SYNC_ERROR_MESSAGE
                }
            }
            activeSyncJob = job
            job.invokeOnCompletion {
                synchronized(syncJobLock) {
                    if (activeSyncJob === job) {
                        activeSyncJob = null
                    }
                }
            }
            job
        }

        jobToStart.start()
    }

    private fun TerritorySortDirection.toggled(): TerritorySortDirection =
        when (this) {
            TerritorySortDirection.ASCENDING -> TerritorySortDirection.DESCENDING
            TerritorySortDirection.DESCENDING -> TerritorySortDirection.ASCENDING
        }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 150L
        const val DEFAULT_ERROR_MESSAGE = "Erro ao consultar Terras Indígenas no banco de dados local."
        const val DEFAULT_MANUAL_SYNC_ERROR_MESSAGE = "Não foi possível concluir a sincronização manual."
    }
}
