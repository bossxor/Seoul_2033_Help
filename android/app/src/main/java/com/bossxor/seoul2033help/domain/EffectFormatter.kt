package com.bossxor.seoul2033help.domain

import com.bossxor.seoul2033help.data.model.Effect
import kotlin.math.roundToInt

object EffectFormatter {
    fun formatAll(effects: List<Effect>): String =
        effects.joinToString(" · ") { format(it) }

    fun format(effect: Effect): String = when (effect) {
        is Effect.Item -> {
            val d = effect.delta ?: 1
            val count = if (kotlin.math.abs(d) > 1) " ×${kotlin.math.abs(d)}" else ""
            (if (d < 0) "- " else "+ ") + effect.name + count
        }
        is Effect.Ability -> "+ ${effect.name}"
        is Effect.Status -> {
            val sign = if (effect.delta >= 0) "+" else "-"
            "$sign ${effect.name}"
        }
        is Effect.Resource -> {
            val sign = if (effect.delta > 0) "+" else ""
            "${effect.name} $sign${effect.delta}"
        }
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
