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

    @Query("SELECT * FROM tags ORDER BY normalizedName")
    fun observeTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity)

    @Query("SELECT * FROM tags WHERE normalizedName = :name LIMIT 1")
    suspend fun findTag(name: String): TagEntity?

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
    suspend fun saveWithTags(note: NoteEntity, tags: List<TagEntity>, linkedNoteIds: Set<String>) {
        val previous = getById(note.id)
        val restoring = previous == null
        if (previous != null && previous.note.content != note.content) clearDismissals(note.id)
        upsert(note)
        tags.forEach { insertTag(it) }
        clearLinks(note.id)
        insertLinks(tags.map { NoteTagEntity(note.id, it.id) })
        // Restoring a deleted note restores only links whose other endpoint still exists.
        // Ordinary content/tag saves must never overwrite independently edited links.
        if (restoring) linkedNoteIds.filter { it != note.id }.forEach { target ->
            if (getById(target) != null) connect(note.id, target)
        }
    }
}
