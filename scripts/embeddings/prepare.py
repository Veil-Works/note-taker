#!/usr/bin/env python3
"""Pin commercially permissive candidates and download only inference assets locally.

This script uses the network for model downloads only. No notebook text is read or
sent. Reruns use the checked-in manifest; deleting it is an explicit model refresh.
"""
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
LOCAL = ROOT / '.local/embeddings'
MANIFEST = ROOT / 'scripts/embeddings/model-manifest.json'
EVIDENCE = ROOT / 'docs/embedding-licenses'


def fetch(url):
    request = urllib.request.Request(url, headers={'User-Agent': 'note-taker-embedding-evaluation/1.0'})
    with urllib.request.urlopen(request, timeout=120) as response:
        return response.read()


def main():
    from huggingface_hub import snapshot_download
    config = json.loads((Path(__file__).parent / 'candidates.json').read_text())
    existing = json.loads(MANIFEST.read_text()) if MANIFEST.exists() else None
    pinned = {m['id']: m for m in existing['models']} if existing else {}
    candidates = [{**c, 'revision': pinned.get(c['id'], c)['revision']} for c in config['models']]
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    manifest = {'reviewDate': '2026-09-29', 'models': []}
    for candidate in candidates:
        repository, revision = candidate['repository'], candidate['revision']
        metadata = json.loads(fetch(f'https://huggingface.co/api/models/{repository}/revision/{revision}'))
        license_id = metadata.get('cardData', {}).get('license')
        if license_id != candidate['license'] or license_id not in {'apache-2.0', 'mit'}:
            raise ValueError(f'License requires review for {repository}: {license_id!r}')
        if metadata.get('gated') or metadata.get('private'):
            raise ValueError(f'Additional access terms require review: {repository}')
        sha = metadata['sha']
        directory = LOCAL / 'models' / candidate['id']
        source = f'https://huggingface.co/{repository}/blob/{sha}/README.md'
        card = fetch(f'https://huggingface.co/{repository}/resolve/{sha}/README.md')
        (EVIDENCE / f"{candidate['id']}-MODEL-CARD.md").write_bytes(card)
        print(f"Downloading {candidate['id']} at {sha} ({license_id})", flush=True)
        snapshot_download(repository, revision=sha, local_dir=directory,
                          allow_patterns=['*.json', '*.txt', '*.safetensors', 'README.md', 'LICENSE*', 'NOTICE*'],
                          max_workers=4)
        records = []
        for file in sorted(directory.rglob('*')):
            if not file.is_file() or '.cache' in file.parts:
                continue
            with file.open('rb') as handle:
                digest = hashlib.file_digest(handle, 'sha256').hexdigest()
            records.append({'path': str(file.relative_to(directory)), 'bytes': file.stat().st_size,
                            'sha256': digest})
            if file.name.upper().startswith(('LICENSE', 'NOTICE')):
                (EVIDENCE / f"{candidate['id']}-{file.name}").write_bytes(file.read_bytes())
        manifest['models'].append({**candidate, 'revision': sha, 'licenseSource': source,
                                   'modelCardSha256': hashlib.sha256(card).hexdigest(), 'files': records})
        MANIFEST.write_text(json.dumps(manifest, indent=2) + '\n')
    print(f'Manifest: {MANIFEST}', flush=True)


if __name__ == '__main__':
    main()
