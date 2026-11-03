package com.bossxor.seoul2033help.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bossxor.seoul2033help.data.EncounterStore
import com.bossxor.seoul2033help.ui.theme.Seoul2033Colors

// 목록 항목마다 새로 만들지 않도록 한 번만 만든다
private val CardShape = RoundedCornerShape(16.dp)
private val PillShape = RoundedCornerShape(50)
private val CardBorder = BorderStroke(1.dp, Seoul2033Colors.BorderNormal)

@Composable
fun SearchScreen(store: EncounterStore, onSettings: () -> Unit = {}, onOpen: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var story by remember { mutableStateOf<String?>(null) }
    val stories = remember { store.all().groupingBy { it.story }.eachCount().entries.sortedByDescending { it.value } }
    val results = remember(query, story) {
        store.search(query).let { r -> if (story == null) r else r.filter { it.story == story } }
    }
    Column(
        Modifier.fillMaxSize().background(Seoul2033Colors.BgGradient).statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("서울 2033 답지", style = MaterialTheme.typography.titleLarge)
                Text(
                    "인카운터 ${results.size}개", style = MaterialTheme.typography.bodySmall,
                    color = Seoul2033Colors.TextMuted
                )
            }
            IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "설정", tint = Seoul2033Colors.Recommend)
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("인카운터·선택지 검색") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Seoul2033Colors.Surface,
                unfocusedContainerColor = Seoul2033Colors.Surface,
                focusedBorderColor = Seoul2033Colors.Recommend,
                unfocusedBorderColor = Seoul2033Colors.BorderNormal
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "all") {
                FilterChip(selected = story == null, onClick = { story = null }, label = { Text("전체") })
            }
            items(stories, key = { it.key }) { (name, count) ->
                FilterChip(
                    selected = story == name,
                    onClick = { story = if (story == name) null else name },
                    label = { Text("$name $count") }
                )
            }
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(results, key = { it.id }) { node ->
                val accent = Seoul2033Colors.storyColor(node.story)
                Surface(
                    onClick = { onOpen(node.id) },
                    shape = CardShape,
                    color = Seoul2033Colors.Surface,
                    border = CardBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.height(IntrinsicSize.Min).heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(5.dp).fillMaxHeight().background(accent))
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Text(node.title, style = MaterialTheme.typography.titleMedium, maxLines = 2,
                                overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    node.story, style = MaterialTheme.typography.labelLarge, color = accent,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clip(PillShape)
                                        .background(accent.copy(alpha = 0.16f))
                                        .padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("선택지 ${node.choices.size}", style = MaterialTheme.typography.bodySmall,
                                    color = Seoul2033Colors.TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}
