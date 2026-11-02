package com.bossxor.seoul2033help.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bossxor.seoul2033help.data.model.EncounterNode
import com.bossxor.seoul2033help.ui.search.Footer
import com.bossxor.seoul2033help.ui.theme.Seoul2033Colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeDetailScreen(node: EncounterNode, onBack: () -> Unit) {
    var recommend by remember { mutableStateOf(true) }
    Scaffold(
        containerColor = Seoul2033Colors.Bg,
        topBar = {
            TopAppBar(
                title = { Text(node.title, maxLines = 1) },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } },
                actions = {
                    FilterChip(
                        selected = recommend,
                        onClick = { recommend = !recommend },
                        label = { Text("추천") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Seoul2033Colors.Surface)
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(horizontal = 16.dp)) {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item(key = "story") {
                    Text(node.story, style = MaterialTheme.typography.bodyLarge)
                    node.body?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium,
                            color = Seoul2033Colors.TextMuted)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                itemsIndexed(node.choices, key = { i, c -> "$i:${c.text}" }) { _, c ->
                    ChoiceRow(c, recommend)
                }
            }
            Footer()
        }
    }
}