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
    fun connections_are_bidirectional_unique_and_survive_edit_delete_undo() = runTest {
        val a = Note("a", "Training plan", 1, 1)
        val b = Note("b", "Running log", 2, 2)
        repository.upsert(a)
        repository.upsert(b)
        repository.linkNotes("a", "b")
        repository.linkNotes("b", "a")
        assertEquals(setOf("b"), repository.getNote("a")!!.linkedNoteIds)
        assertEquals(setOf("a"), repository.getNote("b")!!.linkedNoteIds)
        repository.upsert(a.copy(content = "Updated plan"))
        assertEquals(setOf("b"), repository.getNote("a")!!.linkedNoteIds)
        val snapshot = repository.getNote("b")!!
        repository.delete(snapshot)
        assertEquals(emptySet<String>(), repository.getNote("a")!!.linkedNoteIds)
        repository.upsert(snapshot)
        assertEquals(setOf("b"), repository.getNote("a")!!.linkedNoteIds)
        repository.unlinkNotes("b", "a")
        assertEquals(emptySet<String>(), repository.getNote("a")!!.linkedNoteIds)
        assertEquals(emptySet<String>(), repository.getNote("b")!!.linkedNoteIds)
        repository.upsert(snapshot.copy(content = "Stale editor save"))
        assertEquals(emptySet<String>(), repository.getNote("b")!!.linkedNoteIds)
    }

    @Test
    fun invalid_connections_are_rejected_and_undo_skips_deleted_targets() = runTest {
        repository.upsert(Note("a", "Plan", 1, 1))
        repository.upsert(Note("b", "Log", 1, 1))
        org.junit.Assert.assertTrue(runCatching { repository.linkNotes("a", "a") }.isFailure)
        org.junit.Assert.assertTrue(runCatching { repository.linkNotes("a", "missing") }.isFailure)
        repository.linkNotes("a", "b")
        val snapshot = repository.getNote("a")!!
        repository.delete(snapshot)
        repository.delete(repository.getNote("b")!!)
        repository.upsert(snapshot)
        assertEquals(emptySet<String>(), repository.getNote("a")!!.linkedNoteIds)
    }

    @Test fun bulk_operations_preserve_unselected_notes_and_tag_deletion_cascades() = runTest {
        val tag = repository.createTag("Work")
        val other = repository.createTag("Other")
        repository.upsert(Note("a", "A", 1, 1, listOf(other)))
        repository.upsert(Note("b", "B", 1, 1))
        repository.upsert(Note("c", "C", 1, 1))
        repository.upsert(Note("outside", "Outside", 1, 1))
        repository.tagNotes(setOf("a", "b", "c"), setOf(tag.id))
        assertEquals(setOf(tag, other), repository.getNote("a")!!.tags.toSet())
        assertEquals(emptyList<app.notetaker.model.Tag>(), repository.getNote("outside")!!.tags)
        repository.connectNotes(setOf("c", "a", "b"))
        repository.connectNotes(setOf("a", "b", "c"))
        assertEquals(setOf("b", "c"), repository.getNote("a")!!.linkedNoteIds)
        assertEquals(setOf("a", "c"), repository.getNote("b")!!.linkedNoteIds)
        repository.saveAlias(tag.id, null, "keyword")
        repository.dismissSuggestion("a", tag.id, "A")
        repository.deleteTag(tag.id)
        assertEquals(listOf(other), repository.getNote("a")!!.tags)
        assertEquals(emptyList<app.notetaker.model.TagAlias>(), repository.observeAliases().first())
        assertEquals(emptyList<String>(), database.noteDao().observeDismissals("a").first())
        assertEquals(setOf("b", "c"), repository.getNote("a")!!.linkedNoteIds)
        repository.deleteNotes(setOf("a", "b"))
        assertEquals(setOf("c", "outside"), repository.observeNotes().first().map { it.id }.toSet())
        assertEquals(emptySet<String>(), repository.getNote("c")!!.linkedNoteIds)
    }

    @Test fun bulk_tag_changes_add_and_remove_atomically_without_touching_other_tags_or_links() = runTest {
        val old = repository.createTag("Old")
        val added = repository.createTag("Added")
        val partial = repository.createTag("Partial")
        repository.upsert(Note("a", "A", 1, 1, listOf(old, partial)))
        repository.upsert(Note("b", "B", 1, 1, listOf(old)))
        repository.linkNotes("a", "b")
        org.junit.Assert.assertTrue(runCatching {
            repository.tagNotes(setOf("a", "missing"), setOf(added.id), setOf(old.id))
        }.isFailure)
        assertEquals(setOf(old, partial), repository.getNote("a")!!.tags.toSet())
        repository.tagNotes(setOf("a", "b"), setOf(added.id), setOf(old.id))
        assertEquals(setOf(added, partial), repository.getNote("a")!!.tags.toSet())
        assertEquals(listOf(added), repository.getNote("b")!!.tags)
        assertEquals(setOf("b"), repository.getNote("a")!!.linkedNoteIds)
        repository.tagNotes(setOf("a", "b"), emptySet(), setOf(added.id, partial.id))
        assertEquals(emptyList<app.notetaker.model.Tag>(), repository.getNote("a")!!.tags)
        assertEquals(emptyList<app.notetaker.model.Tag>(), repository.getNote("b")!!.tags)
        assertEquals(setOf(old, added, partial), repository.observeTags().first().toSet())
    }

    @Test fun invalid_bulk_requests_do_not_partially_update_notes() = runTest {
        repository.upsert(Note("a", "A", 1, 1))
        val tag = repository.createTag("Work")
        org.junit.Assert.assertTrue(runCatching { repository.tagNotes(setOf("a", "missing"), setOf(tag.id)) }.isFailure)
        org.junit.Assert.assertTrue(runCatching { repository.connectNotes(setOf("a", "missing")) }.isFailure)
        assertEquals(Note("a", "A", 1, 1), repository.getNote("a"))
    }

    @Test
    fun migration_from_v2_preserves_tags_and_persists_connections_after_reopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-v2-${java.util.UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE notes (id TEXT NOT NULL PRIMARY KEY, content TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
            old.execSQL("CREATE INDEX index_notes_updatedAt ON notes(updatedAt)")
            old.execSQL("CREATE TABLE tags (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, normalizedName TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            old.execSQL("CREATE UNIQUE INDEX index_tags_normalizedName ON tags(normalizedName)")
            old.execSQL("CREATE TABLE note_tags (noteId TEXT NOT NULL, tagId TEXT NOT NULL, PRIMARY KEY(noteId, tagId), FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            old.execSQL("CREATE INDEX index_note_tags_tagId ON note_tags(tagId)")
            old.execSQL("INSERT INTO notes VALUES ('a', 'Plan', 10, 20), ('b', 'Log', 30, 40)")
            old.execSQL("INSERT INTO tags VALUES ('t', 'Running', 'running', 1)")
            old.execSQL("INSERT INTO note_tags VALUES ('a', 't')")
            old.version = 2
        }
        try {
            val migrated = Room.databaseBuilder(context, NoteDatabase::class.java, name)
                .addMigrations(NoteDatabase.MIGRATION_2_3, NoteDatabase.MIGRATION_3_4).build()
            try {
                val repo = RoomNoteRepository(migrated.noteDao())
                val note = repo.getNote("a")!!
                assertEquals("Plan", note.content)
                assertEquals(10L, note.createdAt)
                assertEquals(20L, note.updatedAt)
                assertEquals("Running", note.tags.single().name)
                repo.linkNotes("a", "b")
            } finally { migrated.close() }
            val reopened = Room.databaseBuilder(context, NoteDatabase::class.java, name).build()
            try {
                val repo = RoomNoteRepository(reopened.noteDao())
                assertEquals(setOf("b"), repo.getNote("a")!!.linkedNoteIds)
                assertEquals(setOf("a"), repo.getNote("b")!!.linkedNoteIds)
            } finally { reopened.close() }
        } finally { context.deleteDatabase(name) }
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
                .addMigrations(NoteDatabase.MIGRATION_1_2, NoteDatabase.MIGRATION_2_3, NoteDatabase.MIGRATION_3_4).build()
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
