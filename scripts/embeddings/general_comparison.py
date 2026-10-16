#!/usr/bin/env python3
"""General clustering shortlist: no authored scopes, rival graph, exclusions or note-specific logic."""
import hashlib
import itertools
import json
from pathlib import Path
import numpy as np
from sklearn.cluster import HDBSCAN
from sklearn.feature_extraction.text import CountVectorizer
from benchmark import ROOT, LOCAL, inputs, read
from grouping_experiment import cosine, discover, evaluate, edges, jaccard, THRESHOLDS
from clustering_util import consolidate


def density(vectors, ids, minimum, samples, method):
    order=sorted(range(len(ids)),key=lambda i:ids[i])
    ids=[ids[i] for i in order]
    classes=HDBSCAN(min_cluster_size=minimum,min_samples=samples,cluster_selection_method=method,metric='euclidean',n_jobs=1).fit_predict(vectors[order])
    return sorted([frozenset(ids[i] for i in range(len(ids)) if classes[i]==c) for c in sorted(set(classes)) if c>=0],key=lambda g:tuple(sorted(g)))


def generate(vectors,ids,family,parameters):
    if family=='density':
        return density(vectors,ids,parameters['minClusterSize'],parameters['minSamples'],parameters['method'])
    matrix=cosine(vectors)
    groups=discover(matrix,ids,family,parameters['threshold'])
    if family=='overlapping-cores':
        # consolidate expects original full-corpus ID positions.
        n=max(ids,default=0)
        full=np.zeros((n,n))
        full[np.ix_([i-1 for i in ids],[i-1 for i in ids])]=matrix
        groups=consolidate(groups,full,parameters['threshold'],.5)
    return groups


def representations(texts,groups,vectors):
    """Shared c-TF-IDF-like lexical extraction plus representative notes; never supplied category names."""
    if not groups:
        return []
    try:
        counts=CountVectorizer(stop_words='english',ngram_range=(1,2),min_df=2).fit(texts)
    except ValueError:
        return [dict(members=sorted(g),terms=[],representatives=sorted(g)[:3]) for g in groups]
    per_note=counts.transform(texts)
    vocabulary=counts.get_feature_names_out()
    aggregated=np.vstack([np.asarray(per_note[[i-1 for i in sorted(g)]].sum(axis=0)).ravel() for g in groups]).astype(float)
    lengths=aggregated.sum(axis=1)
    tf=np.divide(aggregated,lengths[:,None],out=np.zeros_like(aggregated),where=lengths[:,None]!=0)
    idf=np.log1p(lengths.mean()/np.maximum(aggregated.sum(axis=0),1))
    weights=tf*idf
    topics=[]
    for group,row in zip(groups,weights):
        members=sorted(group)
        top=sorted((i for i in range(len(vocabulary)) if row[i]>0),key=lambda i:(-row[i],vocabulary[i]))[:5]
        centroid=vectors[np.array(members)-1].mean(axis=0)
        center=centroid/max(float(np.linalg.norm(centroid)),1e-12)
        exemplar=sorted(members,key=lambda i:(-float(np.sum(vectors[i-1]*center)),i))[:3]
        topics.append(dict(members=members,terms=[dict(text=str(vocabulary[i]),weight=float(row[i])) for i in top],representatives=exemplar))
    return topics


def main():
    notes,transfer,_,_=inputs()
    baseline=read('docs/EMBEDDING_COMPARISON_RESULTS.json')
    assert baseline['inputSha256']==hashlib.sha256(json.dumps(inputs(),sort_keys=True).encode()).hexdigest()
    pairs=read('fixtures/grouping-review-pairs.json')
    transfer_review=read('fixtures/hybrid/heldout-review.json')
    positives=sorted({tuple(sorted(p)) for members in transfer_review['subjects'].values() for p in itertools.combinations(members,2)})
    results=[]
    for record in baseline['results']:
        model=record['model']
        path=LOCAL/f'{model}-vectors.npy'
        assert hashlib.sha256(path.read_bytes()).hexdigest()==record['vectorSha256']
        vectors=np.load(path)
        dev=vectors[:len(notes)]
        target=vectors[len(notes):len(notes)+len(transfer)]
        for family in ('density','complete-link','overlapping-cores'):
            parameters=([dict(minClusterSize=m,minSamples=s,method=c) for m,s,c in itertools.product((2,3,5),(2,3,5),('eom','leaf'))] if family=='density' else [dict(threshold=t) for t in THRESHOLDS])
            sweep=[]
            for p in parameters:
                groups=generate(dev,list(range(1,len(notes)+1)),family,p)
                sweep.append(dict(parameters=p,**evaluate(groups,pairs['positive'],pairs['negative'],len(notes))))
            best=max(sweep,key=lambda r:(r['selectionScore'],-r['negativeHits'],-r['largestTopic'],-r['topics'],json.dumps(r['parameters'],sort_keys=True)))
            p=best['parameters']
            groups=generate(dev,list(range(1,len(notes)+1)),family,p)
            transferred=generate(target,list(range(1,len(transfer)+1)),family,p)
            prefix_size=int(len(notes)*.8)
            prefix=generate(dev[:prefix_size],list(range(1,prefix_size+1)),family,p)
            original_edges=edges(groups)
            restricted={pair for pair in original_edges if max(pair)<=prefix_size}
            keep=[i for i in range(1,len(notes)+1) if i%5]
            removed=generate(dev[np.array(keep)-1],keep,family,p)
            retained={pair for pair in original_edges if all(i in keep for i in pair)}
            permutation=np.random.default_rng(42).permutation(len(notes))
            permuted=generate(dev[permutation],[int(i)+1 for i in permutation],family,p)
            assert groups==permuted
            results.append(dict(model=model,family=family,selected=best,transfer=evaluate(transferred,positives,transfer_review['negativePairs'],len(transfer)),prefixPairStability=jaccard(edges(prefix),restricted),removalPairStability=jaccard(edges(removed),retained),permutationStable=True,sweep=sweep,topics=representations(notes,groups,dev),transferTopics=representations(transfer,transferred,target)))
            print(model,family,best['positiveHits'],best['negativeHits'],'transfer',results[-1]['transfer']['positiveHits'],results[-1]['transfer']['negativeHits'],flush=True)
    output=dict(date='2026-09-30',boundary='General algorithms only. Review pairs are used for model-level development calibration and evaluation, never as exceptions or scope logic. Observed transfer data is a regression set, not an untouched benchmark. Supplied scope descriptions, rival relations, rejected label names and scope probes are not consulted by clustering or labeling.',labeling='Shared c-TF-IDF-like term ranking: aggregate per-note unigram/bigram counts, normalize class term frequencies, log(1 + average class token count / total class term count). Standard English stopwords and min document frequency 2, no custom excluded words. Overlapping memberships duplicate counts across classes. Top five terms and three centroid-nearest members are review artifacts, not validated final topic names.',sourceInputSha256=baseline['inputSha256'],sourceVectorSha256={r['model']:r['vectorSha256'] for r in baseline['results']},scriptSha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),results=results)
    (ROOT/'docs/GENERAL_GROUPING_RESULTS.json').write_text(json.dumps(output,indent=2)+'\n')
    lines=['# General processing approach comparison','',output['boundary'],'','All families use the same corpus-derived term extraction and representative-note selection. Models keep their existing commercial-use eligibility. Density clustering uses Euclidean distance on normalized embeddings with no dimensionality reduction. Complete-link requires every within-group pair to clear its threshold. Overlapping reciprocal top-5 cores use the same complete-pair requirement and generic overlap consolidation (Jaccard 0.5).','','86 configurations per model: 18 density settings plus 34 settings for each other family. Selection uses development positive-pair recall minus explicit-negative co-membership rate. Unknown pairs are not negatives. The selected configuration transfers unchanged. Rankings are development descriptions, not evidence of statistically established superiority or label quality.','','| Model | Family | Parameters | Dev positive / negative | Transfer positive / negative | Topics / largest | Coverage | Prefix / removal stability |','|---|---|---|---:|---:|---|---:|---|']
    for r in results:
        d,t=r['selected'],r['transfer']
        lines.append(f"| {r['model']} | {r['family']} | {json.dumps(d['parameters'],sort_keys=True)} | {d['positiveHits']}/33 / {d['negativeHits']}/13 | {t['positiveHits']}/69 / {t['negativeHits']}/5 | {d['topics']} / {d['largestTopic']} | {d['coverage']:.0%} | {r['prefixPairStability']:.3f} / {r['removalPairStability']:.3f} |")
    lines += ['','Every selected configuration passed the fixed-seed input-permutation check. Stability measures compare co-member pairs among retained notes; saved topic identity is separate. High coverage is not necessarily good; density noise is allowed. The transfer fixture has only five explicit negatives, so zero hits there is weak evidence.','','## Representative output for review','','These are extracted term candidates, not user-approved labels. All memberships and terms are in [JSON](GENERAL_GROUPING_RESULTS.json).']
    for r in results:
        if r['model'] in ('bge-small','granite-small'):
            lines += ['',f"### {r['model']} / {r['family']}", '']
            for topic in sorted(r['topics'],key=lambda t:(-len(t['members']),t['members']))[:5]:
                lines.append(f"- {len(topic['members'])} notes; terms: {', '.join(t['text'] for t in topic['terms'])}; representatives: {', '.join(map(str,topic['representatives']))}.")
    lines += ['', 'Method references: [scikit-learn HDBSCAN](https://scikit-learn.org/stable/modules/generated/sklearn.cluster.HDBSCAN.html), [BERTopic class-based TF-IDF](https://maartengr.github.io/BERTopic/getting_started/ctfidf/ctfidf.html). This experiment uses scikit-learn and a small lexical implementation, not the full BERTopic package or its default UMAP pipeline.', '']
    (ROOT/'docs/GENERAL_GROUPING_COMPARISON.md').write_text('\n'.join(lines))


if __name__=='__main__':
    main()
