package app.notetaker

import android.os.Bundle
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.notetaker.ui.*
import app.notetaker.ui.theme.glassPanel
import app.notetaker.ui.theme.notebookBackdrop
import androidx.compose.ui.graphics.Color
import app.notetaker.feature.graph.GraphScreen
import app.notetaker.feature.settings.SettingsScreen
import app.notetaker.feature.concepts.*
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
            NoteTakerApp(repository)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteTakerApp(repository: NoteRepository) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("appearance", android.content.Context.MODE_PRIVATE) }
    var mode by remember { mutableStateOf(preferences.getString("mode", "System") ?: "System") }
    var compact by remember { mutableStateOf(preferences.getBoolean("compact", false)) }
    var capturing by rememberSaveable { mutableStateOf(false) }
    val notesViewModel: NotesViewModel = viewModel(factory = NotesViewModel.factory(repository))
    val concepts: ConceptsViewModel = viewModel(factory = ConceptsViewModel.factory(repository))
    val notebook: NotebookViewModel = viewModel(factory = NotebookViewModel.factory(repository))
    val notebookState by notebook.state.collectAsStateWithLifecycle()
    val topics by concepts.topics.collectAsStateWithLifecycle()
    val status by concepts.organizationStatus.collectAsStateWithLifecycle()
    val actionError by concepts.error.collectAsStateWithLifecycle()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty()
    fun navigate(destination: String) { navController.navigate(destination) {
        popUpTo(NOTES_ROUTE) { saveState = true }
        launchSingleTop = true
        restoreState = true
    } }
    var active by rememberSaveable { mutableStateOf("notes") }
    val destination = when { route == "graph" -> "graph"; route == "settings" -> "settings"; route.startsWith("concepts") || route == "topics" || route == "connections" -> "connections"; else -> "notes" }
    LaunchedEffect(route) { if (!route.startsWith("editor")) active = destination }
    NoteTakerTheme(mode) {
    Scaffold(modifier = Modifier.notebookBackdrop(), containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground, contentWindowInsets = WindowInsets(0),
        bottomBar = { NotebookNavigation(active, ::navigate, { capturing = true }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { shellPadding ->
        Box(Modifier.fillMaxSize().padding(shellPadding).consumeWindowInsets(shellPadding)) {
        NavHost(
            navController = navController,
            startDestination = NOTES_ROUTE,
            enterTransition = { fadeIn(tween(180)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition = { fadeOut(tween(120)) },
        ) {
            composable(NOTES_ROUTE) {
                NotesScreen(
                    viewModel = notesViewModel,
                    compact = compact,
                    onOpenNote = { id -> navController.navigate("$EDITOR_ROUTE?noteId=$id") },
                    onOpenTopics = { navController.navigate("topics") },
                    onOpenConcept = { id -> navController.navigate("concepts?tagId=${id.orEmpty()}") },
                )
            }

            composable("graph") {
                GraphScreen(notebookState.notes, notebookState.edges, topics, notebookState.loading, notebookState.error, status,
                    onOpen = { navController.navigate("$EDITOR_ROUTE?noteId=$it") })
            }
            composable("connections") {
                ConceptsScreen(concepts, null, onBack = { navigate("notes") },
                    onOpenNote = { navController.navigate("$EDITOR_ROUTE?noteId=$it") },
                    onTopics = { navController.navigate("topics") }, root = true)
            }
            composable("settings") {
                SettingsScreen(mode, { mode = it; preferences.edit().putString("mode", it).apply() }, compact,
                    { compact = it; preferences.edit().putBoolean("compact", it).apply() }, status, concepts::retryOrganization, actionError)
            }

            composable("topics") {
                val topicsViewModel: app.notetaker.feature.concepts.ConceptsViewModel = viewModel(factory = app.notetaker.feature.concepts.ConceptsViewModel.factory(repository))
                app.notetaker.feature.concepts.TopicsScreen(topicsViewModel, onBack = { navController.popBackStack() },
                    onOpenNote = { id -> navController.navigate("$EDITOR_ROUTE?noteId=$id") })
            }
            composable("concepts?tagId={tagId}", arguments = listOf(navArgument("tagId") { defaultValue = "" })) { entry ->
                val conceptsViewModel: app.notetaker.feature.concepts.ConceptsViewModel = viewModel(factory = app.notetaker.feature.concepts.ConceptsViewModel.factory(repository))
                app.notetaker.feature.concepts.ConceptsScreen(conceptsViewModel, entry.arguments?.getString("tagId")?.ifBlank { null },
                    onBack = { navController.popBackStack() },
                    onOpenNote = { id -> navController.navigate("$EDITOR_ROUTE?noteId=$id") })
            }

            dialog(
                route = "$EDITOR_ROUTE?noteId={noteId}",
                dialogProperties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
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
                NoteWindowBackdrop(onDismiss = { editorViewModel.discard { navController.popBackStack() } }) {
                EditorScreen(
                    viewModel = editorViewModel,
                    isNewNote = noteId == null,
                    onClose = { navController.popBackStack() },
                    onOpenConcept = { id ->
                        navController.popBackStack()
                        navController.navigate("concepts?tagId=${id.orEmpty()}")
                    },
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
        }

        }
    }
    if (capturing) {
        val draft by notesViewModel.draft.collectAsStateWithLifecycle()
        val saving by notesViewModel.saving.collectAsStateWithLifecycle()
        val error by notesViewModel.captureError.collectAsStateWithLifecycle()
        var submitted by remember { mutableStateOf(false) }
        LaunchedEffect(draft, saving, submitted) { if (submitted && draft.isEmpty() && !saving) { capturing = false; submitted = false; navigate("notes") } }
        val captureFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { captureFocus.requestFocus() }
        Dialog(onDismissRequest = { if (!saving) { notesViewModel.discardDraft(); capturing = false } }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            NoteWindowBackdrop(onDismiss = { if (!saving) { notesViewModel.discardDraft(); capturing = false } }) {
            Surface(modifier = Modifier.glassPanel(28.dp, opaque = true), shape = MaterialTheme.shapes.extraLarge, color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface) {
                Column(Modifier.heightIn(max = 560.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("A new thought", style = MaterialTheme.typography.titleLarge)
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("One thought. Your own words.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(draft, notesViewModel::onDraftChanged, modifier = Modifier.fillMaxWidth().focusRequester(captureFocus).testTag("capture_field"),
                            placeholder = { Text("Write a note…") }, minLines = 4, maxLines = 8, readOnly = saving, shape = MaterialTheme.shapes.large)
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(enabled = !saving, onClick = { notesViewModel.discardDraft(); capturing = false }) { Text("Close") }
                        Button(enabled = draft.isNotBlank() && !saving, onClick = { submitted = true; notesViewModel.completeNote() }, modifier = Modifier.testTag("complete_note")) {
                            Text(if (saving) "Saving…" else "Save note")
                        }
                    }
                }
            }
            }
        }
    }
    }
}

private const val NOTES_ROUTE = "notes"
private const val EDITOR_ROUTE = "editor"
