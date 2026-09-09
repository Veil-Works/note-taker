package app.notetaker.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteConnectionsTest {
    @Test fun `connections follow shared tags and preserve independent manual links`() {
        val t = Tag("t", "Work", 0)
        val u = Tag("u", "Project", 0)
        val a = Note("a", "A", 0, 0, listOf(t, u))
        val b = Note("b", "B", 0, 0, listOf(t, u))
        assertEquals(setOf("b"), a.connectedNoteIds(listOf(a, b)))
        assertEquals(setOf("a"), b.connectedNoteIds(listOf(a, b)))
        assertEquals(setOf("b"), a.connectedNoteIds(listOf(a, b.copy(tags = listOf(u)))))
        assertEquals(emptySet<String>(), a.connectedNoteIds(listOf(a, b.copy(tags = emptyList()))))
        assertEquals(setOf("b"), a.copy(linkedNoteIds = setOf("b")).connectedNoteIds(listOf(a, b.copy(tags = emptyList()))))
    }
}
