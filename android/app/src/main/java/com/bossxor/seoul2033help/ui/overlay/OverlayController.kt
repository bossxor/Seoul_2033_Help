package com.bossxor.seoul2033help.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bossxor.seoul2033help.data.UserSettings
import com.bossxor.seoul2033help.data.model.EncounterNode
import com.bossxor.seoul2033help.domain.ScoredNode
import com.bossxor.seoul2033help.ui.detail.ChoiceRow
import com.bossxor.seoul2033help.ui.theme.Seoul2033Colors

sealed class OverlayState {
    data object Idle : OverlayState()
    data class Matched(val node: EncounterNode, val score: Double) : OverlayState()
    data class Ambiguous(val candidates: List<ScoredNode>) : OverlayState()
    data object Miss : OverlayState()
    data class OcrError(val message: String) : OverlayState()
}

class OverlayActions(
    val onRecognize: () -> Unit,
    val onSearch: () -> Unit,
    val onToggleRecommend: () -> Unit,
    val onAlpha: (Float) -> Unit,
    val onQuit: () -> Unit,
    val onPick: (EncounterNode) -> Unit
)

@Composable
fun OverlayPanel(state: OverlayState, s: UserSettings, a: OverlayActions) {
    Column(
        Modifier.fillMaxWidth()
            .background(Seoul2033Colors.Surface.copy(alpha = s.overlayAlpha), RoundedCornerShape(12.dp))
            .border(2.dp, Seoul2033Colors.Recommend, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) { Body(state, s, a) }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Btn("인식", Modifier.weight(1f), onClick = a.onRecognize)
            Btn("검색", Modifier.weight(1f), onClick = a.onSearch)
            Btn(if (s.showRecommend) "추천 ON" else "추천 OFF", Modifier.weight(1f), onClick = a.onToggleRecommend)
            Btn("종료", Modifier.weight(1f), danger = true, onClick = a.onQuit)
        }
        Slider(
            value = s.overlayAlpha, onValueChange = a.onAlpha,
            valueRange = UserSettings.ALPHA_RANGE,
            modifier = Modifier.heightIn(min = 48.dp)
        )
    }
}

@Composable
private fun Body(state: OverlayState, s: UserSettings, a: OverlayActions) {
    when (state) {
        OverlayState.Idle -> Muted("‘인식’을 눌러 화면을 읽어요")
        OverlayState.Miss -> Muted("정보 없음 · ‘검색’으로 직접 찾아보세요")
        is OverlayState.OcrError -> Text(state.message, color = Seoul2033Colors.Danger)
        is OverlayState.Matched -> {
            Text("${state.node.story} · ${state.node.title}", style = MaterialTheme.typography.titleMedium)
            Muted("신뢰도 ${(state.score * 100).toInt()}%")
            state.node.choices.forEach { ChoiceRow(it, s.showRecommend) }
        }
        is OverlayState.Ambiguous -> state.candidates.forEach { c ->
            Text(
                c.node.title, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clickable { a.onPick(c.node) }.wrapContentHeight(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
private fun Muted(t: String) = Text(t, color = Seoul2033Colors.TextMuted, style = MaterialTheme.typography.bodyMedium)

@Composable
private fun Btn(t: String, m: Modifier, danger: Boolean = false, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, modifier = m.heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (danger) Seoul2033Colors.Danger else Seoul2033Colors.Text
        )
    ) { Text(t, maxLines = 1, style = MaterialTheme.typography.labelLarge) }
}
