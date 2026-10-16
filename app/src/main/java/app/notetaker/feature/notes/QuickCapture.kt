package app.notetaker.feature.notes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun QuickCapture(viewModel: QuickCaptureViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.finish() }
    DisposableEffect(viewModel) { onDispose { viewModel.finish() } }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = state.content,
            onValueChange = viewModel::onContentChanged,
            placeholder = { Text("Jot something down…") },
            modifier = Modifier.fillMaxWidth().testTag("quick_capture_field"),
            minLines = 1, maxLines = 4,
            readOnly = state.finishing,
            shape = MaterialTheme.shapes.large,
        )
        if (state.noteId != null || state.error != null) Row(
            Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(state.error ?: when {
                state.saving || state.dirty -> "Saving…"
                else -> "Saved"
            }, style = MaterialTheme.typography.labelMedium,
                color = if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).testTag("quick_capture_status"))
            if (state.error != null) TextButton(onClick = viewModel::retry) { Text("Retry") }
            TextButton(enabled = !state.finishing, onClick = { viewModel.finish { focus.clearFocus() } },
                modifier = Modifier.testTag("quick_capture_done")) { Text("Done") }
        }
    }
}
