package com.bossxor.seoul2033help.ui.detail

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bossxor.seoul2033help.data.model.Choice
import com.bossxor.seoul2033help.domain.EffectFormatter
import com.bossxor.seoul2033help.ui.theme.Seoul2033Colors

@Composable
fun ChoiceRow(
    choice: Choice,
    recommendHighlight: Boolean,
    borderColorRecommend: Color = Seoul2033Colors.Recommend,
    borderColorNormal: Color = Seoul2033Colors.BorderNormal
) {
    val rec = recommendHighlight && choice.recommend
    Column(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .border(
                if (rec) 2.dp else 1.dp,
                if (rec) borderColorRecommend else borderColorNormal,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Text(choice.text, style = MaterialTheme.typography.titleMedium)
        if (choice.effects.isNotEmpty()) {
            Text(
                EffectFormatter.formatAll(choice.effects),
                style = MaterialTheme.typography.bodyMedium,
                color = Seoul2033Colors.Chance
            )
        }
    }
}