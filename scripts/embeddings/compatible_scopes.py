#!/usr/bin/env python3
"""Controlled compatible-scope comparison using verified existing local vectors."""
import hashlib
import itertools
import json
from pathlib import Path
import numpy as np
from benchmark import ROOT, LOCAL, inputs, read
from grouping_experiment import cosine, discover, evaluate
from scoped_discovery import consolidate, scope_metrics, MODELS


def rival_indices(labels, relations):
    rivals={i:set() for i in range(len(labels))}
    for a,b in relations['rivals']:
        if a==b or a not in labels or b not in labels:
            raise ValueError('Invalid rival scope')
        i,j=labels.index(a),labels.index(b)
        rivals[i].add(j)
        rivals[j].add(i)
    for a,b in relations.get('compatibleExamples',[]):
        if a not in labels or b not in labels or labels.index(b) in rivals[labels.index(a)]:
            raise ValueError('Conflicting compatible example')
    return rivals


def supported_topics(groups, scores, labels, rivals, slack, margin, excluded_labels=()):
    """Allow near-best compatible scopes; rivals require independent positive evidence."""
    if not set(excluded_labels)<=set(labels):
        raise ValueError('Unknown excluded scope')
    active=[i for i,label in enumerate(labels) if label not in excluded_labels]
    if not active:
        return []
    best=scores[:,active].max(axis=1)
    supported={}
    for j,label in enumerate(labels):
        if j not in active:
            continue
        accept=best-scores[:,j]<=slack+1e-12
        active_rivals=sorted(rivals[j]&set(active))
        if active_rivals:
            accept &= scores[:,j]-np.max(scores[:,active_rivals],axis=1)>=margin-1e-12
        supported[label]={int(i)+1 for i in np.flatnonzero(accept)}
    topics=[]
    for group in groups:
        for label,accepted_ids in supported.items():
            members=sorted(set(group)&accepted_ids)
            if len(members)>=2:
                topics.append(dict(label=label,members=members))
    return topics


def merge_topics(topics,matrix,threshold):
    result=[]
    for label in sorted({t['label'] for t in topics}):
        groups=[set(t['members']) for t in topics if t['label']==label]
        result.extend(dict(label=label,members=sorted(g)) for g in consolidate(groups,matrix,threshold,.5))
    return result


def main():
    notes,_,probes,_=inputs()
    relations=read('fixtures/embedding-scope-relations.json')
    labels=sorted(read('fixtures/embedding-scope-descriptions.json')['labels'])
    rivals=rival_indices(labels,relations)
    grouping=read('docs/EMBEDDING_GROUPING_RESULTS.json')
    scoped=read('docs/EMBEDDING_SCOPED_RESULTS.json')
    assert grouping['sourceInputSha256']==hashlib.sha256(json.dumps(inputs(),sort_keys=True).encode()).hexdigest()
    assert scoped['scopeFixtureSha256']==hashlib.sha256((ROOT/'fixtures/embedding-scope-descriptions.json').read_bytes()).hexdigest()
    pairs=read('fixtures/grouping-review-pairs.json')
    results=[]
    for model in MODELS:
        path=LOCAL/f'{model}-scoped.npz'
        assert hashlib.sha256(path.read_bytes()).hexdigest()==scoped['scopeVectorSha256'][model], 'Refresh scoped vectors'
        data=np.load(path)
        vectors,scores=data['noteVectors'],data['scores']
        assert vectors.shape[0]==len(notes) and scores.shape==(len(notes),len(labels))
        assert np.isfinite(scores).all() and np.isfinite(vectors).all()
        matrix=cosine(vectors)
        for policy in ('complete-link','overlapping-cores'):
            threshold=next(r['selected']['threshold'] for r in grouping['results'] if r['model']==model and r['policy']==policy)
            groups=discover(matrix,list(range(1,len(notes)+1)),policy,threshold)
            if policy=='overlapping-cores':
                groups=consolidate(groups,matrix,threshold,.5)
            for slack,margin,filter_labels in itertools.product((0,.025,.05,.10),(0,.025),(False,True)):
                excluded=relations['rejectedDisplayScopes'] if filter_labels else ()
                topics=merge_topics(supported_topics(groups,scores,labels,rivals,slack,margin,excluded),matrix,threshold)
                memberships={(t['label'],i) for t in topics for i in t['members']}
                per_note=[len({label for label,i in memberships if i==note}) for note in range(1,len(notes)+1)]
                scope=scope_metrics(topics,probes)
                required=dict(meetingIdeaPreserved=('Meetings',57) in memberships,walking72Preserved=('Walking',72) in memberships,walking161Preserved=('Walking',161) in memberships,noReviewedRejectedScopes=scope['rejectedPresent']==0)
                results.append(dict(model=model,policy=policy,displayFilter=filter_labels,excludedDisplayScopes=list(excluded),slack=slack,rivalMargin=margin,coreThreshold=threshold,development=evaluate([set(t['members']) for t in topics],pairs['positive'],pairs['negative'],len(notes)),scope=scope,regressions=required,allRequiredPass=all(required.values()),meetingIdeaAlsoInNoteTaking=('Note-taking',57) in memberships,multiLabelNotes=sum(x>1 for x in per_note),maxLabelsPerNote=max(per_note),topics=topics))
        print(model,'compatible-scope variants complete',flush=True)
    hashes={p:hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in ['fixtures/embedding-scope-relations.json','fixtures/embedding-scope-descriptions.json','scripts/embeddings/compatible_scopes.py','scripts/embeddings/scoped_discovery.py','scripts/embeddings/grouping_experiment.py']}
    output=dict(date='2026-09-30',boundary='Observed development data with supplied scope descriptions and analyst rival graph; no untouched accuracy, no open-world naming or Android claim. All 96 variants reported, including a separate user-preference display filter added after the initial 48 runs showed Chapter membership returning. Regressions are evaluation-only: topic generation receives scores, labels, relation graph, optional global label exclusions and coherent cores, never probe note IDs.',sourceHashes=hashes,sourceInputSha256=grouping['sourceInputSha256'],sourceScopeVectorSha256=scoped['scopeVectorSha256'],results=results)
    (ROOT/'docs/EMBEDDING_COMPATIBLE_RESULTS.json').write_text(json.dumps(output,indent=2)+'\n')
    lines=['# Compatible-scope experiment','',output['boundary'],'','A note can support any scope within the slack of its best description. It must also favor that scope over each declared rival by the rival margin. Non-rival labels can coexist. Topics still require at least two notes inside a coherent source core; same-label consolidation cannot bridge weak pairs. Evidence averages whole-note and first-sentence description scores. Rival relations reflect dominant scope in the reviewed examples; they are not a general prohibition of genuine multi-subject notes.','','Display filter is a separate ablation excluding Chapter and Phrases from labels and competitors, based on the explicit user rejection of these labels in this notebook. It is an authored preference, not a model-learned detector of vague labels. Without the filter, their scope descriptions remain ordinary candidates.','','Required development regressions: Meetings #57 and Walking #72/#161 are present, and none of the 15 rejected probe memberships is present. Probe replacements include analyst suggestions and ambiguous socks IDs. Required-case checks are performed only after topic generation. Passing them is insufficient for promotion: positive pair retention and unknown memberships also need review.','','| Model | Cores | Display filter | Slack | Rival margin | Preferred / 15 | Rejected / 15 | Meetings #57 | Both walks | All required | Dev positive / negative | Topics | Coverage | Multi-label notes |','|---|---|---|---:|---:|---:|---:|---|---|---|---:|---:|---:|---:|']
    for r in results:
        d,s,checks=r['development'],r['scope'],r['regressions']
        yes=lambda x:'yes' if x else 'no'
        lines.append(f"| {r['model']} | {r['policy']} | {yes(r['displayFilter'])} | {r['slack']:.3f} | {r['rivalMargin']:.3f} | {s['preferredPresent']} | {s['rejectedPresent']} | {yes(checks['meetingIdeaPreserved'])} | {yes(checks['walking72Preserved'] and checks['walking161Preserved'])} | {yes(r['allRequiredPass'])} | {d['positiveHits']}/33 / {d['negativeHits']}/13 | {d['topics']} | {d['coverage']:.0%} | {r['multiLabelNotes']} |")
    lines += ['','Larger slack admits more compatible memberships but can increase unsupported labels and topic proliferation. Rival margins are raw model scores, not calibrated probabilities. Most note/label memberships remain unreviewed; zero measured rejections is not zero false memberships. No parameters were selected on transfer. Existing model licensing evidence remains unchanged.','','[Full memberships and per-case outcomes](EMBEDDING_COMPATIBLE_RESULTS.json).','']
    (ROOT/'docs/EMBEDDING_COMPATIBLE_COMPARISON.md').write_text('\n'.join(lines))


if __name__=='__main__':
    main()
