package com.bossxor.seoul2033help.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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
    data class Miss(val ocrText: String) : OverlayState()
    data object Decoding : OverlayState()
    data class OcrError(val message: String) : OverlayState()
}

class OverlayActions(
    val onRecognize: () -> Unit,
    val onSearch: () -> Unit,
    val onToggleRecommend: () -> Unit,
    val onAlpha: (Float) -> Unit,
    val onQuit: () -> Unit,
    val onPick: (EncounterNode) -> Unit,
    val onAiDecode: () -> Unit = {},
    val onOpenId: (String) -> Unit = {},
    /** 손잡이를 끌 때 화면 px 이동량 */
    val onDrag: (Float, Float) -> Unit = { _, _ -> },
    /** 접힘/펼침이 바뀔 때 (창 너비 조절용) */
    val onExpand: (Boolean) -> Unit = {}
)

@Composable
fun OverlayPanel(state: OverlayState, s: UserSettings, a: OverlayActions) {
    var expanded by remember { mutableStateOf(false) }
    // 후보를 골라야 하면 자동으로 펼친다
    LaunchedEffect(state is OverlayState.Ambiguous) { if (state is OverlayState.Ambiguous) expanded = true }
    // 창 너비를 접힘/펼침에 맞춘다
    LaunchedEffect(expanded) { a.onExpand(expanded) }

    Column(
        Modifier.fillMaxWidth()
            .background(Seoul2033Colors.Surface.copy(alpha = s.overlayAlpha), RoundedCornerShape(14.dp))
            .border(2.dp, Seoul2033Colors.Recommend, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CompactBar(state, expanded, a) { expanded = !expanded }
        if (expanded) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) { Body(state, s, a) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Btn("검색", Modifier.weight(1f), onClick = a.onSearch)
                Btn(if (s.showRecommend) "추천 ON" else "추천 OFF", Modifier.weight(1f), onClick = a.onToggleRecommend)
                Btn("종료", Modifier.weight(1f), danger = true, onClick = a.onQuit)
            }
            Slider(
                value = s.overlayAlpha, onValueChange = a.onAlpha,
                valueRange = UserSettings.ALPHA_RANGE,
                modifier = Modifier.heightIn(min = 40.dp)
            )
        }
    }
}

/** 접힌 상태의 한 줄: [손잡이] 제목 [인식] [펼침/접기] [✕] */
@Composable
private fun CompactBar(state: OverlayState, expanded: Boolean, a: OverlayActions, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.width(26.dp).height(48.dp).pointerInput(Unit) {
                detectDragGestures { change, drag -> change.consume(); a.onDrag(drag.x, drag.y) }
            },
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.width(5.dp).height(26.dp).background(Seoul2033Colors.BorderNormal, RoundedCornerShape(50)))
        }
        Text(
            summary(state),
            style = MaterialTheme.typography.bodyMedium,
            color = Seoul2033Colors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).clickable(onClick = onToggle).padding(horizontal = 4.dp)
        )
        Btn("인식", Modifier.width(56.dp), onClick = a.onRecognize)
        Spacer(Modifier.width(4.dp))
        Btn(if (expanded) "접기" else "펼침", Modifier.width(56.dp), onClick = onToggle)
        // 접힌 채로도 끌 수 있게 항상 둔다
        IconButton(onClick = a.onQuit, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Close, contentDescription = "종료", tint = Seoul2033Colors.Danger)
        }
    }
}

private fun summary(state: OverlayState): String = when (state) {
    OverlayState.Idle -> "‘인식’을 눌러 주세요"
    is OverlayState.Matched -> state.node.title
    is OverlayState.Ambiguous -> "후보 ${state.candidates.size}개 · 펼쳐서 고르기"
    is OverlayState.Miss -> "정보 없음"
    OverlayState.Decoding -> "AI 해독 중…"
    is OverlayState.OcrError -> "인식 실패"
}

@Composable
private fun Body(state: OverlayState, s: UserSettings, a: OverlayActions) {
    when (state) {
        OverlayState.Idle -> Muted("‘인식’을 눌러 화면을 읽어요")
        is OverlayState.Miss -> {
            Muted("정보 없음 · ‘검색’으로 직접 찾아보세요")
            if (s.aiEnabled && state.ocrText.isNotBlank()) {
                Button(
                    onClick = a.onAiDecode,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text("AI 해독") }
            }
        }
        OverlayState.Decoding -> Muted("AI 해독 중…")
        is OverlayState.OcrError -> Text(state.message, color = Seoul2033Colors.Danger)
        is OverlayState.Matched -> {
            Text("${state.node.story} · ${state.node.title}", style = MaterialTheme.typography.titleMedium)
            Muted("신뢰도 ${(state.score.coerceIn(0.0, 1.0) * 100).toInt()}%")  // 푯말 보너스로 1.0 을 넘을 수 있음
            state.node.choices.forEach { c ->
                ChoiceRow(c, s.showRecommend, onNext = c.nextId?.let { id -> { a.onOpenId(id) } })
            }
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
