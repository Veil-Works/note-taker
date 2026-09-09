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

    @Transaction
    suspend fun saveWithTags(note: NoteEntity, tags: List<TagEntity>) {
        upsert(note)
        tags.forEach { insertTag(it) }
        clearLinks(note.id)
        insertLinks(tags.map { NoteTagEntity(note.id, it.id) })
    }
}
