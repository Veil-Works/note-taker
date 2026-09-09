package app.notetaker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NoteEntity::class, TagEntity::class, NoteTagEntity::class, NoteLinkEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS note_links (firstNoteId TEXT NOT NULL, secondNoteId TEXT NOT NULL, PRIMARY KEY(firstNoteId, secondNoteId), FOREIGN KEY(firstNoteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(secondNoteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_links_secondNoteId ON note_links(secondNoteId)")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS tags (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, normalizedName TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_normalizedName ON tags(normalizedName)")
                db.execSQL("CREATE TABLE IF NOT EXISTS note_tags (noteId TEXT NOT NULL, tagId TEXT NOT NULL, PRIMARY KEY(noteId, tagId), FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_tags_tagId ON note_tags(tagId)")
            }
        }
    }
}
