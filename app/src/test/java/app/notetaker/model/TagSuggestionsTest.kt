package app.notetaker.model

import org.junit.Assert.*
import org.junit.Test

class TagSuggestionsTest {
    @Test fun `matches case insensitive words and literal phrases at Unicode boundaries`() {
        listOf("RUN", "a run!", "(run)").forEach { assertTrue(matchesTagPhrase(it, "run")) }
        listOf("brunch", "runner", "érun", "run2", "run_more").forEach { assertFalse(matchesTagPhrase(it, "run")) }
        assertTrue(matchesTagPhrase("BENCH PRESS 80kg", "bench press"))
        assertFalse(matchesTagPhrase("bench pressure", "bench press"))
        assertTrue(matchesTagPhrase("Use C++ today", "C++"))
        assertFalse(matchesTagPhrase("anything", "  "))
        assertTrue(matchesTagPhrase("ŽLUŤOUČKÝ!", "žluťoučký"))
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
