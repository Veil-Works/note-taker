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

class TagSuggestionsPersistenceTest {
    @Test fun aliases_and_dismissals_survive_reopen_and_reset_only_on_text_change() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "suggestions-${java.util.UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, NoteDatabase::class.java, name).build()
        var db = open()
        try {
            var repo = RoomNoteRepository(db.noteDao())
            val strength = repo.createTag("Strength")
            val bench = repo.createTag("Bench")
            repo.saveAlias(strength.id, null, " bench press ")
            repo.saveAlias(strength.id, null, "BENCH PRESS")
            repo.saveAlias(bench.id, null, "bench press")
            assertEquals(2, repo.observeAliases().first().size)
            assertTrue(runCatching { repo.saveAlias(bench.id, null, "  ") }.isFailure)
            val note = Note("n", "Bench press today", 1, 2)
            repo.upsert(note)
            assertEquals(setOf(bench, strength), repo.observeSuggestions("n").first().toSet())
            repo.dismissSuggestion("n", strength.id, note.content)
            assertTrue(repo.getNote("n")!!.tags.isEmpty())
            repo.upsert(note.copy(tags = listOf(bench)))
            assertTrue(repo.observeSuggestions("n").first().isEmpty())
            db.close()
            db = open()
            repo = RoomNoteRepository(db.noteDao())
            assertEquals(2, repo.observeAliases().first().size)
            assertTrue(repo.observeSuggestions("n").first().isEmpty())
            val alias = repo.observeAliases().first().first { it.tagId == strength.id }
            repo.saveAlias(strength.id, alias.id, "today")
            assertTrue(repo.observeSuggestions("n").first().isEmpty())
            repo.upsert(note.copy(content = "Bench press today!", tags = listOf(bench)))
            assertEquals(listOf(strength), repo.observeSuggestions("n").first())
            repo.dismissSuggestion("n", strength.id, note.content) // stale evaluation is ignored
            assertEquals(listOf(strength), repo.observeSuggestions("n").first())
            repo.deleteAlias(alias.id)
            assertTrue(repo.observeSuggestions("n").first().isEmpty())
            repo.upsert(note.copy(content = "STRENGTH today", tags = listOf(bench)))
            assertEquals(listOf(strength), repo.observeSuggestions("n").first())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun migration_from_v3_preserves_notes_tags_and_connections() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "v3-${java.util.UUID.randomUUID()}.db"
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
            old.execSQL("INSERT INTO notes VALUES ('a', 'Bench press', 10, 20), ('b', 'Plan', 30, 40)")
            old.execSQL("INSERT INTO tags VALUES ('t', 'Training', 'training', 1)")
            old.execSQL("INSERT INTO note_tags VALUES ('b', 't')")
            old.execSQL("INSERT INTO note_links VALUES ('a', 'b')")
            old.version = 3
        }
        val db = Room.databaseBuilder(context, NoteDatabase::class.java, name).addMigrations(NoteDatabase.MIGRATION_3_4, NoteDatabase.MIGRATION_4_5, NoteDatabase.MIGRATION_5_6, NoteDatabase.MIGRATION_6_7, NoteDatabase.MIGRATION_7_8).build()
        try {
            val repo = RoomNoteRepository(db.noteDao())
            assertEquals(Note("a", "Bench press", 10, 20, linkedNoteIds = setOf("b")), repo.getNote("a"))
            assertEquals("Training", repo.getNote("b")!!.tags.single().name)
            repo.saveAlias("t", null, "bench press")
            assertEquals("t", repo.observeSuggestions("a").first().single().id)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
