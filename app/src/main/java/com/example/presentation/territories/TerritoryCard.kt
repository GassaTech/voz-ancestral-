package com.example.presentation.territories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.IndigenousLand
import com.example.presentation.components.BRAZILIAN_PORTUGUESE_LOCALE
import java.text.NumberFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerritoryCard(
    land: IndigenousLand,
    onSelectTerritory: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val displayName = remember(land.name) {
        land.name?.trim()?.takeIf { it.isNotEmpty() }
    } ?: stringResource(R.string.territories_unnamed_land)

    val territoryCode = remember(land.territoryCode) {
        land.territoryCode?.trim()?.takeIf { it.isNotEmpty() }
    }
    val statesText = remember(land.statesUf, land.rawStateUf) {
        resolveDisplayListText(land.statesUf, land.rawStateUf)
    }
    val ethnicitiesText = remember(land.ethnicities, land.rawEthnicity) {
        resolveDisplayListText(land.ethnicities, land.rawEthnicity)
    }
    val municipalitiesText = remember(land.municipalities, land.rawMunicipality) {
        resolveDisplayListText(land.municipalities, land.rawMunicipality)
    }
    val phaseText = remember(land.phase) {
        land.phase?.trim()?.takeIf { it.isNotEmpty() }
    }
    val modalityText = remember(land.modality) {
        land.modality?.trim()?.takeIf { it.isNotEmpty() }
    }
    val formattedArea = remember(land.surfaceArea) {
        land.surfaceArea?.let(::formatSurfaceArea)
    }
    val updatedAtText = remember(land.updatedAtRaw) {
        land.updatedAtRaw?.trim()?.takeIf { it.isNotEmpty() }
    }

    val hasClassificationTags = statesText != null || phaseText != null || modalityText != null
    val hasSecondaryMeta = municipalitiesText != null || formattedArea != null || updatedAtText != null

    Card(
        onClick = { onSelectTerritory(land.id) },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("territory_card_${land.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (territoryCode != null) {
                    val codeCd = stringResource(R.string.cd_territories_code_badge, territoryCode)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.testTag("territory_code_${land.id}")
                    ) {
                        Text(
                            text = stringResource(R.string.territories_code_badge, territoryCode),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .semantics { contentDescription = codeCd }
                        )
                    }
                }

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("territory_name_${land.id}")
                )
            }

            if (hasClassificationTags) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (statesText != null) {
                        val ufLabel = stringResource(R.string.territories_uf_prefix, statesText)
                        val ufCd = stringResource(R.string.cd_territories_uf_chip, statesText)
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.testTag("territory_uf_${land.id}")
                        ) {
                            Text(
                                text = ufLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .semantics { contentDescription = ufCd }
                            )
                        }
                    }

                    if (phaseText != null) {
                        val phaseCd = stringResource(R.string.cd_territories_phase_chip, phaseText)
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.testTag("territory_phase_${land.id}")
                        ) {
                            Text(
                                text = phaseText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .semantics { contentDescription = phaseCd }
                            )
                        }
                    }

                    if (modalityText != null) {
                        val modalityCd = stringResource(
                            R.string.cd_territories_modality_chip,
                            modalityText
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.testTag("territory_modality_${land.id}")
                        ) {
                            Text(
                                text = modalityText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .semantics { contentDescription = modalityCd }
                            )
                        }
                    }
                }
            }

            if (ethnicitiesText != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("territory_ethnicities_${land.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.territories_ethnicities_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = ethnicitiesText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (hasSecondaryMeta) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (municipalitiesText != null) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Rounded.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    R.string.territories_municipalities_label,
                                    municipalitiesText
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (formattedArea != null) {
                        Text(
                            text = stringResource(R.string.territories_surface_area_ha, formattedArea),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (updatedAtText != null) {
                        Text(
                            text = stringResource(R.string.territories_updated_at_label, updatedAtText),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

private fun resolveDisplayListText(items: List<String>, rawFallback: String?): String? {
    val cleanedItems = items.mapNotNull { item -> item.trim().takeIf { it.isNotEmpty() } }
    return if (cleanedItems.isNotEmpty()) {
        cleanedItems.joinToString(", ")
    } else {
        rawFallback?.trim()?.takeIf { it.isNotEmpty() }
    }
}

private fun formatSurfaceArea(areaHa: Double): String {
    val formatter = NumberFormat.getNumberInstance(BRAZILIAN_PORTUGUESE_LOCALE).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    return formatter.format(areaHa)
}
