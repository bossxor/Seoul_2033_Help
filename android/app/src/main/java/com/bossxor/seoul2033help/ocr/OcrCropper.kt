package com.bossxor.seoul2033help.ocr

import android.graphics.Bitmap

object OcrCropper {
    /** Height in px of bottom crop; fraction clamped to 0.25..0.7. */
    fun cropHeight(fullHeight: Int, bottomFraction: Float): Int =
        (fullHeight * bottomFraction.coerceIn(0.25f, 0.7f)).toInt().coerceAtLeast(1)

    fun cropBottom(full: Bitmap, bottomFraction: Float): Bitmap {
        val h = cropHeight(full.height, bottomFraction).coerceAtMost(full.height)
        return Bitmap.createBitmap(full, 0, full.height - h, full.width, h)
    }
}
