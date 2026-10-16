import unittest
import numpy as np
from compatible_scopes import rival_indices, supported_topics, merge_topics


class CompatibleScopesTests(unittest.TestCase):
    def setUp(self):
        self.labels=['Meetings','Note-taking','Meeting planning']
        self.rivals=rival_indices(self.labels,dict(rivals=[['Meetings','Meeting planning']],compatibleExamples=[['Meetings','Note-taking']]))

    def test_compatible_overlap_survives_and_rival_is_rejected(self):
        scores=np.array([[.8,.83,.6],[.81,.82,.6]])
        topics=supported_topics([{1,2}],scores,self.labels,self.rivals,.05,0)
        self.assertEqual({t['label'] for t in topics},{'Meetings','Note-taking'})
        self.assertTrue(all(t['members']==[1,2] for t in topics))

    def test_slack_does_not_override_rival_evidence(self):
        scores=np.array([[.8,.7,.9],[.8,.7,.9]])
        topics=supported_topics([{1,2}],scores,self.labels,self.rivals,.3,0)
        self.assertNotIn('Meetings',{t['label'] for t in topics})

    def test_weak_near_tie_can_abstain(self):
        scores=np.array([[.8,.2,.81],[.8,.2,.81]])
        self.assertEqual(supported_topics([{1,2}],scores,self.labels,self.rivals,.05,.025),[])

    def test_distant_non_rival_does_not_gain_membership(self):
        scores=np.array([[.8,.3,.1],[.8,.3,.1]])
        topics=supported_topics([{1,2}],scores,self.labels,self.rivals,.05,0)
        self.assertEqual([t['label'] for t in topics],['Meetings'])

    def test_conflicting_relation_graph_fails(self):
        with self.assertRaises(ValueError):
            rival_indices(self.labels,dict(rivals=[['Meetings','Note-taking']],compatibleExamples=[['Meetings','Note-taking']]))

    def test_consolidation_does_not_merge_different_labels(self):
        topics=[dict(label='A',members=[1,2]),dict(label='B',members=[1,2])]
        self.assertEqual(len(merge_topics(topics,np.ones((2,2)),.5)),2)

    def test_display_filter_removes_candidate_and_competitor(self):
        scores=np.array([[.8,.3,.9],[.8,.3,.9]])
        topics=supported_topics([{1,2}],scores,self.labels,self.rivals,.05,0,['Meeting planning'])
        self.assertEqual([t['label'] for t in topics],['Meetings'])

    def test_all_excluded_is_empty(self):
        self.assertEqual(supported_topics([{1,2}],np.ones((2,3)),self.labels,self.rivals,0,0,self.labels),[])


if __name__=='__main__':
    unittest.main()
