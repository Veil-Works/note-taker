import unittest
import numpy as np
from general_comparison import density, generate, representations
from grouping_experiment import edges


class GeneralComparisonTests(unittest.TestCase):
    def test_density_order_invariant(self):
        rng=np.random.default_rng(7)
        v=np.vstack([rng.normal(loc=c,scale=.025,size=(8,2)) for c in [(0,1),(1,0),(-1,0)]])
        v=v/np.linalg.norm(v,axis=1)[:,None]
        ids=list(range(1,len(v)+1))
        expected=density(v,ids,3,2,'eom')
        permutation=rng.permutation(len(v))
        actual=density(v[permutation],[ids[i] for i in permutation],3,2,'eom')
        self.assertEqual(expected,actual)
        self.assertTrue(expected)
        # Density clustering may split a blob; it must not mix widely separated blobs.
        self.assertTrue(all(len({(i-1)//8 for i in group})==1 for group in expected))

    def test_consolidation_respects_noncontiguous_ids(self):
        vectors=np.array([[1,0],[.8,.6],[0,1]],dtype=float)
        result=generate(vectors,[2,4,9],'overlapping-cores',dict(threshold=.5))
        self.assertEqual(edges(result),{(2,4),(4,9)})
        self.assertNotIn((2,9),edges(result))

    def test_terms_only_come_from_observed_document_ngrams(self):
        texts=['alpha beta','gamma delta','alpha beta','gamma delta']
        topics=representations(texts,[{1,2,3,4}],np.eye(4))
        terms={t['text'] for t in topics[0]['terms']}
        self.assertTrue(terms<={'alpha','beta','gamma','delta','alpha beta','gamma delta'})
        self.assertNotIn('beta gamma',terms)
        self.assertTrue(set(topics[0]['representatives'])<=set(topics[0]['members']))

    def test_empty_vocabulary_keeps_members_without_invented_label(self):
        topics=representations(['the and','the or'],[{1,2}],np.eye(2))
        self.assertEqual(topics[0]['terms'],[])
        self.assertEqual(topics[0]['members'],[1,2])


if __name__=='__main__':
    unittest.main()
