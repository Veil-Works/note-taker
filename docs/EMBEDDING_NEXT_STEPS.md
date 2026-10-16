# Embedding findings and next experiment

**Current direction (2026-09-30):** prioritize [general processing families](GENERAL_PROCESSING_SHORTLIST.md). Supplied-scope, rival-graph and named-label exclusion experiments below are historical diagnostics, set aside from approach selection. Do not extend their case-specific rules. The active comparison is [general clustering plus corpus-derived representations](GENERAL_GROUPING_COMPARISON.md).

English-first laptop comparison completed across 172 reviewed notes and a 40-note previously observed transfer corpus. See [measurements](EMBEDDING_COMPARISON.md), [protocol](EMBEDDING_BENCHMARK_PROTOCOL.md), and [commercial-use evidence](embedding-licenses/README.md).

## Findings

There is no decisive replacement for the current bundled MiniLM on these fixtures.

- Granite improves reviewed-pair ROC AUC from 0.902 to 0.951 and known-positive recall@10 from 0.939 to 0.985. It is the strongest candidate for a second-stage grouping experiment, but wins only 9/15 scope contrasts versus the current model's 11/15. Its transfer recall@10 is slightly lower (0.964 versus 0.971).
- BGE has the best development recall@5 (0.864 versus 0.803) but lower recall@10 and weaker transfer retrieval. It improves the two walking-versus-meetings margins substantially, yet still prefers meetings on one of them and prefers meeting planning for the meeting-ideas note.
- FP32 MiniLM is close to the bundled quantized model. Quantization alone does not explain the reported grouping problems.
- Every model already prefers Rosemary to Basil for the rosemary-pot note. Every model also prefers Running to Running gear for the two plain-run examples, and Meal preparation to Running gear for the sandwich note. The existing grouping or label policy can override useful distinctions already present in the vectors.
- Focused-work notes mentioning chat and walking notes mentioning meetings remain difficult. Embeddings recognize related words and subjects; they do not reliably distinguish the note's main subject from incidental context. Bare-label similarity is also sensitive to wording and cannot serve as a complete label validator.

These differences are descriptive, not statistically established superiority. The pair sets are small, the transfer corpus has only five explicit negative pairs, and some preferred labels are proposals. Timing compares different representations/runtime settings; it cannot predict Android speed or battery use.

## Proposed operation plan

1. Keep bundled MiniLM in the app while developing the next stage on this laptop. Carry MiniLM and Granite into that stage; retain BGE as a secondary retrieval comparison.
2. Extend human annotations with explicit inclusion/exclusion examples for Running, Running gear, Meals, Walking, Meetings, Focused work, and individual herbs. Resolve the ambiguous socks note and the desired overlap policy. Create a fresh untouched test corpus before tuning.
3. Compare candidate grouping policies on fixed model vectors: mutual-neighbor/cohesive cores, multi-membership assignment, and rejection when evidence is weak. Keep grouping quality separate from label quality. Calibrate thresholds separately for each model on development data.
4. Validate a proposed label against representative members and counterexamples. Compare bare names with short scope descriptions and positive/negative exemplars. Reject vague shared fragments such as Phrases, Chapter and Cancel when they do not describe a coherent subject. Test whether one Meetings topic better matches the annotations than an unnecessary Meeting planning split.
5. Measure false membership, missing membership, label support and update stability end to end. Better pair retrieval alone is insufficient to promote a model. Keep existing identity reconciliation downstream of discovery: identities follow compatible subject/member evidence, not raw embedding closeness. On any eventual model change, re-embed all notes under a new embedding version before rediscovery; never mix vector spaces or reuse old similarity thresholds.
6. Only after a clear quality gain, export/quantize the candidate, rerun laptop parity/quality checks, preserve license and conversion notices, then schedule Android size/latency testing separately.

Commercial-use eligibility remains mandatory: first-round candidates have Apache-2.0 or MIT weight grants, with recorded revisions and publisher evidence. Future models, runtimes and training data must receive the same intended-use review; noncommercial/research-only assets are excluded.

## Follow-up tests — 2026-09-30

Completed 408 model/policy/threshold configurations on the laptop, plus scope-description and first-sentence experiments. [Full comparison](EMBEDDING_GROUPING_COMPARISON.md) and [machine-readable results](EMBEDDING_GROUPING_RESULTS.json) include memberships and all threshold sweeps. Nine benchmark harness tests pass; all 12 selected grouping configurations also pass a fixed-seed input-permutation check. Addition/removal tests measure membership stability, not saved-ID reconciliation.

The follow-up changes the shortlist emphasis: **retain BGE alongside Granite**, because BGE has competitive complete-link grouping and the strongest description-based contrasts here. There is still no justified production winner.

- Granite with complete-link grouping recovers 28/33 reviewed positive pairs with 1/13 explicit-negative pairs grouped together. At its unchanged development-selected threshold, it recovers 38/69 transfer positives with 0/5 explicit negatives. Its pair stability is 0.948 after adding the last 20% of notes and 0.812 after removing every fifth note.
- BGE with complete-link grouping recovers 25/33 positives with 0/13 negatives on development, and 47/69 positives with 1/5 negatives on transfer. It is less stable under the tested removals (0.583).
- BGE overlapping cores recover 30/33 development and 67/69 transfer positives, but also group 3/5 transfer negatives together and produce 108 topics for 172 notes. High retrieval alone is insufficient; this policy needs consolidation and scope validation before it can serve as app discovery.
- Mutual-neighbor connected components can form broad chains; threshold calibration for Granite avoids many false links by dropping most transfer positives (2/69). Do not promote it on development recall or graph connectivity alone.
- Scoped descriptions improve preferred-label contrasts from 10 to 13 of 15 for MiniLM, 11 to 14 for BGE, and 9 to 12 for Granite. Adding first-sentence evidence raises Granite to 13. These are targeted development probes, not independent classification accuracy. Scope descriptions regress Chapter versus Reading notes for BGE and Granite, showing that label quality cannot be reduced to a single cosine comparison.
- All three descriptions prefer Focused work over Chat on both reported notes. First-sentence focus corrects the first walking contrast for Granite; the second walking note remains difficult for MiniLM and Granite. BGE favors Walking for both, but the second margin is only about +0.009 and should be treated as weak evidence.
- At their selected complete-link thresholds, all four representations group the basil and rosemary notes together. This is compatible with a general plant-care group, but it still requires species-specific validation before using Basil as its label. All also join the running/cycling route pair; a coherent pair is not automatically a useful Path topic. Granite and MiniLM still join the two unrelated cancellation notes. These cases reinforce the need for label support and explicit scope constraints.

Next experiment should combine coherent core construction with scope-supported membership and consolidation, then evaluate end-to-end discovery/naming against a fresh human-reviewed corpus. Scope descriptions authored from these examples cannot establish generalization to unseen subjects, and the current pair annotations leave most memberships unknown. Keep model-specific thresholds and commercially eligible artifacts throughout.

## Combined scope and consolidation tests — 2026-09-30

Completed six coherence-preserving consolidation variants and 36 combined grouping/scope variants across MiniLM, BGE and Granite. [Comparison](EMBEDDING_SCOPED_COMPARISON.md) and [full outcomes](EMBEDDING_SCOPED_RESULTS.json) preserve group memberships and every scope probe. Sixteen harness tests pass, including weak-bridge rejection, preservation of existing pairs during consolidation, and splitting a mixed core into two supported subjects.

At overlap Jaccard 0.5, requiring every pair in a merged union to clear the original core threshold reduces MiniLM topics from 104 to 82, BGE from 108 to 69, and Granite from 103 to 78. All reviewed positive and negative co-member counts remain unchanged on development and transfer. This demonstrates some useful consolidation, but the remaining topic counts are still high and existing false memberships survive. Overlap 0.75 does not consolidate these development cores.

The scope stage uses the 16 previously authored descriptions; many notebook subjects are absent. It is a controlled supplied-vocabulary experiment, not an open-world labeling solution. Inference and pruning do not consume annotation IDs or memberships. Every fixed margin and variant is reported; no winner is chosen on transfer.

Choosing one scope for an entire core and pruning dissenting notes is too destructive. Splitting the core into individually supported scopes improves retention: for BGE overlapping cores at zero margin, preferred scope memberships rise from 10/15 to 12/15 while rejected memberships remain 0/15. Both walking notes now have Walking membership. However, the meeting-ideas note #57 still lacks Meetings, and proposed Reading notes #145 and Note-taking #63 are absent. Only 19/33 reviewed positive pairs remain, down from 30/33 in the unlabeled cores. Scope coverage is about 72%; unknown memberships are not counted as false.

MiniLM split overlapping cores preserve 11/15 preferred scopes but retain the two rejected Meetings memberships for walking notes. Granite preserves 10/15 preferred scopes and no rejected scopes, while missing both Walking memberships and Meetings #57. A zero-rejection result therefore cannot be described as solving these cases.

Larger global margins discard much of the notebook, especially for Granite. Raw margin values are not interchangeable across models. They should not become product thresholds based on these observed probes.

The current global competition also exposes a design problem: Meetings and Note-taking can be compatible useful memberships, whereas Running activity versus Running gear requires the narrower reviewed scope. A single winner across all descriptions treats both relationships identically. Next tests should distinguish incompatible scope rivals from compatible overlaps, require independently supported memberships, and preserve #57 as an explicit positive regression. Open-world candidate generation and representative descriptions remain necessary before these supplied labels can become a production discovery/naming pipeline. A fresh human-reviewed evaluation corpus is still required before selecting a replacement model or calibrating shipping thresholds.
