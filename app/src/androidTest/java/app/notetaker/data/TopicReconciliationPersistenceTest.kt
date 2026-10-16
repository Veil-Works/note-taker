package app.notetaker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.notetaker.data.local.*
import app.notetaker.model.Note
import app.notetaker.organization.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class TopicReconciliationPersistenceTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun database() = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).addCallback(NoteDatabase.CALLBACK).build()
    private val model = object : TextEmbedding {
        override val version = "identity-test"
        override fun embed(text: String): FloatArray {
            val plants = text.contains("rosemary", true)
            val books = text.contains("library", true)
            return normalize(if (plants && books) floatArrayOf(1f, 1f) else if (plants) floatArrayOf(1f, 0f) else floatArrayOf(0f, 1f))
        }
    }
    private fun group(key: String, name: String, vararg ids: String) = TopicCandidate(key, name, ids.toSet(),
        ids.associateWith { .8 }, ids.associateWith { "Fixture evidence" })

    @Test fun legacy_mixed_bridge_is_repaired_without_changing_manual_choices_or_note_text() = runTest {
        val db = database()
        try {
            val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            listOf("r1" to "rosemary watering", "r2" to "rosemary pot", "mixed" to "rosemary and library errands", "l1" to "library return")
                .forEach { (id, text) -> repo.upsert(Note(id, text, 1, 1)) }
            val old = repo.createTag("Balcony rosemary")
            db.organizationDao().insertAutomaticTags(listOf("r1", "r2", "mixed", "l1").map { AutomaticTagEntity(it, old.id, "Old contaminated membership", .6) })
            db.organizationDao().saveDiscoveryKeys(listOf(TopicDiscoveryKeyEntity("library", old.id)))
            // A user removal must not count toward identity overlap or be undone.
            repo.saveEdit(repo.getNote("l1")!!, mapOf(old.id to false))
            val before = repo.observeNotes().first().map { Triple(it.id, it.content, it.updatedAt) }
            val organizer = NotebookOrganizer(db, model) {
                listOf(group("plants", "Balcony rosemary", "r1", "r2", "mixed"), group("library", "Library", "mixed", "l1"))
            }
            assertTrue(organizer.runOnce())
            val library = repo.observeTags().first().single { it.name == "Library" }
            assertNotEquals(old.id, library.id)
            assertEquals(setOf(old.id, library.id), repo.getNote("mixed")!!.tags.map { it.id }.toSet())
            assertEquals(setOf(library.id), repo.getNote("l1")!!.tags.map { it.id }.toSet())
            assertTrue(old.id in repo.getNote("l1")!!.rejectedTagIds)
            assertEquals(before, repo.observeNotes().first().map { Triple(it.id, it.content, it.updatedAt) })
            repo.retryOrganization(); organizer.runOnce()
            assertEquals(library.id, repo.observeTags().first().single { it.name == "Library" }.id)
            assertFalse(repo.observeOrganizationStatus().first().pending)
        } finally { db.close() }
    }

    @Test fun user_merge_keeps_two_distinct_branches_when_both_grow() = runTest {
        val db = database()
        try {
            val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            listOf("r1", "r2", "l1", "l2").forEach { repo.upsert(Note(it, if (it.startsWith("r")) "rosemary $it" else "library $it", 1, 1)) }
            var groups = listOf(group("plants", "Rosemary", "r1", "r2"), group("books", "Library", "l1", "l2"))
            val organizer = NotebookOrganizer(db, model) { groups }
            organizer.runOnce()
            val target = repo.getNote("r1")!!.tags.single().id
            val source = repo.getNote("l1")!!.tags.single().id
            repo.mergeTopics(target, source)
            assertTrue(db.organizationDao().discoveryKeys().filter { it.subject != null }.all { it.mergeKind == 1 })
            repo.upsert(Note("r3", "rosemary repotting", 1, 1)); repo.upsert(Note("l3", "library renewal", 1, 1))
            groups = listOf(group("grown-plants", "Rosemary", "r1", "r2", "r3"), group("grown-books", "Library", "l1", "l2", "l3"))
            organizer.runOnce()
            assertEquals(listOf(target), repo.observeTags().first().map { it.id })
            assertEquals(6, repo.observeTopics().first().single().noteIds.size)
            assertEquals(2, db.organizationDao().discoveryKeys().filter { it.subject != null }.map { it.branchKey }.distinct().size)
            repo.retryOrganization(); organizer.runOnce()
            assertEquals(listOf(target), repo.observeTags().first().map { it.id })
        } finally { db.close() }
    }

    @Test fun expiring_name_is_released_before_new_topic_insertion() = runTest {
        val db = database()
        try {
            val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            repo.upsert(Note("l1", "library return", 1, 1)); repo.upsert(Note("l2", "library renewal", 1, 1))
            db.organizationDao().insertDiscoveredTags(listOf(TagEntity("expired", "Library", "library", 1, provisional = true)))
            NotebookOrganizer(db, model) { listOf(group("new", "Library", "l1", "l2")) }.runOnce()
            val saved = repo.observeTags().first().single()
            assertEquals("Library", saved.name)
            assertNotEquals("expired", saved.id)
            assertNull(repo.observeTopicRedirects().first()["expired"])
        } finally { db.close() }
    }

    @Test fun distinct_subjects_with_identical_notes_keep_separate_saved_identities() = runTest {
        val db = database()
        try {
            val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            repo.upsert(Note("a", "rosemary and library errands", 1, 1))
            repo.upsert(Note("b", "library and rosemary plans", 1, 1))
            val organizer = NotebookOrganizer(db, model) {
                listOf(group("shared-content", "Rosemary", "a", "b"), group("shared-content", "Library", "a", "b"))
            }
            organizer.runOnce()
            val before = repo.observeTags().first().associate { it.name to it.id }
            assertEquals(setOf("Rosemary", "Library"), before.keys)
            assertEquals(2, before.values.distinct().size)
            repo.retryOrganization(); organizer.runOnce()
            assertEquals(before, repo.observeTags().first().associate { it.name to it.id })
        } finally { db.close() }
    }

    @Test fun provisional_names_can_swap_in_one_transaction_and_stale_results_write_nothing() = runTest {
        val db = database()
        try {
            val dao = db.organizationDao()
            db.noteDao().upsert(NoteEntity("n", "note", 1, 1))
            dao.insertDiscoveredTags(listOf(TagEntity("a", "One", "one", 1, provisional = true), TagEntity("b", "Two", "two", 1, provisional = true)))
            val generation = dao.state()!!.generation
            val memberships = listOf(AutomaticTagEntity("n", "a", "a", .8), AutomaticTagEntity("n", "b", "b", .8))
            assertTrue(dao.apply(generation, emptyList(), memberships, emptyList(), emptyList(), emptyList(),
                listOf(TagEntity("a", "Two", "two", 1, provisional = true), TagEntity("b", "One", "one", 1, provisional = true))))
            val current = db.noteDao().observeTags().first().associate { it.id to it.name }
            assertEquals(mapOf("a" to "Two", "b" to "One"), current)
            assertFalse(dao.apply(generation, emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
            assertEquals(current, db.noteDao().observeTags().first().associate { it.id to it.name })
        } finally { db.close() }
    }

    @Test fun v7_migration_preserves_data_and_quarantines_ambiguous_merged_lineage() = runTest {
        val name = "identity-${UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        val schema = org.json.JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("app.notetaker.data.local.NoteDatabase/7.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val views = schema.getJSONArray("views")
            for (i in 0 until views.length()) {
                val view = views.getJSONObject(i)
                old.execSQL(view.getString("createSql").replace("\${VIEW_NAME}", view.getString("viewName")))
            }
            old.execSQL("INSERT INTO notes VALUES ('n', 'library and rosemary', 1, 2)")
            old.execSQL("INSERT INTO tags VALUES ('retained', 'My collection', 'my collection', 1, NULL, 0), ('temporary', 'Old', 'old', 1, NULL, 1)")
            old.execSQL("INSERT INTO note_tags VALUES ('n', 'retained')")
            old.execSQL("INSERT INTO topic_discovery_keys VALUES ('plants', 'retained'), ('books', 'retained'), ('merged-id:source', 'retained'), ('ordinary', 'temporary'), ('deleted', NULL)")
            old.execSQL("INSERT INTO organization_state VALUES (1, 7, 7, NULL)")
            old.version = 7
        }
        val db = Room.databaseBuilder(context, NoteDatabase::class.java, name).addMigrations(NoteDatabase.MIGRATION_7_8).build()
        try {
            val keys = db.organizationDao().discoveryKeys().associateBy { it.key }
            assertEquals(2, keys.getValue("plants").mergeKind)
            assertEquals(2, keys.getValue("books").mergeKind)
            assertEquals(0, keys.getValue("ordinary").mergeKind)
            assertNull(keys.getValue("deleted").tagId)
            assertEquals(2L, db.noteDao().getById("n")!!.note.updatedAt)
            assertEquals("retained", db.noteDao().getById("n")!!.tags.single().id)
            assertEquals(8L, db.organizationDao().state()!!.generation)
            assertTrue(db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { !it.moveToFirst() })
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
