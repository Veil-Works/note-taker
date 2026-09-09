package app.notetaker.feature.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.notetaker.data.NoteRepository
import app.notetaker.model.Note
import app.notetaker.model.Tag
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class EditorUiState(
    val content: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isExistingNote: Boolean = false,
    val isMissing: Boolean = false,
    val errorMessage: String? = null,
    val tags: List<Tag> = emptyList(),
    val noteId: String? = null,
    val isLinking: Boolean = false,
)

class EditorViewModel(
    private val repository: NoteRepository,
    private val requestedNoteId: String?,
    private val savedStateHandle: SavedStateHandle,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    private val restoredContent: String? = savedStateHandle[CONTENT_KEY]
    private val restoredNoteId: String? = savedStateHandle[NOTE_ID_KEY]
    private val _uiState = MutableStateFlow(
        EditorUiState(
            content = restoredContent.orEmpty(),
            isLoading = requestedNoteId != null || restoredNoteId != null,
        ),
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()
    val aliases = repository.observeAliases().catch {
        _uiState.value = _uiState.value.copy(errorMessage = "Could not load keywords.")
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val suggestions = uiState.map { it.noteId }.distinctUntilChanged().flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeSuggestions(id)
    }.catch {
        _uiState.value = _uiState.value.copy(errorMessage = "Could not load suggestions.")
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveAlias(tagId: String, aliasId: String?, text: String, onSaved: () -> Unit) = keywordAction {
        repository.saveAlias(tagId, aliasId, text)
        onSaved()
    }

    fun deleteAlias(id: String) = keywordAction { repository.deleteAlias(id) }

    fun dismissSuggestion(tag: Tag) {
        val note = persistedNote ?: return
        keywordAction { repository.dismissSuggestion(note.id, tag.id, note.content) }
    }

    fun acceptSuggestion(tag: Tag) {
        if (_uiState.value.tags.none { it.id == tag.id }) {
            toggleTag(tag)
            flush()
        }
    }

    private fun keywordAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action() }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Could not update suggestions.")
            }
        }
    }

    val availableTags = repository.observeTags().catch {
        _uiState.value = _uiState.value.copy(errorMessage = "Could not load tags.")
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableNotes = flow { emitAll(repository.observeNotes()) }.catch {
        _uiState.value = _uiState.value.copy(errorMessage = "Could not load linked notes.")
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setLinked(targetId: String, linked: Boolean) {
        val sourceId = _uiState.value.noteId ?: return
        if (_uiState.value.isLinking) return
        _uiState.value = _uiState.value.copy(isLinking = true, errorMessage = null)
        viewModelScope.launch {
            try {
                if (linked) repository.linkNotes(sourceId, targetId)
                else repository.unlinkNotes(sourceId, targetId)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Could not update connection. Try again.")
            } finally {
                _uiState.value = _uiState.value.copy(isLinking = false)
            }
        }
    }

    fun toggleTag(tag: Tag) {
        if (!_uiState.value.isExistingNote) return
        val selected = _uiState.value.tags
        val tags = if (selected.any { it.id == tag.id }) selected.filterNot { it.id == tag.id } else selected + tag
        savedStateHandle["editor_tag_ids"] = ArrayList(tags.map { it.id })
        _uiState.value = _uiState.value.copy(tags = tags)
        scheduleAutoSave()
    }

    fun createTag(name: String, onCreated: () -> Unit) {
        viewModelScope.launch {
            try {
                val tag = repository.createTag(name)
                if (_uiState.value.tags.none { it.id == tag.id }) toggleTag(tag)
                onCreated()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Could not create tag.")
            }
        }
    }

    private var persistedNote: Note? = null
    private var autoSaveJob: Job? = null
    private val saveMutex = Mutex()

    init {
        val noteIdToLoad = restoredNoteId ?: requestedNoteId
        if (noteIdToLoad == null) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (!restoredContent.isNullOrBlank()) scheduleAutoSave()
        } else {
            loadExistingNote(noteIdToLoad)
        }
    }

    private fun loadExistingNote(noteId: String) {
        viewModelScope.launch {
            runCatching { repository.getNote(noteId) }
                .onSuccess { note ->
                    if (note == null) {
                        _uiState.value = _uiState.value.copy(isLoading = false, isMissing = true)
                    } else {
                        persistedNote = note
                        savedStateHandle[NOTE_ID_KEY] = note.id
                        savedStateHandle[CREATED_AT_KEY] = note.createdAt
                        val content = restoredContent ?: note.content
                        val savedTagIds = savedStateHandle.get<ArrayList<String>>("editor_tag_ids")
                        val tags = if (savedTagIds == null) note.tags else repository.observeTags().first().filter { it.id in savedTagIds }
                        savedStateHandle[CONTENT_KEY] = content
                        _uiState.value = _uiState.value.copy(
                            content = content,
                            isLoading = false,
                            isExistingNote = true,
                            tags = tags,
                            noteId = note.id,
                        )
                        if (content != note.content || tags != note.tags) scheduleAutoSave()
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Could not open this note.",
                    )
                }
        }
    }

    fun onContentChanged(content: String) {
        if (_uiState.value.isLoading || _uiState.value.isMissing) return
        savedStateHandle[CONTENT_KEY] = content
        _uiState.value = _uiState.value.copy(content = content, errorMessage = null)
        scheduleAutoSave()
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DELAY_MS)
            saveAndReport()
        }
    }

    fun flush() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch { saveAndReport() }
    }

    fun close(onClosed: () -> Unit) {
        if (_uiState.value.isLinking) return
        autoSaveJob?.cancel()
        viewModelScope.launch {
            if (saveAndReport()) onClosed()
        }
    }

    fun delete(onDeleted: (Note?) -> Unit) {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            try {
                val saved = persistCurrentContent()
                val latest = saved?.let { repository.getNote(it.id) }
                if (latest != null) repository.delete(latest)
                onDeleted(latest)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Could not delete this note.",
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private suspend fun saveAndReport(): Boolean {
        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
        return try {
            persistCurrentContent()
            _uiState.value = _uiState.value.copy(isSaving = false)
            true
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                errorMessage = "Could not save changes.",
            )
            false
        }
    }

    private suspend fun persistCurrentContent(): Note? = saveMutex.withLock {
        val content = _uiState.value.content
        val current = persistedNote

        if (current == null && content.isBlank()) return@withLock null
        val tags = _uiState.value.tags
        if (current?.content == content && current.tags == tags) return@withLock current

        val timestamp = now()
        val updatedNote = current?.copy(content = content, updatedAt = timestamp, tags = tags)
            ?: Note(
                id = newId(),
                content = content,
                createdAt = timestamp,
                updatedAt = timestamp,
            )

        repository.upsert(updatedNote)
        persistedNote = updatedNote
        savedStateHandle[NOTE_ID_KEY] = updatedNote.id
        savedStateHandle[CREATED_AT_KEY] = updatedNote.createdAt
        _uiState.value = _uiState.value.copy(isExistingNote = true, noteId = updatedNote.id)
        updatedNote
    }

    companion object {
        private const val CONTENT_KEY = "editor_content"
        private const val NOTE_ID_KEY = "editor_note_id"
        private const val CREATED_AT_KEY = "editor_created_at"
        private const val AUTO_SAVE_DELAY_MS = 600L

        fun factory(
            repository: NoteRepository,
            noteId: String?,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                EditorViewModel(repository, noteId, createSavedStateHandle())
            }
        }
    }
}
