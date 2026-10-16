package app.notetaker.feature.notes

import app.notetaker.ui.countLabel
import androidx.compose.animation.*
import androidx.compose.animation.core.tween

import app.notetaker.ui.theme.glassPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.combinedClickable
import androidx.activity.compose.BackHandler
import app.notetaker.ui.NotebookDialog
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
import androidx.compose.material3.OutlinedTextField
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
    compact: Boolean = false,
    quickCaptureViewModel: QuickCaptureViewModel? = null,
    connections: List<app.notetaker.model.GraphEdge> = emptyList(),
    connectionNotes: List<Note>? = null,
    onOpenRelated: (String) -> Unit = {},
) {
    val quickState = quickCaptureViewModel?.state?.collectAsStateWithLifecycle()?.value
    val captureGeneration by viewModel.captureGeneration.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val selectedTags by viewModel.selectedTags.collectAsStateWithLifecycle()
    val selectedNotes by viewModel.selectedNotes.collectAsStateWithLifecycle()
    val bulkBusy by viewModel.bulkBusy.collectAsStateWithLifecycle()
    var selecting by rememberSaveable { mutableStateOf(false) }
    var deletingNotes by remember { mutableStateOf(false) }
    var taggingNotes by remember { mutableStateOf<List<Note>?>(null) }
    val selectionMode = selecting || selectedNotes.isNotEmpty()
    BackHandler(selectionMode && !bulkBusy) { viewModel.clearSelection(); selecting = false }
    if (deletingNotes) NotebookDialog(
        onDismissRequest = { deletingNotes = false },
        title = { Text("Delete ${countLabel(selectedNotes.size, "note")}?") },
        text = { Text("These notes and their connections will be permanently deleted.") },
        confirmButton = { TextButton(onClick = { viewModel.deleteSelected(); deletingNotes = false; selecting = false }) { Text("Delete notes") } },
        dismissButton = { TextButton(onClick = { deletingNotes = false }) { Text("Cancel") } },
    )
    taggingNotes?.let { notes ->
        BulkTagDialog(tags, notes, viewModel::createTag,
            onApply = { added, removed -> viewModel.tagSelected(added, removed); taggingNotes = null; selecting = false },
            onDismiss = { taggingNotes = null })
    }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val frequentTags by viewModel.frequentTags.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshDates() }
    var searching by rememberSaveable { mutableStateOf(state.query.isNotEmpty()) }
    var grouped by rememberSaveable { mutableStateOf(false) }
    val bottomSpace = 16.dp
    val largeText = LocalDensity.current.fontScale > 1.15f

    if (showFilters) FilterDialog(filters, tags, selectedTags.toSet(), onTopicsApply = { chosen ->
        (selectedTags.toSet() union chosen).filter { (it in selectedTags) != (it in chosen) }.forEach(viewModel::toggleFilterTag)
    }, onApply = {
        viewModel.setFilters(it)
        showFilters = false
    }, onDismiss = { showFilters = false })

    Scaffold(containerColor = Color.Transparent,
        topBar = {
            Column(Modifier.animateContentSize(tween(240))) {
            AnimatedContent(targetState = when { selectionMode -> "selection"; searching -> "search"; else -> "notes" },
                transitionSpec = {
                    (fadeIn(tween(180)) + slideInVertically(tween(240)) { it / 5 }) togetherWith
                        (fadeOut(tween(120)) + slideOutVertically(tween(200)) { -it / 5 }) using SizeTransform(clip = false)
                }, label = "note_toolbar") { mode ->
            Column {
            if (mode == "selection") {
                TopAppBar(colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                    title = { Text("${selectedNotes.size} selected") },
                    navigationIcon = { IconButton(enabled = !bulkBusy, onClick = { viewModel.clearSelection(); selecting = false }) { Icon(Icons.Default.Close, "Cancel selection") } },
                    actions = { TextButton(enabled = !bulkBusy, onClick = { viewModel.selectNotes(state.notes.map { it.id }) }) { Text("Select all") } },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(enabled = !bulkBusy && selectedNotes.isNotEmpty(), onClick = { deletingNotes = true }) { Text("Delete") }
                    TextButton(enabled = !bulkBusy && selectedNotes.isNotEmpty(), onClick = { viewModel.prepareTagging { taggingNotes = it } }) { Text("Topic") }
                    TextButton(enabled = !bulkBusy && selectedNotes.size > 1, onClick = { viewModel.connectSelected(); selecting = false }) { Text("Connect") }
                }
            } else if (mode == "search") {
                SearchBar(
                    query = state.query,
                    onQueryChanged = viewModel::onQueryChanged,
                    onClose = {
                        viewModel.onQueryChanged("")
                        searching = false
                    },
                )
            } else {
                TopAppBar(colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                    title = { Text("Notes", style = MaterialTheme.typography.headlineLarge, maxLines = 1) },
                    actions = {
                        if (!largeText) {
                        TextButton(onClick = { selecting = true }) { Text("Select") }
                        TextButton(onClick = { showFilters = true }) { Text("Filters") }
                        }
                        IconButton(onClick = { searching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search notes")
                        }
                    },
                )
            }
            }
            }
            if (!selectionMode && !searching && quickCaptureViewModel != null) QuickCapture(quickCaptureViewModel)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (largeText && !selectionMode && !searching) {
                    TextButton(onClick = { selecting = true }) { Text("Select") }
                    TextButton(onClick = { showFilters = true }) { Text("Filters") }
                }
                FilterChip(selected = grouped, onClick = { grouped = !grouped }, label = { Text("Group by topics") })
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
            AnimatedVisibility(visible = selectedTags.isNotEmpty(),
                enter = expandVertically(tween(260), expandFrom = Alignment.Top) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(220), shrinkTowards = Alignment.Top) + fadeOut(tween(120))) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp).glassPanel(22.dp).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.filter { it.id in selectedTags }.forEach { tag ->
                            FilterChip(selected = true, onClick = { viewModel.toggleFilterTag(tag.id) }, label = { Text(tag.name) }, modifier = Modifier.testTag("topic_chip_${tag.id}"))
                        }
                        TextButton(onClick = { viewModel.selectTag(null) }) { Text("Clear topic filter") }
                    }
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
                    else if (selectedTags.isNotEmpty()) "No notes match all selected topics and search."
                    else if (state.query.isBlank()) if (quickCaptureViewModel != null) "Write your first note above. No title or topic needed." else "No notes yet. Tap New note to capture your first thought."
                    else "No notes match your search.",
                    Modifier.padding(bottom = bottomSpace),
                )
                else -> NotesList(
                    notes = state.notes.filterNot { it.id == quickState?.noteId },
                    grouped = grouped,
                    compact = compact,
                    onOpenNote = { if (selectionMode) viewModel.toggleNote(it) else onOpenNote(it) },
                    selectedIds = selectedNotes.toSet(),
                    selectionMode = selectionMode,
                    onLongClick = { viewModel.toggleNote(it) },
                    enabled = !bulkBusy,
                    bottomSpace = bottomSpace,
                    resetKey = listOf(state.query, selectedTags, filters, captureGeneration),
                    connections = connections,
                    connectionNotes = connectionNotes ?: state.notes,
                    onOpenRelated = onOpenRelated,
                )
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
    TopAppBar(colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
        title = {
            OutlinedTextField(
                shape = RoundedCornerShape(20.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
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
    grouped: Boolean,
    compact: Boolean,
    onOpenNote: (String) -> Unit,
    selectedIds: Set<String>,
    selectionMode: Boolean,
    onLongClick: (String) -> Unit,
    enabled: Boolean,
    bottomSpace: Dp,
    resetKey: Any,
    connections: List<app.notetaker.model.GraphEdge>,
    connectionNotes: List<Note>,
    onOpenRelated: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(resetKey, grouped) {
        listState.scrollToItem(0)
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().testTag("notes_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomSpace),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val tagNames = notes.flatMap { it.tags }.associate { it.id to it.displayName }
        val groups = if (grouped) {
            notes.flatMap { note -> if (note.tags.isEmpty()) listOf(null to note) else note.tags.map { it.id to note } }
                .groupBy({ it.first }, { it.second }).entries.sortedBy { tagNames[it.key] ?: "Untagged" }.map { it.key to it.value }
        } else listOf(null to notes)
        groups.forEach { (groupId, members) ->
            val name = if (grouped) tagNames[groupId] ?: "Untagged" else ""
            if (name.isNotEmpty()) item(key = "heading-$groupId") {
                Text("$name · ${members.size}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp).semantics { heading() })
            }
            items(members, key = { "$groupId:${it.id}" }) { note ->
                NoteRow(note = note, compact = compact, selected = note.id in selectedIds, selectionMode = selectionMode, enabled = enabled,
                    relatedCount = app.notetaker.model.relatedHistory(note.id, connectionNotes, connections, app.notetaker.model.ConnectionScope.CLOSE).size - 1,
                    onOpenRelated = { onOpenRelated(note.id) },
                    onLongClick = { onLongClick(note.id) }, onClick = { onOpenNote(note.id) })
            }
        }

    }
}

@Composable
private fun NoteRow(note: Note, compact: Boolean, selected: Boolean, selectionMode: Boolean, enabled: Boolean, relatedCount: Int, onOpenRelated: () -> Unit, onLongClick: () -> Unit, onClick: () -> Unit) {
    Surface(modifier = Modifier.glassPanel(22.dp, highlighted = selected), shape = RoundedCornerShape(22.dp),
        color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface, tonalElevation = 0.dp) {
        Row(Modifier.fillMaxWidth().testTag("note_${note.id}")
            .semantics { if (selectionMode) this.selected = selected }
            .combinedClickable(enabled = enabled, onLongClick = onLongClick, onLongClickLabel = "Select note",
                onClickLabel = if (selectionMode) "Toggle selection" else "Open note", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (compact) 12.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedVisibility(visible = selectionMode,
                enter = expandHorizontally(tween(220)) + fadeIn(tween(180)),
                exit = shrinkHorizontally(tween(220)) + fadeOut(tween(120))) {
                Checkbox(checked = selected, onCheckedChange = null)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(note.preview(), maxLines = if (compact) 2 else 5, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Normal)
                if (note.tags.isNotEmpty()) Text(note.tags.joinToString(" · ") { it.displayName },
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (!selectionMode && relatedCount > 0) TextButton(onClick = onOpenRelated, enabled = enabled,
                        modifier = Modifier.testTag("related_${note.id}")) { Text("Related ($relatedCount)") }
                    Text(formatTimestamp(note.updatedAt), modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
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

private fun formatTimestamp(timestamp: Long): String {
    val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    val today = java.time.LocalDate.now()
    val day = when (date.toLocalDate()) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("d MMM"))
    }
    return "$day · ${date.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))}"
}

@Composable
private fun BulkTagDialog(tags: List<Tag>, notes: List<Note>, onCreate: (String, () -> Unit) -> Unit, onApply: (Set<String>, Set<String>) -> Unit, onDismiss: () -> Unit) {
    val counts = remember { notes.flatMap { it.tags.distinctBy(Tag::id) }.groupingBy(Tag::id).eachCount() }
    var changes by remember { mutableStateOf(emptyMap<String, Boolean>()) }
    NotebookDialog(
        onDismissRequest = onDismiss,
        title = { Text("Topic selected notes") },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                Text("Checked: all notes. Dash: some notes. Tap to apply to all, then tap again to remove from all. Untouched topics stay unchanged.")
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
                                ToggleableState.On -> if (notes.size == 1) "1 note" else "All ${notes.size} notes"
                                ToggleableState.Off -> "No notes"
                                ToggleableState.Indeterminate -> "$count of ${notes.size} notes"
                            }, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                TagPicker(emptyList(), emptySet(), {}, onCreate)
            }
        },
        confirmButton = { TextButton(enabled = changes.isNotEmpty(), onClick = { onApply(changes.filterValues { it }.keys, changes.filterValues { !it }.keys) }) { Text("Apply topics") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
