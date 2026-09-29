import unittest
import compare


class OutputContractTest(unittest.TestCase):
    def test_hybrid_rejects_ids_outside_supplied_notes(self):
        with self.assertRaises(ValueError):
            compare.validate_groups({'groups':[{'label':'Gardening','ids':['1','99']}]},{'1','2'})

    def test_rejects_duplicate_ids_and_singletons(self):
        for ids in [['1','1'],['1']]:
            with self.assertRaises(ValueError):
                compare.validate_groups({'groups':[{'label':'Gardening','ids':ids}]},{'1','2'})

    def test_supports_abstention_and_overlapping_topics(self):
        self.assertEqual([],compare.validate_groups({'groups':[]},{'1','2'}))
        groups=compare.validate_groups({'groups':[{'label':'Gardening','ids':['1','2']},
            {'label':'Shopping','ids':['2','3']}]},{'1','2','3'})
        self.assertEqual(2,len(groups))
        self.assertTrue(all('2' in g['members'] for g in groups))

    def test_long_labels_and_unrequested_fields_are_rejected(self):
        for item in [{'label':'Gardening','ids':['1','2'],'delete':True},
                     {'label':'one two three four five six seven','ids':['1','2']}]:
            with self.assertRaises(ValueError):compare.validate_groups({'groups':[item]},{'1','2'})

    def test_candidate_generation_reaches_ungrouped_notes(self):
        corpus={'notes':[{'id':str(i),'text':str(i),'vector':[1.0,0.0]} for i in range(1,11)],
                'groups':[{'label':'Existing','members':['1','2'],'named':False}]}
        batches=compare.candidate_batches(corpus)
        presented={n['id'] for b in batches for n in b['notes']}
        self.assertEqual({str(i) for i in range(1,11)},presented)
        self.assertTrue({'1','2'}<={n['id'] for n in batches[0]['notes']})


if __name__=='__main__':unittest.main()
