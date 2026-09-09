package app.notetaker.feature.notes

import app.notetaker.model.Note
import app.notetaker.model.preview
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

enum class NoteSort(val label: String) {
    UPDATED("Recently edited"), NEWEST("Newest created"), OLDEST("Oldest created"), ALPHABETICAL("Alphabetical")
}

enum class DatePeriod(val label: String) {
    ALL("Any time"), TODAY("Today"), RECENT("Last 7 days"), LAST_MONTH("Last month"), CUSTOM("Custom range")
}

enum class DateField(val label: String) { UPDATED("Edited"), CREATED("Created") }

data class NoteFilters(
    val sort: NoteSort = NoteSort.UPDATED,
    val period: DatePeriod = DatePeriod.ALL,
    val dateField: DateField = DateField.UPDATED,
    val start: LocalDate? = null,
    val end: LocalDate? = null,
) {
    fun dateRange(today: LocalDate): Pair<LocalDate?, LocalDate?> = when (period) {
        DatePeriod.ALL -> null to null
        DatePeriod.TODAY -> today to today
        DatePeriod.RECENT -> today.minusDays(6) to today
        DatePeriod.LAST_MONTH -> today.withDayOfMonth(1).minusMonths(1) to today.withDayOfMonth(1).minusDays(1)
        DatePeriod.CUSTOM -> start to end
    }
}

/** Calendar dates are inclusive in the device zone, including days with DST changes. */
internal fun filterAndSortNotes(
    notes: List<Note>, tagId: String?, filters: NoteFilters, today: LocalDate, zone: ZoneId,
): List<Note> {
    val (start, end) = filters.dateRange(today)
    val from = start?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
    val until = end?.plusDays(1)?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
    val comparator = when (filters.sort) {
        NoteSort.UPDATED -> compareByDescending<Note> { it.updatedAt }
        NoteSort.NEWEST -> compareByDescending<Note> { it.createdAt }
        NoteSort.OLDEST -> compareBy<Note> { it.createdAt }
        NoteSort.ALPHABETICAL -> compareBy<Note> { it.preview().lowercase(Locale.ROOT) }
    }.thenBy { it.id }
    return notes.filter { note ->
        val timestamp = if (filters.dateField == DateField.UPDATED) note.updatedAt else note.createdAt
        (tagId == null || note.tags.any { it.id == tagId }) &&
            (from == null || timestamp >= from) && (until == null || timestamp < until)
    }.sortedWith(comparator)
}
