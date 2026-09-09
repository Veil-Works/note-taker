package app.notetaker.data

import app.notetaker.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(query: String = ""): Flow<List<Note>>
    fun observeNote(id: String): Flow<Note?>
    suspend fun getNote(id: String): Note?
    suspend fun upsert(note: Note)
    suspend fun delete(note: Note)
}

