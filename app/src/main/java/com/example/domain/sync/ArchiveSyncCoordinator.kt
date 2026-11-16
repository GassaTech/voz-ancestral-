package com.example.domain.sync

import com.example.core.common.AppResult

sealed interface ArchiveSyncOutcome {
    data class Executed(val result: AppResult<Unit>) : ArchiveSyncOutcome
    data class JoinedInFlight(val result: AppResult<Unit>) : ArchiveSyncOutcome
    data object AlreadyTriggeredForLaunch : ArchiveSyncOutcome
}

interface ArchiveSyncCoordinator {
    suspend fun requestAutoSync(): ArchiveSyncOutcome
    suspend fun requestManualSync(): ArchiveSyncOutcome
}
