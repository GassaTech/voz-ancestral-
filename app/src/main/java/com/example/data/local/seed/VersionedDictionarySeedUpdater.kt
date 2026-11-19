package com.example.data.local.seed

import androidx.room.withTransaction
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.DictionaryDao
import com.example.data.local.dao.SeedMetadataDao
import com.example.data.local.dao.SyncMetadataDao
import com.example.data.local.database.ParintintinDatabase
import com.example.data.local.entity.SeedMetadataEntity
import com.example.data.mapper.toEntity
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import java.io.EOFException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun interface SeedClock {
    fun nowEpochMillis(): Long
}

enum class SeedUpdateOutcomeStatus {
    APPLIED_INITIAL,
    UPDATED,
    ALREADY_UP_TO_DATE,
    BLOCKED_BY_OFFICIAL_SYNC,
    BLOCKED_INCONSISTENT_VERSION
}

data class SeedUpdateOutcome(
    val status: SeedUpdateOutcomeStatus,
    val entriesProcessedCount: Int,
    val appliedSeedVersion: Int?,
    val appliedSeedHash: String?,
    val currentSeedVersion: Int,
    val currentSeedHash: String,
    val message: String
)

interface DictionarySeedUpdater {
    suspend fun updateIfNeeded(
        currentVersion: Int = DictionarySeedSpec.CURRENT_SEED_VERSION
    ): AppResult<SeedUpdateOutcome>
}

/**
 * Mecanismo controlado e transacional de atualização dos verbetes demonstrativos
 * provenientes de `dictionary_seed.json`.
 *
 * - Valida integralmente o JSON e calcula o hash SHA-256 sobre os mesmos bytes antes da transação.
 * - Protege o acervo oficial caso já exista sincronização remota bem-sucedida (`lastSuccessfulSyncEpochMillis != null`).
 * - Preserva marcações `is_favorite`, registros não-DEMO, artigos culturais e IDs antigos ausentes.
 * - Atualiza verbetes e `seed_metadata` atomicamente dentro de `database.withTransaction`.
 */
@Singleton
class VersionedDictionarySeedUpdater @Inject constructor(
    private val assetSource: DictionarySeedAssetSource,
    private val sha256Calculator: SeedSha256Calculator,
    private val versionComparator: SeedVersionComparator,
    private val jsonParser: DictionarySeedJsonParser,
    private val database: ParintintinDatabase,
    private val dictionaryDao: DictionaryDao,
    private val seedMetadataDao: SeedMetadataDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val dispatchers: DispatcherProvider,
    private val clock: SeedClock = SeedClock(System::currentTimeMillis)
) : DictionarySeedUpdater {

    private val updateMutex = Mutex()

    override suspend fun updateIfNeeded(currentVersion: Int): AppResult<SeedUpdateOutcome> =
        withContext(dispatchers.io) {
            updateMutex.withLock {
                try {
                    val seedBytes = assetSource.openSeedStream().use { it.readBytes() }
                    val currentHash = sha256Calculator.calculate(seedBytes)
                    val dtos = jsonParser.parseAndValidateDtos(seedBytes)
                    if (dtos.isEmpty()) {
                        return@withLock AppResult.Error(
                            message = DEFAULT_EMPTY_ARRAY_ERROR,
                            cause = IllegalArgumentException(DEFAULT_EMPTY_ARRAY_ERROR)
                        )
                    }

                    val outcome = database.withTransaction {
                        val syncMetadata = syncMetadataDao.getMetadataOnce()
                        val localMetadata = seedMetadataDao.getMetadataOnce()

                        if (syncMetadata?.lastSuccessfulSyncEpochMillis != null) {
                            return@withTransaction SeedUpdateOutcome(
                                status = SeedUpdateOutcomeStatus.BLOCKED_BY_OFFICIAL_SYNC,
                                entriesProcessedCount = 0,
                                appliedSeedVersion = localMetadata?.appliedSeedVersion,
                                appliedSeedHash = localMetadata?.appliedSeedHash,
                                currentSeedVersion = currentVersion,
                                currentSeedHash = currentHash,
                                message = "Atualização demonstrativa bloqueada porque já existe sincronização oficial bem-sucedida."
                            )
                        }

                        val inspection = versionComparator.compare(
                            currentVersion = currentVersion,
                            currentHash = currentHash,
                            metadata = localMetadata
                        )

                        when (inspection.status) {
                            SeedChangeStatus.SEED_UP_TO_DATE ->
                                SeedUpdateOutcome(
                                    status = SeedUpdateOutcomeStatus.ALREADY_UP_TO_DATE,
                                    entriesProcessedCount = 0,
                                    appliedSeedVersion = localMetadata?.appliedSeedVersion,
                                    appliedSeedHash = localMetadata?.appliedSeedHash,
                                    currentSeedVersion = currentVersion,
                                    currentSeedHash = currentHash,
                                    message = inspection.diagnosticMessage
                                )

                            SeedChangeStatus.INCONSISTENT_VERSION ->
                                SeedUpdateOutcome(
                                    status = SeedUpdateOutcomeStatus.BLOCKED_INCONSISTENT_VERSION,
                                    entriesProcessedCount = 0,
                                    appliedSeedVersion = localMetadata?.appliedSeedVersion,
                                    appliedSeedHash = localMetadata?.appliedSeedHash,
                                    currentSeedVersion = currentVersion,
                                    currentSeedHash = currentHash,
                                    message = inspection.diagnosticMessage
                                )

                            SeedChangeStatus.SEED_NOT_APPLIED,
                            SeedChangeStatus.SEED_UPDATED -> {
                                val seedIds = dtos.map { it.id }
                                val existingSeedIds = findExistingIds(seedIds)
                                val isInitialApplication = existingSeedIds.isEmpty() && (
                                    localMetadata == null ||
                                        (
                                            localMetadata.appliedSeedVersion == SeedMetadataEntity.INITIAL_SEED_VERSION &&
                                                localMetadata.appliedSeedIdsJson == null
                                            )
                                    )

                                val favoriteIds = dictionaryDao.getFavoriteIds().toHashSet()
                                val entities = dtos.map { dto ->
                                    dto.toEntity(isFavorite = favoriteIds.contains(dto.id))
                                }

                                dictionaryDao.upsertAll(entities)

                                val updatedAtMillis = clock.nowEpochMillis()
                                val updatedMetadata = SeedMetadataEntity(
                                    id = SeedMetadataEntity.SINGLETON_ID,
                                    appliedSeedVersion = currentVersion,
                                    appliedSeedHash = currentHash,
                                    appliedSeedIdsJson = Json.encodeToString(seedIds),
                                    updatedAtEpochMillis = updatedAtMillis
                                )
                                seedMetadataDao.upsert(updatedMetadata)

                                val outcomeStatus = if (isInitialApplication) {
                                    SeedUpdateOutcomeStatus.APPLIED_INITIAL
                                } else {
                                    SeedUpdateOutcomeStatus.UPDATED
                                }

                                SeedUpdateOutcome(
                                    status = outcomeStatus,
                                    entriesProcessedCount = entities.size,
                                    appliedSeedVersion = updatedMetadata.appliedSeedVersion,
                                    appliedSeedHash = updatedMetadata.appliedSeedHash,
                                    currentSeedVersion = currentVersion,
                                    currentSeedHash = currentHash,
                                    message = if (isInitialApplication) {
                                        "Seed demonstrativo aplicado inicialmente (${entities.size} verbetes)."
                                    } else {
                                        "Seed demonstrativo atualizado para a versão $currentVersion (${entities.size} verbetes)."
                                    }
                                )
                            }
                        }
                    }

                    AppResult.Success(outcome)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: EmptySeedFileException) {
                    AppResult.Error(
                        message = e.message ?: DEFAULT_EMPTY_FILE_ERROR,
                        cause = e
                    )
                } catch (e: JsonDataException) {
                    AppResult.Error(
                        message = e.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: DEFAULT_INVALID_FORMAT_ERROR,
                        cause = e
                    )
                } catch (e: JsonEncodingException) {
                    AppResult.Error(
                        message = e.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: DEFAULT_INVALID_FORMAT_ERROR,
                        cause = e
                    )
                } catch (e: EOFException) {
                    AppResult.Error(
                        message = e.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: DEFAULT_INVALID_FORMAT_ERROR,
                        cause = e
                    )
                } catch (e: IllegalArgumentException) {
                    AppResult.Error(
                        message = e.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: DEFAULT_VALIDATION_ERROR,
                        cause = e
                    )
                } catch (e: IOException) {
                    AppResult.Error(
                        message = e.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: DEFAULT_IO_ERROR,
                        cause = e
                    )
                } catch (e: Exception) {
                    AppResult.Error(
                        message = e.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: DEFAULT_UNEXPECTED_ERROR,
                        cause = e
                    )
                }
            }
        }

    private suspend fun findExistingIds(ids: List<String>): Set<String> {
        if (ids.isEmpty()) return emptySet()
        return ids.chunked(SQLITE_IN_CLAUSE_CHUNK_SIZE)
            .flatMap { chunk -> dictionaryDao.getExistingIds(chunk) }
            .toHashSet()
    }

    companion object {
        private const val SQLITE_IN_CLAUSE_CHUNK_SIZE = 500
        private const val DEFAULT_EMPTY_FILE_ERROR =
            "Arquivo de seed do dicionário está vazio e não pôde ser aplicado."
        private const val DEFAULT_EMPTY_ARRAY_ERROR =
            "Arquivo de seed do dicionário não contém verbetes válidos."
        private const val DEFAULT_INVALID_FORMAT_ERROR =
            "Arquivo JSON de dados iniciais do dicionário possui formato inválido."
        private const val DEFAULT_VALIDATION_ERROR =
            "Arquivo de dados iniciais do dicionário contém campos inválidos."
        private const val DEFAULT_IO_ERROR =
            "Não foi possível ler o arquivo local de dados iniciais do dicionário."
        private const val DEFAULT_UNEXPECTED_ERROR =
            "Falha inesperada ao atualizar o seed do dicionário."
    }
}
