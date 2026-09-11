package app.notetaker

import app.notetaker.data.NoteRepository
import app.notetaker.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeNoteRepository(initialNotes: List<Note> = emptyList()) : NoteRepository {
    override fun observeTypes() = kotlinx.coroutines.flow.flowOf(emptyList<app.notetaker.model.EntityType>())
    override suspend fun saveType(id: String?, name: String) = Unit
    override suspend fun deleteType(id: String) = Unit
    override suspend fun updateTag(id: String, name: String, typeId: String?) = Unit

    override suspend fun deleteTag(id: String) {
        failure?.let { throw it }
        notes.value = notes.value.map { it.copy(tags = it.tags.filterNot { tag -> tag.id == id }) }
    }
    override suspend fun deleteNotes(ids: Set<String>) {
        failure?.let { throw it }
        notes.value.filter { it.id in ids }.forEach { delete(it) }
    }
    override suspend fun tagNotes(ids: Set<String>, tagIds: Set<String>, removeTagIds: Set<String>) {
        failure?.let { throw it }
        notes.value.filter { it.id in ids }.forEach { note ->
            upsert(note.copy(tags = (note.tags.filterNot { it.id in removeTagIds } + tagIds.map { app.notetaker.model.Tag(it, it, 0) }).distinctBy { it.id }))
        }
    }
    override suspend fun connectNotes(ids: Set<String>) {
        failure?.let { throw it }
        ids.sorted().forEachIndexed { index, source -> ids.sorted().drop(index + 1).forEach { linkNotes(source, it) } }
    }
    override fun observeAliases() = kotlinx.coroutines.flow.flowOf(emptyList<app.notetaker.model.TagAlias>())
    override suspend fun saveAlias(tagId: String, aliasId: String?, text: String) = Unit
    override suspend fun deleteAlias(id: String) = Unit
    override fun observeSuggestions(noteId: String) = kotlinx.coroutines.flow.flowOf(emptyList<app.notetaker.model.Tag>())
    override suspend fun dismissSuggestion(noteId: String, tagId: String, content: String) = Unit
    override fun observeTags() = kotlinx.coroutines.flow.flowOf(emptyList<app.notetaker.model.Tag>())
    override suspend fun createTag(name: String) = app.notetaker.model.Tag(name, name, 0)
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

    override suspend fun linkNotes(sourceId: String, targetId: String) {
        failure?.let { throw it }
        require(sourceId != targetId)
        notes.value = notes.value.map {
            when (it.id) {
                sourceId -> it.copy(linkedNoteIds = it.linkedNoteIds + targetId)
                targetId -> it.copy(linkedNoteIds = it.linkedNoteIds + sourceId)
                else -> it
            }
        }
    }

    override suspend fun unlinkNotes(sourceId: String, targetId: String) {
        failure?.let { throw it }
        notes.value = notes.value.map {
            when (it.id) {
                sourceId -> it.copy(linkedNoteIds = it.linkedNoteIds - targetId)
                targetId -> it.copy(linkedNoteIds = it.linkedNoteIds - sourceId)
                else -> it
            }
        }
    }

    override suspend fun delete(note: Note) {
        failure?.let { throw it }
        deletedNotes += note
        notes.value = notes.value.filterNot { it.id == note.id }
    }
}
