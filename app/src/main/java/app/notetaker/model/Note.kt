package app.notetaker.model

/** A user-authored note. IDs are UUID strings and timestamps are UTC epoch milliseconds. */
data class Note(
    val id: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<Tag> = emptyList(),
    val linkedNoteIds: Set<String> = emptySet(),
)

fun Note.preview(): String =
    content.lineSequence()
        .map(String::trim)
        .firstOrNull(String::isNotEmpty)
        ?: "Empty note"
