# Commercial-use review: first English embedding comparison

Reviewed 2026-09-29. Product requirement: the app will be sold; intended use and redistribution must be allowed for every shipped component.

| Model | Publisher's declared weight license | Evidence |
|---|---|---|
| all-MiniLM-L6-v2 | Apache-2.0 | Pinned `minilm-MODEL-CARD.md` |
| bge-small-en-v1.5 | MIT | Pinned `bge-small-MODEL-CARD.md`; publisher explicitly allows commercial use |
| granite-embedding-small-english-r2 | Apache-2.0 | Pinned `granite-small-MODEL-CARD.md` |

Exact revisions, source URLs and file hashes are in `scripts/embeddings/model-manifest.json`. These snapshots did not contain standalone LICENSE/NOTICE files. The accompanying Apache/MIT standard texts come from the SPDX license-list repository; they explain the terms, while the pinned publisher cards establish the model license declarations. They do not invent missing publisher copyright notices. Preserve model-specific notices if supplied in a future distribution; document conversion/quantization changes.

These grants allow a commercial proprietary app subject to their conditions. Apache requires applicable license/attribution and NOTICE preservation and modification notices; MIT requires preservation of its copyright and permission notice. Neither gives blanket trademark permission. References: [Apache terms](https://www.apache.org/licenses/LICENSE-2.0), [MIT terms](https://opensource.org/license/mit), [SPDX texts](https://github.com/spdx/license-list-data/tree/main/text).

`desktop-dependencies.json` records the 38 installed benchmark distributions, versions, declared license metadata and discovered license paths. Installed notice/license files are copied to `.local/embeddings/dependency-licenses`. The Python environment is a development tool, not an Android dependency or shipped artifact. Declared licenses are Apache, MIT, BSD, PSF, CNRI-Python, MIT-CMU, or MPL combinations: none is declared noncommercial/research-only. Certifi and tqdm include MPL terms, which allow commercial use but carry file-level obligations when distributed. Preserve relevant license/source notices if distributing this environment; do not assume every package is permissive solely from its top-level classifier. The wheel inventory includes bundled notices where available; flatbuffers and tokenizers wheels did not expose standalone license files, although their metadata declares Apache. A shipped runtime/converted model needs a distribution-specific notice audit; this inventory is not a completed whole-app audit.

Training provenance is separate from the weight license. Granite documents its data governance and exclusion of MS MARCO; permissive weights alone cannot establish the legal status of every training example. No external training datasets are used in this benchmark.

## Isolated label-extraction environment

The label comparison adds spaCy 3.8.7 and en_core_web_sm 3.8.0 only in `.local/embeddings/parser-venv`. spaCy declares MIT; the model's pinned package metadata and included LICENSE declare MIT. The model license and third-party sources notice are saved as `parser-LICENSE` and `parser-LICENSES_SOURCES`; `parser-manifest.json` records model file hashes and dependency versions/license metadata. The hash-pinned installer lock is `scripts/embeddings/requirements-parser.txt`. Published model source: [spaCy English models](https://spacy.io/models/en).

Parser dependency metadata declares MIT, BSD, Apache, ISC, PSF, MPL, or permissive combinations; none declares noncommercial/research-only use. The environment is a laptop development tool, not a shipped Android runtime. Preserve applicable bundled notices and distribution obligations if any parser artifacts are later shipped; the published weight grant and the listed training-source provenance remain separate facts. No training dataset is obtained or used here. The encoder remains the already reviewed Apache-licensed Granite model.
