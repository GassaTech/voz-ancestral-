package com.example.presentation.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.presentation.components.CulturalArticleCard
import com.example.presentation.components.DictionaryEntryCard
import com.example.presentation.components.EmptyArchiveState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onOpenEntry: (String) -> Unit,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
        ) {
            Text(
                text = stringResource(R.string.favorites_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            stringResource(
                                R.string.favorites_tab_words,
                                uiState.favoriteEntries.size
                            )
                        )
                    },
                    modifier = Modifier.testTag("favorites_tab_words")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            stringResource(
                                R.string.favorites_tab_articles,
                                uiState.bookmarkedArticles.size
                            )
                        )
                    },
                    modifier = Modifier.testTag("favorites_tab_articles")
                )
            }

            if (selectedTab == 0) {
                if (uiState.favoriteEntries.isEmpty() && !uiState.isLoading) {
                    EmptyArchiveState(
                        icon = Icons.Outlined.FavoriteBorder,
                        title = stringResource(R.string.favorites_empty_words_title),
                        subtitle = stringResource(R.string.favorites_empty_words_subtitle)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.favoriteEntries,
                            key = { it.id }
                        ) { entry ->
                            DictionaryEntryCard(
                                entry = entry,
                                audioState = uiState.audioState,
                                onClick = { onOpenEntry(entry.id) },
                                onToggleFavorite = { viewModel.onToggleFavorite(entry.id) },
                                onPlayAudio = { viewModel.onPlayAudio(entry) }
                            )
                        }
                    }
                }
            } else {
                if (uiState.bookmarkedArticles.isEmpty() && !uiState.isLoading) {
                    EmptyArchiveState(
                        icon = Icons.Outlined.BookmarkBorder,
                        title = stringResource(R.string.favorites_empty_articles_title),
                        subtitle = stringResource(R.string.favorites_empty_articles_subtitle)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.bookmarkedArticles,
                            key = { it.id }
                        ) { article ->
                            CulturalArticleCard(
                                article = article,
                                onClick = { onOpenArticle(article.id) },
                                onToggleBookmark = { viewModel.onToggleBookmark(article.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
