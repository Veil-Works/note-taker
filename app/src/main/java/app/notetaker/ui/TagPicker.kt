package app.notetaker.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.notetaker.model.Tag

/** Manual tag selection, shared by the note popup and tag browser. */
@Composable
fun TagPicker(tags: List<Tag>, selectedIds: Set<String>, onToggle: (Tag) -> Unit, onCreate: (String, () -> Unit) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (tags.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    FilterChip(selected = tag.id in selectedIds, onClick = { onToggle(tag) }, label = { Text(tag.name) })
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("New tag") },
                singleLine = true, modifier = Modifier.weight(1f))
            TextButton(onClick = {
                val submittedName = name
                onCreate(submittedName) {
                    // Keep any new text entered while the previous request was saving.
                    if (name == submittedName) name = ""
                }
            }, enabled = name.isNotBlank()) { Text("Add tag") }
        }
    }
}
