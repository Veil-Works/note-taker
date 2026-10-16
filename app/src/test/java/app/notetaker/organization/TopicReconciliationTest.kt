package app.notetaker.organization

import org.junit.Assert.*
import org.junit.Test

class TopicReconciliationTest {
    private fun group(key: String, name: String, vararg members: String) =
        TopicCandidate(key, name, members.toSet(), emptyMap(), emptyMap())
    private fun reconciler() = TopicReconciliation(
        { a, b -> if (a == b) 1.0 else 0.0 },
        { a, b -> if (a.first() == b.first()) .9 else .1 },
    )

    @Test fun mixed_errand_cannot_bridge_retained_rosemary_and_library_even_with_old_binding() {
        val topics = listOf(TopicIdentity("rosemary", "Rosemary", setOf("r1", "r2", "mixed")))
        val groups = listOf(group("library", "Library", "mixed", "l1"), group("plants", "Rosemary", "r1", "r2", "mixed"))
        val legacy = listOf(DiscoveryLineage("library", "rosemary", null, emptySet()))
        val matches = reconciler().reconcile(groups, topics, legacy)
        assertEquals("rosemary", matches["plants"]?.topicId)
        assertNull(matches["library"])
        assertEquals(matches, reconciler().reconcile(groups.reversed(), topics, legacy))
    }

    @Test fun compatible_fingerprint_survives_arbitrary_user_rename() {
        val old = DiscoveryLineage("same", "id", "Plants", setOf("p1", "p2"))
        val result = reconciler().reconcile(listOf(group("same", "Plants", "p1", "p2")),
            listOf(TopicIdentity("id", "My corner", old.members)), listOf(old))
        assertEquals("id", result["same"]?.topicId)
    }

    @Test fun growth_uses_stored_subject_and_member_continuity() {
        val old = DiscoveryLineage("old", "id", "Plants", setOf("p1", "p2"))
        val result = reconciler().reconcile(listOf(group("new", "Plants", "p1", "p2", "p3", "p4")),
            listOf(TopicIdentity("id", "My corner", old.members)), listOf(old))
        assertEquals("id", result["new"]?.topicId)
    }

    @Test fun split_has_one_surviving_identity_and_input_order_does_not_choose_the_child() {
        val topics = listOf(TopicIdentity("id", "Plants", setOf("p1", "p2", "p3", "p4", "l1", "l2")))
        val groups = listOf(group("plants", "Plants", "p1", "p2", "p3", "p4"), group("library", "Library", "l1", "l2"))
        val matches = reconciler().reconcile(groups, topics, emptyList())
        assertEquals(1, matches.size)
        assertEquals("id", matches["plants"]?.topicId)
        assertEquals(matches, reconciler().reconcile(groups.reversed(), topics, emptyList()))
    }

    @Test fun exclusions_are_not_positive_overlap() {
        val result = reconciler().reconcile(listOf(group("new", "Plants", "p1", "p2", "p3")),
            listOf(TopicIdentity("id", "Plants", setOf("p1", "p2"), setOf("p1", "p2"))), emptyList())
        assertTrue(result.isEmpty())
    }

    @Test fun unchanged_fingerprint_keeps_identity_after_exclusion_without_treating_it_as_a_positive() {
        val result = reconciler().reconcile(listOf(group("same", "Plants", "p1", "p2")),
            listOf(TopicIdentity("id", "Plants", emptySet(), setOf("p1", "p2"))),
            listOf(DiscoveryLineage("same", "id", "Plants", setOf("p1", "p2"))))
        assertEquals("id", result["same"]?.topicId)
    }

    @Test fun same_name_without_continuity_does_not_reuse_identity() {
        assertTrue(reconciler().reconcile(listOf(group("new", "Plants", "p3", "p4")),
            listOf(TopicIdentity("id", "Plants", setOf("p1", "p2"))), emptyList()).isEmpty())
    }

    @Test fun two_note_topic_can_replace_one_member_when_independent_subject_support_is_strong() {
        assertEquals("id", reconciler().reconcile(listOf(group("new", "Plants", "p1", "p3")),
            listOf(TopicIdentity("id", "Plants", setOf("p1", "p2"))), emptyList())["new"]?.topicId)
    }

    @Test fun incompatible_fingerprint_is_not_authority() {
        val result = reconciler().reconcile(listOf(group("same", "Library", "mixed", "l1")),
            listOf(TopicIdentity("id", "Rosemary", setOf("mixed", "r1"))),
            listOf(DiscoveryLineage("same", "id", "Rosemary", setOf("mixed", "r1"))))
        assertTrue(result.isEmpty())
    }

    @Test fun explicit_merge_preserves_both_branches_and_their_growth() {
        val topics = listOf(TopicIdentity("id", "My collection", setOf("p1", "p2", "l1", "l2")))
        val history = listOf(DiscoveryLineage("plants", "id", "Plants", setOf("p1", "p2"), 1, "branch-p"),
            DiscoveryLineage("library", "id", "Library", setOf("l1", "l2"), 1, "branch-l"))
        val groups = listOf(group("new-p", "Plants", "p1", "p2", "p3"), group("new-l", "Library", "l1", "l2", "l3"))
        val result = reconciler().reconcile(groups, topics, history)
        assertEquals(setOf("new-p", "new-l"), result.keys)
        assertTrue(result.values.all { it.topicId == "id" && it.mergeKind == 1 })
        assertEquals("branch-p", result["new-p"]?.branchKey)
    }

    @Test fun repeated_snapshots_of_a_merged_branch_do_not_authorize_two_children() {
        val history = listOf(DiscoveryLineage("old", "id", "Plants", setOf("p1", "p2"), 1, "branch"),
            DiscoveryLineage("new", "id", "Plants", setOf("p1", "p2", "p3"), 1, "branch"))
        val result = reconciler().reconcile(listOf(group("a", "Plants", "p1", "p2"), group("b", "Plants", "p2", "p3")),
            listOf(TopicIdentity("id", "Plants", setOf("p1", "p2", "p3"))), history)
        assertEquals(1, result.size)
    }

    @Test fun legacy_merge_only_preserves_exact_historical_bindings() {
        val history = listOf(DiscoveryLineage("old-p", "id", null, emptySet(), 2), DiscoveryLineage("old-l", "id", null, emptySet(), 2))
        val result = reconciler().reconcile(listOf(group("old-p", "Plants", "p1", "p2"), group("old-l", "Library", "l1", "l2"),
            group("new", "Library", "l1", "l2", "l3")), listOf(TopicIdentity("id", "Collection", setOf("p1", "p2", "l1", "l2"))), history)
        assertEquals(setOf("old-p", "old-l"), result.keys)
    }

    @Test fun deleted_fingerprints_remain_suppressed() {
        assertTrue(reconciler().reconcile(listOf(group("gone", "Plants", "p1", "p2")),
            listOf(TopicIdentity("id", "Plants", setOf("p1", "p2"))),
            listOf(DiscoveryLineage("gone", null, null, emptySet()))).isEmpty())
    }

    @Test fun equally_plausible_different_identities_abstain() {
        val topics = listOf(TopicIdentity("a", "Plants", setOf("p1", "p2")), TopicIdentity("b", "Plants", setOf("p1", "p2")))
        assertTrue(reconciler().reconcile(listOf(group("new", "Plants", "p1", "p2")), topics, emptyList()).isEmpty())
    }

    @Test fun assignment_optimizes_total_score_instead_of_greedy_first_claim() {
        assertArrayEquals(intArrayOf(1, 0), maximumAssignment(arrayOf(doubleArrayOf(.9, .8), doubleArrayOf(.85, 0.0))))
        assertArrayEquals(intArrayOf(-1), maximumAssignment(arrayOf(doubleArrayOf(-1.0))))
    }

    @Test fun final_names_allow_swaps_and_only_suffix_real_collisions() {
        assertEquals(mapOf("a" to "Library", "b" to "Plants"), allocateTopicNames(mapOf("a" to "Library", "b" to "Plants"), emptyList()))
        assertEquals("Library (2)", allocateTopicNames(mapOf("a" to "Library"), listOf("LIBRARY"))["a"])
        assertEquals(mapOf("a" to "Library", "b" to "Library (2)"), allocateTopicNames(mapOf("b" to "Library", "a" to "Library"), emptyList()))
    }

    @Test fun different_subjects_with_identical_members_get_distinct_stable_lineage_keys() {
        val groups = listOf(group("same", "Atlas", "a", "b"), group("same", "Beacon", "a", "b"))
        val keyed = distinctDiscoveryKeys(groups, emptySet())
        assertEquals(2, keyed.map { it.key }.distinct().size)
        assertEquals(keyed.first().key, distinctDiscoveryKeys(groups.take(1), keyed.map { it.key }.toSet()).single().key)
        assertEquals(keyed.toSet(), distinctDiscoveryKeys(groups.reversed(), emptySet()).toSet())
    }
}
