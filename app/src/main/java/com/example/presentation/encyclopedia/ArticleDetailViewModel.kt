package com.example.presentation.encyclopedia

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.domain.model.CulturalArticle
import com.example.domain.usecase.ObserveCulturalArticleByIdUseCase
import com.example.domain.usecase.ToggleArticleBookmarkUseCase
import com.example.presentation.navigation.ArticleDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeArticleById: ObserveCulturalArticleByIdUseCase,
    private val toggleArticleBookmark: ToggleArticleBookmarkUseCase
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ArticleDetailRoute>()

    val article: StateFlow<CulturalArticle?> = observeArticleById(route.articleId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun onToggleBookmark() {
        viewModelScope.launch {
            toggleArticleBookmark(route.articleId)
        }
    }
}
