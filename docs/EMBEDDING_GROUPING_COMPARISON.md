# Laptop grouping and label-scope experiments

Run 2026-09-30. Observed development/regression data only. These Python policies are candidate algorithms, not a port or evaluation of the Kotlin production discovery pipeline. No app changes or Android runs.

## Grouping comparison

Each policy/model chooses its threshold from 0.150–0.975 (step 0.025) using **development positive-pair recall minus explicit-negative co-membership rate**. The initial 0.30–0.80 grid placed some optima on its boundary, so it was widened before drawing conclusions. This criterion gives equal aggregate weight to the two reviewed classes. It is not production precision. Unknown pairs are excluded from scoring. The selected threshold is then applied unchanged to the observed 40-note transfer corpus.

| Model | Policy | Threshold | Dev positive hits / 33 | Dev negative hits / 13 | Transfer positive hits / 69 | Transfer negative hits / 5 | Topics / largest | Coverage | Prefix pair stability | Removal pair stability |
|---|---|---:|---:|---:|---:|---:|---|---:|---:|---:|
| production-onnx | mutual-components | 0.500 | 20 | 2 | 14 | 0 | 15 / 32 | 73% | 0.988 | 0.799 |
| production-onnx | complete-link | 0.225 | 25 | 1 | 37 | 1 | 32 / 15 | 93% | 0.923 | 0.723 |
| production-onnx | overlapping-cores | 0.325 | 24 | 1 | 38 | 0 | 102 / 6 | 91% | 0.918 | 0.826 |
| minilm | mutual-components | 0.500 | 21 | 2 | 16 | 0 | 15 / 32 | 74% | 1.000 | 0.805 |
| minilm | complete-link | 0.175 | 24 | 1 | 42 | 1 | 31 / 19 | 99% | 0.741 | 0.806 |
| minilm | overlapping-cores | 0.325 | 23 | 1 | 35 | 0 | 104 / 6 | 91% | 0.929 | 0.822 |
| bge-small | mutual-components | 0.700 | 26 | 3 | 11 | 0 | 11 / 78 | 80% | 0.877 | 0.492 |
| bge-small | complete-link | 0.500 | 25 | 0 | 47 | 1 | 22 / 19 | 98% | 0.752 | 0.583 |
| bge-small | overlapping-cores | 0.450 | 30 | 1 | 67 | 3 | 108 / 6 | 90% | 0.897 | 0.715 |
| granite-small | mutual-components | 0.875 | 13 | 0 | 2 | 0 | 20 / 19 | 60% | 0.956 | 0.633 |
| granite-small | complete-link | 0.775 | 28 | 1 | 38 | 0 | 32 / 19 | 99% | 0.948 | 0.812 |
| granite-small | overlapping-cores | 0.800 | 26 | 1 | 43 | 1 | 103 / 6 | 94% | 0.915 | 0.760 |

- Mutual components: connected components of reciprocal top-5 neighbors above threshold. Can chain across weak endpoints.
- Complete link: disjoint agglomerative clusters; every within-cluster pair clears threshold. Singletons are left ungrouped.
- Overlapping cores: deterministic greedy complete-pair cores from each reciprocal-neighbor seed; remove duplicates/subsets. A note may appear in multiple topics. No expansion or lexical rules.

Coverage counts notes in a topic of at least two notes. Stability compares co-member pairs after adding the last 20% of notes or removing every fifth note, restricted to notes present in both runs; higher is more stable. Every selected configuration passed a fixed-seed input-permutation check. This does not test saved topic IDs.

## Label descriptions and focus

Scope descriptions were authored from the reported errors before this inference. They are development interventions, not a discovered taxonomy or independent generalization test. FP32 MiniLM, BGE and Granite only; the quantized production representation is not included in this description comparison.

Each cell gives **bare label → scope description → scope description with focus**. Focus averages whole-note and first-sentence description margins equally. Sentence selection is a simple punctuation/newline rule, not a complete port of the app focus heuristics. A one-sentence note receives no additional focus signal.

| Note | Preferred / rejected | MiniLM | BGE | Granite |
|---|---|---:|---:|---:|
| 120 | Rosemary / Basil | +0.150 → +0.298 → +0.317 | +0.051 → +0.176 → +0.184 | +0.099 → +0.114 → +0.113 |
| 35 | Running / Running gear | +0.109 → +0.264 → +0.244 | +0.067 → +0.121 → +0.105 | +0.036 → +0.043 → +0.043 |
| 103 | Running / Running gear | +0.037 → +0.252 → +0.252 | +0.041 → +0.106 → +0.106 | +0.017 → +0.048 → +0.048 |
| 108 | Meal preparation / Running gear | +0.252 → +0.454 → +0.415 | +0.118 → +0.269 → +0.254 | +0.085 → +0.136 → +0.125 |
| 28 | Focused work / Chat | -0.180 → +0.231 → +0.154 | -0.088 → +0.088 → +0.072 | -0.096 → +0.037 → +0.018 |
| 94 | Focused work / Chat | -0.157 → +0.269 → +0.290 | -0.094 → +0.128 → +0.122 | +0.007 → +0.098 → +0.095 |
| 72 | Walking / Meetings | -0.280 → -0.135 → -0.008 | +0.008 → +0.044 → +0.091 | -0.049 → -0.022 → +0.036 |
| 161 | Walking / Meetings | -0.286 → -0.244 → -0.221 | -0.021 → +0.009 → +0.009 | -0.047 → -0.025 → -0.027 |
| 57 | Meetings / Meeting planning | +0.032 → +0.048 → +0.048 | -0.065 → +0.015 → +0.015 | -0.034 → +0.031 → +0.031 |
| 145 | Reading notes / Chapter | +0.003 → +0.017 → +0.020 | +0.102 → -0.013 → -0.013 | +0.015 → -0.010 → -0.010 |
| 147 | Piano practice / Phrases | +0.254 → +0.308 → +0.308 | +0.191 → +0.227 → +0.227 | +0.061 → +0.083 → +0.083 |
| 151 | French practice / Phrases | +0.046 → +0.111 → +0.111 | +0.002 → +0.075 → +0.075 | +0.019 → +0.051 → +0.051 |
| 63 | Note-taking / Phrases | +0.037 → +0.241 → +0.287 | +0.005 → +0.121 → +0.136 | -0.016 → +0.035 → +0.041 |
| 46 | Running gear / Running | +0.003 → +0.224 → +0.224 | +0.036 → +0.131 → +0.131 | -0.012 → +0.049 → +0.049 |
| 109 | Running gear / Running | -0.009 → +0.072 → +0.078 | +0.045 → +0.069 → +0.070 | +0.027 → +0.034 → +0.031 |

Positive margin favors the preferred scope. Neither sign nor cosine magnitude is calibrated membership probability. Some preferred names remain analyst proposals, and socks note identification remains ambiguous.

Full threshold sweeps, memberships, review-pair diagnostics, stability measures and description cosines are in [results JSON](EMBEDDING_GROUPING_RESULTS.json). All models retain the commercially permissive licenses documented in [license evidence](embedding-licenses/README.md).
