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
