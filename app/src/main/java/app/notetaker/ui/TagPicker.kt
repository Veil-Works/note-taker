package app.notetaker.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.notetaker.model.Tag

/** Manual tag selection, shared by the note popup and tag browser. */
@Composable
fun TagPicker(tags: List<Tag>, selectedIds: Set<String>, onToggle: (Tag) -> Unit, onCreate: (String, () -> Unit) -> Unit, onDelete: ((Tag) -> Unit)? = null, onOpenConcept: ((Tag) -> Unit)? = null) {
    var name by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (tags.isNotEmpty()) {
            OutlinedTextField(query, { query = it }, label = { Text("Find topics") }, singleLine = true, shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow, focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.filter { query.isBlank() || it.displayName.contains(query.trim(), true) }.forEach { tag ->
                    Column {
                    FilterChip(modifier = Modifier.testTag("topic_chip_${tag.id}"), shape = RoundedCornerShape(50), selected = tag.id in selectedIds, onClick = { onToggle(tag) }, label = { Text(tag.displayName) },
                        trailingIcon = onDelete?.let { delete -> { IconButton(onClick = { delete(tag) }, modifier = Modifier.size(32.dp).testTag("delete_tag_${tag.id}")) { Icon(Icons.Default.Delete, "Delete topic ${tag.name}") } } })
                    onOpenConcept?.let { open -> TextButton(onClick = { open(tag) }, modifier = Modifier.testTag("open_concept_${tag.id}")) { Text("Details") } }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("New topic") },
                singleLine = true, shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow, focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.weight(1f))
            TextButton(onClick = {
                val submittedName = name
                onCreate(submittedName) {
                    // Keep any new text entered while the previous request was saving.
                    if (name == submittedName) name = ""
                }
            }, enabled = name.isNotBlank()) { Text("Add topic") }
        }
    }
}
