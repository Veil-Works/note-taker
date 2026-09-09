package app.notetaker

import android.content.Context
import androidx.room.Room
import app.notetaker.data.NoteRepository
import app.notetaker.data.RoomNoteRepository
import app.notetaker.data.local.NoteDatabase

interface AppContainer {
    val noteRepository: NoteRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        NoteDatabase::class.java,
        "notes.db",
    ).addMigrations(NoteDatabase.MIGRATION_1_2).build()

    override val noteRepository: NoteRepository = RoomNoteRepository(database.noteDao())
}
