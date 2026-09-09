package app.notetaker.data.local

import androidx.room.*

@Entity(tableName = "tags", indices = [Index(value = ["normalizedName"], unique = true)])
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val normalizedName: String,
    val createdAt: Long,
)

@Entity(
    tableName = "note_tags",
    primaryKeys = ["noteId", "tagId"],
    indices = [Index("tagId")],
    foreignKeys = [
        ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class NoteTagEntity(val noteId: String, val tagId: String)

data class NoteWithTags(
    @Embedded val note: NoteEntity,
    @Relation(
        parentColumn = "id", entityColumn = "id",
        associateBy = Junction(NoteTagEntity::class, parentColumn = "noteId", entityColumn = "tagId"),
    ) val tags: List<TagEntity>,
)
