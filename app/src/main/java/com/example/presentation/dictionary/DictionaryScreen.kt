package com.example.presentation.dictionary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.DictionaryEntry
import com.example.domain.model.GrammaticalClass
import com.example.domain.model.SemanticCategory
import com.example.presentation.components.DemonstrativeModeNoticeCard

@Composable
fun DictionaryScreen(
    modifier: Modifier = Modifier,
    viewModel: DictionaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DictionaryContent(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onSelectCategory = viewModel::onSelectCategory,
        onSelectGrammaticalClass = viewModel::onSelectGrammaticalClass,
        onClearFilters = viewModel::onClearFilters,
        onRetry = viewModel::onRetry,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DictionaryContent(
    uiState: DictionaryUiState,
    onQueryChange: (String) -> Unit,
    onSelectCategory: (SemanticCategory?) -> Unit,
    onSelectGrammaticalClass: (GrammaticalClass?) -> Unit,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasActiveFilter = uiState.query.isNotBlank() ||
        uiState.selectedCategory != null ||
        uiState.selectedGrammaticalClass != null

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("dictionary_screen_root"),
        contentAlignment = Alignment.TopCenter
    ) {
        val useTwoColumns = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("dictionary_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DictionaryHeaderCard(
                    totalLocalEntriesCount = uiState.totalLocalEntriesCount
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(text = stringResource(R.string.dictionary_search_hint))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(
                                onClick = { onQueryChange("") },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dictionary_clear_search")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.dictionary_clear_search)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dictionary_search_input")
                )
            }

            item {
                SemanticCategoryFilterSection(
                    selectedCategory = uiState.selectedCategory,
                    onSelectCategory = onSelectCategory
                )
            }

            item {
                GrammaticalClassFilterSection(
                    selectedGrammaticalClass = uiState.selectedGrammaticalClass,
                    onSelectGrammaticalClass = onSelectGrammaticalClass
                )
            }

            item {
                DemonstrativeModeNoticeCard()
            }

            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.dictionary_entries_section_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .semantics { heading() }
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.contentState is DictionaryEntriesState.Success) {
                            val count = uiState.contentState.entries.size
                            val countText = if (hasActiveFilter) {
                                stringResource(R.string.dictionary_filtered_results_count, count)
                            } else {
                                stringResource(R.string.dictionary_entries_count, count)
                            }
                            Text(
                                text = countText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("dictionary_entries_count")
                            )
                        }
                        if (hasActiveFilter && uiState.contentState is DictionaryEntriesState.Success) {
                            TextButton(
                                onClick = onClearFilters,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dictionary_clear_filters_button")
                            ) {
                                Text(text = stringResource(R.string.dictionary_clear_filters))
                            }
                        }
                    }
                }
            }

            when (val content = uiState.contentState) {
                DictionaryEntriesState.Loading -> {
                    item {
                        DictionaryLoadingState()
                    }
                }

                is DictionaryEntriesState.Empty -> {
                    item {
                        DictionaryFeedbackCard(
                            icon = if (content.hasActiveFilter) {
                                Icons.Rounded.SearchOff
                            } else {
                                Icons.AutoMirrored.Outlined.MenuBook
                            },
                            title = stringResource(
                                if (content.hasActiveFilter) {
                                    R.string.dictionary_empty_filtered_title
                                } else {
                                    R.string.dictionary_empty_title
                                }
                            ),
                            subtitle = stringResource(
                                if (content.hasActiveFilter) {
                                    R.string.dictionary_empty_filtered_subtitle
                                } else {
                                    R.string.dictionary_empty_subtitle
                                }
                            ),
                            actionLabel = if (content.hasActiveFilter) {
                                stringResource(R.string.dictionary_clear_filters)
                            } else {
                                null
                            },
                            onAction = if (content.hasActiveFilter) onClearFilters else null,
                            cardTestTag = "dictionary_empty_state",
                            actionTestTag = "dictionary_clear_filters_button"
                        )
                    }
                }

                is DictionaryEntriesState.Error -> {
                    item {
                        DictionaryFeedbackCard(
                            icon = Icons.Rounded.ErrorOutline,
                            title = stringResource(R.string.dictionary_error_title),
                            subtitle = content.message,
                            actionLabel = stringResource(R.string.dictionary_retry),
                            onAction = onRetry,
                            cardTestTag = "dictionary_error_state",
                            actionTestTag = "dictionary_retry_button"
                        )
                    }
                }

                is DictionaryEntriesState.Success -> {
                    if (useTwoColumns) {
                        val rows = content.entries.chunked(2)
                        items(
                            items = rows,
                            key = { row -> row.joinToString("_") { it.id } }
                        ) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { entry ->
                                    DictionaryEntryCard(
                                        entry = entry,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        items(
                            items = content.entries,
                            key = { it.id }
                        ) { entry ->
                            DictionaryEntryCard(entry = entry)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DictionaryHeaderCard(totalLocalEntriesCount: Int) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dictionary_header_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.dictionary_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .semantics { heading() }
                )

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(
                                R.string.dictionary_entries_count,
                                totalLocalEntriesCount
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("dictionary_total_entries_count")
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.dictionary_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = stringResource(R.string.dictionary_offline_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun SemanticCategoryFilterSection(
    selectedCategory: SemanticCategory?,
    onSelectCategory: (SemanticCategory?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.dictionary_categories_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() }
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("dictionary_category_filters")
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text(stringResource(R.string.dictionary_all_categories)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("dictionary_category_all")
                )
            }
            items(
                items = SemanticCategory.entries,
                key = { it.name }
            ) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectCategory(if (isSelected) null else category) },
                    label = { Text(category.labelPt) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("dictionary_category_${category.name}")
                )
            }
        }
    }
}

@Composable
private fun GrammaticalClassFilterSection(
    selectedGrammaticalClass: GrammaticalClass?,
    onSelectGrammaticalClass: (GrammaticalClass?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.dictionary_grammatical_classes_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() }
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("dictionary_grammatical_class_filters")
        ) {
            item {
                FilterChip(
                    selected = selectedGrammaticalClass == null,
                    onClick = { onSelectGrammaticalClass(null) },
                    label = { Text(stringResource(R.string.dictionary_all_grammatical_classes)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("dictionary_grammatical_class_all")
                )
            }
            items(
                items = GrammaticalClass.entries,
                key = { it.name }
            ) { grammaticalClass ->
                val isSelected = selectedGrammaticalClass == grammaticalClass
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onSelectGrammaticalClass(if (isSelected) null else grammaticalClass)
                    },
                    label = { Text(grammaticalClass.labelPt) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("dictionary_grammatical_class_${grammaticalClass.name}")
                )
            }
        }
    }
}

@Composable
private fun DictionaryLoadingState() {
    val loadingCd = stringResource(R.string.cd_dictionary_loading)
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dictionary_loading_state")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .semantics { contentDescription = loadingCd }
                    .testTag("dictionary_loading_indicator")
            )
            Text(
                text = stringResource(R.string.dictionary_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DictionaryFeedbackCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    cardTestTag: String,
    actionTestTag: String
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(cardTestTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag(actionTestTag)
                ) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DictionaryEntryCard(
    entry: DictionaryEntry,
    modifier: Modifier = Modifier
) {
    val hasExample = entry.exampleParintintin.isNotBlank() || entry.examplePortuguese.isNotBlank()
    val hasCulturalNote = entry.culturalNote.isNotBlank()

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dictionary_entry_card_${entry.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = entry.parintintinWord,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (entry.phoneticIpa.isNotBlank()) {
                    Text(
                        text = entry.phoneticIpa,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = entry.portugueseTranslation,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = entry.grammaticalClass.labelPt,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = entry.semanticCategory.labelPt,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (hasExample || hasCulturalNote) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            if (hasExample) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.dictionary_example_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (entry.exampleParintintin.isNotBlank()) {
                            Text(
                                text = entry.exampleParintintin,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (entry.examplePortuguese.isNotBlank()) {
                            Text(
                                text = entry.examplePortuguese,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (hasCulturalNote) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.dictionary_cultural_note_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = entry.culturalNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
