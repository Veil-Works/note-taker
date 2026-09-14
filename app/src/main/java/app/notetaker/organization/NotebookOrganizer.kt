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
        val tags = db.noteDao().observeTags().first().map { Tag(it.id, it.name, it.createdAt, it.typeId, it.typeName) }
        val aliases = db.noteDao().observeAliases().first().map { TagAlias(it.id, it.tagId, it.text) }
        val rejected = dao.rejections()
        val oldTopics = dao.topics()
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
            val exact = exactConceptMatches(row.note.content, tags, aliases)
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
            }.sortedWith(compareByDescending<AutomaticTagEntity> { it.score }.thenBy { it.tagId }).take(5)
        }
        val groups = discoverTopics(nonempty, vectors, oldTopics, subjects)
        return dao.apply(state.generation, newEmbeddings, automatic, links, groups.map { it.first }, groups.flatMap { (topic, ids) -> ids.map { TopicNoteEntity(topic.id, it) } })
    }

    private suspend fun discoverTopics(notes: List<NoteWithTags>, vectors: Map<String, FloatArray>, old: List<TopicWithNotes>, subjects: Map<String, String>): List<Pair<TopicEntity, Set<String>>> {
        val available = notes.associateBy { it.note.id }
        val groups = mutableListOf<Pair<TopicEntity, MutableSet<String>>>()
        val assigned = mutableSetOf<String>()
        // User names and merges preserve their existing membership. Ordinary groups remain provisional.
        old.filter { it.topic.membershipLocked || it.topic.customized }.forEach { topic ->
            val members = topic.members.map { it.noteId }.filter { it in available }.toMutableSet()
            groups += topic.topic to members
            assigned += members
        }
        subjects.entries.groupBy({ it.value }, { it.key }).toSortedMap().forEach { (name, ids) ->
            val members = ids.filter { it !in assigned }.toMutableSet()
            if (members.size >= 2) {
                var topicId = "subject-$name"
                while (groups.any { it.first.id == topicId }) topicId += "-new"
                groups += TopicEntity(topicId, name.replaceFirstChar { it.titlecase(java.util.Locale.ROOT) }) to members
                assigned += members
            }
        }
        notes.filter { it.note.id !in assigned }.forEach { row ->
            val id = row.note.id
            val best = groups.filter { (topic, members) -> !topic.membershipLocked && fitsTopic(members.map { similarity(vectors.getValue(id), vectors.getValue(it)) }) }
                .maxByOrNull { (_, members) -> members.map { similarity(vectors.getValue(id), vectors.getValue(it)) }.average() }
            if (best != null) best.second += id
            else {
                var topicId = "topic-$id"
                while (groups.any { it.first.id == topicId }) topicId += "-new"
                groups += TopicEntity(topicId, "") to mutableSetOf(id)
            }
        }
        return groups.filter { it.first.customized || it.second.size >= 2 }.map { (topic, members) ->
            coroutineContext.ensureActive()
            val contents = members.map { available.getValue(it).note.content }
            val named = if (topic.customized) topic else topic.copy(name =
                sharedCategory(contents) ?: if (topic.id.startsWith("subject-")) topic.name else topicName(contents))
            named to members.toSet()
        }
    }
    companion object {
        const val RULES_VERSION = 7
        const val TOPIC_THRESHOLD = 0.42
        const val LEARNED_THRESHOLD = 0.55
    }
}
