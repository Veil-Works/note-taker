package app.notetaker.feature.concepts

import app.notetaker.ui.countLabel
import app.notetaker.ui.NotebookDialog
import app.notetaker.ui.theme.glassPanel
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.notetaker.model.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConceptsScreen(viewModel: ConceptsViewModel, initialTagId: String?, onBack: () -> Unit, onOpenNote: (String) -> Unit, root: Boolean = false) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val status by viewModel.organizationStatus.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf(initialTagId) }
    var query by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var merging by rememberSaveable { mutableStateOf(false) }
    var addingNotes by rememberSaveable { mutableStateOf(false) }
    val tag = state.tags.find { it.id == selectedId }
    fun back() { if (selectedId != null && initialTagId == null) selectedId = null else onBack() }
    BackHandler(selectedId != null || !root) { back() }
    Scaffold(containerColor = Color.Transparent, topBar = {
        TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent), title = { Text(tag?.name ?: "Topics", style = MaterialTheme.typography.headlineLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = { if (selectedId != null || !root) TextButton(onClick = ::back) { Text("Back") } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding().testTag("concept_page"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                (error ?: state.error ?: status.error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (status.error != null) TextButton(onClick = viewModel::retryOrganization) { Text("Retry organization") }
                else if (status.pending) Text("Finding topics in the background…", style = MaterialTheme.typography.bodySmall)
            }
            if (state.loading) item { CircularProgressIndicator() }
            else if (selectedId == null) {
                item { Text("Create topics yourself or let related notes bring them to light.") }
                item { OutlinedTextField(query, { query = it }, label = { Text("Find or create a topic") }, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) }
                item { TextButton(enabled = !busy && query.isNotBlank(), onClick = { viewModel.create(query) { selectedId = it; query = ""; editing = true } }) { Text("Create or open") } }
                val matching = state.tags.filter { candidate ->
                    query.isBlank() || candidate.displayName.contains(query.trim(), true) || state.aliases.any { it.tagId == candidate.id && it.text.contains(query.trim(), true) }
                }
                if (matching.isEmpty()) item { Text(if (query.isBlank()) "No topics yet. Create one, or keep writing to discover them automatically." else "No topics match your search.") }
                items(matching, key = { it.id }) { candidate ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), onClick = { selectedId = candidate.id }, modifier = Modifier.fillMaxWidth().glassPanel(16.dp).testTag("concept_${candidate.id}"), shape = MaterialTheme.shapes.large) {
                        Column(Modifier.fillMaxWidth().padding(18.dp)) {
                            Text(candidate.displayName)
                            if (candidate.provisional) Text("Discovered automatically", style = MaterialTheme.typography.labelSmall)
                            Text("${countLabel(state.notes.count { note -> note.tags.any { it.id == candidate.id } }, "note")}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else if (tag == null) item { Text("This topic no longer exists.") }
            else {
                item {
                    Text(tag.name, style = MaterialTheme.typography.titleLarge)
                    FlowRow {
                        TextButton(enabled = !busy, onClick = { editing = true }) { Text("Rename topic") }
                        TextButton(enabled = !busy && state.tags.size > 1, onClick = { merging = true }) { Text("Merge topic") }
                        TextButton(enabled = !busy, onClick = { addingNotes = true }) { Text("Add notes") }
                        TextButton(enabled = !busy, onClick = { deleting = true }) { Text("Delete topic", color = MaterialTheme.colorScheme.error) }
                    }
                }
                val notes = state.notes.filter { note -> note.tags.any { it.id == tag.id } }
                item { Text("${countLabel(notes.size, "associated note")}", style = MaterialTheme.typography.titleMedium) }
                if (notes.isEmpty()) item { Text("Attach this topic from a note’s topic picker, or accept a suggestion.") }
                items(notes, key = { it.id }) { note ->
                    Column {
                        TextButton(onClick = { onOpenNote(note.id) }, modifier = Modifier.fillMaxWidth().testTag("concept_note_${note.id}")) {
                            Text(note.preview(), modifier = Modifier.fillMaxWidth())
                        }
                        val automatic = tag.id in note.automaticTagReasons
                        Text(if (automatic) "Added automatically" else "Added by you", style = MaterialTheme.typography.labelSmall)
                        Row {
                            if (automatic) TextButton(enabled = !busy, onClick = { viewModel.setMembership(note.id, tag.id, true) }, modifier = Modifier.testTag("topic_keep_${note.id}")) { Text("Keep") }
                            TextButton(enabled = !busy, onClick = { viewModel.setMembership(note.id, tag.id, false) }, modifier = Modifier.testTag("topic_remove_${note.id}")) { Text("Remove") }
                        }
                    }
                }
            }
        }
    }
    if (merging && tag != null) {
        var sourceId by rememberSaveable(tag.id) { mutableStateOf<String?>(null) }
        var mergeQuery by rememberSaveable { mutableStateOf("") }
        val source = state.tags.find { it.id == sourceId }
        NotebookDialog(onDismissRequest = { if (!busy) merging = false }, title = { Text("Merge into ${tag.name}") }, text = {
            Column {
                Text("Combine notes under ${tag.name}. Manual assignments and exclusions are preserved. Automatic assignments can still change.")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(mergeQuery, { mergeQuery = it }, label = { Text("Find a topic to merge") })
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(state.tags.filter { it.id != tag.id && it.displayName.contains(mergeQuery.trim(), true) }, key = { it.id }) { other ->
                        FilterChip(selected = other.id == sourceId, onClick = { sourceId = other.id }, label = { Text(other.displayName) }, modifier = Modifier.testTag("merge_topic_${other.id}"))
                    }
                }
                source?.let { Text("${it.name} will become part of ${tag.name}.") }
            }
        }, confirmButton = { TextButton(enabled = !busy && source != null, onClick = {
            viewModel.mergeTopics(tag.id, checkNotNull(source).id) { merging = false }
        }) { Text("Merge") } }, dismissButton = { TextButton(enabled = !busy, onClick = { merging = false }) { Text("Cancel") } })
    }
    if (addingNotes && tag != null) {
        var noteQuery by rememberSaveable { mutableStateOf("") }
        NotebookDialog(onDismissRequest = { if (!busy) addingNotes = false }, title = { Text("Add notes to ${tag.name}") }, text = {
            Column {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(noteQuery, { noteQuery = it }, label = { Text("Find a note") })
                val available = state.notes.filter { note -> note.tags.none { it.id == tag.id } && note.content.contains(noteQuery.trim(), true) }
                if (available.isEmpty()) Text("No matching notes to add.")
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(available, key = { it.id }) { note ->
                        TextButton(enabled = !busy, onClick = { viewModel.setMembership(note.id, tag.id, true) }, modifier = Modifier.testTag("topic_add_${note.id}")) { Text(note.preview()) }
                    }
                }
            }
        }, confirmButton = { TextButton(enabled = !busy, onClick = { addingNotes = false }) { Text("Done") } })
    }
    if (deleting && tag != null) NotebookDialog(onDismissRequest = { if (!busy) deleting = false },
        title = { Text("Delete ${tag.name}?") }, text = { Text("Remove this topic. Your notes will be kept.") },
        confirmButton = { TextButton(enabled = !busy, onClick = { viewModel.deleteTag(tag.id) { deleting = false; selectedId = null } }) { Text("Delete topic") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancel") } })
    if (editing && tag != null) {
        var name by rememberSaveable(tag.id) { mutableStateOf(tag.name) }
        NotebookDialog(onDismissRequest = { if (!busy) editing = false }, title = { Text("Edit topic") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(name, { name = it }, label = { Text("Topic name") })

            }
        }, confirmButton = { TextButton(enabled = !busy && name.isNotBlank(), onClick = { viewModel.renameTopic(tag.id, name) { editing = false } }) { Text("Save topic") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { editing = false }) { Text("Cancel") } })
    }
}
