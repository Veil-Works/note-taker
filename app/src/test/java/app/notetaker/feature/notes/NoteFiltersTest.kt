package app.notetaker.feature.notes

import app.notetaker.model.Note
import app.notetaker.model.Tag
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteFiltersTest {
    private val zone = ZoneId.of("Europe/Prague")
    private val today = LocalDate.of(2026, 9, 9)
    private fun time(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli()
    private fun apply(notes: List<Note>, filters: NoteFilters, tag: String? = null) =
        filterAndSortNotes(notes, tag, filters, today, zone).map(Note::id)

    @Test fun `custom range includes full local day across daylight saving change`() {
        val day = LocalDate.of(2026, 3, 29)
        val start = time(day)
        val end = time(day.plusDays(1))
        assertEquals(23 * 60 * 60 * 1000L, end - start)
        val notes = listOf(start - 1, start, end - 1, end).mapIndexed { index, timestamp -> Note("$index", "Note", timestamp, timestamp) }
        assertEquals(listOf("2", "1"), apply(notes, NoteFilters(period = DatePeriod.CUSTOM, start = day, end = day)))
    }

    @Test fun `last month means previous calendar month including year and leap transitions`() {
        val filters = NoteFilters(period = DatePeriod.LAST_MONTH)
        assertEquals(LocalDate.of(2025, 12, 1) to LocalDate.of(2025, 12, 31), filters.dateRange(LocalDate.of(2026, 1, 9)))
        assertEquals(LocalDate.of(2024, 2, 1) to LocalDate.of(2024, 2, 29), filters.dateRange(LocalDate.of(2024, 3, 3)))
    }

    @Test fun `recent contains today and six previous calendar days`() {
        val notes = listOf(-7L, -6L, 0L, 1L).map { offset -> Note("$offset", "Note", time(today.plusDays(offset)), time(today.plusDays(offset))) }
        assertEquals(listOf("0", "-6"), apply(notes, NoteFilters(period = DatePeriod.RECENT)))
        assertEquals(listOf("0"), apply(notes, NoteFilters(period = DatePeriod.TODAY)))
    }

    @Test fun `date field and tag both constrain the result`() {
        val tag = Tag("running", "Running", 0)
        val old = time(today.minusMonths(1))
        val recent = time(today)
        val notes = listOf(Note("a", "Plan", old, recent, listOf(tag)), Note("b", "Log", recent, recent))
        assertEquals(listOf("a"), apply(notes, NoteFilters(period = DatePeriod.TODAY), tag.id))
        assertEquals(emptyList<String>(), apply(notes, NoteFilters(period = DatePeriod.TODAY, dateField = DateField.CREATED), tag.id))
    }

    @Test fun `sort modes use correct fields with stable ties`() {
        val notes = listOf(Note("c", "alpha", 3, 1), Note("b", "Bravo", 2, 3), Note("a", "Alpha", 1, 2))
        assertEquals(listOf("b", "a", "c"), apply(notes, NoteFilters()))
        assertEquals(listOf("c", "b", "a"), apply(notes, NoteFilters(sort = NoteSort.NEWEST)))
        assertEquals(listOf("a", "b", "c"), apply(notes, NoteFilters(sort = NoteSort.OLDEST)))
        assertEquals(listOf("a", "c", "b"), apply(notes, NoteFilters(sort = NoteSort.ALPHABETICAL)))
    }
}
