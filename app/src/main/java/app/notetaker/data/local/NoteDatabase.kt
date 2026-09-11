package app.notetaker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NoteEntity::class, TagEntity::class, NoteTagEntity::class, NoteLinkEntity::class, TagAliasEntity::class, TagDismissalEntity::class, EntityTypeEntity::class, OrganizationStateEntity::class, NoteEmbeddingEntity::class, AutomaticTagEntity::class, OrganizationRejectionEntity::class, SemanticLinkEntity::class, TopicEntity::class, TopicNoteEntity::class],
    views = [TagDetails::class],
    version = 6,
    exportSchema = true,
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun organizationDao(): OrganizationDao

    companion object {
        // Triggers make queueing atomic with every input change, including bulk edits.
        fun installOrganizationTriggers(db: SupportSQLiteDatabase) {
            db.execSQL("INSERT OR IGNORE INTO organization_state VALUES (1, 0, -1, NULL)")
            listOf("notes", "note_tags", "tags", "tag_aliases", "entity_types", "organization_rejections").forEach { table ->
                listOf("INSERT", "UPDATE", "DELETE").forEach { event ->
                    val condition = if (table == "notes" && event == "UPDATE") " WHEN NEW.content != OLD.content" else ""
                    db.execSQL("CREATE TRIGGER IF NOT EXISTS queue_${table}_${event} AFTER $event ON $table$condition BEGIN UPDATE organization_state SET generation = generation + 1, error = NULL WHERE id = 1; END")
                }
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS organization_state (id INTEGER NOT NULL PRIMARY KEY, generation INTEGER NOT NULL, completed INTEGER NOT NULL, error TEXT)")
                db.execSQL("CREATE TABLE IF NOT EXISTS note_embeddings (noteId TEXT NOT NULL PRIMARY KEY, content TEXT NOT NULL, model TEXT NOT NULL, vector BLOB NOT NULL, FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE TABLE IF NOT EXISTS automatic_tags (noteId TEXT NOT NULL, tagId TEXT NOT NULL, reason TEXT NOT NULL, score REAL NOT NULL, PRIMARY KEY(noteId, tagId), FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_automatic_tags_tagId ON automatic_tags(tagId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS organization_rejections (noteId TEXT NOT NULL, tagId TEXT NOT NULL, content TEXT NOT NULL, PRIMARY KEY(noteId, tagId), FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_organization_rejections_tagId ON organization_rejections(tagId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS semantic_links (noteId TEXT NOT NULL, targetId TEXT NOT NULL, score REAL NOT NULL, PRIMARY KEY(noteId, targetId), FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(targetId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_semantic_links_targetId ON semantic_links(targetId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS topics (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, customized INTEGER NOT NULL, membershipLocked INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS topic_notes (topicId TEXT NOT NULL, noteId TEXT NOT NULL, PRIMARY KEY(topicId, noteId), FOREIGN KEY(topicId) REFERENCES topics(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_topic_notes_noteId ON topic_notes(noteId)")
                installOrganizationTriggers(db)
            }
        }

        val STARTER_TYPES = listOf("Exercise", "Project", "Research topic", "Algorithm", "Person", "Place", "Task", "Idea")
        fun seedTypes(db: SupportSQLiteDatabase) {
            STARTER_TYPES.forEachIndexed { index, name ->
                db.execSQL("INSERT OR IGNORE INTO entity_types (id, name, normalizedName) VALUES (?, ?, ?)", arrayOf("starter-$index", name, name.lowercase(java.util.Locale.ROOT)))
            }
        }
        val CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) { seedTypes(db); installOrganizationTriggers(db) }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS entity_types (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, normalizedName TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_entity_types_normalizedName ON entity_types(normalizedName)")
                db.execSQL("ALTER TABLE tags ADD COLUMN typeId TEXT REFERENCES entity_types(id) ON UPDATE NO ACTION ON DELETE SET NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tags_typeId ON tags(typeId)")
                db.execSQL("CREATE VIEW `tag_details` AS SELECT tags.*, entity_types.name AS typeName FROM tags LEFT JOIN entity_types ON tags.typeId = entity_types.id")
                seedTypes(db)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS tag_aliases (id TEXT NOT NULL PRIMARY KEY, tagId TEXT NOT NULL, text TEXT NOT NULL, normalizedText TEXT NOT NULL, FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tag_aliases_tagId_normalizedText ON tag_aliases(tagId, normalizedText)")
                db.execSQL("CREATE TABLE IF NOT EXISTS tag_dismissals (noteId TEXT NOT NULL, tagId TEXT NOT NULL, PRIMARY KEY(noteId, tagId), FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tag_dismissals_tagId ON tag_dismissals(tagId)")
            }
        }

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
