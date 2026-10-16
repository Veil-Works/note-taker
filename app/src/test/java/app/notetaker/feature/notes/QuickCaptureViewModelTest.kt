package app.notetaker.feature.notes

import androidx.lifecycle.SavedStateHandle
import app.notetaker.FakeNoteRepository
import app.notetaker.MainDispatcherRule
import app.notetaker.data.NoteRepository
import app.notetaker.model.Note
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuickCaptureViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    @Test fun `literal standalone text autosaves without metadata and keeps one identity`() = runTest {
        val repo = FakeNoteRepository()
        val vm = QuickCaptureViewModel(repo, SavedStateHandle(), { 10 }, { "one" })
        vm.onContentChanged("#library user: me password: example")
        advanceTimeBy(499)
        assertTrue(repo.savedNotes.isEmpty())
        advanceUntilIdle()
        vm.onContentChanged("#library user: me password: updated")
        advanceUntilIdle()
        val saved = repo.observeNotes().first().single()
        assertEquals("one", saved.id)
        assertEquals("#library user: me password: updated", saved.content)
        assertTrue(saved.tags.isEmpty())
        assertEquals(10L, saved.createdAt)
        assertFalse(vm.state.value.dirty)
    }

    @Test fun `finish flushes immediately and next thought has a different identity`() = runTest {
        val repo = FakeNoteRepository()
        var id = 0
        val vm = QuickCaptureViewModel(repo, SavedStateHandle(), { 10 }, { (++id).toString() })
        vm.onContentChanged("First")
        var closed = false
        vm.finish { closed = true }
        advanceUntilIdle()
        assertTrue(closed)
        assertEquals("", vm.state.value.content)
        vm.onContentChanged("Second")
        vm.finish()
        advanceUntilIdle()
        assertEquals(setOf("1", "2"), repo.observeNotes().first().map { it.id }.toSet())
    }

    @Test fun `failed finish retains text and retry does not duplicate the thought`() = runTest {
        val repo = FakeNoteRepository()
        val vm = QuickCaptureViewModel(repo, SavedStateHandle(), { 10 }, { "one" })
        repo.failure = IllegalStateException("disk full")
        vm.onContentChanged("Keep this")
        var navigated = false
        vm.finish { navigated = true }
        advanceUntilIdle()
        assertFalse(navigated)
        assertEquals("Keep this", vm.state.value.content)
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.finishing)
        repo.failure = null
        vm.retry()
        advanceUntilIdle()
        vm.finish { navigated = true }
        advanceUntilIdle()
        assertTrue(navigated)
        assertEquals("one", repo.observeNotes().first().single().id)
    }

    @Test fun `saved-state restoration uses the same persisted identity and creation date`() = runTest {
        val repo = FakeNoteRepository(listOf(Note("one", "Earlier", 3, 4)))
        val handle = SavedStateHandle(mapOf("quick_id" to "one", "quick_content" to "Restored edit", "quick_created" to 3L))
        val vm = QuickCaptureViewModel(repo, handle, { 10 }, { error("Must not allocate another ID") })
        advanceUntilIdle()
        val note = repo.observeNotes().first().single()
        assertEquals("Restored edit", note.content)
        assertEquals(3L, note.createdAt)
        vm.finish()
        advanceUntilIdle()
        assertNull(handle.get<String>("quick_id"))
    }

    @Test fun `erasing an autosaved active thought removes it but not other notes`() = runTest {
        val repo = FakeNoteRepository(listOf(Note("other", "Unrelated", 1, 1)))
        val vm = QuickCaptureViewModel(repo, SavedStateHandle(), { 10 }, { "one" })
        vm.onContentChanged("Temporary")
        advanceUntilIdle()
        vm.onContentChanged("")
        vm.finish()
        advanceUntilIdle()
        assertEquals(listOf("other"), repo.observeNotes().first().map { it.id })
    }

    @Test fun `typing while a write is in flight retains the latest text`() = runTest {
        val base = FakeNoteRepository()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var first = true
        val repo = object : NoteRepository by base {
            override suspend fun upsert(note: Note) {
                if (first) {
                    first = false
                    withContext(NonCancellable) { entered.complete(Unit); release.await(); base.upsert(note) }
                } else base.upsert(note)
            }
        }
        val vm = QuickCaptureViewModel(repo, SavedStateHandle(), { 10 }, { "one" })
        vm.onContentChanged("Earlier")
        advanceTimeBy(501)
        entered.await()
        vm.onContentChanged("Latest")
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals("Latest", base.observeNotes().first().single().content)
        assertFalse(vm.state.value.dirty)
    }
}
