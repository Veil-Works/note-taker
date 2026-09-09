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

class BulkNotesFlowTest {
    @get:Rule val rule = createComposeRule()

    @Test fun select_tag_connect_filter_and_delete() {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), NoteDatabase::class.java).build()
        val repo = RoomNoteRepository(db.noteDao())
        val tags = runBlocking {
            val a = repo.createTag("Work")
            val b = repo.createTag("Project")
            repo.upsert(Note("a", "First", 1, 3, listOf(a)))
            repo.upsert(Note("b", "Second", 1, 2))
            repo.upsert(Note("c", "Outside", 1, 1, listOf(a)))
            a to b
        }
        try {
            rule.setContent { NoteTakerTheme { NoteTakerApp(repo) } }
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("note_a").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("note_a").performTouchInput { longClick() }
            rule.onNodeWithTag("note_b").performClick()
            rule.onNodeWithText("2 selected").assertExists()
            rule.onNodeWithText("Tag").performClick()
            rule.onNodeWithText("Project").performClick()
            rule.onNodeWithText("Apply tags").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("2 selected").fetchSemanticsNodes().isEmpty() }
            runBlocking { assertEquals(setOf(tags.first.id, tags.second.id), repo.getNote("a")!!.tags.map { it.id }.toSet()) }
            rule.onNodeWithText("Select").performClick()
            rule.onNodeWithTag("note_a").performClick()
            rule.onNodeWithTag("note_b").performClick()
            rule.onNodeWithText("Connect").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("2 selected").fetchSemanticsNodes().isEmpty() }
            runBlocking { assertEquals(setOf("b"), repo.getNote("a")!!.linkedNoteIds) }
            rule.onNodeWithTag("frequent_tag_${tags.first.id}").performClick()
            rule.onNodeWithTag("frequent_tag_${tags.second.id}").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("note_b").fetchSemanticsNodes().isEmpty() && rule.onAllNodesWithTag("note_c").fetchSemanticsNodes().isEmpty() }
            rule.onNodeWithTag("note_a").assertExists()
            rule.onNodeWithTag("delete_tag_${tags.second.id}").performClick()
            rule.onNodeWithText("Delete tag").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("note_c").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("Clear filters").performClick()
            rule.onNodeWithText("Select").performClick()
            rule.onNodeWithTag("note_a").performClick()
            rule.onNodeWithTag("note_b").performClick()
            rule.onNodeWithText("Delete", useUnmergedTree = true).performClick()
            rule.onNodeWithText("Cancel").performClick()
            rule.onNodeWithText("2 selected").assertExists()
            rule.onNodeWithText("Delete", useUnmergedTree = true).performClick()
            rule.onNodeWithText("Delete notes").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("note_a").fetchSemanticsNodes().isEmpty() }
            rule.onNodeWithTag("note_c").assertExists()
        } finally { db.close() }
    }

    @Test fun bulk_tagging_shows_all_and_partial_tags_and_removes_only_explicit_choices() {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), NoteDatabase::class.java).build()
        val repo = RoomNoteRepository(db.noteDao())
        val tags = runBlocking {
            val common = repo.createTag("Common")
            val partial = repo.createTag("Partial")
            repo.upsert(Note("a", "First", 1, 3, listOf(common, partial)))
            repo.upsert(Note("b", "Second", 1, 2, listOf(common)))
            repo.upsert(Note("c", "Outside", 1, 1, listOf(common, partial)))
            common to partial
        }
        fun chooseBoth() {
            rule.onNodeWithText("Select").performClick()
            rule.onNodeWithTag("note_a").performClick()
            rule.onNodeWithTag("note_b").performClick()
            rule.onNodeWithText("Tag").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("Tag selected notes").fetchSemanticsNodes().isNotEmpty() }
        }
        fun assertTag(id: String, state: androidx.compose.ui.state.ToggleableState) {
            rule.onNodeWithTag("bulk_tag_$id").assert(SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.ToggleableState, state))
        }
        try {
            rule.setContent { NoteTakerTheme { NoteTakerApp(repo) } }
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("note_a").fetchSemanticsNodes().isNotEmpty() }
            chooseBoth()
            assertTag(tags.first.id, androidx.compose.ui.state.ToggleableState.On)
            assertTag(tags.second.id, androidx.compose.ui.state.ToggleableState.Indeterminate)
            rule.onNodeWithText("1 of 2 notes").assertExists()
            rule.onNodeWithText("Apply tags").assertIsNotEnabled()
            rule.onNodeWithTag("bulk_tag_${tags.first.id}").performClick()
            rule.onNodeWithText("Apply tags").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("2 selected").fetchSemanticsNodes().isEmpty() }
            runBlocking {
                assertEquals(listOf(tags.second), repo.getNote("a")!!.tags)
                assertEquals(emptyList<app.notetaker.model.Tag>(), repo.getNote("b")!!.tags)
                assertEquals(setOf(tags.first, tags.second), repo.getNote("c")!!.tags.toSet())
            }
            chooseBoth()
            assertTag(tags.first.id, androidx.compose.ui.state.ToggleableState.Off)
            assertTag(tags.second.id, androidx.compose.ui.state.ToggleableState.Indeterminate)
            rule.onNodeWithTag("bulk_tag_${tags.second.id}").performClick()
            assertTag(tags.second.id, androidx.compose.ui.state.ToggleableState.On)
            rule.onNodeWithTag("bulk_tag_${tags.second.id}").performClick()
            assertTag(tags.second.id, androidx.compose.ui.state.ToggleableState.Off)
            rule.onNodeWithText("Apply tags").assertIsEnabled().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("2 selected").fetchSemanticsNodes().isEmpty() }
            runBlocking {
                assertEquals(emptyList<app.notetaker.model.Tag>(), repo.getNote("a")!!.tags)
                assertEquals(listOf(tags.first, tags.second), repo.getNote("c")!!.tags)
            }
        } finally { db.close() }
    }

    @Test fun automatic_connections_can_be_made_permanent() {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), NoteDatabase::class.java).build()
        val repo = RoomNoteRepository(db.noteDao())
        val tag = runBlocking {
            val t = repo.createTag("Shared")
            repo.upsert(Note("a", "Plan", 1, 2, listOf(t)))
            repo.upsert(Note("b", "Log", 1, 1, listOf(t)))
            t
        }
        try {
            rule.setContent { NoteTakerTheme { NoteTakerApp(repo) } }
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("note_a").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("note_a").performClick()
            rule.onNodeWithTag("open_link_b").performScrollTo().assertExists()
            runBlocking { repo.upsert(repo.getNote("b")!!.copy(tags = emptyList())) }
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("open_link_b").fetchSemanticsNodes().isEmpty() }
            runBlocking { repo.upsert(repo.getNote("b")!!.copy(tags = listOf(tag))) }
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("keep_link_b").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("keep_link_b").performScrollTo().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("remove_link_b").fetchSemanticsNodes().isNotEmpty() }
            runBlocking { repo.upsert(repo.getNote("b")!!.copy(tags = emptyList())) }
            rule.onNodeWithTag("open_link_b").assertExists()
            rule.onNodeWithTag("remove_link_b").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("open_link_b").fetchSemanticsNodes().isEmpty() }
            rule.onNodeWithText("Done").performScrollTo().performClick()
        } finally { db.close() }
    }
}
