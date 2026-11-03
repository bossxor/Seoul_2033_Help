package com.bossxor.seoul2033help.ui.detail

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.bossxor.seoul2033help.data.model.Choice
import com.bossxor.seoul2033help.data.model.Effect
import com.bossxor.seoul2033help.domain.EffectFormatter
import com.bossxor.seoul2033help.ui.theme.Seoul2033Colors
import kotlin.math.roundToInt

@Composable
fun ChoiceRow(choice: Choice, recommendHighlight: Boolean) {
    val rec = recommendHighlight && choice.recommend
    val angle by rememberInfiniteTransition(label = "recBorder").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .drawBehind {
                val stroke = if (rec) 2.5.dp.toPx() else 1.dp.toPx()
                val radius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                val inset = stroke / 2
                val rectSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset, inset)
                if (rec) {
                    rotate(angle) {
                        drawRoundRect(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Seoul2033Colors.Recommend,
                                    Seoul2033Colors.RecommendSoft,
                                    Seoul2033Colors.RecommendBright,
                                    Seoul2033Colors.RecommendSoft,
                                    Seoul2033Colors.Recommend
                                )
                            ),
                            topLeft = topLeft,
                            size = rectSize,
                            cornerRadius = radius,
                            style = Stroke(width = stroke)
                        )
                    }
                } else {
                    drawRoundRect(
                        color = Seoul2033Colors.BorderNormal,
                        topLeft = topLeft,
                        size = rectSize,
                        cornerRadius = radius,
                        style = Stroke(width = stroke)
                    )
                }
            }
            .padding(12.dp)
    ) {
        Text(choice.text, style = MaterialTheme.typography.titleMedium, color = Seoul2033Colors.Text)
        if (choice.effects.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            choice.effects.forEach { EffectLine(it) }
        }
    }
}

@Composable
private fun EffectLine(effect: Effect) {
    if (effect is Effect.Chance) {
        val pct = (effect.successRate * 100).roundToInt()
        val ok = EffectFormatter.formatAll(effect.success).ifEmpty { "-" }
        val bad = EffectFormatter.formatAll(effect.failure).ifEmpty { "-" }
        Text(
            text = buildAnnotatedString {
                pushStyle(SpanStyle(color = Seoul2033Colors.Chance))
                append("${pct}% ")
                pop()
                pushStyle(SpanStyle(color = Seoul2033Colors.Ok))
                append("성공: $ok")
                pop()
                append(" / ")
                pushStyle(SpanStyle(color = Seoul2033Colors.Danger))
                append("실패: $bad")
                pop()
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 2.dp)
        )
        return
    }
    Text(
        text = EffectFormatter.format(effect),
        style = MaterialTheme.typography.bodyMedium,
        color = EffectTone.color(effect),
        modifier = Modifier.padding(top = 2.dp)
    )
}
