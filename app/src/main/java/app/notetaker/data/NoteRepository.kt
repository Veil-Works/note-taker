package app.notetaker.data

import app.notetaker.model.Note
import app.notetaker.model.Tag
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeTags(): Flow<List<Tag>>
    suspend fun createTag(name: String): Tag
    fun observeNotes(query: String = ""): Flow<List<Note>>
    fun observeNote(id: String): Flow<Note?>
    suspend fun getNote(id: String): Note?
    suspend fun upsert(note: Note)
    suspend fun linkNotes(sourceId: String, targetId: String)
    suspend fun unlinkNotes(sourceId: String, targetId: String)
    suspend fun delete(note: Note)
}
