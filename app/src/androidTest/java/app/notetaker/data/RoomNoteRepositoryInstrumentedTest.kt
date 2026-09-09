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

    @Test
    fun tags_deduplicate_and_survive_edit_delete_and_undo() = runTest {
        val tag = repository.createTag(" Research ")
        assertEquals(tag, repository.createTag("research"))
        val second = repository.createTag("Ideas")
        val note = Note("tagged", "Find me", 1, 2, listOf(second, tag))
        repository.upsert(note)
        assertEquals(setOf(tag, second), repository.getNote(note.id)!!.tags.toSet())
        repository.upsert(note.copy(content = "Changed"))
        assertEquals(2, repository.observeNotes("Changed").first().single().tags.size)
        repository.upsert(note.copy(tags = listOf(tag)))
        val snapshot = repository.getNote(note.id)!!
        repository.delete(snapshot)
        assertEquals(emptyList<Note>(), repository.observeNotes().first())
        repository.upsert(snapshot)
        assertEquals(listOf(tag), repository.getNote(note.id)!!.tags)
        assertEquals(2, repository.observeTags().first().size)
    }

    @Test
    fun migration_from_v1_preserves_notes_and_allows_tagging() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test-${java.util.UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE notes (id TEXT NOT NULL PRIMARY KEY, content TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
            old.execSQL("CREATE INDEX index_notes_updatedAt ON notes(updatedAt)")
            old.execSQL("INSERT INTO notes VALUES ('v1-note', 'Keep this content', 10, 20)")
            old.version = 1
        }
        try {
            val migrated = Room.databaseBuilder(context, NoteDatabase::class.java, name)
                .addMigrations(NoteDatabase.MIGRATION_1_2).build()
            try {
                val repo = RoomNoteRepository(migrated.noteDao())
                val note = repo.getNote("v1-note")!!
                assertEquals(Note("v1-note", "Keep this content", 10, 20), note)
                val tag = repo.createTag("Migrated")
                repo.upsert(note.copy(tags = listOf(tag)))
                assertEquals(listOf(tag), repo.getNote(note.id)!!.tags)
            } finally { migrated.close() }
        } finally { context.deleteDatabase(name) }
    }
}
