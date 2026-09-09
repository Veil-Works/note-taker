package app.notetaker.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** One undirected connection, stored with lexicographically ordered endpoints. */
@Entity(
    tableName = "note_links",
    primaryKeys = ["firstNoteId", "secondNoteId"],
    indices = [Index("secondNoteId")],
    foreignKeys = [
        ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["firstNoteId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["secondNoteId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class NoteLinkEntity(val firstNoteId: String, val secondNoteId: String)
