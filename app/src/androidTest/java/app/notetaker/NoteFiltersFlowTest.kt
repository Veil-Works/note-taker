package app.notetaker

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.notetaker.data.RoomNoteRepository
import app.notetaker.data.local.NoteDatabase
import app.notetaker.model.Note
import app.notetaker.ui.theme.NoteTakerTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class NoteFiltersFlowTest {
    @get:Rule val rule = createComposeRule()

    @Test fun combine_search_tag_dates_sort_and_capture() {
        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), NoteDatabase::class.java).build()
        val repository = RoomNoteRepository(database.noteDao())
        val today = LocalDate.now()
        val lastMonth = today.withDayOfMonth(1).minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val work = runBlocking {
            val tag = repository.createTag("Work")
            repository.upsert(Note("old", "Old budget", lastMonth, lastMonth, listOf(tag)))
            repository.upsert(Note("current", "Current budget", now, now, listOf(tag)))
            repository.upsert(Note("other", "Other budget", lastMonth, lastMonth))
            tag
        }
        try {
            rule.setContent { NoteTakerTheme { NoteTakerApp(repository) } }
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("frequent_tag_${work.id}").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("frequent_tag_${work.id}").performClick()
            rule.onNodeWithContentDescription("Search notes").performClick()
            rule.onNode(hasSetTextAction() and hasText("Search notes")).performTextInput("budget")
            rule.onNodeWithText("Filters").performClick()
            rule.onNodeWithText("Last month").performClick()
            rule.onNodeWithText("Oldest created").performScrollTo().performClick()
            rule.onNodeWithText("Apply").performClick()
            rule.waitUntil(5_000) {
                rule.onAllNodesWithText("Old budget").fetchSemanticsNodes().isNotEmpty() &&
                    rule.onAllNodesWithText("Current budget").fetchSemanticsNodes().isEmpty() &&
                    rule.onAllNodesWithText("Other budget").fetchSemanticsNodes().isEmpty()
            }
            rule.onNodeWithText("Clear filters").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("Current budget").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("Last 7 days").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("Old budget").fetchSemanticsNodes().isEmpty() }
            rule.onNodeWithText("Filters").performClick()
            rule.onNodeWithText("Last month").performClick()
            rule.onNodeWithText("Apply").performClick()
            rule.onNodeWithTag("nav_new").performClick()
            rule.onNodeWithTag("capture_field").performTextInput("New capture")
            rule.onNodeWithTag("complete_note").performClick()
            rule.waitUntil(5_000) {
                rule.onAllNodesWithText("New capture").fetchSemanticsNodes().isNotEmpty() &&
                    rule.onAllNodesWithText("Clear filters").fetchSemanticsNodes().isEmpty()
            }
            rule.onNodeWithTag("capture_field").assertDoesNotExist()
        } finally { database.close() }
    }

    @Test fun custom_calendar_range_includes_end_date() {
        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), NoteDatabase::class.java).build()
        val repository = RoomNoteRepository(database.noteDao())
        val month = LocalDate.now().withDayOfMonth(1)
        runBlocking {
            for (day in listOf(10, 11, 12)) {
                val timestamp = month.withDayOfMonth(day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                repository.upsert(Note("day$day", "Entry $day", timestamp, timestamp))
            }
        }
        try {
            rule.setContent { NoteTakerTheme { NoteTakerApp(repository) } }
            rule.onNodeWithText("Filters").performClick()
            rule.onNodeWithText("Custom range").performClick()
            val formatter = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.FULL)
            rule.onNodeWithText(formatter.format(month.withDayOfMonth(10)), substring = true).performClick()
            rule.onNodeWithText(formatter.format(month.withDayOfMonth(11)), substring = true).performClick()
            rule.onNodeWithText("Use range").performClick()
            rule.onNodeWithText("Apply").performClick()
            rule.waitUntil(5_000) {
                rule.onAllNodesWithText("Entry 10").fetchSemanticsNodes().isNotEmpty() &&
                    rule.onAllNodesWithText("Entry 11").fetchSemanticsNodes().isNotEmpty() &&
                    rule.onAllNodesWithText("Entry 12").fetchSemanticsNodes().isEmpty()
            }
        } finally { database.close() }
    }

    @Test fun calendar_range_can_be_cancelled_without_changing_filters() {
        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), NoteDatabase::class.java).build()
        try {
            rule.setContent { NoteTakerTheme { NoteTakerApp(RoomNoteRepository(database.noteDao())) } }
            rule.onNodeWithText("Filters").performClick()
            rule.onNodeWithText("Custom range").performClick()
            rule.onNodeWithText("Choose date range").assertExists()
            rule.onNodeWithText("Use range").assertIsNotEnabled()
            rule.onNodeWithText("Cancel range").performClick()
            rule.onNodeWithText("Cancel").performClick()
            rule.onNodeWithText("Clear filters").assertDoesNotExist()
        } finally { database.close() }
    }
}
