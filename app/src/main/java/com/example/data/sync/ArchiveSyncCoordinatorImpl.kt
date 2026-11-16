package com.example.data.sync

import com.example.core.common.AppResult
import com.example.di.ApplicationScope
import com.example.domain.repository.SyncStatusRepository
import com.example.domain.sync.ArchiveSyncCoordinator
import com.example.domain.sync.ArchiveSyncOutcome
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

@Singleton
class ArchiveSyncCoordinatorImpl @Inject constructor(
    private val syncStatusRepository: SyncStatusRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ArchiveSyncCoordinator {

    private sealed interface ExecutionPlan {
        data class Start(val deferred: Deferred<AppResult<Unit>>) : ExecutionPlan
        data class Join(val deferred: Deferred<AppResult<Unit>>) : ExecutionPlan
        data object AlreadyTriggered : ExecutionPlan
    }

    private val syncLock = Any()
    private var activeSync: Deferred<AppResult<Unit>>? = null
    private var launchSyncTriggered: Boolean = false

    override suspend fun requestAutoSync(): ArchiveSyncOutcome {
        val plan = synchronized(syncLock) {
            if (launchSyncTriggered) {
                ExecutionPlan.AlreadyTriggered
            } else {
                launchSyncTriggered = true
                val inFlight = activeSync?.takeIf { !it.isCompleted }
                if (inFlight != null) {
                    ExecutionPlan.Join(inFlight)
                } else {
                    ExecutionPlan.Start(createAndRegisterSyncLocked())
                }
            }
        }

        return executePlan(plan)
    }

    override suspend fun requestManualSync(): ArchiveSyncOutcome {
        val plan = synchronized(syncLock) {
            val inFlight = activeSync?.takeIf { !it.isCompleted }
            if (inFlight != null) {
                ExecutionPlan.Join(inFlight)
            } else {
                ExecutionPlan.Start(createAndRegisterSyncLocked())
            }
        }

        return executePlan(plan)
    }

    private suspend fun executePlan(plan: ExecutionPlan): ArchiveSyncOutcome = when (plan) {
        ExecutionPlan.AlreadyTriggered -> ArchiveSyncOutcome.AlreadyTriggeredForLaunch
        is ExecutionPlan.Join -> ArchiveSyncOutcome.JoinedInFlight(plan.deferred.await())
        is ExecutionPlan.Start -> {
            plan.deferred.start()
            ArchiveSyncOutcome.Executed(plan.deferred.await())
        }
    }

    private fun createAndRegisterSyncLocked(): Deferred<AppResult<Unit>> {
        val deferred = applicationScope.async(start = CoroutineStart.LAZY) {
            executeSyncSafely()
        }
        activeSync = deferred
        deferred.invokeOnCompletion {
            synchronized(syncLock) {
                if (activeSync === deferred) {
                    activeSync = null
                }
            }
        }
        return deferred
    }

    private suspend fun executeSyncSafely(): AppResult<Unit> =
        try {
            syncStatusRepository.syncArchive()
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            AppResult.Error(
                message = e.localizedMessage
                    ?.takeIf { it.isNotBlank() }
                    ?: DEFAULT_UNEXPECTED_SYNC_ERROR,
                cause = e
            )
        }

    companion object {
        private const val DEFAULT_UNEXPECTED_SYNC_ERROR =
            "Erro inesperado ao sincronizar o acervo."
    }
}
