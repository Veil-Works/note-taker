package app.notetaker.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteTest {
    @Test
    fun `preview uses the first non-empty trimmed line`() {
        val note = Note("id", "  \n  First thought  \nSecond", 1, 1)

        assertEquals("First thought", note.preview())
    }

    @Test
    fun `preview describes an empty note`() {
        val note = Note("id", " \n\t", 1, 1)

        assertEquals("Empty note", note.preview())
    }
}

