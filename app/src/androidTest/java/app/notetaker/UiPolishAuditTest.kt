package app.notetaker

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.notetaker.data.RoomNoteRepository
import app.notetaker.model.Note
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import app.notetaker.data.local.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/** Real-device visual coverage with disposable notes; never touches the owner's notebook. */
@RunWith(Parameterized::class)
class UiPolishAuditTest(private val mode: String) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun configurations() = listOf(arrayOf<Any>("Dark"), arrayOf<Any>("Light"))
    }
    @get:Rule val rule = createComposeRule()

    @Test fun screen_and_dialog_walkthrough() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
        val oldMode = prefs.getString("mode", null)
        val oldCompact = prefs.getBoolean("compact", false)
        prefs.edit().putString("mode", mode).putBoolean("compact", false).commit()
        val db = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).addCallback(NoteDatabase.CALLBACK).build()
        val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
        val tag = runBlocking {
            val tag = repo.createTag("Weekend garden and balcony planning")
            repo.upsert(Note("audit-a", "Make room for small things.\nPlant rosemary on the balcony and leave Sunday afternoon free.", System.currentTimeMillis(), System.currentTimeMillis(), listOf(tag)))
            repo.upsert(Note("audit-b", "Pick up rosemary and a terracotta pot for the balcony.", 1, 1, listOf(tag)))
            repo.upsert(Note("audit-c", "A quiet morning with a book and coffee.", 2, 2))
            repo.linkNotes("audit-a", "audit-b")
            db.organizationDao().insertTopics(listOf(TopicEntity("garden", "Weekend garden and balcony planning"), TopicEntity("quiet", "Quiet mornings")))
            db.organizationDao().insertTopicNotes(listOf(TopicNoteEntity("garden", "audit-a"), TopicNoteEntity("garden", "audit-b"), TopicNoteEntity("quiet", "audit-c")))
            db.organizationDao().insertSemanticLinks(listOf(SemanticLinkEntity("audit-a", "audit-b", .8)))
            db.organizationDao().complete(db.organizationDao().state()!!.generation)
            tag
        }
        try {
            rule.setContent {
                NoteTakerApp(repo)
            }
            rule.onNodeWithTag("note_audit-a").assertIsDisplayed()
            shot("01-list")
            click("Filters"); shot("02-filters")
            click("Custom range"); shot("03-date-range")
            click("Cancel range"); click("Cancel")
            click("Select"); rule.onNodeWithTag("note_audit-a").performClick(); shot("04-selection")
            click("Tag"); shot("05-bulk-tags"); click("Cancel")
            click("Delete"); shot("06-delete-confirmation"); click("Cancel")
            rule.onNodeWithContentDescription("Cancel selection").performClick()
            rule.onNodeWithTag("note_audit-a").performClick(); shot("07-editor")
            rule.onNodeWithText("Done").assertIsDisplayed()
            rule.onNodeWithText("Tags and connections").performScrollTo().performClick(); shot("08-editor-tools")
            rule.onNodeWithText("Manage tag keywords").performScrollTo().performClick(); shot("09-keywords")
            // The dialog and editor both have Done, so select the visible dialog's node.
            rule.onAllNodesWithText("Done").onLast().performClick()
            rule.onNodeWithText("Connect a note").performScrollTo().performClick(); shot("10-connect-note")
            click("Done connecting"); click("Done")
            rule.onNodeWithTag("nav_graph").performClick(); shot("11-graph")
            click("Browse notes"); shot("12-graph-browser")
            rule.onNodeWithText("Find a thought…").performTextInput("no-matching-thought")
            rule.onNodeWithText("No notes match your search.").assertIsDisplayed(); shot("13-graph-empty-search")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            rule.onNodeWithTag("nav_connections").performClick(); shot("14-connections")
            rule.onNodeWithTag("concept_${tag.id}").performClick(); shot("15-concept-detail")
            rule.onNodeWithText("Edit name and type").performScrollTo().performClick(); shot("16-edit-concept")
            click("Cancel"); click("Types"); shot("17-types"); click("Done")
            click("Back"); click("Automatic topics"); shot("18-topics")
            rule.onNodeWithTag("topic_garden").performClick(); shot("18a-topic-detail")
            click("Rename topic"); shot("18b-rename-topic"); click("Cancel")
            click("Merge topic"); shot("18c-merge-topics"); click("Cancel")
            rule.onNodeWithTag("nav_settings").performClick(); shot("19-settings")
            runBlocking { db.organizationDao().failure("Could not update connections. Try again.") }
            rule.waitForIdle(); shot("19a-organization-error")
            runBlocking { db.organizationDao().request(); db.organizationDao().complete(db.organizationDao().state()!!.generation) }
            rule.onNodeWithTag("nav_new").performClick()
            rule.onNodeWithTag("capture_field").performTextInput("A long thought. ".repeat(35))
            rule.onNodeWithTag("complete_note").assertIsDisplayed(); shot("20-capture-keyboard")
            rule.onNodeWithTag("complete_note").performClick()
            rule.waitUntil(5000) { rule.onAllNodesWithTag("capture_field").fetchSemanticsNodes().isEmpty() }
            rule.onNodeWithContentDescription("Search notes").performClick(); shot("21-list-search")
            rule.onNodeWithContentDescription("Close search").performClick()
            runBlocking { repo.deleteNotes(repo.observeNotes().first().map { it.id }.toSet()) }
            rule.waitUntil(5000) { rule.onAllNodesWithTag("note_audit-a").fetchSemanticsNodes().isEmpty() }
            shot("22-empty-list")
            rule.onNodeWithTag("nav_graph").performClick(); click("Show graph"); shot("23-empty-graph")
        } finally {
            prefs.edit().putString("mode", oldMode).putBoolean("compact", oldCompact).commit()
            db.close()
        }
    }
    private fun click(text: String) = rule.onNodeWithText(text).performClick()
    private fun shot(name: String) {
        rule.waitForIdle()
        android.os.SystemClock.sleep(350)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val fontScale = instrumentation.targetContext.resources.configuration.fontScale
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "ui-audit/$mode-$fontScale").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
