package app.notetaker.organization

import java.util.Locale

/** Identity evidence is history, never a positive training example for membership inference. */
data class TopicIdentity(val id: String, val name: String, val members: Set<String>, val excluded: Set<String> = emptySet())
data class DiscoveryLineage(
    val key: String, val topicId: String?, val subject: String?, val members: Set<String>, val mergeKind: Int = 0,
    val branchKey: String? = null,
)
data class IdentityMatch(val topicId: String, val mergeKind: Int, val branchKey: String? = null)

/** Pure reconciliation: all decisions use the same immutable pre-run snapshot. */
class TopicReconciliation(
    private val subjectSimilarity: (String, String) -> Double,
    private val memberSimilarity: (String, String) -> Double,
) {
    private data class Slot(val topic: TopicIdentity, val history: List<DiscoveryLineage>, val mergeKind: Int)

    fun reconcile(
        discoveries: List<TopicCandidate>, topics: List<TopicIdentity>, lineage: List<DiscoveryLineage>,
    ): Map<String, IdentityMatch> {
        require(discoveries.map { it.key }.distinct().size == discoveries.size) { "Discovery keys must identify distinct subjects" }
        val keys = lineage.associateBy { it.key }
        val available = topics.associateBy { it.id }
        val groups = discoveries.filterNot { keys[it.key]?.let { key -> key.topicId == null } ?: false }.sortedBy { it.key }
        val result = mutableMapOf<String, IdentityMatch>()
        // Ambiguous pre-v8 merges retain only their exact historical groups. They cannot
        // grant a new discovery permission to bypass one-to-one reconciliation.
        groups.forEach { group ->
            keys[group.key]?.takeIf { it.mergeKind == 2 && it.topicId in available }?.let {
                result[group.key] = IdentityMatch(it.topicId!!, 2, it.branchKey ?: it.key)
            }
        }
        val legacyClaimed = result.values.map { it.topicId }.toSet()
        val slots = topics.sortedBy { it.id }.filterNot { it.id in legacyClaimed }.flatMap { topic ->
            val history = lineage.filter { it.topicId == topic.id && !it.key.startsWith("merged-id:") }.sortedBy { it.key }
            val explicit = history.filter { it.mergeKind == 1 && it.subject != null }
            if (explicit.isEmpty()) listOf(Slot(topic, history.filter { it.mergeKind == 0 }, 0))
            else {
                // Several snapshots of the same merged branch must not create extra slots.
                explicit.groupBy { it.branchKey ?: it.subject!!.lowercase(Locale.ROOT) }.toSortedMap().values.map { Slot(topic, it, 1) }
            }
        }
        val pending = groups.filterNot { it.key in result }
        if (pending.isEmpty() || slots.isEmpty()) return result
        val weights = pending.map { group -> slots.map { score(group, it) }.toDoubleArray() }.toTypedArray()
        // A near tie between different identities is unresolved. Two branches of an
        // explicitly merged topic are not competing identities.
        weights.forEach { row ->
            val ranked = slots.indices.groupBy { slots[it].topic.id }.map { (_, indices) -> indices.maxOf { row[it] } }.sortedDescending()
            if (ranked.size > 1 && ranked[0] > 0 && ranked[0] - ranked[1] < .08) row.fill(0.0)
        }
        maximumAssignment(weights).forEachIndexed { row, column ->
            if (column in slots.indices && weights[row][column] > 0) {
                val slot = slots[column]
                result[pending[row].key] = IdentityMatch(slot.topic.id, slot.mergeKind,
                    if (slot.mergeKind == 1) slot.history.first().branchKey ?: slot.history.first().key else slot.topic.id)
            }
        }
        return result
    }

    private fun score(group: TopicCandidate, slot: Slot): Double {
        val topic = slot.topic
        val exact = slot.history.firstOrNull { it.key == group.key }
        // A recorded, compatible subject plus an unchanged content fingerprint is
        // strong lineage even when all its notes were subsequently excluded.
        if (exact?.subject != null && subjectSimilarity(group.name, exact.subject) >= .55) return 3.0
        val histories = slot.history.filter { it.subject != null && it.members.isNotEmpty() }
        val evidence = if (histories.isEmpty()) listOf(DiscoveryLineage("", topic.id, topic.name, topic.members)) else histories
        return evidence.maxOfOrNull { old ->
            val previous = old.members.intersect(topic.members) - topic.excluded
            val current = group.members - topic.excluded
            val overlap = current.intersect(previous)
            if (overlap.isEmpty()) return@maxOfOrNull 0.0
            val forward = overlap.size.toDouble() / current.size.coerceAtLeast(1)
            val backward = overlap.size.toDouble() / previous.size.coerceAtLeast(1)
            if (maxOf(forward, backward) < .5) return@maxOfOrNull 0.0
            val subject = subjectSimilarity(group.name, old.subject ?: topic.name)
            // Compare new members with independent old notes, excluding the common
            // bridge whenever both sides have independent evidence.
            val newOnly = current - overlap
            val oldOnly = previous - overlap
            val left = newOnly.ifEmpty { current }
            val right = oldOnly.ifEmpty { previous }
            fun support(a: Set<String>, b: Set<String>): Double = a.mapNotNull { id ->
                b.filter { it != id }.maxOfOrNull { memberSimilarity(id, it) }
            }.let { if (it.isEmpty()) 0.0 else it.average() }
            val independent = minOf(support(left, right), support(right, left))
            if (overlap.size == 1 && (subject < .65 || independent < .65)) return@maxOfOrNull 0.0
            if (subject < .55 && independent < .65) return@maxOfOrNull 0.0
            // A label alone cannot authorize a broad union with unrelated members.
            if (newOnly.isNotEmpty() && oldOnly.isNotEmpty() && independent < .35) return@maxOfOrNull 0.0
            .45 * backward + .25 * forward + .20 * independent.coerceIn(0.0, 1.0) + .10 * subject.coerceIn(0.0, 1.0)
        } ?: 0.0
    }
}

/** Two named subjects can have exactly the same notes and thus the same content hash. */
internal fun distinctDiscoveryKeys(groups: List<TopicCandidate>, knownKeys: Set<String>): List<TopicCandidate> =
    groups.groupBy { it.key }.flatMap { (key, sameContent) ->
        sameContent.distinctBy { it.name.lowercase(Locale.ROOT) }.map { group ->
            val subjectKey = "$key:subject:" + java.util.UUID.nameUUIDFromBytes(group.name.lowercase(Locale.ROOT).toByteArray(Charsets.UTF_8))
            if (sameContent.size > 1 || subjectKey in knownKeys) group.copy(key = subjectKey) else group
        }
    }

/** Rectangular Hungarian assignment with one zero-weight unmatched option per row. */
internal fun maximumAssignment(weights: Array<DoubleArray>): IntArray {
    val n = weights.size
    if (n == 0) return IntArray(0)
    val realColumns = weights[0].size
    val m = realColumns + n
    val u = DoubleArray(n + 1)
    val v = DoubleArray(m + 1)
    val p = IntArray(m + 1)
    val way = IntArray(m + 1)
    for (i in 1..n) {
        p[0] = i
        var j0 = 0
        val min = DoubleArray(m + 1) { Double.POSITIVE_INFINITY }
        val used = BooleanArray(m + 1)
        do {
            used[j0] = true
            val i0 = p[j0]
            var delta = Double.POSITIVE_INFINITY
            var j1 = 0
            for (j in 1..m) if (!used[j]) {
                val cost = -(if (j <= realColumns) weights[i0 - 1][j - 1] else 0.0) - u[i0] - v[j]
                if (cost < min[j]) { min[j] = cost; way[j] = j0 }
                if (min[j] < delta) { delta = min[j]; j1 = j }
            }
            for (j in 0..m) {
                if (used[j]) { u[p[j]] += delta; v[j] -= delta } else min[j] -= delta
            }
            j0 = j1
        } while (p[j0] != 0)
        do { val j1 = way[j0]; p[j0] = p[j1]; j0 = j1 } while (j0 != 0)
    }
    return IntArray(n) { -1 }.also { assignment ->
        for (j in 1..m) if (p[j] != 0 && j <= realColumns) assignment[p[j] - 1] = j - 1
    }
}

/** Reserve only final surviving names, not names that will be renamed or pruned. */
internal fun allocateTopicNames(requested: Map<String, String>, reserved: Collection<String>): Map<String, String> {
    val used = reserved.map { it.lowercase(Locale.ROOT) }.toMutableSet()
    return requested.toSortedMap().mapValues { (_, desired) ->
        val base = desired.trim().ifBlank { "Untitled topic" }.take(40)
        var name = base
        var suffix = 2
        while (name.lowercase(Locale.ROOT) in used) {
            val ending = " (${suffix++})"
            name = base.take(40 - ending.length) + ending
        }
        used += name.lowercase(Locale.ROOT)
        name
    }
}
