package app.notetaker.feature.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import app.notetaker.ui.theme.glassPanel
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
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FilterDialog(filters: NoteFilters, onApply: (NoteFilters) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(filters) }
    var choosingDates by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.glassPanel(28.dp, opaque = true),
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.Transparent,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("Filter and sort") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Date", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatePeriod.entries.forEach { period ->
                        FilterChip(
                            shape = RoundedCornerShape(50),
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
                        FilterChip(shape = RoundedCornerShape(50), selected = draft.dateField == field, onClick = { draft = draft.copy(dateField = field) }, label = { Text(field.label) })
                    }
                }
                Text("Sort", style = MaterialTheme.typography.titleSmall)
                NoteSort.entries.forEach { sort ->
                    FilterChip(shape = RoundedCornerShape(50), selected = draft.sort == sort, onClick = { draft = draft.copy(sort = sort) }, label = { Text(sort.label) })
                }
            }
        },
        confirmButton = { Button(onClick = { onApply(draft) }) { Text("Apply") } },
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
                    DateRangePicker(state = picker, modifier = Modifier.weight(1f), title = { Text("Choose date range", Modifier.padding(16.dp)) },
                        headline = {
                            fun date(millis: Long?): String = millis?.let {
                                Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                            } ?: "Not selected"
                            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("From: ${date(picker.selectedStartDateMillis)}", style = MaterialTheme.typography.titleMedium)
                                Text("To: ${date(picker.selectedEndDateMillis)}", style = MaterialTheme.typography.titleMedium)
                            }
                        })
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
