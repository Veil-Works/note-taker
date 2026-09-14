package app.notetaker.ui

import androidx.compose.foundation.Canvas
import app.notetaker.ui.theme.glassPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotebookNavigation(selected: String, onNavigate: (String) -> Unit, onNew: () -> Unit) {
    val shortWindow = LocalConfiguration.current.screenHeightDp < 480
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(30.dp), shadowElevation = 0.dp, color = Color.Transparent,
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().glassPanel(30.dp).testTag("floating_navigation")) {
            Row(Modifier.padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("notes" to "List", "graph" to "Net", "new" to "New note", "connections" to "Connections", "settings" to "Settings").forEachIndexed { index, (route, label) ->
                    val active = selected == route
                    Column(Modifier.weight(1f).heightIn(min = if (shortWindow) 48.dp else 66.dp).clip(RoundedCornerShape(20.dp)).semantics { this.selected = active; contentDescription = label }
                        .clickable(role = Role.Tab, onClick = { if (route == "new") onNew() else onNavigate(route) }).testTag("nav_$route"),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Surface(shape = RoundedCornerShape(14.dp),
                            modifier = if (route == "new" || active) Modifier.glassPanel(14.dp, highlighted = true) else Modifier,
                            color = Color.Transparent) {
                            val color by animateColorAsState(if (route == "new" || active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, label = "navigation_tint")
                            Canvas(Modifier.size(width = 42.dp, height = if (shortWindow) 26.dp else 32.dp).padding(7.dp)) {
                                val w = size.width; val h = size.height; val stroke = 1.8.dp.toPx()
                                when (index) {
                                    0 -> repeat(3) { row -> val y = h * (row + 0.5f) / 3; drawCircle(color, 1.5.dp.toPx(), Offset(w * .12f, y)); drawLine(color, Offset(w * .35f, y), Offset(w * .9f, y), stroke) }
                                    1, 3 -> {
                                        val points = if (index == 1) listOf(Offset(w*.2f,h*.2f), Offset(w*.8f,h*.15f), Offset(w*.55f,h*.85f)) else listOf(Offset(w*.2f,h*.5f), Offset(w*.8f,h*.2f), Offset(w*.8f,h*.8f))
                                        drawLine(color, points[0], points[1], stroke); drawLine(color, points[0], points[2], stroke)
                                        if (index == 1) drawLine(color, points[1], points[2], stroke)
                                        points.forEach { drawCircle(color, 3.dp.toPx(), it) }
                                    }
                                    2 -> { drawLine(color, Offset(w/2,0f), Offset(w/2,h),stroke); drawLine(color,Offset(w*.15f,h/2),Offset(w*.85f,h/2),stroke) }
                                    4 -> {
                                        repeat(8) { spoke ->
                                            val angle = spoke * kotlin.math.PI / 4
                                            val direction = Offset(kotlin.math.cos(angle).toFloat(), kotlin.math.sin(angle).toFloat())
                                            drawLine(color, center + direction * h * .33f, center + direction * h * .52f, stroke * 1.4f)
                                        }
                                        drawCircle(color,h*.35f,center,style=Stroke(stroke)); drawCircle(color,h*.12f,center,style=Stroke(stroke))
                                    }
                                }
                            }
                        }
                        Text(if (route == "connections" && LocalDensity.current.fontScale > 1.15f) "Connect" else label, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 12.sp, modifier = Modifier.padding(top = 3.dp), color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
