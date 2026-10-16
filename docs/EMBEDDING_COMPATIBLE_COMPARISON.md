# Compatible-scope experiment

Observed development data with supplied scope descriptions and analyst rival graph; no untouched accuracy, no open-world naming or Android claim. All 96 variants reported, including a separate user-preference display filter added after the initial 48 runs showed Chapter membership returning. Regressions are evaluation-only: topic generation receives scores, labels, relation graph, optional global label exclusions and coherent cores, never probe note IDs.

A note can support any scope within the slack of its best description. It must also favor that scope over each declared rival by the rival margin. Non-rival labels can coexist. Topics still require at least two notes inside a coherent source core; same-label consolidation cannot bridge weak pairs. Evidence averages whole-note and first-sentence description scores. Rival relations reflect dominant scope in the reviewed examples; they are not a general prohibition of genuine multi-subject notes.

Display filter is a separate ablation excluding Chapter and Phrases from labels and competitors, based on the explicit user rejection of these labels in this notebook. It is an authored preference, not a model-learned detector of vague labels. Without the filter, their scope descriptions remain ordinary candidates.

Required development regressions: Meetings #57 and Walking #72/#161 are present, and none of the 15 rejected probe memberships is present. Probe replacements include analyst suggestions and ambiguous socks IDs. Required-case checks are performed only after topic generation. Passing them is insufficient for promotion: positive pair retention and unknown memberships also need review.

| Model | Cores | Display filter | Slack | Rival margin | Preferred / 15 | Rejected / 15 | Meetings #57 | Both walks | All required | Dev positive / negative | Topics | Coverage | Multi-label notes |
|---|---|---|---:|---:|---:|---:|---|---|---|---:|---:|---:|---:|
| minilm | complete-link | no | 0.000 | 0.000 | 12 | 2 | yes | no | no | 19/33 / 1/13 | 35 | 72% | 0 |
| minilm | complete-link | yes | 0.000 | 0.000 | 12 | 2 | yes | no | no | 20/33 / 1/13 | 37 | 75% | 0 |
| minilm | complete-link | no | 0.000 | 0.025 | 11 | 1 | yes | no | no | 18/33 / 1/13 | 31 | 65% | 0 |
| minilm | complete-link | yes | 0.000 | 0.025 | 12 | 1 | yes | no | no | 20/33 / 1/13 | 34 | 69% | 0 |
| minilm | complete-link | no | 0.025 | 0.000 | 12 | 2 | yes | no | no | 20/33 / 1/13 | 48 | 84% | 20 |
| minilm | complete-link | yes | 0.025 | 0.000 | 12 | 2 | yes | no | no | 21/33 / 1/13 | 49 | 85% | 20 |
| minilm | complete-link | no | 0.025 | 0.025 | 11 | 1 | yes | no | no | 19/33 / 1/13 | 41 | 77% | 15 |
| minilm | complete-link | yes | 0.025 | 0.025 | 12 | 1 | yes | no | no | 21/33 / 1/13 | 44 | 80% | 15 |
| minilm | complete-link | no | 0.050 | 0.000 | 12 | 2 | yes | no | no | 21/33 / 1/13 | 60 | 89% | 38 |
| minilm | complete-link | yes | 0.050 | 0.000 | 12 | 2 | yes | no | no | 22/33 / 1/13 | 60 | 90% | 36 |
| minilm | complete-link | no | 0.050 | 0.025 | 11 | 1 | yes | no | no | 19/33 / 1/13 | 52 | 83% | 30 |
| minilm | complete-link | yes | 0.050 | 0.025 | 12 | 1 | yes | no | no | 21/33 / 1/13 | 54 | 85% | 30 |
| minilm | complete-link | no | 0.100 | 0.000 | 13 | 2 | yes | no | no | 23/33 / 1/13 | 100 | 95% | 87 |
| minilm | complete-link | yes | 0.100 | 0.000 | 13 | 2 | yes | no | no | 24/33 / 1/13 | 101 | 96% | 87 |
| minilm | complete-link | no | 0.100 | 0.025 | 12 | 1 | yes | no | no | 22/33 / 1/13 | 82 | 92% | 76 |
| minilm | complete-link | yes | 0.100 | 0.025 | 13 | 1 | yes | no | no | 24/33 / 1/13 | 88 | 94% | 79 |
| minilm | overlapping-cores | no | 0.000 | 0.000 | 11 | 2 | no | no | no | 20/33 / 1/13 | 60 | 73% | 0 |
| minilm | overlapping-cores | yes | 0.000 | 0.000 | 11 | 2 | no | no | no | 21/33 / 1/13 | 62 | 74% | 0 |
| minilm | overlapping-cores | no | 0.000 | 0.025 | 10 | 0 | no | no | no | 18/33 / 1/13 | 56 | 66% | 0 |
| minilm | overlapping-cores | yes | 0.000 | 0.025 | 11 | 0 | no | no | no | 20/33 / 1/13 | 57 | 68% | 0 |
| minilm | overlapping-cores | no | 0.025 | 0.000 | 11 | 2 | no | no | no | 21/33 / 1/13 | 68 | 80% | 10 |
| minilm | overlapping-cores | yes | 0.025 | 0.000 | 11 | 2 | no | no | no | 22/33 / 1/13 | 73 | 81% | 14 |
| minilm | overlapping-cores | no | 0.025 | 0.025 | 10 | 0 | no | no | no | 19/33 / 1/13 | 63 | 73% | 8 |
| minilm | overlapping-cores | yes | 0.025 | 0.025 | 11 | 0 | no | no | no | 21/33 / 1/13 | 66 | 76% | 11 |
| minilm | overlapping-cores | no | 0.050 | 0.000 | 11 | 2 | no | no | no | 21/33 / 1/13 | 93 | 87% | 32 |
| minilm | overlapping-cores | yes | 0.050 | 0.000 | 11 | 2 | no | no | no | 22/33 / 1/13 | 97 | 88% | 32 |
| minilm | overlapping-cores | no | 0.050 | 0.025 | 10 | 0 | no | no | no | 19/33 / 1/13 | 85 | 80% | 25 |
| minilm | overlapping-cores | yes | 0.050 | 0.025 | 11 | 0 | no | no | no | 21/33 / 1/13 | 89 | 83% | 25 |
| minilm | overlapping-cores | no | 0.100 | 0.000 | 12 | 2 | no | no | no | 22/33 / 1/13 | 164 | 90% | 80 |
| minilm | overlapping-cores | yes | 0.100 | 0.000 | 12 | 2 | no | no | no | 23/33 / 1/13 | 174 | 90% | 79 |
| minilm | overlapping-cores | no | 0.100 | 0.025 | 11 | 0 | no | no | no | 20/33 / 1/13 | 135 | 87% | 69 |
| minilm | overlapping-cores | yes | 0.100 | 0.025 | 12 | 0 | no | no | no | 22/33 / 1/13 | 152 | 88% | 75 |
| bge-small | complete-link | no | 0.000 | 0.000 | 11 | 1 | no | no | no | 16/33 / 0/13 | 33 | 78% | 0 |
| bge-small | complete-link | yes | 0.000 | 0.000 | 12 | 0 | no | no | no | 16/33 / 0/13 | 33 | 78% | 0 |
| bge-small | complete-link | no | 0.000 | 0.025 | 10 | 0 | no | no | no | 16/33 / 0/13 | 28 | 69% | 0 |
| bge-small | complete-link | yes | 0.000 | 0.025 | 11 | 0 | no | no | no | 16/33 / 0/13 | 31 | 72% | 0 |
| bge-small | complete-link | no | 0.025 | 0.000 | 13 | 1 | no | yes | no | 21/33 / 0/13 | 60 | 96% | 53 |
| bge-small | complete-link | yes | 0.025 | 0.000 | 14 | 0 | no | yes | no | 21/33 / 0/13 | 57 | 95% | 53 |
| bge-small | complete-link | no | 0.025 | 0.025 | 12 | 0 | no | no | no | 21/33 / 0/13 | 47 | 85% | 36 |
| bge-small | complete-link | yes | 0.025 | 0.025 | 13 | 0 | no | no | no | 21/33 / 0/13 | 47 | 87% | 40 |
| bge-small | complete-link | no | 0.050 | 0.000 | 14 | 1 | yes | yes | no | 23/33 / 0/13 | 84 | 97% | 88 |
| bge-small | complete-link | yes | 0.050 | 0.000 | 15 | 0 | yes | yes | yes | 23/33 / 0/13 | 83 | 97% | 90 |
| bge-small | complete-link | no | 0.050 | 0.025 | 12 | 0 | no | no | no | 22/33 / 0/13 | 64 | 90% | 66 |
| bge-small | complete-link | yes | 0.050 | 0.025 | 13 | 0 | no | no | no | 23/33 / 0/13 | 67 | 94% | 74 |
| bge-small | complete-link | no | 0.100 | 0.000 | 14 | 1 | yes | yes | no | 24/33 / 0/13 | 155 | 98% | 132 |
| bge-small | complete-link | yes | 0.100 | 0.000 | 15 | 0 | yes | yes | yes | 24/33 / 0/13 | 158 | 98% | 131 |
| bge-small | complete-link | no | 0.100 | 0.025 | 12 | 0 | no | no | no | 24/33 / 0/13 | 114 | 96% | 120 |
| bge-small | complete-link | yes | 0.100 | 0.025 | 13 | 0 | no | no | no | 24/33 / 0/13 | 133 | 98% | 129 |
| bge-small | overlapping-cores | no | 0.000 | 0.000 | 12 | 0 | no | yes | no | 19/33 / 0/13 | 56 | 72% | 0 |
| bge-small | overlapping-cores | yes | 0.000 | 0.000 | 13 | 0 | no | yes | no | 19/33 / 0/13 | 56 | 74% | 0 |
| bge-small | overlapping-cores | no | 0.000 | 0.025 | 10 | 0 | no | no | no | 19/33 / 0/13 | 48 | 66% | 0 |
| bge-small | overlapping-cores | yes | 0.000 | 0.025 | 11 | 0 | no | no | no | 19/33 / 0/13 | 48 | 68% | 0 |
| bge-small | overlapping-cores | no | 0.025 | 0.000 | 13 | 1 | yes | yes | no | 24/33 / 0/13 | 95 | 87% | 54 |
| bge-small | overlapping-cores | yes | 0.025 | 0.000 | 14 | 0 | yes | yes | yes | 24/33 / 0/13 | 100 | 87% | 55 |
| bge-small | overlapping-cores | no | 0.025 | 0.025 | 10 | 0 | no | no | no | 24/33 / 0/13 | 79 | 80% | 38 |
| bge-small | overlapping-cores | yes | 0.025 | 0.025 | 11 | 0 | no | no | no | 24/33 / 0/13 | 83 | 81% | 41 |
| bge-small | overlapping-cores | no | 0.050 | 0.000 | 13 | 1 | yes | yes | no | 26/33 / 0/13 | 164 | 89% | 84 |
| bge-small | overlapping-cores | yes | 0.050 | 0.000 | 14 | 0 | yes | yes | yes | 26/33 / 0/13 | 165 | 89% | 87 |
| bge-small | overlapping-cores | no | 0.050 | 0.025 | 11 | 0 | no | no | no | 25/33 / 0/13 | 121 | 83% | 65 |
| bge-small | overlapping-cores | yes | 0.050 | 0.025 | 12 | 0 | no | no | no | 26/33 / 0/13 | 137 | 87% | 74 |
| bge-small | overlapping-cores | no | 0.100 | 0.000 | 14 | 1 | yes | yes | no | 29/33 / 0/13 | 303 | 89% | 120 |
| bge-small | overlapping-cores | yes | 0.100 | 0.000 | 15 | 0 | yes | yes | yes | 29/33 / 1/13 | 321 | 89% | 119 |
| bge-small | overlapping-cores | no | 0.100 | 0.025 | 12 | 0 | no | no | no | 29/33 / 0/13 | 211 | 87% | 102 |
| bge-small | overlapping-cores | yes | 0.100 | 0.025 | 13 | 0 | no | no | no | 29/33 / 1/13 | 260 | 89% | 116 |
| granite-small | complete-link | no | 0.000 | 0.000 | 10 | 0 | no | no | no | 17/33 / 1/13 | 41 | 81% | 0 |
| granite-small | complete-link | yes | 0.000 | 0.000 | 11 | 0 | no | no | no | 20/33 / 1/13 | 41 | 84% | 0 |
| granite-small | complete-link | no | 0.000 | 0.025 | 9 | 0 | no | no | no | 13/33 / 0/13 | 25 | 49% | 0 |
| granite-small | complete-link | yes | 0.000 | 0.025 | 10 | 0 | no | no | no | 16/33 / 0/13 | 26 | 55% | 0 |
| granite-small | complete-link | no | 0.025 | 0.000 | 11 | 0 | no | no | no | 22/33 / 1/13 | 75 | 96% | 72 |
| granite-small | complete-link | yes | 0.025 | 0.000 | 12 | 0 | no | no | no | 25/33 / 1/13 | 73 | 97% | 69 |
| granite-small | complete-link | no | 0.025 | 0.025 | 10 | 0 | no | no | no | 14/33 / 0/13 | 37 | 67% | 11 |
| granite-small | complete-link | yes | 0.025 | 0.025 | 11 | 0 | no | no | no | 18/33 / 0/13 | 47 | 76% | 37 |
| granite-small | complete-link | no | 0.050 | 0.000 | 11 | 0 | no | no | no | 25/33 / 1/13 | 131 | 98% | 116 |
| granite-small | complete-link | yes | 0.050 | 0.000 | 12 | 0 | no | no | no | 27/33 / 1/13 | 134 | 99% | 116 |
| granite-small | complete-link | no | 0.050 | 0.025 | 10 | 0 | no | no | no | 16/33 / 0/13 | 53 | 76% | 35 |
| granite-small | complete-link | yes | 0.050 | 0.025 | 11 | 0 | no | no | no | 20/33 / 1/13 | 81 | 90% | 83 |
| granite-small | complete-link | no | 0.100 | 0.000 | 13 | 0 | yes | no | no | 28/33 / 1/13 | 236 | 99% | 161 |
| granite-small | complete-link | yes | 0.100 | 0.000 | 14 | 0 | yes | no | no | 28/33 / 1/13 | 263 | 99% | 161 |
| granite-small | complete-link | no | 0.100 | 0.025 | 11 | 0 | no | no | no | 17/33 / 0/13 | 73 | 82% | 67 |
| granite-small | complete-link | yes | 0.100 | 0.025 | 12 | 0 | no | no | no | 27/33 / 1/13 | 166 | 98% | 157 |
| granite-small | overlapping-cores | no | 0.000 | 0.000 | 10 | 0 | no | no | no | 18/33 / 1/13 | 58 | 78% | 0 |
| granite-small | overlapping-cores | yes | 0.000 | 0.000 | 11 | 0 | no | no | no | 20/33 / 1/13 | 61 | 81% | 0 |
| granite-small | overlapping-cores | no | 0.000 | 0.025 | 9 | 0 | no | no | no | 11/33 / 0/13 | 38 | 48% | 0 |
| granite-small | overlapping-cores | yes | 0.000 | 0.025 | 10 | 0 | no | no | no | 13/33 / 0/13 | 43 | 53% | 0 |
| granite-small | overlapping-cores | no | 0.025 | 0.000 | 10 | 0 | no | no | no | 22/33 / 1/13 | 134 | 90% | 72 |
| granite-small | overlapping-cores | yes | 0.025 | 0.000 | 11 | 0 | no | no | no | 24/33 / 1/13 | 138 | 90% | 69 |
| granite-small | overlapping-cores | no | 0.025 | 0.025 | 9 | 0 | no | no | no | 12/33 / 0/13 | 56 | 63% | 8 |
| granite-small | overlapping-cores | yes | 0.025 | 0.025 | 10 | 0 | no | no | no | 16/33 / 0/13 | 83 | 72% | 35 |
| granite-small | overlapping-cores | no | 0.050 | 0.000 | 12 | 0 | yes | no | no | 23/33 / 1/13 | 235 | 92% | 114 |
| granite-small | overlapping-cores | yes | 0.050 | 0.000 | 13 | 0 | yes | no | no | 25/33 / 1/13 | 257 | 93% | 116 |
| granite-small | overlapping-cores | no | 0.050 | 0.025 | 9 | 0 | no | no | no | 13/33 / 0/13 | 80 | 73% | 31 |
| granite-small | overlapping-cores | yes | 0.050 | 0.025 | 10 | 0 | no | no | no | 17/33 / 1/13 | 152 | 87% | 79 |
| granite-small | overlapping-cores | no | 0.100 | 0.000 | 13 | 0 | yes | no | no | 26/33 / 1/13 | 403 | 93% | 150 |
| granite-small | overlapping-cores | yes | 0.100 | 0.000 | 14 | 0 | yes | no | no | 26/33 / 1/13 | 511 | 93% | 150 |
| granite-small | overlapping-cores | no | 0.100 | 0.025 | 11 | 0 | no | no | no | 15/33 / 0/13 | 111 | 78% | 61 |
| granite-small | overlapping-cores | yes | 0.100 | 0.025 | 12 | 0 | no | no | no | 25/33 / 1/13 | 327 | 92% | 145 |

Larger slack admits more compatible memberships but can increase unsupported labels and topic proliferation. Rival margins are raw model scores, not calibrated probabilities. Most note/label memberships remain unreviewed; zero measured rejections is not zero false memberships. No parameters were selected on transfer. Existing model licensing evidence remains unchanged.

[Full memberships and per-case outcomes](EMBEDDING_COMPATIBLE_RESULTS.json).
