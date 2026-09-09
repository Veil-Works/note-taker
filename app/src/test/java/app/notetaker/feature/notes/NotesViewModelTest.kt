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
    fun `tag filter combines with search and clears for new capture`() = runTest {
        val tag = app.notetaker.model.Tag("tag", "Work", 1)
        val tagged = Note("tagged", "Meeting", 1, 1, listOf(tag))
        val repository = FakeNoteRepository(listOf(tagged, Note("other", "Meeting", 1, 1)))
        val viewModel = NotesViewModel(repository, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        viewModel.selectTag(tag.id)
        viewModel.onQueryChanged("Meeting")
        advanceUntilIdle()
        assertEquals(listOf(tagged), viewModel.uiState.value.notes)
        viewModel.onQueryChanged("absent")
        advanceUntilIdle()
        assertEquals(emptyList<Note>(), viewModel.uiState.value.notes)
        viewModel.onDraftChanged("New")
        viewModel.completeNote()
        advanceUntilIdle()
        assertEquals(null, viewModel.selectedTag.value)
        assertEquals(3, viewModel.uiState.value.notes.size)
    }

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

    @Test
    fun `date preferences restore and completion clears all filters and sort`() = runTest {
        val state = SavedStateHandle()
        val repository = FakeNoteRepository()
        val first = NotesViewModel(repository, state)
        val dates = NoteFilters(sort = NoteSort.OLDEST, period = DatePeriod.CUSTOM, dateField = DateField.CREATED,
            start = java.time.LocalDate.of(2020, 1, 1), end = java.time.LocalDate.of(2020, 1, 31))
        first.setFilters(dates)
        first.selectTag("tag")
        first.onQueryChanged("search")
        val recreated = NotesViewModel(repository, state)
        assertEquals(dates, recreated.filters.value)
        recreated.onDraftChanged("New thought")
        recreated.completeNote()
        advanceUntilIdle()
        assertEquals(NoteFilters(), recreated.filters.value)
        assertEquals(null, recreated.selectedTag.value)
        assertEquals("", state.get<String>("notes_query"))
    }

    @Test
    fun `frequent tags count notes across entire notebook and update on deletion`() = runTest {
        val a = app.notetaker.model.Tag("a", "Alpha", 1)
        val b = app.notetaker.model.Tag("b", "Beta", 1)
        val one = Note("1", "First", 1, 1, listOf(a, b))
        val two = Note("2", "Second", 2, 2, listOf(b))
        val repository = FakeNoteRepository(listOf(one, two))
        val viewModel = NotesViewModel(repository, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.frequentTags.collect() }
        viewModel.onQueryChanged("missing")
        viewModel.selectTag(a.id)
        advanceUntilIdle()
        assertEquals(listOf(FrequentTag(b, 2), FrequentTag(a, 1)), viewModel.frequentTags.value)
        repository.delete(two)
        advanceUntilIdle()
        assertEquals(listOf(FrequentTag(a, 1), FrequentTag(b, 1)), viewModel.frequentTags.value)
    }

    @Test
    fun `date tag and text filters combine`() = runTest {
        val clock = java.time.Clock.fixed(java.time.Instant.parse("2026-09-09T12:00:00Z"), java.time.ZoneId.of("Europe/Prague"))
        val tag = app.notetaker.model.Tag("work", "Work", 1)
        val repository = FakeNoteRepository(listOf(
            Note("a", "Budget", 1, clock.millis(), listOf(tag)),
            Note("b", "Budget", 1, 1, listOf(tag)),
            Note("c", "Budget", 1, clock.millis()),
            Note("d", "Other", 1, clock.millis(), listOf(tag)),
        ))
        val viewModel = NotesViewModel(repository, SavedStateHandle(), clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        viewModel.setFilters(NoteFilters(period = DatePeriod.TODAY))
        viewModel.selectTag(tag.id)
        viewModel.onQueryChanged("Budget")
        advanceUntilIdle()
        assertEquals(listOf("a"), viewModel.uiState.value.notes.map(Note::id))
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
