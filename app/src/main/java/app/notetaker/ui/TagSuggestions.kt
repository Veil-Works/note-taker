package app.notetaker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.notetaker.model.Tag

@Composable
fun TagSuggestions(suggestions: List<Tag>, onAccept: (Tag) -> Unit, onDismiss: (Tag) -> Unit) {
    if (suggestions.isEmpty()) return
    Column {
        Text("Suggested topics", style = MaterialTheme.typography.labelLarge)
        suggestions.forEach { tag ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SuggestionChip(modifier = Modifier.weight(1f).testTag("suggestion_${tag.name}"), onClick = { onAccept(tag) }, label = { Text(tag.displayName) })
                TextButton(onClick = { onDismiss(tag) }) { Text("Dismiss " + tag.name) }
            }
        }
    }
}
