package app.notetaker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.notetaker.data.local.*
import app.notetaker.model.Note
import app.notetaker.organization.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TopicPrefixCaptureTest {
    @Test fun capture_reuses_case_insensitive_topics_and_keeps_explicit_memberships_through_organization() = runTest {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), NoteDatabase::class.java).addCallback(NoteDatabase.CALLBACK).build()
        try {
            val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            db.organizationDao().insertDiscoveredTags(listOf(TagEntity("atlas", "Atlas", "atlas", 1, provisional = true)))
            val note = Note("captured", "#ATLAS #orion #atlas #\"Running shoes\" Review the release #later", 10, 10)
            repo.capture(note)
            repo.capture(note) // Retry uses the same identity without duplicate topics or memberships.
            val captured = repo.getNote(note.id)!!
            assertEquals("Review the release #later", captured.content)
            assertEquals(setOf("Atlas", "orion", "Running shoes"), captured.tags.map { it.name }.toSet())
            assertTrue(captured.tags.all { !it.provisional })
            assertTrue(captured.tags.any { it.id == "atlas" })
            assertTrue(captured.automaticTagReasons.isEmpty())
            val model = object : TextEmbedding {
                override val version = "prefix-test"
                override fun embed(text: String) = floatArrayOf(1f, 0f)
            }
            assertTrue(NotebookOrganizer(db, model).runOnce())
            assertEquals(captured, repo.getNote(note.id))
            repo.saveEdit(captured.copy(content = "Changed thought"), emptyMap())
            assertEquals(3, repo.getNote(note.id)!!.tags.size)
            assertTrue(db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { !it.moveToFirst() })
        } finally { db.close() }
    }
    @Test fun invalid_capture_leaves_no_note_or_orphan_topics() = runTest {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), NoteDatabase::class.java).build()
        try {
            val repo = RoomNoteRepository(db.noteDao())
            for (text in listOf("#Atlas #orion", "#Atlas #" + "a".repeat(41) + " body")) {
                assertTrue(runCatching { repo.capture(Note("bad", text, 1, 1)) }.exceptionOrNull() is IllegalArgumentException)
                assertNull(repo.getNote("bad"))
                assertTrue(repo.observeTags().first().isEmpty())
            }
        } finally { db.close() }
    }
}
