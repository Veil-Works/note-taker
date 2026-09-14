package app.notetaker

import android.content.Context
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import androidx.room.Room
import app.notetaker.data.NoteRepository
import app.notetaker.data.RoomNoteRepository
import app.notetaker.data.local.NoteDatabase

interface AppContainer {
    val noteRepository: NoteRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    val database = Room.databaseBuilder(
        context.applicationContext,
        NoteDatabase::class.java,
        "notes.db",
    ).addMigrations(NoteDatabase.MIGRATION_1_2, NoteDatabase.MIGRATION_2_3, NoteDatabase.MIGRATION_3_4, NoteDatabase.MIGRATION_4_5, NoteDatabase.MIGRATION_5_6).addCallback(NoteDatabase.CALLBACK).build()

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    init {
        scope.launch {
            // Remove the retired label-generation experiment's private model copies.
            listOf("qwen2.5-1.5b-q4km-6a1a2eb6.gguf", "qwen3-0.6b-q8-9465e63a.gguf",
                "label-qwen3-0.6b-q8-b6500-p1.gguf", "trial-qwen15.gguf", "label-model.tmp").forEach {
                java.io.File(context.noBackupFilesDir, it).delete()
            }
            context.deleteSharedPreferences("generated_topic_labels")
            val preferences = context.getSharedPreferences("organization_rules", Context.MODE_PRIVATE)
            if (preferences.getInt("version", 0) < app.notetaker.organization.NotebookOrganizer.RULES_VERSION) {
                database.organizationDao().request()
                preferences.edit().putInt("version", app.notetaker.organization.NotebookOrganizer.RULES_VERSION).apply()
            }
            database.organizationDao().observeState().collectLatest { state ->
                if (state != null && state.generation != state.completed && state.error == null) {
                    kotlinx.coroutines.delay(350)
                    app.notetaker.organization.OrganizationWorker.schedule(context.applicationContext)
                }
            }
        }
    }
    override val noteRepository: NoteRepository = RoomNoteRepository(database.noteDao(), database.organizationDao())
}
