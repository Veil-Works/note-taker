package app.notetaker.organization

import java.text.Normalizer
import java.util.Locale

/** BERT uncased basic tokenization + greedy WordPiece, matching the bundled tokenizer configuration. */
class WordPieceTokenizer(vocabulary: List<String>) {
    private val ids = vocabulary.withIndex().associate { it.value to it.index.toLong() }
    fun tokenize(text: String): List<Long> {
        val cleaned = buildString {
            text.codePoints().forEach { cp ->
                val category = Character.getType(cp)
                when {
                    cp == 0 || cp == 0xfffd -> Unit
                    Character.isWhitespace(cp) || category == Character.SPACE_SEPARATOR.toInt() -> append(' ')
                    category == Character.CONTROL.toInt() || category == Character.FORMAT.toInt() -> Unit
                    isCjk(cp) -> { append(' '); appendCodePoint(cp); append(' ') }
                    else -> appendCodePoint(cp)
                }
            }
        }
        val normalized = Normalizer.normalize(cleaned.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
        val basic = buildString {
            normalized.codePoints().forEach { cp ->
                if (isPunctuation(cp)) { append(' '); appendCodePoint(cp); append(' ') }
                else appendCodePoint(cp)
            }
        }.split(Regex("\\s+")).filter(String::isNotEmpty)
        return basic.flatMap { word ->
            if (word.length > 100) listOf(100L) else {
                val pieces = mutableListOf<Long>()
                var start = 0
                while (start < word.length) {
                    var end = word.length
                    var id: Long? = null
                    while (end > start) {
                        id = ids[(if (start == 0) "" else "##") + word.substring(start, end)]
                        if (id != null) break
                        end--
                    }
                    if (id == null) { pieces.clear(); pieces += 100L; break }
                    pieces += id
                    start = end
                }
                pieces
            }
        }
    }
    private fun isPunctuation(cp: Int): Boolean = cp in 33..47 || cp in 58..64 || cp in 91..96 || cp in 123..126 ||
        Character.getType(cp) in setOf(Character.CONNECTOR_PUNCTUATION.toInt(), Character.DASH_PUNCTUATION.toInt(), Character.START_PUNCTUATION.toInt(), Character.END_PUNCTUATION.toInt(), Character.INITIAL_QUOTE_PUNCTUATION.toInt(), Character.FINAL_QUOTE_PUNCTUATION.toInt(), Character.OTHER_PUNCTUATION.toInt())
    private fun isCjk(cp: Int) = cp in 0x4e00..0x9fff || cp in 0x3400..0x4dbf || cp in 0x20000..0x2a6df || cp in 0x2a700..0x2b73f || cp in 0x2b740..0x2b81f || cp in 0x2b820..0x2ceaf || cp in 0xf900..0xfaff || cp in 0x2f800..0x2fa1f
}
