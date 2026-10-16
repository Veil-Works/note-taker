# Coherent cores, consolidation and scope validation

Scope stage uses 16 supplied development labels for the whole notebook; incomplete vocabulary, no open-world discovery or transfer naming claim. Groups require 2 notes; no positive/negative annotation is read by worker/validation/consolidation. Core thresholds inherited from prior development-selected runs. Margins 0/.025/.05 and overlap .5/.75 are reported exhaustively, not selected on transfer.

## Unlabeled consolidation

A merge requires membership Jaccard at least the chosen overlap AND every pair in the union clearing the original model/core threshold. Original co-member pairs are retained; added pairs can be false. No transfer-based retuning.

| Model | Overlap | Topics before → after | Dev positive / negative hits | Transfer positive / negative hits |
|---|---:|---:|---:|---:|
| minilm | 0.5 | 104 → 82 | 23/33 / 1/13 | 35/69 / 0/5 |
| minilm | 0.75 | 104 → 104 | 23/33 / 1/13 | 35/69 / 0/5 |
| bge-small | 0.5 | 108 → 69 | 30/33 / 1/13 | 67/69 / 3/5 |
| bge-small | 0.75 | 108 → 108 | 30/33 / 1/13 | 67/69 / 3/5 |
| granite-small | 0.5 | 103 → 78 | 26/33 / 1/13 | 43/69 / 1/5 |
| granite-small | 0.75 | 103 → 103 | 26/33 / 1/13 | 43/69 / 1/5 |

## Supplied-scope membership validation

One-scope chooses the description with strongest 65% mean / 35% weakest-quarter member agreement. Split-scopes instead partitions each core into individually supported scopes. Note evidence averages full text and first sentence. A member must prefer its description over every other supplied scope by the margin; topics with fewer than two accepted notes are suppressed. Same-label consolidation also requires overlap and complete-pair coherence. This is a controlled test of label support, not proof of a complete product taxonomy.

| Model | Core policy | Validation | Margin | Topics | Coverage | Dev positive / negative hits | Preferred scope present / 15 | Rejected scope present / 15 |
|---|---|---|---:|---:|---:|---:|---:|---:|
| minilm | complete-link | one-scope | 0.000 | 22 | 52% | 14/33 / 1/13 | 11 | 2 |
| minilm | complete-link | one-scope | 0.025 | 22 | 47% | 14/33 / 1/13 | 11 | 1 |
| minilm | complete-link | one-scope | 0.050 | 17 | 38% | 11/33 / 0/13 | 10 | 0 |
| minilm | complete-link | split-scopes | 0.000 | 35 | 72% | 19/33 / 1/13 | 12 | 2 |
| minilm | complete-link | split-scopes | 0.025 | 29 | 56% | 16/33 / 1/13 | 11 | 1 |
| minilm | complete-link | split-scopes | 0.050 | 20 | 41% | 13/33 / 0/13 | 10 | 0 |
| minilm | overlapping-cores | one-scope | 0.000 | 53 | 65% | 18/33 / 1/13 | 11 | 2 |
| minilm | overlapping-cores | one-scope | 0.025 | 45 | 52% | 15/33 / 1/13 | 10 | 0 |
| minilm | overlapping-cores | one-scope | 0.050 | 34 | 41% | 12/33 / 0/13 | 10 | 0 |
| minilm | overlapping-cores | split-scopes | 0.000 | 60 | 73% | 20/33 / 1/13 | 11 | 2 |
| minilm | overlapping-cores | split-scopes | 0.025 | 49 | 56% | 16/33 / 1/13 | 10 | 0 |
| minilm | overlapping-cores | split-scopes | 0.050 | 36 | 44% | 13/33 / 0/13 | 10 | 0 |
| bge-small | complete-link | one-scope | 0.000 | 19 | 58% | 14/33 / 0/13 | 10 | 0 |
| bge-small | complete-link | one-scope | 0.025 | 15 | 40% | 11/33 / 0/13 | 9 | 0 |
| bge-small | complete-link | one-scope | 0.050 | 12 | 26% | 7/33 / 0/13 | 7 | 0 |
| bge-small | complete-link | split-scopes | 0.000 | 33 | 78% | 16/33 / 0/13 | 11 | 1 |
| bge-small | complete-link | split-scopes | 0.025 | 17 | 43% | 13/33 / 0/13 | 10 | 0 |
| bge-small | complete-link | split-scopes | 0.050 | 14 | 28% | 8/33 / 0/13 | 8 | 0 |
| bge-small | overlapping-cores | one-scope | 0.000 | 41 | 66% | 19/33 / 0/13 | 10 | 0 |
| bge-small | overlapping-cores | one-scope | 0.025 | 24 | 41% | 16/33 / 0/13 | 10 | 0 |
| bge-small | overlapping-cores | one-scope | 0.050 | 18 | 27% | 10/33 / 0/13 | 8 | 0 |
| bge-small | overlapping-cores | split-scopes | 0.000 | 56 | 72% | 19/33 / 0/13 | 12 | 0 |
| bge-small | overlapping-cores | split-scopes | 0.025 | 26 | 43% | 16/33 / 0/13 | 10 | 0 |
| bge-small | overlapping-cores | split-scopes | 0.050 | 19 | 28% | 10/33 / 0/13 | 8 | 0 |
| granite-small | complete-link | one-scope | 0.000 | 28 | 60% | 16/33 / 1/13 | 10 | 0 |
| granite-small | complete-link | one-scope | 0.025 | 12 | 28% | 11/33 / 0/13 | 9 | 0 |
| granite-small | complete-link | one-scope | 0.050 | 7 | 11% | 5/33 / 0/13 | 4 | 0 |
| granite-small | complete-link | split-scopes | 0.000 | 41 | 81% | 17/33 / 1/13 | 10 | 0 |
| granite-small | complete-link | split-scopes | 0.025 | 13 | 29% | 12/33 / 0/13 | 9 | 0 |
| granite-small | complete-link | split-scopes | 0.050 | 8 | 12% | 6/33 / 0/13 | 4 | 0 |
| granite-small | overlapping-cores | one-scope | 0.000 | 43 | 60% | 17/33 / 1/13 | 10 | 0 |
| granite-small | overlapping-cores | one-scope | 0.025 | 16 | 25% | 9/33 / 0/13 | 8 | 0 |
| granite-small | overlapping-cores | one-scope | 0.050 | 6 | 9% | 5/33 / 0/13 | 4 | 0 |
| granite-small | overlapping-cores | split-scopes | 0.000 | 58 | 78% | 18/33 / 1/13 | 10 | 0 |
| granite-small | overlapping-cores | split-scopes | 0.025 | 17 | 26% | 10/33 / 0/13 | 8 | 0 |
| granite-small | overlapping-cores | split-scopes | 0.050 | 7 | 10% | 6/33 / 0/13 | 4 | 0 |

Unknown memberships are not negatives. The 15 scope cases include proposed preferred labels and ambiguous socks IDs. Removing all topics would score zero rejected scopes: always consider positive retention and coverage alongside exclusions. The supplied vocabulary misses many notebook subjects, so whole-notebook losses here do not isolate a model weakness. Models retain the existing MIT/Apache commercial-use evidence.

Full per-note scope outcomes, groups and source hashes: [JSON](EMBEDDING_SCOPED_RESULTS.json).
