package app.notetaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.notetaker.data.NoteRepository
import app.notetaker.feature.editor.EditorScreen
import app.notetaker.feature.editor.EditorViewModel
import app.notetaker.feature.notes.NotesScreen
import app.notetaker.feature.notes.NotesViewModel
import app.notetaker.ui.theme.NoteTakerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as NoteTakerApplication).container.noteRepository
        setContent {
            NoteTakerTheme {
                NoteTakerApp(repository)
            }
        }
    }
}

@Composable
internal fun NoteTakerApp(repository: NoteRepository) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = NOTES_ROUTE,
        ) {
            composable(NOTES_ROUTE) {
                val notesViewModel: NotesViewModel = viewModel(
                    factory = NotesViewModel.factory(repository),
                )
                NotesScreen(
                    viewModel = notesViewModel,
                    onOpenNote = { id -> navController.navigate("$EDITOR_ROUTE?noteId=$id") },
                )
            }

            dialog(
                route = "$EDITOR_ROUTE?noteId={noteId}",
                dialogProperties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                arguments = listOf(
                    navArgument("noteId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val noteId = entry.arguments?.getString("noteId")
                val editorViewModel: EditorViewModel = viewModel(
                    key = "editor-${noteId ?: "new"}",
                    factory = EditorViewModel.factory(repository, noteId),
                )
                EditorScreen(
                    viewModel = editorViewModel,
                    isNewNote = noteId == null,
                    onClose = { navController.popBackStack() },
                    onOpenNote = { id ->
                        // Replace the popup so old editor snapshots cannot overwrite later edits.
                        navController.popBackStack()
                        navController.navigate("$EDITOR_ROUTE?noteId=$id")
                    },
                    onDeleted = { deletedNote ->
                        navController.popBackStack()
                        if (deletedNote != null) {
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Note deleted",
                                    actionLabel = "Undo",
                                    withDismissAction = true,
                                )
                                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                    runCatching { repository.upsert(deletedNote) }
                                        .onFailure {
                                            snackbarHostState.showSnackbar("Could not restore note.")
                                        }
                                }
                            }
                        }
                    },
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private const val NOTES_ROUTE = "notes"
private const val EDITOR_ROUTE = "editor"
