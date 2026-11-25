package com.example.presentation.territories

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.TerritorySortDirection
import com.example.domain.model.TerritorySortField
import com.example.domain.model.TerritorySortOption
import com.example.presentation.components.CatalogLocalCountBadge

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerritoriesHeaderCard(
    totalLocalTerritoriesCount: Int,
    modifier: Modifier = Modifier
) {
    val countLabel = if (totalLocalTerritoriesCount == 1) {
        stringResource(R.string.home_offline_territory_singular)
    } else {
        stringResource(R.string.home_offline_territories_count, totalLocalTerritoriesCount)
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("territories_header_card")
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
                    text = stringResource(R.string.territories_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .semantics { heading() }
                )

                CatalogLocalCountBadge(
                    label = countLabel,
                    badgeTestTag = "territories_total_count_badge"
                )
            }

            Text(
                text = stringResource(R.string.territories_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = stringResource(R.string.territories_offline_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun TerritorySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = {
            Text(text = stringResource(R.string.territories_search_label))
        },
        placeholder = {
            Text(text = stringResource(R.string.territories_search_hint))
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("territories_clear_search")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.territories_clear_search)
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
        modifier = modifier
            .fillMaxWidth()
            .testTag("territories_search_input")
    )
}

@Composable
fun TerritoryFilterChipRow(
    title: String,
    selectedValue: String?,
    options: List<String>,
    onSelectValue: (String?) -> Unit,
    @StringRes chipDescriptionTemplateRes: Int,
    listTestTag: String,
    allChipTestTag: String,
    chipTestTagPrefix: String,
    modifier: Modifier = Modifier
) {
    val allOptions = remember(options, selectedValue) {
        if (selectedValue != null && selectedValue !in options) {
            listOf(selectedValue) + options
        } else {
            options
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() }
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag(listTestTag)
        ) {
            item {
                val allLabel = stringResource(R.string.territories_filter_all)
                val allCd = stringResource(chipDescriptionTemplateRes, allLabel)
                FilterChip(
                    selected = selectedValue == null,
                    onClick = { onSelectValue(null) },
                    label = { Text(allLabel) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .semantics { contentDescription = allCd }
                        .testTag(allChipTestTag)
                )
            }

            items(
                items = allOptions,
                key = { it }
            ) { option ->
                val isSelected = selectedValue == option
                val optionCd = stringResource(chipDescriptionTemplateRes, option)
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectValue(if (isSelected) null else option) },
                    label = { Text(option) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .semantics { contentDescription = optionCd }
                        .testTag("${chipTestTagPrefix}$option")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerritorySortSection(
    sortOption: TerritorySortOption,
    onSelectSortField: (TerritorySortField) -> Unit,
    onToggleSortDirection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAscending = sortOption.direction == TerritorySortDirection.ASCENDING
    val directionLabel = stringResource(
        if (isAscending) {
            R.string.territories_sort_direction_asc
        } else {
            R.string.territories_sort_direction_desc
        }
    )
    val directionCd = stringResource(
        if (isAscending) {
            R.string.cd_territories_sort_direction_asc
        } else {
            R.string.cd_territories_sort_direction_desc
        }
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.territories_sort_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .semantics { heading() }
            )

            OutlinedButton(
                onClick = onToggleSortDirection,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .semantics { contentDescription = directionCd }
                    .testTag("territories_sort_direction_button")
            ) {
                Icon(
                    imageVector = if (isAscending) {
                        Icons.Rounded.ArrowUpward
                    } else {
                        Icons.Rounded.ArrowDownward
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = directionLabel)
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("territories_sort_fields")
        ) {
            items(
                items = TerritorySortField.entries,
                key = { it.name }
            ) { field ->
                val label = stringResource(field.labelResId())
                val sortCd = stringResource(R.string.cd_territories_sort_chip, label)
                FilterChip(
                    selected = sortOption.field == field,
                    onClick = { onSelectSortField(field) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .semantics { contentDescription = sortCd }
                        .testTag("territories_sort_field_${field.name}")
                )
            }
        }
    }
}

@StringRes
private fun TerritorySortField.labelResId(): Int = when (this) {
    TerritorySortField.NAME -> R.string.territories_sort_field_name
    TerritorySortField.TERRITORY_CODE -> R.string.territories_sort_field_code
    TerritorySortField.SURFACE_AREA -> R.string.territories_sort_field_area
    TerritorySortField.UPDATED_AT -> R.string.territories_sort_field_updated
}
