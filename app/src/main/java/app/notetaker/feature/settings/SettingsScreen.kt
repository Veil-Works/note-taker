package app.notetaker.feature.settings

import app.notetaker.ui.theme.glassPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.notetaker.model.OrganizationStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(mode: String, onMode: (String) -> Unit, compact: Boolean, onCompact: (Boolean) -> Unit,
    status: OrganizationStatus, onRetry: () -> Unit, error: String?) {
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)
        Text("A notebook that feels like yours.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(modifier = Modifier.glassPanel(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("System", "Light", "Dark").forEach { value -> FilterChip(selected = mode == value, onClick = { onMode(value) }, label = { Text(value) }) }
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().toggleable(value = compact, role = Role.Switch, onValueChange = onCompact), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Compact notes"); Text("Show more in your list", style = MaterialTheme.typography.bodySmall) }
                    Switch(checked = compact, onCheckedChange = null)
                }
            }
        }
        Card(modifier = Modifier.glassPanel(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Automatic connections", style = MaterialTheme.typography.titleMedium)
                Text(status.error ?: if (status.pending) "Finding connections…" else "Everything is up to date")
                Text("Related notes and topics are found on your phone.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onRetry, enabled = !status.pending) { Text("Refresh connections") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        Text("Private by design", style = MaterialTheme.typography.titleMedium)
        Text("Your notebook stays on this device. No account, no cloud sync. Uninstalling the app removes its notes; export and backup are not available yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Note-taker · Your thoughts, connected", style = MaterialTheme.typography.labelMedium)
    }
}
