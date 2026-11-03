package com.bossxor.seoul2033help.ocr

import android.graphics.Bitmap

object OcrCropper {
    /** Height in px of bottom crop; fraction clamped to 0.25..0.7. */
    fun cropHeight(fullHeight: Int, bottomFraction: Float): Int =
        (fullHeight * bottomFraction.coerceIn(0.25f, 0.7f)).toInt().coerceAtLeast(1)

    /**
     * 게임 화면 기준: 선택지는 화면 아래쪽에 쌓이고, 그 밑에 이야기 푯말 줄과 아이템 바가 있다.
     * 푯말 줄은 제목 매칭에 쓰므로 읽고, 하단 [BOTTOM_EXCLUDE]의 아이템 바는 뺀다.
     * 위쪽은 [bottomFraction]만큼까지만 읽는다.
     */
    const val BOTTOM_EXCLUDE = 0.10f  // 아이템 바(약 0.90 이후)만 뺀다

    /** 이 비율(화면 높이 기준) 아래의 줄은 선택지가 아니라 `[분류] 제목… - 페이지 -` 푯말 줄 */
    const val FOOTER_START = 0.865f

    fun choiceBand(fullHeight: Int, bottomFraction: Float): Pair<Int, Int> {
        val frac = bottomFraction.coerceIn(0.25f, 0.7f)
        val top = (fullHeight * (1f - frac)).toInt()
        val bottom = (fullHeight * (1f - BOTTOM_EXCLUDE)).toInt().coerceAtLeast(top + 1)
        return top to bottom.coerceAtMost(fullHeight)
    }

    fun cropChoices(full: Bitmap, bottomFraction: Float): Bitmap {
        val (top, bottom) = choiceBand(full.height, bottomFraction)
        return Bitmap.createBitmap(full, 0, top, full.width, (bottom - top).coerceAtLeast(1))
    }
}
