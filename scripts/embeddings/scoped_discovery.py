#!/usr/bin/env python3
"""Controlled supplied-scope experiment; not open-world label discovery."""
import hashlib
import itertools
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import numpy as np
from benchmark import ROOT, LOCAL, inputs, read
from grouping_experiment import cosine, discover, edges, evaluate, jaccard
from clustering_util import consolidate

os.environ.update(HF_HUB_OFFLINE='1', TRANSFORMERS_OFFLINE='1', HF_HUB_DISABLE_TELEMETRY='1', TOKENIZERS_PARALLELISM='false')
MODELS = ('minilm','bge-small','granite-small')


def label_and_validate(groups, score_matrix, labels, margin=0, split=False):
    """Choose scope by mean + weak-quarter agreement; prune independently of calibration labels."""
    topics=[]
    for group in groups:
        ids=sorted(group)
        scores=score_matrix[np.array(ids)-1]
        quarter=max(1,int(np.ceil(len(ids)*.25)))
        agreement=.65*scores.mean(axis=0)+.35*np.sort(scores,axis=0)[:quarter].mean(axis=0)
        candidates=range(len(labels)) if split else [int(np.argmax(agreement))]
        for label_index in candidates:
            other=np.max(np.delete(scores,label_index,axis=1),axis=1)
            members=[i for i,accepted in zip(ids,scores[:,label_index]-other>=margin-1e-12) if accepted]
            if len(members)>=2:
                topics.append(dict(label=labels[label_index],members=members,sourceMembers=ids))
    return topics


def scope_metrics(topics, probes):
    memberships={(t['label'],i) for t in topics for i in t['members']}
    details=[dict(**p,preferredPresent=(p['preferred'],p['note']) in memberships,rejectedPresent=(p['rejected'],p['note']) in memberships) for p in probes]
    return dict(preferredPresent=sum(p['preferredPresent'] for p in details),rejectedPresent=sum(p['rejectedPresent'] for p in details),total=len(details),details=details)


def worker(model):
    import torch
    from sentence_transformers import SentenceTransformer
    manifest=read('scripts/embeddings/model-manifest.json')
    record=next(m for m in manifest['models'] if m['id']==model)
    for entry in record['files']:
        with (LOCAL/'models'/model/entry['path']).open('rb') as f:
            assert hashlib.file_digest(f,'sha256').hexdigest()==entry['sha256']
    torch.set_num_threads(4)
    network=SentenceTransformer(str(LOCAL/'models'/model),device='cpu',trust_remote_code=False).float()
    notes,_,_,_=inputs()
    scopes=read('fixtures/embedding-scope-descriptions.json')['labels']
    labels=sorted(scopes)
    focus=[re.split(r'(?<=[.!?])\s+|\n',t,maxsplit=1)[0].strip() for t in notes]
    texts=notes+focus+[scopes[label] for label in labels]
    assert max(len(network.tokenizer.encode(t,truncation=False)) for t in texts)<=network.max_seq_length
    vectors=network.encode(texts,batch_size=16,normalize_embeddings=True,show_progress_bar=False,prompt='')
    n=len(notes)
    assert np.isfinite(vectors).all() and np.allclose(np.linalg.norm(vectors,axis=1),1,atol=1e-5)
    similarity=cosine(vectors)
    scores=.5*similarity[:n,2*n:]+.5*similarity[n:2*n,2*n:]
    np.savez(LOCAL/f'{model}-scoped.npz',noteVectors=vectors[:n],scores=scores)


def main():
    notes,transfer,probes,_=inputs()
    pairs=read('fixtures/grouping-review-pairs.json')
    prior=read('docs/EMBEDDING_GROUPING_RESULTS.json')
    assert prior['sourceInputSha256']==hashlib.sha256(json.dumps(inputs(),sort_keys=True).encode()).hexdigest()
    labels=sorted(read('fixtures/embedding-scope-descriptions.json')['labels'])
    transfer_review=read('fixtures/hybrid/heldout-review.json')
    transfer_positive=sorted({tuple(sorted(p)) for members in transfer_review['subjects'].values() for p in itertools.combinations(members,2)})
    consolidated_results=[]
    scope_results=[]
    for model in MODELS:
        baseline=next(r for r in read('docs/EMBEDDING_COMPARISON_RESULTS.json')['results'] if r['model']==model)
        vector_path=LOCAL/f'{model}-vectors.npy'
        assert hashlib.sha256(vector_path.read_bytes()).hexdigest()==prior['sourceVectorSha256'][model]==baseline['vectorSha256']
        all_vectors=np.load(vector_path)
        development=cosine(all_vectors[:len(notes)])
        transferred=cosine(all_vectors[len(notes):len(notes)+len(transfer)])
        previous=next(r for r in prior['results'] if r['model']==model and r['policy']=='overlapping-cores')
        threshold=previous['selected']['threshold']
        original=discover(development,list(range(1,len(notes)+1)),'overlapping-cores',threshold)
        original_transfer=discover(transferred,list(range(1,len(transfer)+1)),'overlapping-cores',threshold)
        for overlap in (.5,.75):
            groups=consolidate(original,development,threshold,overlap)
            transfer_groups=consolidate(original_transfer,transferred,threshold,overlap)
            assert edges(original)<=edges(groups) and edges(original_transfer)<=edges(transfer_groups)
            consolidated_results.append(dict(model=model,overlapThreshold=overlap,coreThreshold=threshold,originalTopics=len(original),development=evaluate(groups,pairs['positive'],pairs['negative'],len(notes)),transfer=evaluate(transfer_groups,transfer_positive,transfer_review['negativePairs'],len(transfer)),groups=[sorted(g) for g in groups],transferGroups=[sorted(g) for g in transfer_groups]))
        subprocess.run([sys.executable,__file__,'--worker',model],check=True,cwd=ROOT)
        data=np.load(LOCAL/f'{model}-scoped.npz')
        assert np.allclose(data['noteVectors'],all_vectors[:len(notes)],atol=2e-6), 'Different representation; refresh vectors'
        scores=data['scores']
        for policy in ('complete-link','overlapping-cores'):
            chosen=next(r for r in prior['results'] if r['model']==model and r['policy']==policy)
            threshold=chosen['selected']['threshold']
            groups=discover(development,list(range(1,len(notes)+1)),policy,threshold)
            if policy=='overlapping-cores':
                groups=consolidate(groups,development,threshold,.5)
            for split in (False,True):
                for margin in (0,.025,.05):
                    topics=label_and_validate(groups,scores,labels,margin,split)
                    accepted=[frozenset(t['members']) for t in topics]
                    # Recombine same-label groups only if union meets the original core threshold.
                    merged=[]
                    for label in labels:
                        same=[frozenset(t['members']) for t in topics if t['label']==label]
                        merged.extend(dict(label=label,members=sorted(g)) for g in consolidate(same,development,threshold,.5))
                    assert edges(accepted)<=edges([frozenset(t['members']) for t in merged])
                    scope_results.append(dict(model=model,policy=policy,validation='split-scopes' if split else 'one-scope',margin=margin,coreThreshold=threshold,sourceTopics=len(groups),development=evaluate([set(t['members']) for t in merged],pairs['positive'],pairs['negative'],len(notes)),scope=scope_metrics(merged,probes),topics=merged))
        print(model,'consolidation and scope tests complete',flush=True)
    output=dict(date='2026-09-30',boundary='Scope stage uses 16 supplied development labels for the whole notebook; incomplete vocabulary, no open-world discovery or transfer naming claim. Groups require 2 notes; no positive/negative annotation is read by worker/validation/consolidation. Core thresholds inherited from prior development-selected runs. Margins 0/.025/.05 and overlap .5/.75 are reported exhaustively, not selected on transfer.',scriptSha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),scopeFixtureSha256=hashlib.sha256((ROOT/'fixtures/embedding-scope-descriptions.json').read_bytes()).hexdigest(),sourceVectorSha256=prior['sourceVectorSha256'],scopeVectorSha256={m:hashlib.sha256((LOCAL/f'{m}-scoped.npz').read_bytes()).hexdigest() for m in MODELS},consolidation=consolidated_results,scoped=scope_results)
    (ROOT/'docs/EMBEDDING_SCOPED_RESULTS.json').write_text(json.dumps(output,indent=2)+'\n')
    lines=['# Coherent cores, consolidation and scope validation','',output['boundary'],'','## Unlabeled consolidation','','A merge requires membership Jaccard at least the chosen overlap AND every pair in the union clearing the original model/core threshold. Original co-member pairs are retained; added pairs can be false. No transfer-based retuning.','','| Model | Overlap | Topics before → after | Dev positive / negative hits | Transfer positive / negative hits |','|---|---:|---:|---:|---:|']
    for r in consolidated_results:
        d,t=r['development'],r['transfer']
        lines.append(f"| {r['model']} | {r['overlapThreshold']} | {r['originalTopics']} → {d['topics']} | {d['positiveHits']}/33 / {d['negativeHits']}/13 | {t['positiveHits']}/69 / {t['negativeHits']}/5 |")
    lines += ['','## Supplied-scope membership validation','','One-scope chooses the description with strongest 65% mean / 35% weakest-quarter member agreement. Split-scopes instead partitions each core into individually supported scopes. Note evidence averages full text and first sentence. A member must prefer its description over every other supplied scope by the margin; topics with fewer than two accepted notes are suppressed. Same-label consolidation also requires overlap and complete-pair coherence. This is a controlled test of label support, not proof of a complete product taxonomy.','','| Model | Core policy | Validation | Margin | Topics | Coverage | Dev positive / negative hits | Preferred scope present / 15 | Rejected scope present / 15 |','|---|---|---|---:|---:|---:|---:|---:|---:|']
    for r in scope_results:
        d,s=r['development'],r['scope']
        lines.append(f"| {r['model']} | {r['policy']} | {r['validation']} | {r['margin']:.3f} | {d['topics']} | {d['coverage']:.0%} | {d['positiveHits']}/33 / {d['negativeHits']}/13 | {s['preferredPresent']} | {s['rejectedPresent']} |")
    lines += ['','Unknown memberships are not negatives. The 15 scope cases include proposed preferred labels and ambiguous socks IDs. Removing all topics would score zero rejected scopes: always consider positive retention and coverage alongside exclusions. The supplied vocabulary misses many notebook subjects, so whole-notebook losses here do not isolate a model weakness. Models retain the existing MIT/Apache commercial-use evidence.','','Full per-note scope outcomes, groups and source hashes: [JSON](EMBEDDING_SCOPED_RESULTS.json).','']
    (ROOT/'docs/EMBEDDING_SCOPED_COMPARISON.md').write_text('\n'.join(lines))


if __name__=='__main__':
    if len(sys.argv)>1 and sys.argv[1]=='--worker':
        worker(sys.argv[2])
    else:
        main()
