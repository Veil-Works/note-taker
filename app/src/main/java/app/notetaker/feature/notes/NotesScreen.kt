package app.notetaker.feature.notes

import androidx.compose.foundation.clickable
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
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
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
            if (searching) {
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
                FilterChip(
                    selected = filters.period == DatePeriod.RECENT,
                    onClick = { viewModel.setFilters(filters.copy(period = if (filters.period == DatePeriod.RECENT) DatePeriod.ALL else DatePeriod.RECENT)) },
                    label = { Text("Last 7 days") },
                )
                if (searching) TextButton(onClick = { showFilters = true }) { Text("Filters") }
                frequentTags.forEach { frequent ->
                    FilterChip(
                        selected = selectedTag == frequent.tag.id,
                        onClick = { viewModel.selectTag(if (selectedTag == frequent.tag.id) null else frequent.tag.id) },
                        label = { Text("${frequent.tag.name} (${frequent.count})") },
                        modifier = Modifier.testTag("frequent_tag_${frequent.tag.id}"),
                    )
                }
            }
            if (filters.period != DatePeriod.ALL || filters.sort != NoteSort.UPDATED || selectedTag != null || state.query.isNotBlank()) {
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
            if (showTags || selectedTag != null) {
                Column(Modifier.padding(horizontal = 12.dp)) {
                    TextButton(onClick = { viewModel.selectTag(null) }) { Text(if (selectedTag == null) "All notes" else "Clear tag filter") }
                    TagPicker(tags, setOfNotNull(selectedTag), { tag -> viewModel.selectTag(if (selectedTag == tag.id) null else tag.id) }, viewModel::createTag)
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
                    else if (selectedTag != null) "No notes match this tag and search."
                    else if (state.query.isBlank()) "No notes yet. Write your first thought below."
                    else "No notes match your search.",
                    Modifier.padding(bottom = bottomSpace),
                )
                else -> NotesList(
                    notes = state.notes,
                    onOpenNote = onOpenNote,
                    bottomSpace = bottomSpace,
                    resetKey = Triple(state.query, selectedTag, filters),
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
            NoteRow(note = note, onClick = { onOpenNote(note.id) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun NoteRow(note: Note, onClick: () -> Unit) {
    ListItem(
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
            if (note.tags.isNotEmpty()) Text(note.tags.joinToString(" · ") { it.name }, style = MaterialTheme.typography.labelMedium)
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
        modifier = Modifier.clickable(
            onClickLabel = "Open note",
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
