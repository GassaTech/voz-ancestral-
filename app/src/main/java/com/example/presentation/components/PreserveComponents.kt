package com.example.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.ArchiveStats
import com.example.domain.model.SyncState
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun HeroPreservationBanner(
    badgeText: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_hero_banner")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 232.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_amazon),
                contentDescription = stringResource(R.string.cd_hero_illustration),
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x550E2419),
                                Color(0xBD0D2318),
                                Color(0xEE0A1D14)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 232.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = Color(0x38FFFFFF),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFBE2AF),
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("home_hero_badge")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        modifier = Modifier
                            .semantics { heading() }
                            .testTag("home_hero_title")
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE9F2EC),
                        modifier = Modifier.testTag("home_hero_subtitle")
                    )
                }
            }
        }
    }
}

@Composable
fun IntroNoticeCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("intro_notice_card")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val useStackedBody = maxWidth < 305.dp || (maxWidth < 350.dp && fontScale >= 1.3f)

            if (useStackedBody) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_cultural_oral),
                            contentDescription = stringResource(R.string.cd_archive_illustration),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(MaterialTheme.shapes.small)
                                .testTag("intro_notice_image")
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { heading() }
                                .testTag("intro_notice_title")
                        )
                    }
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("intro_notice_body")
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_cultural_oral),
                        contentDescription = stringResource(R.string.cd_archive_illustration),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(MaterialTheme.shapes.small)
                            .testTag("intro_notice_image")
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .semantics { heading() }
                                .testTag("intro_notice_title")
                        )
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                            modifier = Modifier.testTag("intro_notice_body")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DemonstrativeModeNoticeCard(
    modifier: Modifier = Modifier,
    testTagPrefix: String = "demo_mode_notice"
) {
    val containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
    val contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    val accentColor = MaterialTheme.colorScheme.tertiary

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.demo_mode_notice_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    modifier = Modifier
                        .semantics { heading() }
                        .testTag("${testTagPrefix}_title")
                )
                Text(
                    text = stringResource(R.string.demo_mode_notice_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.92f),
                    modifier = Modifier.testTag("${testTagPrefix}_body")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfflineArchiveStatusCard(
    stats: ArchiveStats,
    syncState: SyncState,
    onManualSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when (syncState) {
        SyncState.Loading -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        SyncState.Idle -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        SyncState.Syncing -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        is SyncState.Synced -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        is SyncState.OfflineFallback -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
    }

    val primaryTextColor = when (syncState) {
        SyncState.Loading -> MaterialTheme.colorScheme.onSurface
        SyncState.Idle,
        SyncState.Syncing,
        is SyncState.Synced -> MaterialTheme.colorScheme.onPrimaryContainer
        is SyncState.OfflineFallback -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    val secondaryTextColor = when (syncState) {
        SyncState.Loading -> MaterialTheme.colorScheme.onSurfaceVariant
        SyncState.Idle,
        SyncState.Syncing,
        is SyncState.Synced -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
        is SyncState.OfflineFallback -> MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
    }

    val accentColor = when (syncState) {
        SyncState.Loading,
        SyncState.Idle,
        SyncState.Syncing,
        is SyncState.Synced -> MaterialTheme.colorScheme.primary
        is SyncState.OfflineFallback -> MaterialTheme.colorScheme.tertiary
    }

    val statusTitle = when (syncState) {
        SyncState.Loading -> stringResource(R.string.home_sync_loading_title)
        SyncState.Idle -> stringResource(R.string.home_sync_idle_title)
        SyncState.Syncing -> stringResource(R.string.home_sync_syncing_title)
        is SyncState.Synced -> stringResource(R.string.home_sync_synced_title)
        is SyncState.OfflineFallback -> stringResource(R.string.home_sync_offline_title)
    }

    val locale = Locale.getDefault()
    val timeZone = TimeZone.getDefault()

    val statusBody = when (syncState) {
        SyncState.Loading -> stringResource(R.string.home_sync_loading_body)
        SyncState.Idle -> stringResource(R.string.home_sync_idle_body)
        SyncState.Syncing -> stringResource(R.string.home_sync_syncing_body)
        is SyncState.Synced -> {
            val formattedTimestamp = remember(syncState.lastSyncedEpochMillis, locale, timeZone) {
                formatSyncTimestamp(
                    epochMillis = syncState.lastSyncedEpochMillis,
                    locale = locale,
                    timeZone = timeZone
                )
            }
            stringResource(R.string.home_sync_synced_body, formattedTimestamp)
        }
        is SyncState.OfflineFallback -> stringResource(R.string.home_sync_offline_body)
    }

    val secondaryDetail = when (syncState) {
        is SyncState.Synced -> {
            if (syncState.entriesCount != null && syncState.articlesCount != null) {
                stringResource(
                    R.string.home_sync_synced_counts,
                    syncState.entriesCount,
                    syncState.articlesCount
                )
            } else {
                null
            }
        }
        is SyncState.OfflineFallback -> {
            val formattedAttempt = remember(syncState.lastAttemptEpochMillis, locale, timeZone) {
                formatSyncTimestamp(
                    epochMillis = syncState.lastAttemptEpochMillis,
                    locale = locale,
                    timeZone = timeZone
                )
            }
            stringResource(R.string.home_sync_offline_last_attempt, formattedAttempt)
        }
        else -> null
    }

    val isSyncing = syncState is SyncState.Syncing
    val buttonLabel = when (syncState) {
        SyncState.Syncing -> stringResource(R.string.home_sync_button_syncing)
        is SyncState.OfflineFallback -> stringResource(R.string.home_sync_button_retry)
        SyncState.Loading,
        SyncState.Idle,
        is SyncState.Synced -> stringResource(R.string.home_sync_button_now)
    }
    val buttonIcon = when (syncState) {
        is SyncState.OfflineFallback -> Icons.Rounded.Refresh
        else -> Icons.Rounded.Sync
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_offline_status_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.home_offline_status_title),
                style = MaterialTheme.typography.labelSmall,
                color = secondaryTextColor,
                modifier = Modifier.semantics { heading() }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (syncState) {
                    SyncState.Loading,
                    SyncState.Syncing -> {
                        val progressCd = stringResource(
                            if (syncState is SyncState.Loading) {
                                R.string.cd_home_sync_loading
                            } else {
                                R.string.cd_home_sync_in_progress
                            }
                        )
                        CircularProgressIndicator(
                            color = accentColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(20.dp)
                                .semantics { contentDescription = progressCd }
                                .testTag("home_sync_progress_indicator")
                        )
                    }
                    SyncState.Idle -> {
                        Icon(
                            imageVector = Icons.Rounded.Storage,
                            contentDescription = null,
                            tint = accentColor
                        )
                    }
                    is SyncState.Synced -> {
                        Icon(
                            imageVector = Icons.Rounded.CloudDone,
                            contentDescription = null,
                            tint = accentColor
                        )
                    }
                    is SyncState.OfflineFallback -> {
                        Icon(
                            imageVector = Icons.Rounded.CloudOff,
                            contentDescription = null,
                            tint = accentColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = statusTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = primaryTextColor,
                    modifier = Modifier.weight(1f)
                )
            }

            if (isSyncing) {
                LinearProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_sync_linear_progress")
                )
            }

            Text(
                text = statusBody,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryTextColor
            )

            if (secondaryDetail != null) {
                Text(
                    text = secondaryDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusMetricChip(
                    label = stringResource(R.string.home_offline_entries_count, stats.totalEntries),
                    accentColor = accentColor,
                    modifier = Modifier.testTag("home_offline_entries_chip")
                )
                StatusMetricChip(
                    label = stringResource(R.string.home_offline_articles_count, stats.totalArticles),
                    accentColor = accentColor,
                    modifier = Modifier.testTag("home_offline_articles_chip")
                )
            }

            OutlinedButton(
                onClick = onManualSync,
                enabled = !isSyncing,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = accentColor
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSyncing) {
                        MaterialTheme.colorScheme.outlineVariant
                    } else {
                        accentColor.copy(alpha = 0.6f)
                    }
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("home_sync_button")
            ) {
                Icon(
                    imageVector = buttonIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = buttonLabel)
            }
        }
    }
}

@Composable
private fun StatusMetricChip(
    label: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

internal fun formatSyncTimestamp(
    epochMillis: Long,
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    val formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale)
    formatter.timeZone = timeZone
    return formatter.format(Date(epochMillis))
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PlannedModuleCard(
    icon: ImageVector,
    title: String,
    description: String,
    statusLabel: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ExpandableStageDetailsCard(
    title: String,
    body: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stateLabel = stringResource(
        if (expanded) {
            R.string.home_stage_details_state_expanded
        } else {
            R.string.home_stage_details_state_collapsed
        }
    )
    val actionLabel = stringResource(
        if (expanded) {
            R.string.home_stage_details_collapse
        } else {
            R.string.home_stage_details_expand
        }
    )

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.medium)
            .clickable(
                onClickLabel = actionLabel,
                role = Role.Button,
                onClick = onToggle
            )
            .semantics {
                stateDescription = stateLabel
            }
            .testTag("stage_details_toggle")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ProvisionalDestinationScreen(
    icon: ImageVector,
    title: String,
    description: String,
    tagSuffix: String,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .testTag("provisional_screen_$tagSuffix")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.size(68.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(18.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = stringResource(R.string.provisional_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = onBackToHome,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("provisional_back_home_$tagSuffix")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.provisional_back_home))
                }
            }
        }
    }
}

