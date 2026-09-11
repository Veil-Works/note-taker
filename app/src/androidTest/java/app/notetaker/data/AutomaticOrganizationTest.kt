package app.notetaker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.notetaker.data.local.*
import app.notetaker.model.Note
import app.notetaker.organization.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

class AutomaticOrganizationTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun database() = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).addCallback(NoteDatabase.CALLBACK).build()

    @Test fun real_model_benchmark_and_semantic_neighbors() = runTest {
        withContext(Dispatchers.Default) {
            val started = System.nanoTime()
            LocalTextEmbedding(context).use { model ->
                val texts = listOf("Bench press 80 kg, 3 sets of 5 reps", "Barbell chest workout at the gym", "Fix a runtime exception in the Android app", "Debugging a software crash", "Book a flight and hotel for the holiday", "Reserve accommodation for our vacation", "run 7km thursday", "Running five kilometers this morning")
                val vectors = texts.map(model::embed)
                val elapsed = (System.nanoTime() - started) / 1_000_000
                texts.indices.forEach { i ->
                    android.util.Log.i("OrganizerBenchmark", "$i: " + texts.indices.filter { it != i }.joinToString { j -> "$j=${"%.3f".format(java.util.Locale.ROOT, similarity(vectors[i], vectors[j]))}" })
                }
                android.util.Log.i("OrganizerBenchmark", "Model load + ${texts.size} notes: ${elapsed}ms; vector dimensions=${vectors.first().size}")
                for ((a,b) in listOf(0 to 1, 2 to 3, 4 to 5, 6 to 7)) {
                    assertTrue("Expected semantic pair $a/$b above unrelated note", similarity(vectors[a], vectors[b]) > similarity(vectors[a], vectors[(b + 2) % vectors.size]))
                }
                assertTrue("Embedding must use all chunks", !model.embed("x ".repeat(150) + texts[0]).contentEquals(model.embed("x ".repeat(150) + texts[2])))
            }
        }
    }

    @Test fun automatic_matches_corrections_and_content_saves_survive_reopen() = runTest {
        val name = "automatic-${java.util.UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, NoteDatabase::class.java, name).addCallback(NoteDatabase.CALLBACK).build()
        var db = open()
        val model = object : TextEmbedding { override val version = "test"; override fun embed(text: String) = if (text.contains("bench", true)) floatArrayOf(1f,0f) else floatArrayOf(0f,1f) }
        try {
            var repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            val tag = repo.createTag("Bench press")
            repo.upsert(Note("a", "Bench press 80kg", 1, 2))
            repo.upsert(Note("b", "bench training", 3, 4))
            assertTrue(NotebookOrganizer(db, model).runOnce())
            val organized = repo.getNote("a")!!
            assertEquals(tag.id, organized.tags.single().id)
            assertTrue(tag.id in organized.automaticTagReasons)
            assertEquals(2L, organized.updatedAt)
            assertEquals("b", repo.observeRelated("a").first().single().noteId)
            assertEquals(1, repo.observeTopics().first().size)
            // A text-only save from a stale editor must not replace organization.
            repo.saveEdit(Note("a", "Bench press 82kg", 1, 5), emptyMap())
            assertEquals(tag.id, repo.getNote("a")!!.tags.single().id)
            repo.saveEdit(repo.getNote("a")!!, mapOf(tag.id to false))
            assertTrue(repo.getNote("a")!!.tags.isEmpty())
            assertTrue(NotebookOrganizer(db, model).runOnce())
            assertTrue(repo.getNote("a")!!.tags.isEmpty())
            db.close(); db = open(); repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            repo.saveEdit(repo.getNote("a")!!.copy(content = "Bench press again"), emptyMap())
            assertTrue(NotebookOrganizer(db, model).runOnce())
            assertTrue(repo.getNote("a")!!.tags.isEmpty())
            repo.saveEdit(repo.getNote("a")!!, mapOf(tag.id to true))
            assertFalse(tag.id in repo.getNote("a")!!.automaticTagReasons)
            assertTrue(repo.getNote("a")!!.rejectedTagIds.isEmpty())
            repo.saveEdit(repo.getNote("a")!!.copy(content = "Completely changed subject"), emptyMap())
            assertTrue(NotebookOrganizer(db, model).runOnce())
            assertEquals(tag.id, repo.getNote("a")!!.tags.single().id)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun stale_generation_cannot_commit_and_unchanged_notes_reuse_embeddings() = runTest {
        val db = database()
        try {
            val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            repo.upsert(Note("a", "Original", 1, 2))
            var count = 0
            val model = object : TextEmbedding { override val version = "test"; override fun embed(text: String): FloatArray { count++; return floatArrayOf(1f,0f) } }
            val old = db.organizationDao().state()!!.generation
            repo.saveEdit(repo.getNote("a")!!.copy(content = "Changed"), emptyMap())
            assertFalse(db.organizationDao().apply(old, emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
            assertTrue(NotebookOrganizer(db, model).runOnce())
            assertEquals(1, count)
            db.organizationDao().request()
            assertTrue(NotebookOrganizer(db, model).runOnce())
            assertEquals(1, count)
            repo.delete(repo.getNote("a")!!)
            assertTrue(NotebookOrganizer(db, model).runOnce())
            assertTrue(db.organizationDao().embeddings().isEmpty())
            assertTrue(repo.observeTopics().first().isEmpty())
        } finally { db.close() }
    }
    @Test fun v5_migration_preserves_manual_organization_and_queues_existing_notes() = runTest {
        val name = "v7-migrate-${java.util.UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        val schema = org.json.JSONObject(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets
            .open("app.notetaker.data.local.NoteDatabase/5.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                val indices = entity.getJSONArray("indices")
                for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val views = schema.getJSONArray("views")
            for (i in 0 until views.length()) {
                val view = views.getJSONObject(i)
                old.execSQL(view.getString("createSql").replace("\${VIEW_NAME}", view.getString("viewName")))
            }
            old.execSQL("INSERT INTO notes VALUES ('a', 'Bench press', 1, 2), ('b', 'Other note', 3, 4)")
            old.execSQL("INSERT INTO entity_types VALUES ('exercise', 'Exercise', 'exercise')")
            old.execSQL("INSERT INTO tags VALUES ('bench', 'Bench press', 'bench press', 1, 'exercise')")
            old.execSQL("INSERT INTO note_tags VALUES ('a', 'bench')")
            old.execSQL("INSERT INTO note_links VALUES ('a', 'b')")
            old.execSQL("INSERT INTO tag_aliases VALUES ('alias', 'bench', 'barbell', 'barbell')")
            old.execSQL("INSERT INTO tag_dismissals VALUES ('b', 'bench')")
            old.version = 5
        }
        val db = Room.databaseBuilder(context, NoteDatabase::class.java, name).addMigrations(NoteDatabase.MIGRATION_5_6).build()
        try {
            val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            assertEquals("Exercise", repo.getNote("a")!!.tags.single().typeName)
            assertTrue(repo.getNote("a")!!.automaticTagReasons.isEmpty())
            assertEquals(setOf("b"), repo.getNote("a")!!.linkedNoteIds)
            assertEquals("barbell", repo.observeAliases().first().single().text)
            assertEquals(listOf("bench"), db.noteDao().observeDismissals("b").first())
            assertTrue(repo.observeOrganizationStatus().first().pending)
            val before = db.organizationDao().state()!!.generation
            repo.saveEdit(repo.getNote("a")!!.copy(content = "Edited"), emptyMap())
            assertTrue(db.organizationDao().state()!!.generation > before)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun learned_tags_use_manual_examples_and_rejections_and_undo_restores_corrections() = runTest {
        val db = database()
        val model = object : TextEmbedding { override val version = "test"; override fun embed(text: String) = floatArrayOf(1f,0f) }
        try {
            val repo = RoomNoteRepository(db.noteDao(), db.organizationDao())
            val tag = repo.createTag("Training")
            repo.upsert(Note("a", "Weights session", 1, 1, listOf(tag)))
            repo.upsert(Note("b", "Strength session", 2, 2, listOf(tag)))
            repo.upsert(Note("c", "Barbell practice", 3, 3))
            assertTrue(NotebookOrganizer(db, model).runOnce())
            assertTrue(repo.getNote("c")!!.automaticTagReasons.getValue(tag.id).startsWith("Similar"))
            repo.saveEdit(repo.getNote("c")!!, mapOf(tag.id to false))
            val rejected = repo.getNote("c")!!
            repo.delete(rejected); repo.upsert(rejected)
            assertEquals(setOf(tag.id), repo.getNote("c")!!.rejectedTagIds)
            repo.upsert(Note("d", "A similar but rejected session", 4, 4))
            assertTrue(NotebookOrganizer(db, model).runOnce())
            assertTrue(repo.getNote("d")!!.tags.isEmpty())
            assertEquals(tag.id, repo.getNote("a")!!.tags.single().id)
            val topic = repo.observeTopics().first().single()
            repo.renameTopic(topic.id, "Sessions")
            assertTrue(NotebookOrganizer(db, model).runOnce())
            assertEquals("Sessions", repo.observeTopics().first().single().name)
        } finally { db.close() }
    }

}
