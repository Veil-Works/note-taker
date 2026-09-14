package app.notetaker.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Routes outside taps to the owning editor while consuming taps within its panel. */
@Composable
fun NoteWindowBackdrop(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val dismiss by rememberUpdatedState(onDismiss)
    Box(Modifier.fillMaxSize().testTag("note_window_backdrop")
        .pointerInput(Unit) { detectTapGestures { dismiss() } }
        .systemBarsPadding()
        .imePadding()
        .padding(24.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.widthIn(max = 560.dp).fillMaxWidth()
            // Consume blank panel taps; editing controls handle their own gestures.
            .pointerInput(Unit) { detectTapGestures { } }) {
            content()
        }
    }
}
