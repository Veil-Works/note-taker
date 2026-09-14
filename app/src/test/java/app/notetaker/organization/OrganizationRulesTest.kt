package app.notetaker.organization

import app.notetaker.model.Tag
import app.notetaker.model.TagAlias
import org.junit.Assert.*
import org.junit.Test

class OrganizationRulesTest {
    @Test fun shared_categories_name_groups_without_matching_unrelated_words() {
        assertEquals("Dogs", topicName(listOf("doberman", "rottweiler")))
        assertEquals("Running", topicName(listOf("run by river 5.2km", "run 7.1km river bridge park")))
        assertNotEquals("Running", topicName(listOf("Relay dry run", "run the backfill")))
        assertNotEquals("Dogs", topicName(listOf("hotdog lunch", "dogmatic discussion")))
        assertEquals("Related notes", topicName(listOf("alpha", "beta")))
    }
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
    @Test fun gym_labels_ignore_generic_quantifiers() {
        assertEquals("Gym", topicName(listOf("gym squat each 3x5", "gym row each 3x10", "gym bench each 3x8")))
    }
    @Test fun recurring_subjects_rescue_short_notes_without_matching_unrelated_text() {
        val contents = mapOf("a" to "Orion release monday", "b" to "Orion retry fix", "c" to "Orion checklist click", "d" to "Orion constellation tonight")
        val vectors = mapOf("a" to floatArrayOf(1f, 0f), "b" to floatArrayOf(0.9f, 0.1f), "c" to floatArrayOf(0.35f, 0.65f), "d" to floatArrayOf(-1f, 0f))
        // Without the unrelated homonym, the repeated project subject is coherent.
        val subjects = recurringSubjects(contents.filterKeys { it != "d" }, vectors)
        assertEquals(setOf("a", "b", "c"), subjects.keys)
        assertEquals(0.415, relevantConnectionScore("a", "c", vectors, subjects)!!, 0.001)
        assertNull(relevantConnectionScore("a", "d", vectors, subjects))
        assertFalse(recurringSubjects(contents, vectors).containsKey("d"))
    }
    @Test fun grouping_allows_variation_but_rejects_bridges_to_unrelated_notes() {
        assertTrue(fitsTopic(listOf(0.65, 0.38, 0.40)))
        assertFalse(fitsTopic(listOf(0.90, 0.10)))
        assertFalse(fitsTopic(emptyList()))
    }
    @Test fun relevance_uses_similarity_and_subject_evidence_without_a_rank_limit() {
        val vectors = mapOf("a" to floatArrayOf(1f,0f), "weak" to floatArrayOf(0.387f,0.922f), "good" to floatArrayOf(0.6f,0.8f))
        assertNull(relevantConnectionScore("a", "weak", vectors, emptyMap()))
        assertEquals(0.6, relevantConnectionScore("a", "good", vectors, emptyMap())!!, 0.001)
        val subject = mapOf("a" to "orion", "weak" to "orion")
        assertNotNull(relevantConnectionScore("a", "weak", vectors, subject))
        assertEquals(relevantConnectionScore("a", "weak", vectors, subject), relevantConnectionScore("weak", "a", vectors, subject))
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
