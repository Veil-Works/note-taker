package app.notetaker.feature.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.notetaker.model.*
import app.notetaker.ui.NotebookState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatedHistoryScreen(anchorId: String, state: NotebookState, onBack: () -> Unit, onOpen: (String) -> Unit) {
    var scope by rememberSaveable(anchorId) { mutableStateOf(ConnectionScope.CLOSE) }
    var newestFirst by rememberSaveable(anchorId) { mutableStateOf(false) }
    val history = remember(anchorId, state.notes, state.edges, scope) { relatedHistory(anchorId, state.notes, state.edges, scope) }
    val ordered = if (newestFirst) history.asReversed() else history
    Scaffold(containerColor = Color.Transparent, topBar = {
        TopAppBar(title = { Text("Related notes", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("related_history"),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(state.notes.find { it.id == anchorId }?.preview().orEmpty(), style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Connections to this note, in time order. Broader matches may be less relevant.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(scope == ConnectionScope.CLOSE, { scope = ConnectionScope.CLOSE }, label = { Text("Closer") }, modifier = Modifier.testTag("history_close"))
                    FilterChip(scope == ConnectionScope.BROAD, { scope = ConnectionScope.BROAD }, label = { Text("Broader") }, modifier = Modifier.testTag("history_broad"))
                    TextButton(onClick = { newestFirst = !newestFirst }, modifier = Modifier.testTag("history_order")) { Text(if (newestFirst) "Newest first" else "Oldest first") }
                }
                when {
                    state.loading -> CircularProgressIndicator()
                    state.error != null -> Text(state.error, color = MaterialTheme.colorScheme.error)
                    history.isEmpty() -> Text("This note no longer exists.")
                    history.size == 1 -> Text("No other notes at this connection strength. This note stands on its own.")
                }
            }
            items(ordered, key = { it.id }) { note ->
                Column(Modifier.fillMaxWidth().clickable { onOpen(note.id) }.padding(vertical = 8.dp).testTag("history_note_${note.id}"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(Instant.ofEpochMilli(note.createdAt).atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm")) + if (note.id == anchorId) " · This note" else "",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(note.content, style = MaterialTheme.typography.bodyLarge)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
