#!/usr/bin/env python3
"""Local blinded review of fixed Granite groups. Standard library only."""
import argparse
import copy
import hashlib
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
import secrets
import threading
from urllib.parse import urlsplit

ROOT=Path(__file__).resolve().parents[2]
UI=Path(__file__).parent/'review_ui'
VARIANTS=('A_current','D_hybrid_semantic')


def canonical(value):
    return json.dumps(value,sort_keys=True,ensure_ascii=False,separators=(',',':'))


def atomic_json(path,value):
    temporary=path.with_suffix('.tmp')
    temporary.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n')
    temporary.replace(path)


class Conflict(ValueError):
    pass


class ReviewStore:
    def __init__(self,comparison,directory):
        groups=[]
        for corpus,data in comparison['corpora'].items():
            for topic in data['topics']:
                assert set(topic['members'])=={n['id'] for n in topic['notes']}
                groups.append(dict(id=f"{corpus}-{topic['index']:03}",corpus=corpus,number=topic['index'],notes=topic['notes'],candidates={v:[c['text'] for c in topic['variants'][v]] for v in VARIANTS}))
        self.study_id=hashlib.sha256(canonical(dict(model=comparison['model'],revision=comparison['revision'],clustering=comparison['clustering'],groups=groups)).encode()).hexdigest()
        self.directory=directory/self.study_id[:16]
        self.directory.mkdir(parents=True,exist_ok=True)
        self.lock=threading.Lock()
        self.path=self.directory/'reviews.json'
        key_path=self.directory/'blind-key.json'
        if key_path.exists():
            self.key=json.loads(key_path.read_text())
            assert self.key['studyId']==self.study_id
        else:
            mapping={}
            for g in groups:
                order=list(VARIANTS)
                if secrets.randbelow(2):
                    order.reverse()
                mapping[g['id']]={'1':order[0],'2':order[1]}
            self.key=dict(studyId=self.study_id,mapping=mapping)
            atomic_json(key_path,self.key)
        self.groups={}
        for g in groups:
            self.groups[g['id']]=dict(id=g['id'],corpus=g['corpus'],number=g['number'],notes=g['notes'],proposals={slot:g['candidates'][variant] for slot,variant in self.key['mapping'][g['id']].items()})
        self.data=json.loads(self.path.read_text()) if self.path.exists() else dict(schemaVersion=1,studyId=self.study_id,reviews={})
        assert self.data['studyId']==self.study_id
        for group_id,record in self.data['reviews'].items():
            self.validate(group_id,record['review'])
        if not self.path.exists():
            atomic_json(self.path,self.data)

    def public(self):
        with self.lock:
            return copy.deepcopy(dict(studyId=self.study_id,groups=list(self.groups.values()),reviews=self.data['reviews']))

    def validate(self,group_id,value):
        if group_id not in self.groups or not isinstance(value,dict):
            raise ValueError('Unknown topic or invalid review')
        required={'preferred','labels','coherence','incorrectNoteIds','comment','suggestedLabel','complete'}
        if set(value)!=required:
            raise ValueError('Invalid review fields')
        if value['preferred'] not in (None,'1','2','tie','neither') or value['coherence'] not in (None,'good','mixed','unsure'):
            raise ValueError('Invalid rating')
        if not isinstance(value['labels'],dict) or set(value['labels'])!={'1','2'}:
            raise ValueError('Invalid label ratings')
        for slot,rating in value['labels'].items():
            if not isinstance(rating,dict) or set(rating)!={'clarity','fit'}:
                raise ValueError('Invalid label rating fields')
            if any(x is not None and (type(x) is not int or x not in (1,2,3)) for x in rating.values()):
                raise ValueError('Invalid label score')
            if not self.groups[group_id]['proposals'][slot] and any(x is not None for x in rating.values()):
                raise ValueError('A missing proposal cannot receive a label score')
        ids=value['incorrectNoteIds']
        allowed={n['id'] for n in self.groups[group_id]['notes']}
        if not isinstance(ids,list) or any(type(i) is not int or i not in allowed for i in ids) or len(set(ids))!=len(ids):
            raise ValueError('Invalid note selection')
        if any(not isinstance(value[k],str) or len(value[k])>8000 for k in ('comment','suggestedLabel')) or type(value['complete']) is not bool:
            raise ValueError('Invalid comment or completion flag')
        if value['complete']:
            if value['preferred'] is None or value['coherence'] is None:
                raise ValueError('Choose a preferred proposal and rate group coherence')
            for slot,rating in value['labels'].items():
                if self.groups[group_id]['proposals'][slot] and any(x is None for x in rating.values()):
                    raise ValueError('Rate clarity and fit for both available proposals')

    def save(self,group_id,value,revision):
        self.validate(group_id,value)
        if type(revision) is not int or revision<0:
            raise ValueError('Invalid revision')
        with self.lock:
            current=self.data['reviews'].get(group_id,dict(revision=0))
            if revision!=current['revision']:
                raise Conflict('This topic changed in another tab. Reload to get the saved review.')
            record=dict(revision=revision+1,review=copy.deepcopy(value))
            updated=copy.deepcopy(self.data)
            updated['reviews'][group_id]=record
            atomic_json(self.path,updated)
            self.data=updated
            return copy.deepcopy(record)

    def export(self):
        with self.lock:
            return copy.deepcopy(dict(**self.data,groups=list(self.groups.values())))

    def summary(self):
        totals={v:dict(preferred=0,ties=0,neither=0,clarity=[],fit=[]) for v in VARIANTS}
        with self.lock:
            reviewed=0
            excluded=0
            for group_id,record in self.data['reviews'].items():
                r=record['review']
                if not r['complete']:
                    continue
                reviewed+=1
                excluded+=len(r['incorrectNoteIds'])
                for slot,v in self.key['mapping'][group_id].items():
                    totals[v]['preferred']+=r['preferred']==slot
                    totals[v]['ties']+=r['preferred']=='tie'
                    totals[v]['neither']+=r['preferred']=='neither'
                    for metric in ('clarity','fit'):
                        if r['labels'][slot][metric] is not None:
                            totals[v][metric].append(r['labels'][slot][metric])
            for v,t in totals.items():
                for metric in ('clarity','fit'):
                    scores=t.pop(metric)
                    t[metric+'Mean']=sum(scores)/len(scores) if scores else None
                    t[metric+'Count']=len(scores)
            return dict(studyId=self.study_id,completed=reviewed,total=len(self.groups),flaggedMemberships=excluded,methods=totals)


def make_handler(store):
    token=secrets.token_urlsafe(32)
    class Handler(BaseHTTPRequestHandler):
        def log_message(self,*args):
            pass

        def allowed_host(self):
            return self.headers.get('Host') in {f'127.0.0.1:{self.server.server_port}',f'localhost:{self.server.server_port}'}

        def respond(self,status,payload,content_type='application/json; charset=utf-8',filename=None):
            body=payload if isinstance(payload,bytes) else json.dumps(payload,ensure_ascii=False).encode()
            self.send_response(status)
            self.send_header('Content-Type',content_type)
            self.send_header('Content-Length',str(len(body)))
            self.send_header('Cache-Control','no-store')
            self.send_header('X-Content-Type-Options','nosniff')
            self.send_header('Content-Security-Policy',"default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'")
            if filename:
                self.send_header('Content-Disposition',f'attachment; filename="{filename}"')
            self.end_headers()
            self.wfile.write(body)

        def do_GET(self):
            if not self.allowed_host():
                return self.respond(403,dict(error='Local access only'))
            path=urlsplit(self.path).path
            assets={'/':('index.html','text/html; charset=utf-8'),'/app.js':('app.js','text/javascript; charset=utf-8'),'/style.css':('style.css','text/css; charset=utf-8')}
            if path in assets:
                filename,mime=assets[path]
                return self.respond(200,(UI/filename).read_bytes(),mime)
            if path=='/api/data':
                return self.respond(200,dict(**store.public(),token=token))
            if path=='/api/export':
                return self.respond(200,store.export(),filename=f'topic-review-{store.study_id[:12]}.json')
            if path=='/api/summary':
                summary=store.summary()
                if summary['completed']!=summary['total']:
                    return self.respond(409,dict(error='Complete the review before revealing method identities'))
                return self.respond(200,summary)
            return self.respond(404,dict(error='Not found'))

        def do_POST(self):
            expected={f'http://127.0.0.1:{self.server.server_port}',f'http://localhost:{self.server.server_port}'}
            if not self.allowed_host() or self.headers.get('X-Review-Token')!=token or self.headers.get('Origin') not in expected:
                return self.respond(403,dict(error='Invalid local review request'))
            if self.path!='/api/review':
                return self.respond(404,dict(error='Not found'))
            try:
                length=int(self.headers.get('Content-Length','0'))
                if not 0<length<=32768 or self.headers.get('Content-Type','').split(';')[0]!='application/json':
                    raise ValueError('Invalid request size or type')
                payload=json.loads(self.rfile.read(length))
                if not isinstance(payload,dict) or set(payload)!={'studyId','groupId','revision','review'} or payload['studyId']!=store.study_id:
                    raise ValueError('Invalid study')
                record=store.save(payload['groupId'],payload['review'],payload['revision'])
                return self.respond(200,record)
            except Conflict as error:
                return self.respond(409,dict(error=str(error)))
            except (ValueError,TypeError,KeyError) as error:
                return self.respond(400,dict(error=str(error)))
            except OSError:
                return self.respond(500,dict(error='Could not save the review locally. Please retry.'))
    return Handler


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--port',type=int,default=8765)
    parser.add_argument('--review-dir',type=Path,default=ROOT/'.local/embeddings/reviews',help='Local review directory (override for isolated UI tests)')
    parser.add_argument('--summary',action='store_true',help='Explicitly reveal method identities and summarize completed reviews')
    args=parser.parse_args()
    comparison=json.loads((ROOT/'docs/LABEL_COMPARISON_RESULTS.json').read_text())
    store=ReviewStore(comparison,args.review_dir)
    if args.summary:
        print(json.dumps(store.summary(),indent=2))
        return
    server=ThreadingHTTPServer(('127.0.0.1',args.port),make_handler(store))
    print(f'Review: http://127.0.0.1:{server.server_port}',flush=True)
    print(f'Local saves: {store.path}',flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__=='__main__':
    main()
