package app.notetaker.organization

import java.security.MessageDigest
import java.util.Locale
import kotlin.math.ln

/** Discovery uses current text only. These cores never become user training examples. */
data class DiscoveryNote(val id: String, val text: String, val vector: FloatArray)
data class TopicCandidate(
    val key: String, val name: String, val members: Set<String>,
    val scores: Map<String, Double>, val reasons: Map<String, String>,
    val namedSubject: Boolean = false,
)

private val genericWords = ("a an the and or but to of in on at for with from by my me i it is was were be been are " +
    "this that these those today yesterday tomorrow note notes about had have has do did done felt very some more " +
    "then than as so we you they he she our their its not no again new need want just also into after before each last first next still really get got take make can could should would will been only all any other out off too much time week monday tuesday wednesday thursday friday saturday sunday " +
    "idea ideas observation observations todo todos task tasks session sessions work thing things try check use using add ask keep same few every look one two three during instead should better feels feel").split(' ').toSet()
private val wordPattern = Regex("[\\p{L}][\\p{L}\\p{N}-]*")
private fun stem(word: String): String {
    val singular = if (word.endsWith("s") && word.length > 4 && !word.endsWith("ss")) word.dropLast(1) else word
    return when {
        singular == "running" || singular == "runs" -> "run"
        singular.endsWith("ing") && singular.length > 6 -> singular.dropLast(3).let { if (it.length > 2 && it.last() == it[it.lastIndex - 1]) it.dropLast(1) else it }
        else -> singular
    }
}
internal fun subjectWords(text: String): Set<String> = wordPattern.findAll(text.lowercase(Locale.ROOT))
    .map { it.value }.filter { it.length >= 3 && it !in genericWords }.map(::stem).toSet()

/** A meaningful opening sentence is extra evidence, never a replacement for the whole note. */
internal fun topicFocus(text: String): String {
    val first = text.split(Regex("(?<=[.!?])\\s+|\\n"), limit = 2).first().trim()
    return if (subjectWords(first).size >= 3) first else text
}

/** Keep list items together; only independently meaningful clauses add another view. */
internal fun topicFragments(text: String): List<String> = text
    .split(Regex("(?<=[.!?])\\s+|[;\\n]+"))
    .flatMap { sentence ->
        val clauses = sentence.split(Regex("\\s+(?:and|but|then)\\s+", RegexOption.IGNORE_CASE))
        if (clauses.size >= 2 && clauses.all { subjectWords(it).size >= 3 }) clauses else listOf(sentence)
    }.map { it.trim() }.filter { it != text.trim() && subjectWords(it).size >= 3 }.distinct().take(4)

/** Normalize observed word forms, retaining real words rather than displaying stems. */
internal fun observedLabelCandidates(contents: List<String>): Set<String> {
    val occurrences = mutableMapOf<String, MutableSet<Int>>()
    val forms = mutableMapOf<String, MutableSet<String>>()
    contents.forEachIndexed { index, text ->
        val tokens = wordPattern.findAll(text.lowercase(Locale.ROOT)).map { it.value }.toList()
        (1..3).forEach { size -> tokens.windowed(size).forEach phraseLoop@{ phrase ->
            if (phrase.any { it.length < 3 || it in genericWords }) return@phraseLoop
            val key = phrase.joinToString(" ") { stem(it) }
            occurrences.getOrPut(key) { mutableSetOf() } += index
            forms.getOrPut(key) { mutableSetOf() } += phrase.joinToString(" ")
        } }
    }
    return occurrences.filterValues { it.size >= 2 }.keys.flatMap { forms.getValue(it) }.toSortedSet()
}

/** Split only independently coherent cohorts separated by a substantial similarity gap. */
internal fun coherentPartition(ids: Set<String>, sim: (String, String) -> Double): List<Set<String>> {
    if (ids.size < 6) return listOf(ids)
    val ordered = ids.sorted()
    val pair = ordered.flatMapIndexed { i, a -> ordered.drop(i + 1).map { b -> a to b } }
        .minByOrNull { (a, b) -> sim(a, b) } ?: return listOf(ids)
    var left = setOf(pair.first)
    var right = setOf(pair.second)
    repeat(4) {
        val next = ordered.partition { id ->
            left.map { sim(id, it) }.average() >= right.map { sim(id, it) }.average()
        }
        if (next.first.size < 3 || next.second.size < 3) return listOf(ids)
        left = next.first.toSet(); right = next.second.toSet()
    }
    fun within(group: Set<String>): Double = group.flatMap { a -> group.filter { it != a }.map { sim(a, it) } }.average()
    val cross = left.flatMap { a -> right.map { sim(a, it) } }.average()
    val weakestWithin = minOf(within(left), within(right))
    return if (weakestWithin >= .45 && cross <= .38 && weakestWithin - cross >= .14) listOf(left, right) else listOf(ids)
}

/** Reward coverage across the group, rather than an excellent match to just one member. */
internal fun labelAgreement(whole: List<Double>, focus: List<Double>): Double {
    require(whole.isNotEmpty() && whole.size == focus.size)
    val agreement = whole.zip(focus) { a, b -> .5 * a + .5 * b }.sorted()
    val weakest = agreement.take(kotlin.math.ceil(agreement.size * .25).toInt()).average()
    return .65 * agreement.average() + .35 * weakest
}

/** Two independent neighbors are required; a single bridge cannot admit a note. */
fun representativeMatch(scores: List<Double>, lexicalSupport: Boolean = false): Double? {
    if (scores.size < 2) return null
    val top = scores.sortedDescending().take(3)
    val pair = top.take(2).average()
    val accepted = (top[0] >= .52 && top[1] >= (if (lexicalSupport) .32 else .38) && pair >= .44) ||
        (top[0] >= .46 && top[1] >= .38 && top.average() >= .42) ||
        (top.size == 3 && top[1] >= .40 && top[2] >= .28 && top.average() >= .37) ||
        (lexicalSupport && top[0] >= .40 && top[1] >= .30 && pair >= .38)
    return if (accepted) pair else null
}

/** Capitalization inside sentences distinguishes recurring names from generic sentence starts. */
fun namedSubjectMembers(notes: List<DiscoveryNote>): Map<String, Set<String>> {
    val occurrences = mutableMapOf<String, MutableSet<String>>()
    val internalCapital = mutableMapOf<String, Int>()
    notes.forEach { note ->
        wordPattern.findAll(note.text).forEach { match ->
            val word = match.value.lowercase(Locale.ROOT)
            if (word.length >= 3 && word !in genericWords) {
                occurrences.getOrPut(word) { mutableSetOf() } += note.id
                val before = note.text.take(match.range.first).trimEnd()
                if (match.value.first().isUpperCase() && before.isNotEmpty() && before.last() !in ".!?\n") {
                    internalCapital[word] = (internalCapital[word] ?: 0) + 1
                }
            }
        }
    }
    val byId = notes.associateBy { it.id }
    return occurrences.toSortedMap().mapNotNull { (word, ids) ->
        if (ids.size < 3 || (internalCapital[word] ?: 0) < 2) return@mapNotNull null
        val coherent = ids.filter { id ->
            val scores = ids.filter { it != id }.map { similarity(byId.getValue(id).vector, byId.getValue(it).vector) }.sortedDescending()
            scores.take(2).average() >= .30 && scores.getOrElse(1) { 0.0 } >= .20
        }.toSet()
        if (coherent.size >= 3) word to coherent else null
    }.toMap()
}

/** Deterministic core construction, then independent expansion against frozen cores. */
class TopicDiscovery(private val embedding: TextEmbedding) {
    fun discover(input: List<DiscoveryNote>): List<TopicCandidate> {
        val notes = input.filter { it.text.isNotBlank() }.sortedWith(compareBy<DiscoveryNote> { it.text }.thenBy { it.id })
        if (notes.size < 2) return emptyList()
        notes.forEach { labelVectors[it.text] = it.vector }
        val fragments = notes.associate { note -> note.id to topicFragments(note.text).map { it to labelVector(it) } }
        val byId = notes.associateBy { it.id }
        val index = notes.mapIndexed { i, note -> note.id to i }.toMap()
        val matrix = Array(notes.size) { i -> DoubleArray(notes.size) { j -> if (i == j) 1.0 else similarity(notes[i].vector, notes[j].vector) } }
        fun sim(a: String, b: String) = matrix[index.getValue(a)][index.getValue(b)]
        val words = notes.associate { it.id to subjectWords(it.text) }
        val frequency = words.values.flatMap { it }.groupingBy { it }.eachCount()
        fun usefulShared(a: Set<String>, b: Set<String>) = a.intersect(b).any { (frequency[it] ?: 0) <= notes.size * .4 }
        val named = namedSubjectMembers(notes)
        val namedIds = named.values.flatten().toSet()
        val cores = notes.filter { it.id !in namedIds }.map { mutableSetOf(it.id) }.toMutableList()
        fun coreWords(ids: Set<String>) = ids.flatMap { words.getValue(it) }.groupingBy { it }.eachCount()
            .filter { (_, count) -> count >= maxOf(2, kotlin.math.ceil(ids.size * .4).toInt()) }.keys
        // Globally select the strongest eligible merge, instead of depending on insertion order.
        while (true) {
            var best: Pair<Int, Int>? = null
            var bestScore = -1.0
            for (a in cores.indices) for (b in a + 1 until cores.size) {
                val cross = cores[a].flatMap { x -> cores[b].map { y -> sim(x, y) } }
                val shared = if (cores[a].size == 1 && cores[b].size == 1)
                    usefulShared(words.getValue(cores[a].first()), words.getValue(cores[b].first()))
                else usefulShared(coreWords(cores[a]), coreWords(cores[b]))
                val strongPair = cores[a].size == 1 && cores[b].size == 1 && (cross[0] >= .42 || shared && cross[0] >= .40)
                val coherent = cores[a].size + cores[b].size >= 3 && cross.min() >= .20 && cross.average() >= .42
                val sameSubject = cores[a].size >= 2 && cores[b].size >= 2 && shared && cross.average() >= .30 && cross.max() >= .46
                if ((strongPair || coherent || sameSubject) && cross.average() > bestScore) {
                    best = a to b; bestScore = cross.average()
                }
            }
            val (a, b) = best ?: break
            cores[a].addAll(cores[b]); cores.removeAt(b)
        }
        val seeds = named.map { (name, ids) -> name to ids } + cores.filter { it.size >= 2 }.map { null to it.toSet() }
        val results = seeds.mapNotNull { (name, core) ->
            // Central representatives are kept separate from admitted notes to prevent chaining.
            val representatives = core.sortedWith(compareByDescending<String> { id -> core.filter { it != id }.map { sim(id, it) }.average() }
                .thenBy { byId.getValue(it).text }).take(5).toSet()
            val scores = mutableMapOf<String, Double>()
            val reasons = mutableMapOf<String, String>()
            notes.forEach { note ->
                val peers = (if (note.id in core) core else representatives).filter { it != note.id }
                val values = peers.map { sim(note.id, it) }
                val namedMatch = name != null && wordPattern.findAll(note.text.lowercase(Locale.ROOT)).any { it.value == name } &&
                    values.sortedDescending().take(2).average() >= .25
                val supported = usefulShared(words.getValue(note.id), coreWords(core))
                val match = representativeMatch(values, supported)
                val lexicalSubject = name == null && supported && values.sortedDescending().take(2).let { it.size == 2 && it[0] >= .38 && it[1] >= .28 && it.average() >= .34 }
                val strongUnnamedMatch = values.sortedDescending().take(2).let { it.size == 2 && it[0] >= .65 && it[1] >= .50 }
                val otherNamedSubject = name != null && named.any { (other, ids) -> other != name && note.id in ids } && !namedMatch
                // The same clause must match multiple independent notes; never pool unrelated clauses.
                val clauseMatch = if (name == null && note.id !in core) fragments.getValue(note.id).mapNotNull { (text, vector) ->
                    val clauseScores = peers.map { similarity(vector, byId.getValue(it).vector) }.sortedDescending()
                    val corroborated = clauseScores.size >= 2 && clauseScores[0] >= .48 && clauseScores[1] >= .34 && clauseScores.take(2).average() >= .42
                    if (corroborated && values.maxOrNull()!! >= .30) text to clauseScores.take(2).average() else null
                }.maxByOrNull { it.second } else null
                if (note.id in core || namedMatch || clauseMatch != null || ((match != null || lexicalSubject) && !otherNamedSubject && (name == null || strongUnnamedMatch))) {
                    scores[note.id] = clauseMatch?.second ?: (values.sortedDescending().take(2).average().takeIf { it.isFinite() } ?: 0.0)
                    reasons[note.id] = if (namedMatch) "Mentions ${name!!.replaceFirstChar { it.titlecase(Locale.ROOT) }} and matches its notes"
                        else if (clauseMatch != null) "“${clauseMatch.first.take(72)}” matches multiple notes"
                        else "Similar to “${peers.maxByOrNull { sim(note.id, it) }?.let { byId.getValue(it).text }?.take(72).orEmpty()}”"
                }
            }
            val contents = core.map { byId.getValue(it).text }
            val label = name?.replaceFirstChar { it.titlecase(Locale.ROOT) } ?: nameTopic(contents, notes.map { it.text }) ?: return@mapNotNull null
            if (name == null) {
                val labelVector = labelVector(label)
                val terms = subjectWords(label)
                // A weak admission must not invalidate an otherwise coherent, well-named core.
                val unsupported = scores.keys.filter { id -> id !in core && !words.getValue(id).containsAll(terms) &&
                    (.5 * similarity(byId.getValue(id).vector, labelVector) +
                        .5 * similarity(labelVector(topicFocus(byId.getValue(id).text)), labelVector)) < .20 }
                unsupported.forEach { scores.remove(it); reasons.remove(it) }
            }
            val members = scores.keys.toSet()
            TopicCandidate(fingerprint(members.map { byId.getValue(it).text }), label, members, scores, reasons, namedSubject = name != null)
        }
        // Near-identical inferred groups add clutter, not another useful topic.
        val consolidated = results.sortedWith(compareByDescending<TopicCandidate> { it.members.size }.thenBy { it.name }).fold(mutableListOf<TopicCandidate>()) { kept, candidate ->
            val duplicate = kept.indexOfFirst { other ->
                val common = candidate.members.intersect(other.members).size
                val overlap = common.toDouble() / candidate.members.union(other.members).size
                overlap >= .75 || (common.toDouble() / minOf(candidate.members.size, other.members.size) >= .5 && candidate.name.equals(other.name, true))
            }
            if (duplicate < 0) kept += candidate else {
                val other = kept[duplicate]
                val members = other.members + candidate.members
                kept[duplicate] = other.copy(key = fingerprint(members.map { byId.getValue(it).text }), members = members,
                    scores = candidate.scores + other.scores, reasons = candidate.reasons + other.reasons)
            }
            kept
        }
        // Expansion and consolidation can broaden a group beyond its original core.
        // Re-evaluate descriptive labels against every final member; preserve explicit subjects.
        val usedLabels = consolidated.filter { it.namedSubject }.map { it.name }.toMutableSet()
        return consolidated.flatMap { group ->
            if (group.namedSubject) return@flatMap listOf(group)
            val partitions = coherentPartition(group.members, ::sim)
            val children = partitions.mapNotNull { members ->
                val label = nameTopic(members.map { byId.getValue(it).text }, notes.map { it.text }, usedLabels) ?: return@mapNotNull null
                group.copy(key = fingerprint(members.map { byId.getValue(it).text }), name = label, members = members,
                    scores = group.scores.filterKeys { it in members }, reasons = group.reasons.filterKeys { it in members })
            }
            val labeled = if (children.size == 2 && children.map { it.name.lowercase(Locale.ROOT) }.distinct().size == 2) children
            else nameTopic(group.members.map { byId.getValue(it).text }, notes.map { it.text }, usedLabels)?.let { listOf(group.copy(name = it)) }.orEmpty()
            usedLabels += labeled.map { it.name }
            labeled
        }
    }

    private fun fingerprint(contents: List<String>): String = "group-" + MessageDigest.getInstance("SHA-256")
        .digest(contents.sorted().joinToString { "${it.length}:$it" }.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private val labelVectors = mutableMapOf<String, FloatArray>()
    private fun labelVector(text: String) = labelVectors.getOrPut(text) { embedding.embed(text) }

    /** Candidate labels are evaluated semantically; no category changes membership. */
    private fun nameTopic(contents: List<String>, notebook: List<String>, excluded: Set<String> = emptySet()): String? {
        val leading = contents.map { wordPattern.find(it.trim())?.value }
        val sharedHeading = leading.firstOrNull()
        if (contents.size >= 3 && sharedHeading != null && sharedHeading.first().isUpperCase() &&
            sharedHeading.lowercase(Locale.ROOT) !in genericWords && leading.all { it == sharedHeading } && excluded.none { subjectWords(it) == subjectWords(sharedHeading) }) return sharedHeading.take(40)
        val repeated = topicName(contents)
        val phrases = observedLabelCandidates(contents)
        val purposes = listOf("Strength training", "Running", "Running gear", "Workout gear", "Meal planning", "Meal preparation", "Groceries", "Grocery shopping", "Shopping list", "Food shopping", "Shopping", "Gardening",
            "Meeting planning", "Note-taking", "Software debugging", "User experience design", "Project planning", "Travel planning", "Personal productivity")
        val vectors = contents.map(::labelVector)
        val focuses = contents.map(::topicFocus)
        val focusVectors = focuses.map(::labelVector)
        fun labelScore(label: String): Double {
            val vector = labelVector(label)
            val semantic = labelAgreement(vectors.map { similarity(it, vector) }, focusVectors.map { similarity(it, vector) })
            val term = Regex("(?<![\\p{L}\\p{N}_])" + Regex.escape(label) + "(?![\\p{L}\\p{N}_])", RegexOption.IGNORE_CASE)
            val occurrences = focuses.count { term.containsMatchIn(it) }
            val outside = notebook.count { it !in contents && term.containsMatchIn(it) }
            val distinctiveness = ln(1.0 + notebook.size.toDouble() / (outside + contents.size))
            return semantic + minOf(.015, .01 * occurrences.toDouble() / contents.size * distinctiveness) - (if (label.lowercase(Locale.ROOT) in genericWords) .15 else 0.0)
        }
        val candidates = (purposes + phrases.sortedByDescending { it.length }.take(40) + repeated).distinct().filter { label ->
            label != "Related notes" && subjectWords(label).isNotEmpty() && excluded.none { subjectWords(it) == subjectWords(label) }
        }
        fun coversGroup(label: String): Boolean {
            val vector = labelVector(label)
            val scores = vectors.indices.map { .5 * similarity(vectors[it], vector) + .5 * similarity(focusVectors[it], vector) }
            val terms = subjectWords(label)
            if (scores.indices.any { scores[it] < .20 && !subjectWords(contents[it]).containsAll(terms) } || scores.average() < .28) return false
            // A literal multiword phrase should not silently expand to a different subject.
            if (label.lowercase(Locale.ROOT) in phrases && label.contains(' ')) {
                if (contents.indices.any { !subjectWords(contents[it]).containsAll(terms) && scores[it] < .55 }) return false
            }
            return true
        }
        val eligible = candidates.filter(::coversGroup)
        val best = eligible.maxByOrNull(::labelScore) ?: return null
        // Prefer a shared purpose; a substantially stronger specific phrase can override it.
        val specific = eligible.filter { it.lowercase(Locale.ROOT) in phrases && it.contains(' ') }.maxByOrNull(::labelScore)
        val purpose = eligible.filter { it in purposes }.maxByOrNull(::labelScore)
        var preferred = if (purpose != null && labelScore(purpose) >= .30 && labelScore(purpose) >= labelScore(best) - .12) purpose else best
        // Prefer a shared, simpler observed subject when the extra qualifier adds little evidence.
        val broader = eligible.filter { candidate -> preferred.contains(' ') && candidate != preferred && !candidate.contains(' ') &&
            subjectWords(candidate).isNotEmpty() && subjectWords(preferred).containsAll(subjectWords(candidate)) &&
            contents.indices.all { subjectWords(contents[it]).containsAll(subjectWords(candidate)) ||
                similarity(vectors[it], labelVector(candidate)) >= .30 } }.maxByOrNull(::labelScore)
        if (broader != null && labelScore(broader) >= labelScore(preferred) - .12 &&
            contents.any { subjectWords(it).containsAll(subjectWords(preferred)) } &&
            contents.any { !subjectWords(it).containsAll(subjectWords(preferred)) }) preferred = broader
        val selected = if (specific != null && labelScore(specific) >= labelScore(preferred) + .04) specific else preferred
        return if (labelScore(selected) >= .28) selected.take(40).replaceFirstChar { it.titlecase(Locale.ROOT) } else null
    }
}
