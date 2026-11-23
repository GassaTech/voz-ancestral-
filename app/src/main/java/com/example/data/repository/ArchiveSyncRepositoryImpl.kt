package com.example.data.repository

import com.example.core.common.AppResult
import com.example.core.common.ApplicationScope
import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.SyncMetadataDao
import com.example.data.local.entity.SyncMetadataEntity
import com.example.data.local.seed.DictionarySeedUpdater
import com.example.data.local.seed.SeedUpdateOutcomeStatus
import com.example.data.mapper.toDomainSyncState
import com.example.domain.connectivity.NetworkConnectivityObserver
import com.example.domain.model.ArchiveStats
import com.example.domain.model.RemoteSyncOutcome
import com.example.domain.model.SyncFailureType
import com.example.domain.model.SyncState
import com.example.domain.repository.DictionaryRepository
import com.example.domain.repository.EncyclopediaRepository
import com.example.domain.repository.IndigenousLandRepository
import com.example.domain.repository.SyncStatusRepository
import com.example.domain.sync.SyncFailureClassifier
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class ArchiveSyncRepositoryImpl @Inject constructor(
    private val dictionaryRepository: DictionaryRepository,
    private val encyclopediaRepository: EncyclopediaRepository,
    private val indigenousLandRepository: IndigenousLandRepository,
    private val syncMetadataDao: SyncMetadataDao,
    private val dictionarySeedUpdater: DictionarySeedUpdater,
    private val networkConnectivityObserver: NetworkConnectivityObserver,
    private val dispatchers: DispatcherProvider,
    @ApplicationScope private val applicationScope: CoroutineScope
) : SyncStatusRepository {

    private sealed interface TransientSyncOverride {
        data object None : TransientSyncOverride
        data object Syncing : TransientSyncOverride
        data class PendingRoomEmission(
            val attemptEpochMillis: Long,
            val expectedStatus: String,
            val resolvedState: SyncState
        ) : TransientSyncOverride

        data class InMemoryFallback(
            val fallbackState: SyncState.OfflineFallback
        ) : TransientSyncOverride
    }

    private data class ClassifiedFailure(
        val attemptEpochMillis: Long,
        val failureType: SyncFailureType
    )

    private data class AttemptSnapshot(
        val nextEpochMillis: Long,
        val persistedMetadata: SyncMetadataEntity?
    )

    private val syncMutex = Mutex()
    private val transientOverride =
        MutableStateFlow<TransientSyncOverride>(TransientSyncOverride.None)
    private var lastAttemptEpochMillisInMemory: Long? = null

    @Volatile
    private var lastClassifiedFailure: ClassifiedFailure? = null

    override val syncState: StateFlow<SyncState> = combine(
        transientOverride,
        syncMetadataDao.observeMetadata()
    ) { override, metadata ->
        resolveEffectiveState(override = override, metadata = metadata)
    }
        .flowOn(dispatchers.io)
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = SyncState.Loading
        )

    private fun resolveEffectiveState(
        override: TransientSyncOverride,
        metadata: SyncMetadataEntity?
    ): SyncState = when (override) {
        TransientSyncOverride.None ->
            metadata.toDomainSyncState(
                defaultOfflineReason = DEFAULT_OFFLINE_REASON,
                offlineFailureType = resolveFailureTypeFor(metadata)
            )

        TransientSyncOverride.Syncing ->
            SyncState.Syncing

        is TransientSyncOverride.PendingRoomEmission -> {
            val mappedState = metadata.toDomainSyncState(
                defaultOfflineReason = DEFAULT_OFFLINE_REASON,
                offlineFailureType = resolveFailureTypeFor(metadata)
            )
            val isConfirmedByRoom = metadata != null && (
                metadata.lastAttemptEpochMillis > override.attemptEpochMillis ||
                    (metadata.lastAttemptEpochMillis == override.attemptEpochMillis &&
                        metadata.lastAttemptStatus == override.expectedStatus &&
                        mappedState == override.resolvedState)
                )
            if (isConfirmedByRoom) {
                transientOverride.compareAndSet(override, TransientSyncOverride.None)
                resolveLatestActiveOverride(metadata)
            } else {
                override.resolvedState
            }
        }

        is TransientSyncOverride.InMemoryFallback ->
            override.fallbackState
    }

    private fun resolveLatestActiveOverride(metadata: SyncMetadataEntity?): SyncState =
        when (val latest = transientOverride.value) {
            TransientSyncOverride.None ->
                metadata.toDomainSyncState(
                    defaultOfflineReason = DEFAULT_OFFLINE_REASON,
                    offlineFailureType = resolveFailureTypeFor(metadata)
                )

            TransientSyncOverride.Syncing ->
                SyncState.Syncing

            is TransientSyncOverride.PendingRoomEmission ->
                latest.resolvedState

            is TransientSyncOverride.InMemoryFallback ->
                latest.fallbackState
        }

    private fun resolveFailureTypeFor(metadata: SyncMetadataEntity?): SyncFailureType {
        if (metadata == null || metadata.lastAttemptStatus != SyncMetadataEntity.STATUS_OFFLINE_FALLBACK) {
            return SyncFailureType.NO_INTERNET
        }
        val classified = lastClassifiedFailure
        return if (classified != null && classified.attemptEpochMillis == metadata.lastAttemptEpochMillis) {
            classified.failureType
        } else {
            classifyCurrentConnectivity()
        }
    }

    private fun classifyCurrentConnectivity(): SyncFailureType =
        SyncFailureClassifier.classify(networkConnectivityObserver.connectivityState.value)

    private fun currentKnownSuccessfulSyncEpochMillis(): Long? = when (val current = syncState.value) {
        is SyncState.Synced -> current.lastSyncedEpochMillis
        is SyncState.RemoteEmpty -> current.lastSyncedEpochMillis
        is SyncState.OfflineFallback -> current.lastSuccessfulSyncEpochMillis
        SyncState.Loading,
        SyncState.Idle,
        SyncState.Syncing -> null
    }

    override fun observeArchiveStats(): Flow<ArchiveStats> =
        combine(
            dictionaryRepository.observeTotalCount(),
            dictionaryRepository.observeFavoriteCount(),
            encyclopediaRepository.observeTotalCount(),
            encyclopediaRepository.observeBookmarkedCount(),
            indigenousLandRepository.observeTotalCount()
        ) { totalEntries, favoriteEntries, totalArticles, bookmarkedArticles, totalTerritories ->
            ArchiveStats(
                totalEntries = totalEntries,
                favoriteEntries = favoriteEntries,
                totalArticles = totalArticles,
                bookmarkedArticles = bookmarkedArticles,
                totalTerritories = totalTerritories
            )
        }.flowOn(dispatchers.io)

    override suspend fun syncArchive(): AppResult<Unit> = syncMutex.withLock {
        withContext(dispatchers.io) {
            val previousSuccessfulSyncEpochMillis = currentKnownSuccessfulSyncEpochMillis()
            val previousOverride = transientOverride.value
            val restoreOnCancel = when (previousOverride) {
                is TransientSyncOverride.InMemoryFallback -> previousOverride
                else -> TransientSyncOverride.None
            }
            transientOverride.value = TransientSyncOverride.Syncing

            try {
                runSeedUpdateSafely()
                val territoriesResult = indigenousLandRepository.syncFromRemote()

                when (territoriesResult) {
                    is AppResult.Success -> {
                        val attemptSnapshot = nextAttemptSnapshot()
                        val now = attemptSnapshot.nextEpochMillis
                        val previousMetadata = attemptSnapshot.persistedMetadata

                        when (val outcome = territoriesResult.data) {
                            is RemoteSyncOutcome.Synced -> {
                                lastClassifiedFailure = null
                                val syncedState = SyncState.Synced(
                                    lastSyncedEpochMillis = now,
                                    entriesCount = outcome.entriesCount,
                                    articlesCount = null
                                )
                                syncMetadataDao.upsert(
                                    SyncMetadataEntity(
                                        id = SyncMetadataEntity.SINGLETON_ID,
                                        lastAttemptEpochMillis = now,
                                        lastAttemptStatus = SyncMetadataEntity.STATUS_SYNCED,
                                        lastFailureReason = null,
                                        lastSuccessfulSyncEpochMillis = now,
                                        lastSuccessfulEntriesCount = outcome.entriesCount,
                                        lastSuccessfulArticlesCount = null,
                                        lastSuccessfulRemoteVersion = IBAMA_TERRITORIES_REMOTE_VERSION
                                    )
                                )
                                transientOverride.value = TransientSyncOverride.PendingRoomEmission(
                                    attemptEpochMillis = now,
                                    expectedStatus = SyncMetadataEntity.STATUS_SYNCED,
                                    resolvedState = syncedState
                                )
                                AppResult.Success(Unit)
                            }

                            RemoteSyncOutcome.Empty -> {
                                lastClassifiedFailure = null
                                syncMetadataDao.recordRemoteEmptyAttempt(attemptEpochMillis = now)
                                val remoteEmptyState = SyncState.RemoteEmpty(
                                    lastAttemptEpochMillis = now,
                                    lastSyncedEpochMillis = previousMetadata?.lastSuccessfulSyncEpochMillis,
                                    entriesCount = previousMetadata?.lastSuccessfulEntriesCount,
                                    articlesCount = previousMetadata?.lastSuccessfulArticlesCount
                                )
                                transientOverride.value = TransientSyncOverride.PendingRoomEmission(
                                    attemptEpochMillis = now,
                                    expectedStatus = SyncMetadataEntity.STATUS_REMOTE_EMPTY,
                                    resolvedState = remoteEmptyState
                                )
                                AppResult.Success(Unit)
                            }
                        }
                    }

                    is AppResult.Error -> recordRemoteError(
                        error = territoriesResult,
                        restoreOnCancel = restoreOnCancel
                    )
                }
            } catch (ce: CancellationException) {
                transientOverride.value = restoreOnCancel
                throw ce
            } catch (e: Exception) {
                val failureType = classifyCurrentConnectivity()
                val initialReason = e.localizedMessage
                    ?.takeIf { it.isNotBlank() }
                    ?: DEFAULT_OFFLINE_REASON
                handleFailureWithTimestampCheck(
                    initialReason = initialReason,
                    initialError = e,
                    failureType = failureType,
                    fallbackSuccessfulSyncEpochMillis = previousSuccessfulSyncEpochMillis,
                    restoreOnCancel = restoreOnCancel
                )
            }
        }
    }

    private suspend fun runSeedUpdateSafely() {
        try {
            when (val result = dictionarySeedUpdater.updateIfNeeded()) {
                is AppResult.Success -> when (result.data.status) {
                    SeedUpdateOutcomeStatus.APPLIED_INITIAL,
                    SeedUpdateOutcomeStatus.UPDATED,
                    SeedUpdateOutcomeStatus.ALREADY_UP_TO_DATE,
                    SeedUpdateOutcomeStatus.BLOCKED_BY_OFFICIAL_SYNC,
                    SeedUpdateOutcomeStatus.BLOCKED_INCONSISTENT_VERSION -> Unit
                }

                is AppResult.Error -> {
                    // Falha local na atualização do Seed não deve ser convertida em falha de rede
                    // nem impedir a tentativa de sincronização remota.
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            // Exceção comum isolada na etapa local do Seed não deve impedir a sincronização remota.
        }
    }

    private suspend fun nextAttemptSnapshot(): AttemptSnapshot {
        val persistedMetadata = syncMetadataDao.getMetadataOnce()
        val persistedLastAttempt = persistedMetadata?.lastAttemptEpochMillis
        val latestReference = listOfNotNull(
            persistedLastAttempt,
            lastAttemptEpochMillisInMemory
        ).maxOrNull()
        val nextTimestamp = computeNextTimestamp(latestReference)
        lastAttemptEpochMillisInMemory = nextTimestamp
        return AttemptSnapshot(
            nextEpochMillis = nextTimestamp,
            persistedMetadata = persistedMetadata
        )
    }

    private suspend fun handleFailureWithTimestampCheck(
        initialReason: String,
        initialError: Exception,
        failureType: SyncFailureType,
        fallbackSuccessfulSyncEpochMillis: Long?,
        restoreOnCancel: TransientSyncOverride
    ): AppResult<Unit> {
        val verifiedSnapshot = try {
            nextAttemptSnapshot()
        } catch (ce: CancellationException) {
            transientOverride.value = restoreOnCancel
            throw ce
        } catch (readError: Exception) {
            // Não foi possível consultar o último timestamp persistido no Room;
            // registra apenas fallback em memória sem afirmar monotonicidade com o banco.
            val unverifiedNow = computeNextTimestamp(lastAttemptEpochMillisInMemory)
            lastAttemptEpochMillisInMemory = unverifiedNow
            val readErrorReason = readError.localizedMessage
                ?.takeIf { it.isNotBlank() }
                ?: DEFAULT_METADATA_READ_ERROR_REASON
            val combinedReason = if (readError === initialError || readErrorReason == initialReason) {
                initialReason
            } else {
                "$initialReason | $readErrorReason"
            }
            transientOverride.value = TransientSyncOverride.InMemoryFallback(
                SyncState.OfflineFallback(
                    reason = combinedReason,
                    lastAttemptEpochMillis = unverifiedNow,
                    failureType = failureType,
                    lastSuccessfulSyncEpochMillis = fallbackSuccessfulSyncEpochMillis
                )
            )
            return AppResult.Error(message = combinedReason, cause = readError)
        }

        lastClassifiedFailure = ClassifiedFailure(
            attemptEpochMillis = verifiedSnapshot.nextEpochMillis,
            failureType = failureType
        )
        persistOfflineFallback(
            now = verifiedSnapshot.nextEpochMillis,
            reason = initialReason,
            failureType = failureType,
            lastSuccessfulSyncEpochMillis = verifiedSnapshot.persistedMetadata
                ?.lastSuccessfulSyncEpochMillis
                ?: fallbackSuccessfulSyncEpochMillis,
            restoreOnCancel = restoreOnCancel
        )
        return AppResult.Error(message = initialReason, cause = initialError)
    }

    private fun computeNextTimestamp(latestReference: Long?): Long {
        val systemNow = System.currentTimeMillis()
        return if (latestReference != null && systemNow <= latestReference) {
            latestReference + 1L
        } else {
            systemNow
        }
    }

    private suspend fun persistOfflineFallback(
        now: Long,
        reason: String,
        failureType: SyncFailureType,
        lastSuccessfulSyncEpochMillis: Long?,
        restoreOnCancel: TransientSyncOverride
    ) {
        val offlineState = SyncState.OfflineFallback(
            reason = reason,
            lastAttemptEpochMillis = now,
            failureType = failureType,
            lastSuccessfulSyncEpochMillis = lastSuccessfulSyncEpochMillis
        )
        try {
            syncMetadataDao.recordAttemptFailure(
                attemptEpochMillis = now,
                failureReason = reason
            )
            transientOverride.value = TransientSyncOverride.PendingRoomEmission(
                attemptEpochMillis = now,
                expectedStatus = SyncMetadataEntity.STATUS_OFFLINE_FALLBACK,
                resolvedState = offlineState
            )
        } catch (ce: CancellationException) {
            transientOverride.value = restoreOnCancel
            throw ce
        } catch (_: Exception) {
            // Se o registro no Room falhar, mantém o erro em memória sem parecer Syncing ou Synced.
            transientOverride.value = TransientSyncOverride.InMemoryFallback(offlineState)
        }
    }

    private suspend fun recordRemoteError(
        error: AppResult.Error,
        restoreOnCancel: TransientSyncOverride
    ): AppResult.Error {
        val failureType = classifyCurrentConnectivity()
        val attemptSnapshot = nextAttemptSnapshot()
        val now = attemptSnapshot.nextEpochMillis
        val reason = error.message
            .takeIf { it.isNotBlank() }
            ?: DEFAULT_OFFLINE_REASON
        lastClassifiedFailure = ClassifiedFailure(
            attemptEpochMillis = now,
            failureType = failureType
        )
        persistOfflineFallback(
            now = now,
            reason = reason,
            failureType = failureType,
            lastSuccessfulSyncEpochMillis = attemptSnapshot.persistedMetadata
                ?.lastSuccessfulSyncEpochMillis,
            restoreOnCancel = restoreOnCancel
        )
        return AppResult.Error(
            message = reason,
            cause = error.cause,
            remoteErrorKind = error.remoteErrorKind
        )
    }

    companion object {
        internal const val IBAMA_TERRITORIES_REMOTE_VERSION = "IBAMA/SIGAGEO Camada 25"
        private const val DEFAULT_OFFLINE_REASON = "Modo offline ativo. Dados locais preservados."
        private const val DEFAULT_METADATA_READ_ERROR_REASON =
            "Falha ao consultar metadados de sincronização no banco local."
    }
}
