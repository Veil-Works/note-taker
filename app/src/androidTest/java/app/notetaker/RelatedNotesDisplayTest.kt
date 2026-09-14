package app.notetaker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.notetaker.model.*
import app.notetaker.ui.AutomaticOrganization
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
                    AutomaticOrganization(emptyList(), emptyMap(), notes.mapIndexed { index, n -> RelatedNote(n.id, 0.5 + index * 0.05) }, notes,
                        OrganizationStatus(), {}, {}, {}, {}, noteId = "source")
                }
            }
        }
        rule.onNodeWithTag("semantic_n8").assertExists()
        rule.onNodeWithTag("semantic_n4").assertExists()
        rule.onNodeWithTag("semantic_n3").assertDoesNotExist()
        rule.onNodeWithTag("related_show_all").performScrollTo().performClick()
        rule.onNodeWithTag("semantic_n1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("related_show_all").performScrollTo().performClick()
        rule.onNodeWithTag("semantic_n1").assertDoesNotExist()
    }
}
