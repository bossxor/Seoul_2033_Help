package com.bossxor.seoul2033help.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bossxor.seoul2033help.data.EncounterRepository
import com.bossxor.seoul2033help.ui.theme.Seoul2033Colors

@Composable
fun SearchScreen(repo: EncounterRepository, onOpen: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { repo.search(query) }
    Column(
        Modifier.fillMaxSize().background(Seoul2033Colors.Bg).statusBarsPadding().padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("인카운터·선택지 검색") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        LazyColumn(Modifier.weight(1f)) {
            items(results, key = { it.id }) { node ->
                Column(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable { onOpen(node.id) }.padding(vertical = 12.dp)
                ) {
                    Text(node.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        node.story, style = MaterialTheme.typography.bodySmall,
                        color = Seoul2033Colors.TextMuted, maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HorizontalDivider(color = Seoul2033Colors.BorderNormal)
            }
        }
        Footer()
    }
}

@Composable
fun Footer() = Text(
    "출처: 나무위키 등 · 개인 비상업 용도",
    style = MaterialTheme.typography.labelSmall,
    color = Seoul2033Colors.TextMuted,
    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 8.dp)
)
