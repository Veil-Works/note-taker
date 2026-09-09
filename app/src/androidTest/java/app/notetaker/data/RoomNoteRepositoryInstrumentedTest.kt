package app.notetaker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.notetaker.data.local.NoteDatabase
import app.notetaker.model.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomNoteRepositoryInstrumentedTest {
    private lateinit var database: NoteDatabase
    private lateinit var repository: RoomNoteRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomNoteRepository(database.noteDao())
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun repository_orders_updates_and_searches_wildcards_literally() = runTest {
        repository.upsert(Note("old", "100% complete", 1, 1))
        repository.upsert(Note("new", "A_B traffic idea", 2, 3))

        assertEquals(listOf("new", "old"), repository.observeNotes().first().map(Note::id))
        assertEquals(listOf("old"), repository.observeNotes("100%").first().map(Note::id))
        assertEquals(listOf("new"), repository.observeNotes("A_B").first().map(Note::id))
        assertEquals(listOf("new"), repository.observeNotes("traffic").first().map(Note::id))
    }

    @Test
    fun deleted_note_can_be_restored_with_the_same_identity() = runTest {
        val note = Note("stable-id", "Restore me", 1, 2)
        repository.upsert(note)
        repository.delete(note)
        assertEquals(emptyList<Note>(), repository.observeNotes().first())

        repository.upsert(note)
        assertEquals(note, repository.getNote(note.id))
    }
}

