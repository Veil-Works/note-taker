package app.notetaker.model

import org.junit.Assert.*
import org.junit.Test

class RelatedHistoryTest {
    private val notes = listOf(Note("a", "French", 20, 900), Note("b", "French again", 10, 10),
        Note("c", "Japanese", 30, 30), Note("d", "Standalone credential", 40, 40))

    @Test fun `strength changes direct neighbors without following a transitive chain`() {
        val edges = listOf(GraphEdge("a", "b", .8), GraphEdge("a", "c", .5), GraphEdge("c", "d", .9))
        assertEquals(listOf("b", "a"), relatedHistory("a", notes, edges, ConnectionScope.CLOSE).map { it.id })
        assertEquals(listOf("b", "a", "c"), relatedHistory("a", notes, edges, ConnectionScope.BROAD).map { it.id })
        assertEquals(listOf("b", "a"), relatedHistory("b", notes, edges, ConnectionScope.BROAD).map { it.id })
    }

    @Test fun `shared labels do not imply connections and standalone remains accessible`() {
        val tagged = notes.map { it.copy(tags = listOf(Tag("same", "Language", 1))) }
        assertEquals(listOf("a"), relatedHistory("a", tagged, emptyList(), ConnectionScope.BROAD).map { it.id })
        assertEquals(listOf("d"), relatedHistory("d", notes, emptyList(), ConnectionScope.CLOSE).map { it.id })
        assertTrue(relatedHistory("deleted", notes, emptyList(), ConnectionScope.CLOSE).isEmpty())
    }

    @Test fun `manual links survive either scope and duplicates dangling and invalid edges do not leak`() {
        val edges = listOf(GraphEdge("b", "a", .1, manual = true), GraphEdge("a", "b", .1, manual = true),
            GraphEdge("a", "deleted", .9), GraphEdge("a", "d", Double.NaN))
        val linked = notes.map { if (it.id == "c") it.copy(linkedNoteIds = setOf("a")) else it }
        assertEquals(listOf("b", "a", "c"), relatedHistory("a", linked, edges, ConnectionScope.CLOSE).map { it.id })
    }
}
