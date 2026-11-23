package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ArcGisFeatureQueryResponseDto(
    @Json(name = "fields") val fields: List<ArcGisFieldDto>? = null,
    @Json(name = "features") val features: List<ArcGisFeatureDto>? = null,
    @Json(name = "exceededTransferLimit") val exceededTransferLimit: Boolean? = null,
    @Json(name = "error") val error: ArcGisErrorDto? = null
)

@JsonClass(generateAdapter = true)
data class ArcGisFieldDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "alias") val alias: String? = null
)

@JsonClass(generateAdapter = true)
data class ArcGisErrorDto(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "details") val details: List<String?>? = null
)

@JsonClass(generateAdapter = true)
data class ArcGisFeatureDto(
    @Json(name = "attributes") val attributes: IndigenousLandAttributesDto? = null
)

data class IndigenousLandAttributesDto(
    val objectId: Long? = null,
    val terraiCod: String? = null,
    val terraiNom: String? = null,
    val etniaNome: String? = null,
    val municipio: String? = null,
    val ufSigla: String? = null,
    val superficie: Double? = null,
    val faseTi: String? = null,
    val modalidade: String? = null,
    val reestudoT: String? = null,
    val cr: String? = null,
    val faixaFron: String? = null,
    val undadmCod: String? = null,
    val undadmNom: String? = null,
    val undadmSig: String? = null,
    val dominioUn: String? = null,
    val dataAtual: String? = null,
    val epsg: String? = null
)
