package app.notetaker.model

enum class ConnectionScope(val minimum: Double) { CLOSE(.60), BROAD(.42) }

/** Direct neighbors only: neither shared tags nor transitive paths imply membership. */
fun relatedHistory(anchorId: String, notes: List<Note>, edges: List<GraphEdge>, scope: ConnectionScope): List<Note> {
    if (notes.none { it.id == anchorId }) return emptyList()
    val manual = notes.first { it.id == anchorId }.linkedNoteIds +
        notes.filter { anchorId in it.linkedNoteIds }.map { it.id }
    val neighbors = edges.filter { it.manual || (it.strength.isFinite() && it.strength >= scope.minimum) }
        .mapNotNull { edge -> when (anchorId) {
            edge.source -> edge.target
            edge.target -> edge.source
            else -> null
        } }.toSet() + manual
    return notes.filter { it.id == anchorId || it.id in neighbors }.distinctBy { it.id }
        .sortedWith(compareBy<Note> { it.createdAt }.thenBy { it.id })
}
