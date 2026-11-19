package com.example.data.local.seed

import com.example.data.local.entity.DictionaryEntryEntity
import com.example.data.mapper.toEntity
import com.example.data.remote.dto.DictionaryEntryDto
import com.example.domain.model.GrammaticalClass
import com.example.domain.model.SemanticCategory
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import okio.buffer
import okio.source

/**
 * Parser e validador compartilhado de `dictionary_seed.json`.
 * Garante contrato único e estrito entre a importação inicial e a atualização versionada.
 */
@Singleton
class DictionarySeedJsonParser @Inject constructor(
    private val moshi: Moshi
) {
    private val entryAdapter by lazy {
        moshi.adapter(DictionaryEntryDto::class.java)
    }

    fun parseAndValidateDtos(bytes: ByteArray): List<DictionaryEntryDto> =
        ByteArrayInputStream(bytes).use { stream ->
            parseAndValidateDtos(stream)
        }

    fun parseAndValidateDtos(inputStream: InputStream): List<DictionaryEntryDto> {
        val dtos = JsonReader.of(inputStream.source().buffer()).use { reader ->
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

        val seenIds = HashSet<String>(dtos.size)
        dtos.forEachIndexed { index, dto ->
            validateEntry(dto = dto, index = index, seenIds = seenIds)
        }
        return dtos
    }

    fun parseAndValidateEntities(
        inputStream: InputStream,
        favoriteIds: Set<String> = emptySet()
    ): List<DictionaryEntryEntity> =
        parseAndValidateDtos(inputStream).map { dto ->
            dto.toEntity(isFavorite = favoriteIds.contains(dto.id))
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
        private val VALID_GRAMMATICAL_CLASSES = GrammaticalClass.entries.map { it.name }.toSet()
        private val VALID_SEMANTIC_CATEGORIES = SemanticCategory.entries.map { it.name }.toSet()
    }
}
