package app.notetaker.data.local

import androidx.room.*

@Entity(tableName = "tag_aliases", indices = [Index(value = ["tagId", "normalizedText"], unique = true)],
    foreignKeys = [ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)])
data class TagAliasEntity(@PrimaryKey val id: String, val tagId: String, val text: String, val normalizedText: String)

@Entity(tableName = "tag_dismissals", primaryKeys = ["noteId", "tagId"], indices = [Index("tagId")],
    foreignKeys = [
        ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)])
data class TagDismissalEntity(val noteId: String, val tagId: String)
