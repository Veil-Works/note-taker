#!/usr/bin/env python3
"""Offline development comparisons; tuning and transfer scores are reported separately."""
import hashlib
import itertools
import json
import os
import re
from pathlib import Path
import subprocess
import sys

from benchmark import ROOT, LOCAL, inputs, read
os.environ.update(HF_HUB_OFFLINE='1', TRANSFORMERS_OFFLINE='1', HF_HUB_DISABLE_TELEMETRY='1', TOKENIZERS_PARALLELISM='false')
POLICIES = ('mutual-components', 'complete-link', 'overlapping-cores')
THRESHOLDS = tuple(round(.15 + .025*i, 3) for i in range(34))


def cosine(v):
    import numpy as np
    return np.clip(np.einsum('ik,jk->ij', v.astype(np.float64), v.astype(np.float64), optimize=False), -1, 1)


def discover(matrix, ids, policy, threshold, k=5):
    """Return canonical 1-based ID sets. Input order never acts as a tie breaker."""
    import numpy as np
    ordering = sorted(range(len(ids)), key=lambda i: ids[i])
    ids = [ids[i] for i in ordering]
    s = matrix[np.ix_(ordering, ordering)]
    if len(ids) < 2:
        return []
    if policy == 'complete-link':
        from scipy.cluster.hierarchy import linkage, fcluster
        from scipy.spatial.distance import squareform
        distance = np.clip(1-s, 0, 2)
        np.fill_diagonal(distance, 0)
        classes = fcluster(linkage(squareform(distance, checks=True), method='complete'), t=1-threshold, criterion='distance')
        groups = [{ids[i] for i in range(len(ids)) if classes[i]==c} for c in sorted(set(classes))]
    else:
        neighbors = [set(sorted((j for j in range(len(ids)) if j != i and s[i,j]>=threshold), key=lambda j:(-s[i,j], ids[j]))[:k]) for i in range(len(ids))]
        mutual = [{j for j in neighbors[i] if i in neighbors[j]} for i in range(len(ids))]
        if policy == 'mutual-components':
            seen, groups = set(), []
            for seed in range(len(ids)):
                if seed in seen:
                    continue
                stack, component = [seed], set()
                while stack:
                    i = stack.pop()
                    if i in component:
                        continue
                    component.add(i)
                    stack.extend(mutual[i]-component)
                seen.update(component)
                groups.append({ids[i] for i in component})
        elif policy == 'overlapping-cores':
            groups = []
            for seed in range(len(ids)):
                core = {seed}
                for j in sorted(mutual[seed], key=lambda j:(-s[seed,j], ids[j])):
                    if all(s[j,i]>=threshold for i in core):
                        core.add(j)
                groups.append({ids[i] for i in core})
        else:
            raise ValueError(policy)
    unique = {frozenset(g) for g in groups if len(g)>=2}
    maximal = [g for g in unique if not any(g < other for other in unique)]
    return sorted(maximal, key=lambda g:tuple(sorted(g)))


def edges(groups):
    return {tuple(p) for g in groups for p in itertools.combinations(sorted(g), 2)}


def evaluate(groups, positives, negatives, n):
    positive = {tuple(sorted(p)) for p in positives}
    negative = {tuple(sorted(p)) for p in negatives}
    assert not positive & negative
    e = edges(groups)
    hits, errors = len(e & positive), len(e & negative)
    memberships = [sum(i in g for g in groups) for i in range(1,n+1)]
    return dict(positiveHits=hits, positiveTotal=len(positive), positiveRecall=hits/len(positive), negativeHits=errors, negativeTotal=len(negative), negativeFalsePositiveRate=errors/len(negative), selectionScore=hits/len(positive)-errors/len(negative), topics=len(groups), coverage=sum(x>0 for x in memberships)/n, overlappingNotes=sum(x>1 for x in memberships), largestTopic=max(map(len,groups), default=0))


def jaccard(a,b):
    return len(a & b)/len(a | b) if a | b else 1.0


def description_worker(model_id):
    import numpy as np
    import torch
    from sentence_transformers import SentenceTransformer
    torch.set_num_threads(4)
    descriptions = read('fixtures/embedding-scope-descriptions.json')['labels']
    notes, _, probes, labels = inputs()
    assert set(labels)==set(descriptions)
    model = SentenceTransformer(str(LOCAL/'models'/model_id), device='cpu', trust_remote_code=False).float()
    focuses = [re.split(r'(?<=[.!?])\s+|\n', text, maxsplit=1)[0].strip() for text in notes]
    texts = notes + labels + [descriptions[label] for label in labels] + focuses
    lengths = [len(model.tokenizer.encode(t,truncation=False)) for t in texts]
    assert max(lengths)<=model.max_seq_length
    v = model.encode(texts,batch_size=16,normalize_embeddings=True,show_progress_bar=False,prompt='')
    scores = cosine(v)
    rows = []
    for p in probes:
        a,b = labels.index(p['preferred']),labels.index(p['rejected'])
        i=p['note']-1
        bare=float(scores[i,len(notes)+a]-scores[i,len(notes)+b])
        preferred=float(scores[i,len(notes)+len(labels)+a])
        rejected=float(scores[i,len(notes)+len(labels)+b])
        focus_index = len(notes)+2*len(labels)+i
        focus_margin = float(scores[focus_index,len(notes)+len(labels)+a]-scores[focus_index,len(notes)+len(labels)+b])
        rows.append(dict(**p,bareMargin=bare,descriptionMargin=preferred-rejected,preferredDescriptionCosine=preferred,rejectedDescriptionCosine=rejected,firstSentence=focuses[i],firstSentenceDescriptionMargin=focus_margin,mixedFocusDescriptionMargin=.5*(preferred-rejected)+.5*focus_margin))
    (LOCAL/f'{model_id}-descriptions.json').write_text(json.dumps(rows,indent=2)+'\n')


def main():
    import numpy as np
    notes, transfer, probes, labels = inputs()
    baseline = read('docs/EMBEDDING_COMPARISON_RESULTS.json')
    assert baseline['inputSha256']==hashlib.sha256(json.dumps(inputs(),sort_keys=True).encode()).hexdigest(), 'Refresh benchmark inputs'
    manifest_path='scripts/embeddings/model-manifest.json'
    assert baseline['reproductionHashes'][manifest_path]==hashlib.sha256((ROOT/manifest_path).read_bytes()).hexdigest()
    pairs = read('fixtures/grouping-review-pairs.json')
    review = read('fixtures/hybrid/heldout-review.json')
    transfer_pairs = sorted({tuple(sorted(p)) for members in review['subjects'].values() for p in itertools.combinations(members,2)})
    results = []
    for record in baseline['results']:
        model=record['model']
        path=LOCAL/f'{model}-vectors.npy'
        assert hashlib.sha256(path.read_bytes()).hexdigest()==record['vectorSha256'], 'Refresh vectors'
        vectors=np.load(path)
        assert vectors.shape[0]==len(notes)+len(transfer)+len(labels)
        assert np.isfinite(vectors).all() and np.allclose(np.linalg.norm(vectors,axis=1),1,atol=1e-5)
        development=cosine(vectors[:len(notes)])
        transferred=cosine(vectors[len(notes):len(notes)+len(transfer)])
        for policy in POLICIES:
            sweep=[]
            for threshold in THRESHOLDS:
                groups=discover(development,list(range(1,len(notes)+1)),policy,threshold)
                sweep.append(dict(threshold=threshold,**evaluate(groups,pairs['positive'],pairs['negative'],len(notes))))
            # Tune exclusively on recorded development pairs. Fixed k=5; no transfer tuning.
            best=max(sweep,key=lambda r:(r['selectionScore'],-r['negativeHits'],r['threshold']))
            threshold=best['threshold']
            groups=discover(development,list(range(1,len(notes)+1)),policy,threshold)
            transfer_groups=discover(transferred,list(range(1,len(transfer)+1)),policy,threshold)
            e=edges(groups)
            prefix_size=int(len(notes)*.8)
            prefix=discover(development[:prefix_size,:prefix_size],list(range(1,prefix_size+1)),policy,threshold)
            full_restricted={p for p in e if max(p)<=prefix_size}
            removed={i for i in range(1,len(notes)+1) if i%5==0}
            retained=[i for i in range(1,len(notes)+1) if i not in removed]
            reduced=discover(development[np.ix_([i-1 for i in retained],[i-1 for i in retained])],retained,policy,threshold)
            retained_edges={p for p in e if not set(p)&removed}
            rng=np.random.default_rng(42)
            permutation=rng.permutation(len(notes))
            shuffled=discover(development[np.ix_(permutation,permutation)],[int(i)+1 for i in permutation],policy,threshold)
            assert groups==shuffled, 'Insertion-order instability'
            diagnostic_pairs=[(65,120),(35,39),(35,108),(72,57),(161,57),(106,129),(93,136),(147,151),(63,151)]
            results.append(dict(model=model,policy=policy,selected=best,sweep=sweep,transfer=evaluate(transfer_groups,transfer_pairs,review['negativePairs'],len(transfer)),prefix80PairJaccard=jaccard(edges(prefix),full_restricted),removeEveryFifthPairJaccard=jaccard(edges(reduced),retained_edges),permutationStable=True,reviewContrasts=[dict(pair=list(p),sameGroup=tuple(sorted(p)) in e) for p in diagnostic_pairs],developmentGroups=[sorted(g) for g in groups],transferGroups=[sorted(g) for g in transfer_groups]))
            print(model,policy,'threshold',threshold,'dev',best['positiveHits'],best['negativeHits'],'transfer',results[-1]['transfer']['positiveHits'],results[-1]['transfer']['negativeHits'],flush=True)
    descriptions={}
    for model in ('minilm','bge-small','granite-small'):
        subprocess.run([sys.executable,__file__,'--descriptions',model],check=True,cwd=ROOT)
        descriptions[model]=read(f'.local/embeddings/{model}-descriptions.json')
    result=dict(date='2026-09-30',parameters=dict(policies=POLICIES,thresholdGrid=THRESHOLDS,mutualK=5,selection='Development positive-pair recall minus explicit-negative co-membership rate; tie: fewer negatives then larger threshold',stability='Jaccard of co-member pairs among retained notes; not identity reconciliation'),sourceInputSha256=baseline['inputSha256'],sourceVectorSha256={r['model']:r['vectorSha256'] for r in baseline['results']},descriptionFixtureSha256=hashlib.sha256((ROOT/'fixtures/embedding-scope-descriptions.json').read_bytes()).hexdigest(),scriptSha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),results=results,descriptionContrasts=descriptions)
    (ROOT/'docs/EMBEDDING_GROUPING_RESULTS.json').write_text(json.dumps(result,indent=2)+'\n')
    lines=['# Laptop grouping and label-scope experiments','', 'Run 2026-09-30. Observed development/regression data only. These Python policies are candidate algorithms, not a port or evaluation of the Kotlin production discovery pipeline. No app changes or Android runs.', '', '## Grouping comparison', '', 'Each policy/model chooses its threshold from 0.150–0.975 (step 0.025) using **development positive-pair recall minus explicit-negative co-membership rate**. The initial 0.30–0.80 grid placed some optima on its boundary, so it was widened before drawing conclusions. This criterion gives equal aggregate weight to the two reviewed classes. It is not production precision. Unknown pairs are excluded from scoring. The selected threshold is then applied unchanged to the observed 40-note transfer corpus.', '', '| Model | Policy | Threshold | Dev positive hits / 33 | Dev negative hits / 13 | Transfer positive hits / 69 | Transfer negative hits / 5 | Topics / largest | Coverage | Prefix pair stability | Removal pair stability |','|---|---|---:|---:|---:|---:|---:|---|---:|---:|---:|']
    assert len(transfer_pairs)==69
    for r in results:
        d,t=r['selected'],r['transfer']
        lines.append(f"| {r['model']} | {r['policy']} | {d['threshold']:.3f} | {d['positiveHits']} | {d['negativeHits']} | {t['positiveHits']} | {t['negativeHits']} | {d['topics']} / {d['largestTopic']} | {d['coverage']:.0%} | {r['prefix80PairJaccard']:.3f} | {r['removeEveryFifthPairJaccard']:.3f} |")
    lines += ['', '- Mutual components: connected components of reciprocal top-5 neighbors above threshold. Can chain across weak endpoints.', '- Complete link: disjoint agglomerative clusters; every within-cluster pair clears threshold. Singletons are left ungrouped.', '- Overlapping cores: deterministic greedy complete-pair cores from each reciprocal-neighbor seed; remove duplicates/subsets. A note may appear in multiple topics. No expansion or lexical rules.', '', 'Coverage counts notes in a topic of at least two notes. Stability compares co-member pairs after adding the last 20% of notes or removing every fifth note, restricted to notes present in both runs; higher is more stable. Every selected configuration passed a fixed-seed input-permutation check. This does not test saved topic IDs.', '', '## Label descriptions and focus', '', 'Scope descriptions were authored from the reported errors before this inference. They are development interventions, not a discovered taxonomy or independent generalization test. FP32 MiniLM, BGE and Granite only; the quantized production representation is not included in this description comparison.', '', 'Each cell gives **bare label → scope description → scope description with focus**. Focus averages whole-note and first-sentence description margins equally. Sentence selection is a simple punctuation/newline rule, not a complete port of the app focus heuristics. A one-sentence note receives no additional focus signal.', '', '| Note | Preferred / rejected | MiniLM | BGE | Granite |','|---|---|---:|---:|---:|']
    for i,p in enumerate(probes):
        cells=[f"{descriptions[m][i]['bareMargin']:+.3f} → {descriptions[m][i]['descriptionMargin']:+.3f} → {descriptions[m][i]['mixedFocusDescriptionMargin']:+.3f}" for m in descriptions]
        lines.append(f"| {p['note']} | {p['preferred']} / {p['rejected']} | "+' | '.join(cells)+' |')
    lines += ['', 'Positive margin favors the preferred scope. Neither sign nor cosine magnitude is calibrated membership probability. Some preferred names remain analyst proposals, and socks note identification remains ambiguous.', '', 'Full threshold sweeps, memberships, review-pair diagnostics, stability measures and description cosines are in [results JSON](EMBEDDING_GROUPING_RESULTS.json). All models retain the commercially permissive licenses documented in [license evidence](embedding-licenses/README.md).', '']
    (ROOT/'docs/EMBEDDING_GROUPING_COMPARISON.md').write_text('\n'.join(lines))


if __name__=='__main__':
    if len(sys.argv)>1 and sys.argv[1]=='--descriptions':
        description_worker(sys.argv[2])
    else:
        main()
