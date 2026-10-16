import tempfile
import unittest
from pathlib import Path
import numpy as np
from benchmark import inputs, pair_metrics, ROOT


class BenchmarkTests(unittest.TestCase):
    def test_unknown_pairs_excluded_and_self_excluded(self):
        vectors = np.eye(7)
        result = pair_metrics(vectors, [[1, 6], [6, 1]], [[1, 7]])
        self.assertEqual(result['positivePairs'], 1)
        self.assertEqual(result['negativePairs'], 1)
        self.assertEqual(result['knownPositiveRecall']['5'], 1)
        self.assertEqual(len(result['pairScores']), 2)

    def test_conflicting_labels_fail(self):
        with self.assertRaises(AssertionError):
            pair_metrics(np.eye(2), [[1, 2]], [[2, 1]])

    def test_fixture_and_probe_indices(self):
        notes, transfer, probes, labels = inputs()
        self.assertEqual((len(notes), len(transfer)), (172, 40))
        for p in probes:
            self.assertTrue(1 <= p['note'] <= len(notes))
            self.assertIn(p['preferred'], labels)
            self.assertNotEqual(p['preferred'], p['rejected'])

    def test_tokenizer_matches_existing_kotlin_golden_cases(self):
        from transformers import BertTokenizer
        vocab = ['unused'+str(i) for i in range(105)]
        vocab[100:] = ['[UNK]', 'cafe', 'run', '##ning', ',']
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'vocab.txt'
            path.write_text('\n'.join(vocab)+'\n')
            tokenizer = BertTokenizer(vocab_file=str(path), do_lower_case=True)
            self.assertEqual(tokenizer.encode('CAFÉ, running', add_special_tokens=False), [101,104,102,103])
            self.assertEqual(tokenizer.encode('missing', add_special_tokens=False), [100])
            self.assertEqual(tokenizer.encode('\x00 \n', add_special_tokens=False), [])


if __name__ == '__main__':
    unittest.main()
