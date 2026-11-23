package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "indigenous_lands",
    indices = [
        Index(value = ["terrai_cod"], unique = true),
        Index(value = ["terrai_nom"]),
        Index(value = ["uf_sigla"]),
        Index(value = ["fase_ti"]),
        Index(value = ["modalidade"])
    ]
)
data class IndigenousLandEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "objectid")
    val objectId: Long?,
    @ColumnInfo(name = "terrai_cod")
    val territoryCode: String?,
    @ColumnInfo(name = "terrai_nom")
    val name: String?,
    @ColumnInfo(name = "etnia_nome")
    val rawEthnicity: String?,
    @ColumnInfo(name = "ethnicities")
    val ethnicities: List<String>,
    @ColumnInfo(name = "municipio_")
    val rawMunicipality: String?,
    @ColumnInfo(name = "municipalities")
    val municipalities: List<String>,
    @ColumnInfo(name = "uf_sigla")
    val rawStateUf: String?,
    @ColumnInfo(name = "states_uf")
    val statesUf: List<String>,
    @ColumnInfo(name = "superficie")
    val surfaceArea: Double?,
    @ColumnInfo(name = "fase_ti")
    val phase: String?,
    @ColumnInfo(name = "modalidade")
    val modality: String?,
    @ColumnInfo(name = "data_atual")
    val updatedAtRaw: String?
)
