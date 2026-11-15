package com.example.presentation.encyclopedia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ArchiveStats
import com.example.domain.model.CulturalArticle
import com.example.domain.model.CulturalTopic
import com.example.domain.usecase.ObserveArchiveStatsUseCase
import com.example.domain.usecase.ObserveCulturalArticlesUseCase
import com.example.domain.usecase.ToggleArticleBookmarkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EncyclopediaUiState(
    val selectedTopic: CulturalTopic? = null,
    val articles: List<CulturalArticle> = emptyList(),
    val stats: ArchiveStats = ArchiveStats(),
    val isLoading: Boolean = true
)

@HiltViewModel
class EncyclopediaViewModel @Inject constructor(
    private val observeCulturalArticles: ObserveCulturalArticlesUseCase,
    private val toggleArticleBookmark: ToggleArticleBookmarkUseCase,
    observeArchiveStats: ObserveArchiveStatsUseCase
) : ViewModel() {

    private val selectedTopic = MutableStateFlow<CulturalTopic?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val articlesFlow = selectedTopic.flatMapLatest { topic ->
        observeCulturalArticles(topic = topic)
    }

    val uiState: StateFlow<EncyclopediaUiState> = combine(
        selectedTopic,
        articlesFlow,
        observeArchiveStats()
    ) { topic, articles, stats ->
        EncyclopediaUiState(
            selectedTopic = topic,
            articles = articles,
            stats = stats,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EncyclopediaUiState()
    )

    fun selectTopic(topic: CulturalTopic?) {
        selectedTopic.value = topic
    }

    fun onToggleBookmark(articleId: String) {
        viewModelScope.launch {
            toggleArticleBookmark(articleId)
        }
    }
}
