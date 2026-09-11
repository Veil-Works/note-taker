package app.notetaker.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.notetaker.model.Tag
import app.notetaker.model.TagAlias

@Composable
fun TagSuggestions(
    errorMessage: String?,
    tags: List<Tag>,
    aliases: List<TagAlias>,
    suggestions: List<Tag>,
    onAccept: (Tag) -> Unit,
    onDismiss: (Tag) -> Unit,
    onSaveAlias: (String, String?, String, () -> Unit) -> Unit,
    onDeleteAlias: (String) -> Unit,
) {
    var editingTagId by rememberSaveable { mutableStateOf<String?>(null) }
    var showKeywords by rememberSaveable { mutableStateOf(false) }
    Column {
        if (suggestions.isNotEmpty()) {
            Text("Suggested tags", style = MaterialTheme.typography.labelLarge)
            Text("From saved text. Tap a tag to accept it.", style = MaterialTheme.typography.bodySmall)
            suggestions.forEach { tag ->
                Column(Modifier.fillMaxWidth()) {
                    SuggestionChip(modifier = Modifier.testTag("suggestion_${tag.name}"), onClick = { onAccept(tag) }, label = { Text(tag.displayName) })
                    Row {
                    TextButton(onClick = { onDismiss(tag) }) { Text("Dismiss " + tag.name) }
                    TextButton(onClick = { editingTagId = tag.id; showKeywords = true }) { Text("Keywords") }
                    }
                }
            }
        }
        if (tags.isNotEmpty()) {
            TextButton(onClick = { showKeywords = true }) { Text("Manage tag keywords") }
        }
    }
    if (showKeywords) {
        AlertDialog(
            modifier = Modifier.testTag("tag_keywords_dialog"),
            onDismissRequest = { showKeywords = false },
            title = { Text("Tag keywords") },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Text("Tag names already match. Add words or phrases to suggest an existing tag.")
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { tag ->
                            FilterChip(selected = editingTagId == tag.id, onClick = { editingTagId = tag.id }, label = { Text(tag.displayName) })
                        }
                    }
                    val tag = tags.find { it.id == editingTagId }
                    if (tag == null) Text("Choose a tag to edit its keywords.")
                    else key(tag.id) {
                        aliases.filter { it.tagId == tag.id }.forEach { alias ->
                            key(alias.id) {
                                var text by rememberSaveable(alias.text) { mutableStateOf(alias.text) }
                                OutlinedTextField(value = text, onValueChange = { text = it },
                                    label = { Text("Keyword for " + tag.name) }, modifier = Modifier.fillMaxWidth())
                                Row {
                                    TextButton(onClick = { onSaveAlias(tag.id, alias.id, text) {} }, enabled = text.isNotBlank() && text != alias.text) { Text("Save keyword") }
                                    TextButton(onClick = { onDeleteAlias(alias.id) }) { Text("Remove keyword") }
                                }
                            }
                        }
                        var newText by rememberSaveable { mutableStateOf("") }
                        OutlinedTextField(value = newText, onValueChange = { newText = it },
                            label = { Text("New keyword or phrase") }, modifier = Modifier.fillMaxWidth())
                        TextButton(onClick = {
                            val submitted = newText
                            onSaveAlias(tag.id, null, submitted) { if (newText == submitted) newText = "" }
                        }, enabled = newText.isNotBlank()) { Text("Add keyword") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showKeywords = false }) { Text("Done") } },
        )
    }
}
