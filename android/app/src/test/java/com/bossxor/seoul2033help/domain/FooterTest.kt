package com.bossxor.seoul2033help.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FooterTest {
    @Test fun tagTitlePrefixAndPage() =
        assertEquals(Footer("이야기", "무지개", 169), Footer.parse("이야기 무지개… - 169 -"))

    @Test fun pageOnly() = assertEquals(Footer(null, null, 7), Footer.parse("- 7 -"))

    @Test fun titleWithoutTag() = assertEquals(Footer(null, "철물점", null), Footer.parse("철물점"))

    @Test fun blank() = assertNull(Footer.parse("  "))
}
