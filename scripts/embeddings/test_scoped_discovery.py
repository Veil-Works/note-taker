import unittest
import numpy as np
from scoped_discovery import consolidate, label_and_validate, scope_metrics
from grouping_experiment import edges


class ScopedDiscoveryTests(unittest.TestCase):
    def test_union_must_not_bridge_weak_pair(self):
        matrix=np.array([[1,.8,.2],[.8,1,.8],[.2,.8,1]])
        groups=[{1,2},{2,3}]
        self.assertEqual(len(consolidate(groups,matrix,.6,.3)),2)

    def test_coherent_union_merges_without_losing_edges(self):
        matrix=np.full((4,4),.8)
        np.fill_diagonal(matrix,1)
        original=[{1,2,3},{2,3,4}]
        merged=consolidate(original,matrix,.6,.5)
        self.assertEqual(merged,[frozenset({1,2,3,4})])
        self.assertTrue(edges(original)<=edges(merged))
        self.assertEqual(merged,consolidate(list(reversed(original)),matrix,.6,.5))

    def test_unrelated_disjoint_groups_never_consolidate(self):
        self.assertEqual(len(consolidate([{1,2},{3,4}],np.ones((4,4)),.6,.5)),2)

    def test_scope_pruning_preserves_supported_pair(self):
        scores=np.array([[.9,.3],[.8,.2],[.4,.7]])
        topics=label_and_validate([{1,2,3}],scores,['Activity','Equipment'])
        self.assertEqual(topics[0]['members'],[1,2])
        self.assertEqual(topics[0]['label'],'Activity')

    def test_singleton_and_low_margin_suppression(self):
        scores=np.array([[.9,.2],[.51,.5]])
        self.assertEqual(label_and_validate([{1,2}],scores,['A','B'],.025),[])

    def test_scope_split_preserves_two_supported_subjects(self):
        scores=np.array([[.9,.1],[.8,.1],[.1,.7],[.1,.8]])
        original=label_and_validate([{1,2,3,4}],scores,['A','B'])
        split=label_and_validate([{1,2,3,4}],scores,['A','B'],split=True)
        self.assertEqual(len(original),1)
        self.assertEqual([(t['label'],t['members']) for t in split],[('A',[1,2]),('B',[3,4])])
        self.assertNotIn((1,3),edges([set(t['members']) for t in split]))

    def test_empty_topics_do_not_count_as_positive_scope_hits(self):
        result=scope_metrics([], [dict(note=1,preferred='A',rejected='B')])
        self.assertEqual(result['preferredPresent'],0)
        self.assertEqual(result['rejectedPresent'],0)


if __name__=='__main__':
    unittest.main()
