package com.example.data.local.seed

import com.example.data.local.entity.SeedMetadataEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Classificação do estado do seed local em comparação com `DictionarySeedSpec.CURRENT_SEED_VERSION`
 * e o hash SHA-256 atual do arquivo empacotado.
 */
enum class SeedChangeStatus {
    /**
     * Não existe metadata válida registrada ou a versão aplicada localmente é inferior à versão atual.
     */
    SEED_NOT_APPLIED,

    /**
     * A versão aplicada coincide com a versão atual, porém o hash SHA-256 do arquivo mudou.
     */
    SEED_UPDATED,

    /**
     * A versão aplicada e o hash SHA-256 coincidem integralmente com o arquivo atual.
     */
    SEED_UP_TO_DATE,

    /**
     * Estado inconsistente detectado na metadata local (por exemplo, versão aplicada superior
     * à versão atual suportada pelo aplicativo ou versão negativa inválida).
     * Não deve ser classificado silenciosamente como atualizado.
     */
    INCONSISTENT_VERSION
}

data class SeedInspectionResult(
    val status: SeedChangeStatus,
    val currentSeedVersion: Int,
    val currentSeedHash: String,
    val appliedSeedVersion: Int?,
    val appliedSeedHash: String?,
    val appliedSeedIdsJson: String?,
    val isInconsistentVersion: Boolean = status == SeedChangeStatus.INCONSISTENT_VERSION,
    val diagnosticMessage: String
)

/**
 * Comparador puro entre a versão/hash atuais do seed empacotado e a metadata persistida em Room.
 * Não possui dependência de `Context`, Room ou E/S.
 */
@Singleton
class SeedVersionComparator @Inject constructor() {

    fun compare(
        currentVersion: Int,
        currentHash: String,
        metadata: SeedMetadataEntity?
    ): SeedInspectionResult {
        require(currentVersion > 0) {
            "A versão atual do seed deve ser positiva, mas foi $currentVersion."
        }
        require(currentHash.isNotBlank()) {
            "O hash SHA-256 atual do seed não pode estar vazio."
        }

        val normalizedCurrentHash = currentHash.lowercase()

        if (metadata == null) {
            return SeedInspectionResult(
                status = SeedChangeStatus.SEED_NOT_APPLIED,
                currentSeedVersion = currentVersion,
                currentSeedHash = normalizedCurrentHash,
                appliedSeedVersion = null,
                appliedSeedHash = null,
                appliedSeedIdsJson = null,
                diagnosticMessage = "Nenhuma metadata de seed encontrada no banco local."
            )
        }

        val appliedVersion = metadata.appliedSeedVersion
        val appliedHash = metadata.appliedSeedHash?.trim()?.takeIf { it.isNotEmpty() }

        return when {
            appliedVersion < 0 -> SeedInspectionResult(
                status = SeedChangeStatus.INCONSISTENT_VERSION,
                currentSeedVersion = currentVersion,
                currentSeedHash = normalizedCurrentHash,
                appliedSeedVersion = appliedVersion,
                appliedSeedHash = appliedHash,
                appliedSeedIdsJson = metadata.appliedSeedIdsJson,
                diagnosticMessage = "Versão aplicada inválida ($appliedVersion) registrada na metadata do seed."
            )

            appliedVersion > currentVersion -> SeedInspectionResult(
                status = SeedChangeStatus.INCONSISTENT_VERSION,
                currentSeedVersion = currentVersion,
                currentSeedHash = normalizedCurrentHash,
                appliedSeedVersion = appliedVersion,
                appliedSeedHash = appliedHash,
                appliedSeedIdsJson = metadata.appliedSeedIdsJson,
                diagnosticMessage = "Versão aplicada ($appliedVersion) é superior à versão atual empacotada ($currentVersion)."
            )

            appliedVersion < currentVersion -> SeedInspectionResult(
                status = SeedChangeStatus.SEED_NOT_APPLIED,
                currentSeedVersion = currentVersion,
                currentSeedHash = normalizedCurrentHash,
                appliedSeedVersion = appliedVersion,
                appliedSeedHash = appliedHash,
                appliedSeedIdsJson = metadata.appliedSeedIdsJson,
                diagnosticMessage = "Versão aplicada ($appliedVersion) é anterior à versão atual ($currentVersion)."
            )

            appliedHash == null -> SeedInspectionResult(
                status = SeedChangeStatus.SEED_NOT_APPLIED,
                currentSeedVersion = currentVersion,
                currentSeedHash = normalizedCurrentHash,
                appliedSeedVersion = appliedVersion,
                appliedSeedHash = null,
                appliedSeedIdsJson = metadata.appliedSeedIdsJson,
                diagnosticMessage = "Metadata na versão atual ($appliedVersion) não possui hash SHA-256 registrado."
            )

            !appliedHash.equals(normalizedCurrentHash, ignoreCase = true) -> SeedInspectionResult(
                status = SeedChangeStatus.SEED_UPDATED,
                currentSeedVersion = currentVersion,
                currentSeedHash = normalizedCurrentHash,
                appliedSeedVersion = appliedVersion,
                appliedSeedHash = appliedHash,
                appliedSeedIdsJson = metadata.appliedSeedIdsJson,
                diagnosticMessage = "Hash SHA-256 do arquivo de seed foi alterado na versão $currentVersion."
            )

            else -> SeedInspectionResult(
                status = SeedChangeStatus.SEED_UP_TO_DATE,
                currentSeedVersion = currentVersion,
                currentSeedHash = normalizedCurrentHash,
                appliedSeedVersion = appliedVersion,
                appliedSeedHash = appliedHash,
                appliedSeedIdsJson = metadata.appliedSeedIdsJson,
                diagnosticMessage = "Seed local está atualizado na versão $currentVersion."
            )
        }
    }
}
