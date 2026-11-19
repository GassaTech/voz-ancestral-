package com.example.data.mapper

import com.example.data.local.entity.CulturalArticleEntity
import com.example.data.local.entity.DictionaryEntryEntity
import com.example.data.local.entity.SyncMetadataEntity
import com.example.data.remote.dto.CulturalArticleDto
import com.example.data.remote.dto.DictionaryEntryDto
import com.example.domain.model.CulturalArticle
import com.example.domain.model.CulturalTopic
import com.example.domain.model.DictionaryEntry
import com.example.domain.model.GrammaticalClass
import com.example.domain.model.SemanticCategory
import com.example.domain.model.SyncState

fun DictionaryEntryEntity.toDomain(): DictionaryEntry = DictionaryEntry(
    id = id,
    parintintinWord = parintintinWord,
    phoneticIpa = phoneticIpa,
    portugueseTranslation = portugueseTranslation,
    grammaticalClass = runCatching { GrammaticalClass.valueOf(grammaticalClass) }
        .getOrDefault(GrammaticalClass.NOUN),
    semanticCategory = runCatching { SemanticCategory.valueOf(semanticCategory) }
        .getOrDefault(SemanticCategory.EVERYDAY),
    exampleParintintin = exampleParintintin,
    examplePortuguese = examplePortuguese,
    culturalNote = culturalNote,
    audioUrl = audioUrl,
    isFavorite = isFavorite,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun DictionaryEntryDto.toEntity(isFavorite: Boolean): DictionaryEntryEntity = DictionaryEntryEntity(
    id = id,
    parintintinWord = parintintinWord,
    phoneticIpa = phoneticIpa,
    portugueseTranslation = portugueseTranslation,
    grammaticalClass = grammaticalClass,
    semanticCategory = semanticCategory,
    exampleParintintin = exampleParintintin,
    examplePortuguese = examplePortuguese,
    culturalNote = culturalNote,
    audioUrl = audioUrl,
    isFavorite = isFavorite,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun CulturalArticleEntity.toDomain(): CulturalArticle = CulturalArticle(
    id = id,
    title = title,
    subtitle = subtitle,
    summary = summary,
    bodyParagraphs = bodyParagraphs,
    topic = runCatching { CulturalTopic.valueOf(topic) }
        .getOrDefault(CulturalTopic.HISTORY),
    readingTimeMinutes = readingTimeMinutes,
    sourceReference = sourceReference,
    isBookmarked = isBookmarked,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun CulturalArticleDto.toEntity(isBookmarked: Boolean): CulturalArticleEntity = CulturalArticleEntity(
    id = id,
    title = title,
    subtitle = subtitle,
    summary = summary,
    bodyParagraphs = bodyParagraphs,
    topic = topic,
    readingTimeMinutes = readingTimeMinutes,
    sourceReference = sourceReference,
    isBookmarked = isBookmarked,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun SyncMetadataEntity?.toDomainSyncState(defaultOfflineReason: String): SyncState {
    if (this == null) return SyncState.Idle
    return when (lastAttemptStatus) {
        SyncMetadataEntity.STATUS_SYNCED -> {
            val syncedAt = lastSuccessfulSyncEpochMillis
            if (syncedAt != null) {
                SyncState.Synced(
                    lastSyncedEpochMillis = syncedAt,
                    entriesCount = lastSuccessfulEntriesCount,
                    articlesCount = lastSuccessfulArticlesCount
                )
            } else {
                SyncState.Idle
            }
        }

        SyncMetadataEntity.STATUS_REMOTE_EMPTY -> SyncState.RemoteEmpty(
            lastAttemptEpochMillis = lastAttemptEpochMillis,
            lastSyncedEpochMillis = lastSuccessfulSyncEpochMillis,
            entriesCount = lastSuccessfulEntriesCount,
            articlesCount = lastSuccessfulArticlesCount
        )

        SyncMetadataEntity.STATUS_OFFLINE_FALLBACK -> {
            val resolvedReason = lastFailureReason
                ?.takeIf { it.isNotBlank() }
                ?: defaultOfflineReason
            SyncState.OfflineFallback(
                reason = resolvedReason,
                lastAttemptEpochMillis = lastAttemptEpochMillis
            )
        }

        else -> SyncState.Idle
    }
}
