package app.notetaker.model

data class TagAlias(val id: String, val tagId: String, val text: String)

/** Letters, marks, numbers and underscores form words; punctuation separates them. */
fun matchesTagPhrase(content: String, phrase: String): Boolean {
    if (phrase.isBlank()) return false
    val pattern = "(?<![\\p{L}\\p{M}\\p{N}_])" + Regex.escape(phrase.trim()) + "(?![\\p{L}\\p{M}\\p{N}_])"
    return Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(content)
}

fun suggestTags(content: String, tags: List<Tag>, aliases: List<TagAlias>, excludedIds: Set<String>): List<Tag> =
    tags.filter { tag ->
        tag.id !in excludedIds && (matchesTagPhrase(content, tag.name) ||
            aliases.any { it.tagId == tag.id && matchesTagPhrase(content, it.text) })
    }.distinctBy { it.id }.sortedWith(compareBy({ it.name.lowercase(java.util.Locale.ROOT) }, { it.id }))
