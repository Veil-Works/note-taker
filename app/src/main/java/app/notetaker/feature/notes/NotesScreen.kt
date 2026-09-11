package app.notetaker.feature.notes

import androidx.compose.foundation.combinedClickable
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import app.notetaker.model.Tag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.FilterChip
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.notetaker.model.Note
import app.notetaker.model.preview
import app.notetaker.ui.TagPicker
import androidx.compose.material3.TextButton
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onOpenNote: (String) -> Unit,
    onOpenConcept: (String?) -> Unit = {},
    onOpenTopics: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val selectedTags by viewModel.selectedTags.collectAsStateWithLifecycle()
    val selectedNotes by viewModel.selectedNotes.collectAsStateWithLifecycle()
    val bulkBusy by viewModel.bulkBusy.collectAsStateWithLifecycle()
    var selecting by rememberSaveable { mutableStateOf(false) }
    var deletingNotes by remember { mutableStateOf(false) }
    var taggingNotes by remember { mutableStateOf<List<Note>?>(null) }
    var deletingTag by remember { mutableStateOf<Tag?>(null) }
    val selectionMode = selecting || selectedNotes.isNotEmpty()
    BackHandler(selectionMode && !bulkBusy) { viewModel.clearSelection(); selecting = false }
    if (deletingNotes) AlertDialog(
        onDismissRequest = { deletingNotes = false },
        title = { Text("Delete ${selectedNotes.size} notes?") },
        text = { Text("These notes and their connections will be permanently deleted.") },
        confirmButton = { TextButton(onClick = { viewModel.deleteSelected(); deletingNotes = false; selecting = false }) { Text("Delete notes") } },
        dismissButton = { TextButton(onClick = { deletingNotes = false }) { Text("Cancel") } },
    )
    deletingTag?.let { tag ->
        AlertDialog(
            onDismissRequest = { deletingTag = null },
            title = { Text("Delete tag “${tag.name}”?") },
            text = { Text("Remove this tag from every note and delete its keywords. Notes and manual connections will be kept.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteTag(tag); deletingTag = null }) { Text("Delete tag") } },
            dismissButton = { TextButton(onClick = { deletingTag = null }) { Text("Cancel") } },
        )
    }
    taggingNotes?.let { notes ->
        BulkTagDialog(tags, notes, viewModel::createTag,
            onApply = { added, removed -> viewModel.tagSelected(added, removed); taggingNotes = null; selecting = false },
            onDismiss = { taggingNotes = null })
    }
    var showTags by rememberSaveable { mutableStateOf(false) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val frequentTags by viewModel.frequentTags.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshDates() }
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val captureError by viewModel.captureError.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(state.query.isNotEmpty()) }
    var captureHeight by remember { mutableStateOf(0) }
    val bottomSpace = with(LocalDensity.current) { captureHeight.toDp() } + 12.dp

    if (showFilters) FilterDialog(filters, onApply = {
        viewModel.setFilters(it)
        showFilters = false
    }, onDismiss = { showFilters = false })

    Scaffold(
        topBar = {
            Column {
            if (selectionMode) {
                TopAppBar(
                    title = { Text("${selectedNotes.size} selected") },
                    navigationIcon = { IconButton(enabled = !bulkBusy, onClick = { viewModel.clearSelection(); selecting = false }) { Icon(Icons.Default.Close, "Cancel selection") } },
                    actions = { TextButton(enabled = !bulkBusy, onClick = { viewModel.selectNotes(state.notes.map { it.id }) }) { Text("Select all") } },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(enabled = !bulkBusy && selectedNotes.isNotEmpty(), onClick = { deletingNotes = true }) { Text("Delete") }
                    TextButton(enabled = !bulkBusy && selectedNotes.isNotEmpty(), onClick = { viewModel.prepareTagging { taggingNotes = it } }) { Text("Tag") }
                    TextButton(enabled = !bulkBusy && selectedNotes.size > 1, onClick = { viewModel.connectSelected(); selecting = false }) { Text("Connect") }
                }
            } else if (searching) {
                SearchBar(
                    query = state.query,
                    onQueryChanged = viewModel::onQueryChanged,
                    onClose = {
                        viewModel.onQueryChanged("")
                        searching = false
                    },
                )
            } else {
                TopAppBar(
                    title = { Text("Notes") },
                    actions = {
                        TextButton(onClick = { selecting = true }) { Text("Select") }
                        TextButton(onClick = { showFilters = true }) { Text("Filters") }
                        TextButton(onClick = { showTags = !showTags }) { Text("Tags") }
                        IconButton(onClick = { searching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search notes")
                        }
                    },
                )
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onOpenTopics) { Text("Topics") }
                FilterChip(
                    selected = filters.period == DatePeriod.RECENT,
                    onClick = { viewModel.setFilters(filters.copy(period = if (filters.period == DatePeriod.RECENT) DatePeriod.ALL else DatePeriod.RECENT)) },
                    label = { Text("Last 7 days") },
                )
                if (searching) TextButton(onClick = { showFilters = true }) { Text("Filters") }
                frequentTags.forEach { frequent ->
                    FilterChip(
                        selected = frequent.tag.id in selectedTags,
                        onClick = { viewModel.toggleFilterTag(frequent.tag.id) },
                        label = { Text("${frequent.tag.displayName} (${frequent.count})") },
                        modifier = Modifier.testTag("frequent_tag_${frequent.tag.id}"),
                    )
                }
            }
            if (filters.period != DatePeriod.ALL || filters.sort != NoteSort.UPDATED || selectedTags.isNotEmpty() || state.query.isNotBlank()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val dateLabel = if (filters.period == DatePeriod.CUSTOM) "${filters.start} – ${filters.end}" else filters.period.label
                    Text(
                        "${filters.dateField.label}: $dateLabel · ${filters.sort.label}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = viewModel::clearFilters) { Text("Clear filters") }
                }
            }
            if (showTags || selectedTags.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 12.dp)) {
                    TextButton(onClick = { onOpenConcept(null) }) { Text("Browse concepts and types") }
                    Text("Match all selected tags", style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = { viewModel.selectTag(null) }) { Text(if (selectedTags.isEmpty()) "All notes" else "Clear tag filter") }
                    TagPicker(tags, selectedTags.toSet(), { tag -> viewModel.toggleFilterTag(tag.id) }, viewModel::createTag, onDelete = { deletingTag = it }, onOpenConcept = { onOpenConcept(it.id) })
                }
            }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            when {
                state.isLoading -> LoadingContent()
                state.errorMessage != null -> MessageContent(state.errorMessage)
                state.notes.isEmpty() -> MessageContent(
                    if (filters.period != DatePeriod.ALL) "No notes match these filters."
                    else if (selectedTags.isNotEmpty()) "No notes match all selected tags and search."
                    else if (state.query.isBlank()) "No notes yet. Write your first thought below."
                    else "No notes match your search.",
                    Modifier.padding(bottom = bottomSpace),
                )
                else -> NotesList(
                    notes = state.notes,
                    onOpenNote = { if (selectionMode) viewModel.toggleNote(it) else onOpenNote(it) },
                    selectedIds = selectedNotes.toSet(),
                    selectionMode = selectionMode,
                    onLongClick = { viewModel.toggleNote(it) },
                    enabled = !bulkBusy,
                    bottomSpace = bottomSpace,
                    resetKey = Triple(state.query, selectedTags, filters),
                )
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .onSizeChanged { captureHeight = it.height }
                    .padding(12.dp).testTag("capture_overlay"),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 8.dp,
                tonalElevation = 3.dp,
            ) {
            Column(Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    TextField(
                        value = draft,
                        onValueChange = viewModel::onDraftChanged,
                        modifier = Modifier.weight(1f).testTag("capture_field"),
                        placeholder = { Text("Write a note…") },
                        readOnly = saving,
                        minLines = 1,
                        maxLines = 5,
                    )
                    IconButton(
                        onClick = viewModel::completeNote,
                        enabled = draft.isNotBlank() && !saving,
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Complete note")
                    }
                }
                captureError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClose: () -> Unit,
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search notes") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close search")
            }
        },
    )
}

@Composable
private fun NotesList(
    notes: List<Note>,
    onOpenNote: (String) -> Unit,
    selectedIds: Set<String>,
    selectionMode: Boolean,
    onLongClick: (String) -> Unit,
    enabled: Boolean,
    bottomSpace: Dp,
    resetKey: Any,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(resetKey, notes.firstOrNull()?.id) {
        listState.scrollToItem(0)
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().testTag("notes_list"),
        contentPadding = PaddingValues(bottom = bottomSpace),
    ) {
        items(notes, key = Note::id) { note ->
            NoteRow(note = note, selected = note.id in selectedIds, selectionMode = selectionMode, enabled = enabled, onLongClick = { onLongClick(note.id) }, onClick = { onOpenNote(note.id) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun NoteRow(note: Note, selected: Boolean, selectionMode: Boolean, enabled: Boolean, onLongClick: () -> Unit, onClick: () -> Unit) {
    ListItem(
        leadingContent = if (selectionMode) { { Checkbox(checked = selected, onCheckedChange = null) } } else null,
        headlineContent = {
            Text(
                text = note.preview(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
            )
        },
        supportingContent = {
            Column {
            if (note.tags.isNotEmpty()) Text(note.tags.joinToString(" · ") { it.displayName }, style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = remember(note.updatedAt) { formatTimestamp(note.updatedAt) },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            }
        },
        modifier = Modifier.testTag("note_${note.id}").semantics { if (selectionMode) this.selected = selected }.combinedClickable(
            enabled = enabled,
            onLongClick = onLongClick,
            onLongClickLabel = "Select note",
            onClickLabel = if (selectionMode) "Toggle selection" else "Open note",
            onClick = onClick,
        ),
    )
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MessageContent(message: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message.orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
    }
}

private fun formatTimestamp(timestamp: Long): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(timestamp))

@Composable
private fun BulkTagDialog(tags: List<Tag>, notes: List<Note>, onCreate: (String, () -> Unit) -> Unit, onApply: (Set<String>, Set<String>) -> Unit, onDismiss: () -> Unit) {
    val counts = remember { notes.flatMap { it.tags.distinctBy(Tag::id) }.groupingBy(Tag::id).eachCount() }
    var changes by remember { mutableStateOf(emptyMap<String, Boolean>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tag selected notes") },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                Text("Checked: all notes. Dash: some notes. Tap to apply to all, then tap again to remove from all. Untouched tags stay unchanged.")
                tags.forEach { tag ->
                    val count = counts[tag.id] ?: 0
                    val state = when {
                        changes[tag.id] == true -> ToggleableState.On
                        changes[tag.id] == false -> ToggleableState.Off
                        count == notes.size -> ToggleableState.On
                        count > 0 -> ToggleableState.Indeterminate
                        else -> ToggleableState.Off
                    }
                    Row(
                        Modifier.fillMaxWidth().testTag("bulk_tag_${tag.id}").triStateToggleable(
                            state = state, role = Role.Checkbox,
                            onClick = { changes = changes + (tag.id to (state != ToggleableState.On)) },
                        ).padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TriStateCheckbox(state = state, onClick = null)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(tag.displayName)
                            Text(when (state) {
                                ToggleableState.On -> "All ${notes.size} notes"
                                ToggleableState.Off -> "No notes"
                                ToggleableState.Indeterminate -> "$count of ${notes.size} notes"
                            }, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                TagPicker(emptyList(), emptySet(), {}, onCreate)
            }
        },
        confirmButton = { TextButton(enabled = changes.isNotEmpty(), onClick = { onApply(changes.filterValues { it }.keys, changes.filterValues { !it }.keys) }) { Text("Apply tags") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
