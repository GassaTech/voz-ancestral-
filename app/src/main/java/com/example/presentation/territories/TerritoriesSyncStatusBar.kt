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
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import com.example.domain.model.SyncFailureType
import com.example.domain.model.SyncState
import com.example.presentation.components.BRAZILIAN_PORTUGUESE_LOCALE
import com.example.presentation.components.formatSyncTimestamp
import java.util.TimeZone

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerritoriesSyncStatusBar(
    syncState: SyncState,
    manualSyncErrorMessage: String?,
    onManualSync: () -> Unit,
    onDismissManualSyncError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when (syncState) {
        SyncState.Loading -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        SyncState.Idle,
        SyncState.Syncing,
        is SyncState.Synced -> MaterialTheme.colorScheme.surface
        is SyncState.RemoteEmpty -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        is SyncState.OfflineFallback -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
    }

    val accentColor = when (syncState) {
        SyncState.Loading,
        SyncState.Idle,
        SyncState.Syncing,
        is SyncState.Synced -> MaterialTheme.colorScheme.primary
        is SyncState.RemoteEmpty -> MaterialTheme.colorScheme.secondary
        is SyncState.OfflineFallback -> MaterialTheme.colorScheme.tertiary
    }

    val statusTitle = when (syncState) {
        SyncState.Loading -> stringResource(R.string.home_sync_loading_title)
        SyncState.Idle -> stringResource(R.string.home_sync_idle_title)
        SyncState.Syncing -> stringResource(R.string.home_sync_syncing_title)
        is SyncState.Synced -> stringResource(R.string.home_sync_synced_title)
        is SyncState.RemoteEmpty -> stringResource(
            if (syncState.lastSyncedEpochMillis == null) {
                R.string.home_sync_remote_empty_title
            } else {
                R.string.home_sync_remote_empty_title_with_history
            }
        )
        is SyncState.OfflineFallback -> stringResource(
            when (syncState.failureType) {
                SyncFailureType.NO_INTERNET -> R.string.home_sync_offline_no_internet_title
                SyncFailureType.UNVALIDATED_NETWORK -> R.string.home_sync_offline_unvalidated_title
                SyncFailureType.SERVER_UNAVAILABLE ->
                    R.string.home_sync_offline_server_unavailable_title
            }
        )
    }

    val locale = BRAZILIAN_PORTUGUESE_LOCALE
    val timeZone = TimeZone.getDefault()
    val statusDetail = when (syncState) {
        SyncState.Loading -> stringResource(R.string.home_sync_loading_body)
        SyncState.Idle -> stringResource(R.string.home_sync_idle_body)
        SyncState.Syncing -> stringResource(R.string.home_sync_syncing_body)
        is SyncState.Synced -> {
            val formatted = remember(syncState.lastSyncedEpochMillis, locale, timeZone) {
                formatSyncTimestamp(syncState.lastSyncedEpochMillis, locale, timeZone)
            }
            stringResource(R.string.home_sync_synced_body, formatted)
        }
        is SyncState.RemoteEmpty -> stringResource(
            if (syncState.lastSyncedEpochMillis == null) {
                R.string.home_sync_remote_empty_body
            } else {
                R.string.home_sync_remote_empty_body_with_history
            }
        )
        is SyncState.OfflineFallback -> stringResource(
            when (syncState.failureType) {
                SyncFailureType.NO_INTERNET -> R.string.home_sync_offline_no_internet_body
                SyncFailureType.UNVALIDATED_NETWORK -> R.string.home_sync_offline_unvalidated_body
                SyncFailureType.SERVER_UNAVAILABLE ->
                    R.string.home_sync_offline_server_unavailable_body
            }
        )
    }

    val isSyncing = syncState is SyncState.Syncing
    val buttonLabel = when (syncState) {
        SyncState.Syncing -> stringResource(R.string.home_sync_button_syncing)
        is SyncState.OfflineFallback -> stringResource(R.string.home_sync_button_retry)
        SyncState.Loading,
        SyncState.Idle,
        is SyncState.Synced,
        is SyncState.RemoteEmpty -> stringResource(R.string.home_sync_button_now)
    }
    val buttonIcon = if (syncState is SyncState.OfflineFallback) {
        Icons.Rounded.Refresh
    } else {
        Icons.Rounded.Sync
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("territories_sync_status_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
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
                                        .size(18.dp)
                                        .semantics { contentDescription = progressCd }
                                        .testTag("territories_sync_progress_indicator")
                                )
                            }
                            SyncState.Idle -> Icon(
                                imageVector = Icons.Rounded.Storage,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            is SyncState.Synced -> Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            is SyncState.RemoteEmpty -> Icon(
                                imageVector = Icons.Rounded.CloudQueue,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            is SyncState.OfflineFallback -> Icon(
                                imageVector = if (syncState.failureType == SyncFailureType.UNVALIDATED_NETWORK) {
                                    Icons.Rounded.WarningAmber
                                } else {
                                    Icons.Rounded.CloudOff
                                },
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("territories_sync_status_title")
                        )
                    }

                    OutlinedButton(
                        onClick = onManualSync,
                        enabled = !isSyncing,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
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
                            .testTag("territories_sync_button")
                    ) {
                        Icon(
                            imageVector = buttonIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = buttonLabel)
                    }
                }

                if (isSyncing) {
                    LinearProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("territories_sync_linear_progress")
                    )
                }

                Text(
                    text = statusDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("territories_sync_status_body")
                )
            }
        }

        if (!manualSyncErrorMessage.isNullOrBlank()) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("territories_manual_sync_error_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.territories_manual_sync_error_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = manualSyncErrorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    TextButton(
                        onClick = onDismissManualSyncError,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("territories_dismiss_sync_error_button")
                    ) {
                        Text(
                            text = stringResource(R.string.territories_dismiss_sync_error),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}
