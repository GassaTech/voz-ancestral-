package com.example.data.mapper

import com.example.data.local.entity.CulturalArticleEntity
import com.example.data.local.entity.DictionaryEntryEntity
import com.example.data.local.entity.SyncMetadataEntity
import com.example.data.remote.dto.CulturalArticleDto
import com.example.data.remote.dto.DictionaryEntryDto
import com.example.domain.model.CulturalTopic
import com.example.domain.model.GrammaticalClass
import com.example.domain.model.SemanticCategory
import com.example.domain.model.SyncFailureType
import com.example.domain.model.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveMappersTest {

    @Test
    fun `DictionaryEntryEntity toDomain mapeia campos e aplica fallback para enums desconhecidos`() {
        val valid = DictionaryEntryEntity(
            id = "e1",
            parintintinWord = "Parana",
            phoneticIpa = "/pa.ra.na/",
            portugueseTranslation = "Rio grande",
            grammaticalClass = "NOUN",
            semanticCategory = "NATURE",
            exampleParintintin = "Parana oku",
            examplePortuguese = "O rio corre",
            culturalNote = "Nota",
            audioUrl = "https://example.invalid/audio.mp3",
            isFavorite = true,
            updatedAtEpochMillis = 1_700_000_000_000L
        ).toDomain()

        assertEquals(GrammaticalClass.NOUN, valid.grammaticalClass)
        assertEquals(SemanticCategory.NATURE, valid.semanticCategory)
        assertTrue(valid.isFavorite)

        val unknownEnums = DictionaryEntryEntity(
            id = "e2",
            parintintinWord = "Termo",
            phoneticIpa = "/ter.mo/",
            portugueseTranslation = "Tradução",
            grammaticalClass = "CLASSE_INEXISTENTE",
            semanticCategory = "CATEGORIA_INEXISTENTE",
            exampleParintintin = "Ex",
            examplePortuguese = "Ex pt",
            culturalNote = "",
            audioUrl = null,
            isFavorite = false,
            updatedAtEpochMillis = 1_700_000_000_100L
        ).toDomain()

        assertEquals(GrammaticalClass.NOUN, unknownEnums.grammaticalClass)
        assertEquals(SemanticCategory.EVERYDAY, unknownEnums.semanticCategory)
        assertFalse(unknownEnums.isFavorite)
    }

    @Test
    fun `DictionaryEntryDto toEntity preserva flag de favorito e todos os campos`() {
        val dto = DictionaryEntryDto(
            id = "d1",
            parintintinWord = "Yvy",
            phoneticIpa = "/y.vy/",
            portugueseTranslation = "Terra",
            grammaticalClass = "NOUN",
            semanticCategory = "NATURE",
            exampleParintintin = "Yvy porang",
            examplePortuguese = "Terra boa",
            culturalNote = "Território",
            audioUrl = null,
            updatedAtEpochMillis = 1_700_000_001_000L
        )

        val entity = dto.toEntity(isFavorite = true)

        assertEquals("d1", entity.id)
        assertEquals("Yvy", entity.parintintinWord)
        assertTrue(entity.isFavorite)
        assertNull(entity.audioUrl)
    }

    @Test
    fun `adaptador Moshi gerado pelo KSP esta ativo para DictionaryEntryDto`() {
        val moshi = com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(DictionaryEntryDto::class.java)

        assertTrue(adapter.toString().contains("GeneratedJsonAdapter(DictionaryEntryDto)"))
    }

    @Test
    fun `CulturalArticleEntity e DTO realizam conversao bidirecional e fallback de topico desconhecido`() {
        val dto = CulturalArticleDto(
            id = "a1",
            title = "História",
            subtitle = "Sub",
            summary = "Resumo",
            bodyParagraphs = listOf("P1", "P2"),
            topic = "LINGUISTICS",
            readingTimeMinutes = 4,
            sourceReference = "Fonte",
            updatedAtEpochMillis = 1_700_000_002_000L
        )
        val entity = dto.toEntity(isBookmarked = true)
        assertTrue(entity.isBookmarked)
        assertEquals(CulturalTopic.LINGUISTICS, entity.toDomain().topic)

        val fallback = entity.copy(topic = "TOPICO_DESCONHECIDO").toDomain()
        assertEquals(CulturalTopic.HISTORY, fallback.topic)
    }

    @Test
    fun `SyncMetadataEntity toDomainSyncState cobre null SYNCED sem timestamp REMOTE_EMPTY OFFLINE_FALLBACK e desconhecido`() {
        val nullEntity: SyncMetadataEntity? = null
        assertEquals(SyncState.Idle, nullEntity.toDomainSyncState("Offline"))

        val syncedValid = SyncMetadataEntity(
            lastAttemptEpochMillis = 100L,
            lastAttemptStatus = SyncMetadataEntity.STATUS_SYNCED,
            lastFailureReason = null,
            lastSuccessfulSyncEpochMillis = 100L,
            lastSuccessfulEntriesCount = 20,
            lastSuccessfulArticlesCount = null,
            lastSuccessfulRemoteVersion = "1.0.0"
        ).toDomainSyncState("Offline")
        assertEquals(
            SyncState.Synced(lastSyncedEpochMillis = 100L, territoriesCount = 20),
            syncedValid
        )

        val syncedMissingEpoch = SyncMetadataEntity(
            lastAttemptEpochMillis = 100L,
            lastAttemptStatus = SyncMetadataEntity.STATUS_SYNCED,
            lastFailureReason = null,
            lastSuccessfulSyncEpochMillis = null,
            lastSuccessfulEntriesCount = 20,
            lastSuccessfulArticlesCount = null,
            lastSuccessfulRemoteVersion = null
        ).toDomainSyncState("Offline")
        assertEquals(SyncState.Idle, syncedMissingEpoch)

        val remoteEmpty = SyncMetadataEntity(
            lastAttemptEpochMillis = 200L,
            lastAttemptStatus = SyncMetadataEntity.STATUS_REMOTE_EMPTY,
            lastFailureReason = null,
            lastSuccessfulSyncEpochMillis = 100L,
            lastSuccessfulEntriesCount = 20,
            lastSuccessfulArticlesCount = null,
            lastSuccessfulRemoteVersion = "1.0.0"
        ).toDomainSyncState("Offline")
        assertEquals(
            SyncState.RemoteEmpty(
                lastAttemptEpochMillis = 200L,
                lastSyncedEpochMillis = 100L,
                territoriesCount = 20
            ),
            remoteEmpty
        )

        val offlineBlankReason = SyncMetadataEntity(
            lastAttemptEpochMillis = 300L,
            lastAttemptStatus = SyncMetadataEntity.STATUS_OFFLINE_FALLBACK,
            lastFailureReason = "   ",
            lastSuccessfulSyncEpochMillis = 100L,
            lastSuccessfulEntriesCount = 20,
            lastSuccessfulArticlesCount = null,
            lastSuccessfulRemoteVersion = "1.0.0"
        ).toDomainSyncState("Sem internet", SyncFailureType.SERVER_UNAVAILABLE)
        assertEquals(
            SyncState.OfflineFallback(
                reason = "Sem internet",
                lastAttemptEpochMillis = 300L,
                failureType = SyncFailureType.SERVER_UNAVAILABLE,
                lastSuccessfulSyncEpochMillis = 100L
            ),
            offlineBlankReason
        )

        val unknownStatus = SyncMetadataEntity(
            lastAttemptEpochMillis = 400L,
            lastAttemptStatus = "OUTRO_STATUS",
            lastFailureReason = null,
            lastSuccessfulSyncEpochMillis = null,
            lastSuccessfulEntriesCount = null,
            lastSuccessfulArticlesCount = null,
            lastSuccessfulRemoteVersion = null
        ).toDomainSyncState("Offline")
        assertEquals(SyncState.Idle, unknownStatus)
    }
}
