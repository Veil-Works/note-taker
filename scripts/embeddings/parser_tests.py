"""Run explicitly in the isolated parser environment."""
import unittest
import en_core_web_sm
from extract_label_phrases import phrases


class ParserPhraseTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.model=en_core_web_sm.load(disable=['ner'])

    def test_every_candidate_preserves_source_span(self):
        text='The old wooden boat has a red sail. These bicycles need new tires.'
        candidates=phrases(self.model(text))
        self.assertIn('wooden boat',candidates)
        for phrase,spans in candidates.items():
            for span in spans:
                self.assertEqual(text[span['start']:span['end']],span['text'])
                self.assertEqual(' '.join(span['text'].lower().split()),phrase)
                self.assertNotIn('.',span['text'])

    def test_removed_stopwords_do_not_create_nonexistent_phrase(self):
        result=phrases(self.model('Notes about music are useful. Notes about music improve recall.'))
        self.assertNotIn('notes music',result)


if __name__=='__main__':
    unittest.main()
