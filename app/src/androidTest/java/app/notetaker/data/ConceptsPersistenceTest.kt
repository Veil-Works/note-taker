package app.notetaker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.notetaker.data.local.NoteDatabase
import app.notetaker.model.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ConceptsPersistenceTest {
    @Test fun upgrade_rename_revert_and_ambiguous_aliases_preserve_identity() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "concepts-${java.util.UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, NoteDatabase::class.java, name).addCallback(NoteDatabase.CALLBACK).build()
        var db = open()
        try {
            var repo = RoomNoteRepository(db.noteDao())
            val exercise = repo.observeTypes().first().single { it.name == "Exercise" }
            val project = repo.observeTypes().first().single { it.name == "Project" }
            val bench = repo.createTag("Bench press")
            repo.upsert(Note("old", "Training log", 1, 2, listOf(bench)))
            repo.saveAlias(bench.id, null, "barbell bench press")
            repo.updateTag(bench.id, bench.name, exercise.id)
            val typed = repo.getNote("old")!!.tags.single()
            assertEquals(bench.id, typed.id)
            assertEquals("Bench press · Exercise", typed.displayName)
            repo.upsert(Note("new", "barbell bench press today", 3, 4))
            assertEquals(typed, repo.observeSuggestions("new").first().single())
            val other = repo.createTag("Bench research")
            repo.updateTag(other.id, other.name, project.id)
            repo.saveAlias(other.id, null, "barbell bench press")
            assertEquals(setOf("Exercise", "Project"), repo.observeSuggestions("new").first().map { it.typeName }.toSet())
            repo.dismissSuggestion("new", other.id, "barbell bench press today")
            repo.upsert(repo.getNote("new")!!.copy(tags = listOf(typed)))
            assertEquals(2, repo.observeNotes().first().count { note -> note.tags.any { it.id == bench.id } })
            repo.saveType(exercise.id, "Movement")
            repo.updateTag(bench.id, "Barbell press", exercise.id)
            assertEquals("Barbell press · Movement", repo.getNote("old")!!.tags.single().displayName)
            assertTrue(repo.observeSuggestions("new").first().isEmpty())
            assertTrue(runCatching { repo.saveType(null, " movement ") }.isFailure)
            assertTrue(runCatching { repo.updateTag(bench.id, other.name, exercise.id) }.isFailure)
            db.close()
            db = open()
            repo = RoomNoteRepository(db.noteDao())
            assertEquals("Movement", repo.getNote("old")!!.tags.single().typeName)
            repo.updateTag(bench.id, "Barbell press", null)
            assertNull(repo.getNote("old")!!.tags.single().typeId)
            repo.updateTag(bench.id, "Barbell press", exercise.id)
            val deleted = repo.getNote("old")!!
            repo.delete(deleted)
            repo.deleteType(exercise.id)
            repo.upsert(deleted)

            assertNull(repo.getNote("new")!!.tags.single().typeId)
            assertEquals(2, repo.observeAliases().first().size)
            assertEquals(1L, repo.getNote("old")!!.createdAt)
            assertEquals(2L, repo.getNote("old")!!.updatedAt)
            assertEquals(2, repo.observeNotes().first().count { note -> note.tags.any { it.id == bench.id } })
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun v4_migration_preserves_aliases_dismissals_tags_and_links() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "concept-migration-${java.util.UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE notes (id TEXT NOT NULL PRIMARY KEY, content TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
            old.execSQL("CREATE INDEX index_notes_updatedAt ON notes(updatedAt)")
            old.execSQL("CREATE TABLE tags (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, normalizedName TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            old.execSQL("CREATE UNIQUE INDEX index_tags_normalizedName ON tags(normalizedName)")
            old.execSQL("CREATE TABLE note_tags (noteId TEXT NOT NULL, tagId TEXT NOT NULL, PRIMARY KEY(noteId, tagId), FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            old.execSQL("CREATE INDEX index_note_tags_tagId ON note_tags(tagId)")
            old.execSQL("CREATE TABLE note_links (firstNoteId TEXT NOT NULL, secondNoteId TEXT NOT NULL, PRIMARY KEY(firstNoteId, secondNoteId), FOREIGN KEY(firstNoteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(secondNoteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            old.execSQL("CREATE INDEX index_note_links_secondNoteId ON note_links(secondNoteId)")
            old.execSQL("CREATE TABLE tag_aliases (id TEXT NOT NULL PRIMARY KEY, tagId TEXT NOT NULL, text TEXT NOT NULL, normalizedText TEXT NOT NULL, FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            old.execSQL("CREATE UNIQUE INDEX index_tag_aliases_tagId_normalizedText ON tag_aliases(tagId, normalizedText)")
            old.execSQL("CREATE TABLE tag_dismissals (noteId TEXT NOT NULL, tagId TEXT NOT NULL, PRIMARY KEY(noteId, tagId), FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            old.execSQL("CREATE INDEX index_tag_dismissals_tagId ON tag_dismissals(tagId)")
            old.execSQL("INSERT INTO notes VALUES ('a', 'barbell bench press', 10, 20), ('b', 'Log', 30, 40)")
            old.execSQL("INSERT INTO tags VALUES ('t', 'Bench press', 'bench press', 1)")
            old.execSQL("INSERT INTO note_tags VALUES ('b', 't')")
            old.execSQL("INSERT INTO note_links VALUES ('a', 'b')")
            old.execSQL("INSERT INTO tag_aliases VALUES ('alias', 't', 'barbell bench press', 'barbell bench press')")
            old.execSQL("INSERT INTO tag_dismissals VALUES ('a', 't')")
            old.version = 4
        }
        val db = Room.databaseBuilder(context, NoteDatabase::class.java, name).addMigrations(NoteDatabase.MIGRATION_4_5, NoteDatabase.MIGRATION_5_6).build()
        try {
            val repo = RoomNoteRepository(db.noteDao())
            assertEquals(8, repo.observeTypes().first().size)
            assertEquals(setOf("b"), repo.getNote("a")!!.linkedNoteIds)
            assertEquals("t", repo.getNote("b")!!.tags.single().id)
            assertNull(repo.getNote("b")!!.tags.single().typeId)
            assertEquals("barbell bench press", repo.observeAliases().first().single().text)
            assertTrue(repo.observeSuggestions("a").first().isEmpty())
            repo.updateTag("t", "Bench press", repo.observeTypes().first().first().id)
            assertTrue(repo.observeSuggestions("a").first().isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
