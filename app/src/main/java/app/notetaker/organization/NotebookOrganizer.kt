package app.notetaker.organization

import app.notetaker.data.local.*
import app.notetaker.model.Tag
import app.notetaker.model.TagAlias
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlin.coroutines.coroutineContext

/** All inference happens outside the transaction; a generation guard rejects stale results. */
class NotebookOrganizer(
    private val db: NoteDatabase,
    private val embedding: TextEmbedding,
    private val connectionsOnly: Boolean = false,
    private val discover: (List<DiscoveryNote>) -> List<TopicCandidate> = { TopicDiscovery(embedding).discover(it) },
) {
    suspend fun runOnce(): Boolean {
        val dao = db.organizationDao()
        val state = dao.state() ?: return true
        if (state.completed == state.generation) return true
        val notes = db.noteDao().observeAll().first().sortedBy { it.note.id }
        val tags = db.noteDao().observeTags().first().map { Tag(it.id, it.name, it.createdAt, it.typeId, it.typeName, it.provisional) }
        val aliases = db.noteDao().observeAliases().first().map { TagAlias(it.id, it.tagId, it.text) }
        val rejected = dao.rejections()
        val discoveryKeys = dao.discoveryKeys()
        val cached = dao.embeddings().associateBy { it.noteId }
        val newEmbeddings = mutableListOf<NoteEmbeddingEntity>()
        val vectors = notes.associate { row ->
            coroutineContext.ensureActive()
            val old = cached[row.note.id]
            val vector = if (old?.content == row.note.content && old.model == embedding.version) old.vector.vector()
                else embedding.embed(row.note.content).also { newEmbeddings += NoteEmbeddingEntity(row.note.id, row.note.content, embedding.version, it.bytes()) }
            row.note.id to vector
        }
        val nonempty = notes.filter { it.note.content.isNotBlank() }
        val subjects = if (connectionsOnly) emptyMap() else recurringSubjects(nonempty.associate { it.note.id to it.note.content }, vectors)
        val links = buildList {
            nonempty.forEachIndexed { index, row ->
                coroutineContext.ensureActive()
                for (otherIndex in index + 1 until nonempty.size) {
                    val other = nonempty[otherIndex]
                    val score = relevantConnectionScore(row.note.id, other.note.id, vectors, subjects) ?: continue
                    // An accepted relationship always exists in both directions, regardless of rank.
                    add(SemanticLinkEntity(row.note.id, other.note.id, score))
                    add(SemanticLinkEntity(other.note.id, row.note.id, score))
                }
            }
        }
        if (connectionsOnly) {
            // Replace derived organization without inventing group identities. Manual
            // topics, links, aliases and rejections remain owned by the user.
            return dao.apply(state.generation, newEmbeddings, emptyList(), links, emptyList(), emptyList())
        }
        // Only user-attached tags are positive examples. Never train on our own guesses.
        val examples = tags.associate { tag -> tag.id to notes.filter { row -> row.tags.any { it.id == tag.id } } }
        val rejectionVectors = rejected.associateWith { rejection ->
            notes.find { it.note.id == rejection.noteId && it.note.content == rejection.content }?.let { vectors.getValue(it.note.id) }
                ?: embedding.embed(rejection.content)
        }
        val dismissals = nonempty.associate { it.note.id to db.noteDao().observeDismissals(it.note.id).first().toSet() }
        val automatic = nonempty.flatMap { row ->
            coroutineContext.ensureActive()
            val exact = exactConceptMatches(row.note.content, tags.filterNot { it.provisional }, aliases)
            tags.mapNotNull { tag ->
                if (row.tags.any { it.id == tag.id } || rejected.any { it.noteId == row.note.id && it.tagId == tag.id } || tag.id in dismissals.getValue(row.note.id)) return@mapNotNull null
                val positives = examples.getValue(tag.id).filter { it.note.id != row.note.id }.map { similarity(vectors.getValue(row.note.id), vectors.getValue(it.note.id)) }.sortedDescending()
                val positive = positives.firstOrNull() ?: 0.0
                val negative = rejectionVectors.filterKeys { it.tagId == tag.id }.values.maxOfOrNull { similarity(vectors.getValue(row.note.id), it) } ?: 0.0
                val term = exact[tag.id]
                val explicit = term != null && (term.contains(' ') || row.note.content.trim().equals(term, true) || examples.getValue(tag.id).isNotEmpty())
                val learned = (positives.count { it >= LEARNED_THRESHOLD } >= 2 || positive >= 0.82) && positive > negative + 0.08
                when {
                    explicit && negative < 0.80 -> AutomaticTagEntity(row.note.id, tag.id, "Matches “$term”", 1.0)
                    learned -> AutomaticTagEntity(row.note.id, tag.id, "Similar to notes you tagged ${tag.name}", positive)
                    else -> null
                }
            }.sortedWith(compareByDescending<AutomaticTagEntity> { it.score }.thenBy { it.tagId })
        }
        val keys = discoveryKeys.associateBy { it.key }
        val discoveries = distinctDiscoveryKeys(discover(nonempty.map { DiscoveryNote(it.note.id, it.note.content, vectors.getValue(it.note.id)) })
            .filterNot { keys[it.key]?.let { key -> key.tagId == null } == true }, keys.keys)
        val previousMembers = tags.associate { tag -> tag.id to notes.filter { row ->
            (row.tags.any { it.id == tag.id } || row.automaticTags.any { it.id == tag.id }) &&
                rejected.none { it.noteId == row.note.id && it.tagId == tag.id } &&
                (cached[row.note.id]?.takeIf { it.model == embedding.version }?.let {
                    similarity(vectors.getValue(row.note.id), it.vector.vector()) >= .55
                } ?: true)
        }.map { it.note.id }.toSet() }
        val lineage = discoveryKeys.map { key ->
            val members = key.memberIds?.let { json ->
                val array = org.json.JSONArray(json)
                (0 until array.length()).map { array.getString(it) }.toSet()
            } ?: emptySet()
            DiscoveryLineage(key.key, key.tagId, key.subject, members, key.mergeKind, key.branchKey)
        }
        val subjectVectors = mutableMapOf<String, FloatArray>()
        fun subjectVector(text: String) = subjectVectors.getOrPut(text) { embedding.embed(text) }
        // If every member is explicitly excluded or strongly matches a rejection,
        // rediscovering it under a fresh ID would evade the user's correction.
        // This is negative evidence only; it never authorizes a positive assignment.
        val groups = discoveries.filterNot { group ->
            tags.any { tag ->
                lineage.any { it.topicId == tag.id &&
                    ((it.subject ?: tag.name).equals(group.name, true) || similarity(subjectVector(group.name), subjectVector(it.subject ?: tag.name)) >= .55) } &&
                    group.members.all { id -> rejected.any { it.noteId == id && it.tagId == tag.id } ||
                        rejectionVectors.filterKeys { it.tagId == tag.id }.values.any { similarity(vectors.getValue(id), it) >= .80 } }
            }
        }
        val reconciler = TopicReconciliation(
            subjectSimilarity = { a, b -> if (a.equals(b, true)) 1.0 else similarity(subjectVector(a), subjectVector(b)) },
            memberSimilarity = { a, b -> similarity(vectors.getValue(a), vectors.getValue(b)) },
        )
        val identities = reconciler.reconcile(groups, tags.map { tag ->
            // Current exact-name/manual-example matches are independent evidence,
            // unlike memberships produced by another discovered group in this pass.
            val independent = automatic.filter { it.tagId == tag.id }.map { it.noteId }
            TopicIdentity(tag.id, tag.name, previousMembers.getValue(tag.id) + independent,
                rejected.filter { it.tagId == tag.id }.map { it.noteId }.toSet())
        }, lineage)
        val independentAssignments = automatic.map { it.noteId to it.tagId }.toSet()
        val memberships = automatic.associateBy { it.noteId to it.tagId }.toMutableMap()
        val bindings = mutableListOf<TopicDiscoveryKeyEntity>()
        val assigned = groups.sortedBy { it.key }.mapNotNull { group ->
            coroutineContext.ensureActive()
            if (keys[group.key]?.let { it.tagId == null } == true) return@mapNotNull null
            val match = identities[group.key]
            // A group already covered by independent user-topic assignments needs no
            // extra provisional identity, even when two user topics are equally valid.
            if (match == null && tags.any { tag -> !tag.provisional && group.members.all { id ->
                notes.first { it.note.id == id }.tags.any { it.id == tag.id } || (id to tag.id) in independentAssignments
            } }) return@mapNotNull null
            val id = match?.topicId ?: java.util.UUID.nameUUIDFromBytes(("topic:${state.generation}:" + group.key).toByteArray(Charsets.UTF_8)).toString()
            bindings += TopicDiscoveryKeyEntity(group.key, id, group.name,
                org.json.JSONArray(group.members.sorted()).toString(), match?.branchKey ?: id, match?.mergeKind ?: 0)
            group.members.forEach { noteId ->
                val row = nonempty.first { it.note.id == noteId }
                val negative = rejectionVectors.filterKeys { it.tagId == id }.values.maxOfOrNull { similarity(vectors.getValue(noteId), it) } ?: 0.0
                if (row.tags.none { it.id == id } && rejected.none { it.noteId == noteId && it.tagId == id } &&
                    id !in dismissals.getValue(noteId) && negative < .80) {
                    memberships.putIfAbsent(noteId to id, AutomaticTagEntity(noteId, id, group.reasons.getValue(noteId), group.scores.getValue(noteId)))
                }
            }
            group to id
        }
        val requestedNames = assigned.filter { (_, id) -> tags.none { it.id == id && !it.provisional } }
            .associate { (group, id) -> id to group.name }
        val survivingIds = memberships.values.map { it.tagId }.toSet() + notes.flatMap { it.tags }.map { it.id }
        val finalNames = allocateTopicNames(requestedNames, tags.filter {
            it.id !in requestedNames && (!it.provisional || it.id in survivingIds)
        }.map { it.name })
        val discovered = mutableListOf<TagEntity>()
        val renamed = mutableListOf<TagEntity>()
        finalNames.forEach { (id, name) ->
            val old = tags.find { it.id == id }
            val row = TagEntity(id, name, name.lowercase(java.util.Locale.ROOT), old?.createdAt ?: System.currentTimeMillis(), provisional = true)
            if (old == null) discovered += row else if (old.name != name) renamed += row
        }
        return dao.apply(state.generation, newEmbeddings, memberships.values.toList(), links, discovered, bindings, renamed)
    }

    companion object {
        const val RULES_VERSION = 13
        const val TOPIC_THRESHOLD = 0.42
        const val LEARNED_THRESHOLD = 0.55
    }
}
