package app.notetaker

import app.notetaker.data.NoteRepository
import app.notetaker.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeNoteRepository(initialNotes: List<Note> = emptyList()) : NoteRepository {
    private val notes = MutableStateFlow(initialNotes)
    val savedNotes = mutableListOf<Note>()
    val deletedNotes = mutableListOf<Note>()
    val receivedQueries = mutableListOf<String>()
    var failure: Exception? = null

    override fun observeNotes(query: String): Flow<List<Note>> {
        failure?.let { throw it }
        receivedQueries += query
        return notes.map { current ->
            current
                .filter { query.isBlank() || it.content.contains(query.trim(), ignoreCase = true) }
                .sortedByDescending(Note::updatedAt)
        }
    }

    override fun observeNote(id: String): Flow<Note?> =
        notes.map { all -> all.firstOrNull { it.id == id } }

    override suspend fun getNote(id: String): Note? {
        failure?.let { throw it }
        return notes.value.firstOrNull { it.id == id }
    }

    override suspend fun upsert(note: Note) {
        failure?.let { throw it }
        savedNotes += note
        notes.value = notes.value.filterNot { it.id == note.id } + note
    }

    override suspend fun delete(note: Note) {
        failure?.let { throw it }
        deletedNotes += note
        notes.value = notes.value.filterNot { it.id == note.id }
    }
}

