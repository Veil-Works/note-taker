package app.notetaker.model

data class TagAlias(val id: String, val tagId: String, val text: String)

private val wordPattern = Regex("[\\p{L}\\p{M}\\p{N}_]+")
private val lettersOnly = Regex("[\\p{L}\\p{M}]+")

/** Match literal terms, plus word prefixes of at least three letters in either direction. */
fun matchesTagPhrase(content: String, phrase: String): Boolean {
    val trimmed = phrase.trim()
    if (trimmed.isEmpty()) return false
    val pattern = "(?<![\\p{L}\\p{M}\\p{N}_])" + Regex.escape(trimmed) + "(?![\\p{L}\\p{M}\\p{N}_])"
    if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(content)) return true

    // Keep punctuation-bearing names such as C++ literal, and avoid partial numeric IDs.
    val phraseWords = trimmed.split(Regex("\\s+"))
    if (phraseWords.any { !lettersOnly.matches(it) }) return false
    val contentWords = wordPattern.findAll(content).map { it.value }.toList()
    return contentWords.windowed(phraseWords.size).any { window ->
        window.zip(phraseWords).all { (word, term) ->
            word.equals(term, ignoreCase = true) ||
                (lettersOnly.matches(word) &&
                    minOf(word.codePointCount(0, word.length), term.codePointCount(0, term.length)) >= 3 &&
                    (word.startsWith(term, ignoreCase = true) || term.startsWith(word, ignoreCase = true)))
        }
    }
}

fun suggestTags(content: String, tags: List<Tag>, aliases: List<TagAlias>, excludedIds: Set<String>): List<Tag> =
    tags.filter { tag ->
        tag.id !in excludedIds && (matchesTagPhrase(content, tag.name) ||
            aliases.any { it.tagId == tag.id && matchesTagPhrase(content, it.text) })
    }.distinctBy { it.id }.sortedWith(compareBy({ it.name.lowercase(java.util.Locale.ROOT) }, { it.id }))
