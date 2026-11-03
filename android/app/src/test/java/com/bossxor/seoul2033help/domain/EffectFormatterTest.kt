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
        assertEquals("+ 상비약", EffectFormatter.format(Effect.Item("상비약")))
    }

    @Test
    fun formatsResourceSigned() {
        assertEquals("멘탈 -1", EffectFormatter.format(Effect.Resource("멘탈", -1)))
        assertEquals("체력 +1", EffectFormatter.format(Effect.Resource("체력", 1)))
    }

    @Test
    fun formatsStatusRemove() {
        assertEquals("- 수배 중", EffectFormatter.format(Effect.Status("수배 중", -1)))
    }

    @Test
    fun itemLossAndCount() {
        assertEquals("- 음료", EffectFormatter.format(Effect.Item("음료", -1)))
        assertEquals("+ 소시지 ×2", EffectFormatter.format(Effect.Item("소시지", 2)))
    }
}
