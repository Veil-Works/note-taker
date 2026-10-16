package app.notetaker.feature.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.notetaker.data.NoteRepository
import app.notetaker.model.Note
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class QuickCaptureState(
    val content: String = "",
    val noteId: String? = null,
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val finishing: Boolean = false,
    val error: String? = null,
)

/** One identity per thought, even across autosaves, retries and saved-state restoration. */
class QuickCaptureViewModel(
    private val repository: NoteRepository,
    private val savedState: SavedStateHandle,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    private val _state = MutableStateFlow(QuickCaptureState(
        content = savedState["quick_content"] ?: "",
        noteId = savedState["quick_id"],
        dirty = savedState.get<String>("quick_id") != null,
    ))
    val state: StateFlow<QuickCaptureState> = _state
    private val mutex = Mutex()
    private var debounce: Job? = null

    init { if (_state.value.noteId != null) scheduleSave() }

    fun onContentChanged(content: String) {
        val old = _state.value
        if (old.finishing || content == old.content) return
        val id = old.noteId ?: if (content.isNotBlank()) newId() else null
        if (old.noteId == null && id != null) savedState["quick_created"] = now()
        savedState["quick_id"] = id
        savedState["quick_content"] = content
        _state.value = old.copy(content = content, noteId = id, dirty = id != null, error = null)
        scheduleSave()
    }

    private fun scheduleSave() {
        debounce?.cancel()
        debounce = viewModelScope.launch { delay(500); persist() }
    }

    fun retry() { debounce?.cancel(); debounce = viewModelScope.launch { persist() } }

    /** Called on Done, leaving the feed, or backgrounding. Errors retain the draft. */
    fun finish(onFinished: () -> Unit = {}) {
        if (_state.value.finishing) return
        _state.value = _state.value.copy(finishing = true)
        debounce?.cancel()
        viewModelScope.launch {
            val saved = persist()
            if (saved) {
                savedState["quick_id"] = null
                savedState["quick_content"] = ""
                savedState["quick_created"] = null
                _state.value = QuickCaptureState()
                onFinished()
            } else _state.value = _state.value.copy(finishing = false)
        }
    }

    private suspend fun persist(): Boolean = mutex.withLock {
        val snapshot = _state.value
        val id = snapshot.noteId ?: return@withLock true
        if (!snapshot.dirty) return@withLock true
        _state.value = snapshot.copy(saving = true)
        try {
            val existing = repository.getNote(id)
            when {
                snapshot.content.isBlank() -> if (existing != null) repository.delete(existing)
                existing != null -> repository.saveEdit(existing.copy(content = snapshot.content, updatedAt = now()), emptyMap())
                else -> {
                    val created = savedState.get<Long>("quick_created") ?: now()
                    // Plain text is literal: credentials and leading # characters are not commands.
                    repository.upsert(Note(id, snapshot.content, created, now()))
                }
            }
            _state.value = _state.value.copy(
                dirty = _state.value.content != snapshot.content,
                saving = false,
                error = null,
            )
            true
        } catch (cancelled: CancellationException) {
            _state.value = _state.value.copy(saving = false)
            throw cancelled
        } catch (_: Exception) {
            _state.value = _state.value.copy(saving = false, error = "Could not save. Your text is still here.")
            false
        }
    }

    companion object {
        fun factory(repository: NoteRepository) = viewModelFactory {
            initializer { QuickCaptureViewModel(repository, createSavedStateHandle()) }
        }
    }
}
