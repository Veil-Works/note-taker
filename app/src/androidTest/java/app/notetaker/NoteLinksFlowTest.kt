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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NoteLinksFlowTest {
    @get:Rule val rule = createComposeRule()

    @Test fun connect_search_navigate_edit_and_remove() {
        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), NoteDatabase::class.java).build()
        val repository = RoomNoteRepository(database.noteDao())
        runBlocking {
            val tag = repository.createTag("Running")
            repository.upsert(Note("a", "Training plan", 1, 2))
            repository.upsert(Note("b", "Morning log", 1, 1, listOf(tag)))
            repository.upsert(Note("c", "Shopping", 1, 3))
        }
        try {
            rule.setContent { NoteTakerTheme { NoteTakerApp(repository) } }
            rule.waitUntil(5_000) { rule.onAllNodesWithText("Training plan").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("Training plan").performClick()
            rule.onNodeWithText("Connect a note").performScrollTo().performClick()
            rule.onNodeWithTag("connect_a").assertDoesNotExist()
            rule.onNodeWithTag("link_search").performTextInput("Running")
            rule.onNodeWithTag("connect_c").assertDoesNotExist()
            rule.onNodeWithTag("connect_b").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("connect_b").fetchSemanticsNodes().isEmpty() }
            rule.onNodeWithText("Done connecting").performClick()
            rule.onNodeWithTag("note_popup_content").performScrollTo().performTextReplacement("Updated training plan")
            rule.onNodeWithTag("open_link_b").performScrollTo().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("open_link_a").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("open_link_a").performScrollTo().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("open_link_b").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("note_popup_content").assertTextEquals("Updated training plan")
            rule.onNodeWithTag("remove_link_b").performScrollTo().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("open_link_b").fetchSemanticsNodes().isEmpty() }
            runBlocking {
                assertEquals(emptySet<String>(), repository.getNote("b")!!.linkedNoteIds)
            }
            rule.onNodeWithText("Done").performScrollTo().performClick()
        } finally { database.close() }
    }
}
