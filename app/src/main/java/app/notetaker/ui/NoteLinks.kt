package app.notetaker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.notetaker.model.Note
import app.notetaker.model.preview
import app.notetaker.model.connectedNoteIds

@Composable
fun NoteLinks(
    currentId: String?,
    notes: List<Note>,
    tagIds: Set<String>,
    enabled: Boolean,
    errorMessage: String?,
    onSetLinked: (String, Boolean) -> Unit,
    onOpenNote: (String) -> Unit,
    related: List<app.notetaker.model.RelatedNote> = emptyList(),
) {
    var choosing by rememberSaveable(currentId) { mutableStateOf(false) }
    var query by rememberSaveable(currentId) { mutableStateOf("") }
    val current = notes.firstOrNull { it.id == currentId }
    val manualIds = current?.linkedNoteIds.orEmpty()
    val scores = related.groupBy { it.noteId }.mapValues { (_, rows) -> rows.maxOf { it.score } }
    val linkedIds = (current?.connectedNoteIds(notes).orEmpty() + scores.keys) - currentId
    val linkedNotes = notes.filter { it.id in linkedIds }.distinctBy { it.id }.sortedWith(
        compareByDescending<Note> { it.id in manualIds }.thenByDescending { scores[it.id] ?: -1.0 }
            .thenByDescending { note -> note.tags.count { it.id in tagIds } }.thenBy { it.id })
    var showAll by rememberSaveable(currentId) { mutableStateOf(false) }

    if (linkedNotes.isEmpty()) Text("Connect notes about the same project, training, or other theme.")
    if (linkedNotes.any { it.id !in manualIds }) Text("Keep linked saves an automatic connection. Remove link removes that saved link; automatic matches can still appear.", style = MaterialTheme.typography.bodySmall)
    (if (showAll) linkedNotes else linkedNotes.take(5)).forEach { note ->
        val shared = note.tags.filter { it.id in tagIds }
        Text(when {
            note.id in manualIds -> "Linked by you"
            shared.isNotEmpty() -> "Shared: " + shared.joinToString { it.displayName }
            else -> "Similar in meaning"
        }, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth()) {
            TextButton(
                onClick = { onOpenNote(note.id) }, enabled = enabled,
                modifier = Modifier.weight(1f).testTag("open_link_${note.id}"),
            ) { Text(note.preview(), maxLines = 2, overflow = TextOverflow.Ellipsis) }
            TextButton(
                onClick = { onSetLinked(note.id, note.id !in manualIds) }, enabled = enabled,
                modifier = Modifier.testTag(if (note.id in manualIds) "remove_link_${note.id}" else "keep_link_${note.id}"),
            ) { Text(if (note.id in manualIds) "Remove link" else "Keep linked") }
        }
    }
    if (linkedNotes.size > 5) TextButton(onClick = { showAll = !showAll }, modifier = Modifier.testTag("connections_show_all")) {
        Text(if (showAll) "Show fewer" else "Show all (${linkedNotes.size})")
    }
    TextButton(onClick = { choosing = true }, enabled = enabled && currentId != null) {
        Text("Connect a note")
    }
    if (choosing) {
        val candidates = notes.filter {
            it.id != currentId && it.id !in linkedIds &&
                (query.isBlank() || it.content.contains(query.trim(), ignoreCase = true) ||
                    it.tags.any { tag -> tag.name.contains(query.trim(), ignoreCase = true) })
        }.sortedByDescending { note -> note.tags.count { it.id in tagIds } }
        NotebookDialog(
            onDismissRequest = { choosing = false },
            title = { Text("Connect a note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        label = { Text("Search notes or topics") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("link_search"),
                    )
                    Text("Notes with shared topics appear first.")
                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (candidates.isEmpty()) Text("No notes available to connect.")
                    LazyColumn(Modifier.heightIn(max = 280.dp)) {
                        items(candidates, key = { it.id }) { note ->
                            TextButton(
                                onClick = { onSetLinked(note.id, true) }, enabled = enabled,
                                modifier = Modifier.fillMaxWidth().testTag("connect_${note.id}"),
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(note.preview(), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    if (note.tags.isNotEmpty()) Text(
                                        note.tags.joinToString { it.displayName },
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosing = false }) { Text("Done connecting") } },
        )
    }
}
