package app.notetaker.organization

import app.notetaker.data.local.*
import app.notetaker.model.Tag
import app.notetaker.model.TagAlias
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlin.coroutines.coroutineContext

/** All inference happens outside the transaction; a generation guard rejects stale results. */
class NotebookOrganizer(private val db: NoteDatabase, private val embedding: TextEmbedding) {
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
        val subjects = recurringSubjects(nonempty.associate { it.note.id to it.note.content }, vectors)
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
        val groups = TopicDiscovery(embedding).discover(nonempty.map { DiscoveryNote(it.note.id, it.note.content, vectors.getValue(it.note.id)) })
        val discovered = mutableListOf<TagEntity>()
        val bindings = mutableListOf<TopicDiscoveryKeyEntity>()
        val renamed = mutableListOf<TagEntity>()
        val consolidated = mutableMapOf<String, String>()
        val claimed = mutableSetOf<String>()
        val memberships = automatic.associateBy { it.noteId to it.tagId }.toMutableMap()
        val knownNames = tags.map { it.name.lowercase(java.util.Locale.ROOT) }.toMutableSet()
        val keys = discoveryKeys.associateBy { it.key }
        val previousMembers = tags.associate { tag -> tag.id to notes.filter { row ->
            row.tags.any { it.id == tag.id } || row.automaticTags.any { it.id == tag.id } || row.rejections.any { it.tagId == tag.id }
        }.map { it.note.id }.toSet() }
        groups.forEach { group ->
            coroutineContext.ensureActive()
            val binding = keys[group.key]
            if (binding != null && binding.tagId == null) return@forEach // Explicitly deleted.
            val candidates = tags.filter { it.id !in consolidated && (!it.provisional || it.id !in claimed) }.mapNotNull { tag ->
                // Membership overlap identifies a group, but changed text must still agree
                // with its previous meaning. A matching name by itself is never a merge.
                val overlap = group.members.count { id ->
                    memberships.containsKey(id to tag.id) ||
                        (id in previousMembers.getValue(tag.id) &&
                            (cached[id]?.let { similarity(vectors.getValue(id), it.vector.vector()) >= .55 } ?: true))
                }
                val priorSize = previousMembers.getValue(tag.id).size.coerceAtLeast(1)
                if (overlap > 0 && (overlap.toDouble() / group.members.size >= .5 || overlap.toDouble() / priorSize >= .5)) tag to overlap else null
            }.sortedWith(compareByDescending<Pair<Tag, Int>> { it.second }.thenBy { it.first.provisional }.thenBy { it.first.id })
            val boundId = binding?.tagId?.let { consolidated[it] ?: it }
            val existing = boundId?.takeIf { id -> tags.any { it.id == id && (!it.provisional || id !in claimed) } }
                ?: candidates.firstOrNull()?.first?.id
            val id = existing ?: java.util.UUID.randomUUID().toString()
            val old = tags.find { it.id == id }
            if (old != null) {
                // Only untouched inferred topics can be consolidated automatically.
                // User-confirmed identities, exclusions, aliases and types remain independent.
                tags.filter { it.provisional && it.id != id && it.id !in claimed && it.id !in consolidated }.forEach { other ->
                    val members = previousMembers.getValue(other.id)
                    val unchanged = members.count { member -> member in group.members &&
                        (cached[member]?.let { similarity(vectors.getValue(member), it.vector.vector()) >= .55 } ?: true) }
                    if (members.size >= 2 && unchanged.toDouble() / members.size >= .75) {
                        consolidated[other.id] = id
                        knownNames -= other.name.lowercase(java.util.Locale.ROOT)
                    }
                }
            }
            if (old == null || old.provisional) {
                if (old != null) knownNames -= old.name.lowercase(java.util.Locale.ROOT)
                val base = group.name.trim().ifBlank { "Untitled topic" }.take(40)
                var name = base
                var suffix = 2
                while (name.lowercase(java.util.Locale.ROOT) in knownNames) {
                    val ending = " ($suffix)"
                    name = base.take(40 - ending.length) + ending
                    suffix++
                }
                knownNames += name.lowercase(java.util.Locale.ROOT)
                val row = TagEntity(id, name, name.lowercase(java.util.Locale.ROOT), old?.createdAt ?: System.currentTimeMillis(), provisional = true)
                if (old == null) discovered += row else if (old.name != name) renamed += row
            }
            claimed += id
            bindings += TopicDiscoveryKeyEntity(group.key, id)
            group.members.forEach { noteId ->
                val row = nonempty.first { it.note.id == noteId }
                val negative = rejectionVectors.filterKeys { it.tagId == id }.values.maxOfOrNull { similarity(vectors.getValue(noteId), it) } ?: 0.0
                if (row.tags.none { it.id == id } && rejected.none { it.noteId == noteId && it.tagId == id } &&
                    id !in dismissals.getValue(noteId) && negative < .80) {
                    memberships.putIfAbsent(noteId to id, AutomaticTagEntity(noteId, id, group.reasons.getValue(noteId), group.scores.getValue(noteId)))
                }
            }
        }
        return dao.apply(state.generation, newEmbeddings, memberships.values.toList(), links, discovered, bindings, renamed, consolidated)
    }

    companion object {
        const val RULES_VERSION = 11
        const val TOPIC_THRESHOLD = 0.42
        const val LEARNED_THRESHOLD = 0.55
    }
}
