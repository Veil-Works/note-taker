package app.notetaker.feature.graph

import app.notetaker.model.GraphEdge
import kotlin.math.*

data class GraphPoint(val x: Float, val y: Float)

/** Short-range pair repulsion, confidence-weighted springs, and damped integration.
 * Computed off the UI thread. No topic memberships influence the simulation. */
fun graphLayout(ids: List<String>, edges: List<GraphEdge>): Map<String, GraphPoint> {
    if (ids.isEmpty()) return emptyMap()
    if (ids.size == 1) return mapOf(ids.first() to GraphPoint(0.5f, 0.5f))
    val ordered = ids.distinct().sorted()
    val index = ordered.withIndex().associate { it.value to it.index }
    val count = ordered.size
    // A sunflower seed avoids coincident nodes and gives the simulation no preferred grouping.
    val x = DoubleArray(count) { sqrt((it + 0.5) / count) * cos(it * 2.399963229728653) * 0.55 }
    val y = DoubleArray(count) { sqrt((it + 0.5) / count) * sin(it * 2.399963229728653) * 0.55 }
    val vx = DoubleArray(count); val vy = DoubleArray(count)
    val links = edges.filter { it.source != it.target && it.strength.isFinite() }
        .groupBy { minOf(it.source, it.target) to maxOf(it.source, it.target) }
        .mapNotNull { (pair, matches) ->
            index[pair.first]?.let { a -> index[pair.second]?.let { b ->
                Triple(a, b, if (matches.any { it.manual }) 1.0 else matches.maxOf { it.strength }.coerceIn(0.0, 1.0))
            } }
        }
        .sortedWith(compareBy<Triple<Int, Int, Double>> { it.first }.thenBy { it.second })
    val parent = IntArray(count) { it }
    fun component(node: Int): Int {
        var root = node
        while (parent[root] != root) root = parent[root]
        return root
    }
    links.forEach { (a, b, _) -> parent[component(a)] = component(b) }
    val components = IntArray(count) { component(it) }
    val degree = IntArray(count)
    links.forEach { (a, b, _) -> degree[a]++; degree[b]++ }
    repeat(650) {
        val fx = DoubleArray(count); val fy = DoubleArray(count)
        // Nearby notes repel much more strongly than distant ones. Unlike linear
        // outward force, this distributes disconnected notes through an area, not a ring.
        for (a in 0 until count) for (b in 0 until a) {
            val dx = x[a] - x[b]; val dy = y[a] - y[b]
            val squared = (dx * dx + dy * dy).coerceAtLeast(0.0001)
            // Keep groups near each other while spacing their individual notes.
            val force = (if (components[a] == components[b]) 0.0015 else 0.0010) / squared
            fx[a] += dx * force; fy[a] += dy * force
            fx[b] -= dx * force; fy[b] -= dy * force
        }
        links.forEach { (a, b, strength) ->
            val dx = x[b] - x[a]; val dy = y[b] - y[a]
            val distance = hypot(dx, dy).coerceAtLeast(0.0001)
            val spring = 0.35 * strength * (distance - 0.18) / (distance * sqrt(max(degree[a], degree[b]).toDouble()))
            fx[a] += dx * spring; fy[a] += dy * spring
            fx[b] -= dx * spring; fy[b] -= dy * spring
        }
        for (a in ordered.indices) {
            vx[a] = (vx[a] + (fx[a] - x[a] * 0.05) * 0.20) * 0.80
            vy[a] = (vy[a] + (fy[a] - y[a] * 0.05) * 0.20) * 0.80
            x[a] += vx[a].coerceIn(-0.03, 0.03)
            y[a] += vy[a].coerceIn(-0.03, 0.03)
        }
    }
    val centerX = (x.min() + x.max()) / 2
    val centerY = (y.min() + y.max()) / 2
    val span = max(x.max() - x.min(), y.max() - y.min()).coerceAtLeast(0.01)
    return ordered.indices.associate { ordered[it] to GraphPoint(
        (0.5 + 0.84 * (x[it] - centerX) / span).toFloat(),
        (0.5 + 0.84 * (y[it] - centerY) / span).toFloat(),
    ) }
}
