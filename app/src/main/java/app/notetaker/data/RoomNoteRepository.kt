package app.notetaker.data

import app.notetaker.data.local.NoteDao
import app.notetaker.data.local.NoteEntity
import app.notetaker.data.local.NoteWithTags
import app.notetaker.data.local.TagEntity
import app.notetaker.data.local.TagAliasEntity
import app.notetaker.model.TagAlias
import app.notetaker.model.suggestTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import app.notetaker.model.Tag
import java.util.Locale
import java.util.UUID
import app.notetaker.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn

class RoomNoteRepository(
    private val noteDao: NoteDao,
) : NoteRepository {
    override fun observeAliases() = noteDao.observeAliases().map { rows ->
        rows.map { TagAlias(it.id, it.tagId, it.text) }
    }

    override suspend fun saveAlias(tagId: String, aliasId: String?, text: String) {
        val trimmed = text.trim()
        require(trimmed.isNotBlank()) { "Enter a keyword or phrase." }
        noteDao.saveAlias(TagAliasEntity(
            aliasId ?: UUID.randomUUID().toString(), tagId, trimmed, trimmed.lowercase(Locale.ROOT)))
    }

    override suspend fun deleteAlias(id: String) = noteDao.deleteAlias(id)

    override fun observeSuggestions(noteId: String): Flow<List<Tag>> = combine(
        observeNote(noteId), observeTags(), observeAliases(), noteDao.observeDismissals(noteId),
    ) { note, tags, aliases, dismissed ->
        if (note == null) emptyList() else suggestTags(
            note.content, tags, aliases, dismissed.toSet() + note.tags.map { it.id })
    }.flowOn(Dispatchers.Default)

    override suspend fun dismissSuggestion(noteId: String, tagId: String, content: String) =
        noteDao.dismiss(noteId, tagId, content)

    override suspend fun deleteTag(id: String) = noteDao.deleteTag(id)
    override suspend fun deleteNotes(ids: Set<String>) = noteDao.deleteNotes(ids.toList())
    override suspend fun tagNotes(ids: Set<String>, tagIds: Set<String>, removeTagIds: Set<String>) =
        noteDao.tagNotes(ids, tagIds, removeTagIds, System.currentTimeMillis())
    override suspend fun connectNotes(ids: Set<String>) = noteDao.connectNotes(ids)

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
        }, note.linkedNoteIds)
    }

    override suspend fun linkNotes(sourceId: String, targetId: String) = noteDao.connect(sourceId, targetId)

    override suspend fun unlinkNotes(sourceId: String, targetId: String) =
        noteDao.deleteNoteLink(minOf(sourceId, targetId), maxOf(sourceId, targetId))

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
    linkedNoteIds = (outgoingLinks.map { it.secondNoteId } + incomingLinks.map { it.firstNoteId }).toSet(),
    tags = tags.sortedBy { it.normalizedName }.map { Tag(it.id, it.name, it.createdAt) },
)

private fun Note.toEntity() = NoteEntity(
    id = id,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
