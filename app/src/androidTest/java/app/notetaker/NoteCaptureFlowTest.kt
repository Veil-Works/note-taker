package app.notetaker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollTo
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.notetaker.data.RoomNoteRepository
import app.notetaker.data.local.NoteDatabase
import app.notetaker.ui.theme.NoteTakerTheme
import org.junit.Before
import org.junit.After
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import app.notetaker.model.Note
import app.notetaker.model.preview
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NoteCaptureFlowTest {
    @Test fun capture_prefixes_and_focused_detail_panels() {
        runBlocking { repository.createTag("Atlas") }
        composeRule.onNodeWithTag("nav_new").performClick()
        composeRule.onNodeWithTag("capture_field").performTextInput("#ATLAS #orion")
        composeRule.onNodeWithTag("complete_note").performClick()
        composeRule.onNodeWithText("Write a note after the topic names.").assertIsDisplayed()
        composeRule.onNodeWithTag("capture_field").performTextInput(" Review release")
        composeRule.onNodeWithTag("complete_note").performClick()
        composeRule.waitUntil(5_000) { runBlocking { repository.observeNotes().first().size == 1 } }
        val note = runBlocking { repository.observeNotes().first().single() }
        org.junit.Assert.assertEquals("Review release", note.content)
        org.junit.Assert.assertEquals(setOf("Atlas", "orion"), note.tags.map { it.name }.toSet())
        composeRule.onNodeWithTag("note_${note.id}").performClick()
        composeRule.onNodeWithText("New topic").assertDoesNotExist()
        composeRule.onNodeWithText("Connect a note").assertDoesNotExist()
        composeRule.onNodeWithTag("edit_topics").performClick()
        composeRule.onNodeWithText("New topic").assertExists()
        composeRule.onNodeWithText("Connect a note").assertDoesNotExist()
        composeRule.onNodeWithTag("back_to_note").performClick()
        composeRule.onNodeWithTag("note_popup_content").assertTextEquals("Review release")
        composeRule.onNodeWithTag("edit_connections").performClick()
        composeRule.onNodeWithText("New topic").assertDoesNotExist()
        composeRule.onNodeWithText("Connect a note").assertExists()
        composeRule.onNodeWithText("Done").performClick()
    }
    @Test
    fun existing_note_edits_in_popup_and_returns_to_list() {
        val now = System.currentTimeMillis()
        val note = Note(java.util.UUID.randomUUID().toString(), "Popup check $now", now, now)
        runBlocking { repository.upsert(note) }
        try {
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("notes_list").performScrollToNode(hasText(note.content))
            composeRule.waitUntil(5_000) {
                composeRule.onAllNodesWithText(note.content).fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText(note.content).performClick()
            composeRule.onNodeWithTag("note_popup").assertIsDisplayed()
            composeRule.onNodeWithTag("notes_list").assertExists()
            composeRule.waitUntil(5_000) {
                composeRule.onAllNodesWithText(note.content).fetchSemanticsNodes().size >= 2
            }
            composeRule.onNodeWithTag("note_popup_content").performTextReplacement("Updated $now")
            composeRule.onNodeWithText("Done").performClick()
            composeRule.waitUntil(5_000) {
                composeRule.onAllNodesWithText("Updated $now").fetchSemanticsNodes().isNotEmpty()
                    && composeRule.onAllNodesWithText("Done").fetchSemanticsNodes().isEmpty()
            }
            composeRule.onNodeWithText("Updated $now").assertIsDisplayed()
        } finally {
            runBlocking { repository.delete(note) }
        }
    }

    @get:Rule
    val composeRule = createComposeRule()
    private lateinit var database: NoteDatabase
    private lateinit var repository: RoomNoteRepository

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), NoteDatabase::class.java).build()
        repository = RoomNoteRepository(database.noteDao())
        composeRule.setContent { NoteTakerTheme { NoteTakerApp(repository) } }
    }

    @After fun tearDown() = database.close()

    @Test
    fun create_note_and_return_to_list() {
        val content = "Instrumentation capture ${System.currentTimeMillis()}"

        composeRule.onNodeWithTag("nav_new").performClick()

        composeRule.onNodeWithTag("capture_field").performTextInput(content)
        composeRule.onNodeWithTag("complete_note").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNode(hasText(content) and !hasSetTextAction()).assertIsDisplayed()
                composeRule.onNodeWithTag("capture_field").assertDoesNotExist()
            }.isSuccess
        }
        composeRule.onNode(hasText(content) and !hasSetTextAction()).assertIsDisplayed()
        composeRule.onNodeWithTag("capture_field").assertDoesNotExist()
    }

    @Test
    fun last_note_scrolls_above_floating_navigation() {
        val notes = runBlocking {
            repeat(35) { index ->
                repository.upsert(Note("overlay-test-$index", "Overlay row $index", index.toLong(), index.toLong()))
            }
            repository.observeNotes().first()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notes_list").performScrollToIndex(notes.lastIndex)
        val lastRow = composeRule.onNodeWithText(notes.last().preview())
        lastRow.assertIsDisplayed()
        val rowBounds = lastRow.getUnclippedBoundsInRoot()
        val overlayBounds = composeRule.onNodeWithTag("floating_navigation").getUnclippedBoundsInRoot()
        assertTrue("Last note must be above the floating capture field", rowBounds.bottom <= overlayBounds.top)
    }
}
