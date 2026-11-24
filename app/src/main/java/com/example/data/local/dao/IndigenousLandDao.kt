package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.data.local.entity.IndigenousLandEntity
import com.example.domain.model.TerritorySortOption
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

    @RawQuery(observedEntities = [IndigenousLandEntity::class])
    fun observeTerritoriesRaw(query: SupportSQLiteQuery): Flow<List<IndigenousLandEntity>>

    fun observeTerritories(
        query: String = "",
        stateUf: String? = null,
        phase: String? = null,
        modality: String? = null,
        sortOption: TerritorySortOption = TerritorySortOption()
    ): Flow<List<IndigenousLandEntity>> =
        observeTerritoriesRaw(
            IndigenousLandQueryBuilder.build(
                query = query,
                stateUf = stateUf,
                phase = phase,
                modality = modality,
                sortOption = sortOption
            )
        )

    fun observeByStateUf(stateUf: String): Flow<List<IndigenousLandEntity>> =
        observeTerritories(stateUf = stateUf)

    @Query("SELECT DISTINCT states_uf FROM indigenous_lands WHERE states_uf != '[]'")
    fun observeDistinctStatesUfJson(): Flow<List<String>>

    @Query(
        """
        SELECT DISTINCT TRIM(uf_sigla)
        FROM indigenous_lands
        WHERE uf_sigla IS NOT NULL AND TRIM(uf_sigla) != ''
        """
    )
    fun observeDistinctRawStateUfs(): Flow<List<String>>

    @Query(
        """
        SELECT DISTINCT TRIM(fase_ti)
        FROM indigenous_lands
        WHERE fase_ti IS NOT NULL AND TRIM(fase_ti) != ''
        ORDER BY TRIM(fase_ti) COLLATE NOCASE ASC
        """
    )
    fun observeDistinctPhases(): Flow<List<String>>

    @Query(
        """
        SELECT DISTINCT TRIM(modalidade)
        FROM indigenous_lands
        WHERE modalidade IS NOT NULL AND TRIM(modalidade) != ''
        ORDER BY TRIM(modalidade) COLLATE NOCASE ASC
        """
    )
    fun observeDistinctModalities(): Flow<List<String>>

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
