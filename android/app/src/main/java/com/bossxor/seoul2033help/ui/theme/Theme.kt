package com.bossxor.seoul2033help.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Seoul2033Colors {
    val Bg = Color(0xFF121212)
    val Surface = Color(0xFF1C1C1E)
    val Text = Color(0xFFE8E4DC)
    val TextMuted = Color(0xFF9A958C)
    val Recommend = Color(0xFFE6A817)
    val BorderNormal = Color(0xFF3F3C38)
    val Chance = Color(0xFFD4A574)
    val Danger = Color(0xFFC45C4A)
    val Ok = Color(0xFF7A9E7E)
}

private val scheme = darkColorScheme(
    primary = Seoul2033Colors.Recommend,
    onPrimary = Seoul2033Colors.Bg,
    background = Seoul2033Colors.Bg,
    onBackground = Seoul2033Colors.Text,
    surface = Seoul2033Colors.Surface,
    onSurface = Seoul2033Colors.Text,
    surfaceVariant = Seoul2033Colors.Surface,
    onSurfaceVariant = Seoul2033Colors.TextMuted,
    outline = Seoul2033Colors.BorderNormal,
    error = Seoul2033Colors.Danger,
    tertiary = Seoul2033Colors.Ok
)

@Composable
fun Seoul2033Theme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = scheme, content = content)