package com.example.data.mapper

import com.example.data.local.entity.IndigenousLandEntity
import com.example.data.remote.model.RemoteIndigenousLand
import com.example.domain.model.IndigenousLand
import java.security.MessageDigest

fun RemoteIndigenousLand.toDomain(): IndigenousLand {
    val normalizedCode = territoryCode.normalizeBlankToNull()
    val normalizedName = name.normalizeBlankToNull()
    val normalizedEthnicity = rawEthnicity.normalizeBlankToNull()
    val normalizedMunicipality = rawMunicipality.normalizeBlankToNull()
    val normalizedStateUf = rawStateUf.normalizeBlankToNull()
    val normalizedPhase = phase.normalizeBlankToNull()
    val normalizedModality = modality.normalizeBlankToNull()
    val normalizedUpdatedAt = updatedAtRaw.normalizeBlankToNull()

    val resolvedEthnicities = ethnicities
        .mapNotNull { it.normalizeBlankToNull() }
        .distinct()
        .ifEmpty { splitDelimitedValues(normalizedEthnicity) }

    val resolvedMunicipalities = municipalities
        .mapNotNull { it.normalizeBlankToNull() }
        .distinct()
        .ifEmpty { splitDelimitedValues(normalizedMunicipality) }

    val resolvedStatesUf = statesUf
        .mapNotNull { it.normalizeBlankToNull()?.uppercase() }
        .distinct()
        .ifEmpty { splitDelimitedValues(normalizedStateUf).map { it.uppercase() }.distinct() }

    return IndigenousLand(
        id = resolveIndigenousLandId(
            territoryCode = normalizedCode,
            objectId = objectId,
            name = normalizedName,
            rawEthnicity = normalizedEthnicity,
            rawMunicipality = normalizedMunicipality,
            rawStateUf = normalizedStateUf,
            phase = normalizedPhase,
            modality = normalizedModality
        ),
        objectId = objectId,
        territoryCode = normalizedCode,
        name = normalizedName,
        rawEthnicity = normalizedEthnicity,
        ethnicities = resolvedEthnicities,
        rawMunicipality = normalizedMunicipality,
        municipalities = resolvedMunicipalities,
        rawStateUf = normalizedStateUf,
        statesUf = resolvedStatesUf,
        surfaceArea = surfaceArea?.takeIf { it.isFinite() && it >= 0.0 },
        phase = normalizedPhase,
        modality = normalizedModality,
        updatedAtRaw = normalizedUpdatedAt
    )
}

fun RemoteIndigenousLand.toEntity(): IndigenousLandEntity =
    toDomain().toEntity()

fun IndigenousLandEntity.toDomain(): IndigenousLand = IndigenousLand(
    id = id,
    objectId = objectId,
    territoryCode = territoryCode,
    name = name,
    rawEthnicity = rawEthnicity,
    ethnicities = ethnicities,
    rawMunicipality = rawMunicipality,
    municipalities = municipalities,
    rawStateUf = rawStateUf,
    statesUf = statesUf,
    surfaceArea = surfaceArea,
    phase = phase,
    modality = modality,
    updatedAtRaw = updatedAtRaw
)

fun IndigenousLand.toEntity(): IndigenousLandEntity {
    val normalizedCode = territoryCode.normalizeBlankToNull()
    val normalizedName = name.normalizeBlankToNull()
    val normalizedEthnicity = rawEthnicity.normalizeBlankToNull()
    val normalizedMunicipality = rawMunicipality.normalizeBlankToNull()
    val normalizedStateUf = rawStateUf.normalizeBlankToNull()
    val normalizedPhase = phase.normalizeBlankToNull()
    val normalizedModality = modality.normalizeBlankToNull()
    val normalizedUpdatedAt = updatedAtRaw.normalizeBlankToNull()

    val resolvedId = id.normalizeBlankToNull() ?: resolveIndigenousLandId(
        territoryCode = normalizedCode,
        objectId = objectId,
        name = normalizedName,
        rawEthnicity = normalizedEthnicity,
        rawMunicipality = normalizedMunicipality,
        rawStateUf = normalizedStateUf,
        phase = normalizedPhase,
        modality = normalizedModality
    )

    return IndigenousLandEntity(
        id = resolvedId,
        objectId = objectId,
        territoryCode = normalizedCode,
        name = normalizedName,
        rawEthnicity = normalizedEthnicity,
        ethnicities = ethnicities
            .mapNotNull { it.normalizeBlankToNull() }
            .distinct()
            .ifEmpty { splitDelimitedValues(normalizedEthnicity) },
        rawMunicipality = normalizedMunicipality,
        municipalities = municipalities
            .mapNotNull { it.normalizeBlankToNull() }
            .distinct()
            .ifEmpty { splitDelimitedValues(normalizedMunicipality) },
        rawStateUf = normalizedStateUf,
        statesUf = statesUf
            .mapNotNull { it.normalizeBlankToNull()?.uppercase() }
            .distinct()
            .ifEmpty { splitDelimitedValues(normalizedStateUf).map { it.uppercase() }.distinct() },
        surfaceArea = surfaceArea?.takeIf { it.isFinite() && it >= 0.0 },
        phase = normalizedPhase,
        modality = normalizedModality,
        updatedAtRaw = normalizedUpdatedAt
    )
}

internal fun resolveIndigenousLandId(
    territoryCode: String?,
    objectId: Long?,
    name: String? = null,
    rawEthnicity: String? = null,
    rawMunicipality: String? = null,
    rawStateUf: String? = null,
    phase: String? = null,
    modality: String? = null
): String {
    val normalizedCode = territoryCode.normalizeBlankToNull()
    if (normalizedCode != null) {
        return "terrai:$normalizedCode"
    }
    if (objectId != null) {
        return "oid:$objectId"
    }
    val fingerprintInput = listOf(
        name.orEmpty(),
        rawEthnicity.orEmpty(),
        rawMunicipality.orEmpty(),
        rawStateUf.orEmpty(),
        phase.orEmpty(),
        modality.orEmpty()
    ).joinToString(separator = "|")
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(fingerprintInput.toByteArray(Charsets.UTF_8))
        .take(12)
        .joinToString(separator = "") { "%02x".format(it) }
    return "anon:$digest"
}

private fun String?.normalizeBlankToNull(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() }
