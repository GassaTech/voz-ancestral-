package com.example.presentation.territories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.SyncState
import com.example.domain.model.TerritorySortField
import com.example.presentation.components.CatalogFeedbackCard
import com.example.presentation.components.CatalogLoadingCard

@Composable
fun TerritoriesScreen(
    onSelectTerritory: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TerritoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TerritoriesContent(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onSelectStateUf = viewModel::onSelectStateUf,
        onSelectPhase = viewModel::onSelectPhase,
        onSelectModality = viewModel::onSelectModality,
        onSelectSortField = viewModel::onSelectSortField,
        onToggleSortDirection = viewModel::onToggleSortDirection,
        onClearFilters = viewModel::onClearFilters,
        onRetry = viewModel::onRetry,
        onManualSync = viewModel::syncManually,
        onDismissManualSyncError = viewModel::onDismissManualSyncError,
        onSelectTerritory = onSelectTerritory,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerritoriesContent(
    uiState: TerritoriesUiState,
    onQueryChange: (String) -> Unit,
    onSelectStateUf: (String?) -> Unit,
    onSelectPhase: (String?) -> Unit,
    onSelectModality: (String?) -> Unit,
    onSelectSortField: (TerritorySortField) -> Unit,
    onToggleSortDirection: () -> Unit,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
    onManualSync: () -> Unit,
    onDismissManualSyncError: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectTerritory: (String) -> Unit = {}
) {
    val hasActiveFilter = uiState.hasActiveFilter
    val showUfFilters = uiState.filterOptions.statesUf.isNotEmpty() || uiState.selectedStateUf != null
    val showPhaseFilters = uiState.filterOptions.phases.isNotEmpty() || uiState.selectedPhase != null
    val showModalityFilters =
        uiState.filterOptions.modalities.isNotEmpty() || uiState.selectedModality != null

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("territories_screen_root"),
        contentAlignment = Alignment.TopCenter
    ) {
        val useTwoColumns = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("territories_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TerritoriesHeaderCard(
                    totalLocalTerritoriesCount = uiState.totalLocalTerritoriesCount,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            item {
                TerritoriesSyncStatusBar(
                    syncState = uiState.syncState,
                    manualSyncErrorMessage = uiState.manualSyncErrorMessage,
                    onManualSync = onManualSync,
                    onDismissManualSyncError = onDismissManualSyncError
                )
            }

            item {
                TerritorySearchField(
                    query = uiState.query,
                    onQueryChange = onQueryChange
                )
            }

            if (showUfFilters) {
                item {
                    TerritoryFilterChipRow(
                        title = stringResource(R.string.territories_uf_filter_label),
                        selectedValue = uiState.selectedStateUf,
                        options = uiState.filterOptions.statesUf,
                        onSelectValue = onSelectStateUf,
                        chipDescriptionTemplateRes = R.string.cd_territories_uf_chip,
                        listTestTag = "territories_uf_filters",
                        allChipTestTag = "territories_uf_all",
                        chipTestTagPrefix = "territories_uf_"
                    )
                }
            }

            if (showPhaseFilters) {
                item {
                    TerritoryFilterChipRow(
                        title = stringResource(R.string.territories_phase_filter_label),
                        selectedValue = uiState.selectedPhase,
                        options = uiState.filterOptions.phases,
                        onSelectValue = onSelectPhase,
                        chipDescriptionTemplateRes = R.string.cd_territories_phase_chip,
                        listTestTag = "territories_phase_filters",
                        allChipTestTag = "territories_phase_all",
                        chipTestTagPrefix = "territories_phase_"
                    )
                }
            }

            if (showModalityFilters) {
                item {
                    TerritoryFilterChipRow(
                        title = stringResource(R.string.territories_modality_filter_label),
                        selectedValue = uiState.selectedModality,
                        options = uiState.filterOptions.modalities,
                        onSelectValue = onSelectModality,
                        chipDescriptionTemplateRes = R.string.cd_territories_modality_chip,
                        listTestTag = "territories_modality_filters",
                        allChipTestTag = "territories_modality_all",
                        chipTestTagPrefix = "territories_modality_"
                    )
                }
            }

            item {
                TerritorySortSection(
                    sortOption = uiState.sortOption,
                    onSelectSortField = onSelectSortField,
                    onToggleSortDirection = onToggleSortDirection
                )
            }

            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.territories_results_section_title),
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
                        if (uiState.contentState is TerritoriesContentState.Success) {
                            val count = uiState.contentState.territories.size
                            val countText = when {
                                hasActiveFilter && count == 1 ->
                                    stringResource(R.string.territories_filtered_results_singular)
                                hasActiveFilter ->
                                    stringResource(R.string.territories_filtered_results_count, count)
                                count == 1 ->
                                    stringResource(R.string.territories_local_results_singular)
                                else ->
                                    stringResource(R.string.territories_local_results_count, count)
                            }
                            Text(
                                text = countText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("territories_results_count")
                            )
                        }
                        if (hasActiveFilter && uiState.contentState !is TerritoriesContentState.Empty) {
                            TextButton(
                                onClick = onClearFilters,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("territories_clear_filters_button")
                            ) {
                                Text(text = stringResource(R.string.territories_clear_filters))
                            }
                        }
                    }
                }
            }

            when (val content = uiState.contentState) {
                TerritoriesContentState.Loading -> {
                    item {
                        CatalogLoadingCard(
                            message = stringResource(R.string.territories_loading),
                            progressContentDescription = stringResource(R.string.cd_territories_loading),
                            cardTestTag = "territories_loading_state",
                            indicatorTestTag = "territories_loading_indicator"
                        )
                    }
                }

                is TerritoriesContentState.Empty -> {
                    item {
                        if (content.hasActiveFilter) {
                            CatalogFeedbackCard(
                                icon = Icons.Rounded.SearchOff,
                                title = stringResource(R.string.territories_empty_filtered_title),
                                subtitle = stringResource(R.string.territories_empty_filtered_subtitle),
                                actionLabel = stringResource(R.string.territories_clear_filters),
                                onAction = onClearFilters,
                                cardTestTag = "territories_empty_state",
                                actionTestTag = "territories_clear_filters_button"
                            )
                        } else {
                            val isSyncing = uiState.syncState is SyncState.Syncing
                            CatalogFeedbackCard(
                                icon = Icons.Rounded.Public,
                                title = stringResource(R.string.territories_empty_title),
                                subtitle = stringResource(R.string.territories_empty_subtitle),
                                actionLabel = stringResource(
                                    if (isSyncing) {
                                        R.string.home_sync_button_syncing
                                    } else {
                                        R.string.home_sync_button_now
                                    }
                                ),
                                onAction = onManualSync,
                                actionEnabled = !isSyncing,
                                cardTestTag = "territories_empty_state",
                                actionTestTag = "territories_empty_sync_button"
                            )
                        }
                    }
                }

                is TerritoriesContentState.Error -> {
                    item {
                        CatalogFeedbackCard(
                            icon = Icons.Rounded.ErrorOutline,
                            title = stringResource(R.string.territories_error_title),
                            subtitle = content.message,
                            actionLabel = stringResource(R.string.territories_retry),
                            onAction = onRetry,
                            cardTestTag = "territories_error_state",
                            actionTestTag = "territories_retry_button"
                        )
                    }
                }

                is TerritoriesContentState.Success -> {
                    if (useTwoColumns) {
                        val rows = content.territories.chunked(2)
                        items(
                            items = rows,
                            key = { row -> row.joinToString("_") { it.id } }
                        ) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { land ->
                                    TerritoryCard(
                                        land = land,
                                        onSelectTerritory = onSelectTerritory,
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
                            items = content.territories,
                            key = { it.id }
                        ) { land ->
                            TerritoryCard(
                                land = land,
                                onSelectTerritory = onSelectTerritory
                            )
                        }
                    }
                }
            }
        }
    }
}
