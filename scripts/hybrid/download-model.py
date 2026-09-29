#!/usr/bin/env python3
"""Download the pinned optional audit model, validating bytes and SHA-256 before use."""
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT=Path(__file__).resolve().parents[2]


def matches(path,manifest):
    if not path.exists() or path.stat().st_size!=manifest['bytes']:return False
    sha=hashlib.sha256()
    with path.open('rb') as source:
        for chunk in iter(lambda:source.read(1024*1024),b''):sha.update(chunk)
    return sha.hexdigest()==manifest['sha256']


def main():
    manifest=json.loads((ROOT/'scripts/hybrid/model-manifest.json').read_text())
    folder=ROOT/'.local/hybrid';folder.mkdir(parents=True,exist_ok=True)
    target=folder/manifest['file']
    if matches(target,manifest):
        print('Optional model already verified.');return
    temporary=target.with_suffix(target.suffix+'.partial')
    with urllib.request.urlopen(manifest['url'],timeout=60) as response, temporary.open('wb') as output:
        for chunk in iter(lambda:response.read(1024*1024),b''):output.write(chunk)
    if not matches(temporary,manifest):raise ValueError('Model integrity check failed; partial file has not been enabled')
    temporary.replace(target)
    print(f'Verified {target.name}: {manifest["bytes"]} bytes')


if __name__=='__main__':main()
