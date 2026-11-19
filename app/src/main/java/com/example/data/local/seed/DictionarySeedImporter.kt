package com.example.data.local.seed

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.withTransaction
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.DictionaryDao
import com.example.data.local.database.ParintintinDatabase
import com.example.data.local.entity.DictionaryEntryEntity
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface DictionarySeedImporter {
    suspend fun importIfEmpty(): AppResult<Int>
}

@Singleton
class AssetDictionarySeedImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi,
    private val database: ParintintinDatabase,
    private val dictionaryDao: DictionaryDao,
    private val dispatchers: DispatcherProvider
) : DictionarySeedImporter {

    private val importMutex = Mutex()
    private val jsonParser by lazy { DictionarySeedJsonParser(moshi) }

    override suspend fun importIfEmpty(): AppResult<Int> =
        importIfEmptyFromStream { context.assets.open(SEED_ASSET_FILE_NAME) }

    @VisibleForTesting
    internal suspend fun importIfEmptyFromStream(
        openSeedStream: () -> InputStream
    ): AppResult<Int> = withContext(dispatchers.io) {
        importMutex.withLock {
            try {
                val importedCount = database.withTransaction {
                    if (dictionaryDao.countOnce() > 0) {
                        return@withTransaction 0
                    }
                    val entities = parseAndValidateEntries(openSeedStream)
                    if (entities.isEmpty()) {
                        return@withTransaction 0
                    }
                    dictionaryDao.upsertAll(entities)
                    entities.size
                }
                AppResult.Success(importedCount)
            } catch (ce: CancellationException) {
                throw ce
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

    private fun parseAndValidateEntries(
        openSeedStream: () -> InputStream
    ): List<DictionaryEntryEntity> =
        openSeedStream().use { inputStream ->
            jsonParser.parseAndValidateEntities(inputStream)
        }

    companion object {
        const val SEED_ASSET_FILE_NAME = DictionarySeedSpec.SEED_ASSET_FILE_NAME

        private const val DEFAULT_INVALID_FORMAT_ERROR =
            "Arquivo JSON de dados iniciais do dicionário possui formato inválido."
        private const val DEFAULT_VALIDATION_ERROR =
            "Arquivo de dados iniciais do dicionário contém campos inválidos."
        private const val DEFAULT_IO_ERROR =
            "Não foi possível ler o arquivo local de dados iniciais do dicionário."
        private const val DEFAULT_UNEXPECTED_ERROR =
            "Falha inesperada ao importar dados iniciais do dicionário."
    }
}
