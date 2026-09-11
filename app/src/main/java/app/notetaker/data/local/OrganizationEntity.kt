package app.notetaker.data.local

import androidx.room.*

@Entity(tableName = "organization_state")
data class OrganizationStateEntity(@PrimaryKey val id: Int = 1, val generation: Long = 0, val completed: Long = -1, val error: String? = null)

@Entity(tableName = "note_embeddings", foreignKeys = [ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE)])
data class NoteEmbeddingEntity(@PrimaryKey val noteId: String, val content: String, val model: String, val vector: ByteArray)

@Entity(tableName = "automatic_tags", primaryKeys = ["noteId", "tagId"], indices = [Index("tagId")], foreignKeys = [
    ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)])
data class AutomaticTagEntity(val noteId: String, val tagId: String, val reason: String, val score: Double)

@Entity(tableName = "organization_rejections", primaryKeys = ["noteId", "tagId"], indices = [Index("tagId")], foreignKeys = [
    ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)])
data class OrganizationRejectionEntity(val noteId: String, val tagId: String, val content: String)

@Entity(tableName = "semantic_links", primaryKeys = ["noteId", "targetId"], indices = [Index("targetId")], foreignKeys = [
    ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["targetId"], onDelete = ForeignKey.CASCADE)])
data class SemanticLinkEntity(val noteId: String, val targetId: String, val score: Double)

@Entity(tableName = "topics")
data class TopicEntity(@PrimaryKey val id: String, val name: String, val customized: Boolean = false, val membershipLocked: Boolean = false)

@Entity(tableName = "topic_notes", primaryKeys = ["topicId", "noteId"], indices = [Index("noteId")], foreignKeys = [
    ForeignKey(entity = TopicEntity::class, parentColumns = ["id"], childColumns = ["topicId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE)])
data class TopicNoteEntity(val topicId: String, val noteId: String)

data class TopicWithNotes(@Embedded val topic: TopicEntity, @Relation(parentColumn = "id", entityColumn = "topicId") val members: List<TopicNoteEntity>)
