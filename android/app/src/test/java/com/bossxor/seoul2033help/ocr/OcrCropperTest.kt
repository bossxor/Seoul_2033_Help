package com.bossxor.seoul2033help.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrCropperTest {
    @Test fun fortyPercentOf1000() = assertEquals(400, OcrCropper.cropHeight(1000, 0.4f))
    @Test fun clampsLow() = assertEquals(250, OcrCropper.cropHeight(1000, 0.1f))
    @Test fun clampsHigh() = assertEquals(700, OcrCropper.cropHeight(1000, 0.9f))
    @Test fun neverZero() = assertEquals(1, OcrCropper.cropHeight(1, 0.25f))
}
