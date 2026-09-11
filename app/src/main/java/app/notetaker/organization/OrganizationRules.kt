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
    "then than as so we you they he she our their its not no again new need want just also into after before").split(" ").toSet()
fun topicName(contents: List<String>): String {
    val phrases = mutableMapOf<String, Int>()
    contents.forEach { content ->
        val words = Regex("[\\p{L}][\\p{L}-]*").findAll(content.lowercase(Locale.ROOT)).map { it.value }.toList()
        val candidates = (1..3).flatMap { size -> words.windowed(size).filter { window -> window.all { it.length >= 3 && it !in stopWords } }.map { it.joinToString(" ") } }.toSet()
        candidates.forEach { phrases[it] = (phrases[it] ?: 0) + 1 }
    }
    return phrases.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> {
        // Prefer repeated phrases, then informative multiword phrases from the representative note.
        it.value * (1.0 + 0.35 * it.key.count { c -> c == ' ' })
    }.thenBy { it.key }).firstOrNull()?.key?.take(40)?.replaceFirstChar { it.titlecase(Locale.ROOT) } ?: "Related notes"
}
