package app.notetaker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.notetaker.model.*

@Composable
fun AutomaticOrganization(tags: List<Tag>, reasons: Map<String, String>, status: OrganizationStatus,
    onRemove: (Tag) -> Unit, onKeep: (Tag) -> Unit, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (status.error != null) {
            Text(status.error, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRetry) { Text("Retry organization") }
        } else if (status.pending) Text("Organizing in the background…", style = MaterialTheme.typography.bodySmall)
        val automatic = tags.filter { it.id in reasons }
        if (automatic.isNotEmpty()) Text("Organized automatically", style = MaterialTheme.typography.labelLarge)
        automatic.forEach { tag ->
            Text(tag.displayName)
            Text(reasons.getValue(tag.id), style = MaterialTheme.typography.bodySmall)
            Row {
                TextButton(onClick = { onRemove(tag) }, modifier = Modifier.testTag("reject_auto_${tag.id}")) { Text("Remove") }
                TextButton(onClick = { onKeep(tag) }, modifier = Modifier.testTag("keep_auto_${tag.id}")) { Text("Keep") }
            }
        }
    }
}
