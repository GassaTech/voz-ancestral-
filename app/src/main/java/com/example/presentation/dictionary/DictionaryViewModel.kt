package com.example.presentation.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.DictionaryEntry
import com.example.domain.model.GrammaticalClass
import com.example.domain.model.SemanticCategory
import com.example.domain.usecase.ObserveDictionaryEntriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
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

sealed interface DictionaryEntriesState {
    data object Loading : DictionaryEntriesState
    data class Empty(val hasActiveFilter: Boolean) : DictionaryEntriesState
    data class Success(val entries: List<DictionaryEntry>) : DictionaryEntriesState
    data class Error(val message: String) : DictionaryEntriesState
}

data class DictionaryUiState(
    val query: String = "",
    val selectedCategory: SemanticCategory? = null,
    val selectedGrammaticalClass: GrammaticalClass? = null,
    val contentState: DictionaryEntriesState = DictionaryEntriesState.Loading
)

@HiltViewModel
class DictionaryViewModel @Inject constructor(
    private val observeDictionaryEntries: ObserveDictionaryEntriesUseCase
) : ViewModel() {

    private data class DictionaryQueryCriteria(
        val query: String,
        val category: SemanticCategory?,
        val grammaticalClass: GrammaticalClass?,
        val retryCount: Int
    )

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<SemanticCategory?>(null)
    private val selectedGrammaticalClass = MutableStateFlow<GrammaticalClass?>(null)
    private val retryTrigger = MutableStateFlow(0)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val entriesStateFlow = combine(
        searchQuery.debounce(SEARCH_DEBOUNCE_MILLIS),
        selectedCategory,
        selectedGrammaticalClass,
        retryTrigger
    ) { query, category, grammaticalClass, retryCount ->
        DictionaryQueryCriteria(
            query = query,
            category = category,
            grammaticalClass = grammaticalClass,
            retryCount = retryCount
        )
    }.flatMapLatest { criteria ->
        observeDictionaryEntries(
            query = criteria.query,
            category = criteria.category,
            onlyFavorites = false,
            grammaticalClass = criteria.grammaticalClass
        )
            .map<List<DictionaryEntry>, DictionaryEntriesState> { entries ->
                if (entries.isEmpty()) {
                    DictionaryEntriesState.Empty(
                        hasActiveFilter = criteria.query.isNotBlank() ||
                            criteria.category != null ||
                            criteria.grammaticalClass != null
                    )
                } else {
                    DictionaryEntriesState.Success(entries)
                }
            }
            .onStart { emit(DictionaryEntriesState.Loading) }
            .catch { throwable ->
                if (throwable is CancellationException) {
                    throw throwable
                }
                emit(
                    DictionaryEntriesState.Error(
                        throwable.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: DEFAULT_ERROR_MESSAGE
                    )
                )
            }
    }

    val uiState: StateFlow<DictionaryUiState> = combine(
        searchQuery,
        selectedCategory,
        selectedGrammaticalClass,
        entriesStateFlow
    ) { query, category, grammaticalClass, contentState ->
        DictionaryUiState(
            query = query,
            selectedCategory = category,
            selectedGrammaticalClass = grammaticalClass,
            contentState = contentState
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

    fun onSelectGrammaticalClass(grammaticalClass: GrammaticalClass?) {
        selectedGrammaticalClass.value = grammaticalClass
    }

    fun onClearFilters() {
        searchQuery.value = ""
        selectedCategory.value = null
        selectedGrammaticalClass.value = null
    }

    fun onRetry() {
        retryTrigger.value += 1
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 150L
        const val DEFAULT_ERROR_MESSAGE = "Erro ao consultar verbetes no banco de dados local."
    }
}
