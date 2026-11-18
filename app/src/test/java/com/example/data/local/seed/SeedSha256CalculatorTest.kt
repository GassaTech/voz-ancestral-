package com.example.data.local.seed

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SeedSha256CalculatorTest {

    private val calculator = SeedSha256Calculator()

    @Test
    fun `calcula hash SHA-256 conhecido para conteudo fixo`() {
        val abcHash = calculator.calculate("abc".toByteArray(StandardCharsets.US_ASCII))
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            abcHash
        )
        assertEquals(64, abcHash.length)

        val multiBlockHash = calculator.calculate(
            "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".toByteArray(StandardCharsets.US_ASCII)
        )
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            multiBlockHash
        )
    }

    @Test
    fun `mesmo conteudo produz sempre o mesmo hash inclusive acima do tamanho do buffer`() {
        val repeatedPayload = """[{"id":"DEMO-001","parintintin_word":"Riolume"}]"""
            .repeat(500)
            .toByteArray(StandardCharsets.UTF_8)

        val firstHash = calculator.calculate(repeatedPayload)
        val secondHash = calculator.calculateFromSource { ByteArrayInputStream(repeatedPayload) }

        assertEquals(firstHash, secondHash)
        assertTrue(firstHash.matches(Regex("^[0-9a-f]{64}$")))
    }

    @Test
    fun `conteudos diferentes ou com quebra de linha distinta produzem hashes diferentes sem normalizacao`() {
        val lfBytes = "{\n  \"id\": \"DEMO-001\"\n}".toByteArray(StandardCharsets.UTF_8)
        val crlfBytes = "{\r\n  \"id\": \"DEMO-001\"\r\n}".toByteArray(StandardCharsets.UTF_8)
        val modifiedBytes = "{\n  \"id\": \"DEMO-002\"\n}".toByteArray(StandardCharsets.UTF_8)

        val lfHash = calculator.calculate(lfBytes)
        val crlfHash = calculator.calculate(crlfBytes)
        val modifiedHash = calculator.calculate(modifiedBytes)

        assertNotEquals(lfHash, crlfHash)
        assertNotEquals(lfHash, modifiedHash)
        assertNotEquals(crlfHash, modifiedHash)
    }

    @Test
    fun `arquivo vazio lanca EmptySeedFileException conforme contrato`() {
        try {
            calculator.calculate(ByteArray(0))
            fail("Era esperado EmptySeedFileException para array vazio.")
        } catch (_: EmptySeedFileException) {
            // Contrato esperado
        }

        try {
            calculator.calculateFromSource { ByteArrayInputStream(ByteArray(0)) }
            fail("Era esperado EmptySeedFileException para stream vazio.")
        } catch (_: EmptySeedFileException) {
            // Contrato esperado
        }
    }

    @Test
    fun `leitura invalida propaga IOException e fecha o stream`() {
        var streamClosed = false
        val failingStream = object : InputStream() {
            override fun read(): Int = throw IOException("Erro simulado de leitura")

            override fun read(b: ByteArray, off: Int, len: Int): Int =
                throw IOException("Erro simulado de leitura em bloco")

            override fun close() {
                streamClosed = true
            }
        }

        try {
            calculator.calculateFromSource { failingStream }
            fail("Era esperado propagar IOException.")
        } catch (expected: IOException) {
            assertEquals("Erro simulado de leitura em bloco", expected.message)
        }

        assertTrue(streamClosed)
    }
}
