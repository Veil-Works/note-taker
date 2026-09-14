package app.notetaker.data

import app.notetaker.model.Note
import app.notetaker.model.TagAlias
import app.notetaker.model.Tag
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeGraphEdges(): Flow<List<app.notetaker.model.GraphEdge>> = kotlinx.coroutines.flow.flowOf(emptyList())
    fun observeRelated(id: String): Flow<List<app.notetaker.model.RelatedNote>> = kotlinx.coroutines.flow.flowOf(emptyList())
    fun observeTopics(): Flow<List<app.notetaker.model.Topic>> = kotlinx.coroutines.flow.flowOf(emptyList())
    fun observeOrganizationStatus(): Flow<app.notetaker.model.OrganizationStatus> = kotlinx.coroutines.flow.flowOf(app.notetaker.model.OrganizationStatus())
    suspend fun retryOrganization() {}
    suspend fun renameTopic(id: String, name: String) {}
    suspend fun mergeTopics(target: String, source: String) {}
    suspend fun saveEdit(note: Note, tagChanges: Map<String, Boolean>) = upsert(note)

    fun observeTypes(): Flow<List<app.notetaker.model.EntityType>>
    suspend fun saveType(id: String?, name: String)
    suspend fun deleteType(id: String)
    suspend fun updateTag(id: String, name: String, typeId: String?)

    fun observeAliases(): Flow<List<TagAlias>>
    suspend fun saveAlias(tagId: String, aliasId: String?, text: String)
    suspend fun deleteAlias(id: String)
    fun observeSuggestions(noteId: String): Flow<List<Tag>>
    suspend fun dismissSuggestion(noteId: String, tagId: String, content: String)

    suspend fun deleteTag(id: String)
    suspend fun deleteNotes(ids: Set<String>)
    suspend fun tagNotes(ids: Set<String>, tagIds: Set<String>, removeTagIds: Set<String> = emptySet())
    suspend fun connectNotes(ids: Set<String>)

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
