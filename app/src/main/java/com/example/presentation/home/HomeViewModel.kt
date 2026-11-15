package com.example.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ArchiveStats
import com.example.domain.usecase.ObserveArchiveStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val archiveStats: ArchiveStats = ArchiveStats(),
    val isStageDetailsExpanded: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeArchiveStats: ObserveArchiveStatsUseCase
) : ViewModel() {

    private val stageDetailsExpanded = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        observeArchiveStats(),
        stageDetailsExpanded
    ) { stats, expanded ->
        HomeUiState(
            archiveStats = stats,
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
}
