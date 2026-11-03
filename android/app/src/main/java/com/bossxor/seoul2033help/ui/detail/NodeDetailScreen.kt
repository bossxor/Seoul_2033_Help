package com.bossxor.seoul2033help.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bossxor.seoul2033help.data.model.EncounterNode
import com.bossxor.seoul2033help.ui.theme.Seoul2033Colors

@Composable
fun NodeDetailScreen(node: EncounterNode, onOpen: (String) -> Unit = {}, onBack: () -> Unit) {
    var recommend by remember { mutableStateOf(true) }
    val accent = Seoul2033Colors.storyColor(node.story)
    Column(Modifier.fillMaxSize().background(Seoul2033Colors.BgGradient).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Seoul2033Colors.Recommend)
            }
            Text(
                node.title, style = MaterialTheme.typography.titleLarge, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            FilterChip(selected = recommend, onClick = { recommend = !recommend }, label = { Text("추천") })
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "story", contentType = "story") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        node.story, style = MaterialTheme.typography.labelLarge, color = accent,
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(accent.copy(alpha = 0.16f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    node.body?.let {
                        Text(
                            it, style = MaterialTheme.typography.bodyMedium, color = Seoul2033Colors.TextMuted,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(Seoul2033Colors.Surface.copy(alpha = 0.6f)).padding(14.dp)
                        )
                    }
                }
            }
            itemsIndexed(node.choices, key = { i, c -> "$i:${c.text}" }, contentType = { _, _ -> "choice" }) { _, c ->
                ChoiceRow(c, recommend, onNext = c.nextId?.let { id -> { onOpen(id) } })
            }
        }
    }
}
