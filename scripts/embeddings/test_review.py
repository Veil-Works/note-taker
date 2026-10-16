import copy
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from review import ReviewStore, Conflict


def comparison():
    return dict(model='granite-small',revision='fixed',clustering=dict(threshold=.775),corpora={
        'development':dict(topics=[
            dict(index=1,members=[1,2],notes=[dict(id=1,text='First note <script>'),dict(id=2,text='Second note')],variants={
                'A_current':[dict(text='first')],'D_hybrid_semantic':[dict(text='second')]}),
            dict(index=2,members=[3,4],notes=[dict(id=3,text='Third note'),dict(id=4,text='Fourth note')],variants={
                'A_current':[dict(text='third')],'D_hybrid_semantic':[]})
        ])})


def rating(complete=False):
    return dict(preferred='tie' if complete else None,coherence='good' if complete else None,labels={
        '1':dict(clarity=3 if complete else None,fit=2 if complete else None),
        '2':dict(clarity=3 if complete else None,fit=2 if complete else None)
    },incorrectNoteIds=[],comment='',suggestedLabel='',complete=complete)


class ReviewTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory()
        self.directory=Path(self.temp.name)
        self.store=ReviewStore(comparison(),self.directory)

    def tearDown(self):
        self.temp.cleanup()

    def test_public_payload_and_export_keep_methods_blinded(self):
        for data in (self.store.public(),self.store.export()):
            encoded=json.dumps(data)
            for forbidden in ('A_current','D_hybrid_semantic','mapping','fusedRankScore','lexicalWeight'):
                self.assertNotIn(forbidden,encoded)
        self.assertEqual(len(self.store.public()['groups']),2)

    def test_order_stays_fixed_after_restart(self):
        self.assertEqual(self.store.public()['groups'],ReviewStore(comparison(),self.directory).public()['groups'])

    def test_save_and_resume_utf8(self):
        review=rating()
        review['comment']='Résumé — thoughts'
        review['incorrectNoteIds']=[2]
        self.store.save('development-001',review,0)
        restored=ReviewStore(comparison(),self.directory)
        self.assertEqual(restored.public()['reviews']['development-001']['review'],review)

    def test_stale_tab_cannot_overwrite_review(self):
        self.store.save('development-001',rating(),0)
        with self.assertRaises(Conflict):
            self.store.save('development-001',rating(),0)
        self.assertEqual(self.store.public()['reviews']['development-001']['revision'],1)

    def test_reject_note_from_another_group(self):
        review=rating()
        review['incorrectNoteIds']=[3]
        with self.assertRaises(ValueError):
            self.store.save('development-001',review,0)

    def test_completion_requires_scores(self):
        review=rating()
        review['complete']=True
        with self.assertRaises(ValueError):
            self.store.save('development-001',review,0)

    def test_no_score_required_for_missing_proposal(self):
        review=rating(True)
        missing=next(slot for slot,names in self.store.groups['development-002']['proposals'].items() if not names)
        review['labels'][missing]=dict(clarity=None,fit=None)
        self.store.save('development-002',review,0)
        self.assertEqual(self.store.summary()['completed'],1)

    def test_partial_reviews_do_not_enter_summary(self):
        self.store.save('development-001',rating(),0)
        self.assertEqual(self.store.summary()['completed'],0)
        self.assertEqual(self.store.summary()['methods']['A_current']['clarityCount'],0)

    def test_summary_maps_random_slots_back_to_methods(self):
        review=rating(True)
        baseline=next(slot for slot,method in self.store.key['mapping']['development-001'].items() if method=='A_current')
        review['preferred']=baseline
        review['incorrectNoteIds']=[2]
        self.store.save('development-001',review,0)
        result=self.store.summary()
        self.assertEqual(result['methods']['A_current']['preferred'],1)
        self.assertEqual(result['methods']['D_hybrid_semantic']['preferred'],0)
        self.assertEqual(result['flaggedMemberships'],1)

    def test_failed_atomic_save_keeps_previous_state(self):
        self.store.save('development-001',rating(),0)
        previous=self.store.path.read_bytes()
        with patch('review.atomic_json',side_effect=OSError('disk error')):
            with self.assertRaises(OSError):
                self.store.save('development-001',rating(True),1)
        self.assertEqual(self.store.path.read_bytes(),previous)
        self.assertEqual(self.store.public()['reviews']['development-001']['revision'],1)

    def test_changed_labels_start_a_separate_study(self):
        changed=comparison()
        changed['corpora']['development']['topics'][0]['variants']['A_current'][0]['text']='changed'
        other=ReviewStore(changed,self.directory)
        self.assertNotEqual(other.study_id,self.store.study_id)
        self.assertEqual(other.public()['reviews'],{})


if __name__=='__main__':
    unittest.main()
