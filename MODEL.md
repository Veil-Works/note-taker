# Offline organization model

V7 bundles the English sentence-transformers/all-MiniLM-L6-v2 model, quantized to
int8 for ARM64, with ONNX Runtime Android 1.23.2. No network inference or runtime
model downloads occur. The model and vocabulary are committed as assets.

Source: https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2

Upstream revision: `1110a243fdf4706b3f48f1d95db1a4f5529b4d41`

Model: https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/1110a243fdf4706b3f48f1d95db1a4f5529b4d41/onnx/model_qint8_arm64.onnx

Vocabulary: https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/1110a243fdf4706b3f48f1d95db1a4f5529b4d41/vocab.txt

License: Apache 2.0 (declared in the upstream model card). License text ships in
`app/src/main/assets/MINILM-LICENSE.txt`. The vocabulary and quantized ONNX export
are unmodified; preprocessing and chunk aggregation are implemented in this app.

- `minilm.onnx`: SHA-256 `4278337fd0ff3c68bfb6291042cad8ab363e1d9fbc43dcb499fe91c871902474`
- `minilm-vocab.txt`: SHA-256 `07eced375cec144d27c900241f3e339478dec958f92fddbc551f295c992038a3`

## Inference

BERT uncased basic tokenization and greedy WordPiece use the bundled vocabulary.
Inputs are split into at most 254 word pieces plus CLS/SEP. Attention masks are
all ones because each chunk runs independently without padding. Token embeddings
are mean-pooled and normalized; chunk embeddings are weighted by their word-piece
counts and normalized again. This covers the whole note, although averaging can
lose distinctions in notes with several unrelated subjects. Outputs contain 384
floats. Only changed note text is re-embedded; caches include the model version.

## Initial phone evaluation

An eight-note synthetic benchmark covered weight training, software debugging,
travel, and running. Loading the model plus embedding all eight took 286 ms on
the connected phone. Related pairs scored 0.439–0.685; unrelated pairs in this
small fixture scored at most 0.230. These are similarity scores, not calibrated
probabilities. This is a smoke evaluation, not a general accuracy guarantee.

General related-note matches require similarity of at least 0.42; recurring subjects
can support weaker matches (at least 0.20). Topic discovery uses coherent cores and
representative-note checks in `TopicDiscovery.kt`. Existing-topic attachment uses
manual examples (two at 0.55 or one at 0.82) and requires a margin of 0.08 over
negative examples. Exact unambiguous user-defined names/aliases use a separate rule.
Generated provisional names are excluded from that exact-match route.

In v8, discovery produces memberships in the same Topics system. Each pass derives
groups from current note text; old memberships only help identify an existing topic,
not train the matcher. Stored content fingerprints preserve discovery lineage through
merges. Generated names and automatic assignments are not positive training examples.
Removal exclusions and strong negative examples prevent reassignment to the topic.
See [unified topic rules](docs/TOPICS.md) for details.

Thresholds should be revisited with a larger representative evaluation set. English
shorthand, ambiguity, and mixed subjects remain limitations; other languages have
not been validated.

## Topic labels

Labels use recurring names, repeated phrases, and a small English purpose vocabulary
ranked against the core notes by the bundled embedding model. Purpose candidates
help replace generic nouns with names such as Strength training or Meal planning;
they do not assign notes to predefined categories. Custom names are preserved.
The experimental generative model and native runtime have
been removed; the bundled MiniLM embedding model still supplies connections.
Upgrading removes the experimental model files and cached generated labels,
then regenerates automatic topic names without changing note contents.

## Short-note discovery (rules version 9)

Recurring capitalized subjects can occur anywhere in a note. Semantic checks reject
unrelated uses of the same name, and multiple project mentions can yield multiple
memberships. Generic cores are constructed in deterministic strongest-match order,
with checks on similarity and repeated subject words. Each core then selects up to
five central representatives. Additional notes need support from multiple peers;
admitted notes never become new representatives during that pass.

Near-identical candidates are consolidated, as are overlapping candidates with the
same purpose label. Untouched provisional topic identities may be consolidated
when their old memberships substantially survive; redirects keep filters working.
User-retained identities and names remain protected. Inferred membership stores its
measured similarity and a representative-note explanation, not a probability.

The existing 72-note development notebook is also exercised by a real-model
instrumentation regression. It checks previously missed short notes and explicit
multi-project memberships. This is a focused regression, not an accuracy estimate.
Weak paraphrases, ambiguous names, overlapping purposes, and large-notebook
performance still need broader evaluation.

## Label coverage (rules version 10)

Label ranking uses the existing MiniLM model for both the whole note and its first
meaningful sentence. An opening with fewer than three subject words falls back to
the whole note. Their scores are combined equally, then ranked using 65% group
average and 35% weakest-quarter agreement. This makes incidental mentions and
poorly covered members less likely to determine the label. These are heuristics,
not probabilities or a guarantee that the first sentence is the main subject.

Repeated phrases count once per note, exact lexical support uses word boundaries,
and its contribution is capped at 0.015. Generic descriptive labels are recomputed
for the final membership after expansion/consolidation. Explicit named subjects
and user-retained names remain protected. No additional model is introduced.

See the [full label audit](docs/TOPIC_LABEL_AUDIT.md) for all 14 phone groups and
remaining distinctions between bad labels and questionable memberships.

## Scope, clauses, and uncertainty (rules version 11)

Discovery now embeds up to four meaningful sentences/clauses alongside each whole
note. A clause must match two independent core representatives; matches from
different clauses cannot be pooled. Whole-note support remains required, and
low-relevance admissions are pruned against the core label. Short shopping-list
items are kept together. This supports overlapping subjects in short notes without
allowing new members to propagate a group through a chain.

Candidate labels include observed one-to-three-word phrases normalized across
plural and -ing forms. Display names retain observed words. Final labels need
average and individual-member support; narrow observed phrases require strong
evidence for members lacking that phrase. A simpler observed subject can replace
a weak qualifier, and overlapping groups receive distinct supported labels.

Groups of at least six notes may split into two cohorts of at least three. Both
cohorts must be internally coherent, separated by a similarity gap, and have
different supported labels. Explicit named subjects are preserved. When no label
is sufficiently supported, discovery omits the inferred topic; independent
semantic connections remain available. All thresholds are heuristics.

No additional model or topic-specific exception is introduced. See the
[emulator evaluation](docs/GROUPING_V11_REVIEW.md) for outcomes and limitations.
