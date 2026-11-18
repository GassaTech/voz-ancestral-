package com.example.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.presentation.components.DemonstrativeModeNoticeCard
import com.example.presentation.components.ExpandableStageDetailsCard
import com.example.presentation.components.HeroPreservationBanner
import com.example.presentation.components.IntroNoticeCard
import com.example.presentation.components.OfflineArchiveStatusCard
import com.example.presentation.components.PlannedModuleCard
import com.example.presentation.components.SectionHeader

private data class PlannedModuleItem(
    val id: String,
    val icon: ImageVector,
    val titleRes: Int,
    val descRes: Int
)

private val plannedModules = listOf(
    PlannedModuleItem(
        id = "dictionary",
        icon = Icons.Rounded.MenuBook,
        titleRes = R.string.module_dictionary_title,
        descRes = R.string.module_dictionary_desc
    ),
    PlannedModuleItem(
        id = "encyclopedia",
        icon = Icons.Rounded.AutoStories,
        titleRes = R.string.module_encyclopedia_title,
        descRes = R.string.module_encyclopedia_desc
    ),
    PlannedModuleItem(
        id = "audio",
        icon = Icons.AutoMirrored.Rounded.VolumeUp,
        titleRes = R.string.module_audio_title,
        descRes = R.string.module_audio_desc
    ),
    PlannedModuleItem(
        id = "search_favorites",
        icon = Icons.Rounded.Bookmarks,
        titleRes = R.string.module_search_favorites_title,
        descRes = R.string.module_search_favorites_desc
    )
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        uiState = uiState,
        onManualSync = viewModel::syncManually,
        onToggleStageDetails = viewModel::toggleStageDetails,
        modifier = modifier
    )
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onManualSync: () -> Unit,
    onToggleStageDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusLabel = stringResource(R.string.module_status_planned)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_root"),
        contentAlignment = Alignment.TopCenter
    ) {
        val useTwoColumns = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("home_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeroPreservationBanner(
                    badgeText = stringResource(R.string.home_provisional_badge),
                    title = stringResource(R.string.home_hero_title),
                    subtitle = stringResource(R.string.home_hero_subtitle)
                )
            }

            item {
                DemonstrativeModeNoticeCard()
            }

            item {
                IntroNoticeCard(
                    title = stringResource(R.string.home_intro_title),
                    body = stringResource(R.string.home_intro_body)
                )
            }

            item {
                OfflineArchiveStatusCard(
                    stats = uiState.archiveStats,
                    syncState = uiState.syncState,
                    onManualSync = onManualSync
                )
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.home_modules_section_title),
                    subtitle = stringResource(R.string.home_modules_section_subtitle)
                )
            }

            if (useTwoColumns) {
                val rows = plannedModules.chunked(2)
                items(
                    items = rows,
                    key = { row -> row.joinToString("_") { it.id } }
                ) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { module ->
                            PlannedModuleCard(
                                icon = module.icon,
                                title = stringResource(module.titleRes),
                                description = stringResource(module.descRes),
                                statusLabel = statusLabel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                items(
                    items = plannedModules,
                    key = { it.id }
                ) { module ->
                    PlannedModuleCard(
                        icon = module.icon,
                        title = stringResource(module.titleRes),
                        description = stringResource(module.descRes),
                        statusLabel = statusLabel
                    )
                }
            }

            item {
                ExpandableStageDetailsCard(
                    title = stringResource(R.string.home_stage_details_title),
                    body = stringResource(R.string.home_stage_details_body),
                    expanded = uiState.isStageDetailsExpanded,
                    onToggle = onToggleStageDetails
                )
            }
        }
    }
}
