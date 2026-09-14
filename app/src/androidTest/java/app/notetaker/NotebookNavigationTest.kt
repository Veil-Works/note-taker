package app.notetaker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.notetaker.data.RoomNoteRepository
import app.notetaker.data.local.NoteDatabase
import app.notetaker.model.Note
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test

class NotebookNavigationTest {
    @get:Rule val rule = createComposeRule()
    @Test fun outside_taps_discard_edits_and_new_drafts() {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), NoteDatabase::class.java).build()
        val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
        val original = Note("one", "Original thought", 1, 1)
        runBlocking { repo.upsert(original) }
        try {
            rule.setContent { NoteTakerApp(repo) }
            rule.onNodeWithText("Original thought").performClick()
            rule.onNodeWithTag("note_popup_content").performTextReplacement("Discard these edits")
            // Discard must also undo content already persisted by the debounce timer.
            rule.waitUntil(5000) { runBlocking { repo.getNote("one")?.content == "Discard these edits" } }
            rule.onNodeWithTag("note_window_backdrop").performTouchInput { click(androidx.compose.ui.geometry.Offset(4f, 4f)) }
            rule.waitUntil(5000) { rule.onAllNodesWithTag("note_popup").fetchSemanticsNodes().isEmpty() }
            org.junit.Assert.assertEquals(original, runBlocking { repo.getNote("one") })
            rule.onNodeWithTag("nav_new").performClick()
            rule.onNodeWithTag("capture_field").performTextInput("Discard this draft")
            rule.onNodeWithTag("note_window_backdrop").performTouchInput { click(androidx.compose.ui.geometry.Offset(4f, 4f)) }
            rule.waitUntil(5000) { rule.onAllNodesWithTag("capture_field").fetchSemanticsNodes().isEmpty() }
            rule.onNodeWithTag("nav_new").performClick()
            rule.onNodeWithTag("capture_field").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
            org.junit.Assert.assertEquals(1, runBlocking { repo.observeNotes().first().size })
        } finally { db.close() }
    }

    @Test fun all_destinations_and_capture_from_graph_work() {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),NoteDatabase::class.java).build()
        val repo = RoomNoteRepository(db.noteDao(),db.organizationDao())
        runBlocking { repo.upsert(Note("one","A connected thought",1,1)) }
        try {
            rule.setContent { NoteTakerApp(repo) }
            rule.onNodeWithTag("nav_graph").performClick()
            rule.onNodeWithTag("graph_view").assertIsDisplayed()
            rule.onNodeWithTag("floating_navigation").assertIsDisplayed()
            rule.onNodeWithText("Browse notes").performClick()
            rule.onNodeWithText("A connected thought").performClick()
            rule.onNodeWithText("A connected thought").performClick()
            rule.onNodeWithTag("note_popup").assertIsDisplayed()
            rule.onNodeWithText("Done").performClick()
            rule.onNodeWithTag("nav_connections").performClick()
            rule.onNodeWithText("Tags & types").assertIsDisplayed()
            rule.onNodeWithText("Automatic topics").performClick()
            rule.onNodeWithTag("floating_navigation").assertIsDisplayed()
            rule.onNodeWithTag("nav_settings").performClick()
            rule.onNodeWithText("Appearance").assertIsDisplayed()
            rule.onNodeWithTag("nav_new").performClick()
            rule.onNodeWithTag("capture_field").performTextInput("One short finished thought")
            rule.onNodeWithTag("complete_note").performClick()
            rule.waitUntil(5000) { rule.onAllNodesWithText("One short finished thought").fetchSemanticsNodes().isNotEmpty() && rule.onAllNodesWithTag("capture_field").fetchSemanticsNodes().isEmpty() }
            rule.onNodeWithText("One short finished thought").assertIsDisplayed()
            rule.onNodeWithTag("nav_new").performClick()
            rule.onNodeWithTag("capture_field").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
        } finally { db.close() }
    }
}
