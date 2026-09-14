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
fun ConceptsScreen(viewModel: ConceptsViewModel, initialTagId: String?, onBack: () -> Unit, onOpenNote: (String) -> Unit, onTopics: () -> Unit = {}, root: Boolean = false) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf(initialTagId) }
    var query by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var managingTypes by rememberSaveable { mutableStateOf(false) }
    val tag = state.tags.find { it.id == selectedId }
    fun back() { if (selectedId != null && initialTagId == null) selectedId = null else onBack() }
    BackHandler(selectedId != null || !root) { back() }
    Scaffold(containerColor = Color.Transparent, topBar = {
        TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent), title = { Text(tag?.name ?: if (root) "Connections" else "Concepts", style = MaterialTheme.typography.headlineLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = { if (selectedId != null || !root) TextButton(onClick = ::back) { Text("Back") } },
            actions = { TextButton(onClick = { managingTypes = true }) { Text("Types") } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding().testTag("concept_page"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { (error ?: state.error)?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (state.loading) item { CircularProgressIndicator() }
            else if (selectedId == null) {
                item {
                    if (root) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = true, onClick = {}, label = { Text("Tags & types") })
                        FilterChip(selected = false, onClick = onTopics, label = { Text("Automatic topics") })
                    }
                }
                item { OutlinedTextField(query, { query = it }, label = { Text("Find or create a tag") }, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) }
                item { TextButton(enabled = !busy && query.isNotBlank(), onClick = { viewModel.create(query) { selectedId = it; query = ""; editing = true } }) { Text("Create or open") } }
                val matching = state.tags.filter { candidate ->
                    query.isBlank() || candidate.displayName.contains(query.trim(), true) || state.aliases.any { it.tagId == candidate.id && it.text.contains(query.trim(), true) }
                }
                if (matching.isEmpty()) item { Text(if (query.isBlank()) "No tags yet. Create one to connect your notes." else "No tags match your search.") }
                items(matching, key = { it.id }) { candidate ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), onClick = { selectedId = candidate.id }, modifier = Modifier.fillMaxWidth().glassPanel(16.dp).testTag("concept_${candidate.id}"), shape = MaterialTheme.shapes.large) {
                        Column(Modifier.fillMaxWidth().padding(18.dp)) {
                            Text(candidate.displayName)
                            Text("${countLabel(state.notes.count { note -> note.tags.any { it.id == candidate.id } }, "note")}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else if (tag == null) item { Text("This concept no longer exists.") }
            else {
                item {
                    Text(tag.name, style = MaterialTheme.typography.titleLarge)
                    Text(tag.typeName ?: "Tag · no type", style = MaterialTheme.typography.titleMedium)
                    FlowRow {
                        TextButton(enabled = !busy, onClick = { editing = true }) { Text("Edit name and type") }
                        TextButton(enabled = !busy, onClick = { deleting = true }) { Text("Delete tag", color = MaterialTheme.colorScheme.error) }
                    }
                    Text("Aliases", style = MaterialTheme.typography.titleMedium)
                    Text("Words or phrases that suggest this concept in saved notes.", style = MaterialTheme.typography.bodySmall)
                }
                items(state.aliases.filter { it.tagId == tag.id }, key = { it.id }) { alias ->
                    var value by rememberSaveable(alias.text) { mutableStateOf(alias.text) }
                    OutlinedTextField(value, { value = it }, label = { Text("Alias") }, modifier = Modifier.fillMaxWidth())
                    FlowRow {
                        TextButton(enabled = !busy && value.isNotBlank() && value != alias.text, onClick = { viewModel.saveAlias(tag.id, alias.id, value) {} }) { Text("Save alias") }
                        TextButton(enabled = !busy, onClick = { viewModel.deleteAlias(alias.id) }) { Text("Remove alias") }
                    }
                }
                item {
                    var value by rememberSaveable(tag.id) { mutableStateOf("") }
                    OutlinedTextField(value, { value = it }, label = { Text("New alias") }, modifier = Modifier.fillMaxWidth())
                    TextButton(enabled = !busy && value.isNotBlank(), onClick = {
                        val submitted = value
                        viewModel.saveAlias(tag.id, null, submitted) { if (value == submitted) value = "" }
                    }) { Text("Add alias") }
                    HorizontalDivider()
                }
                val notes = state.notes.filter { note -> note.tags.any { it.id == tag.id } }
                item { Text("${countLabel(notes.size, "associated note")}", style = MaterialTheme.typography.titleMedium) }
                if (notes.isEmpty()) item { Text("Attach this concept from a note’s tag picker, or accept a suggestion.") }
                items(notes, key = { it.id }) { note ->
                    TextButton(onClick = { onOpenNote(note.id) }, modifier = Modifier.fillMaxWidth().testTag("concept_note_${note.id}")) {
                        Text(note.preview(), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
    if (deleting && tag != null) NotebookDialog(onDismissRequest = { if (!busy) deleting = false },
        title = { Text("Delete ${tag.name}?") }, text = { Text("Remove this tag and its aliases. Your notes will be kept.") },
        confirmButton = { TextButton(enabled = !busy, onClick = { viewModel.deleteTag(tag.id) { deleting = false; selectedId = null } }) { Text("Delete tag") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancel") } })
    if (editing && tag != null) {
        var name by rememberSaveable(tag.id) { mutableStateOf(tag.name) }
        var typeId by rememberSaveable(tag.id) { mutableStateOf(tag.typeId) }
        NotebookDialog(onDismissRequest = { if (!busy) editing = false }, title = { Text("Edit concept") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(name, { name = it }, label = { Text("Concept name") })
                Text("Type")
                Column {
                    FilterChip(selected = typeId == null, onClick = { typeId = null }, label = { Text("No type (ordinary tag)") })
                    state.types.forEach { type -> FilterChip(selected = typeId == type.id, onClick = { typeId = type.id }, label = { Text(type.name) }) }
                }
                Text("Manage available types using Types on the concept page.", style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = { TextButton(enabled = !busy && name.isNotBlank(), onClick = { viewModel.save(tag, name, typeId) { editing = false } }) { Text("Save concept") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { editing = false }) { Text("Cancel") } })
    }
    if (managingTypes) {
        var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
        NotebookDialog(onDismissRequest = { if (!busy) managingTypes = false }, title = { Text("Entity types") }, text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                state.types.forEach { type -> key(type.id) {
                    var name by rememberSaveable(type.name) { mutableStateOf(type.name) }
                    OutlinedTextField(name, { name = it }, label = { Text("Type name") })
                    FlowRow {
                        TextButton(enabled = !busy && name.isNotBlank() && name != type.name, onClick = { viewModel.saveType(type.id, name) {} }) { Text("Rename") }
                        TextButton(enabled = !busy, onClick = { deletingId = type.id }) { Text("Delete ${type.name}") }
                    }
                } }
                var name by rememberSaveable { mutableStateOf("") }
                OutlinedTextField(name, { name = it }, label = { Text("New type") })
                TextButton(enabled = !busy && name.isNotBlank(), onClick = { val submitted = name; viewModel.saveType(null, submitted) { if (name == submitted) name = "" } }) { Text("Add type") }
            }
        }, confirmButton = { TextButton(enabled = !busy, onClick = { managingTypes = false }) { Text("Done") } })
        deletingId?.let { id ->
            NotebookDialog(onDismissRequest = { deletingId = null }, title = { Text("Delete type?") },
                text = { Text("${state.tags.count { it.typeId == id }} concepts will become ordinary tags. Their notes and aliases will be kept.") },
                confirmButton = { TextButton(enabled = !busy, onClick = { viewModel.deleteType(id); deletingId = null }) { Text("Delete type") } },
                dismissButton = { TextButton(onClick = { deletingId = null }) { Text("Cancel") } })
        }
    }
}
