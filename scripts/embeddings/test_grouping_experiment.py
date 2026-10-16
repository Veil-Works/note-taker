import unittest
import numpy as np
from grouping_experiment import discover, edges, evaluate, jaccard, POLICIES


class GroupingExperimentTests(unittest.TestCase):
    def setUp(self):
        self.matrix=np.array([[1,.8,.2],[.8,1,.8],[.2,.8,1]])

    def test_bridge_cannot_join_complete_or_overlap_core(self):
        self.assertEqual(discover(self.matrix,[1,2,3],'mutual-components',.6),[frozenset({1,2,3})])
        for policy in ('complete-link','overlapping-cores'):
            self.assertNotIn((1,3),edges(discover(self.matrix,[1,2,3],policy,.6)))

    def test_overlap_retains_two_distinct_subjects(self):
        groups=discover(self.matrix,[1,2,3],'overlapping-cores',.6)
        self.assertEqual(groups,[frozenset({1,2}),frozenset({2,3})])
        metrics=evaluate(groups,[[1,2],[2,3]],[[1,3]],3)
        self.assertEqual(metrics['positiveRecall'],1)
        self.assertEqual(metrics['negativeHits'],0)
        self.assertEqual(metrics['overlappingNotes'],1)

    def test_permutation_uses_ids_for_ties(self):
        order=[2,0,1]
        for policy in POLICIES:
            expected=discover(self.matrix,[1,2,3],policy,.6)
            actual=discover(self.matrix[np.ix_(order,order)],[3,1,2],policy,.6)
            self.assertEqual(expected,actual)

    def test_isolates_do_not_become_topics(self):
        for policy in POLICIES:
            self.assertEqual(discover(np.eye(3),[1,2,3],policy,.6),[])

    def test_unknown_co_members_are_not_negative_annotations(self):
        result=evaluate([{1,2,3}],[[1,2]],[[1,4]],4)
        self.assertEqual(result['negativeHits'],0)
        self.assertEqual(result['positiveHits'],1)
        self.assertEqual(result['coverage'],.75)
        self.assertEqual(jaccard(set(),set()),1)


if __name__=='__main__':
    unittest.main()
