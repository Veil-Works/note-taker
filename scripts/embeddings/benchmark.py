#!/usr/bin/env python3
"""Offline CPU comparison. Run prepare.py first; outputs are development diagnostics."""
import hashlib
import itertools
import json
import os
from pathlib import Path
import platform
import resource
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[2]
LOCAL = ROOT / '.local/embeddings'
os.environ.update(HF_HUB_OFFLINE='1', TRANSFORMERS_OFFLINE='1', HF_HUB_DISABLE_TELEMETRY='1', TOKENIZERS_PARALLELISM='false')


def read(path):
    return json.loads((ROOT / path).read_text())


def inputs():
    notes = read('fixtures/sample-notes.json') + read('fixtures/expanded-notes.json')
    transfer = read('fixtures/hybrid/heldout-notes.json')
    probes = read('fixtures/embedding-scope-probes.json')['probes']
    labels = sorted({p[k] for p in probes for k in ('preferred', 'rejected')})
    return notes, transfer, probes, labels


def pair_metrics(vectors, positive, negative):
    import numpy as np
    from sklearn.metrics import average_precision_score, roc_auc_score
    positive = sorted({tuple(sorted(p)) for p in positive})
    negative = sorted({tuple(sorted(p)) for p in negative})
    assert not set(positive) & set(negative)
    assert all(1 <= a <= len(vectors) and 1 <= b <= len(vectors) and a != b for a, b in positive + negative)
    # Explicit float64 reduction avoids platform BLAS floating-point flag warnings.
    scores = np.einsum('ik,jk->ij', vectors.astype(np.float64), vectors.astype(np.float64), optimize=False)
    assert np.isfinite(scores).all()
    pairs = positive + negative
    truth = [1] * len(positive) + [0] * len(negative)
    values = [float(scores[a-1, b-1]) for a, b in pairs]
    adjacency = {}
    for a, b in positive:
        adjacency.setdefault(a-1, set()).add(b-1)
        adjacency.setdefault(b-1, set()).add(a-1)
    recall = {}
    for k in (5, 10):
        found = total = 0
        for anchor, relevant in adjacency.items():
            ranking = [int(i) for i in np.argsort(-scores[anchor], kind='stable') if i != anchor][:k]
            found += len(set(ranking) & relevant)
            total += len(relevant)
        recall[str(k)] = found / total
    return dict(positivePairs=len(positive), negativePairs=len(negative), rocAuc=float(roc_auc_score(truth, values)), averagePrecision=float(average_precision_score(truth, values)), knownPositiveRecall=recall, pairScores=[dict(pair=p, positive=bool(t), cosine=v) for p,t,v in zip(pairs,truth,values)])


def worker(model_id):
    import numpy as np
    notes, transfer, probes, labels = inputs()
    texts = notes + transfer + labels
    start = time.perf_counter()
    if model_id == 'production-onnx':
        import onnxruntime as ort
        from transformers import BertTokenizer
        tokenizer = BertTokenizer(vocab_file=str(ROOT / 'app/src/main/assets/minilm-vocab.txt'), do_lower_case=True)
        # BERT basic + WordPiece matches the app for this English fixture; validated separately.
        options = ort.SessionOptions()
        options.intra_op_num_threads = 2
        options.inter_op_num_threads = 1
        session = ort.InferenceSession(str(ROOT / 'app/src/main/assets/minilm.onnx'), sess_options=options, providers=['CPUExecutionProvider'])
        def encode(batch):
            results = []
            for text in batch:
                pieces = tokenizer.encode(text, add_special_tokens=False)
                pooled = np.zeros(384, dtype=np.float32)
                for offset in range(0, len(pieces), 254):
                    chunk = pieces[offset:offset+254]
                    ids = np.array([[101] + chunk + [102]], dtype=np.int64)
                    output = session.run(None, dict(input_ids=ids, attention_mask=np.ones_like(ids), token_type_ids=np.zeros_like(ids)))[0]
                    vector = output[0].mean(axis=0)
                    pooled += vector / np.linalg.norm(vector) * len(chunk)
                results.append(pooled / max(float(np.linalg.norm(pooled)), 1e-12))
            return np.array(results)
        lengths = [len(tokenizer.encode(t, add_special_tokens=False)) + 2 for t in texts]
        threads, limit = 2, 'chunked at 256'
    else:
        import torch
        from sentence_transformers import SentenceTransformer
        torch.set_num_threads(4)
        model = SentenceTransformer(str(LOCAL / 'models' / model_id), device='cpu', trust_remote_code=False).float()
        def encode(batch):
            return model.encode(batch, batch_size=16, normalize_embeddings=True, show_progress_bar=False, prompt='')
        lengths = [len(model.tokenizer.encode(t, truncation=False)) for t in texts]
        threads, limit = 4, model.max_seq_length
        assert max(lengths) <= limit, 'Fixture would truncate'
    load_seconds = time.perf_counter() - start
    start = time.perf_counter()
    vectors = encode(texts)
    encoding_seconds = time.perf_counter() - start
    assert np.isfinite(vectors).all()
    assert np.allclose(np.linalg.norm(vectors, axis=1), 1, atol=1e-5)
    encode([notes[0]])
    timings = []
    for i in range(30):
        start = time.perf_counter()
        encode([notes[i % len(notes)]])
        timings.append((time.perf_counter()-start)*1000)
    stats = dict(model=model_id, loadSeconds=load_seconds, encodeSeconds=encoding_seconds, texts=len(texts), textsPerSecond=len(texts)/encoding_seconds, warmSingleMedianMs=float(np.median(timings)), warmSingleP95Ms=float(np.percentile(timings,95)), peakRssMiB=resource.getrusage(resource.RUSAGE_SELF).ru_maxrss/(1024**2 if sys.platform=='darwin' else 1024), maxFixtureTokens=max(lengths), tokenLimit=limit, threads=threads)
    np.save(LOCAL / f'{model_id}-vectors.npy', vectors)
    stats['vectorSha256'] = hashlib.sha256((LOCAL / f'{model_id}-vectors.npy').read_bytes()).hexdigest()
    (LOCAL / f'{model_id}-timing.json').write_text(json.dumps(stats, indent=2))
    print(json.dumps(stats), flush=True)


def main():
    import numpy as np
    notes, transfer, probes, labels = inputs()
    manifest = read('scripts/embeddings/model-manifest.json')
    models = ['production-onnx'] + [m['id'] for m in manifest['models']]
    # Verify pinned bytes before every run; results are never silently reused.
    artifacts = {}
    for m in manifest['models']:
        for entry in m['files']:
            path = LOCAL / 'models' / m['id'] / entry['path']
            with path.open('rb') as handle:
                digest = hashlib.file_digest(handle, 'sha256').hexdigest()
            assert digest == entry['sha256'], f'Artifact changed: {path}'
        artifacts[m['id']] = dict(revision=m['revision'], license=m['license'], weightMiB=sum(f['bytes'] for f in m['files'] if f['path'].endswith('.safetensors'))/1024**2)
    onnx = ROOT / 'app/src/main/assets/minilm.onnx'
    with onnx.open('rb') as handle:
        digest = hashlib.file_digest(handle, 'sha256').hexdigest()
    assert digest == '4278337fd0ff3c68bfb6291042cad8ab363e1d9fbc43dcb499fe91c871902474'
    artifacts['production-onnx'] = dict(sha256=digest, weightMiB=onnx.stat().st_size/1024**2, license='apache-2.0')
    pairs = read('fixtures/grouping-review-pairs.json')
    reference = read('fixtures/hybrid/heldout-review.json')
    positives = sorted({tuple(sorted(p)) for members in reference['subjects'].values() for p in itertools.combinations(members, 2)})
    results = []
    for model_id in models:
        subprocess.run([sys.executable, __file__, '--worker', model_id], check=True, cwd=ROOT)
        v = np.load(LOCAL / f'{model_id}-vectors.npy')
        scope = []
        for p in probes:
            def score(label):
                return float(v[p['note']-1] @ v[len(notes)+len(transfer)+labels.index(label)])
            preferred, rejected = score(p['preferred']), score(p['rejected'])
            scope.append(dict(**p, preferredCosine=preferred, rejectedCosine=rejected, margin=preferred-rejected))
        results.append(dict(**read(f'.local/embeddings/{model_id}-timing.json'), artifacts=artifacts[model_id], development=pair_metrics(v[:len(notes)], pairs['positive'], pairs['negative']), transfer=pair_metrics(v[len(notes):len(notes)+len(transfer)], positives, reference['negativePairs']), scopeProbes=scope))
    reproduction = {name: hashlib.sha256((ROOT / name).read_bytes()).hexdigest() for name in ['scripts/embeddings/benchmark.py', 'scripts/embeddings/requirements.txt', 'scripts/embeddings/model-manifest.json', 'app/src/main/assets/minilm-vocab.txt']}
    output = dict(platform=platform.platform(), machine=platform.machine(), python=platform.python_version(), reproductionHashes=reproduction, inputSha256=hashlib.sha256(json.dumps(inputs(),sort_keys=True).encode()).hexdigest(), limitations='Small observed development fixtures; no held-out accuracy, no Android performance, scope replacements include proposals.', results=results)
    (ROOT / 'docs/EMBEDDING_COMPARISON_RESULTS.json').write_text(json.dumps(output, indent=2)+'\n')
    lines = ['# Laptop embedding comparison', '', output['limitations'], '', f"Environment: {output['platform']}; Python {output['python']}. CPU only. FP32 candidates: 4 threads, batch 16. Bundled ONNX: 2 threads, sequential per-note app pooling/chunking. Timing includes Python runtime overhead; one run, not a controlled mobile comparison.", '', '| Model | Weight MiB | Dev AUC / AP | Dev recall@5 / @10 | Transfer AUC / AP | Transfer recall@5 / @10 | Scope margins > 0 | Warm median / p95 ms | Peak RSS MiB |', '|---|---:|---|---|---|---|---:|---|---:|']
    for r in results:
        d,t=r['development'],r['transfer']
        lines.append(f"| {r['model']} | {r['artifacts']['weightMiB']:.1f} | {d['rocAuc']:.3f} / {d['averagePrecision']:.3f} | {d['knownPositiveRecall']['5']:.3f} / {d['knownPositiveRecall']['10']:.3f} | {t['rocAuc']:.3f} / {t['averagePrecision']:.3f} | {t['knownPositiveRecall']['5']:.3f} / {t['knownPositiveRecall']['10']:.3f} | {sum(p['margin']>0 for p in r['scopeProbes'])}/{len(probes)} | {r['warmSingleMedianMs']:.1f} / {r['warmSingleP95Ms']:.1f} | {r['peakRssMiB']:.0f} |")
    lines += ['', 'Scope counts are diagnostic contrasts, not classification accuracy. Unknown note pairs are excluded from pair metrics, not treated as negatives. Recall counts directed known-positive links, excluding self matches.', '', '## Scope contrasts', '', '| Note | Preferred / rejected (some preferred labels are proposed) | ' + ' | '.join(models) + ' |', '|---|---|'+'---:|'*len(models)]
    for i,p in enumerate(probes):
        lines.append(f"| {p['note']} | {p['preferred']} / {p['rejected']} | " + ' | '.join(f"{r['scopeProbes'][i]['margin']:+.3f}" for r in results) + ' |')
    lines += ['', 'Positive margin means greater cosine similarity to the preferred label. Full scores, evidence qualifiers, pair scores, pinned artifacts and timings are in [results JSON](EMBEDDING_COMPARISON_RESULTS.json). See [protocol](EMBEDDING_BENCHMARK_PROTOCOL.md) for licensing and evaluation constraints.', '']
    (ROOT / 'docs/EMBEDDING_COMPARISON.md').write_text('\n'.join(lines))


if __name__ == '__main__':
    if len(sys.argv)>1 and sys.argv[1]=='--worker':
        worker(sys.argv[2])
    else:
        main()
