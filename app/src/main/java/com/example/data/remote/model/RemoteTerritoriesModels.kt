package com.example.data.remote.model

/**
 * Representação normalizada na camada Data de um registro remoto retornado pela camada 25
 * (Terras Indígenas) do serviço ArcGIS REST IBAMA / SIGAGEO.
 *
 * Todos os campos textuais ausentes ou em branco permanecem `null` (ou listas vazias) sem
 * preenchimento fictício.
 */
data class RemoteIndigenousLand(
    val objectId: Long?,
    val territoryCode: String?,
    val name: String?,
    val rawEthnicity: String?,
    val ethnicities: List<String>,
    val rawMunicipality: String?,
    val municipalities: List<String>,
    val rawStateUf: String?,
    val statesUf: List<String>,
    val surfaceArea: Double?,
    val phase: String?,
    val modality: String?,
    val reStudy: String?,
    val regionalCoordination: String?,
    val borderStrip: String?,
    val adminUnitCode: String?,
    val adminUnitName: String?,
    val adminUnitAcronym: String?,
    val unionDomain: String?,
    val updatedAtRaw: String?,
    val epsg: String?
)

/**
 * Resultado paginado de uma consulta à camada ArcGIS REST.
 */
data class RemoteTerritoriesPage(
    val items: List<RemoteIndigenousLand>,
    val requestedOffset: Int,
    val requestedRecordCount: Int,
    val exceededTransferLimit: Boolean,
    val nextOffset: Int?
) {
    val hasMorePages: Boolean
        get() = exceededTransferLimit && nextOffset != null
}
