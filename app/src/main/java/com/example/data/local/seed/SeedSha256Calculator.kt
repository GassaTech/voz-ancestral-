package com.example.data.local.seed

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exceção lançada quando o arquivo/fluxo de seed está vazio (0 bytes).
 * Por contrato, um arquivo de seed vazio não é válido e não deve gerar hash aplicável.
 */
class EmptySeedFileException(
    message: String = "Arquivo de seed do dicionário está vazio (0 bytes) e não pode gerar hash SHA-256 válido."
) : IllegalStateException(message)

/**
 * Calcula o hash SHA-256 hexadecimal (64 caracteres em minúsculas) sobre os bytes
 * reais do arquivo de seed, sem qualquer normalização textual (como fim de linha,
 * espaços ou codificação).
 */
@Singleton
class SeedSha256Calculator @Inject constructor() {

    fun calculateFromSource(assetSource: DictionarySeedAssetSource): String =
        assetSource.openSeedStream().use { stream ->
            calculate(stream)
        }

    fun calculate(bytes: ByteArray): String =
        ByteArrayInputStream(bytes).use { stream ->
            calculate(stream)
        }

    fun calculate(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance(ALGORITHM_SHA_256)
        val buffer = ByteArray(BUFFER_SIZE_BYTES)
        var totalBytesRead = 0L

        while (true) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead == -1) {
                break
            }
            if (bytesRead > 0) {
                digest.update(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
            }
        }

        if (totalBytesRead == 0L) {
            throw EmptySeedFileException()
        }

        return digest.digest().toLowercaseHex()
    }

    private fun ByteArray.toLowercaseHex(): String {
        val hexChars = CharArray(size * 2)
        forEachIndexed { index, byte ->
            val unsigned = byte.toInt() and 0xFF
            hexChars[index * 2] = HEX_DIGITS[unsigned ushr 4]
            hexChars[index * 2 + 1] = HEX_DIGITS[unsigned and 0x0F]
        }
        return String(hexChars)
    }

    companion object {
        private const val ALGORITHM_SHA_256 = "SHA-256"
        private const val BUFFER_SIZE_BYTES = 8 * 1024
        private val HEX_DIGITS = "0123456789abcdef".toCharArray()
    }
}
