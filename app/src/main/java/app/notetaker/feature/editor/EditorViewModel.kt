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
    val automaticTagReasons: Map<String, String> = emptyMap(),
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
    val organizationStatus = repository.observeOrganizationStatus().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), app.notetaker.model.OrganizationStatus())
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val related = uiState.map { it.noteId }.distinctUntilChanged().flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeRelated(id)
    }.catch { emit(emptyList()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun retryOrganization() = keywordAction { repository.retryOrganization() }
    fun keepAutomaticTag(tag: Tag) = keywordAction {
        val id = _uiState.value.noteId ?: return@keywordAction
        repository.tagNotes(setOf(id), setOf(tag.id))
    }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val suggestions = uiState.map { it.noteId }.distinctUntilChanged().flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeSuggestions(id)
    }.catch {
        _uiState.value = _uiState.value.copy(errorMessage = "Could not load suggestions.")
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
        _uiState.value = _uiState.value.copy(errorMessage = "Could not load topics.")
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableNotes = flow { emitAll(repository.observeNotes()) }.catch {
        _uiState.value = _uiState.value.copy(errorMessage = "Could not load linked notes.")
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setLinked(targetId: String, linked: Boolean) {
        edited = true
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

    private fun tagChanges(): Map<String, Boolean> =
        savedStateHandle.get<ArrayList<String>>("pending_tag_adds").orEmpty().associateWith { true } +
            savedStateHandle.get<ArrayList<String>>("pending_tag_removes").orEmpty().associateWith { false }
    private fun storeTagChanges(changes: Map<String, Boolean>) {
        savedStateHandle["pending_tag_adds"] = ArrayList(changes.filterValues { it }.keys)
        savedStateHandle["pending_tag_removes"] = ArrayList(changes.filterValues { !it }.keys)
    }
    fun toggleTag(tag: Tag) {
        edited = true
        if (!_uiState.value.isExistingNote) return
        val selected = _uiState.value.tags
        val tags = if (selected.any { it.id == tag.id }) selected.filterNot { it.id == tag.id } else selected + tag
        storeTagChanges(tagChanges() + (tag.id to tags.any { it.id == tag.id }))
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
                _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Could not create topic.")
            }
        }
    }

    private var originalNote: Note? = null
    private var discarding = false
    private var edited = false
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
                        originalNote = note
                        persistedNote = note
                        savedStateHandle[NOTE_ID_KEY] = note.id
                        savedStateHandle[CREATED_AT_KEY] = note.createdAt
                        val content = restoredContent ?: note.content
                        val pending = tagChanges()
                        val added = if (pending.values.any { it }) repository.observeTags().first().filter { pending[it.id] == true } else emptyList()
                        val tags = (note.tags.filterNot { pending[it.id] == false } + added).distinctBy { it.id }
                        savedStateHandle[CONTENT_KEY] = content
                        _uiState.value = _uiState.value.copy(
                            content = content,
                            isLoading = false,
                            isExistingNote = true,
                            tags = tags,
                            noteId = note.id,
                            automaticTagReasons = note.automaticTagReasons,
                        )
                        if (content != note.content || tagChanges().isNotEmpty()) scheduleAutoSave()
                        observeOrganization(noteId)
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

    private fun observeOrganization(noteId: String) {
        viewModelScope.launch {
            repository.observeNote(noteId).catch {
                _uiState.value = _uiState.value.copy(errorMessage = "Could not refresh organization.")
                emit(null)
            }.collect { latest ->
                if (latest == null) return@collect
                // Merge organization arrivals with the user's unsaved tag edits.
                val old = persistedNote ?: return@collect
                val changes = tagChanges()
                val added = (_uiState.value.tags + availableTags.value).filter { changes[it.id] == true }
                val merged = (latest.tags.filterNot { changes[it.id] == false } + added).distinctBy { it.id }
                persistedNote = old.copy(tags = latest.tags, automaticTagReasons = latest.automaticTagReasons, rejectedTagIds = latest.rejectedTagIds)
                savedStateHandle["editor_tag_ids"] = ArrayList(merged.map { it.id })
                _uiState.value = _uiState.value.copy(tags = merged, automaticTagReasons = latest.automaticTagReasons)
            }
        }
    }

    fun onContentChanged(content: String) {
        if (_uiState.value.isLoading || _uiState.value.isMissing) return
        edited = true
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

    fun discard(onClosed: () -> Unit) {
        if (discarding || _uiState.value.isLinking || _uiState.value.isLoading) return
        discarding = true
        autoSaveJob?.cancel()
        viewModelScope.launch {
            try {
                saveMutex.withLock {
                    val original = originalNote
                    if (original != null && edited) repository.upsert(original)
                    else if (original == null) persistedNote?.let { repository.delete(it) }
                    persistedNote = original
                    storeTagChanges(emptyMap())
                    savedStateHandle[CONTENT_KEY] = original?.content.orEmpty()
                    _uiState.value = _uiState.value.copy(content = original?.content.orEmpty())
                }
                onClosed()
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) {
                discarding = false
                _uiState.value = _uiState.value.copy(errorMessage = "Could not discard changes. Try again.")
            }
        }
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
        if (discarding) return@withLock persistedNote
        val content = _uiState.value.content
        val current = persistedNote

        if (current == null && content.isBlank()) return@withLock null
        val tags = _uiState.value.tags
        val changes = tagChanges()
        if (current?.content == content && changes.isEmpty()) return@withLock current

        val timestamp = now()
        val updatedNote = current?.copy(content = content, updatedAt = timestamp, tags = tags)
            ?: Note(
                id = newId(),
                content = content,
                createdAt = timestamp,
                updatedAt = timestamp,
            )

        if (current == null) repository.upsert(updatedNote)
        else repository.saveEdit(updatedNote, changes)
        storeTagChanges(tagChanges().filter { (id, value) -> changes[id] != value })
        val latest = repository.getNote(updatedNote.id) ?: updatedNote
        persistedNote = latest
        val pending = tagChanges()
        val mergedTags = (latest.tags.filterNot { pending[it.id] == false } + _uiState.value.tags.filter { pending[it.id] == true }).distinctBy { it.id }
        savedStateHandle["editor_tag_ids"] = ArrayList(mergedTags.map { it.id })
        _uiState.value = _uiState.value.copy(tags = mergedTags, automaticTagReasons = latest.automaticTagReasons)
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
