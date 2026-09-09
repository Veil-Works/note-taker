package app.notetaker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
    @Test
    fun existing_note_edits_in_popup_and_returns_to_list() {
        val repository = (composeRule.activity.application as NoteTakerApplication).container.noteRepository
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
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun create_note_and_return_to_list() {
        val content = "Instrumentation capture ${System.currentTimeMillis()}"

        composeRule.onNodeWithTag("capture_field").performTextInput(content)
        composeRule.onNodeWithContentDescription("Complete note").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNode(hasText(content) and !hasSetTextAction()).assertIsDisplayed()
                composeRule.onNodeWithTag("capture_field").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
            }.isSuccess
        }
        composeRule.onNode(hasText(content) and !hasSetTextAction()).assertIsDisplayed()
        composeRule.onNodeWithTag("capture_field").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
    }

    @Test
    fun last_note_scrolls_above_capture_overlay_with_keyboard_open() {
        val repository = (composeRule.activity.application as NoteTakerApplication).container.noteRepository
        val notes = runBlocking {
            repeat(35) { index ->
                repository.upsert(Note("overlay-test-$index", "Overlay row $index", index.toLong(), index.toLong()))
            }
            repository.observeNotes().first()
        }
        composeRule.onNodeWithTag("capture_field").performTextInput("Draft\nSecond line\nThird line")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notes_list").performScrollToIndex(notes.lastIndex)
        val lastRow = composeRule.onNodeWithText(notes.last().preview())
        lastRow.assertIsDisplayed()
        val rowBounds = lastRow.getUnclippedBoundsInRoot()
        val overlayBounds = composeRule.onNodeWithTag("capture_overlay").getUnclippedBoundsInRoot()
        assertTrue("Last note must be above the floating capture field", rowBounds.bottom <= overlayBounds.top)
    }
}
