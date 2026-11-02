package com.bossxor.seoul2033help.domain

import com.bossxor.seoul2033help.data.model.Effect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EffectFormatterTest {
    @Test
    fun formatsChanceWithPercent() {
        val e = Effect.Chance(
            successRate = 0.44,
            success = listOf(Effect.Ability("날렵함")),
            failure = listOf(Effect.Resource("체력", -1))
        )
        val s = EffectFormatter.format(e)
        assertTrue(s.contains("44%"))
        assertTrue(s.contains("날렵함"))
        assertTrue(s.contains("체력"))
    }

    @Test
    fun formatsItem() {
        assertEquals("아이템: 상비약", EffectFormatter.format(Effect.Item("상비약")))
    }
}
