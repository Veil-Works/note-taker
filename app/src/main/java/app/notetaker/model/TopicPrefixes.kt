package app.notetaker.model

import java.util.Locale

data class TopicPrefixedNote(val content: String, val topics: List<String>)

/** Only a whitespace-separated prefix is syntax; hashtags in the body stay literal. */
fun parseTopicPrefixes(text: String): TopicPrefixedNote {
    val token = Regex("#(?:\"([^\"\\r\\n]+)\"|([\\p{L}\\p{N}_][\\p{L}\\p{M}\\p{N}_-]*))(?=\\s|$)")
    var offset = text.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: 0
    val names = mutableListOf<String>()
    while (offset < text.length) {
        val match = token.find(text, offset)?.takeIf { it.range.first == offset } ?: break
        val name = (match.groups[1]?.value ?: match.groups[2]!!.value).trim()
        require(name.isNotBlank() && name.length <= 40) { "Use topic names between 1 and 40 characters." }
        names += name
        offset = match.range.last + 1
        while (offset < text.length && text[offset].isWhitespace()) offset++
    }
    return TopicPrefixedNote(if (names.isEmpty()) text else text.substring(offset), names.distinctBy { it.lowercase(Locale.ROOT) })
}
