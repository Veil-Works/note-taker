package app.notetaker.feature.editor

import app.notetaker.ui.theme.glassPanel
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.notetaker.model.Note
import app.notetaker.ui.TagSuggestions
import app.notetaker.ui.TagPicker
import app.notetaker.ui.NoteLinks

/** The list destination remains visible beneath this navigation dialog. */
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    isNewNote: Boolean,
    onClose: () -> Unit,
    onDeleted: (Note?) -> Unit,
    onOpenNote: (String) -> Unit,
    onOpenConcept: (String?) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val tags by viewModel.availableTags.collectAsStateWithLifecycle()
    val related by viewModel.related.collectAsStateWithLifecycle()
    val organizationStatus by viewModel.organizationStatus.collectAsStateWithLifecycle()
    var panel by rememberSaveable { mutableStateOf("note") }
    val notes by viewModel.availableNotes.collectAsStateWithLifecycle()
    fun close() = viewModel.close(onClose)
    BackHandler { if (panel != "note") panel = "note" else close() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.flush() }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 0.dp,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().animateContentSize(tween(220)).glassPanel(28.dp, opaque = true).testTag("note_popup"),
    ) {
        Column(
            modifier = Modifier.heightIn(max = 560.dp).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (panel != "note") TextButton(onClick = { panel = "note" }, modifier = Modifier.testTag("back_to_note")) { Text("Back to note") }
            Text(when (panel) { "topics" -> "Topics"; "connections" -> "Connections"; else -> if (isNewNote) "New note" else "Edit note" }, style = MaterialTheme.typography.titleLarge)
            key(panel) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (panel == "note") {
                        when {
                            state.isLoading -> CircularProgressIndicator()
                            state.isMissing -> Text("This note no longer exists.")
                            else -> OutlinedTextField(
                                value = state.content,
                                onValueChange = viewModel::onContentChanged,
                                modifier = Modifier.fillMaxWidth().testTag("note_popup_content"),
                                shape = MaterialTheme.shapes.large,
                                minLines = 3,
                                maxLines = 8,
                                placeholder = { Text("Write a note…") },
                            )
                        }
                    }
                    if (state.isExistingNote) {
                        if (panel == "note") {
                            if (state.tags.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.tags.forEach { tag ->
                                    AssistChip(onClick = { panel = "topics" }, label = { Text(tag.displayName) })
                                }
                            }
                            if (organizationStatus.pending) Text("Organizing…", style = MaterialTheme.typography.bodySmall)
                            if (organizationStatus.error != null) TextButton(onClick = viewModel::retryOrganization) { Text("Retry organization") }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TextButton(onClick = { panel = "topics" }, modifier = Modifier.testTag("edit_topics")) { Text("Topics") }
                                TextButton(onClick = { panel = "connections" }, modifier = Modifier.testTag("edit_connections")) { Text("Connections") }
                            }
                        }
                        if (panel == "topics") {
                            app.notetaker.ui.AutomaticOrganization(state.tags, state.automaticTagReasons, organizationStatus,
                                onRemove = { viewModel.toggleTag(it); viewModel.flush() }, onKeep = viewModel::keepAutomaticTag,
                                onRetry = viewModel::retryOrganization)
                            TagPicker(tags, state.tags.map { it.id }.toSet(), viewModel::toggleTag, viewModel::createTag, onOpenConcept = { tag -> viewModel.close { onOpenConcept(tag.id) } })
                            TextButton(onClick = { viewModel.close { onOpenConcept(null) } }) { Text("Manage topics") }
                            TagSuggestions(
                                suggestions = suggestions.filterNot { suggested -> state.tags.any { it.id == suggested.id } },
                                onAccept = viewModel::acceptSuggestion,
                                onDismiss = viewModel::dismissSuggestion,
                            )
                        }
                        if (panel == "connections") {
                            if (organizationStatus.pending) Text("Organizing…", style = MaterialTheme.typography.bodySmall)
                            if (organizationStatus.error != null) TextButton(onClick = viewModel::retryOrganization) { Text("Retry organization") }
                            NoteLinks(
                                currentId = state.noteId,
                                related = related,
                                errorMessage = state.errorMessage,
                                notes = notes,
                                tagIds = state.tags.map { it.id }.toSet(),
                                enabled = !state.isLinking && !state.isSaving,
                                onSetLinked = viewModel::setLinked,
                                onOpenNote = { id -> viewModel.close { onOpenNote(id) } },
                            )
                        }
                    }
                    state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    onClick = { viewModel.delete(onDeleted) },
                    enabled = state.isExistingNote && !state.isSaving && !state.isLinking,
                ) { Text("Delete note", color = MaterialTheme.colorScheme.error) }
                Button(onClick = ::close, enabled = !state.isLinking && !state.isSaving) { Text("Done") }
            }
        }
    }
}
