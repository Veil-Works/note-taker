package app.notetaker

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.notetaker.data.RoomNoteRepository
import app.notetaker.data.local.NoteDatabase
import app.notetaker.model.Note
import app.notetaker.organization.*
import app.notetaker.ui.theme.NoteTakerTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AutomaticOrganizationFlowTest {
    @get:Rule val rule = createComposeRule()
    @Test fun capture_is_organized_without_tags_or_review_and_corrections_stick() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).addCallback(NoteDatabase.CALLBACK).build()
        val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
        fun organize() = runBlocking(Dispatchers.Default) { LocalTextEmbedding(context).use { assertTrue(NotebookOrganizer(db, it).runOnce()) } }
        try {
            runBlocking { repo.upsert(Note("old", "Barbell chest workout at the gym", 1, 2)) }
            rule.setContent { NoteTakerTheme { NoteTakerApp(repo) } }
            rule.onNodeWithTag("nav_new").performClick()
            rule.onNodeWithTag("capture_field").performTextInput("Bench press 80 kg, 3 sets of 5 reps")
            rule.onNodeWithTag("complete_note").performClick()
            rule.waitUntil(5_000) { runBlocking { repo.observeNotes().first().size == 2 } }
            rule.onNodeWithTag("capture_field").assertDoesNotExist()
            organize()
            val newNote = runBlocking { repo.observeNotes().first().first { it.id != "old" } }
            assertEquals(1, runBlocking { repo.observeTopics().first().size })
            rule.onNodeWithTag("note_${newNote.id}").performClick()
            rule.onNodeWithTag("edit_connections").performScrollTo().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("open_link_old").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("open_link_old").performScrollTo().performClick()
            rule.onNodeWithTag("note_popup_content").assertTextContains("Barbell chest workout at the gym")
            rule.onNodeWithText("Done").performClick()
            val tag = runBlocking { repo.createTag("Bench press") }
            organize()
            rule.onNodeWithTag("note_${newNote.id}").performClick()
            rule.onNodeWithTag("edit_topics").performScrollTo().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("reject_auto_${tag.id}").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("reject_auto_${tag.id}").performScrollTo().performClick()
            rule.onNodeWithText("Done").performClick()
            rule.waitUntil(5_000) { runBlocking { tag.id in repo.getNote(newNote.id)!!.rejectedTagIds } }
            organize()
            assertFalse(runBlocking { repo.getNote(newNote.id)!!.tags.any { it.id == tag.id } })
            rule.onNodeWithTag("nav_connections").performClick()
            rule.onNodeWithTag("concept_page").assertExists()
            val topic = runBlocking { repo.observeTopics().first().single { it.id != tag.id } }
            rule.onNodeWithTag("concept_${topic.id}").performClick()
            rule.onNodeWithText("Rename topic").performClick()
            rule.onNodeWithText("Topic name").performTextReplacement("Strength sessions")
            rule.onNodeWithText("Save topic").performClick()
            rule.waitUntil(5_000) { runBlocking { repo.observeTopics().first().single { it.id != tag.id }.name == "Strength sessions" } }
            organize()
            assertEquals("Strength sessions", runBlocking { repo.observeTopics().first().single { it.id != tag.id }.name })
        } finally { db.close() }
    }
}
