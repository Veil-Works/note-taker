package app.notetaker.feature.concepts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.notetaker.data.NoteRepository
import app.notetaker.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ConceptsState(
    val tags: List<Tag> = emptyList(), val types: List<EntityType> = emptyList(),
    val aliases: List<TagAlias> = emptyList(), val notes: List<Note> = emptyList(),
    val loading: Boolean = true, val error: String? = null,
)

class ConceptsViewModel(private val repository: NoteRepository) : ViewModel() {
    val topics = repository.observeTopics().catch { emit(emptyList()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val organizationStatus = repository.observeOrganizationStatus().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrganizationStatus())
    fun retryOrganization() = action { repository.retryOrganization() }
    fun renameTopic(id: String, name: String, done: () -> Unit) = action { repository.renameTopic(id, name); done() }
    fun mergeTopics(target: String, source: String, done: () -> Unit) = action { repository.mergeTopics(target, source); done() }
    val error = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val state = combine(repository.observeTags(), repository.observeTypes(), repository.observeAliases(), repository.observeNotes()) { tags, types, aliases, notes ->
        ConceptsState(tags, types, aliases, notes, loading = false)
    }.catch { emit(ConceptsState(loading = false, error = "Could not load concepts. Reopen this page to retry.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConceptsState())

    private fun action(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        error.value = null
        viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (exception: Exception) {
                error.value = if (exception is IllegalArgumentException || exception is IllegalStateException) exception.message
                    else "Could not save. Check for a duplicate name and try again."
            } finally { busy.value = false }
        }
    }
    fun create(name: String, done: (String) -> Unit) = action { done(repository.createTag(name).id) }
    fun save(tag: Tag, name: String, typeId: String?, done: () -> Unit) = action { repository.updateTag(tag.id, name, typeId); done() }
    fun saveType(id: String?, name: String, done: () -> Unit) = action { repository.saveType(id, name); done() }
    fun deleteTag(id: String, done: () -> Unit) = action { repository.deleteTag(id); done() }
    fun deleteType(id: String) = action { repository.deleteType(id) }
    fun saveAlias(tagId: String, id: String?, text: String, done: () -> Unit) = action { repository.saveAlias(tagId, id, text); done() }
    fun deleteAlias(id: String) = action { repository.deleteAlias(id) }
    companion object {
        fun factory(repository: NoteRepository) = viewModelFactory { initializer { ConceptsViewModel(repository) } }
    }
}
