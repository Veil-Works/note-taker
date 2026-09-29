package app.notetaker.organization

import org.junit.Assert.*
import org.junit.Test

class TopicDiscoveryTest {
    @Test fun fragments_require_independent_content_and_keep_short_list_items_together() {
        assertEquals(emptyList<String>(), topicFragments("Buy oats, milk and eggs"))
        assertEquals(listOf("Fix the server retry loop.", "Book train tickets for Vienna."),
            topicFragments("Fix the server retry loop. Book train tickets for Vienna."))
        assertEquals(listOf("Review server timeout traces", "prepare the quarterly revenue report"),
            topicFragments("Review server timeout traces and prepare the quarterly revenue report"))
    }
    @Test fun observed_candidates_include_inflection_variants_and_need_distinct_notes() {
        val labels = observedLabelCandidates(listOf("Planning meeting agenda", "Meetings need clear decisions", "Coffee coffee coffee"))
        assertTrue("meeting" in labels && "meetings" in labels)
        assertFalse("coffee" in labels)
    }
    @Test fun subgroup_splitting_requires_two_coherent_supported_cohorts() {
        val ids = setOf("a1", "a2", "a3", "b1", "b2", "b3")
        assertEquals(setOf(setOf("a1", "a2", "a3"), setOf("b1", "b2", "b3")),
            coherentPartition(ids) { a, b -> if (a.first() == b.first()) .8 else .3 }.toSet())
        assertEquals(listOf(ids), coherentPartition(ids) { _, _ -> .5 })
        assertEquals(1, coherentPartition(ids - "b3") { a, b -> if (a.first() == b.first()) .8 else .1 }.size)
    }
    @Test fun a_similar_pair_without_a_supported_label_remains_ungrouped() {
        val notes = listOf(DiscoveryNote("a", "Miscellaneous detail", floatArrayOf(1f, 0f)),
            DiscoveryNote("b", "Another fragment", floatArrayOf(1f, 0f)))
        assertTrue(TopicDiscovery(labels).discover(notes).isEmpty())
    }
    @Test fun label_agreement_penalizes_incidental_mentions_and_one_member_dominance() {
        val incidental = labelAgreement(listOf(.59, .55), listOf(.59, .21))
        val shared = labelAgreement(listOf(.50, .48), listOf(.50, .48))
        assertTrue(shared > incidental)
        assertTrue(labelAgreement(listOf(.50, .50), listOf(.50, .50)) >
            labelAgreement(listOf(.85, .15), listOf(.85, .15)))
    }
    @Test fun focus_keeps_vague_openings_and_single_sentence_notes_intact() {
        assertEquals("Finally. Fix the server retry loop.", topicFocus("Finally. Fix the server retry loop."))
        assertEquals("Bench press 80 kg, 3x5", topicFocus("Bench press 80 kg, 3x5"))
        assertEquals("Book train tickets for Vienna.", topicFocus("Book train tickets for Vienna. Pack a spare cable."))
    }
    private val labels = object : TextEmbedding {
        override val version = "no-label-model"
        override fun embed(text: String) = floatArrayOf(0f, 0f)
    }
    @Test fun representatives_admit_good_neighbors_without_accepting_a_single_bridge() {
        assertNotNull(representativeMatch(listOf(.567, .398, .374, .29)))
        assertNotNull(representativeMatch(listOf(.545, .384, .25)))
        assertNotNull(representativeMatch(listOf(.487, .449, .421, .325)))
        assertNotNull(representativeMatch(listOf(.415, .408, .296)))
        assertNull(representativeMatch(listOf(.558, .365, .352)))
        assertNull(representativeMatch(listOf(.9, .10, .05)))
        assertNull(representativeMatch(listOf(.9)))
        assertNull(representativeMatch(listOf(.38, .30, .25)))
    }
    @Test fun named_subjects_work_inside_short_notes_and_allow_multiple_projects() {
        val notes = listOf(
            DiscoveryNote("a", "Review Aster release", floatArrayOf(1f, 0f)),
            DiscoveryNote("b", "Fix Aster retry", floatArrayOf(1f, 0f)),
            DiscoveryNote("c", "Aster checklist", floatArrayOf(1f, 0f)),
            DiscoveryNote("d", "Review Birch billing", floatArrayOf(0f, 1f)),
            DiscoveryNote("e", "Fix Birch receipts", floatArrayOf(0f, 1f)),
            DiscoveryNote("f", "Birch checklist", floatArrayOf(0f, 1f)),
            DiscoveryNote("both", "Ask about Aster release and Birch receipts", normalize(floatArrayOf(1f, 1f))),
            DiscoveryNote("unrelated", "Aster flowers in the garden", floatArrayOf(-1f, 0f)),
        )
        val groups = TopicDiscovery(labels).discover(notes)
        assertEquals(setOf("Aster", "Birch"), groups.map { it.name }.toSet())
        assertTrue(groups.all { "both" in it.members })
        assertTrue(groups.none { "unrelated" in it.members })
        assertFalse("d" in groups.single { it.name == "Aster" }.members)
        assertFalse("a" in groups.single { it.name == "Birch" }.members)
        val reordered = TopicDiscovery(labels).discover(notes.reversed())
        assertEquals(groups.map { it.name to it.members }, reordered.map { it.name to it.members })
    }
    @Test fun generic_sentence_starts_do_not_become_named_subjects() {
        val notes = listOf("Idea for a park", "Idea for a server", "Idea for dinner").mapIndexed { i, text -> DiscoveryNote("$i", text, floatArrayOf(1f, 0f)) }
        assertTrue(namedSubjectMembers(notes).isEmpty())
    }
}
