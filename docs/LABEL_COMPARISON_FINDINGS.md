# Label comparison findings

Completed on the fixed Granite + complete-link groups: 32 development groups and 11 groups from the previously observed transfer corpus. Memberships and threshold 0.775 were unchanged. [All labels and notes](LABEL_COMPARISON.md), [scores/source spans](LABEL_COMPARISON_RESULTS.json), and [fixed protocol](LABEL_COMPARISON_PROTOCOL.md) are saved.

## Outcome

No variant is established as a uniformly better final naming method. Candidate generation and semantic ranking expose different weaknesses; higher embedding scores alone cannot choose the winner.

| Variant | Groups with supported candidates / 43 | Finding |
|---|---:|---|
| Current c-TF-IDF-like terms | 43 | Best availability. Some alternatives are constructed by skipping stopwords; labels can be generic words or incidental details. |
| Noun phrases + distinct-note support | 33 | Natural source spans, but the noun-only restriction plus repeated-phrase requirement loses useful candidates. It can prefer Phrases over French because grammatical filtering narrows the pool. |
| Noun phrases + semantic reranking | 33 | More descriptive names in some groups: search → atlas search; bike → bike light. Other choices remain generic or become incidental details. |
| Hybrid source/noun phrases + semantic reranking | 38 | Better candidate recall than noun-only extraction. Produces useful-looking candidates such as meal prep and running shoes, but also 80 kg and balcony rosemary, which can overstate a detail or one subgroup. |

The auxiliary noun/token control separates extraction from per-note binary weighting. MMR has a small effect on the noun-semantic alternatives in development (mean pair cosine approximately 0.805 → 0.803) and essentially none in transfer. It leaves the leading label unchanged and does not solve label informativeness.

All variants' means are reported both over their available groups and over identical common groups. On the 26 common development groups, member cosine increases from about 0.806 for the baseline to 0.826 for the hybrid. This is an optimized diagnostic, not independent evidence that humans will prefer its labels. Literal member support drops from roughly 75.5% to 69.2%. On the seven common transfer groups, cosine remains about 0.809 for baseline and hybrid; their top-label literal support is the same, roughly 67.9%.

## Recommended next direction

Keep current term ranking as the reference, and carry hybrid candidate generation plus semantic ranking forward as the general alternative. Treat noun phrases as an additional candidate source, rather than a hard grammatical eligibility gate. Keep the original baseline and the observed regressions visible; do not add excluded words, authored topic names, or special treatment for the examples above.

Candidate recall is the immediate limiting factor: a two-note exact-source requirement can miss common concepts expressed with different wording. A general next comparison should test broader source-phrase recall and lemma-based support with original display wording preserved. Separately evaluate whether robust member support, contrast and representative-note evidence produce more informative labels. Freeze group memberships throughout that study. Do not tune those checks to make named examples pass.

Review labels for informativeness, scope fit and useful specificity, preferably blind to the method. The extraction tests and diagnostic scores establish implementation behavior; they do not replace that review or establish final label accuracy. No app or Android change is included.

Parser/model licensing evidence is saved alongside the existing encoder evidence; the isolated parser uses published MIT-licensed spaCy and English-small artifacts. Thirty-six main harness tests and two isolated parser tests passed.
