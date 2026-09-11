package app.notetaker.model

/** A user-authored note. IDs are UUID strings and timestamps are UTC epoch milliseconds. */
data class Note(
    val id: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<Tag> = emptyList(),
    val linkedNoteIds: Set<String> = emptySet(),
    val automaticTagReasons: Map<String, String> = emptyMap(),
    val rejectedTagIds: Set<String> = emptySet(),
)

fun Note.preview(): String =
    content.lineSequence()
        .map(String::trim)
        .firstOrNull(String::isNotEmpty)
        ?: "Empty note"

/** Automatic connections are derived, so they never become persisted manual links. */
fun Note.connectedNoteIds(notes: List<Note>): Set<String> {
    val tagIds = tags.map { it.id }.toSet()
    return linkedNoteIds + notes.filter { other ->
        other.id != id && other.tags.any { it.id in tagIds }
    }.map { it.id }
}
