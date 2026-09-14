package app.notetaker.feature.graph

import app.notetaker.model.*
import app.notetaker.ui.graphEdges
import org.junit.Assert.*
import org.junit.Test

class GraphLayoutTest {
    @Test fun layout_is_bounded_deterministic_and_handles_isolated_notes() {
        val ids = (1..74).map { "n$it" }
        val edges = ids.zipWithNext { a,b -> GraphEdge(a,b,0.7) }
        val layout = graphLayout(ids, edges)
        assertEquals(ids.toSet(), layout.keys)
        assertEquals(layout, graphLayout(ids, edges))
        assertTrue(layout.values.all { it.x.isFinite() && it.y.isFinite() && it.x in 0f..1f && it.y in 0f..1f })
        assertEquals(GraphPoint(0.5f,0.5f),graphLayout(listOf("a"),emptyList())["a"])
        assertTrue(graphLayout(emptyList(),emptyList()).isEmpty())
    }
    @Test fun larger_notebooks_keep_every_vertex() {
        val ids = (1..400).map { "note-$it" }
        val layout = graphLayout(ids, emptyList())
        assertEquals(ids.toSet(), layout.keys)
        assertTrue(layout.values.all { it.x in 0f..1f && it.y in 0f..1f })
    }
    @Test fun disconnected_notes_fill_an_area_instead_of_a_hollow_ring() {
        val layout = graphLayout((1..74).map { "n$it" }, emptyList())
        assertTrue(layout.values.count { kotlin.math.hypot(it.x - .5f, it.y - .5f) < .22f } >= 8)
    }
    @Test fun connected_communities_settle_closer_than_unconnected_notes() {
        val first = listOf("a", "c", "e", "g")
        val second = listOf("b", "d", "f", "h")
        val edges = listOf(first,second).flatMap { group -> group.flatMap { a -> group.filter { it > a }.map { GraphEdge(a,it,0.8) } } }
        val layout = graphLayout(first + second,edges)
        fun distance(a: String,b: String): Double {
            val p=layout.getValue(a);val q=layout.getValue(b)
            return kotlin.math.hypot((p.x-q.x).toDouble(),(p.y-q.y).toDouble())
        }
        val within = listOf(first,second).flatMap { g -> g.flatMap { a -> g.filter { it>a }.map { distance(a,it) } } }.average()
        val between = first.flatMap { a -> second.map { distance(a,it) } }.average()
        assertTrue("Connected groups should separate: within=$within between=$between", within < between * 0.5)
        assertEquals(layout, graphLayout((first+second).reversed(),edges.reversed()))
    }
    @Test fun graph_deduplicates_bidirectional_links_and_preserves_manual_connections() {
        val notes = listOf(Note("a","First",1,1,linkedNoteIds=setOf("b")),Note("b","Second",2,2))
        val result = graphEdges(notes,listOf(GraphEdge("b","a",0.6),GraphEdge("a","b",0.8),GraphEdge("a","missing",0.9),GraphEdge("a","a",1.0)))
        assertEquals(listOf(GraphEdge("a","b",1.0,true)),result)
    }
}
