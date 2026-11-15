package com.example.presentation.dictionary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.core.audio.AudioPlaybackState
import com.example.core.audio.AudioPlayer
import com.example.domain.model.DictionaryEntry
import com.example.domain.usecase.ObserveDictionaryEntryByIdUseCase
import com.example.domain.usecase.ToggleFavoriteEntryUseCase
import com.example.presentation.navigation.EntryDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EntryDetailUiState(
    val entry: DictionaryEntry? = null,
    val audioState: AudioPlaybackState = AudioPlaybackState.Idle
)

@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeEntryById: ObserveDictionaryEntryByIdUseCase,
    private val toggleFavoriteEntry: ToggleFavoriteEntryUseCase,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val route = savedStateHandle.toRoute<EntryDetailRoute>()

    val uiState: StateFlow<EntryDetailUiState> = combine(
        observeEntryById(route.entryId),
        audioPlayer.playbackState
    ) { entry, audioState ->
        EntryDetailUiState(
            entry = entry,
            audioState = audioState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EntryDetailUiState()
    )

    fun onToggleFavorite() {
        viewModelScope.launch {
            toggleFavoriteEntry(route.entryId)
        }
    }

    fun onPlayOrStopAudio() {
        val currentEntry = uiState.value.entry ?: return
        audioPlayer.playOrToggle(currentEntry.id, currentEntry.audioUrl)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
