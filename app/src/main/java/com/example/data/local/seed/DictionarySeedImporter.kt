package com.example.data.local.seed

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.withTransaction
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.DictionaryDao
import com.example.data.local.database.ParintintinDatabase
import com.example.data.local.entity.DictionaryEntryEntity
import com.example.data.mapper.toEntity
import com.example.data.remote.dto.DictionaryEntryDto
import com.example.domain.model.GrammaticalClass
import com.example.domain.model.SemanticCategory
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.JsonReader
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
import okio.buffer
import okio.source

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
    private val entryAdapter by lazy {
        moshi.adapter(DictionaryEntryDto::class.java)
    }

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
    ): List<DictionaryEntryEntity> {
        val dtos = openSeedStream().use { inputStream ->
            JsonReader.of(inputStream.source().buffer()).use { reader ->
                val parsedItems = mutableListOf<DictionaryEntryDto>()
                reader.beginArray()
                var index = 0
                while (reader.hasNext()) {
                    if (reader.peek() == JsonReader.Token.NULL) {
                        throw JsonDataException(
                            "Elemento nulo encontrado no seed do dicionário (índice $index)."
                        )
                    }
                    val dto = entryAdapter.fromJson(reader)
                        ?: throw JsonDataException(
                            "Elemento nulo ou inválido no seed do dicionário (índice $index)."
                        )
                    parsedItems += dto
                    index += 1
                }
                reader.endArray()
                if (reader.peek() != JsonReader.Token.END_DOCUMENT) {
                    throw JsonDataException("Conteúdo inesperado após o array JSON de seed.")
                }
                parsedItems
            }
        }

        val seenIds = HashSet<String>(dtos.size)
        val entities = ArrayList<DictionaryEntryEntity>(dtos.size)
        dtos.forEachIndexed { index, dto ->
            validateEntry(dto = dto, index = index, seenIds = seenIds)
            entities += dto.toEntity(isFavorite = false)
        }
        return entities
    }

    private fun validateEntry(
        dto: DictionaryEntryDto,
        index: Int,
        seenIds: MutableSet<String>
    ) {
        if (dto.id.isBlank()) {
            throw IllegalArgumentException(
                "Verbete de seed no índice $index possui 'id' vazio ou em branco."
            )
        }
        if (!seenIds.add(dto.id)) {
            throw IllegalArgumentException(
                "ID duplicado encontrado no seed do dicionário: '${dto.id}'."
            )
        }
        if (dto.parintintinWord.isBlank()) {
            throw IllegalArgumentException(
                "Verbete '${dto.id}' possui 'parintintin_word' vazio ou em branco."
            )
        }
        if (dto.portugueseTranslation.isBlank()) {
            throw IllegalArgumentException(
                "Verbete '${dto.id}' possui 'portuguese_translation' vazio ou em branco."
            )
        }
        if (VALID_GRAMMATICAL_CLASSES.none { it == dto.grammaticalClass }) {
            throw IllegalArgumentException(
                "Verbete '${dto.id}' possui 'grammatical_class' inválido: '${dto.grammaticalClass}'."
            )
        }
        if (VALID_SEMANTIC_CATEGORIES.none { it == dto.semanticCategory }) {
            throw IllegalArgumentException(
                "Verbete '${dto.id}' possui 'semantic_category' inválido: '${dto.semanticCategory}'."
            )
        }
        if (dto.updatedAtEpochMillis < 0L) {
            throw IllegalArgumentException(
                "Verbete '${dto.id}' possui 'updated_at_epoch_millis' negativo: ${dto.updatedAtEpochMillis}."
            )
        }
    }

    companion object {
        const val SEED_ASSET_FILE_NAME = "dictionary_seed.json"

        private val VALID_GRAMMATICAL_CLASSES = GrammaticalClass.entries.map { it.name }.toSet()
        private val VALID_SEMANTIC_CATEGORIES = SemanticCategory.entries.map { it.name }.toSet()

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
