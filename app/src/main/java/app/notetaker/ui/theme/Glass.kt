package app.notetaker.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Translucent tinted glass with a reflected upper edge; no expensive backdrop blur. */
@Composable
fun Modifier.glassPanel(radius: Dp = 22.dp, highlighted: Boolean = false, opaque: Boolean = false): Modifier {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.2f
    val shape = RoundedCornerShape(radius)
    val translucentFill = if (dark) listOf(Color(0xD91A3042), Color(0xB80B1723), Color(0xD909121B))
        else listOf(Color(0xEFFFFFFF), Color(0xCBE3F1FC), Color(0xE6F4FAFF))
    val fill = if (opaque) translucentFill.map { it.copy(alpha = 1f) } else translucentFill
    val rim = listOf(colors.primary.copy(alpha = if (highlighted) .55f else .22f),
        colors.onSurface.copy(alpha = .06f), colors.primary.copy(alpha = if (highlighted) .25f else .10f))
    return this.shadow(if (highlighted) 6.dp else 2.dp, shape, clip = false,
        ambientColor = Color.Black.copy(alpha = .20f), spotColor = Color.Black.copy(alpha = .25f))
        .background(Brush.linearGradient(fill), shape)
        .border(BorderStroke(1.dp, Brush.linearGradient(rim)), shape)
}

@Composable
fun Modifier.notebookBackdrop(): Modifier {
    val base = MaterialTheme.colorScheme.background
    val dark = base.luminance() < 0.2f
    return drawWithCache {
        val upper = Brush.radialGradient(listOf(Color(0xFF365779).copy(alpha = if (dark) .17f else .10f), Color.Transparent),
            center = Offset(size.width * .95f, size.height * .08f), radius = size.width * 1.1f)
        val lower = Brush.radialGradient(listOf(Color(0xFF2B4056).copy(alpha = if (dark) .10f else .06f), Color.Transparent),
            center = Offset(size.width * .05f, size.height * .85f), radius = size.width * .95f)
        onDrawBehind { drawRect(base); drawRect(upper); drawRect(lower) }
    }
}
