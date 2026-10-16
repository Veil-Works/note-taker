#!/usr/bin/env python3
"""Fixed Granite groups: lexical baseline, source noun phrases, semantic reranking."""
import hashlib
import json
import math
import os
from pathlib import Path
import re
import time
import numpy as np
from scipy.stats import rankdata
from sklearn.feature_extraction.text import CountVectorizer, ENGLISH_STOP_WORDS
from benchmark import ROOT, LOCAL, read

os.environ.update(HF_HUB_OFFLINE='1',TRANSFORMERS_OFFLINE='1',HF_HUB_DISABLE_TELEMETRY='1',TOKENIZERS_PARALLELISM='false')
VARIANTS=('A_current','B_noun_support','C_semantic_mmr','D_hybrid_semantic','B_token_control')


def source_phrases(text):
    """True contiguous 1–3 token spans; stopwords filter endpoints, never remove gaps."""
    tokens=list(re.finditer(r"(?u)\b[^\W_]+(?:[-'][^\W_]+)*\b",text))
    result={}
    for i in range(len(tokens)):
        for size in range(1,4):
            span=tokens[i:i+size]
            if len(span)!=size or any(text[a.end():b.start()].strip() for a,b in zip(span,span[1:])):
                continue
            if span[0].group().lower() in ENGLISH_STOP_WORDS or span[-1].group().lower() in ENGLISH_STOP_WORDS:
                continue
            if not any(c.isalpha() for token in span for c in token.group()):
                continue
            value=text[span[0].start():span[-1].end()]
            if '\n' in value or '\r' in value:
                continue
            key=' '.join(value.lower().split())
            result.setdefault(key,[]).append(dict(start=span[0].start(),end=span[-1].end(),text=value))
    return result


def literal_spans(text,phrase):
    pattern=r'(?<!\w)'+r'\s+'.join(re.escape(w) for w in phrase.split())+r'(?!\w)'
    return [dict(start=m.start(),end=m.end(),text=m.group()) for m in re.finditer(pattern,text,re.IGNORECASE)]


def lexical_pools(texts,groups,extracted=None,binary=False):
    if extracted is None:
        vectorizer=CountVectorizer(stop_words='english',ngram_range=(1,2),min_df=2)
        per_note=vectorizer.fit_transform(texts).toarray()
        vocabulary=list(vectorizer.get_feature_names_out())
    else:
        vocabulary=sorted({p for note in extracted for p in note if sum(p in other for other in extracted)>=2})
        per_note=np.array([[len(note.get(p,[])) for p in vocabulary] for note in extracted],dtype=float)
    if binary:
        per_note=(per_note>0).astype(float)
    totals=np.vstack([per_note[np.array(sorted(g))-1].sum(axis=0) for g in groups]).astype(float)
    lengths=totals.sum(axis=1)
    tf=np.divide(totals,lengths[:,None],out=np.zeros_like(totals),where=lengths[:,None]!=0)
    weights=tf*np.log1p(lengths.mean()/np.maximum(totals.sum(axis=0),1))
    pools=[]
    for group,row in zip(groups,weights):
        candidates=[]
        for j,phrase in enumerate(vocabulary):
            if row[j]<=0:
                continue
            sources=[]
            for note in sorted(group):
                spans=literal_spans(texts[note-1],phrase) if extracted is None else extracted[note-1].get(phrase,[])
                if spans:
                    sources.append(dict(note=note,spans=spans))
            if extracted is not None and len(sources)<2:
                continue
            candidates.append(dict(text=str(phrase),lexicalWeight=float(row[j]),support=len(sources),sources=sources))
        candidates.sort(key=(lambda c:(-c['lexicalWeight'],c['text'])) if extracted is None else (lambda c:(-c['lexicalWeight'],-c['support'],c['text'])))
        pools.append(candidates[:30])
    return pools


def fractions(values):
    if len(values)==1:
        return np.ones(1)
    return (rankdata(values,method='average')-1)/(len(values)-1)


def add_semantics(pool,members,groups,note_vectors,phrase_vectors):
    result=[]
    for candidate in pool:
        vector=phrase_vectors[candidate['text']]
        scores=np.einsum('ij,j->i',note_vectors[np.array(sorted(members))-1].astype(float),vector.astype(float),optimize=False)
        mean=float(scores.mean())
        weak=float(np.sort(scores)[:max(1,math.ceil(len(scores)*.25))].mean())
        outside=[float(np.einsum('ij,j->i',note_vectors[np.array(sorted(g))-1].astype(float),vector.astype(float),optimize=False).mean()) for g in groups if g!=members]
        strongest=max(outside,default=0)
        result.append(dict(**candidate,memberMeanCosine=mean,weakQuarterCosine=weak,coverageCosine=.65*mean+.35*weak,strongestOtherGroupMeanCosine=strongest,contrastCosine=mean-strongest))
    if result:
        for key in ('coverageCosine','contrastCosine','lexicalWeight'):
            for c,value in zip(result,fractions([c[key] for c in result])):
                c[key+'Rank']=float(value)
        for c in result:
            c['fusedRankScore']=sum(c[k+'Rank'] for k in ('coverageCosine','contrastCosine','lexicalWeight'))/3
    return result


def semantic_rank(pool):
    return sorted(pool,key=lambda c:(-c['fusedRankScore'],-c['support'],c['text']))


def diverse(pool,phrase_vectors,k=5):
    candidates=semantic_rank(pool)
    if not candidates:
        return []
    selected=[candidates.pop(0)]
    while candidates and len(selected)<k:
        def utility(c):
            redundancy=max(float(np.sum(phrase_vectors[c['text']].astype(float)*phrase_vectors[s['text']].astype(float))) for s in selected)
            return .8*c['fusedRankScore']-.2*redundancy
        winner=min(candidates,key=lambda c:(-utility(c),-c['support'],c['text']))
        selected.append(winner)
        candidates.remove(winner)
    return selected


def summarize(topics,variant,phrase_vectors):
    choices=[t['variants'][variant] for t in topics]
    nonempty=[(t,c) for t,c in zip(topics,choices) if c]
    names=[c[0]['text'] for _,c in nonempty]
    support=[c[0]['support']/len(t['members']) for t,c in nonempty]
    redundancy=[]
    for _,candidates in nonempty:
        for i,a in enumerate(candidates):
            for b in candidates[i+1:]:
                redundancy.append(float(np.sum(phrase_vectors[a['text']].astype(float)*phrase_vectors[b['text']].astype(float))))
    alternatives=[c for _,cs in nonempty for c in cs]
    return dict(topics=len(topics),withCandidates=len(nonempty),uniqueTopLabels=len(set(names)),zeroLiteralSupportTopLabels=sum(c[0]['support']==0 for _,c in nonempty),zeroLiteralSupportAlternatives=sum(c['support']==0 for c in alternatives),totalAlternatives=len(alternatives),meanTopLabelLiteralSupportFraction=float(np.mean(support)) if support else None,meanTopLabelMemberCosine=float(np.mean([c[0]['memberMeanCosine'] for _,c in nonempty])) if nonempty else None,meanTopLabelContrast=float(np.mean([c[0]['contrastCosine'] for _,c in nonempty])) if nonempty else None,meanTop5SemanticRedundancy=float(np.mean(redundancy)) if redundancy else None)


def main():
    notes=read('fixtures/sample-notes.json')+read('fixtures/expanded-notes.json')
    transfer=read('fixtures/hybrid/heldout-notes.json')
    parser=read('.local/embeddings/label-phrases.json')
    comparison=read('docs/GENERAL_GROUPING_RESULTS.json')
    chosen=next(r for r in comparison['results'] if r['model']=='granite-small' and r['family']=='complete-link')
    vector_path=LOCAL/'granite-small-vectors.npy'
    assert hashlib.sha256(vector_path.read_bytes()).hexdigest()==comparison['sourceVectorSha256']['granite-small']
    vectors=np.load(vector_path)
    datasets={}
    for name,texts,records,offset in [('development',notes,chosen['topics'],0),('transfer',transfer,chosen['transferTopics'],len(notes))]:
        assert parser['corpora'][name]['inputSha256']==hashlib.sha256(json.dumps(texts,ensure_ascii=False).encode()).hexdigest()
        groups=[frozenset(t['members']) for t in records]
        extracted=parser['corpora'][name]['notes']
        pools=dict(A_current=lexical_pools(texts,groups),B_noun_support=lexical_pools(texts,groups,extracted,True),B_token_control=lexical_pools(texts,groups,extracted,False))
        hybrid=[]
        for text,nouns in zip(texts,extracted):
            observed=source_phrases(text)
            # Preserve parser phrases longer than three tokens; avoid counting a source occurrence twice.
            for phrase,spans in nouns.items():
                existing=observed.setdefault(phrase,[])
                existing.extend(s for s in spans if (s['start'],s['end']) not in {(p['start'],p['end']) for p in existing})
            hybrid.append(observed)
        pools['D_hybrid_semantic']=lexical_pools(texts,groups,hybrid,True)
        # Confirm that baseline top terms reproduce the frozen previous report.
        for pool,old in zip(pools['A_current'],records):
            assert [c['text'] for c in pool[:5]]==[c['text'] for c in old['terms']]
        datasets[name]=dict(texts=texts,groups=groups,records=records,pools=pools,noteVectors=vectors[offset:offset+len(texts)])
    phrases=sorted({c['text'] for dataset in datasets.values() for pools in dataset['pools'].values() for pool in pools for c in pool})
    manifest=read('scripts/embeddings/model-manifest.json')
    model_record=next(m for m in manifest['models'] if m['id']=='granite-small')
    for f in model_record['files']:
        with (LOCAL/'models/granite-small'/f['path']).open('rb') as handle:
            assert hashlib.file_digest(handle,'sha256').hexdigest()==f['sha256']
    import torch
    from sentence_transformers import SentenceTransformer
    torch.set_num_threads(4)
    start=time.perf_counter()
    model=SentenceTransformer(str(LOCAL/'models/granite-small'),device='cpu',trust_remote_code=False).float()
    assert max(len(model.tokenizer.encode(p,truncation=False)) for p in phrases)<=model.max_seq_length
    phrase_embeddings=model.encode(phrases,batch_size=32,normalize_embeddings=True,show_progress_bar=False,prompt='')
    assert np.isfinite(phrase_embeddings).all() and np.allclose(np.linalg.norm(phrase_embeddings,axis=1),1,atol=1e-5)
    np.savez(LOCAL/'label-candidate-vectors.npz',phrases=np.array(phrases),vectors=phrase_embeddings)
    phrase_vectors=dict(zip(phrases,phrase_embeddings))
    encoding_seconds=time.perf_counter()-start
    output=dict(model='granite-small',revision=model_record['revision'],clustering=chosen['selected']['parameters'],phraseEmbeddingCount=len(phrases),loadAndPhraseEncodingSeconds=encoding_seconds,protocol='docs/LABEL_COMPARISON_PROTOCOL.md',corpora={})
    for name,data in datasets.items():
        topics=[]
        for index,(members,record) in enumerate(zip(data['groups'],data['records'])):
            variants={}
            candidates={}
            for variant,pools in data['pools'].items():
                enriched=add_semantics(pools[index],members,data['groups'],data['noteVectors'],phrase_vectors)
                candidates[variant]=enriched
                variants[variant]=enriched[:5]
            variants['C_semantic_mmr']=diverse(candidates['B_noun_support'],phrase_vectors)
            variants['D_hybrid_semantic']=diverse(candidates['D_hybrid_semantic'],phrase_vectors)
            candidates['C_semantic_mmr']=semantic_rank(candidates['B_noun_support'])
            variants['C_semantic_no_mmr']=candidates['C_semantic_mmr'][:5]
            topics.append(dict(index=index+1,members=sorted(members),notes=[dict(id=i,text=data['texts'][i-1]) for i in sorted(members)],representatives=record['representatives'],variants=variants,candidates=candidates))
        common=[t for t in topics if all(t['variants'][v] for v in VARIANTS)]
        output['corpora'][name]=dict(inputSha256=parser['corpora'][name]['inputSha256'],groupsSha256=hashlib.sha256(json.dumps([sorted(g) for g in data['groups']]).encode()).hexdigest(),topics=topics,summary={v:summarize(topics,v,phrase_vectors) for v in (*VARIANTS,'C_semantic_no_mmr')},commonGroupSummary={v:summarize(common,v,phrase_vectors) for v in VARIANTS})
    sources=['scripts/embeddings/label_comparison.py','scripts/embeddings/extract_label_phrases.py','scripts/embeddings/requirements-parser.txt','docs/LABEL_COMPARISON_PROTOCOL.md','docs/GENERAL_GROUPING_RESULTS.json','scripts/embeddings/model-manifest.json']
    output['sourceHashes']={p:hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in sources}
    output['artifactHashes']={p:hashlib.sha256((LOCAL/p).read_bytes()).hexdigest() for p in ['label-phrases.json','label-candidate-vectors.npz','granite-small-vectors.npy']}
    (ROOT/'docs/LABEL_COMPARISON_RESULTS.json').write_text(json.dumps(output,indent=2,ensure_ascii=False)+'\n')
    lines=['# Fixed Granite-group labeling comparison','','Groups and memberships are identical across variants. A = current terms; B = source noun phrases with distinct-note support; C = B candidates semantically reranked with Granite and MMR alternatives. D is a follow-up control combining real contiguous source phrases with noun phrases, using the same semantic ranking. Token control isolates noun extraction with original token-count weighting. No authored categories, label exclusions or note-specific rules. See [protocol](LABEL_COMPARISON_PROTOCOL.md).','','Automatic diagnostics below are not label accuracy. C/D optimize semantic scores, so higher scores do not independently prove better labels. Literal support favors observed wording and can penalize a valid paraphrase; a copied phrase can still be a poor topic name. Human review of informativeness and group fit is pending. Means in the first table include only groups with candidates for that variant; the second table compares identical common groups.','','| Corpus | Variant | Candidate availability | Unique top labels | Top labels absent verbatim | Mean literal member support | Member cosine | Contrast | Top-5 redundancy |','|---|---|---:|---:|---:|---:|---:|---:|---:|']
    for name,data in output['corpora'].items():
        for variant,s in data['summary'].items():
            value=lambda x:f'{x:.3f}' if x is not None else 'n/a'
            lines.append(f"| {name} | {variant} | {s['withCandidates']}/{s['topics']} | {s['uniqueTopLabels']} | {s['zeroLiteralSupportTopLabels']} | {value(s['meanTopLabelLiteralSupportFraction'])} | {value(s['meanTopLabelMemberCosine'])} | {value(s['meanTopLabelContrast'])} | {value(s['meanTop5SemanticRedundancy'])} |")
    lines += ['','## Same-group diagnostics','','| Corpus | Variant | Common groups | Literal member support | Member cosine | Contrast |','|---|---|---:|---:|---:|---:|']
    for name,data in output['corpora'].items():
        for variant,s in data['commonGroupSummary'].items():
            value=lambda x:f'{x:.3f}' if x is not None else 'n/a'
            lines.append(f"| {name} | {variant} | {s['topics']} | {value(s['meanTopLabelLiteralSupportFraction'])} | {value(s['meanTopLabelMemberCosine'])} | {value(s['meanTopLabelContrast'])} |")
    for name,data in output['corpora'].items():
        lines += ['',f'## {name.capitalize()}: side-by-side labels','','| Group | Notes | A: current | B: noun/support | C: semantic | D: hybrid/semantic | Noun/token control |','|---|---:|---|---|---|---|---|']
        for t in data['topics']:
            top=lambda v:t['variants'][v][0]['text'] if t['variants'][v] else '(no supported candidate)'
            lines.append(f"| {t['index']} | {len(t['members'])} | {top('A_current')} | {top('B_noun_support')} | {top('C_semantic_mmr')} | {top('D_hybrid_semantic')} | {top('B_token_control')} |")
        lines += ['','## Member notes and alternatives','']
        for t in data['topics']:
            lines += [f"### {name} group {t['index']} ({len(t['members'])} notes)",'']
            for variant in VARIANTS:
                names=', '.join(c['text'] for c in t['variants'][variant]) or '(no supported candidate)'
                lines.append(f'- **{variant}:** {names}')
            lines += ['','Members:','']
            for note in t['notes']:
                lines.append(f"- #{note['id']}: {note['text']}")
            lines.append('')
    lines += ['Full rankings, phrase source spans, component scores and hashes: [JSON](LABEL_COMPARISON_RESULTS.json). Parser evidence: [manifest](embedding-licenses/parser-manifest.json). No Android or app discovery change is included.','']
    (ROOT/'docs/LABEL_COMPARISON.md').write_text('\n'.join(lines))
    print(json.dumps(dict(phrases=len(phrases),seconds=encoding_seconds,summaries={k:v['summary'] for k,v in output['corpora'].items()}),indent=2),flush=True)


if __name__=='__main__':
    main()
