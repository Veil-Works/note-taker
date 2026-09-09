package app.notetaker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RoomNoteRepositoryTest {
    @Test
    fun `LIKE wildcards and escape character are escaped`() {
        assertEquals("100\\%\\_done\\\\later", "100%_done\\later".escapeForLike())
    }
}

