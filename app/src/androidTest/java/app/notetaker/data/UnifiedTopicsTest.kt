package app.notetaker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.notetaker.data.local.*
import app.notetaker.model.Note
import app.notetaker.organization.NotebookOrganizer
import app.notetaker.organization.TextEmbedding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class UnifiedTopicsTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun database() = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).addCallback(NoteDatabase.CALLBACK).build()
    private val model = object : TextEmbedding {
        override val version = "unified-test"
        override fun embed(text: String) = if (text.startsWith("Orion")) floatArrayOf(1f, 0f) else floatArrayOf(0f, 1f)
    }

    @Test fun rediscovery_consolidates_untouched_topics_and_preserves_redirects_and_user_names() = runTest {
        val db = database()
        try {
            val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            val dao = db.organizationDao()
            listOf("a", "b", "c", "d").forEach { repo.upsert(Note(it, "Orion release $it", 1, 1)) }
            val retained = repo.createTag("My release notes")
            dao.insertDiscoveredTags(listOf(
                TagEntity("old-a", "Session", "session", 1, provisional = true),
                TagEntity("old-b", "Session (2)", "session (2)", 2, provisional = true),
            ))
            dao.insertAutomaticTags(listOf("a", "b", "c", "d").map {
                AutomaticTagEntity(it, if (it < "c") "old-a" else "old-b", "Old inference", .5)
            })
            dao.saveDiscoveryKeys(listOf(TopicDiscoveryKeyEntity("previous-b", "old-b")))
            val organizer = NotebookOrganizer(db, model)
            assertTrue(organizer.runOnce())
            val topics = repo.observeTags().first()
            assertEquals(2, topics.size)
            assertEquals("My release notes", topics.single { it.id == retained.id }.name)
            val inferred = topics.single { it.provisional }
            assertEquals("old-a", inferred.id)
            assertNotEquals("Session", inferred.name)
            assertEquals(setOf("a", "b", "c", "d"), repo.observeTopics().first().single { it.id == inferred.id }.noteIds)
            assertEquals(inferred.id, repo.observeTopicRedirects().first()["old-b"])
            assertEquals(inferred.id, dao.discoveryKeys().single { it.key == "previous-b" }.tagId)
            repo.retryOrganization(); assertTrue(organizer.runOnce())
            assertEquals(topics.map { it.id to it.name }, repo.observeTags().first().map { it.id to it.name })
            assertTrue(repo.observeNotes().first().all { it.updatedAt == 1L })
            assertFalse(repo.observeOrganizationStatus().first().pending)
        } finally { db.close() }
    }

    @Test fun discovered_topics_share_the_picker_and_keep_identity_without_freezing_membership() = runTest {
        val db = database()
        try {
            val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            repo.upsert(Note("a", "Orion launch", 1, 1))
            repo.upsert(Note("b", "Orion planning", 2, 2))
            val organizer = NotebookOrganizer(db, model)
            assertTrue(organizer.runOnce())
            val topic = repo.observeTags().first().single()
            assertTrue(topic.provisional)
            assertEquals(topic.id, repo.observeTopics().first().single().id)
            assertEquals(setOf("a", "b"), repo.observeTopics().first().single().noteIds)
            assertEquals(setOf(topic.id), repo.getNote("a")!!.automaticTagReasons.keys)
            assertFalse(repo.observeOrganizationStatus().first().pending)
            repo.retryOrganization(); assertTrue(organizer.runOnce())
            assertEquals(topic.id, repo.observeTags().first().single().id)
            repo.renameTopic(topic.id, "Launch project")
            repo.saveEdit(repo.getNote("a")!!, mapOf(topic.id to true))
            repo.saveEdit(repo.getNote("b")!!.copy(content = "Basil watering"), emptyMap())
            assertTrue(organizer.runOnce())
            assertEquals("Launch project", repo.observeTags().first().single().name)
            assertFalse(repo.observeTags().first().single().provisional)
            assertEquals(setOf("a"), repo.observeTopics().first().single().noteIds)
            assertFalse(topic.id in repo.getNote("a")!!.automaticTagReasons)
            assertEquals(1L, repo.getNote("a")!!.updatedAt)
        } finally { db.close() }
    }

    @Test fun generated_names_do_not_become_training_evidence_and_renamed_groups_can_grow() = runTest {
        val db = database()
        try {
            val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            repo.upsert(Note("a", "Orion launch", 1, 1)); repo.upsert(Note("b", "Orion plan", 2, 2))
            val organizer = NotebookOrganizer(db, model)
            organizer.runOnce()
            val provisional = repo.observeTags().first().single()
            repo.deleteNotes(setOf("b")); organizer.runOnce()
            assertTrue(repo.getNote("a")!!.tags.isEmpty())
            assertTrue(repo.observeTags().first().isEmpty())
            repo.upsert(Note("b", "Orion plan", 2, 2)); organizer.runOnce()
            val retained = repo.observeTags().first().single()
            assertNotEquals(provisional.id, retained.id)
            repo.renameTopic(retained.id, "My launch")
            repeat(8) { repo.upsert(Note("extra$it", "Orion task $it", 3, 3)) }
            organizer.runOnce()
            assertEquals(listOf(retained.id), repo.observeTags().first().map { it.id })
            assertEquals(10, repo.observeTopics().first().single().noteIds.size)
        } finally { db.close() }
    }

    @Test fun automatic_topics_expire_but_rejections_and_manual_topics_survive() = runTest {
        val db = database()
        try {
            val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            repo.upsert(Note("a", "Orion launch", 1, 1)); repo.upsert(Note("b", "Orion plan", 2, 2))
            val organizer = NotebookOrganizer(db, model)
            organizer.runOnce()
            val rejected = repo.observeTags().first().single()
            repo.saveEdit(repo.getNote("a")!!, mapOf(rejected.id to false))
            repo.saveEdit(repo.getNote("b")!!, mapOf(rejected.id to false))
            repo.upsert(Note("c", "Orion demo", 3, 3))
            organizer.runOnce()
            assertEquals(listOf(rejected.id), repo.observeTags().first().map { it.id })
            assertTrue(repo.observeNotes().first().all { it.tags.isEmpty() })
            repo.saveEdit(repo.getNote("a")!!.copy(content = "Orion revised launch"), emptyMap())
            organizer.runOnce()
            assertTrue(rejected.id in repo.getNote("a")!!.rejectedTagIds)
            assertTrue(repo.getNote("a")!!.tags.isEmpty())
            repo.upsert(Note("d", "Basil watering", 4, 4)); repo.upsert(Note("e", "Basil pots", 5, 5))
            organizer.runOnce()
            val temporary = repo.observeTags().first().single { it.provisional }
            repo.deleteNotes(setOf("d", "e")); organizer.runOnce()
            assertFalse(repo.observeTags().first().any { it.id == temporary.id })
            assertTrue(repo.observeTopicRedirects().first().containsKey(temporary.id))
            assertNull(repo.observeTopicRedirects().first()[temporary.id])
            assertTrue(repo.observeTags().first().any { it.id == rejected.id })
        } finally { db.close() }
    }

    @Test fun a_note_can_have_multiple_automatic_topics_without_creating_a_duplicate_group() = runTest {
        val db = database()
        try {
            val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            val a = repo.createTag("Project work")
            val b = repo.createTag("Release planning")
            repo.upsert(Note("a", "Orion launch", 1, 1, listOf(a, b)))
            repo.upsert(Note("b", "Orion plan", 2, 2, listOf(a, b)))
            repo.upsert(Note("c", "Orion demo", 3, 3))
            NotebookOrganizer(db, model).runOnce()
            assertEquals(setOf(a.id, b.id), repo.getNote("c")!!.automaticTagReasons.keys)
            assertEquals(2, repo.observeTopics().first().size)
        } finally { db.close() }
    }

    @Test fun merge_preserves_assignments_exclusions_aliases_types_and_discovery_lineage() = runTest {
        val db = database()
        try {
            val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            repo.upsert(Note("a", "Orion launch", 1, 1)); repo.upsert(Note("b", "Orion plan", 2, 2))
            repo.upsert(Note("c", "Basil watering", 3, 3)); repo.upsert(Note("d", "Basil pots", 4, 4))
            val organizer = NotebookOrganizer(db, model)
            organizer.runOnce()
            val target = repo.getNote("a")!!.tags.single()
            val source = repo.getNote("c")!!.tags.single()
            repo.saveAlias(source.id, null, "Garden chores")
            val type = repo.observeTypes().first().first()
            repo.updateTag(source.id, source.name, type.id)
            repo.saveEdit(repo.getNote("a")!!, mapOf(target.id to true))
            repo.saveEdit(repo.getNote("d")!!, mapOf(source.id to false))
            repo.mergeTopics(target.id, source.id)
            assertEquals(target.id, repo.observeTags().first().single().id)
            assertEquals(type.id, repo.observeTags().first().single().typeId)
            assertTrue(repo.observeAliases().first().any { it.text == "Garden chores" && it.tagId == target.id })
            assertTrue(repo.observeAliases().first().any { it.text == source.name && it.tagId == target.id })
            assertTrue(target.id in repo.getNote("d")!!.rejectedTagIds)
            assertTrue(target.id in repo.getNote("c")!!.automaticTagReasons)
            organizer.runOnce(); repo.retryOrganization(); organizer.runOnce()
            assertEquals(listOf(target.id), repo.observeTags().first().map { it.id })
            assertTrue(target.id in repo.getNote("d")!!.rejectedTagIds)
            assertTrue(repo.getNote("d")!!.tags.isEmpty())
            assertEquals(1L, repo.getNote("a")!!.updatedAt)
            assertFalse(repo.observeOrganizationStatus().first().pending)
        } finally { db.close() }
    }

    @Test fun merge_keeps_explicit_inclusions_even_with_different_legacy_types() = runTest {
        val db = database()
        try {
            val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            val target = repo.createTag("One"); val source = repo.createTag("Two")
            repo.upsert(Note("a", "Orion note", 1, 1, listOf(source)))
            repo.saveEdit(repo.getNote("a")!!, mapOf(target.id to false))
            val types = repo.observeTypes().first()
            repo.updateTag(target.id, target.name, types[0].id)
            repo.updateTag(source.id, source.name, types[1].id)
            repo.mergeTopics(target.id, source.id)
            assertEquals(target.id, repo.getNote("a")!!.tags.single().id)
            assertTrue(repo.getNote("a")!!.rejectedTagIds.isEmpty())
            assertTrue(repo.getNote("a")!!.automaticTagReasons.isEmpty())
        } finally { db.close() }
    }

    @Test fun v6_migration_preserves_existing_tags_groups_and_colliding_names_without_guessing_merges() = runTest {
        val name = "unified-${UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        val schema = org.json.JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("app.notetaker.data.local.NoteDatabase/6.json").bufferedReader().use { it.readText() }).getJSONObject("database")
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
            old.execSQL("INSERT INTO notes VALUES ('a', 'Orion launch', 1, 2), ('b', 'Orion plan', 3, 4), ('c', 'Basil watering', 5, 6)")
            old.execSQL("INSERT INTO entity_types VALUES ('project', 'Project', 'project')")
            old.execSQL("INSERT INTO tags VALUES ('run', 'Running', 'running', 7, 'project')")
            old.execSQL("INSERT INTO note_tags VALUES ('a', 'run')")
            old.execSQL("INSERT INTO automatic_tags VALUES ('c', 'run', 'Original reason', 0.9)")
            old.execSQL("INSERT INTO organization_rejections VALUES ('b', 'run', 'Orion plan')")
            old.execSQL("INSERT INTO tag_dismissals VALUES ('b', 'run')")
            old.execSQL("INSERT INTO tag_aliases VALUES ('alias', 'run', 'jog', 'jog')")
            old.execSQL("INSERT INTO note_links VALUES ('a', 'b')")
            old.execSQL("INSERT INTO topics VALUES ('run', 'Running', 0, 0), ('named', 'My project', 1, 0), ('merged', 'Garden', 1, 1)")
            old.execSQL("INSERT INTO topic_notes VALUES ('run', 'a'), ('run', 'b'), ('named', 'b'), ('merged', 'c')")
            old.version = 6
        }
        val db = Room.databaseBuilder(context, NoteDatabase::class.java, name).addMigrations(NoteDatabase.MIGRATION_6_7).build()
        try {
            val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            val topics = repo.observeTags().first()
            assertEquals(4, topics.size)
            assertEquals("Project", topics.single { it.id == "run" }.typeName)
            assertEquals("Running (2)", topics.single { it.id == "migrated-run" }.name)
            assertTrue(topics.single { it.id == "migrated-run" }.provisional)
            assertFalse(topics.single { it.id == "named" }.provisional)
            assertTrue("migrated-run" in repo.getNote("a")!!.automaticTagReasons)
            assertFalse("merged" in repo.getNote("c")!!.automaticTagReasons)
            assertTrue(repo.getNote("c")!!.tags.any { it.id == "merged" })
            assertEquals("Original reason", repo.getNote("c")!!.automaticTagReasons["run"])
            assertEquals(setOf("run"), repo.getNote("b")!!.rejectedTagIds)
            assertEquals(listOf("run"), db.noteDao().observeDismissals("b").first())
            assertEquals("jog", repo.observeAliases().first().single().text)
            assertEquals(setOf("b"), repo.getNote("a")!!.linkedNoteIds)
            assertEquals(2L, repo.getNote("a")!!.updatedAt)
            assertTrue(repo.observeOrganizationStatus().first().pending)
            assertTrue(db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { !it.moveToFirst() })
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
