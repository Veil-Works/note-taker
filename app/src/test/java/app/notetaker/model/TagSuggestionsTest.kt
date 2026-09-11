package app.notetaker.model

import org.junit.Assert.*
import org.junit.Test

class TagSuggestionsTest {
    @Test fun `matches case insensitive words and literal phrases at Unicode boundaries`() {
        listOf("RUN", "a run!", "(run)").forEach { assertTrue(matchesTagPhrase(it, "run")) }
        listOf("brunch", "érun", "run2", "run_more").forEach { assertFalse(matchesTagPhrase(it, "run")) }
        assertTrue(matchesTagPhrase("BENCH PRESS 80kg", "bench press"))
        assertTrue(matchesTagPhrase("bench pressure", "bench press"))
        assertTrue(matchesTagPhrase("Use C++ today", "C++"))
        assertFalse(matchesTagPhrase("anything", "  "))
        assertTrue(matchesTagPhrase("ŽLUŤOUČKÝ!", "žluťoučký"))
    }

    @Test fun `partial words match in either direction with a three letter minimum`() {
        assertTrue(matchesTagPhrase("run 7km thursday", "runs"))
        assertTrue(matchesTagPhrase("RUN 7km thursday", "running"))
        assertTrue(matchesTagPhrase("running today", "run"))
        assertTrue(matchesTagPhrase("bench presses today", "bench press"))
        assertTrue(matchesTagPhrase("bench press today", "bench presses"))
        assertFalse(matchesTagPhrase("ru today", "runs"))
        assertFalse(matchesTagPhrase("inside today", "in"))
        assertFalse(matchesTagPhrase("C today", "C++"))
        assertFalse(matchesTagPhrase("7km today", "7k"))
        assertFalse(matchesTagPhrase("press bench", "bench press"))
        val tags = listOf(Tag("runs", "runs", 0), Tag("fitness", "Fitness", 0))
        val aliases = listOf(TagAlias("alias", "fitness", "running"))
        assertEquals(setOf("runs", "fitness"), suggestTags("run 7km thursday", tags, aliases, emptySet()).map { it.id }.toSet())
        assertEquals(listOf("fitness"), suggestTags("run 7km thursday", tags, aliases, setOf("runs")).map { it.id })
    }

    @Test fun `shared aliases suggest multiple existing tags once and respect exclusions`() {
        val tags = listOf(Tag("b", "Strength", 0), Tag("a", "Bench", 0), Tag("c", "Running", 0))
        val aliases = listOf(TagAlias("1", "a", "bench press"), TagAlias("2", "b", "bench press"), TagAlias("3", "b", "80 kg"))
        assertEquals(listOf("a", "b"), suggestTags("Bench press 80 kg", tags, aliases, emptySet()).map { it.id })
        assertEquals(listOf("b"), suggestTags("Bench press 80 kg", tags, aliases, setOf("a")).map { it.id })
        assertTrue(suggestTags("marathon", tags, aliases, emptySet()).isEmpty())
        assertEquals(listOf("c"), suggestTags("RUNNING today", tags, aliases, emptySet()).map { it.id })
    }
}
