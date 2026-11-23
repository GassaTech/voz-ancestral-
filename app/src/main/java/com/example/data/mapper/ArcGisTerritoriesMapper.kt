package com.example.data.mapper

import com.example.data.remote.dto.ArcGisFeatureDto
import com.example.data.remote.dto.ArcGisFeatureQueryResponseDto
import com.example.data.remote.dto.IndigenousLandAttributesDto
import com.example.data.remote.model.RemoteIndigenousLand
import com.example.data.remote.model.RemoteTerritoriesPage

private val MULTI_VALUE_DELIMITERS = Regex("[,;/|]+")

fun ArcGisFeatureQueryResponseDto.toRemoteTerritoriesPage(
    requestedOffset: Int,
    requestedRecordCount: Int
): RemoteTerritoriesPage {
    val rawFeatures = features.orEmpty()
    val items = rawFeatures.mapNotNull { it.toRemoteIndigenousLandOrNull() }
    val exceeded = exceededTransferLimit == true
    val nextOffset = if (exceeded && rawFeatures.isNotEmpty()) {
        requestedOffset + rawFeatures.size
    } else {
        null
    }
    return RemoteTerritoriesPage(
        items = items,
        requestedOffset = requestedOffset,
        requestedRecordCount = requestedRecordCount,
        exceededTransferLimit = exceeded,
        nextOffset = nextOffset
    )
}

fun ArcGisFeatureDto.toRemoteIndigenousLandOrNull(): RemoteIndigenousLand? =
    attributes?.toRemoteIndigenousLand()

fun IndigenousLandAttributesDto.toRemoteIndigenousLand(): RemoteIndigenousLand {
    val normalizedEthnicity = etniaNome.normalizeBlankToNull()
    val normalizedMunicipality = municipio.normalizeBlankToNull()
    val normalizedState = ufSigla.normalizeBlankToNull()

    return RemoteIndigenousLand(
        objectId = objectId,
        territoryCode = terraiCod.normalizeBlankToNull(),
        name = terraiNom.normalizeBlankToNull(),
        rawEthnicity = normalizedEthnicity,
        ethnicities = splitDelimitedValues(normalizedEthnicity),
        rawMunicipality = normalizedMunicipality,
        municipalities = splitDelimitedValues(normalizedMunicipality),
        rawStateUf = normalizedState,
        statesUf = splitDelimitedValues(normalizedState).map { it.uppercase() }.distinct(),
        surfaceArea = superficie?.takeIf { it.isFinite() && it >= 0.0 },
        phase = faseTi.normalizeBlankToNull(),
        modality = modalidade.normalizeBlankToNull(),
        reStudy = reestudoT.normalizeBlankToNull(),
        regionalCoordination = cr.normalizeBlankToNull(),
        borderStrip = faixaFron.normalizeBlankToNull(),
        adminUnitCode = undadmCod.normalizeBlankToNull(),
        adminUnitName = undadmNom.normalizeBlankToNull(),
        adminUnitAcronym = undadmSig.normalizeBlankToNull(),
        unionDomain = dominioUn.normalizeBlankToNull(),
        updatedAtRaw = dataAtual.normalizeBlankToNull(),
        epsg = epsg.normalizeBlankToNull()
    )
}

internal fun splitDelimitedValues(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.split(MULTI_VALUE_DELIMITERS)
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .toList()
}

private fun String?.normalizeBlankToNull(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() }
