# General processing shortlist

Updated 2026-09-30 following the user's direction: identify two or three broadly useful processing approaches for further testing. Do not add rules for individual notes, authored subject rival graphs, or named-label exclusions to make the observed review examples pass.

The earlier supplied-scope experiments are set aside from shortlist selection. Their outputs remain historical diagnostics; any gains from manually defining scopes or suppressing Chapter/Phrases do not establish a generally better pipeline. Review examples belong in evaluation, not algorithm exceptions.

## Shortlist

| Priority | General pipeline | Why retain it | Main limitation to investigate |
|---|---|---|---|
| Primary | Normalized sentence embeddings → complete-link hierarchical clustering → corpus-derived phrase representation | Strongest current balance of reviewed-pair recovery and update stability, especially with Granite. Simple baseline, no fixed taxonomy, controllable granularity. | Disjoint groups can miss legitimate overlaps; raw similarity cutoffs need model-level calibration. High coverage can conceal overly broad clusters. |
| Primary | Normalized sentence embeddings → HDBSCAN density clustering → class-based TF-IDF representation | Finds fewer clusters without choosing a topic count and can leave weakly supported notes ungrouped. A useful independent alternative to threshold merging. | In the current test it misses more reviewed connections. Density structure and memberships can change substantially as notes are added or removed. |
| Third, if overlapping membership is a product requirement | Normalized sentence embeddings → reciprocal-neighbor coherent cores → generic overlap consolidation → corpus-derived phrase representation | Recovers the most reviewed connections with BGE and naturally supports multiple memberships without predeclared categories. | Too many topics and more negative co-memberships. Consolidation helps but does not establish useful granularity. |

This is a shortlist for comparative testing, not a claim of universal superiority. [344-configuration comparison](GENERAL_GROUPING_COMPARISON.md) and [full memberships/terms](GENERAL_GROUPING_RESULTS.json) support the tradeoffs. All selected configurations passed an input-permutation check. Addition/removal stability is measured on memberships, not persistent topic IDs.

The density experiment uses scikit-learn HDBSCAN directly on normalized embedding vectors, without UMAP. It is a density/representation baseline, not a test of the complete default BERTopic pipeline. A reduced-dimension density variant can be a general ablation later; keep it inside this family rather than adding an unrelated candidate for every parameter change. References: [HDBSCAN documentation](https://scikit-learn.org/stable/modules/generated/sklearn.cluster.HDBSCAN.html), [BERTopic c-TF-IDF](https://maartengr.github.io/BERTopic/getting_started/ctfidf/ctfidf.html).

## Current evidence

Granite complete-link recovers 28/33 development positives with 1/13 explicit negatives, and 38/69 transfer positives with 0/5 negatives. Its membership-pair stability is 0.948 after additions and 0.812 after removals.

Granite density clustering yields 17 topics, covering 64% of development notes, with 18/33 positives and 0/13 negatives. It recovers 55/69 transfer positives with 1/5 negatives, but stability after additions is only 0.403. BGE density also yields 17 topics and 0/13 development negatives, with different retention/stability tradeoffs.

BGE overlapping cores with generic consolidation recover 30/33 development positives and 68/69 transfer positives, while retaining 1/13 and 3/5 explicit negatives respectively. It still produces 69 topics for 172 notes. High pair recovery alone is not a sufficient reason to choose it.

The models and clustering families interact; do not first crown an embedding and assume it will win with every algorithm. Keep MiniLM as the current-model reference, with BGE and Granite as candidate encoders. First-round weights remain MIT/Apache licensed with saved commercial-use evidence. Existing scikit-learn is BSD licensed; no new dependency or model is introduced.

## Shared labeling comparison

The completed general test uses the same corpus-derived lexical representation for every family: observed unigrams/bigrams, standard English stopwords, class-based TF-IDF-like ranking, and centroid-nearest representative notes. Per-note term counts are aggregated, avoiding invented bigrams across note boundaries. There is no supplied category list or custom excluded-word list.

These top terms are candidate descriptions, not validated final names. Naming quality still needs a separate comparison: plain class-based TF-IDF versus semantic reranking of corpus-derived phrases, with generic member coverage and redundancy checks. Apply each labeling variant to the same fixed groups so improvements can be attributed to labeling rather than changed membership. Preserve awkward outputs for evaluation instead of banning their words to improve the score.

## Further testing plan

1. Freeze these three families and a small encoder set. Compare general configurations, not manually authored exceptions. Keep all preprocessing and labeling identical while evaluating clustering.
2. Build several independent notebooks with diverse subjects, incidental mentions, genuine overlaps, small topics, and isolated notes. Obtain human annotations for useful memberships and labels; reserve whole notebooks for final evaluation. Synthetic examples can exercise behavior but cannot substitute for human preference evidence.
3. Calibrate global/model-specific parameters on development notebooks, then apply them unchanged to the reserved notebooks. Selected positive/negative pairs in the current 172-note corpus and the already observed 40-note corpus remain development/regression diagnostics.
4. Measure false membership, missed membership, ungrouped notes, redundant topics, useful topic count, label informativeness and label coverage. Use blind review of extracted names and representative notes. Do not treat unknown pairs as negative or zero reviewed errors as perfect precision.
5. Measure additions, deletions and edits, including small versus large notebooks; assess stable subject membership and persistent identity separately. Report size, latency and memory on the laptop before any Android work.
6. Carry forward two finalists after this general comparison. Then implement an app experiment with reversible model/version changes and the existing identity reconciliation, using commercially eligible weights and runtimes throughout.

The current small observed datasets support prioritizing these families for testing; they cannot establish which will be generally best in the sellable product.

## Selected starting point and immediate labeling work

The user selected Granite-small-English-R2 plus complete-link hierarchical clustering as the starting combination. Keep its groups fixed during the next labeling comparison; the current development-selected similarity threshold is 0.775, not a shipping constant.

The first concrete candidate-extraction issue is stopword handling: the current CountVectorizer removes stopwords before constructing bigrams. For example, `notes about music` produces the candidate `notes music`, although that phrase was never written. Use actual contiguous source spans, or noun phrases extracted by a POS/dependency model, so candidate labels preserve natural wording. This is a general text-processing correction, not a subject-specific exclusion.

Compare three labeling variants on the same Granite groups:

1. Existing class-based TF-IDF-like unigram/bigram ranking as the baseline.
2. Actual source phrases / noun phrases, ranked using class-based TF-IDF and support across distinct notes. Avoid rewarding repetition in a single note as though many members support a label.
3. The same phrase candidates semantically reranked with Granite. Combine representative-member relevance, broader member coverage, and contrast against other groups; report component scores. Apply MMR to alternative labels to reduce redundant variants. Low evidence should be visible rather than forcing a confident name.

Candidate sources stay open-world: no authored subjects, note-specific exceptions or banned label names. Parsing and semantic reranking do not guarantee informative labels, so assess candidate readability and group coverage with blind review; similarity scores alone cannot establish success. Keep clustering fixed until label differences have been assessed.

BERTopic documents separate POS, KeyBERT-inspired semantic reranking and MMR representation stages: [representation methods](https://maartengr.github.io/BERTopic/getting_started/representation/representation.html). spaCy supports [noun-phrase extraction](https://spacy.io/usage/linguistic-features#noun-chunks), and its published English small pipeline lists an MIT model license ([model page](https://spacy.io/models/en)). A parser is optional for this laptop experiment; pin/review its exact artifact and dependencies before introducing it. Semantic ranking can reuse the already reviewed Granite model without installing KeyBERT or BERTopic. No LLM generation is required.

The fixed Granite-group label comparison is complete: [findings](LABEL_COMPARISON_FINDINGS.md) and [side-by-side labels with all member notes](LABEL_COMPARISON.md). Noun-only extraction has insufficient candidate recall (33/43 groups). Hybrid source/noun extraction with semantic ranking reaches 38/43, with mixed naming quality and no established winner. Keep the current method as a reference and hybrid as the next general alternative; preserve memberships during label review.
