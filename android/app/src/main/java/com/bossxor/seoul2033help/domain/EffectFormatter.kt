package com.bossxor.seoul2033help.domain

import com.bossxor.seoul2033help.data.model.Effect
import kotlin.math.roundToInt

object EffectFormatter {
    fun formatAll(effects: List<Effect>): String =
        effects.joinToString(" · ") { format(it) }

    fun format(effect: Effect): String = when (effect) {
        is Effect.Item -> "아이템: ${effect.name}" + (effect.delta?.let { " ($it)" } ?: "")
        is Effect.Ability -> "능력: ${effect.name}"
        is Effect.Status -> "상태: ${effect.name}"
        is Effect.Resource -> "${effect.name}: ${effect.delta}"
        is Effect.Text -> effect.name
        is Effect.Condition -> "조건: ${effect.name}"
        is Effect.Chance -> {
            val pct = (effect.successRate * 100).roundToInt()
            val ok = formatAll(effect.success).ifEmpty { "-" }
            val bad = formatAll(effect.failure).ifEmpty { "-" }
            "${pct}% 성공: $ok / 실패: $bad"
        }
    }
}
