package com.bossxor.seoul2033help.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UserSettingsTest {
    @Test fun clampsHigh() {
        val s = UserSettings(overlayAlpha = 2f, cropBottomFraction = 0.9f).clamped()
        assertEquals(0.95f, s.overlayAlpha)
        assertEquals(0.7f, s.cropBottomFraction)
    }

    @Test fun clampsLow() {
        val s = UserSettings(overlayAlpha = 0f, cropBottomFraction = 0f).clamped()
        assertEquals(0.4f, s.overlayAlpha)
        assertEquals(0.25f, s.cropBottomFraction)
    }
}
