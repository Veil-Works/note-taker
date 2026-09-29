# Grouping and labeling: research and proposed operation plan

Research date: 2026-09-29. Status: proposal, not an implemented replacement.

The immediate experimental environment is this laptop. Embedding and grouping
benchmarks must run independently of Android. Android export, integration, and
physical-device performance testing follow only after desktop quality results
justify a candidate. No new model benchmark was run during this research.

## Recommendation

Keep organization non-generative. Compare compact sentence encoders and standard
topic-discovery methods, then independently test whether a discriminative model
can validate note-to-topic membership and label scope. Fix topic identity
reconciliation independently: a model upgrade cannot correct persistence logic
that combines unrelated discoveries under one saved topic.

The actual task is discovering an unknown number of overlapping topics in short
notes, selecting accurate names, and preserving topic identities through notebook
growth and user edits. It is not simply partitioning documents into clusters.

## Current implementation and evidence

- `organization/TextEmbedding.kt`: bundled int8 all-MiniLM-L6-v2 through ONNX
  Runtime; 384-dimensional vectors; normalized mean pooling and aggregation of
  long-note chunks. Whole-note embeddings are cached by content and model version.
- `organization/TopicDiscovery.kt`: recurrent capitalized subjects, greedy generic
  cores, up to five fixed representatives, whole-note and clause expansion,
  consolidation and optional splitting. Memberships can overlap.
- Naming combines observed phrases with a fixed purpose vocabulary, scored using
  whole-note/opening-text similarity and lower-tail member agreement.
- `organization/NotebookOrganizer.kt`: matches user-defined topics using names,
  aliases and manual examples; applies exclusions; reconciles discovery output
  with saved topics using content fingerprints and membership overlap.
- Manual assignments are positive examples; automatic assignments are not.
  Keep/Remove already provide the foundation for personalization.

The [172-note audit](EXPANDED_GROUPING_REVIEW.md) separates three failure classes:

| Failure | Example | Implication |
| --- | --- | --- |
| Membership | Soup preparation enters Running through “work runs late”; unrelated cancellations form Cancel | Shared vocabulary and proximity do not establish a common subject |
| Label scope | Ordinary runs enter Running gear; exercise logs receive Workout gear | Qualifiers need evidence, including for original core members |
| Identity | Separate Balcony, Basil and Library discoveries reuse Balcony rosemary | Identity reconciliation can corrupt otherwise separate discoveries |

The [rule comparison](GROUPING_RULE_BALANCE.md) tested 11 policies. Raising thresholds
removed useful memberships, and none recovered the selected missing photography
pairs. Photography notes fail before naming: named notes are excluded from generic
core seeding, and other photography pairs score below seed thresholds. These are
diagnostic findings, not an overall accuracy estimate.

The [optional Qwen comparison](HYBRID_COMPARISON_FIRST_PASS.md) does not establish
a production-quality generative replacement. On 172 notes, the rules recovered
25/33 selected desired pairs and included 10/13 selected unwanted pairs; the hybrid
recovered 18/33 and included 2/13, trading fewer selected errors for fragmentation
and lost relationships. These selected pairs are not exhaustive ground truth.

## What current research supports

There is no demonstrated universal winner for this complete application task.
[MMTEB](https://arxiv.org/abs/2502.13595) is useful for selecting embedding candidates,
but task-specific scores and notebook evaluation matter more than aggregate rank.
A [2025 human evaluation](https://aclanthology.org/2025.acl-long.375/) found that more
readable LLM topics can be overly generic and that model suitability depends on the
application. Benchmark results on large corpora do not establish reliable behavior
for two-note topics, overlapping memberships, or persistent identities.

### Approaches to compare

| Approach | Mechanism | Proposed role and limitation |
| --- | --- | --- |
| TF-IDF + NMF | Factorize weighted word usage into topics and document weights | Cheap interpretable baseline; likely weak on short-note paraphrases |
| Embeddings + agglomerative clustering | Merge semantically close notes with controlled linkage | Simple baseline; needs explicit overlap and naming policy |
| BERTopic | Embeddings, dimensionality reduction, clustering, c-TF-IDF | Standard non-generative comparator; ordinary clustering does not itself solve multilabel assignment |
| Contextual Top2Vec | Contextual token embeddings and topic spans | Directly relevant to mixed-subject notes; experimental implementation and more representation work |
| FASTopic | Optimal transport among document, topic and word embeddings | Alternative document-topic mixtures; requires a topic count |
| S³ | Independent Component Analysis of contextual document embeddings | Efficient alternative to clustering; components still need product-level interpretation |
| NLI / topic-membership classifier | Score text against a candidate topic statement | Explicit scope verification; adds inference cost and needs calibration |
| Few-shot personalization | Classifier or metric learning from reviewed examples | Learns user intent; insufficient examples during cold start |

[BERTopic](https://maartengr.github.io/BERTopic/algorithm/algorithm.html) supports
non-generative term representations, including KeyBERT-inspired and part-of-speech
methods. Its [topic distributions](https://maartengr.github.io/BERTopic/getting_started/visualization/visualization.html)
can support overlap experiments, but must not be treated as calibrated correctness
probabilities. Compare the usual UMAP/HDBSCAN pipeline with an agglomerative baseline
and a reduction-free variant rather than assuming UMAP helps small notebooks.

[Contextual Top2Vec](https://github.com/ddangelov/Top2Vec) identifies multiple topics
and spans within documents and supports phrase representations. It supports MiniLM,
allowing a representation-strategy comparison without changing the encoder. Its
contextual implementation is marked beta.

[FASTopic, NeurIPS 2024](https://github.com/bobxwu/FASTopic) produces topic and
document-topic representations through optimal transport.
[S³, ACL 2025](https://aclanthology.org/2025.acl-long.32/) finds independent semantic
axes using ICA. Both are useful research comparators; neither is established as
superior on this notebook. Evaluate their topic-count sensitivity and stability.

[Constrained NMF, EMNLP 2025](https://aclanthology.org/2025.emnlp-main.1802/) shows a
guided alternative for discovering minority topics using seed vocabulary. This is
relevant if rare subjects need explicit guidance, not an immediate default.

### Membership verification and naming

Similarity asks whether two texts are related. Membership asks whether a note
actually concerns the subject described by the topic. For Running gear, that
includes equipment, not merely running.

Use embeddings to shortlist topics, then test a non-generative NLI classifier or
a classifier trained on note/topic pairs. A candidate statement could be “This
note concerns equipment used for running.” Unsupported cases may remain unassigned.
[NLI classification research](https://arxiv.org/abs/2312.17543) and
[ModernBERT zero-shot classification](https://huggingface.co/MoritzLaurer/ModernBERT-base-zeroshot-v2.0)
provide starting points. Generic NLI is not a guaranteed membership oracle: test
wording sensitivity, neutral cases, ambiguous language and false confidence.

Generate names from observed noun phrases, entities, distinctive terms, user
names/aliases, and a documented vocabulary of broader subjects. An absent umbrella
term such as Photography must come from a vocabulary or another external knowledge
source if text generation is excluded. A fixed vocabulary alone would restrict
discovery, so retain open-ended entities and observed phrases.

Evaluate each label against all proposed members, including seeds. Measure both
coverage and specificity; a vague label can fit everything without helping the user.
If two groups compete for a name, reconsider redundancy or subdivision rather than
forcing one to take a less accurate unused label.

For personalization, begin with frozen embeddings and a simple classifier or
positive/negative prototypes. Compare [SetFit multilabel learning](https://huggingface.co/docs/setfit/main/en/how_to/multilabel)
once enough reviewed examples exist. Unreviewed absence is not a negative label.
An exclusion from Running gear does not imply exclusion from Running. Automatic
memberships must never become self-confirming training labels.

### Embedding shortlist

| Candidate | Size / representation | Purpose |
| --- | --- | --- |
| Existing all-MiniLM-L6-v2 | 384 dimensions | Reproduce the current baseline |
| [BGE-small-en-v1.5](https://huggingface.co/BAAI/bge-small-en-v1.5) | About 33M parameters; 384 dimensions | Compact English challenger |
| [Granite small English R2](https://huggingface.co/ibm-granite/granite-embedding-small-english-r2) | About 47M; 384 dimensions | Compact ModernBERT candidate |
| [Granite 97M multilingual R2](https://huggingface.co/ibm-granite/granite-embedding-97m-multilingual-r2) | About 97M; 384 dimensions; 2026 release | Multilingual candidate |
| [EmbeddingGemma](https://huggingface.co/google/embeddinggemma-300m) | Roughly 300M; configurable output dimensions | Larger on-device-oriented quality comparison |

EmbeddingGemma produces vectors without generation but has Gemma lineage. If the
constraint excludes LLM-derived models as well as generation, omit it. Model size
and author-reported retrieval scores do not prove notebook quality or phone speed.
Follow each model's tokenizer, pooling and task-prefix requirements. Never reuse
MiniLM cosine thresholds unchanged across different embedding spaces.

## Proposed identity reconciliation fix

Identity reconciliation decides whether a newly inferred topic continues an existing
topic. It must not create new semantic relationships merely to reuse a database ID.

### Current defect

The current overlap gate accepts a candidate if at least half of either membership
set overlaps. A Library pair sharing one mixed rosemary/library note therefore
passes at 1/2. Retained topics can be claimed repeatedly because the claimed check
only limits provisional topics. Separate discoveries are then unioned under the
retained name. Comparing the overlapping note with its own previous text cannot
detect this: an unchanged mixed note trivially passes.

Rejected notes currently also participate in the previous-members set. Exclusions
must not act as positive overlap evidence in the replacement. Fingerprint bindings
also need provenance: an automatic association is weaker than an explicit user merge.

### Proposed algorithm and invariants

1. Extract reconciliation into a pure component. Give it immutable discoveries,
   previous topic snapshots, memberships with origins, exclusions and lineage.
   Return a complete mapping and changes without mutating database state while
   evaluating candidates.
2. Apply explicit user operations first: retained names, manual memberships,
   exclusions, deletion suppression, and deliberate merge redirects. Preserve
   those decisions independently of automatic matching.
3. Build candidate identity matches from genuine member continuity and compatible
   subject evidence. Assess both directions of overlap, distinct supporting notes,
   representative/span agreement and competing identities. Do not use names alone,
   excluded notes, or a single mixed note as sufficient continuity evidence.
   Exact fingerprints indicate lineage, not infallible subject compatibility.
4. Solve ordinary matches globally as a deterministic maximum-weight partial
   one-to-one assignment. Include an unmatched option, minimum evidence and an
   ambiguity margin. Topic retention must not exempt a topic from this rule.
   Thresholds and weights remain to be selected from identity-specific fixtures.
5. Treat splits and merges explicitly. For a split, at most one compatible child
   inherits the old identity; other children get new IDs. Ambiguous continuity
   may leave the retained topic in place without claiming a new discovery.
   Preserve multiple discovery lineages pointing to one target when the user
   explicitly merged them. Many-to-one matching is not allowed merely because
   membership overlap is large. Defer automatic consolidation unless equivalence
   is independently established; never automatically merge retained identities.
6. Apply memberships only after reconciliation. Keep manual assignments and apply
   exclusions; retain origin and supporting evidence for inferred memberships.
   Reconciliation must not broaden the accepted subject scope.
7. Allocate final display names after computing all surviving identities and
   renames. Reserve retained names, then deterministically resolve actual final
   collisions. This avoids suffixes caused by names that are about to disappear.
8. Commit the complete result atomically using the existing generation guard.
   Persist sufficient representative/lineage provenance for subsequent runs.

One-to-one matching alone is insufficient: an unrelated group might claim the old
ID first. Compatibility gating and global assignment are both needed. A retained
display name may be an arbitrary user choice, so it cannot be the sole semantic
definition; use explicit memberships and recorded discovery evidence as well.

Existing contaminated identities need conservative recovery. Recompute automatic
memberships from current evidence; preserve manual choices. Do not blindly delete
all bindings: some preserve explicit merges and deleted-topic suppression. Inspect
what provenance the current schema can recover; add a migration if necessary.
Where a historic binding's intent cannot be recovered, preserve user-owned state
and avoid inventing an automatic merge.

Identity tests must cover the rosemary/library bridge, rejected-note overlap,
different processing orders, ordinary growth, rewritten notes, splits, competing
discoveries, retained names, explicit merges, exclusions, deleted topics, legitimate
two-note continuity, and final-name collisions. With discoveries held fixed,
permuting input order must not change the identity assignment.

## Operation plan: laptop first

### Phase 1: establish reproducible evaluation

- Build a Python desktop harness reading existing JSON fixtures, with model adapters
  and versioned result files. Reuse prior exports where useful; do not require an
  emulator, Android model loader, or device journal access.
- Pin model revisions, tokenizer/pooling settings, package versions, seeds, hardware
  and execution backend. Cache vectors by model/configuration/content hash.
- Verify MiniLM preprocessing and exported baseline parity on fixed texts. Desktop
  float inference and the existing quantized Android export are different conditions;
  record them separately and quantify vector/output differences.
- Preserve 72/172/40-note sets as observed regressions. Create a new untouched set
  before further model selection, with unrelated subjects, ambiguity, short notes,
  overlapping subjects and rare topics. Split by notebook/subject family where
  possible, not random near-duplicate notes. Include target languages if relevant.
- Human annotations should distinguish required memberships, acceptable optional
  overlaps, invalid memberships, and acceptable label scope/alternatives. Establish
  annotation policy before inspecting candidate outputs.

### Phase 2: isolate encoder quality

- Compare MiniLM, BGE-small and Granite small English first. Add multilingual Granite
  when language coverage matters and EmbeddingGemma as a larger optional comparator.
- Hold discovery methodology constant; tune model-specific thresholds only on
  development data. Evaluate difficult neighbor retrieval and topic separation,
  including equipment/activity and ambiguous-word negatives.
- Record loading time, throughput, warm/cold latency, peak memory, cache/storage size
  and CPU/accelerator backend. Laptop timings are not phone estimates.

### Phase 3: compare discovery and labels independently

- Compare current rules, agglomerative clustering and non-generative BERTopic with
  a fixed encoder. Let named-topic notes contribute to generic candidates too.
- Add Contextual Top2Vec as the first overlap-oriented experiment; add FASTopic/S³
  if initial results warrant broader comparison. Limit configuration search and
  report its size to avoid selecting on the test set.
- Fix memberships while comparing naming strategies; fix candidate names while
  testing membership verification. This separates improvements from compensating
  errors. Prototype NLI on shortlisted pairs rather than every note/topic combination.

### Phase 4: reconcile and replay notebook history

- Implement and test the pure reconciliation component independently of encoder
  selection. Replay existing discoveries, then sequential additions, edits and user
  operations. Fresh-discovery quality and persistent-state quality are separate tests.
- Compare snapshots after every operation, measuring unintended ID switches,
  automatic merges, label churn and violations of user decisions.

### Phase 5: decision and later Android integration

- Select a pipeline only after reviewing both errors and resource costs. Define
  product acceptance targets before the final held-out run; do not invent an
  accuracy target from current spot-check results.
- Integrate only the winning components. Verify export/quantization parity and
  benchmark on a physical device later. Keep the existing engine as a comparison
  baseline until the replacement passes membership, identity and persistence checks.

Report membership precision/recall with overlap-aware annotations, false admissions,
missed subjects, label coverage/specificity, abstention coverage, rare-topic quality,
and identity stability separately. Do not optimize only topic count, grouped-note
coverage, selected pairs, or automated coherence. Give per-notebook results and
uncertainty rather than one misleading aggregate score.
