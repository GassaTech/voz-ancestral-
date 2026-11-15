package com.example.presentation.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.audio.AudioPlaybackState
import com.example.core.audio.AudioPlayer
import com.example.domain.model.DictionaryEntry
import com.example.domain.model.SemanticCategory
import com.example.domain.usecase.ObserveDictionaryEntriesUseCase
import com.example.domain.usecase.ToggleFavoriteEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DictionaryUiState(
    val query: String = "",
    val selectedCategory: SemanticCategory? = null,
    val entries: List<DictionaryEntry> = emptyList(),
    val audioState: AudioPlaybackState = AudioPlaybackState.Idle,
    val isLoading: Boolean = true
)

@HiltViewModel
class DictionaryViewModel @Inject constructor(
    private val observeDictionaryEntries: ObserveDictionaryEntriesUseCase,
    private val toggleFavoriteEntry: ToggleFavoriteEntryUseCase,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<SemanticCategory?>(null)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val entriesFlow = combine(
        searchQuery.debounce(150L),
        selectedCategory
    ) { query, category ->
        query to category
    }.flatMapLatest { (query, category) ->
        observeDictionaryEntries(query = query, category = category)
    }

    val uiState: StateFlow<DictionaryUiState> = combine(
        searchQuery,
        selectedCategory,
        entriesFlow,
        audioPlayer.playbackState
    ) { query, category, entries, audioState ->
        DictionaryUiState(
            query = query,
            selectedCategory = category,
            entries = entries,
            audioState = audioState,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DictionaryUiState()
    )

    fun onQueryChange(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun onSelectCategory(category: SemanticCategory?) {
        selectedCategory.value = category
    }

    fun onToggleFavorite(entryId: String) {
        viewModelScope.launch {
            toggleFavoriteEntry(entryId)
        }
    }

    fun onPlayAudio(entry: DictionaryEntry) {
        audioPlayer.playOrToggle(entry.id, entry.audioUrl)
    }

    fun dismissAudioNotice() {
        audioPlayer.stop()
    }
}
