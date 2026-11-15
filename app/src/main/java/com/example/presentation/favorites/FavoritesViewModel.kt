package com.example.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.audio.AudioPlaybackState
import com.example.core.audio.AudioPlayer
import com.example.domain.model.CulturalArticle
import com.example.domain.model.DictionaryEntry
import com.example.domain.usecase.ObserveCulturalArticlesUseCase
import com.example.domain.usecase.ObserveDictionaryEntriesUseCase
import com.example.domain.usecase.ToggleArticleBookmarkUseCase
import com.example.domain.usecase.ToggleFavoriteEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favoriteEntries: List<DictionaryEntry> = emptyList(),
    val bookmarkedArticles: List<CulturalArticle> = emptyList(),
    val audioState: AudioPlaybackState = AudioPlaybackState.Idle,
    val isLoading: Boolean = true
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    observeDictionaryEntries: ObserveDictionaryEntriesUseCase,
    observeCulturalArticles: ObserveCulturalArticlesUseCase,
    private val toggleFavoriteEntry: ToggleFavoriteEntryUseCase,
    private val toggleArticleBookmark: ToggleArticleBookmarkUseCase,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    val uiState: StateFlow<FavoritesUiState> = combine(
        observeDictionaryEntries(onlyFavorites = true),
        observeCulturalArticles(onlyBookmarked = true),
        audioPlayer.playbackState
    ) { entries, articles, audioState ->
        FavoritesUiState(
            favoriteEntries = entries,
            bookmarkedArticles = articles,
            audioState = audioState,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FavoritesUiState()
    )

    fun onToggleFavorite(entryId: String) {
        viewModelScope.launch {
            toggleFavoriteEntry(entryId)
        }
    }

    fun onToggleBookmark(articleId: String) {
        viewModelScope.launch {
            toggleArticleBookmark(articleId)
        }
    }

    fun onPlayAudio(entry: DictionaryEntry) {
        audioPlayer.playOrToggle(entry.id, entry.audioUrl)
    }
}
