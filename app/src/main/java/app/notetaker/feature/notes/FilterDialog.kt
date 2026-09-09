package app.notetaker.feature.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FilterDialog(filters: NoteFilters, onApply: (NoteFilters) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(filters) }
    var choosingDates by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter and sort") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Date", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatePeriod.entries.forEach { period ->
                        FilterChip(
                            selected = draft.period == period,
                            onClick = {
                                if (period == DatePeriod.CUSTOM) choosingDates = true
                                else draft = draft.copy(period = period)
                            },
                            label = { Text(period.label) },
                        )
                    }
                }
                if (draft.period == DatePeriod.CUSTOM) Text("${draft.start} – ${draft.end}")
                Text("Use dates when notes were")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField.entries.forEach { field ->
                        FilterChip(selected = draft.dateField == field, onClick = { draft = draft.copy(dateField = field) }, label = { Text(field.label) })
                    }
                }
                Text("Sort", style = MaterialTheme.typography.titleSmall)
                NoteSort.entries.forEach { sort ->
                    FilterChip(selected = draft.sort == sort, onClick = { draft = draft.copy(sort = sort) }, label = { Text(sort.label) })
                }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(draft) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (choosingDates) {
        val picker = rememberDateRangePickerState(
            initialSelectedStartDateMillis = draft.start?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            initialSelectedEndDateMillis = draft.end?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        Dialog(onDismissRequest = { choosingDates = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize().systemBarsPadding(), shape = MaterialTheme.shapes.large) {
                Column {
                    DateRangePicker(state = picker, modifier = Modifier.weight(1f), title = { Text("Choose date range", Modifier.padding(16.dp)) })
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { choosingDates = false }) { Text("Cancel range") }
                        TextButton(
                            enabled = picker.selectedStartDateMillis != null && picker.selectedEndDateMillis != null,
                            onClick = {
                                // Material date pickers encode calendar dates at UTC midnight.
                                draft = draft.copy(
                                    period = DatePeriod.CUSTOM,
                                    start = Instant.ofEpochMilli(picker.selectedStartDateMillis!!).atZone(ZoneOffset.UTC).toLocalDate(),
                                    end = Instant.ofEpochMilli(picker.selectedEndDateMillis!!).atZone(ZoneOffset.UTC).toLocalDate(),
                                )
                                choosingDates = false
                            },
                        ) { Text("Use range") }
                    }
                }
            }
        }
    }
}
