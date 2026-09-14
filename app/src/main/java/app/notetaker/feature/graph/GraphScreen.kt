package app.notetaker.feature.graph

import app.notetaker.ui.countLabel
import app.notetaker.ui.theme.glassPanel
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.notetaker.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GraphScreen(notes: List<Note>, edges: List<GraphEdge>, topics: List<Topic>, loading: Boolean,
    error: String?, status: OrganizationStatus, onOpen: (String) -> Unit) {
    val shortWindow = LocalConfiguration.current.screenHeightDp < 480
    var threshold by rememberSaveable { mutableFloatStateOf(0.38f) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var browsing by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val visible = notes
    val ids = remember(visible) { visible.map { it.id }.sorted() }
    val idSet = remember(ids) { ids.toSet() }
    val shownEdges = remember(edges, idSet, threshold) { edges.filter { it.source in idSet && it.target in idSet && (it.manual || it.strength >= threshold) } }
    val layout by produceState<Map<String, GraphPoint>>(emptyMap(), ids, edges) {
        value = withContext(Dispatchers.Default) { graphLayout(ids, edges.filter { it.source in idSet && it.target in idSet }) }
    }
    val selectedNote = visible.find { it.id == selected }
    val primary = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(if (shortWindow) 4.dp else 20.dp))
        if (!shortWindow) Text("THE BIG PICTURE", style = MaterialTheme.typography.labelSmall, color = primary)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Your net", style = if (shortWindow) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineLarge)
                if (shortWindow) Text("${countLabel(visible.size, "note")} · ${countLabel(shownEdges.size, "connection")}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { browsing = !browsing }) { Text(if (browsing) "Show graph" else "Browse notes") }
        }
        if (!shortWindow) Text("${countLabel(visible.size, "note")} · ${countLabel(shownEdges.size, "connection")}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (status.pending) Text("Updating connections…", style = MaterialTheme.typography.labelSmall)
        (error ?: status.error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Strength", style = MaterialTheme.typography.labelMedium)
            Slider(value = threshold, onValueChange = { threshold = it }, valueRange = 0.38f..0.85f,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp).semantics { contentDescription = "Minimum connection strength" })
            Text("${(threshold * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        if (browsing) {
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Find a thought…") }, singleLine = true, shape = RoundedCornerShape(20.dp))
            val matches = visible.filter { it.content.contains(query.trim(), true) }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 12.dp)) {
                if (matches.isEmpty()) item { Text("No notes match your search.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
                items(matches, key = { it.id }) { note ->
                    TextButton(onClick = { selected = note.id; browsing = false; zoom = 1f; pan = Offset.Zero }, modifier = Modifier.fillMaxWidth()) {
                        Text(note.preview(), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth().testTag("graph_view")) {
                if (notes.isEmpty() && !loading) Text("A little empty, for now.\nAdd a note to start your net.", Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Canvas(Modifier.fillMaxSize().clipToBounds()
                    .semantics { contentDescription = "Note graph. Pinch to zoom, drag to pan. Use Browse notes to select a note." }
                    .pointerInput(Unit) { detectTransformGestures { centroid, delta, scale, _ ->
                        val old = zoom; zoom = (zoom * scale).coerceIn(0.5f, 4f)
                        val center = Offset(size.width / 2f, size.height / 2f)
                        pan = (pan - (centroid - center)) * (zoom / old) + (centroid - center) + delta
                    } }
                    .pointerInput(layout, zoom, pan) { detectTapGestures { tap ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        selected = layout.entries.minByOrNull { (_, point) ->
                            ((Offset(point.x * size.width, point.y * size.height) - center) * zoom + center + pan - tap).getDistance()
                        }?.takeIf { (_, point) ->
                            ((Offset(point.x * size.width, point.y * size.height) - center) * zoom + center + pan - tap).getDistance() <= 28.dp.toPx()
                        }?.key
                    } }) {
                    fun point(id: String): Offset? = layout[id]?.let { (Offset(it.x * size.width, it.y * size.height) - center) * zoom + center + pan }
                    shownEdges.forEach { edge ->
                        val a = point(edge.source); val b = point(edge.target)
                        if (a != null && b != null) {
                            val highlighted = selected == edge.source || selected == edge.target
                            val alpha = if (selected != null && !highlighted) 0.06f else (0.10 + edge.strength * 0.42).toFloat()
                            val width = ((if (highlighted) 1.1f else 0.20f) + edge.strength.toFloat() * 0.6f) * density
                            drawLine(primary.copy(alpha = alpha), a, b, strokeWidth = width,
                                pathEffect = if (edge.manual) PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 4.dp.toPx())) else null)

                        }
                    }
                    visible.forEach { note -> point(note.id)?.let { p ->
                        val active = note.id == selected
                        val adjacent = selected == null || active || shownEdges.any { (it.source == selected && it.target == note.id) || (it.target == selected && it.source == note.id) }
                        val opacity = if (adjacent) 1f else .35f
                        val radius = (if (active) 7f else 3.5f).dp.toPx()
                        if (active) drawCircle(primary.copy(alpha = .12f), radius * 1.8f, p)
                        drawCircle(Brush.linearGradient(listOf(Color(0xFF67839A).copy(alpha = opacity), Color(0xFF344D63).copy(alpha = opacity)),
                            start = p - Offset(radius, radius), end = p + Offset(radius, radius)), radius, p)
                        drawCircle(primary.copy(alpha = if (active) .9f else .35f * opacity), radius, p, style = Stroke((if (active) 1.2f else .5f).dp.toPx()))

                    } }
                    if (selected == null) {
                        val occupied = mutableListOf<android.graphics.RectF>()
                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            textSize = 11 * density * fontScale; typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                        }
                        topics.sortedByDescending { it.noteIds.size }.forEach { topic ->
                            val points = topic.noteIds.mapNotNull(::point)
                            if (points.size >= 2) {
                                val at = Offset(points.map { it.x }.average().toFloat(), points.map { it.y }.average().toFloat())
                                val fullLabel = topic.name.replaceFirstChar { it.titlecase() }
                                var label = fullLabel
                                val maxWidth = (size.width - 28.dp.toPx()).coerceAtLeast(1f)
                                while (label.length > 1 && paint.measureText(label) > maxWidth) {
                                    label = label.removeSuffix("…").dropLast(1) + "…"
                                }
                                val width = paint.measureText(label)
                                val padding = 7.dp.toPx()
                                val left = (at.x - width / 2 - padding).coerceIn(0f, (size.width - width - padding * 2).coerceAtLeast(0f))
                                val rect = listOf(-23f, 1f, -47f, 25f).map { offset ->
                                    val top = (at.y + offset.dp.toPx()).coerceIn(0f, (size.height - (paint.fontSpacing + 8.dp.toPx())).coerceAtLeast(0f))
                                    android.graphics.RectF(left, top, left + width + padding * 2, top + (paint.fontSpacing + 8.dp.toPx()))
                                }.firstOrNull { candidate -> occupied.none { android.graphics.RectF.intersects(it, candidate) } }
                                if (at.x in 0f..size.width && at.y in 0f..size.height && rect != null) {
                                    occupied += rect
                                    drawIntoCanvas { canvas ->
                                        paint.color = surface.copy(alpha = .92f).toArgb()
                                        canvas.nativeCanvas.drawRoundRect(rect, 6.dp.toPx(), 6.dp.toPx(), paint)
                                        paint.color = textColor.toArgb()
                                        canvas.nativeCanvas.drawText(label, rect.left + padding, rect.top + 4.dp.toPx() - paint.ascent(), paint)
                                    }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.align(Alignment.BottomEnd), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalIconButton(onClick = { zoom = (zoom / 1.3f).coerceAtLeast(0.5f) }, modifier = Modifier.semantics { contentDescription = "Zoom out" }) { Text("−") }
                    FilledTonalIconButton(onClick = { zoom = (zoom * 1.3f).coerceAtMost(4f) }, modifier = Modifier.semantics { contentDescription = "Zoom in" }) { Text("+") }
                    FilledTonalButton(onClick = { zoom = 1f; pan = Offset.Zero }) { Text("Fit") }
                }
            }
        }
        if (selectedNote != null) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), onClick = { onOpen(selectedNote.id) }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).glassPanel(20.dp, highlighted = true), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(if (shortWindow) 8.dp else 16.dp)) {
                    Text(selectedNote.preview(), maxLines = if (shortWindow) 1 else 3, overflow = TextOverflow.Ellipsis)
                    Text("${countLabel(shownEdges.count { it.source == selectedNote.id || it.target == selectedNote.id }, "connection")} · Tap to open", style = MaterialTheme.typography.labelSmall, color = primary)
                }
            }
        } else if (!browsing && !shortWindow) Text("Tap a dot to explore a thought", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
    }
}
