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

class ConceptsFlowTest {
    @get:Rule val rule = createComposeRule()

    @Test fun rename_topic_accept_alias_and_browse_old_and_new_notes() {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), NoteDatabase::class.java)
            .addCallback(NoteDatabase.CALLBACK).build()
        val repo = RoomNoteRepository(db.noteDao())
        val tag = runBlocking {
            val tag = repo.createTag("Bench press")
            repo.upsert(Note("old", "Old training log", 1, 2, listOf(tag)))
            repo.upsert(Note("new", "barbell bench press today", 3, 4))
            repo.saveAlias(tag.id, null, "barbell bench press")
            tag
        }
        try {
            rule.setContent { NoteTakerTheme { NoteTakerApp(repo) } }
            waitFor("Topics")
            rule.onNodeWithTag("nav_connections").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("concept_${tag.id}").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("concept_${tag.id}").performScrollTo().performClick()
            rule.onNodeWithText("Rename topic").performScrollTo().performClick()
            rule.onNodeWithText("Type").assertDoesNotExist()
            rule.onNodeWithText("Types").assertDoesNotExist()
            rule.onNodeWithText("Topic name").performTextReplacement("Strength")
            rule.onNodeWithText("Save topic").performClick()
            rule.waitUntil(5_000) { runBlocking { repo.getNote("old")!!.tags.single().name == "Strength" } }
            rule.onNodeWithText("New alias").assertDoesNotExist()
            rule.onNodeWithText("Aliases").assertDoesNotExist()
            rule.onNodeWithTag("concept_note_old").performScrollTo().assertExists()
            rule.onNodeWithText("Back").performClick()
            rule.onNodeWithTag("nav_notes").performClick()
            rule.onNodeWithTag("note_new").performScrollTo().performClick()
            rule.onNodeWithTag("edit_topics").performScrollTo().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("suggestion_Strength").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("suggestion_Strength").performScrollTo().assertTextContains("Strength").performClick()
            rule.onNode(hasTestTag("open_concept_${tag.id}") and hasAnyAncestor(hasTestTag("note_popup"))).performScrollTo().performClick()
            rule.waitUntil(5_000) { runBlocking { repo.getNote("new")!!.tags.any { it.id == tag.id } } }
            rule.onNodeWithTag("concept_page").performScrollToNode(hasTestTag("concept_note_old"))
            rule.onNodeWithTag("concept_note_old").assertExists()
            rule.onNodeWithTag("concept_page").performScrollToNode(hasTestTag("concept_note_new"))
            rule.onNodeWithTag("concept_note_new").performScrollTo().assertExists()
            rule.onNodeWithTag("concept_page").performScrollToNode(hasTestTag("concept_note_old"))
            rule.onNodeWithTag("concept_note_old").performClick()
            rule.onNodeWithTag("note_popup_content").assertTextContains("Old training log")
        } finally { db.close() }
    }
    private fun waitFor(text: String) = rule.waitUntil(5_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
}
