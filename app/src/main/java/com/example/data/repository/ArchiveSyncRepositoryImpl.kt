package com.example.data.repository

import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.SyncMetadataDao
import com.example.data.local.entity.SyncMetadataEntity
import com.example.data.local.seed.DictionarySeedUpdater
import com.example.data.local.seed.SeedUpdateOutcomeStatus
import com.example.data.mapper.toDomainSyncState
import com.example.di.ApplicationScope
import com.example.domain.model.ArchiveStats
import com.example.domain.model.RemoteSyncOutcome
import com.example.domain.model.SyncState
import com.example.domain.repository.DictionaryRepository
import com.example.domain.repository.EncyclopediaRepository
import com.example.domain.repository.SyncStatusRepository
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
    private val syncMetadataDao: SyncMetadataDao,
    private val dictionarySeedUpdater: DictionarySeedUpdater,
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

    private val syncMutex = Mutex()
    private val transientOverride =
        MutableStateFlow<TransientSyncOverride>(TransientSyncOverride.None)
    private var lastAttemptEpochMillisInMemory: Long? = null

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
            metadata.toDomainSyncState(DEFAULT_OFFLINE_REASON)

        TransientSyncOverride.Syncing ->
            SyncState.Syncing

        is TransientSyncOverride.PendingRoomEmission -> {
            val isConfirmedByRoom = metadata != null && (
                metadata.lastAttemptEpochMillis > override.attemptEpochMillis ||
                    (metadata.lastAttemptEpochMillis == override.attemptEpochMillis &&
                        metadata.lastAttemptStatus == override.expectedStatus &&
                        metadata.toDomainSyncState(DEFAULT_OFFLINE_REASON) == override.resolvedState)
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
                metadata.toDomainSyncState(DEFAULT_OFFLINE_REASON)

            TransientSyncOverride.Syncing ->
                SyncState.Syncing

            is TransientSyncOverride.PendingRoomEmission ->
                latest.resolvedState

            is TransientSyncOverride.InMemoryFallback ->
                latest.fallbackState
        }

    override fun observeArchiveStats(): Flow<ArchiveStats> =
        combine(
            dictionaryRepository.observeTotalCount(),
            dictionaryRepository.observeFavoriteCount(),
            encyclopediaRepository.observeTotalCount(),
            encyclopediaRepository.observeBookmarkedCount()
        ) { totalEntries, favoriteEntries, totalArticles, bookmarkedArticles ->
            ArchiveStats(
                totalEntries = totalEntries,
                favoriteEntries = favoriteEntries,
                totalArticles = totalArticles,
                bookmarkedArticles = bookmarkedArticles
            )
        }.flowOn(dispatchers.io)

    override suspend fun syncArchive(): AppResult<Unit> = syncMutex.withLock {
        withContext(dispatchers.io) {
            val previousOverride = transientOverride.value
            val restoreOnCancel = when (previousOverride) {
                is TransientSyncOverride.InMemoryFallback -> previousOverride
                else -> TransientSyncOverride.None
            }
            transientOverride.value = TransientSyncOverride.Syncing

            try {
                runSeedUpdateSafely()
                val entriesResult = dictionaryRepository.syncFromRemote()
                val now = nextAttemptEpochMillis()

                when (entriesResult) {
                    is AppResult.Success -> when (val outcome = entriesResult.data) {
                        is RemoteSyncOutcome.Synced -> {
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
                                    // Na versão 1.0 a enciclopédia não participa da sincronização;
                                    // mantém null em vez de registrar contagem fictícia (ex.: 0).
                                    lastSuccessfulArticlesCount = null,
                                    // Limitação conhecida: versão real do manifesto ainda não integrada.
                                    lastSuccessfulRemoteVersion = MANIFEST_VERSION_UNAVAILABLE
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
                            val previousMetadata = syncMetadataDao.getMetadataOnce()
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

                    is AppResult.Error -> {
                        val reason = entriesResult.message
                            .takeIf { it.isNotBlank() }
                            ?: DEFAULT_OFFLINE_REASON
                        persistOfflineFallback(
                            now = now,
                            reason = reason,
                            restoreOnCancel = restoreOnCancel
                        )
                        AppResult.Error(message = reason, cause = entriesResult.cause)
                    }
                }
            } catch (ce: CancellationException) {
                transientOverride.value = restoreOnCancel
                throw ce
            } catch (e: Exception) {
                val initialReason = e.localizedMessage
                    ?.takeIf { it.isNotBlank() }
                    ?: DEFAULT_OFFLINE_REASON
                handleFailureWithTimestampCheck(
                    initialReason = initialReason,
                    initialError = e,
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

    private suspend fun nextAttemptEpochMillis(): Long {
        val persistedLastAttempt = syncMetadataDao.getMetadataOnce()?.lastAttemptEpochMillis
        val latestReference = listOfNotNull(
            persistedLastAttempt,
            lastAttemptEpochMillisInMemory
        ).maxOrNull()
        val nextTimestamp = computeNextTimestamp(latestReference)
        lastAttemptEpochMillisInMemory = nextTimestamp
        return nextTimestamp
    }

    private suspend fun handleFailureWithTimestampCheck(
        initialReason: String,
        initialError: Exception,
        restoreOnCancel: TransientSyncOverride
    ): AppResult<Unit> {
        val verifiedNow = try {
            nextAttemptEpochMillis()
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
                    lastAttemptEpochMillis = unverifiedNow
                )
            )
            return AppResult.Error(message = combinedReason, cause = readError)
        }

        persistOfflineFallback(
            now = verifiedNow,
            reason = initialReason,
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
        restoreOnCancel: TransientSyncOverride
    ) {
        val offlineState = SyncState.OfflineFallback(
            reason = reason,
            lastAttemptEpochMillis = now
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

    companion object {
        // TODO: Obter versão real de ParintintinApiService.getArchiveManifest() quando integrado.
        internal const val MANIFEST_VERSION_UNAVAILABLE = "manifesto_nao_integrado"
        private const val DEFAULT_OFFLINE_REASON = "Modo offline ativo. Dados locais preservados."
        private const val DEFAULT_METADATA_READ_ERROR_REASON =
            "Falha ao consultar metadados de sincronização no banco local."
    }
}
