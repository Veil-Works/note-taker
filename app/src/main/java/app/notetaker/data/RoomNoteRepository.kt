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
    private val organizationDao: app.notetaker.data.local.OrganizationDao? = null,
) : NoteRepository {
    override fun observeRelated(id: String) = organizationDao?.related(id)?.map { rows -> rows.map { app.notetaker.model.RelatedNote(it.targetId, it.score) } } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    override fun observeTopics() = organizationDao?.observeTopics()?.map { rows -> rows.map { app.notetaker.model.Topic(it.topic.id, it.topic.name, it.members.map { m -> m.noteId }.toSet(), it.topic.customized) }.filter { it.noteIds.isNotEmpty() } } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    override fun observeOrganizationStatus() = organizationDao?.observeState()?.map { app.notetaker.model.OrganizationStatus(it != null && it.completed != it.generation, it?.error) } ?: kotlinx.coroutines.flow.flowOf(app.notetaker.model.OrganizationStatus())
    override suspend fun retryOrganization() { organizationDao?.request() }
    override suspend fun renameTopic(id: String, name: String) { organizationDao?.renameTopic(id, validatedName(name)) }
    override suspend fun mergeTopics(target: String, source: String) { organizationDao?.mergeTopics(target, source) }
    override suspend fun saveEdit(note: Note, tagChanges: Map<String, Boolean>) {
        noteDao.saveEdit(note.toEntity(), tagChanges)
    }

    override fun observeTypes() = noteDao.observeTypes().map { rows -> rows.map { app.notetaker.model.EntityType(it.id, it.name) } }

    private fun validatedName(name: String): String {
        val value = name.trim()
        require(value.isNotBlank()) { "Enter a name." }
        require(value.length <= 40) { "Use 40 characters or fewer." }
        return value
    }
    override suspend fun saveType(id: String?, name: String) {
        val value = validatedName(name)
        if (id == null) noteDao.insertType(app.notetaker.data.local.EntityTypeEntity(UUID.randomUUID().toString(), value, value.lowercase(Locale.ROOT)))
        else check(noteDao.renameType(id, value, value.lowercase(Locale.ROOT)) == 1) { "This type no longer exists." }
    }
    override suspend fun deleteType(id: String) = noteDao.deleteType(id)
    override suspend fun updateTag(id: String, name: String, typeId: String?) {
        val value = validatedName(name)
        check(noteDao.updateTag(id, value, value.lowercase(Locale.ROOT), typeId) == 1) { "This concept no longer exists." }
    }

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
            note.content, tags, aliases, dismissed.toSet() + note.rejectedTagIds + note.tags.map { it.id })
    }.flowOn(Dispatchers.Default)

    override suspend fun dismissSuggestion(noteId: String, tagId: String, content: String) =
        noteDao.dismiss(noteId, tagId, content)

    override suspend fun deleteTag(id: String) = noteDao.deleteTag(id)
    override suspend fun deleteNotes(ids: Set<String>) = noteDao.deleteNotes(ids.toList())
    override suspend fun tagNotes(ids: Set<String>, tagIds: Set<String>, removeTagIds: Set<String>) =
        noteDao.tagNotes(ids, tagIds, removeTagIds, System.currentTimeMillis())
    override suspend fun connectNotes(ids: Set<String>) = noteDao.connectNotes(ids)

    override fun observeTags(): Flow<List<Tag>> = noteDao.observeTags().map { tags -> tags.map { Tag(it.id, it.name, it.createdAt, it.typeId, it.typeName) } }

    override suspend fun createTag(name: String): Tag {
        val displayName = name.trim()
        require(displayName.isNotEmpty()) { "Enter a tag name." }
        require(displayName.length <= 40) { "Use 40 characters or fewer." }
        val normalizedName = displayName.lowercase(Locale.ROOT)
        noteDao.insertTag(TagEntity(UUID.randomUUID().toString(), displayName, normalizedName, System.currentTimeMillis()))
        val tag = checkNotNull(noteDao.findTag(normalizedName))
        return Tag(tag.id, tag.name, tag.createdAt, tag.typeId, tag.typeName)
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
        noteDao.saveWithTags(note.toEntity(), note.tags.filterNot { it.id in note.automaticTagReasons }.distinctBy { it.id }.map {
            TagEntity(it.id, it.name, it.name.lowercase(Locale.ROOT), it.createdAt, it.typeId)
        }, note.linkedNoteIds, note.automaticTagReasons.map { (id, reason) ->
            app.notetaker.data.local.AutomaticTagEntity(note.id, id, reason, 1.0)
        }, note.rejectedTagIds)
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
    tags = (tags + automaticTags.filterNot { auto -> rejections.any { it.tagId == auto.id } }).distinctBy { it.id }.sortedBy { it.normalizedName }.map { Tag(it.id, it.name, it.createdAt, it.typeId, it.typeName) },
    automaticTagReasons = automaticReasons.filterNot { auto -> tags.any { it.id == auto.tagId } || rejections.any { it.tagId == auto.tagId } }.associate { it.tagId to it.reason },
    rejectedTagIds = rejections.map { it.tagId }.toSet(),
)

private fun Note.toEntity() = NoteEntity(
    id = id,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
