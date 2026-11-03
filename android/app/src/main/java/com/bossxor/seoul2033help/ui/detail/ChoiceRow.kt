package com.bossxor.seoul2033help.ui.detail

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/** 추천 테두리 회전 각도. 추천이 아닌 행은 애니메이션을 돌리지 않는다. */
@Composable
private fun recommendAngle(active: Boolean): Float =
    if (!active) 0f else rememberInfiniteTransition(label = "recBorder").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    ).value

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceRow(choice: Choice, recommendHighlight: Boolean, onNext: (() -> Unit)? = null) {
    val rec = recommendHighlight && choice.recommend
    val angle = recommendAngle(rec)
    val chances = choice.effects.filterIsInstance<Effect.Chance>()
    val chips = choice.effects.filter { it !is Effect.Chance }

    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .drawBehind {
                val stroke = if (rec) 2.5.dp.toPx() else 1.dp.toPx()
                val radius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                val inset = stroke / 2
                val rectSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset, inset)
                drawRoundRect(color = Seoul2033Colors.Surface, cornerRadius = radius)
                if (rec) {
                    rotate(angle) {
                        drawRoundRect(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Seoul2033Colors.Recommend, Seoul2033Colors.RecommendSoft,
                                    Seoul2033Colors.RecommendBright, Seoul2033Colors.RecommendSoft,
                                    Seoul2033Colors.Recommend
                                )
                            ),
                            topLeft = topLeft, size = rectSize, cornerRadius = radius,
                            style = Stroke(width = stroke)
                        )
                    }
                } else {
                    drawRoundRect(
                        color = Seoul2033Colors.BorderNormal, topLeft = topLeft, size = rectSize,
                        cornerRadius = radius, style = Stroke(width = stroke)
                    )
                }
            }
            .then(if (onNext != null) Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onNext) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(choice.text, style = MaterialTheme.typography.titleMedium, color = Seoul2033Colors.Text)
        if (chips.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) { chips.forEach { EffectChip(it) } }
        }
        chances.forEach { ChanceLine(it) }
        if (onNext != null) {
            Text(
                "다음 화면 →", style = MaterialTheme.typography.labelLarge, color = Seoul2033Colors.Recommend,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun ChanceLine(effect: Effect.Chance) {
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
            append("  /  ")
            pushStyle(SpanStyle(color = Seoul2033Colors.Danger))
            append("실패: $bad")
            pop()
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 10.dp)
    )
}

/** 효과 칩: 진한 단색 대신 색을 옅게 깔고 글자색으로 종류를 구분한다. */
@Composable
private fun EffectChip(effect: Effect) {
    val color: Color = when (EffectTone.kind(effect)) {
        EffectKind.Good -> Seoul2033Colors.Ok
        EffectKind.Bad -> Seoul2033Colors.Danger
        EffectKind.Condition -> Seoul2033Colors.Condition
        EffectKind.Chance -> Seoul2033Colors.Chance
        EffectKind.Neutral -> Seoul2033Colors.TextMuted
    }
    Text(
        text = EffectFormatter.format(effect),
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 11.dp, vertical = 4.dp)
    )
}
