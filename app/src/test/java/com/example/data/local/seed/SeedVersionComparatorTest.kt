package com.example.data.local.seed

import com.example.data.local.entity.SeedMetadataEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedVersionComparatorTest {

    private val comparator = SeedVersionComparator()
    private val sampleHash = "a".repeat(64)
    private val otherHash = "b".repeat(64)

    @Test
    fun `constante CURRENT_SEED_VERSION esta centralizada com valor inicial 2`() {
        assertEquals(2, DictionarySeedSpec.CURRENT_SEED_VERSION)
    }

    @Test
    fun `metadata ausente classifica como SEED_NOT_APPLIED`() {
        val result = comparator.compare(
            currentVersion = DictionarySeedSpec.CURRENT_SEED_VERSION,
            currentHash = sampleHash,
            metadata = null
        )

        assertEquals(SeedChangeStatus.SEED_NOT_APPLIED, result.status)
        assertFalse(result.isInconsistentVersion)
        assertNull(result.appliedSeedVersion)
        assertNull(result.appliedSeedHash)
    }

    @Test
    fun `seed ainda nao aplicado com versao zero e hash nulo classifica como SEED_NOT_APPLIED`() {
        val initialMetadata = SeedMetadataEntity(
            id = SeedMetadataEntity.SINGLETON_ID,
            appliedSeedVersion = SeedMetadataEntity.INITIAL_SEED_VERSION,
            appliedSeedHash = null,
            appliedSeedIdsJson = "[\"DEMO-001\"]",
            updatedAtEpochMillis = 0L
        )

        val result = comparator.compare(
            currentVersion = DictionarySeedSpec.CURRENT_SEED_VERSION,
            currentHash = sampleHash,
            metadata = initialMetadata
        )

        assertEquals(SeedChangeStatus.SEED_NOT_APPLIED, result.status)
        assertFalse(result.isInconsistentVersion)
        assertEquals(0, result.appliedSeedVersion)
        assertNull(result.appliedSeedHash)
        assertEquals("[\"DEMO-001\"]", result.appliedSeedIdsJson)
    }

    @Test
    fun `seed com versao antiga classifica como SEED_NOT_APPLIED mesmo com hash preenchido`() {
        val oldVersionMetadata = SeedMetadataEntity(
            appliedSeedVersion = 1,
            appliedSeedHash = sampleHash,
            updatedAtEpochMillis = 1_700_000_000_000L
        )

        val result = comparator.compare(
            currentVersion = DictionarySeedSpec.CURRENT_SEED_VERSION,
            currentHash = sampleHash,
            metadata = oldVersionMetadata
        )

        assertEquals(SeedChangeStatus.SEED_NOT_APPLIED, result.status)
        assertFalse(result.isInconsistentVersion)
        assertEquals(1, result.appliedSeedVersion)
    }

    @Test
    fun `seed na versao atual com hash nulo ou em branco classifica como SEED_NOT_APPLIED`() {
        val nullHashMetadata = SeedMetadataEntity(
            appliedSeedVersion = DictionarySeedSpec.CURRENT_SEED_VERSION,
            appliedSeedHash = null
        )
        val blankHashMetadata = SeedMetadataEntity(
            appliedSeedVersion = DictionarySeedSpec.CURRENT_SEED_VERSION,
            appliedSeedHash = "   "
        )

        val nullHashResult = comparator.compare(
            currentVersion = DictionarySeedSpec.CURRENT_SEED_VERSION,
            currentHash = sampleHash,
            metadata = nullHashMetadata
        )
        val blankHashResult = comparator.compare(
            currentVersion = DictionarySeedSpec.CURRENT_SEED_VERSION,
            currentHash = sampleHash,
            metadata = blankHashMetadata
        )

        assertEquals(SeedChangeStatus.SEED_NOT_APPLIED, nullHashResult.status)
        assertNull(nullHashResult.appliedSeedHash)
        assertEquals(SeedChangeStatus.SEED_NOT_APPLIED, blankHashResult.status)
        assertNull(blankHashResult.appliedSeedHash)
    }

    @Test
    fun `seed com versao atual e hash diferente classifica como SEED_UPDATED`() {
        val metadata = SeedMetadataEntity(
            appliedSeedVersion = DictionarySeedSpec.CURRENT_SEED_VERSION,
            appliedSeedHash = otherHash,
            appliedSeedIdsJson = "[\"DEMO-001\",\"DEMO-002\"]",
            updatedAtEpochMillis = 1_700_000_100_000L
        )

        val result = comparator.compare(
            currentVersion = DictionarySeedSpec.CURRENT_SEED_VERSION,
            currentHash = sampleHash,
            metadata = metadata
        )

        assertEquals(SeedChangeStatus.SEED_UPDATED, result.status)
        assertFalse(result.isInconsistentVersion)
        assertEquals(DictionarySeedSpec.CURRENT_SEED_VERSION, result.appliedSeedVersion)
        assertEquals(otherHash, result.appliedSeedHash)
        assertEquals(sampleHash, result.currentSeedHash)
    }

    @Test
    fun `seed com versao e hash iguais classifica como SEED_UP_TO_DATE`() {
        val metadata = SeedMetadataEntity(
            appliedSeedVersion = DictionarySeedSpec.CURRENT_SEED_VERSION,
            appliedSeedHash = sampleHash.uppercase(),
            appliedSeedIdsJson = "[\"DEMO-001\"]",
            updatedAtEpochMillis = 1_700_000_200_000L
        )

        val result = comparator.compare(
            currentVersion = DictionarySeedSpec.CURRENT_SEED_VERSION,
            currentHash = sampleHash,
            metadata = metadata
        )

        assertEquals(SeedChangeStatus.SEED_UP_TO_DATE, result.status)
        assertFalse(result.isInconsistentVersion)
        assertEquals(DictionarySeedSpec.CURRENT_SEED_VERSION, result.currentSeedVersion)
        assertEquals(sampleHash, result.currentSeedHash)
    }

    @Test
    fun `versao aplicada superior a versao atual nao classifica como atualizado e sinaliza INCONSISTENT_VERSION`() {
        val futureMetadata = SeedMetadataEntity(
            appliedSeedVersion = DictionarySeedSpec.CURRENT_SEED_VERSION + 1,
            appliedSeedHash = sampleHash,
            updatedAtEpochMillis = 1_700_000_300_000L
        )

        val result = comparator.compare(
            currentVersion = DictionarySeedSpec.CURRENT_SEED_VERSION,
            currentHash = sampleHash,
            metadata = futureMetadata
        )

        assertEquals(SeedChangeStatus.INCONSISTENT_VERSION, result.status)
        assertTrue(result.isInconsistentVersion)
        assertTrue(result.status != SeedChangeStatus.SEED_UP_TO_DATE)
        assertEquals(3, result.appliedSeedVersion)
    }

    @Test
    fun `versao aplicada negativa sinaliza INCONSISTENT_VERSION`() {
        val invalidMetadata = SeedMetadataEntity(
            appliedSeedVersion = -1,
            appliedSeedHash = sampleHash
        )

        val result = comparator.compare(
            currentVersion = DictionarySeedSpec.CURRENT_SEED_VERSION,
            currentHash = sampleHash,
            metadata = invalidMetadata
        )

        assertEquals(SeedChangeStatus.INCONSISTENT_VERSION, result.status)
        assertTrue(result.isInconsistentVersion)
    }
}
