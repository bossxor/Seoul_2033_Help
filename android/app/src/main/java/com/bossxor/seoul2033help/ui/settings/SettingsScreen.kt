package com.bossxor.seoul2033help.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bossxor.seoul2033help.data.SettingsRepository
import com.bossxor.seoul2033help.data.UserSettings
import com.bossxor.seoul2033help.ui.theme.Seoul2033Colors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(repo: SettingsRepository, onBack: () -> Unit, onStartOverlay: () -> Unit = {}) {
    val s by repo.settings.collectAsState(initial = UserSettings())
    val scope = rememberCoroutineScope()
    fun update(t: (UserSettings) -> UserSettings) = scope.launch { repo.update(t) }

    Column(
        Modifier.fillMaxSize().background(Seoul2033Colors.Bg).statusBarsPadding()
            .navigationBarsPadding().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("← 뒤로") }
        Text("설정", style = MaterialTheme.typography.headlineSmall)

        SliderRow("투명도", "${(s.overlayAlpha * 100).toInt()}%", s.overlayAlpha, UserSettings.ALPHA_RANGE, null) { v ->
            update { it.copy(overlayAlpha = v) }
        }
        SwitchRow("자동 OCR", s.autoOcr) { v -> update { it.copy(autoOcr = v) } }
        SliderRow(
            "자동 간격(초)", "${s.autoOcrIntervalSec}초", s.autoOcrIntervalSec.toFloat(),
            1f..10f, 8
        ) { v -> update { it.copy(autoOcrIntervalSec = v.toInt()) } }
        SliderRow("OCR 하단 비율", "${(s.cropBottomFraction * 100).toInt()}%", s.cropBottomFraction, UserSettings.CROP_RANGE, null) { v ->
            update { it.copy(cropBottomFraction = v) }
        }
        SwitchRow("추천 강조", s.showRecommend) { v -> update { it.copy(showRecommend = v) } }

        Button(
            onClick = onStartOverlay,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 8.dp)
        ) { Text("오버레이 시작") }
    }
}

@Composable
private fun SliderRow(
    label: String, value: String, v: Float, range: ClosedFloatingPointRange<Float>,
    steps: Int?, onChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(value, color = Seoul2033Colors.TextMuted)
        }
        Slider(
            value = v.coerceIn(range), onValueChange = onChange, valueRange = range,
            steps = steps ?: 0,
            modifier = Modifier.heightIn(min = 48.dp)
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(checked, onChange)
    }
}
