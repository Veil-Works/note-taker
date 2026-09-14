package app.notetaker.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizationDao {
    @Query("SELECT * FROM semantic_links") fun observeGraphLinks(): Flow<List<SemanticLinkEntity>>
    @Query("SELECT * FROM organization_state WHERE id = 1") fun observeState(): Flow<OrganizationStateEntity?>
    @Query("SELECT * FROM organization_state WHERE id = 1") suspend fun state(): OrganizationStateEntity?
    @Query("UPDATE organization_state SET generation = generation + 1, error = NULL WHERE id = 1") suspend fun request()
    @Query("UPDATE organization_state SET error = :message WHERE id = 1") suspend fun failure(message: String)
    @Query("UPDATE organization_state SET completed = :generation, error = NULL WHERE id = 1") suspend fun complete(generation: Long)
    @Query("SELECT * FROM note_embeddings") suspend fun embeddings(): List<NoteEmbeddingEntity>
    @Upsert suspend fun saveEmbeddings(rows: List<NoteEmbeddingEntity>)
    @Query("SELECT * FROM organization_rejections") suspend fun rejections(): List<OrganizationRejectionEntity>
    @Query("SELECT * FROM semantic_links WHERE noteId = :id ORDER BY score DESC, targetId") fun related(id: String): Flow<List<SemanticLinkEntity>>
    @Transaction @Query("SELECT * FROM topics ORDER BY name, id") fun observeTopics(): Flow<List<TopicWithNotes>>
    @Transaction @Query("SELECT * FROM topics ORDER BY id") suspend fun topics(): List<TopicWithNotes>
    @Query("DELETE FROM automatic_tags") suspend fun clearAutomaticTags()
    @Insert suspend fun insertAutomaticTags(rows: List<AutomaticTagEntity>)
    @Query("DELETE FROM semantic_links") suspend fun clearSemanticLinks()
    @Insert suspend fun insertSemanticLinks(rows: List<SemanticLinkEntity>)
    @Query("DELETE FROM topics") suspend fun clearTopics()
    @Insert suspend fun insertTopics(rows: List<TopicEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertTopicNotes(rows: List<TopicNoteEntity>)

    @Transaction
    suspend fun apply(generation: Long, embeddings: List<NoteEmbeddingEntity>, tags: List<AutomaticTagEntity>, links: List<SemanticLinkEntity>, topics: List<TopicEntity>, members: List<TopicNoteEntity>): Boolean {
        if (state()?.generation != generation) return false
        saveEmbeddings(embeddings)
        clearAutomaticTags(); insertAutomaticTags(tags)
        clearSemanticLinks(); insertSemanticLinks(links)
        clearTopics(); insertTopics(topics); insertTopicNotes(members)
        complete(generation)
        return true
    }

    @Query("UPDATE topics SET name = :name, customized = 1 WHERE id = :id") suspend fun updateTopicName(id: String, name: String): Int
    @Query("UPDATE topics SET customized = 1, membershipLocked = 1 WHERE id = :id") suspend fun customizeTopic(id: String)
    @Query("DELETE FROM topics WHERE id = :id") suspend fun deleteTopic(id: String)
    @Transaction suspend fun renameTopic(id: String, name: String) {
        check(updateTopicName(id, name) == 1) { "This topic no longer exists." }
        request()
    }
    @Transaction suspend fun mergeTopics(target: String, source: String) {
        require(target != source)
        val current = topics()
        check(current.any { it.topic.id == target }) { "This topic no longer exists." }
        val from = checkNotNull(current.find { it.topic.id == source }) { "This topic no longer exists." }
        insertTopicNotes(from.members.map { it.copy(topicId = target) })
        customizeTopic(target)
        deleteTopic(source)
        request()
    }
}
