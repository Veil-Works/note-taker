package app.notetaker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.notetaker.data.NoteRepository
import app.notetaker.model.*
import kotlinx.coroutines.flow.*

data class NotebookState(val notes: List<Note> = emptyList(), val edges: List<GraphEdge> = emptyList(), val loading: Boolean = true, val error: String? = null)
fun graphEdges(notes: List<Note>, automatic: List<GraphEdge>): List<GraphEdge> {
    val valid = notes.map { it.id }.toSet()
    val manual = notes.flatMap { n -> n.linkedNoteIds.map { GraphEdge(minOf(n.id, it), maxOf(n.id, it), 1.0, true) } }
    return (automatic + manual).filter { it.source != it.target && it.source in valid && it.target in valid }
        .groupBy { minOf(it.source, it.target) to maxOf(it.source, it.target) }
        .map { (pair, matches) -> GraphEdge(pair.first, pair.second, matches.maxOf { it.strength }, matches.any { it.manual }) }
}
class NotebookViewModel(repository: NoteRepository) : ViewModel() {
    val state = combine(repository.observeNotes(), repository.observeGraphEdges()) { notes, edges -> NotebookState(notes, graphEdges(notes, edges), false) }
        .catch { emit(NotebookState(loading = false, error = "Could not load the notebook. Reopen this screen to retry.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotebookState())
    companion object { fun factory(repository: NoteRepository) = viewModelFactory { initializer { NotebookViewModel(repository) } } }
}
