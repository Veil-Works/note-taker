package app.notetaker.feature.concepts

import app.notetaker.ui.countLabel
import app.notetaker.ui.NotebookDialog
import app.notetaker.ui.theme.glassPanel
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.notetaker.model.preview

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TopicsScreen(viewModel: ConceptsViewModel, onBack: () -> Unit, onOpenNote: (String) -> Unit) {
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val status by viewModel.organizationStatus.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var merging by rememberSaveable { mutableStateOf(false) }
    val topic = topics.find { it.id == selected }
    fun back() { if (selected != null) selected = null else onBack() }
    BackHandler { back() }
    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent), title = { Text(topic?.name ?: "Topics", style = MaterialTheme.typography.headlineLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = { TextButton(onClick = ::back) { Text("Back") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("topics_page"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                (error ?: state.error ?: status.error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (status.error != null) TextButton(onClick = viewModel::retryOrganization) { Text("Retry organization") }
                else if (status.pending) Text("Finding related notes in the background…")
            }
            if (selected == null) {
                item { Text("Your notes, grouped automatically by meaning. Names come from phrases in the notes.") }
                if (topics.isEmpty()) item { Text("Keep writing. Topics appear when related notes accumulate; no tags or setup needed.") }
                items(topics, key = { it.id }) { candidate ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), onClick = { selected = candidate.id }, modifier = Modifier.fillMaxWidth().glassPanel(16.dp).testTag("topic_${candidate.id}"), shape = MaterialTheme.shapes.large) {
                        Column(Modifier.fillMaxWidth().padding(18.dp)) {
                            Text(candidate.name)
                            Text("${countLabel(candidate.noteIds.size, "note")} · ${if (candidate.customized) "Customized" else "Automatic group"}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else if (topic == null) item { Text("This topic has changed. Go back to see the current groups.") }
            else {
                item { Text(topic.name, style = MaterialTheme.typography.titleLarge) }
                item { FlowRow {
                    TextButton(enabled = !busy, onClick = { renaming = true }) { Text("Rename topic") }
                    TextButton(enabled = !busy && topics.size > 1, onClick = { merging = true }) { Text("Merge topic") }
                } }
                items(state.notes.filter { it.id in topic.noteIds }, key = { it.id }) { note ->
                    TextButton(onClick = { onOpenNote(note.id) }, modifier = Modifier.fillMaxWidth()) { Text(note.preview(), Modifier.fillMaxWidth()) }
                }
            }
        }
    }
    if (renaming && topic != null) {
        var name by rememberSaveable(topic.id) { mutableStateOf(topic.name) }
        NotebookDialog(onDismissRequest = { if (!busy) renaming = false }, title = { Text("Rename topic") }, text = {
            Column { error?.let { Text(it, color = MaterialTheme.colorScheme.error) }; OutlinedTextField(name, { name = it }, label = { Text("Topic name") }) }
        }, confirmButton = { TextButton(enabled = !busy && name.isNotBlank(), onClick = { viewModel.renameTopic(topic.id, name) { renaming = false } }) { Text("Save") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { renaming = false }) { Text("Cancel") } })
    }
    if (merging && topic != null) {
        NotebookDialog(onDismissRequest = { if (!busy) merging = false }, title = { Text("Merge into ${topic.name}") }, text = {
            Column {
                Text("Choose a group to combine with this one. Your chosen name and combined membership will be kept.")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                LazyColumn(Modifier.heightIn(max = 320.dp)) { items(topics.filter { it.id != topic.id }, key = { it.id }) { other ->
                    TextButton(enabled = !busy, onClick = { viewModel.mergeTopics(topic.id, other.id) { merging = false } }) { Text(other.name) }
                } }
            }
        }, confirmButton = { TextButton(enabled = !busy, onClick = { merging = false }) { Text("Cancel") } })
    }
}
