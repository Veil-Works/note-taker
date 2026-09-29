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
            rule.onNodeWithTag("edit_topics").performScrollTo().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("New topic").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("New topic").performScrollTo().performTextInput("Work")
            rule.onNodeWithText("Add topic").performScrollTo().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("Work").fetchSemanticsNodes().isNotEmpty() }
            rule.onNode(hasText("Work") and isSelectable()).assertIsSelected()
            rule.onNodeWithText("New topic").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
            rule.onNodeWithText("New topic").performTextInput("Ideas")
            rule.onNodeWithText("Add topic").performScrollTo().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("Ideas").fetchSemanticsNodes().isNotEmpty() }
            rule.onNode(hasText("Ideas") and isSelectable()).assertIsSelected()
            rule.onNodeWithText("New topic").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
            rule.onNodeWithText("Done").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("Done").fetchSemanticsNodes().isEmpty() }
            rule.onNodeWithText("Filters").performClick()
            rule.onNode(hasText("Work") and isSelectable()).performScrollTo().performClick()
            rule.onNodeWithText("Apply").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("Shopping").fetchSemanticsNodes().isEmpty() }
            rule.onNodeWithText("Project idea").assertIsDisplayed()
            rule.onNodeWithText("Project idea").performClick()
            rule.onNodeWithTag("edit_topics").performScrollTo().performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("New topic").fetchSemanticsNodes().isNotEmpty() }
            rule.onNode(hasText("Work") and isSelectable() and hasAnyAncestor(hasTestTag("note_popup"))).performScrollTo().performClick()
            rule.onNodeWithText("Done").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("No notes match all selected topics and search.").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("Clear topic filter").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("Shopping").fetchSemanticsNodes().isNotEmpty() }
        } finally { database.close() }
    }
}
