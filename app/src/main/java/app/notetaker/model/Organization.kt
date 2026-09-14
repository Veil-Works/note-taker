package app.notetaker.model

data class RelatedNote(val noteId: String, val score: Double)
data class Topic(val id: String, val name: String, val noteIds: Set<String>, val customized: Boolean = false)
data class OrganizationStatus(val pending: Boolean = false, val error: String? = null)

data class GraphEdge(val source: String, val target: String, val strength: Double, val manual: Boolean = false)
