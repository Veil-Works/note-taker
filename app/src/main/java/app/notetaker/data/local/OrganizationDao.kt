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
    @Query("DELETE FROM automatic_tags") suspend fun clearAutomaticTags()
    @Insert suspend fun insertAutomaticTags(rows: List<AutomaticTagEntity>)
    @Query("DELETE FROM semantic_links") suspend fun clearSemanticLinks()
    @Insert suspend fun insertSemanticLinks(rows: List<SemanticLinkEntity>)
    @Query("SELECT * FROM topic_discovery_keys") suspend fun discoveryKeys(): List<TopicDiscoveryKeyEntity>
    @Upsert suspend fun saveDiscoveryKeys(rows: List<TopicDiscoveryKeyEntity>)
    @Insert suspend fun insertDiscoveredTags(rows: List<TagEntity>)
    @Query("DELETE FROM topic_discovery_keys WHERE tagId IN (SELECT id FROM tags WHERE provisional = 1 AND id NOT IN (SELECT tagId FROM automatic_tags) AND id NOT IN (SELECT tagId FROM note_tags))")
    suspend fun forgetUnusedKeys()
    @Query("INSERT OR IGNORE INTO topic_discovery_keys (`key`, tagId) SELECT 'merged-id:' || id, NULL FROM tags WHERE provisional = 1 AND id NOT IN (SELECT tagId FROM automatic_tags) AND id NOT IN (SELECT tagId FROM note_tags)")
    suspend fun recordExpiredTopics()
    @Query("DELETE FROM tags WHERE provisional = 1 AND id NOT IN (SELECT tagId FROM automatic_tags) AND id NOT IN (SELECT tagId FROM note_tags)")
    suspend fun pruneUnusedTopics()

    @Query("UPDATE topic_discovery_keys SET tagId = :target WHERE tagId = :source")
    suspend fun redirectKeys(source: String, target: String)
    @Query("DELETE FROM tags WHERE id = :id AND provisional = 1") suspend fun deleteProvisional(id: String)
    @Query("UPDATE tags SET name = :name, normalizedName = :normalized WHERE id = :id AND provisional = 1")
    suspend fun renameProvisional(id: String, name: String, normalized: String)

    @Transaction
    suspend fun apply(generation: Long, embeddings: List<NoteEmbeddingEntity>, tags: List<AutomaticTagEntity>, links: List<SemanticLinkEntity>, discovered: List<TagEntity>, keys: List<TopicDiscoveryKeyEntity>, renamed: List<TagEntity> = emptyList(), consolidated: Map<String, String> = emptyMap()): Boolean {
        if (state()?.generation != generation) return false
        saveEmbeddings(embeddings)
        consolidated.forEach { (source, target) ->
            redirectKeys(source, target)
            saveDiscoveryKeys(listOf(TopicDiscoveryKeyEntity("merged-id:$source", target)))
            deleteProvisional(source)
        }
        // Free all old names first, including when two provisional topics exchange names.
        renamed.forEach {
            val temporary = "renaming-${java.util.UUID.randomUUID()}"
            renameProvisional(it.id, temporary, temporary)
        }
        renamed.forEach { renameProvisional(it.id, it.name, it.normalizedName) }
        insertDiscoveredTags(discovered)
        saveDiscoveryKeys(keys)
        clearAutomaticTags(); insertAutomaticTags(tags)
        clearSemanticLinks(); insertSemanticLinks(links)
        forgetUnusedKeys()
        recordExpiredTopics()
        pruneUnusedTopics()
        // Topic creation/pruning fires the input triggers within this same transaction.
        // Those changes are already part of this evaluation, so consume their generation too.
        complete(checkNotNull(state()).generation)
        return true
    }
}
