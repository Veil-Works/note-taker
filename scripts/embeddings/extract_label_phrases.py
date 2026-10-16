#!/usr/bin/env python3
"""Parser worker: source noun phrases only; no authored categories or review annotations."""
import hashlib
import importlib.metadata as metadata
import json
from pathlib import Path
import spacy
import en_core_web_sm

ROOT=Path(__file__).resolve().parents[2]
LOCAL=ROOT/'.local/embeddings'


def phrases(doc):
    spans={}
    for chunk in doc.noun_chunks:
        if chunk.root.pos_ not in {'NOUN','PROPN'}:
            continue
        tokens=list(chunk)
        while tokens and tokens[0].pos_=='DET':
            tokens=tokens[1:]
        for size in range(1,min(4,len(tokens))+1):
            for start in range(len(tokens)-size+1):
                selected=tokens[start:start+size]
                if selected[-1].pos_ not in {'NOUN','PROPN'} or selected[0].is_stop or selected[-1].is_stop:
                    continue
                if any(t.is_punct or t.is_space for t in selected):
                    continue
                value=doc.text[selected[0].idx:selected[-1].idx+len(selected[-1])]
                if any(c in value for c in '\n\r'):
                    continue
                key=' '.join(value.lower().split())
                if any(t.is_alpha for t in selected):
                    spans.setdefault(key,[]).append(dict(start=selected[0].idx,end=selected[-1].idx+len(selected[-1]),text=value))
    return spans


def main():
    corpora={name:json.loads((ROOT/path).read_text()) for name,path in [('development','fixtures/sample-notes.json'),('expanded','fixtures/expanded-notes.json'),('transfer','fixtures/hybrid/heldout-notes.json')]}
    corpora['development']+=corpora.pop('expanded')
    model=en_core_web_sm.load(disable=['ner'])
    assert model.meta['license']=='MIT',model.meta
    output=dict(spacyVersion=spacy.__version__,modelVersion=model.meta['version'],modelLicense=model.meta['license'],corpora={})
    for name,texts in corpora.items():
        docs=list(model.pipe(texts,batch_size=32))
        output['corpora'][name]=dict(inputSha256=hashlib.sha256(json.dumps(texts,ensure_ascii=False).encode()).hexdigest(),notes=[phrases(doc) for doc in docs])
    artifact_root=Path(en_core_web_sm.__file__).parent
    files=[]
    evidence=ROOT/'docs/embedding-licenses'
    for f in sorted(artifact_root.rglob('*')):
        if f.is_file() and '__pycache__' not in f.parts:
            with f.open('rb') as handle:
                digest=hashlib.file_digest(handle,'sha256').hexdigest()
            files.append(dict(path=str(f.relative_to(artifact_root)),sha256=digest,bytes=f.stat().st_size))
            if f.name.upper().startswith(('LICENSE','NOTICE')):
                (evidence/('parser-'+f.name)).write_bytes(f.read_bytes())
    packages=[]
    for dist in sorted(metadata.distributions(),key=lambda d:d.metadata['Name'].lower()):
        packages.append(dict(name=dist.metadata['Name'],version=dist.version,licenseExpression=dist.metadata.get('License-Expression'),license=dist.metadata.get('License'),classifiers=[c for c in dist.metadata.get_all('Classifier',[]) if c.startswith('License')]))
    output['modelFiles']=files
    (LOCAL/'label-phrases.json').write_text(json.dumps(output,indent=2,ensure_ascii=False)+'\n')
    (evidence/'parser-manifest.json').write_text(json.dumps(dict(spacyVersion=spacy.__version__,modelMeta=model.meta,modelFiles=files,dependencies=packages),indent=2)+'\n')
    print('Extracted',sum(len(n) for c in output['corpora'].values() for n in c['notes']),'note/phrase entries',flush=True)


if __name__=='__main__':
    main()
