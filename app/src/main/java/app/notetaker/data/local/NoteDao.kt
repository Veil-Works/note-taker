package app.notetaker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Transaction
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun restoreAutomaticTags(rows: List<AutomaticTagEntity>)

    @Query("DELETE FROM automatic_tags WHERE noteId = :noteId AND tagId = :tagId")
    suspend fun removeAutomaticTag(noteId: String, tagId: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun rejectTag(row: OrganizationRejectionEntity)
    @Query("DELETE FROM organization_rejections WHERE noteId = :noteId AND tagId = :tagId")
    suspend fun clearRejection(noteId: String, tagId: String)
    @Query("DELETE FROM tag_dismissals WHERE noteId = :noteId AND tagId = :tagId")
    suspend fun clearDismissal(noteId: String, tagId: String)

    @Transaction suspend fun changeTags(noteId: String, changes: Map<String, Boolean>) {
        val note = checkNotNull(getById(noteId)) { "This note no longer exists." }
        changes.forEach { (tagId, attached) ->
            check(existingTagIds(listOf(tagId)).isNotEmpty()) { "This concept no longer exists." }
            if (attached) {
                clearRejection(noteId, tagId)
                clearDismissal(noteId, tagId)
                insertLinks(listOf(NoteTagEntity(noteId, tagId)))
            } else {
                removeTags(noteId, listOf(tagId))
                rejectTag(OrganizationRejectionEntity(noteId, tagId, note.note.content))
            }
            removeAutomaticTag(noteId, tagId)
        }
    }
    @Transaction suspend fun saveEdit(note: NoteEntity, changes: Map<String, Boolean>) {
        check(getById(note.id) != null) { "This note no longer exists." }
        val previous = getById(note.id)!!
        if (previous.note.content != note.content) clearDismissals(note.id)
        upsert(note)
        changeTags(note.id, changes)
    }

    @Query("SELECT * FROM entity_types ORDER BY normalizedName")
    fun observeTypes(): Flow<List<EntityTypeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM entity_types WHERE id = :id)")
    suspend fun typeExists(id: String): Boolean

    @Insert
    suspend fun insertType(type: EntityTypeEntity)

    @Query("UPDATE entity_types SET name = :name, normalizedName = :normalized WHERE id = :id")
    suspend fun renameType(id: String, name: String, normalized: String): Int

    @Query("DELETE FROM entity_types WHERE id = :id")
    suspend fun deleteType(id: String)

    @Query("UPDATE tags SET name = :name, normalizedName = :normalized, typeId = :typeId WHERE id = :id")
    suspend fun updateTag(id: String, name: String, normalized: String, typeId: String?): Int

    @Query("SELECT * FROM tag_aliases ORDER BY normalizedText, id")
    fun observeAliases(): Flow<List<TagAliasEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlias(alias: TagAliasEntity)

    @Query("DELETE FROM tag_aliases WHERE id = :id")
    suspend fun deleteAlias(id: String)

    @Transaction
    suspend fun saveAlias(alias: TagAliasEntity) {
        deleteAlias(alias.id)
        insertAlias(alias)
    }

    @Query("SELECT tagId FROM tag_dismissals WHERE noteId = :noteId")
    fun observeDismissals(noteId: String): Flow<List<String>>

    @Query("DELETE FROM tag_dismissals WHERE noteId = :noteId")
    suspend fun clearDismissals(noteId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDismissal(dismissal: TagDismissalEntity)

    @Transaction
    suspend fun dismiss(noteId: String, tagId: String, content: String) {
        // An evaluation of older text must not dismiss a suggestion for newer text.
        if (getById(noteId)?.note?.content == content) insertDismissal(TagDismissalEntity(noteId, tagId))
    }

    @Transaction
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NoteWithTags>>

    @Transaction
    @Query(
        """
        SELECT * FROM notes
        WHERE content LIKE '%' || :escapedQuery || '%' ESCAPE '\'
        COLLATE NOCASE
        ORDER BY updatedAt DESC
        """,
    )
    fun observeSearch(escapedQuery: String): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<NoteWithTags?>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NoteWithTags?

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: String)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteNotes(ids: List<String>)

    @Query("SELECT id FROM tags WHERE id IN (:ids)")
    suspend fun existingTagIds(ids: List<String>): List<String>

    @Query("DELETE FROM note_tags WHERE noteId = :noteId AND tagId IN (:tagIds)")
    suspend fun removeTags(noteId: String, tagIds: List<String>)

    @Transaction
    suspend fun tagNotes(ids: Set<String>, tagIds: Set<String>, removeTagIds: Set<String>, timestamp: Long) {
        require(tagIds.intersect(removeTagIds).isEmpty()) { "A tag cannot be added and removed together." }
        check(existingTagIds(tagIds.toList()).toSet() == tagIds) { "A selected tag no longer exists." }
        val notes = ids.map { checkNotNull(getById(it)) { "A selected note no longer exists." } }
        notes.forEach { row ->
            changeTags(row.note.id, tagIds.associateWith { true } + removeTagIds.associateWith { false })
            if (tagIds.isNotEmpty() || removeTagIds.isNotEmpty()) upsert(row.note.copy(updatedAt = timestamp))
        }
    }

    @Transaction
    suspend fun connectNotes(ids: Set<String>) {
        val ordered = ids.sorted()
        ordered.forEach { check(getById(it) != null) { "A selected note no longer exists." } }
        ordered.forEachIndexed { index, source ->
            ordered.drop(index + 1).forEach { target -> insertNoteLink(NoteLinkEntity(source, target)) }
        }
    }

    @Query("SELECT * FROM tag_details ORDER BY normalizedName")
    fun observeTags(): Flow<List<TagDetails>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity)

    @Query("SELECT * FROM tag_details WHERE normalizedName = :name LIMIT 1")
    suspend fun findTag(name: String): TagDetails?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLinks(links: List<NoteTagEntity>)

    @Query("DELETE FROM note_tags WHERE noteId = :id")
    suspend fun clearLinks(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteLink(link: NoteLinkEntity)

    @Query("DELETE FROM note_links WHERE firstNoteId = :first AND secondNoteId = :second")
    suspend fun deleteNoteLink(first: String, second: String)

    @Transaction
    suspend fun connect(source: String, target: String) {
        require(source != target) { "A note cannot link to itself." }
        check(getById(source) != null && getById(target) != null) { "This note no longer exists." }
        insertNoteLink(NoteLinkEntity(minOf(source, target), maxOf(source, target)))
    }

    @Transaction
    suspend fun saveWithTags(note: NoteEntity, tags: List<TagEntity>, linkedNoteIds: Set<String>, automatic: List<AutomaticTagEntity> = emptyList(), rejected: Set<String> = emptySet()) {
        val previous = getById(note.id)
        val restoring = previous == null
        if (previous != null && previous.note.content != note.content) clearDismissals(note.id)
        upsert(note)
        tags.forEach { tag ->
            // Undo can carry a snapshot of a type deleted since the note was removed.
            val validType = tag.typeId?.takeIf { typeExists(it) }
            insertTag(tag.copy(typeId = validType))
        }
        clearLinks(note.id)
        insertLinks(tags.map { NoteTagEntity(note.id, it.id) })
        if (restoring) {
            val available = existingTagIds((automatic.map { it.tagId } + rejected).distinct()).toSet()
            restoreAutomaticTags(automatic.filter { it.tagId in available && it.tagId !in rejected })
            rejected.filter { it in available }.forEach { rejectTag(OrganizationRejectionEntity(note.id, it, note.content)) }
        }
        // Restoring a deleted note restores only links whose other endpoint still exists.
        // Ordinary content/tag saves must never overwrite independently edited links.
        if (restoring) linkedNoteIds.filter { it != note.id }.forEach { target ->
            if (getById(target) != null) connect(note.id, target)
        }
    }
}
