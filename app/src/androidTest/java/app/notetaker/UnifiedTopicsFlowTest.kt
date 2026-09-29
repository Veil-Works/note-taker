package app.notetaker

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.notetaker.data.RoomNoteRepository
import app.notetaker.data.local.NoteDatabase
import app.notetaker.model.Note
import app.notetaker.organization.NotebookOrganizer
import app.notetaker.organization.TextEmbedding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class UnifiedTopicsFlowTest {
    @get:Rule val rule = createComposeRule()

    @Test fun browse_merge_keep_remove_and_filter_a_discovered_topic() {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), NoteDatabase::class.java)
            .addCallback(NoteDatabase.CALLBACK).build()
        val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
        val model = object : TextEmbedding { override val version = "flow"; override fun embed(text: String) = floatArrayOf(1f, 0f) }
        val (discovered, manual) = runBlocking {
            repo.upsert(Note("a", "Orion launch", 1, 1))
            repo.upsert(Note("b", "Orion plan", 2, 2))
            NotebookOrganizer(db, model).runOnce()
            repo.observeTags().first().single() to repo.createTag("Release plans")
        }
        try {
            rule.setContent { NoteTakerApp(repo) }
            rule.onNodeWithText("Filters").performClick()
            rule.onNodeWithTag("filter_topic_${discovered.id}").performScrollTo().performClick()
            rule.onNodeWithText("Apply").performClick()
            rule.onNodeWithTag("nav_connections").performClick()
            rule.onNodeWithTag("concept_${discovered.id}").assertIsDisplayed()
            rule.onNodeWithTag("concept_${manual.id}").performClick()
            rule.onNodeWithText("Merge topic").performScrollTo().performClick()
            rule.onNodeWithTag("merge_topic_${discovered.id}").performClick()
            rule.onNodeWithText("Merge").performClick()
            rule.waitUntil(5000) { runBlocking { repo.observeTags().first().size == 1 } }
            rule.onNodeWithTag("concept_page").performScrollToNode(hasTestTag("topic_keep_a"))
            rule.onNodeWithTag("topic_keep_a").performClick()
            rule.waitUntil(5000) { runBlocking { repo.getNote("a")!!.automaticTagReasons.isEmpty() } }
            rule.onNodeWithTag("concept_page").performScrollToNode(hasTestTag("topic_remove_b"))
            rule.onNodeWithTag("topic_remove_b").performClick()
            rule.waitUntil(5000) { runBlocking { manual.id in repo.getNote("b")!!.rejectedTagIds } }
            runBlocking { assertTrue(NotebookOrganizer(db, model).runOnce()) }
            assertEquals(listOf(manual.id), runBlocking { repo.observeTags().first().map { it.id } })
            assertTrue(runBlocking { repo.getNote("b")!!.tags.isEmpty() })
            rule.onNodeWithTag("nav_notes").performClick()
            rule.onNodeWithTag("topic_chip_${manual.id}").assertIsSelected()
            rule.waitUntil(5000) { rule.onAllNodesWithTag("note_b").fetchSemanticsNodes().isEmpty() }
            rule.onNodeWithTag("note_a").assertIsDisplayed()
            rule.onNodeWithTag("note_a").performClick()
            rule.onNodeWithTag("edit_topics").performScrollTo().performClick()
            rule.onNode(hasTestTag("open_concept_${manual.id}") and hasAnyAncestor(hasTestTag("note_popup"))).performScrollTo().assertExists()
        } finally { db.close() }
    }
}
