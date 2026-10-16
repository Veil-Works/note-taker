"""Record desktop distributions and preserve installed license/notice files locally."""
import importlib.metadata as metadata
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
records = []
for dist in sorted(metadata.distributions(), key=lambda d:d.metadata['Name'].lower()):
    name = dist.metadata['Name']
    files = []
    for file in dist.files or []:
        if any(part.lower().startswith(('license', 'copying', 'notice')) for part in file.parts):
            source = Path(dist.locate_file(file))
            if source.is_file():
                target = ROOT / '.local/embeddings/dependency-licenses' / name / str(file)
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(source.read_bytes())
                files.append(str(file))
    records.append(dict(name=name, version=dist.version, licenseExpression=dist.metadata.get('License-Expression'), license=dist.metadata.get('License'), classifiers=[c for c in dist.metadata.get_all('Classifier',[]) if c.startswith('License')], licenseFiles=files))
(ROOT/'docs/embedding-licenses/desktop-dependencies.json').write_text(json.dumps(records,indent=2)+'\n')
for r in records:
    value = r['licenseExpression'] or r['classifiers'] or (r['license'] or '')[:100]
    print(r['name'],r['version'],value,'files:',len(r['licenseFiles']))
