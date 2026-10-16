# Fixed-group label comparison protocol

Granite-small-English-R2 and complete-link groups from the general comparison are frozen. Both the 172-note development notebook and the 40-note previously observed transfer notebook are included. No memberships, clustering thresholds, subject taxonomy, authored label scopes, or review-based exceptions are changed.

Three main variants, fixed before phrase embedding inference:

- A: current c-TF-IDF-like unigram/bigram term ranking, including its existing stopword-before-bigram behavior and token-count weighting.
- B: actual noun-phrase spans (one to four tokens) extracted locally with spaCy English small. Endpoints must be non-stopword nominal/modified-nominal spans inside a parsed noun chunk. Candidates require support from at least two distinct group members. Class-based weighting uses per-note binary support, so repetition within one note adds no votes.
- C: same candidate pool as B, reranked with the existing Granite encoder. Rank-fuse equally: member semantic coverage (65% mean + 35% mean of weakest quarter), distinctiveness (mean member similarity minus strongest other-group mean similarity), and B's lexical score. Report component cosines and ranks. Rank fusion avoids treating raw Granite cosine and lexical weights as equivalent scales. Ties resolve by higher distinct-note support then lexical phrase text. MMR diversifies the five alternatives using 0.8 relevance / 0.2 cosine redundancy; first label is unchanged by MMR.

A noun-phrase/token-count auxiliary variant holds A's counting method constant to help distinguish phrase extraction from per-note support weighting. It is a diagnostic, not an additional approach family.

Use the same pinned Granite tokenizer/pooling, normalized FP32 CPU vectors, four threads and no retrieval prompt. Verify artifact and input/vector hashes. Parser runs in an isolated pinned environment; it reads note text only, and inference stays local. spaCy code and English small model declare MIT licensing; preserve the model license and third-party notices. No training data or new Android dependencies are introduced.

Report all groups, source snippets, top five candidates, component scores, literal support, duplicate top labels, candidate availability, and alternative-list semantic redundancy. These are automatic diagnostics, not human label accuracy. Higher within-model cosine alone cannot validate semantic reranking. Blind review of informativeness and group fit remains necessary; do not invent reviewer decisions or tune weights to the already reported label mismatches.

## Follow-up extraction control

The first run left 6/32 development and 4/11 transfer groups without noun-supported candidates. Before any second-stage parameter tuning, add D: union of source noun phrases and real contiguous source unigrams/bigrams/trigrams, filtered only at stopword endpoints and phrase punctuation/newline boundaries. Keep the same two-note support minimum, binary weights, semantic rank fusion and MMR settings as C. No particular rejected or preferred label is inserted, excluded or boosted. D addresses general candidate recall; report it separately from the original three variants. Also report semantic diagnostics on common groups to avoid comparing averages over different candidate-availability subsets.
