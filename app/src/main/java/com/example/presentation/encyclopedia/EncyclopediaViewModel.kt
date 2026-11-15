package com.example.presentation.encyclopedia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.CulturalArticle
import com.example.domain.model.CulturalTopic
import com.example.domain.usecase.ObserveCulturalArticlesUseCase
import com.example.domain.usecase.ToggleArticleBookmarkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EncyclopediaArticlesState {
    data object Loading : EncyclopediaArticlesState
    data class Empty(val hasActiveFilter: Boolean) : EncyclopediaArticlesState
    data class Success(val articles: List<CulturalArticle>) : EncyclopediaArticlesState
    data class Error(val message: String) : EncyclopediaArticlesState
}

data class EncyclopediaUiState(
    val query: String = "",
    val selectedTopic: CulturalTopic? = null,
    val expandedArticleId: String? = null,
    val contentState: EncyclopediaArticlesState = EncyclopediaArticlesState.Loading
)

@HiltViewModel
class EncyclopediaViewModel @Inject constructor(
    private val observeCulturalArticles: ObserveCulturalArticlesUseCase,
    private val toggleArticleBookmark: ToggleArticleBookmarkUseCase
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedTopic = MutableStateFlow<CulturalTopic?>(null)
    private val expandedArticleId = MutableStateFlow<String?>(null)
    private val retryTrigger = MutableStateFlow(0)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val articlesStateFlow = combine(
        searchQuery.debounce(150L),
        selectedTopic,
        retryTrigger
    ) { query, topic, _ ->
        query to topic
    }.flatMapLatest { (query, topic) ->
        observeCulturalArticles(query = query, topic = topic)
            .map<List<CulturalArticle>, EncyclopediaArticlesState> { articles ->
                if (articles.isEmpty()) {
                    EncyclopediaArticlesState.Empty(
                        hasActiveFilter = query.isNotBlank() || topic != null
                    )
                } else {
                    EncyclopediaArticlesState.Success(articles)
                }
            }
            .onStart { emit(EncyclopediaArticlesState.Loading) }
            .catch { throwable ->
                emit(
                    EncyclopediaArticlesState.Error(
                        throwable.localizedMessage
                            ?: "Erro ao consultar artigos no banco de dados local."
                    )
                )
            }
    }

    val uiState: StateFlow<EncyclopediaUiState> = combine(
        searchQuery,
        selectedTopic,
        expandedArticleId,
        articlesStateFlow
    ) { query, topic, expandedId, contentState ->
        EncyclopediaUiState(
            query = query,
            selectedTopic = topic,
            expandedArticleId = expandedId,
            contentState = contentState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EncyclopediaUiState()
    )

    fun onQueryChange(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun onSelectTopic(topic: CulturalTopic?) {
        selectedTopic.value = topic
    }

    fun onClearFilters() {
        searchQuery.value = ""
        selectedTopic.value = null
    }

    fun onToggleArticleExpanded(articleId: String) {
        expandedArticleId.value = if (expandedArticleId.value == articleId) null else articleId
    }

    fun onToggleBookmark(articleId: String) {
        viewModelScope.launch {
            toggleArticleBookmark(articleId)
        }
    }

    fun onRetry() {
        retryTrigger.value += 1
    }
}
