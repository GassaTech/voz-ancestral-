package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.DictionaryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DictionaryDao {

    @Query(
        """
        SELECT * FROM dictionary_entries
        WHERE (:onlyFavorites = 0 OR is_favorite = 1)
          AND (:category IS NULL OR semantic_category = :category)
          AND (
            :query = ''
            OR parintintin_word LIKE '%' || :query || '%'
            OR portuguese_translation LIKE '%' || :query || '%'
            OR cultural_note LIKE '%' || :query || '%'
          )
        ORDER BY parintintin_word COLLATE NOCASE ASC
        """
    )
    fun observeEntries(
        query: String,
        category: String?,
        onlyFavorites: Boolean
    ): Flow<List<DictionaryEntryEntity>>

    @Query("SELECT * FROM dictionary_entries WHERE id = :entryId LIMIT 1")
    fun observeEntryById(entryId: String): Flow<DictionaryEntryEntity?>

    @Query("SELECT id FROM dictionary_entries WHERE is_favorite = 1")
    suspend fun getFavoriteIds(): List<String>

    @Query("SELECT COUNT(*) FROM dictionary_entries")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM dictionary_entries WHERE is_favorite = 1")
    fun observeFavoriteCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM dictionary_entries")
    suspend fun countOnce(): Int

    @Query("UPDATE dictionary_entries SET is_favorite = NOT is_favorite WHERE id = :entryId")
    suspend fun toggleFavorite(entryId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<DictionaryEntryEntity>)
}
