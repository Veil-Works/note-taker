package app.notetaker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NoteEntity::class, TagEntity::class, NoteTagEntity::class, NoteLinkEntity::class, TagAliasEntity::class, TagDismissalEntity::class, EntityTypeEntity::class, OrganizationStateEntity::class, NoteEmbeddingEntity::class, AutomaticTagEntity::class, OrganizationRejectionEntity::class, SemanticLinkEntity::class, TopicDiscoveryKeyEntity::class],
    views = [TagDetails::class],
    version = 7,
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
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP VIEW IF EXISTS tag_details")
                db.execSQL("ALTER TABLE tags ADD COLUMN provisional INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE VIEW `tag_details` AS SELECT tags.*, entity_types.name AS typeName FROM tags LEFT JOIN entity_types ON tags.typeId = entity_types.id")
                db.execSQL("CREATE TABLE topic_discovery_keys (`key` TEXT NOT NULL PRIMARY KEY, tagId TEXT, FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE INDEX index_topic_discovery_keys_tagId ON topic_discovery_keys(tagId)")
                // Name equality alone is not enough evidence to collapse an existing group into a tag.
                val ids = mutableSetOf<String>()
                val names = mutableSetOf<String>()
                db.query("SELECT id, normalizedName FROM tags").use { rows ->
                    while (rows.moveToNext()) { ids += rows.getString(0); names += rows.getString(1) }
                }
                db.query("SELECT id, name, customized, membershipLocked FROM topics ORDER BY id").use { rows ->
                    while (rows.moveToNext()) {
                        val oldId = rows.getString(0)
                        var id = oldId
                        while (id in ids) id = "migrated-$id"
                        ids += id
                        val base = rows.getString(1).trim().ifBlank { "Untitled topic" }.take(40)
                        var name = base
                        var suffix = 2
                        while (name.lowercase(java.util.Locale.ROOT) in names) {
                            val ending = " ($suffix)"
                            name = base.take(40 - ending.length) + ending
                            suffix++
                        }
                        val normalized = name.lowercase(java.util.Locale.ROOT)
                        names += normalized
                        val locked = rows.getInt(3) != 0
                        val provisional = if (rows.getInt(2) != 0 || locked) 0 else 1
                        db.execSQL("INSERT INTO tags (id, name, normalizedName, createdAt, typeId, provisional) VALUES (?, ?, ?, 0, NULL, ?)", arrayOf(id, name, normalized, provisional))
                        db.execSQL("INSERT INTO topic_discovery_keys (`key`, tagId) VALUES (?, ?)", arrayOf(oldId, id))
                        if (locked) {
                            // Legacy explicit merges promised to keep the combined membership.
                            db.execSQL("INSERT OR IGNORE INTO note_tags SELECT noteId, ? FROM topic_notes WHERE topicId = ?", arrayOf(id, oldId))
                        } else {
                            db.execSQL("INSERT OR IGNORE INTO automatic_tags SELECT noteId, ?, 'Discovered from related notes', 1.0 FROM topic_notes WHERE topicId = ?", arrayOf(id, oldId))
                        }
                    }
                }
                db.execSQL("DROP TABLE topic_notes")
                db.execSQL("DROP TABLE topics")
                installOrganizationTriggers(db)
                db.execSQL("UPDATE organization_state SET generation = generation + 1, error = NULL WHERE id = 1")
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
