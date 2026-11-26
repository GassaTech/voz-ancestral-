package com.example.presentation.territories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.domain.model.IndigenousLand
import com.example.domain.usecase.ObserveIndigenousLandByIdUseCase
import com.example.presentation.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

sealed interface TerritoryDetailsUiState {
    data object Loading : TerritoryDetailsUiState
    data class Success(val land: IndigenousLand) : TerritoryDetailsUiState
    data object NotFound : TerritoryDetailsUiState
    data class Error(val message: String) : TerritoryDetailsUiState
}

@HiltViewModel
class TerritoryDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeLandById: ObserveIndigenousLandByIdUseCase
) : ViewModel() {

    private val landId =
        savedStateHandle.toRoute<AppDestination.TerritoryDetails>().landId

    val uiState: StateFlow<TerritoryDetailsUiState> =
        observeLandById(landId)
            .map<IndigenousLand?, TerritoryDetailsUiState> { land ->
                if (land == null) {
                    TerritoryDetailsUiState.NotFound
                } else {
                    TerritoryDetailsUiState.Success(land)
                }
            }
            .onStart {
                emit(TerritoryDetailsUiState.Loading)
            }
            .catch { throwable ->
                if (throwable is CancellationException) {
                    throw throwable
                }

                emit(
                    TerritoryDetailsUiState.Error(
                        throwable.localizedMessage
                            ?.takeIf { it.isNotBlank() }
                            ?: "Não foi possível carregar os dados deste território."
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = TerritoryDetailsUiState.Loading
            )
}