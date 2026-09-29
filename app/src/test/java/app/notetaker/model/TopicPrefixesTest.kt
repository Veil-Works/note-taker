package app.notetaker.model

import org.junit.Assert.*
import org.junit.Test

class TopicPrefixesTest {
    @Test fun multiple_names_are_deduplicated_and_body_hashtags_are_literal() {
        assertEquals(TopicPrefixedNote("Review #release tomorrow", listOf("Atlas", "orion")),
            parseTopicPrefixes("  #Atlas #orion #ATLAS\nReview #release tomorrow"))
    }
    @Test fun quoted_unicode_and_hyphenated_topics_are_supported() {
        assertEquals(listOf("Running shoes", "健康", "side-project"),
            parseTopicPrefixes("#\"Running shoes\" #健康 #side-project Pack a bag").topics)
    }
    @Test fun ordinary_markdown_and_mid_note_hashtags_are_unchanged() {
        listOf("# Heading", "## Heading", "Remember #Atlas", "#Atlas, release tomorrow", "  Plain text  ").forEach {
            assertEquals(TopicPrefixedNote(it, emptyList()), parseTopicPrefixes(it))
        }
    }
    @Test fun topic_only_input_has_no_body_and_long_names_are_rejected() {
        assertEquals("", parseTopicPrefixes("#Atlas #orion ").content)
        assertTrue(runCatching { parseTopicPrefixes("#" + "a".repeat(41) + " body") }.exceptionOrNull() is IllegalArgumentException)
    }
}
