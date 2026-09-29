package app.notetaker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.notetaker.model.*
import app.notetaker.ui.NoteLinks
import app.notetaker.ui.theme.NoteTakerTheme
import org.junit.Rule
import org.junit.Test

class RelatedNotesDisplayTest {
    @get:Rule val rule = createComposeRule()
    @Test fun editor_shows_five_strongest_and_can_expand_every_connection() {
        val notes = (1..8).map { Note("n$it", "Thought $it", 1, 1) }
        rule.setContent {
            NoteTakerTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    NoteLinks("source", notes + Note("source", "Source", 1, 1, linkedNoteIds = setOf("n8")), emptySet(), true, null, { _, _ -> }, {},
                        related = notes.mapIndexed { index, n -> RelatedNote(n.id, 0.5 + index * 0.05) } + RelatedNote("n8", .9))
                }
            }
        }
        rule.onAllNodesWithTag("open_link_n8").assertCountEquals(1)
        rule.onNodeWithText("Related notes").assertDoesNotExist()
        rule.onNodeWithText("Connected notes").assertDoesNotExist()
        rule.onNodeWithTag("open_link_n4").assertExists()
        rule.onNodeWithTag("open_link_n3").assertDoesNotExist()
        rule.onNodeWithTag("connections_show_all").performScrollTo().performClick()
        rule.onNodeWithTag("open_link_n1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("connections_show_all").performScrollTo().performClick()
        rule.onNodeWithTag("open_link_n1").assertDoesNotExist()
    }
}
