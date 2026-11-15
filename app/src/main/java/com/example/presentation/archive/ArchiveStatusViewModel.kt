package com.example.presentation.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ArchiveStats
import com.example.domain.model.SyncState
import com.example.domain.usecase.ObserveArchiveStatsUseCase
import com.example.domain.usecase.ObserveSyncStateUseCase
import com.example.domain.usecase.SyncArchiveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArchiveStatusUiState(
    val stats: ArchiveStats = ArchiveStats(),
    val syncState: SyncState = SyncState.Idle
)

@HiltViewModel
class ArchiveStatusViewModel @Inject constructor(
    observeArchiveStats: ObserveArchiveStatsUseCase,
    observeSyncState: ObserveSyncStateUseCase,
    private val syncArchive: SyncArchiveUseCase
) : ViewModel() {

    val uiState: StateFlow<ArchiveStatusUiState> = combine(
        observeArchiveStats(),
        observeSyncState()
    ) { stats, syncState ->
        ArchiveStatusUiState(
            stats = stats,
            syncState = syncState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ArchiveStatusUiState()
    )

    fun onSyncNow() {
        viewModelScope.launch {
            syncArchive()
        }
    }
}
