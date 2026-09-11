package app.notetaker.data.local

import androidx.room.*

@Entity(tableName = "entity_types", indices = [Index(value = ["normalizedName"], unique = true)])
data class EntityTypeEntity(@PrimaryKey val id: String, val name: String, val normalizedName: String)

@DatabaseView("SELECT tags.*, entity_types.name AS typeName FROM tags LEFT JOIN entity_types ON tags.typeId = entity_types.id", viewName = "tag_details")
data class TagDetails(val id: String, val name: String, val normalizedName: String, val createdAt: Long, val typeId: String?, val typeName: String?)

@Entity(tableName = "tags", indices = [Index(value = ["normalizedName"], unique = true), Index("typeId")],
    foreignKeys = [ForeignKey(entity = EntityTypeEntity::class, parentColumns = ["id"], childColumns = ["typeId"], onDelete = ForeignKey.SET_NULL)])
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val normalizedName: String,
    val createdAt: Long,
    val typeId: String? = null,
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
    ) val tags: List<TagDetails>,
    @Relation(parentColumn = "id", entityColumn = "id", associateBy = Junction(AutomaticTagEntity::class, parentColumn = "noteId", entityColumn = "tagId"))
    val automaticTags: List<TagDetails>,
    @Relation(parentColumn = "id", entityColumn = "noteId") val automaticReasons: List<AutomaticTagEntity>,
    @Relation(parentColumn = "id", entityColumn = "noteId") val rejections: List<OrganizationRejectionEntity>,
    @Relation(parentColumn = "id", entityColumn = "firstNoteId")
    val outgoingLinks: List<NoteLinkEntity>,
    @Relation(parentColumn = "id", entityColumn = "secondNoteId")
    val incomingLinks: List<NoteLinkEntity>,
)
