package app.notetaker.model

data class EntityType(val id: String, val name: String)

data class Tag(val id: String, val name: String, val createdAt: Long, val typeId: String? = null, val typeName: String? = null, val provisional: Boolean = false) {
    val displayName: String get() = name
}
