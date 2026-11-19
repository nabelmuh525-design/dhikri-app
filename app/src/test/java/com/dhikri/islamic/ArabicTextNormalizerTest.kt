package com.dhikri.islamic

import com.dhikri.islamic.domain.util.ArabicTextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicTextNormalizerTest {

    @Test
    fun normalizeForSearch_stripsDiacriticsAndUnifiesAlef() {
        val uthmani = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"
        val normalized = ArabicTextNormalizer.normalizeForSearch(uthmani)
        assertTrue(normalized.contains("بسم"))
        assertTrue(normalized.contains("الله"))
        assertTrue(normalized.contains("الرحيم"))
    }

    @Test
    fun normalizeForSearch_normalizesTaMarbutaAndAlefMaksura() {
        val raw = "إِلَى الصَّلَاةِ"
        val normalized = ArabicTextNormalizer.normalizeForSearch(raw)
        assertEquals("الي الصلاه", normalized)
    }
}
