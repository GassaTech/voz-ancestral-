package com.example.data.repository

import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.SyncMetadataDao
import com.example.data.local.entity.SyncMetadataEntity
import com.example.domain.model.ArchiveStats
import com.example.domain.model.SyncState
import com.example.domain.repository.DictionaryRepository
import com.example.domain.repository.EncyclopediaRepository
import com.example.domain.repository.SyncStatusRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class ArchiveSyncRepositoryImpl @Inject constructor(
    private val dictionaryRepository: DictionaryRepository,
    private val encyclopediaRepository: EncyclopediaRepository,
    private val syncMetadataDao: SyncMetadataDao,
    private val dispatchers: DispatcherProvider
) : SyncStatusRepository {

    private val syncMutex = Mutex()
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

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
            val previousState = _syncState.value
            _syncState.value = SyncState.Syncing

            try {
                val (entriesResult, articlesResult) = coroutineScope {
                    val entriesDeferred = async { dictionaryRepository.syncFromRemote() }
                    val articlesDeferred = async { encyclopediaRepository.syncFromRemote() }
                    entriesDeferred.await() to articlesDeferred.await()
                }
                val now = System.currentTimeMillis()

                when {
                    entriesResult is AppResult.Success && articlesResult is AppResult.Success -> {
                        syncMetadataDao.upsert(
                            SyncMetadataEntity(
                                lastSyncEpochMillis = now,
                                // Limitação conhecida: versão real do manifesto ainda não integrada.
                                remoteVersion = MANIFEST_VERSION_UNAVAILABLE,
                                lastStatus = STATUS_SYNCED
                            )
                        )
                        _syncState.value = SyncState.Synced(
                            lastSyncedEpochMillis = now,
                            entriesCount = entriesResult.data,
                            articlesCount = articlesResult.data
                        )
                        AppResult.Success(Unit)
                    }

                    else -> {
                        val reason = buildFailureReason(entriesResult, articlesResult)
                        persistOfflineFallback(now = now, reason = reason)
                        AppResult.Error(reason)
                    }
                }
            } catch (ce: CancellationException) {
                _syncState.value = previousState
                throw ce
            } catch (e: Exception) {
                val now = System.currentTimeMillis()
                val reason = e.localizedMessage ?: DEFAULT_OFFLINE_REASON
                try {
                    persistOfflineFallback(now = now, reason = reason)
                } catch (ce: CancellationException) {
                    _syncState.value = previousState
                    throw ce
                } catch (_: Exception) {
                    // Mesmo se o registro no banco de metadados falhar, não mantém estado Syncing.
                    _syncState.value = SyncState.OfflineFallback(
                        reason = reason,
                        lastAttemptEpochMillis = now
                    )
                }
                AppResult.Error(message = reason, cause = e)
            }
        }
    }

    private suspend fun persistOfflineFallback(now: Long, reason: String) {
        syncMetadataDao.upsert(
            SyncMetadataEntity(
                lastSyncEpochMillis = now,
                // Limitação conhecida: versão real do manifesto ainda não integrada.
                remoteVersion = MANIFEST_VERSION_UNAVAILABLE,
                lastStatus = STATUS_OFFLINE_FALLBACK
            )
        )
        _syncState.value = SyncState.OfflineFallback(
            reason = reason,
            lastAttemptEpochMillis = now
        )
    }

    private fun buildFailureReason(
        entriesResult: AppResult<Int>,
        articlesResult: AppResult<Int>
    ): String {
        val errors = listOfNotNull(
            (entriesResult as? AppResult.Error)?.message,
            (articlesResult as? AppResult.Error)?.message
        ).distinct()
        return if (errors.isEmpty()) {
            DEFAULT_OFFLINE_REASON
        } else {
            errors.joinToString(" | ")
        }
    }

    companion object {
        // TODO: Obter versão real de ParintintinApiService.getArchiveManifest() quando integrado.
        internal const val MANIFEST_VERSION_UNAVAILABLE = "manifesto_nao_integrado"
        internal const val STATUS_SYNCED = "SYNCED"
        internal const val STATUS_OFFLINE_FALLBACK = "OFFLINE_FALLBACK"
        private const val DEFAULT_OFFLINE_REASON = "Modo offline ativo. Dados locais preservados."
    }
}
