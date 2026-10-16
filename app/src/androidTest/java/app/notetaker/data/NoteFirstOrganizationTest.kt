package app.notetaker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.notetaker.data.local.NoteDatabase
import app.notetaker.model.Note
import app.notetaker.organization.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NoteFirstOrganizationTest {
    @Test fun connections_only_retires_guesses_and_preserves_authored_notes_and_manual_organization() = runTest {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), NoteDatabase::class.java)
            .addCallback(NoteDatabase.CALLBACK).build()
        val model = object : TextEmbedding {
            override val version = "note-first-test"
            override fun embed(text: String) = if (text.startsWith("French")) floatArrayOf(1f, 0f) else floatArrayOf(0f, 1f)
        }
        try {
            val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            val manual = repo.createTag("My project")
            repo.upsert(Note("a", "French practice one", 1, 1))
            repo.upsert(Note("b", "French practice two", 2, 2))
            repo.upsert(Note("c", "Standalone login", 3, 3, listOf(manual)))
            repo.linkNotes("a", "c")
            repo.saveAlias(manual.id, null, "project alias")
            val members = setOf("a", "b")
            assertTrue(NotebookOrganizer(db, model) {
                listOf(TopicCandidate("old", "French", members, members.associateWith { .8 }, members.associateWith { "Discovery" }))
            }.runOnce())
            assertTrue(repo.observeTags().first().any { it.provisional })
            db.organizationDao().request()
            assertTrue(NotebookOrganizer(db, model, connectionsOnly = true) { error("Must not discover groups") }.runOnce())
            assertEquals(setOf("a", "b", "c"), repo.observeNotes().first().map { it.id }.toSet())
            assertEquals(listOf(manual.id), repo.observeTags().first().map { it.id })
            assertTrue(repo.getNote("a")!!.tags.isEmpty())
            assertTrue(repo.getNote("b")!!.tags.isEmpty())
            assertEquals(manual.id, repo.getNote("c")!!.tags.single().id)
            assertEquals(setOf("c"), repo.getNote("a")!!.linkedNoteIds)
            assertEquals("project alias", repo.observeAliases().first().single().text)
            assertEquals("b", repo.observeRelated("a").first().single().noteId)
            assertTrue(repo.observeRelated("c").first().isEmpty())
            assertFalse(repo.observeOrganizationStatus().first().pending)
        } finally { db.close() }
    }
}
