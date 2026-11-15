package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CulturalArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EncyclopediaDao {

    @Query(
        """
        SELECT * FROM cultural_articles
        WHERE (:onlyBookmarked = 0 OR is_bookmarked = 1)
          AND (:topic IS NULL OR topic = :topic)
        ORDER BY updated_at_epoch_millis DESC
        """
    )
    fun observeArticles(
        topic: String?,
        onlyBookmarked: Boolean
    ): Flow<List<CulturalArticleEntity>>

    @Query("SELECT * FROM cultural_articles WHERE id = :articleId LIMIT 1")
    fun observeArticleById(articleId: String): Flow<CulturalArticleEntity?>

    @Query("SELECT id FROM cultural_articles WHERE is_bookmarked = 1")
    suspend fun getBookmarkedIds(): List<String>

    @Query("SELECT COUNT(*) FROM cultural_articles")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cultural_articles WHERE is_bookmarked = 1")
    fun observeBookmarkedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cultural_articles")
    suspend fun countOnce(): Int

    @Query("UPDATE cultural_articles SET is_bookmarked = NOT is_bookmarked WHERE id = :articleId")
    suspend fun toggleBookmark(articleId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(articles: List<CulturalArticleEntity>)
}
