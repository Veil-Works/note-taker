package app.notetaker

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.notetaker.data.RoomNoteRepository
import app.notetaker.data.local.NoteDatabase
import app.notetaker.model.Note
import app.notetaker.ui.theme.NoteTakerTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ManualTagsFlowTest {
    @get:Rule val rule = createComposeRule()

    @Test fun create_attach_filter_and_remove_manual_tag() {
        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), NoteDatabase::class.java).build()
        val repository = RoomNoteRepository(database.noteDao())
        runBlocking {
            repository.upsert(Note("one", "Project idea", 1, 2))
            repository.upsert(Note("two", "Shopping", 1, 1))
        }
        try {
            rule.setContent { NoteTakerTheme { NoteTakerApp(repository) } }
            rule.waitUntil(5_000) { rule.onAllNodesWithText("Project idea").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("Project idea").performClick()
            rule.onNodeWithText("Tags and connections").performScrollTo().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("New tag").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("New tag").performScrollTo().performTextInput("Work")
            rule.onNodeWithText("Add tag").performScrollTo().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("Work").fetchSemanticsNodes().isNotEmpty() }
            rule.onNode(hasText("Work") and isSelectable()).assertIsSelected()
            rule.onNodeWithText("New tag").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
            rule.onNodeWithText("New tag").performTextInput("Ideas")
            rule.onNodeWithText("Add tag").performScrollTo().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("Ideas").fetchSemanticsNodes().isNotEmpty() }
            rule.onNode(hasText("Ideas") and isSelectable()).assertIsSelected()
            rule.onNodeWithText("New tag").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
            rule.onNodeWithText("Done").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("Done").fetchSemanticsNodes().isEmpty() }
            rule.onNodeWithText("Tags").performClick()
            rule.onNode(hasText("Work") and isSelectable()).performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("Shopping").fetchSemanticsNodes().isEmpty() }
            rule.onNodeWithText("Project idea").assertIsDisplayed()
            rule.onNodeWithText("Project idea").performClick()
            rule.onNodeWithText("Tags and connections").performScrollTo().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("note_popup_content").fetchSemanticsNodes().isNotEmpty() }
            rule.onNode(hasText("Work") and isSelectable() and hasAnyAncestor(hasTestTag("note_popup"))).performScrollTo().performClick()
            rule.onNodeWithText("Done").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("No notes match all selected tags and search.").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("Clear tag filter").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("Shopping").fetchSemanticsNodes().isNotEmpty() }
        } finally { database.close() }
    }
}
