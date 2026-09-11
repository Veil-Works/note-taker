package app.notetaker.organization

import app.notetaker.model.Tag
import app.notetaker.model.TagAlias
import org.junit.Assert.*
import org.junit.Test

class OrganizationRulesTest {
    @Test fun prefixes_and_ambiguous_aliases_do_not_become_automatic_matches() {
        val tags = listOf(Tag("run", "Run", 0), Tag("a", "Bench press", 0), Tag("b", "Bench research", 0))
        val aliases = listOf(TagAlias("1", "a", "bench"), TagAlias("2", "b", "bench"))
        assertTrue(exactConceptMatches("Fix a runtime error at the bench", tags, aliases).isEmpty())
        assertEquals(mapOf("a" to "bench press"), exactConceptMatches("BENCH PRESS 80kg", tags, aliases))
    }
    @Test fun topic_labels_prefer_recurring_informative_phrases() {
        assertEquals("Bench press", topicName(listOf("Bench press today", "Bench press again")))
        assertEquals("Related notes", topicName(listOf("the and", "123")))
    }
    @Test fun wordpiece_handles_case_accents_punctuation_and_unknown_words() {
        val vocab = MutableList(110) { "unused-$it" }
        vocab[100] = "[UNK]"; vocab[101] = "cafe"; vocab[102] = "run"; vocab[103] = "##ning"; vocab[104] = ","
        val tokenizer = WordPieceTokenizer(vocab)
        assertEquals(listOf(101L,104L,102L,103L), tokenizer.tokenize("CAFÉ, running"))
        assertEquals(listOf(100L), tokenizer.tokenize("missing"))
        assertTrue(tokenizer.tokenize("\u0000 \n").isEmpty())
    }
}
