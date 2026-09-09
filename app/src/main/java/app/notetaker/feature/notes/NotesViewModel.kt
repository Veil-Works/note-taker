package app.notetaker.feature.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.notetaker.data.NoteRepository
import app.notetaker.model.Note
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

data class NotesUiState(
    val query: String = "",
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

private sealed interface NotesResult {
    data object Loading : NotesResult
    data class Success(val notes: List<Note>) : NotesResult
    data class Error(val message: String) : NotesResult
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class NotesViewModel(
    private val repository: NoteRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val draft = savedStateHandle.getStateFlow("capture_text", "")
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving
    private val _captureError = MutableStateFlow<String?>(null)
    val captureError: StateFlow<String?> = _captureError
    private var pendingNote: Note? = null

    fun onDraftChanged(text: String) {
        if (_saving.value) return
        savedStateHandle["capture_text"] = text
        _captureError.value = null
    }

    fun completeNote() {
        if (_saving.value || draft.value.isBlank()) return
        val timestamp = System.currentTimeMillis()
        val note = pendingNote?.copy(content = draft.value, updatedAt = timestamp)
            ?: Note(UUID.randomUUID().toString(), draft.value, timestamp, timestamp)
        pendingNote = note
        _saving.value = true
        viewModelScope.launch {
            try {
                repository.upsert(note)
                savedStateHandle["capture_text"] = ""
                pendingNote = null
                _captureError.value = null
                onQueryChanged("")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _captureError.value = "Could not save note. Tap Complete note to retry."
            } finally {
                _saving.value = false
            }
        }
    }

    private val query = MutableStateFlow(savedStateHandle[QUERY_KEY] ?: "")

    private val results: Flow<NotesResult> = query
        .debounce { value -> if (value.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
        .flatMapLatest { value ->
            repository.observeNotes(value)
                .map<List<Note>, NotesResult>(NotesResult::Success)
                .onStart { emit(NotesResult.Loading) }
                .catch { emit(NotesResult.Error("Could not load notes.")) }
        }

    val uiState: StateFlow<NotesUiState> = combine(query, results) { currentQuery, result ->
        when (result) {
            NotesResult.Loading -> NotesUiState(query = currentQuery, isLoading = true)
            is NotesResult.Success -> NotesUiState(query = currentQuery, notes = result.notes, isLoading = false)
            is NotesResult.Error -> NotesUiState(
                query = currentQuery,
                isLoading = false,
                errorMessage = result.message,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotesUiState(query = query.value),
    )

    fun onQueryChanged(value: String) {
        savedStateHandle[QUERY_KEY] = value
        query.value = value
    }

    companion object {
        private const val QUERY_KEY = "notes_query"
        private const val SEARCH_DEBOUNCE_MS = 250L

        fun factory(repository: NoteRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                NotesViewModel(repository, createSavedStateHandle())
            }
        }
    }
}
