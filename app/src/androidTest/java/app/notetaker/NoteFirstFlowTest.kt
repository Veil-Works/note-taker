package app.notetaker

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.notetaker.data.RoomNoteRepository
import app.notetaker.data.local.NoteDatabase
import app.notetaker.data.local.SemanticLinkEntity
import app.notetaker.model.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

@org.junit.runner.RunWith(org.junit.runners.Parameterized::class)
class NoteFirstFlowTest(private val mode: String) {
    companion object {
        @JvmStatic @org.junit.runners.Parameterized.Parameters(name = "{0}")
        fun modes() = listOf(arrayOf<Any>("Light"), arrayOf<Any>("Dark"))
    }
    @get:Rule val rule = createComposeRule()

    @Test fun backgrounding_flushes_the_active_thought_and_returns_to_an_empty_composer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build()
        val repo = RoomNoteRepository(db.noteDao())
        val owner = object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this)
        }
        val vm = app.notetaker.feature.notes.QuickCaptureViewModel(repo, androidx.lifecycle.SavedStateHandle())
        try {
            rule.runOnIdle { owner.lifecycle.currentState = androidx.lifecycle.Lifecycle.State.RESUMED }
            rule.setContent {
                androidx.compose.runtime.CompositionLocalProvider(androidx.lifecycle.compose.LocalLifecycleOwner provides owner) {
                    app.notetaker.ui.theme.NoteTakerTheme(mode) { app.notetaker.feature.notes.QuickCapture(vm) }
                }
            }
            rule.onNodeWithTag("quick_capture_field").performTextInput("A standalone reminder")
            rule.runOnIdle { owner.lifecycle.currentState = androidx.lifecycle.Lifecycle.State.CREATED }
            rule.waitUntil(5000) { runBlocking { repo.observeNotes().first().size == 1 } && vm.state.value.noteId == null }
            rule.runOnIdle { owner.lifecycle.currentState = androidx.lifecycle.Lifecycle.State.RESUMED }
            rule.onNodeWithTag("quick_capture_field").assert(SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
            assertEquals("A standalone reminder", runBlocking { repo.observeNotes().first().single().content })
            rule.runOnIdle { owner.lifecycle.currentState = androidx.lifecycle.Lifecycle.State.DESTROYED }
        } finally { db.close() }
    }

    @Test fun inline_capture_saves_literal_text_and_related_browsing_is_chronological() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
        val oldMode = preferences.getString("mode", null)
        preferences.edit().putString("mode", mode).commit()
        val db = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).addCallback(NoteDatabase.CALLBACK).build()
        val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
        runBlocking {
            repo.upsert(Note("run-new", "Run: 7 km in 39 minutes", 300, 300))
            repo.upsert(Note("run-old", "Run: 5 km in 29 minutes", 100, 900))
            repo.upsert(Note("walk", "Walked to the lake", 200, 200))
            repo.upsert(Note("unrelated", "Door code: 1234", 400, 400))
            db.organizationDao().insertSemanticLinks(listOf(SemanticLinkEntity("run-new", "run-old", .8), SemanticLinkEntity("run-new", "walk", .5)))
        }
        try {
            rule.setContent { NoteTakerApp(repo) }
            rule.onNodeWithTag("quick_capture_field").assertIsDisplayed()
            shot("feed")
            val literal = "#library username: example password: demo-only"
            rule.onNodeWithTag("quick_capture_field").performTextInput(literal)
            rule.waitUntil(5000) { runBlocking { repo.observeNotes().first().any { it.content == literal } } }
            shot("capture-keyboard")
            rule.onNodeWithTag("quick_capture_done").performClick()
            rule.waitUntil(5000) { rule.onAllNodesWithText(literal).fetchSemanticsNodes().size == 1 }
            assertEquals(1, runBlocking { repo.observeNotes().first().count { it.content == literal } })
            assertTrue(runBlocking { repo.observeNotes().first().first { it.content == literal }.tags.isEmpty() })
            rule.onNodeWithTag("notes_list").performScrollToNode(hasTestTag("related_run-new"))
            rule.onNodeWithTag("related_run-new").performClick()
            rule.onNodeWithTag("related_history").assertIsDisplayed()
            rule.onNodeWithTag("history_note_walk").assertDoesNotExist()
            rule.onNodeWithTag("history_note_unrelated").assertDoesNotExist()
            assertTrue(rule.onNodeWithTag("history_note_run-old").getUnclippedBoundsInRoot().top <
                rule.onNodeWithTag("history_note_run-new").getUnclippedBoundsInRoot().top)
            shot("history-close")
            rule.onNodeWithTag("history_broad").performClick()
            rule.onNodeWithTag("history_note_walk").assertExists()
            shot("history-broad")
            rule.onNodeWithTag("history_order").performClick()
            assertTrue(rule.onNodeWithTag("history_note_run-new").getUnclippedBoundsInRoot().top <
                rule.onNodeWithTag("history_note_run-old").getUnclippedBoundsInRoot().top)
        } finally { preferences.edit().putString("mode", oldMode).commit(); db.close() }
    }

    private fun shot(name: String) {
        rule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "note-first-audit").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        // Keep synthetic screenshots after Gradle uninstalls the test app.
        val copy = instrumentation.uiAutomation.executeShellCommand("cp ${File(dir, "$name.png").absolutePath} /data/local/tmp/note-first-$mode-$name.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(copy).use { it.readBytes() }
    }
}
