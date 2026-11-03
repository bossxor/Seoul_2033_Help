package com.bossxor.seoul2033help.ui.detail

import androidx.compose.ui.graphics.Color
import com.bossxor.seoul2033help.data.model.Effect
import com.bossxor.seoul2033help.ui.theme.Seoul2033Colors

enum class EffectKind { Condition, Good, Bad, Chance, Neutral }

object EffectTone {
    fun kind(effect: Effect): EffectKind = when (effect) {
        is Effect.Condition -> EffectKind.Condition
        is Effect.Item, is Effect.Ability -> EffectKind.Good
        is Effect.Status -> EffectKind.Bad
        is Effect.Resource -> if (effect.delta >= 0) EffectKind.Good else EffectKind.Bad
        is Effect.Chance -> EffectKind.Chance
        is Effect.Text -> {
            val n = effect.name
            when {
                n.contains("회복") || n.contains("획득") || n.contains("얻") -> EffectKind.Good
                n.contains("깎") || n.contains("잃") || n.contains("소모") || n.contains("실패") ||
                    n.contains("즉사") || n.contains("부상") -> EffectKind.Bad
                else -> EffectKind.Neutral
            }
        }
    }

    fun color(effect: Effect): Color = when (kind(effect)) {
        EffectKind.Condition -> Seoul2033Colors.Condition
        EffectKind.Good -> Seoul2033Colors.Ok
        EffectKind.Bad -> Seoul2033Colors.Danger
        EffectKind.Chance -> Seoul2033Colors.Chance
        EffectKind.Neutral -> Seoul2033Colors.TextMuted
    }
}
