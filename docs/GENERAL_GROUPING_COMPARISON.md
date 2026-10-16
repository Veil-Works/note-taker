# General processing approach comparison

General algorithms only. Review pairs are used for model-level development calibration and evaluation, never as exceptions or scope logic. Observed transfer data is a regression set, not an untouched benchmark. Supplied scope descriptions, rival relations, rejected label names and scope probes are not consulted by clustering or labeling.

All families use the same corpus-derived term extraction and representative-note selection. Models keep their existing commercial-use eligibility. Density clustering uses Euclidean distance on normalized embeddings with no dimensionality reduction. Complete-link requires every within-group pair to clear its threshold. Overlapping reciprocal top-5 cores use the same complete-pair requirement and generic overlap consolidation (Jaccard 0.5).

86 configurations per model: 18 density settings plus 34 settings for each other family. Selection uses development positive-pair recall minus explicit-negative co-membership rate. Unknown pairs are not negatives. The selected configuration transfers unchanged. Rankings are development descriptions, not evidence of statistically established superiority or label quality.

| Model | Family | Parameters | Dev positive / negative | Transfer positive / negative | Topics / largest | Coverage | Prefix / removal stability |
|---|---|---|---:|---:|---|---:|---|
| production-onnx | density | {"method": "eom", "minClusterSize": 5, "minSamples": 2} | 19/33 / 3/13 | 48/69 / 1/5 | 10 / 23 | 72% | 0.993 / 0.607 |
| production-onnx | complete-link | {"threshold": 0.225} | 25/33 / 1/13 | 37/69 / 1/5 | 32 / 15 | 93% | 0.923 / 0.723 |
| production-onnx | overlapping-cores | {"threshold": 0.325} | 24/33 / 1/13 | 38/69 / 0/5 | 82 / 8 | 91% | 0.918 / 0.814 |
| minilm | density | {"method": "leaf", "minClusterSize": 5, "minSamples": 2} | 15/33 / 2/13 | 51/69 / 1/5 | 11 / 23 | 66% | 0.901 / 0.671 |
| minilm | complete-link | {"threshold": 0.175} | 24/33 / 1/13 | 42/69 / 1/5 | 31 / 19 | 99% | 0.741 / 0.806 |
| minilm | overlapping-cores | {"threshold": 0.325} | 23/33 / 1/13 | 35/69 / 0/5 | 82 / 8 | 91% | 0.929 / 0.826 |
| bge-small | density | {"method": "leaf", "minClusterSize": 3, "minSamples": 2} | 18/33 / 0/13 | 38/69 / 1/5 | 17 / 14 | 60% | 0.977 / 0.560 |
| bge-small | complete-link | {"threshold": 0.475} | 25/33 / 0/13 | 51/69 / 2/5 | 18 / 19 | 99% | 0.627 / 0.552 |
| bge-small | overlapping-cores | {"threshold": 0.4} | 30/33 / 1/13 | 68/69 / 3/5 | 69 / 10 | 90% | 0.870 / 0.686 |
| granite-small | density | {"method": "eom", "minClusterSize": 3, "minSamples": 2} | 18/33 / 0/13 | 55/69 / 1/5 | 17 / 19 | 64% | 0.403 / 0.526 |
| granite-small | complete-link | {"threshold": 0.775} | 28/33 / 1/13 | 38/69 / 0/5 | 32 / 19 | 99% | 0.948 / 0.812 |
| granite-small | overlapping-cores | {"threshold": 0.8} | 26/33 / 1/13 | 43/69 / 1/5 | 78 / 12 | 94% | 0.898 / 0.755 |

Every selected configuration passed the fixed-seed input-permutation check. Stability measures compare co-member pairs among retained notes; saved topic identity is separate. High coverage is not necessarily good; density noise is allowed. The transfer fixture has only five explicit negatives, so zero hits there is weak evidence.

## Representative output for review

These are extracted term candidates, not user-approved labels. All memberships and terms are in [JSON](GENERAL_GROUPING_RESULTS.json).

### bge-small / density

- 14 notes; terms: notebook, topic, notes, note, idea; representatives: 52, 99, 102.
- 12 notes; terms: vienna, train, camera, station, trip; representatives: 167, 68, 67.
- 11 notes; terms: atlas, search, atlas search, document, documents; representatives: 61, 88, 14.
- 11 notes; terms: afternoon, coffee, lunch, meal, meal prep; representatives: 118, 49, 108.
- 7 notes; terms: beacon, date, receipt, payment, charge date; representatives: 21, 19, 84.

### bge-small / complete-link

- 19 notes; terms: notebook, note, topic, notes, idea; representatives: 52, 99, 96.
- 16 notes; terms: coding, lunch, long, short, afternoon; representatives: 95, 72, 28.
- 15 notes; terms: orion, maya, people, review, onboarding; representatives: 74, 1, 8.
- 15 notes; terms: atlas, search, atlas search, documents, document; representatives: 61, 88, 15.
- 14 notes; terms: session, kg, 80, 80 kg, bench; representatives: 29, 111, 33.

### bge-small / overlapping-cores

- 10 notes; terms: meal, meal prep, prep, rice, grocery; representatives: 118, 49, 170.
- 8 notes; terms: maya, review, jonas, ask maya, orion; representatives: 89, 16, 88.
- 8 notes; terms: maya, review, review orion, orion, onboarding; representatives: 27, 2, 90.
- 8 notes; terms: filter, search, deleted, atlas, document; representatives: 88, 81, 61.
- 8 notes; terms: session, kg, 80, 80 kg, bench; representatives: 111, 29, 33.

### granite-small / density

- 19 notes; terms: orion, onboarding, maya, review, people; representatives: 7, 3, 2.
- 14 notes; terms: sunday, week, long run, meal, meal prep; representatives: 170, 108, 118.
- 12 notes; terms: beacon, payment, date, receipt, subscription; representatives: 18, 19, 21.
- 11 notes; terms: notebook, topic, note, notes, idea; representatives: 52, 102, 55.
- 9 notes; terms: vienna, train, station, trip, camera; representatives: 167, 68, 67.

### granite-small / complete-link

- 19 notes; terms: orion, onboarding, maya, orion onboarding, people; representatives: 7, 3, 2.
- 13 notes; terms: search, atlas, atlas search, documents, document; representatives: 15, 61, 10.
- 12 notes; terms: beacon, payment, subscription, billing, date; representatives: 18, 19, 21.
- 12 notes; terms: notebook, topic, note, automatic, notes; representatives: 52, 102, 56.
- 9 notes; terms: run, km, easy, pace, run km; representatives: 35, 107, 105.

### granite-small / overlapping-cores

- 12 notes; terms: beacon, payment, subscription, date, beacon payment; representatives: 18, 19, 21.
- 8 notes; terms: onboarding, flow, onboarding flow, orion todo, invitation; representatives: 2, 73, 4.
- 8 notes; terms: kg, 80, 80 kg, bench, session; representatives: 111, 30, 29.
- 8 notes; terms: notebook, topic, notebook todo, notebook idea, note; representatives: 52, 56, 55.
- 7 notes; terms: people, sample, workspace, orion let, sample workspace; representatives: 74, 1, 77.

Method references: [scikit-learn HDBSCAN](https://scikit-learn.org/stable/modules/generated/sklearn.cluster.HDBSCAN.html), [BERTopic class-based TF-IDF](https://maartengr.github.io/BERTopic/getting_started/ctfidf/ctfidf.html). This experiment uses scikit-learn and a small lexical implementation, not the full BERTopic package or its default UMAP pipeline.
