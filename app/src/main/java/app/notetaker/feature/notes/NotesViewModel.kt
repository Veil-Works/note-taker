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
import java.time.Clock
import java.time.LocalDate
import app.notetaker.model.Tag
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
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

data class FrequentTag(val tag: Tag, val count: Int)

private sealed interface NotesResult {
    data object Loading : NotesResult
    data class Success(val notes: List<Note>) : NotesResult
    data class Error(val message: String) : NotesResult
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class NotesViewModel(
    private val repository: NoteRepository,
    private val savedStateHandle: SavedStateHandle,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val _filters = MutableStateFlow(NoteFilters(
        sort = NoteSort.valueOf(savedStateHandle["notes_sort"] ?: NoteSort.UPDATED.name),
        period = DatePeriod.valueOf(savedStateHandle["notes_period"] ?: DatePeriod.ALL.name),
        dateField = DateField.valueOf(savedStateHandle["notes_date_field"] ?: DateField.UPDATED.name),
        start = savedStateHandle.get<Long>("notes_start")?.let(LocalDate::ofEpochDay),
        end = savedStateHandle.get<Long>("notes_end")?.let(LocalDate::ofEpochDay),
    ))
    val filters: StateFlow<NoteFilters> = _filters
    private val today = MutableStateFlow(LocalDate.now(clock))
    fun refreshDates() { today.value = LocalDate.now(clock) }

    fun setFilters(value: NoteFilters) {
        require(value.period != DatePeriod.CUSTOM || (value.start != null && value.end != null && value.start <= value.end))
        savedStateHandle["notes_sort"] = value.sort.name
        savedStateHandle["notes_period"] = value.period.name
        savedStateHandle["notes_date_field"] = value.dateField.name
        savedStateHandle["notes_start"] = value.start?.toEpochDay()
        savedStateHandle["notes_end"] = value.end?.toEpochDay()
        _filters.value = value
        refreshDates()
    }

    fun clearFilters() {
        onQueryChanged("")
        selectTag(null)
        setFilters(NoteFilters())
    }

    val frequentTags = flow { emitAll(repository.observeNotes()) }.map { notes ->
        notes.flatMap { it.tags.distinctBy(Tag::id) }.groupBy(Tag::id).values
            .map { FrequentTag(it.first(), it.size) }
            .sortedWith(compareByDescending<FrequentTag> { it.count }.thenBy { it.tag.name.lowercase(java.util.Locale.ROOT) }.thenBy { it.tag.id })
            .take(5)
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tags = repository.observeTags().catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val selectedTags = savedStateHandle.getStateFlow("selected_tags", arrayListOf<String>())
    fun selectTag(id: String?) {
        savedStateHandle["selected_tags"] = if (id == null) arrayListOf<String>() else arrayListOf(id)
    }
    fun toggleFilterTag(id: String) {
        val current = selectedTags.value
        savedStateHandle["selected_tags"] = ArrayList(if (id in current) current - id else current + id)
    }
    val selectedNotes = savedStateHandle.getStateFlow("selected_notes", arrayListOf<String>())
    private val _bulkBusy = MutableStateFlow(false)
    val bulkBusy: StateFlow<Boolean> = _bulkBusy
    fun toggleNote(id: String) {
        if (_bulkBusy.value) return
        val current = selectedNotes.value
        savedStateHandle["selected_notes"] = ArrayList(if (id in current) current - id else current + id)
    }
    fun clearSelection() { if (!_bulkBusy.value) savedStateHandle["selected_notes"] = arrayListOf<String>() }
    fun selectNotes(ids: List<String>) { if (!_bulkBusy.value) savedStateHandle["selected_notes"] = ArrayList(ids) }
    private fun bulkAction(action: suspend (Set<String>) -> Unit) {
        if (_bulkBusy.value) return
        val ids = selectedNotes.value.toSet()
        _bulkBusy.value = true
        viewModelScope.launch {
            try {
                action(ids)
                savedStateHandle["selected_notes"] = arrayListOf<String>()
                _captureError.value = null
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _captureError.value = error.message ?: "Could not update notes." }
            finally { _bulkBusy.value = false }
        }
    }
    fun deleteSelected() = bulkAction { repository.deleteNotes(it) }
    fun connectSelected() = bulkAction { repository.connectNotes(it) }
    fun tagSelected(tagIds: Set<String>, removeTagIds: Set<String>) = bulkAction { repository.tagNotes(it, tagIds, removeTagIds) }
    fun prepareTagging(onReady: (List<Note>) -> Unit) {
        if (_bulkBusy.value) return
        val ids = selectedNotes.value.toList()
        if (ids.isEmpty()) return
        _bulkBusy.value = true
        viewModelScope.launch {
            try {
                val notes = ids.map { checkNotNull(repository.getNote(it)) { "A selected note no longer exists." } }
                _captureError.value = null
                onReady(notes)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _captureError.value = error.message ?: "Could not load selected tags." }
            finally { _bulkBusy.value = false }
        }
    }
    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            try {
                repository.deleteTag(tag.id)
                savedStateHandle["selected_tags"] = ArrayList(selectedTags.value - tag.id)
                _captureError.value = null
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _captureError.value = error.message ?: "Could not delete tag." }
        }
    }
    fun createTag(name: String, onCreated: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.createTag(name)
                _captureError.value = null
                onCreated()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _captureError.value = error.message ?: "Could not create tag." }
        }
    }
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
        val timestamp = clock.millis()
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
                clearFilters()
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
            flow { emitAll(repository.observeNotes(value)) }
                .map<List<Note>, NotesResult>(NotesResult::Success)
                .onStart { emit(NotesResult.Loading) }
                .catch { emit(NotesResult.Error("Could not load notes.")) }
        }

    val uiState: StateFlow<NotesUiState> = combine(query, results, selectedTags, filters, today) { currentQuery, result, tagIds, currentFilters, currentDay ->
        when (result) {
            NotesResult.Loading -> NotesUiState(query = currentQuery, isLoading = true)
            is NotesResult.Success -> NotesUiState(query = currentQuery, notes = filterAndSortNotes(result.notes, tagIds.toSet(), currentFilters, currentDay, clock.zone), isLoading = false)
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
