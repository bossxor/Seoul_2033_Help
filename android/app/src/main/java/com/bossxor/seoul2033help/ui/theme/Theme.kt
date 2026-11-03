package com.bossxor.seoul2033help.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 한강 노을 슬레이트: 어둡지만 칙칙하지 않게 푸른 기운을 두고, 포인트색은 채도를 올렸다. */
object Seoul2033Colors {
    val BgTop = Color(0xFF2B3759)
    val Bg = Color(0xFF1D2640)
    val Surface = Color(0xFF29345A)
    val SurfaceHigh = Color(0xFF34416B)
    val Text = Color(0xFFF6F2EA)
    val TextMuted = Color(0xFFB4BED6)
    val Recommend = Color(0xFFFFC247)
    val RecommendSoft = Color(0x66FFC247)
    val RecommendBright = Color(0xFFFFF1C9)
    val BorderNormal = Color(0xFF45548A)
    val Chance = Color(0xFFFFA45B)
    val Condition = Color(0xFF6EC6FF)
    val Danger = Color(0xFFFF7B7B)
    val Ok = Color(0xFF5FD6A0)

    val BgGradient = Brush.verticalGradient(listOf(BgTop, Bg))
    private val storyAccents = listOf(Recommend, Condition, Ok, Danger, Color(0xFFB69CFF), Color(0xFFFF8FC8))

    /** 스토리 이름마다 고정된 포인트색 (칩·목록 구분용). */
    fun storyColor(story: String): Color = storyAccents[(story.hashCode() and 0x7fffffff) % storyAccents.size]
}

private val scheme = darkColorScheme(
    primary = Seoul2033Colors.Recommend,
    onPrimary = Seoul2033Colors.Bg,
    background = Seoul2033Colors.Bg,
    onBackground = Seoul2033Colors.Text,
    surface = Seoul2033Colors.Surface,
    onSurface = Seoul2033Colors.Text,
    surfaceVariant = Seoul2033Colors.SurfaceHigh,
    onSurfaceVariant = Seoul2033Colors.TextMuted,
    secondaryContainer = Seoul2033Colors.BorderNormal,
    outline = Seoul2033Colors.BorderNormal,
    error = Seoul2033Colors.Danger,
    tertiary = Seoul2033Colors.Ok
)

private val typography = Typography(
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun Seoul2033Theme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = scheme, typography = typography) {
        // Surface sets LocalContentColor so un-colored Text is readable on the dark bg
        Surface(color = Seoul2033Colors.Bg, content = content)
    }
