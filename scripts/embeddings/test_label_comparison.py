import unittest
import numpy as np
from label_comparison import literal_spans, lexical_pools, fractions, diverse, add_semantics, source_phrases


class LabelComparisonTests(unittest.TestCase):
    def test_literal_source_does_not_match_removed_stopwords(self):
        self.assertEqual(literal_spans('notes about music','notes music'),[])
        self.assertEqual(literal_spans('Notes about music','notes about music')[0]['text'],'Notes about music')
        self.assertEqual(literal_spans('runner','run'),[])

    def test_binary_support_ignores_in_note_repetition(self):
        notes=['alpha alpha alpha beta','alpha beta']
        original=[{'alpha':[{}, {}, {}],'beta':[{}]},{'alpha':[{}],'beta':[{}]}]
        repeated=[{'alpha':[{}]*20,'beta':[{}]},{'alpha':[{}],'beta':[{}]}]
        a=lexical_pools(notes,[{1,2}],original,True)
        b=lexical_pools(notes,[{1,2}],repeated,True)
        self.assertEqual([(c['text'],c['lexicalWeight']) for c in a[0]],[(c['text'],c['lexicalWeight']) for c in b[0]])

    def test_single_member_candidate_is_excluded(self):
        extraction=[{'alpha':[{}],'solo':[{}]},{'alpha':[{}]}, {'solo':[{}]}]
        pools=lexical_pools(['alpha solo','alpha','solo'],[{1,2}],extraction,True)
        self.assertEqual([c['text'] for c in pools[0]],['alpha'])

    def test_tied_ranks_are_neutral(self):
        self.assertTrue(np.allclose(fractions([3,3,3]),.5))
        self.assertTrue(np.allclose(fractions([1]),1))

    def test_mmr_keeps_best_label_and_reduces_redundancy(self):
        candidates=[dict(text='a',fusedRankScore=1,support=2),dict(text='b',fusedRankScore=.99,support=2),dict(text='c',fusedRankScore=.98,support=2)]
        vectors=dict(a=np.array([1.,0]),b=np.array([1.,0]),c=np.array([0.,1]))
        selected=diverse(candidates,vectors,2)
        self.assertEqual([c['text'] for c in selected],['a','c'])

    def test_contrast_uses_other_groups_only(self):
        pool=[dict(text='candidate',lexicalWeight=1,support=2)]
        notes=np.array([[1.,0],[1.,0],[0.,1],[0.,1]])
        result=add_semantics(pool,{1,2},[{1,2},{3,4}],notes,{'candidate':np.array([1.,0])})
        self.assertEqual(result[0]['contrastCosine'],1)
        self.assertEqual(result[0]['weakQuarterCosine'],1)

    def test_source_phrase_union_preserves_stopword_gaps(self):
        phrases=source_phrases('Notes about music')
        self.assertIn('notes about music',phrases)
        self.assertNotIn('notes music',phrases)

    def test_source_phrase_union_does_not_cross_sentence_boundaries(self):
        text='Red boat. Blue sky'
        phrases=source_phrases(text)
        self.assertIn('red boat',phrases)
        self.assertNotIn('boat blue',phrases)
        for key,spans in phrases.items():
            for span in spans:
                self.assertEqual(text[span['start']:span['end']],span['text'])
                self.assertEqual(' '.join(span['text'].lower().split()),key)


if __name__=='__main__':
    unittest.main()
