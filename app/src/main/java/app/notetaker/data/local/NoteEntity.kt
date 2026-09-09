package app.notetaker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [Index(value = ["updatedAt"])],
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
)

