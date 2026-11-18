package com.example.data.local.seed

import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.SeedMetadataDao
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/**
 * Coordena a detecção de versão e hash SHA-256 do arquivo de seed em comparação com
 * a metadata persistida em Room via [SeedMetadataDao].
 *
 * Esta camada é estritamente somente leitura: não altera verbetes, favoritos nem metadata.
 */
interface DictionarySeedChangeDetector {
    suspend fun inspectSeedStatus(
        currentVersion: Int = DictionarySeedSpec.CURRENT_SEED_VERSION
    ): AppResult<SeedInspectionResult>
}

@Singleton
class DefaultDictionarySeedChangeDetector @Inject constructor(
    private val assetSource: DictionarySeedAssetSource,
    private val sha256Calculator: SeedSha256Calculator,
    private val seedMetadataDao: SeedMetadataDao,
    private val versionComparator: SeedVersionComparator,
    private val dispatchers: DispatcherProvider
) : DictionarySeedChangeDetector {

    override suspend fun inspectSeedStatus(currentVersion: Int): AppResult<SeedInspectionResult> =
        withContext(dispatchers.io) {
            try {
                val currentHash = sha256Calculator.calculateFromSource(assetSource)
                val localMetadata = seedMetadataDao.getMetadataOnce()
                val inspection = versionComparator.compare(
                    currentVersion = currentVersion,
                    currentHash = currentHash,
                    metadata = localMetadata
                )
                AppResult.Success(inspection)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: EmptySeedFileException) {
                AppResult.Error(
                    message = e.message ?: DEFAULT_EMPTY_SEED_ERROR,
                    cause = e
                )
            } catch (e: IOException) {
                AppResult.Error(
                    message = e.localizedMessage?.takeIf { it.isNotBlank() } ?: DEFAULT_IO_ERROR,
                    cause = e
                )
            } catch (e: IllegalArgumentException) {
                AppResult.Error(
                    message = e.localizedMessage?.takeIf { it.isNotBlank() } ?: DEFAULT_INVALID_STATE_ERROR,
                    cause = e
                )
            } catch (e: Exception) {
                AppResult.Error(
                    message = e.localizedMessage?.takeIf { it.isNotBlank() } ?: DEFAULT_UNEXPECTED_ERROR,
                    cause = e
                )
            }
        }

    companion object {
        private const val DEFAULT_EMPTY_SEED_ERROR =
            "Arquivo de seed do dicionário está vazio e não pôde ser verificado."
        private const val DEFAULT_IO_ERROR =
            "Não foi possível ler o arquivo de seed do dicionário para verificação de hash."
        private const val DEFAULT_INVALID_STATE_ERROR =
            "Parâmetros inválidos ao verificar a versão do seed."
        private const val DEFAULT_UNEXPECTED_ERROR =
            "Falha inesperada ao inspecionar alterações no seed do dicionário."
    }
}
