package app.notetaker.data

import app.notetaker.data.local.NoteDao
import app.notetaker.data.local.NoteEntity
import app.notetaker.data.local.NoteWithTags
import app.notetaker.data.local.TagEntity
import app.notetaker.model.Tag
import java.util.Locale
import java.util.UUID
import app.notetaker.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomNoteRepository(
    private val noteDao: NoteDao,
) : NoteRepository {
    override fun observeTags(): Flow<List<Tag>> = noteDao.observeTags().map { tags -> tags.map { Tag(it.id, it.name, it.createdAt) } }

    override suspend fun createTag(name: String): Tag {
        val displayName = name.trim()
        require(displayName.isNotEmpty()) { "Enter a tag name." }
        require(displayName.length <= 40) { "Use 40 characters or fewer." }
        val normalizedName = displayName.lowercase(Locale.ROOT)
        noteDao.insertTag(TagEntity(UUID.randomUUID().toString(), displayName, normalizedName, System.currentTimeMillis()))
        val tag = checkNotNull(noteDao.findTag(normalizedName))
        return Tag(tag.id, tag.name, tag.createdAt)
    }
    override fun observeNotes(query: String): Flow<List<Note>> {
        val normalizedQuery = query.trim()
        val entities = if (normalizedQuery.isEmpty()) {
            noteDao.observeAll()
        } else {
            noteDao.observeSearch(normalizedQuery.escapeForLike())
        }
        return entities.map { notes -> notes.map(NoteWithTags::toModel) }
    }

    override fun observeNote(id: String): Flow<Note?> =
        noteDao.observeById(id).map { it?.toModel() }

    override suspend fun getNote(id: String): Note? = noteDao.getById(id)?.toModel()

    override suspend fun upsert(note: Note) {
        noteDao.saveWithTags(note.toEntity(), note.tags.distinctBy { it.id }.map {
            TagEntity(it.id, it.name, it.name.lowercase(Locale.ROOT), it.createdAt)
        })
    }

    override suspend fun delete(note: Note) {
        noteDao.delete(note.toEntity())
    }
}

internal fun String.escapeForLike(): String =
    replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

private fun NoteWithTags.toModel() = Note(
    id = note.id,
    content = note.content,
    createdAt = note.createdAt,
    updatedAt = note.updatedAt,
    tags = tags.sortedBy { it.normalizedName }.map { Tag(it.id, it.name, it.createdAt) },
)

private fun Note.toEntity() = NoteEntity(
    id = id,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
