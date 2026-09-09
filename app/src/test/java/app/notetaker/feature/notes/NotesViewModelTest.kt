package app.notetaker.feature.notes

import androidx.lifecycle.SavedStateHandle
import app.notetaker.FakeNoteRepository
import app.notetaker.MainDispatcherRule
import app.notetaker.model.Note
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {
    @Test
    fun `capture saves only on completion and clears after success`() = runTest {
        val repository = FakeNoteRepository()
        val viewModel = NotesViewModel(repository, SavedStateHandle())
        viewModel.onDraftChanged("First thought")
        advanceUntilIdle()
        assertEquals(0, repository.savedNotes.size)
        viewModel.completeNote()
        advanceUntilIdle()
        assertEquals("First thought", repository.savedNotes.single().content)
        assertEquals("", viewModel.draft.value)
        viewModel.completeNote()
        assertEquals(1, repository.savedNotes.size)
        viewModel.onDraftChanged("Next thought")
        viewModel.completeNote()
        advanceUntilIdle()
        assertEquals(2, repository.savedNotes.size)
    }

    @Test
    fun `failed completion keeps draft for retry`() = runTest {
        val repository = FakeNoteRepository().apply { failure = IllegalStateException("full") }
        val viewModel = NotesViewModel(repository, SavedStateHandle())
        viewModel.onDraftChanged("Keep me")
        viewModel.completeNote()
        advanceUntilIdle()
        assertEquals("Keep me", viewModel.draft.value)
        assertEquals(0, repository.savedNotes.size)
        repository.failure = null
        viewModel.completeNote()
        advanceUntilIdle()
        assertEquals("Keep me", repository.savedNotes.single().content)
        assertEquals("", viewModel.draft.value)
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `notes are loaded in repository order`() = runTest {
        val newer = Note("2", "Newer", 2, 2)
        val older = Note("1", "Older", 1, 1)
        val viewModel = NotesViewModel(FakeNoteRepository(listOf(older, newer)), SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(listOf(newer, older), viewModel.uiState.value.notes)
    }

    @Test
    fun `search query is debounced and retained in state`() = runTest {
        val repository = FakeNoteRepository(listOf(Note("1", "Traffic idea", 1, 1)))
        val savedState = SavedStateHandle()
        val viewModel = NotesViewModel(repository, savedState)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onQueryChanged("traffic")
        assertEquals("traffic", viewModel.uiState.value.query)
        assertEquals("traffic", savedState.get<String>("notes_query"))
        advanceTimeBy(249)
        assertEquals(listOf(""), repository.receivedQueries)

        advanceTimeBy(1)
        advanceUntilIdle()
        assertEquals(listOf("", "traffic"), repository.receivedQueries)
        assertEquals(listOf("Traffic idea"), viewModel.uiState.value.notes.map(Note::content))
    }
}
