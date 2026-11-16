package com.example.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ArchiveStats
import com.example.domain.model.SyncState
import com.example.domain.sync.ArchiveSyncCoordinator
import com.example.domain.usecase.ObserveArchiveStatsUseCase
import com.example.domain.usecase.ObserveSyncStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val archiveStats: ArchiveStats = ArchiveStats(),
    val syncState: SyncState = SyncState.Loading,
    val isStageDetailsExpanded: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeArchiveStats: ObserveArchiveStatsUseCase,
    observeSyncState: ObserveSyncStateUseCase,
    private val syncCoordinator: ArchiveSyncCoordinator
) : ViewModel() {

    private val syncStateFlow: StateFlow<SyncState> = observeSyncState()
    private val stageDetailsExpanded = MutableStateFlow(false)
    private val syncJobLock = Any()
    private var activeSyncJob: Job? = null

    val uiState: StateFlow<HomeUiState> = combine(
        observeArchiveStats(),
        syncStateFlow,
        stageDetailsExpanded
    ) { stats, syncState, expanded ->
        HomeUiState(
            archiveStats = stats,
            syncState = syncState,
            isStageDetailsExpanded = expanded
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun toggleStageDetails() {
        stageDetailsExpanded.value = !stageDetailsExpanded.value
    }

    fun syncManually() {
        val jobToStart = synchronized(syncJobLock) {
            val currentJob = activeSyncJob
            val hasReservedOrActiveJob = currentJob != null && !currentJob.isCompleted
            if (hasReservedOrActiveJob || syncStateFlow.value is SyncState.Syncing) {
                return
            }

            val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
                try {
                    syncCoordinator.requestManualSync()
                } catch (ce: CancellationException) {
                    throw ce
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
}
