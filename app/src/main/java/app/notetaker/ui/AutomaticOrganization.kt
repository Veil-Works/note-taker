package app.notetaker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.notetaker.model.*

@Composable
fun AutomaticOrganization(tags: List<Tag>, reasons: Map<String, String>, related: List<RelatedNote>, notes: List<Note>, status: OrganizationStatus,
    onRemove: (Tag) -> Unit, onKeep: (Tag) -> Unit, onOpen: (String) -> Unit, onRetry: () -> Unit, noteId: String? = null) {
    var showAll by rememberSaveable(noteId) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (status.error != null) {
            Text(status.error, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRetry) { Text("Retry organization") }
        } else if (status.pending) Text("Organizing in the background…", style = MaterialTheme.typography.bodySmall)
        val automatic = tags.filter { it.id in reasons }
        if (automatic.isNotEmpty()) Text("Organized automatically", style = MaterialTheme.typography.labelLarge)
        automatic.forEach { tag ->
            Text(tag.displayName)
            Text(reasons.getValue(tag.id), style = MaterialTheme.typography.bodySmall)
            Row {
                TextButton(onClick = { onRemove(tag) }, modifier = Modifier.testTag("reject_auto_${tag.id}")) { Text("Remove") }
                TextButton(onClick = { onKeep(tag) }, modifier = Modifier.testTag("keep_auto_${tag.id}")) { Text("Keep") }
            }
        }
        val byId = remember(notes) { notes.associateBy { it.id } }
        val matches = related.sortedWith(compareByDescending<RelatedNote> { it.score }.thenBy { it.noteId })
            .distinctBy { it.noteId }.mapNotNull { byId[it.noteId] }
        if (matches.isNotEmpty()) {
            Text("Related notes", style = MaterialTheme.typography.labelLarge)
            Text("Similar in meaning", style = MaterialTheme.typography.bodySmall)
            (if (showAll) matches else matches.take(5)).forEach { note ->
                TextButton(onClick = { onOpen(note.id) }, modifier = Modifier.fillMaxWidth().testTag("semantic_${note.id}")) {
                    Text(note.preview(), modifier = Modifier.fillMaxWidth())
                }
            }
            if (matches.size > 5) TextButton(onClick = { showAll = !showAll }, modifier = Modifier.testTag("related_show_all")) {
                Text(if (showAll) "Show fewer" else "Show all (${matches.size})")
            }
        }
    }
}
