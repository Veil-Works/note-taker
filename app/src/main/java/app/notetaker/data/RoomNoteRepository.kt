package app.notetaker.data

import app.notetaker.data.local.NoteDao
import app.notetaker.data.local.NoteEntity
import app.notetaker.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomNoteRepository(
    private val noteDao: NoteDao,
) : NoteRepository {
    override fun observeNotes(query: String): Flow<List<Note>> {
        val normalizedQuery = query.trim()
        val entities = if (normalizedQuery.isEmpty()) {
            noteDao.observeAll()
        } else {
            noteDao.observeSearch(normalizedQuery.escapeForLike())
        }
        return entities.map { notes -> notes.map(NoteEntity::toModel) }
    }

    override fun observeNote(id: String): Flow<Note?> =
        noteDao.observeById(id).map { it?.toModel() }

    override suspend fun getNote(id: String): Note? = noteDao.getById(id)?.toModel()

    override suspend fun upsert(note: Note) {
        noteDao.upsert(note.toEntity())
    }

    override suspend fun delete(note: Note) {
        noteDao.delete(note.toEntity())
    }
}

internal fun String.escapeForLike(): String =
    replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

private fun NoteEntity.toModel() = Note(
    id = id,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun Note.toEntity() = NoteEntity(
    id = id,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

