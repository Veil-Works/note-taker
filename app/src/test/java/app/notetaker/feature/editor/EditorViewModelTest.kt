package app.notetaker.feature.editor

import androidx.lifecycle.SavedStateHandle
import app.notetaker.FakeNoteRepository
import app.notetaker.MainDispatcherRule
import app.notetaker.model.Note
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {
    @Test
    fun `tags can be attached and removed without changing content`() = runTest {
        val note = Note("note", "Original", 1, 2)
        val repository = FakeNoteRepository(listOf(note))
        val viewModel = editor(repository, "note")
        advanceUntilIdle()
        val tag = app.notetaker.model.Tag("tag", "Research", 1)
        viewModel.toggleTag(tag)
        advanceUntilIdle()
        assertEquals(listOf(tag), repository.getNote("note")!!.tags)
        viewModel.toggleTag(tag)
        advanceUntilIdle()
        assertEquals(emptyList<app.notetaker.model.Tag>(), repository.getNote("note")!!.tags)
        assertEquals("Original", repository.getNote("note")!!.content)
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `blank new draft closes without creating a note`() = runTest {
        val repository = FakeNoteRepository()
        val viewModel = editor(repository)
        var closed = false

        viewModel.close { closed = true }
        advanceUntilIdle()

        assertTrue(closed)
        assertTrue(repository.savedNotes.isEmpty())
    }

    @Test
    fun `non-empty draft autosaves after debounce`() = runTest {
        val repository = FakeNoteRepository()
        val viewModel = editor(repository)

        viewModel.onContentChanged("A useful thought")
        advanceTimeBy(599)
        assertTrue(repository.savedNotes.isEmpty())

        advanceTimeBy(1)
        advanceUntilIdle()
        assertEquals("A useful thought", repository.savedNotes.single().content)
        assertEquals("generated-id", repository.savedNotes.single().id)
        assertTrue(viewModel.uiState.value.isExistingNote)
    }

    @Test
    fun `editing preserves identity and creation timestamp`() = runTest {
        val original = Note("existing", "Before", createdAt = 10, updatedAt = 20)
        val repository = FakeNoteRepository(listOf(original))
        val viewModel = editor(repository, noteId = original.id)
        advanceUntilIdle()

        viewModel.onContentChanged("After")
        advanceTimeBy(600)
        advanceUntilIdle()

        val saved = repository.savedNotes.single()
        assertEquals(original.id, saved.id)
        assertEquals(original.createdAt, saved.createdAt)
        assertEquals(100L, saved.updatedAt)
        assertEquals("After", saved.content)
    }

    @Test
    fun `delete first saves current text and returns restorable snapshot`() = runTest {
        val original = Note("existing", "Before", createdAt = 10, updatedAt = 20)
        val repository = FakeNoteRepository(listOf(original))
        val viewModel = editor(repository, noteId = original.id)
        advanceUntilIdle()
        viewModel.onContentChanged("Latest")
        var deleted: Note? = null

        viewModel.delete { deleted = it }
        advanceUntilIdle()

        assertEquals("Latest", repository.deletedNotes.single().content)
        assertEquals(repository.deletedNotes.single(), deleted)
    }

    @Test
    fun `failed save keeps editor open and exposes an error`() = runTest {
        val repository = FakeNoteRepository().apply { failure = IllegalStateException("disk full") }
        val viewModel = editor(repository)
        viewModel.onContentChanged("Unsaved")
        var closed = false

        viewModel.close { closed = true }
        advanceUntilIdle()

        assertFalse(closed)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `recreated new editor reloads its saved UUID instead of duplicating the note`() = runTest {
        val repository = FakeNoteRepository()
        val savedState = SavedStateHandle()
        val firstViewModel = EditorViewModel(
            repository = repository,
            requestedNoteId = null,
            savedStateHandle = savedState,
            now = { 100L },
            newId = { "stable-generated-id" },
        )
        firstViewModel.onContentChanged("Before recreation")
        advanceTimeBy(600)
        advanceUntilIdle()

        val recreatedViewModel = EditorViewModel(
            repository = repository,
            requestedNoteId = null,
            savedStateHandle = savedState,
            now = { 200L },
            newId = { "duplicate-id" },
        )
        advanceUntilIdle()
        recreatedViewModel.onContentChanged("After recreation")
        advanceTimeBy(600)
        advanceUntilIdle()

        val latest = repository.savedNotes.last()
        assertEquals("stable-generated-id", latest.id)
        assertEquals(100L, latest.createdAt)
        assertEquals("After recreation", latest.content)
    }

    private fun editor(
        repository: FakeNoteRepository,
        noteId: String? = null,
    ) = EditorViewModel(
        repository = repository,
        requestedNoteId = noteId,
        savedStateHandle = SavedStateHandle(),
        now = { 100L },
        newId = { "generated-id" },
    )
}
