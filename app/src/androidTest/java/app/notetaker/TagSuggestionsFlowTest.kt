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
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TagSuggestionsFlowTest {
    @get:Rule val rule = createComposeRule()

    @Test fun configure_accept_dismiss_and_reevaluate_saved_edits() {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), NoteDatabase::class.java).build()
        val repo = RoomNoteRepository(db.noteDao())
        val strength = runBlocking {
            repo.upsert(Note("one", "Bench press today", 1, 2))
            repo.createTag("Strength")
        }
        try {
            rule.setContent { NoteTakerTheme { NoteTakerApp(repo) } }
            waitForText("Bench press today")
            rule.onNodeWithText("Bench press today").performClick()
            rule.onNodeWithText("Tags and connections").performScrollTo().performClick()
            waitForText("Manage tag keywords")
            rule.onNodeWithText("Manage tag keywords").performScrollTo().performClick()
            rule.onNode(hasText("Strength") and isSelectable() and hasAnyAncestor(hasTestTag("tag_keywords_dialog"))).performClick()
            rule.onNodeWithText("New keyword or phrase").performScrollTo().performTextInput("bench press")
            rule.onNodeWithText("Add keyword").performScrollTo().performClick()
            waitForText("Keyword for Strength")
            rule.onNodeWithText("Keyword for Strength").performScrollTo().performTextReplacement("BENCH PRESS")
            rule.onNodeWithText("Save keyword").performScrollTo().performClick()
            rule.onNode(hasText("Done") and hasAnyAncestor(hasTestTag("tag_keywords_dialog"))).performClick()
            waitForSuggestion()
            rule.onNodeWithText("Dismiss Strength").performScrollTo().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("suggestion_Strength").fetchSemanticsNodes().isEmpty() }
            rule.onNodeWithText("Done").performScrollTo().performClick()
            waitForText("Bench press today")
            rule.onNodeWithText("Bench press today").performClick()
            rule.onNodeWithText("Tags and connections").performScrollTo().performClick()
            waitForText("Manage tag keywords")
            rule.onNodeWithTag("suggestion_Strength").assertDoesNotExist()
            rule.onNodeWithTag("note_popup_content").performScrollTo().performTextReplacement("Bench press tomorrow")
            waitForSuggestion()
            rule.onNodeWithTag("suggestion_Strength").performScrollTo().performClick()
            rule.onNodeWithTag("note_popup_content").performScrollTo()
            rule.onNode(hasText("Strength") and isSelectable()).performScrollTo().assertIsSelected()
            rule.onNodeWithText("Done").performScrollTo().performClick()
            rule.waitUntil(5_000) { runBlocking { repo.getNote("one")!!.tags.any { it.id == strength.id } } }
            assertEquals("Bench press tomorrow", runBlocking { repo.getNote("one")!!.content })
        } finally { db.close() }
    }

    @Test fun saved_run_note_suggests_runs_tag() {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), NoteDatabase::class.java).build()
        val repo = RoomNoteRepository(db.noteDao())
        val tag = runBlocking { repo.createTag("runs") }
        try {
            rule.setContent { NoteTakerTheme { NoteTakerApp(repo) } }
            rule.onNodeWithTag("capture_field").performTextInput("run 7km thursday")
            rule.onNodeWithContentDescription("Complete note").performClick()
            rule.waitUntil(5_000) { rule.onAllNodes(hasText("run 7km thursday") and !hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
            rule.onNode(hasText("run 7km thursday") and !hasSetTextAction()).performClick()
            rule.onNodeWithText("Tags and connections").performScrollTo().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("suggestion_runs").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("suggestion_runs").performScrollTo().performClick()
            rule.onNodeWithText("Done").performScrollTo().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("Done").fetchSemanticsNodes().isEmpty() }
            assertTrue(runBlocking { repo.observeNotes().first().single().tags.contains(tag) })
        } finally { db.close() }
    }

    private fun waitForText(text: String) = rule.waitUntil(5_000) {
        rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
    private fun waitForSuggestion() = rule.waitUntil(5_000) {
        rule.onAllNodesWithTag("suggestion_Strength").fetchSemanticsNodes().isNotEmpty()
    }
}
