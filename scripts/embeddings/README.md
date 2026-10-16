# Laptop embedding benchmark

Run from the repository root. Python 3.12; English first. No Android required.

```sh
UV_CACHE_DIR=.local/embeddings/uv-cache UV_PYTHON_INSTALL_DIR=.local/embeddings/python uv venv --python 3.12 .local/embeddings/venv
UV_CACHE_DIR=.local/embeddings/uv-cache uv pip sync scripts/embeddings/requirements.txt --python .local/embeddings/venv/bin/python
.local/embeddings/venv/bin/python scripts/embeddings/prepare.py
.local/embeddings/venv/bin/python scripts/embeddings/license_inventory.py
.local/embeddings/venv/bin/python -m unittest discover -s scripts/embeddings -p 'test_*.py'
.local/embeddings/venv/bin/python scripts/embeddings/benchmark.py
.local/embeddings/venv/bin/python scripts/embeddings/general_comparison.py
```

Only preparation/dependency installation use the network. Inference sets Hugging Face offline mode, loads local artifacts and disables remote custom code. Preparation does not read notebook text. Models are pinned and hash-verified before inference. Model files, vectors and environments stay in ignored `.local/embeddings`; reports and license metadata are in `docs`.

Each run recomputes vectors in a fresh process per model. It overwrites the generated comparison Markdown/JSON. Interpret the numbers using `docs/EMBEDDING_BENCHMARK_PROTOCOL.md`; these observed fixtures cannot establish production accuracy. The ONNX reference mirrors app pooling/chunking; the desktop BERT tokenizer is checked against the existing Kotlin golden examples, not an exhaustive Unicode parity guarantee.

The active comparison is `general_comparison.py`: density clustering, complete-link hierarchy, and overlapping neighbor cores with generic consolidation. All use the same corpus-derived c-TF-IDF-like lexical representations and centroid-nearest representative notes. It reads no authored scope taxonomy, rival relations or label exclusions for clustering/naming. Its 344 configurations use existing commercial-use-eligible model artifacts and dependencies. Reports are `docs/GENERAL_GROUPING_COMPARISON.md` and `docs/GENERAL_GROUPING_RESULTS.json`; shortlist and next evaluation plan are in `docs/GENERAL_PROCESSING_SHORTLIST.md`.

Earlier `grouping_experiment.py`, `scoped_discovery.py`, and `compatible_scopes.py` outputs are historical exploratory diagnostics. The supplied-scope/rival/exclusion experiments are set aside from approach selection following the user's direction on 2026-09-30. Their apparent improvements do not establish a generally better pipeline. Do not extend their case-specific rules or use their scope win counts to choose a production model.

The fixed Granite-group label comparison uses an isolated parser environment. After preparation and `general_comparison.py`:

```sh
UV_CACHE_DIR=.local/embeddings/uv-cache uv venv .local/embeddings/parser-venv --python .local/embeddings/venv/bin/python
UV_CACHE_DIR=.local/embeddings/uv-cache uv pip sync scripts/embeddings/requirements-parser.txt --python .local/embeddings/parser-venv/bin/python
.local/embeddings/parser-venv/bin/python scripts/embeddings/parser_tests.py
.local/embeddings/parser-venv/bin/python scripts/embeddings/extract_label_phrases.py
.local/embeddings/venv/bin/python scripts/embeddings/label_comparison.py
```

No benchmark dependencies are changed by parser installation. Extraction and semantic inference run locally. `docs/LABEL_COMPARISON.md` contains all labels and member notes, with scores/spans in `docs/LABEL_COMPARISON_RESULTS.json`. Exact weighting and the follow-up hybrid control are documented in `docs/LABEL_COMPARISON_PROTOCOL.md`. The source/model/input/vector hashes are checked or recorded for reproduction. These label diagnostics are not a substitute for human review.

The blinded laptop review screen is ready:

    .local/embeddings/venv/bin/python scripts/embeddings/review.py

Open http://127.0.0.1:8765/. It compares current/hybrid labels on the fixed Granite groups, autosaves ratings and incorrect-member flags, and exports a blinded review. No additional Python or frontend dependencies are needed. See docs/LAPTOP_TOPIC_REVIEW.md for the workflow, local storage, and summary command. Automated browser-test ratings are isolated from real reviews.
