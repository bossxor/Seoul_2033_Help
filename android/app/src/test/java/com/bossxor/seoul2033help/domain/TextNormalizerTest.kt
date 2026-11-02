package com.bossxor.seoul2033help.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {
    @Test
    fun stripsSpacesAndPunctuation() {
        assertEquals("나무위로올라간다", TextNormalizer.normalize("나무 위로 올라간다!"))
    }

    @Test
    fun lowercasesAsciiOnlyKeepsHangul() {
        assertEquals("총으로쏜다", TextNormalizer.normalize("총으로 쏜다"))
    }
}
