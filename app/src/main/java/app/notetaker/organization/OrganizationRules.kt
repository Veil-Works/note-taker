package app.notetaker.organization

import app.notetaker.model.Tag
import app.notetaker.model.TagAlias
import java.util.Locale

/** No prefix expansion here: suggestion matching is deliberately weaker than automatic attachment. */
fun exactConceptMatches(content: String, tags: List<Tag>, aliases: List<TagAlias>): Map<String, String> {
    val terms = tags.flatMap { tag -> (listOf(tag.name) + aliases.filter { it.tagId == tag.id }.map { it.text })
        .map { it.trim().lowercase(Locale.ROOT) to tag.id } }.groupBy({ it.first }, { it.second })
    val found = mutableMapOf<String, String>()
    terms.forEach { (phrase, ids) ->
        if (ids.toSet().size == 1 && phrase.isNotBlank()) {
            val whole = Regex("(?<![\\p{L}\\p{M}\\p{N}_])" + Regex.escape(phrase) + "(?![\\p{L}\\p{M}\\p{N}_])", RegexOption.IGNORE_CASE)
            if (whole.containsMatchIn(content)) found[ids.first()] = phrase
        }
    }
    return found
}

private val stopWords = ("a an the and or but to of in on at for with from by my me i it is was were be been am are " +
    "this that these those today yesterday tomorrow note notes about had have has do did done felt very some more " +
    "then than as so we you they he she our their its not no again new need want just also into after before each last first next still really get got take make can could should would will been only all any other out off too much time week monday tuesday wednesday thursday friday saturday sunday").split(" ").toSet()
// Naming rules describe existing groups; they never change connections or membership.
private val dogTerms = setOf("dog", "dogs", "puppy", "puppies", "canine", "doberman", "dobermann", "rottweiler", "labrador", "poodle", "beagle", "chihuahua", "dachshund", "husky", "bulldog", "corgi")
internal fun sharedCategory(contents: List<String>): String? {
    if (contents.size < 2) return null
    val words = contents.map { Regex("[\\p{L}]+").findAll(it.lowercase(Locale.ROOT)).map { match -> match.value }.toSet() }
    if (words.all { tokens -> tokens.any { it in dogTerms } }) return "Dogs"
    val runs = contents.zip(words).all { (text, tokens) ->
        tokens.any { it in setOf("running", "jog", "jogging", "jogged") } ||
            ("run" in tokens && Regex("\\b\\d+(?:[.,]\\d+)?\\s*(?:km|mi|miles|kilometers|kilometres)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text))
    }
    return if (runs) "Running" else null
}

fun topicName(contents: List<String>): String {
    sharedCategory(contents)?.let { return it }
    val phrases = mutableMapOf<String, Int>()
    contents.forEach { content ->
        val words = Regex("[\\p{L}][\\p{L}-]*").findAll(content.lowercase(Locale.ROOT)).map { it.value }.toList()
        val candidates = (1..3).flatMap { size -> words.windowed(size).filter { window -> window.all { it.length >= 3 && it !in stopWords } }.map { it.joinToString(" ") } }.toSet()
        candidates.forEach { phrases[it] = (phrases[it] ?: 0) + 1 }
    }
    return phrases.entries.filter { it.value >= minOf(2, contents.size) }.sortedWith(compareByDescending<Map.Entry<String, Int>> {
        // Prefer repeated phrases, then informative multiword phrases from the representative note.
        it.value * (1.0 + 0.35 * it.key.count { c -> c == ' ' })
    }.thenBy { it.key }).firstOrNull()?.key?.take(40)?.replaceFirstChar { it.titlecase(Locale.ROOT) } ?: "Related notes"
}

/** Repeated leading subjects are useful evidence, but only with semantic agreement.
 * A generic repeated word alone must not join unrelated notes. */
fun recurringSubjects(contents: Map<String, String>, vectors: Map<String, FloatArray>): Map<String, String> {
    val candidates = contents.entries.mapNotNull { (id, text) ->
        val word = Regex("^[\\p{L}][\\p{L}-]*").find(text.trim().lowercase(Locale.ROOT))?.value
        if (word == null || word.length < 3 || word in stopWords) null else word to id
    }.groupBy({ it.first }, { it.second })
    return buildMap {
        candidates.toSortedMap().forEach { (word, ids) ->
            if (ids.size >= 3) {
                val coherent = ids.filter { id ->
                    ids.filter { it != id }.map { similarity(vectors.getValue(id), vectors.getValue(it)) }.sortedDescending().take(3).average() >= 0.30
                }
                if (coherent.size >= 3) coherent.forEach { put(it, word) }
            }
        }
    }
}

/** Average agreement avoids requiring every pair to match, with a floor to stop topic drift. */
fun fitsTopic(scores: List<Double>): Boolean = scores.isNotEmpty() &&
    scores.average() >= 0.42 && scores.min() >= 0.20

/** Similarity is not a probability. A recurring subject supports otherwise weaker matches. */
fun relevantConnectionScore(a: String, b: String, vectors: Map<String, FloatArray>, subjects: Map<String, String>): Double? {
    val semantic = similarity(vectors.getValue(a), vectors.getValue(b))
    val sameSubject = subjects[a] != null && subjects[a] == subjects[b]
    if (semantic < 0.42 && !(sameSubject && semantic >= 0.20)) return null
    return if (sameSubject) maxOf(semantic, 0.38 + semantic * 0.10) else semantic
}
