package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.data.local.entity.IndigenousLandEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IndigenousLandDao {

    @Query(
        """
        SELECT * FROM indigenous_lands
        ORDER BY COALESCE(terrai_nom, '') COLLATE NOCASE ASC,
                 COALESCE(terrai_cod, '') COLLATE NOCASE ASC,
                 id ASC
        """
    )
    fun observeAll(): Flow<List<IndigenousLandEntity>>

    @Query(
        """
        SELECT * FROM indigenous_lands
        WHERE (
            :query = ''
            OR terrai_nom LIKE '%' || :query || '%'
            OR etnia_nome LIKE '%' || :query || '%'
            OR municipio_ LIKE '%' || :query || '%'
            OR terrai_cod LIKE '%' || :query || '%'
        )
        AND (
            :stateUf IS NULL
            OR :stateUf = ''
            OR UPPER(uf_sigla) = UPPER(:stateUf)
            OR states_uf LIKE '%"' || UPPER(:stateUf) || '"%'
        )
        AND (:phase IS NULL OR :phase = '' OR fase_ti = :phase)
        AND (:modality IS NULL OR :modality = '' OR modalidade = :modality)
        ORDER BY COALESCE(terrai_nom, '') COLLATE NOCASE ASC,
                 COALESCE(terrai_cod, '') COLLATE NOCASE ASC,
                 id ASC
        """
    )
    fun observeTerritories(
        query: String = "",
        stateUf: String? = null,
        phase: String? = null,
        modality: String? = null
    ): Flow<List<IndigenousLandEntity>>

    @Query(
        """
        SELECT * FROM indigenous_lands
        WHERE UPPER(uf_sigla) = UPPER(:stateUf)
           OR states_uf LIKE '%"' || UPPER(:stateUf) || '"%'
        ORDER BY COALESCE(terrai_nom, '') COLLATE NOCASE ASC,
                 COALESCE(terrai_cod, '') COLLATE NOCASE ASC,
                 id ASC
        """
    )
    fun observeByStateUf(stateUf: String): Flow<List<IndigenousLandEntity>>

    @Query("SELECT * FROM indigenous_lands WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<IndigenousLandEntity?>

    @Query("SELECT * FROM indigenous_lands WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: String): IndigenousLandEntity?

    @Query("SELECT * FROM indigenous_lands WHERE terrai_cod = :territoryCode LIMIT 1")
    suspend fun findByTerritoryCode(territoryCode: String): IndigenousLandEntity?

    @Query("SELECT COUNT(*) FROM indigenous_lands")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM indigenous_lands")
    suspend fun countOnce(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(entity: IndigenousLandEntity)

    @Upsert
    suspend fun upsert(entity: IndigenousLandEntity)

    @Upsert
    suspend fun upsertAll(entities: List<IndigenousLandEntity>)

    @Update
    suspend fun update(entity: IndigenousLandEntity): Int

    @Query("DELETE FROM indigenous_lands WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM indigenous_lands")
    suspend fun deleteAll()
}
