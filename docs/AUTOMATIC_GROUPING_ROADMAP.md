# Automatic grouping improvements

Automatic grouping is the app’s main feature. Improvements should prioritize useful,
coherent topics, reliable membership, and understandable results while preserving
offline operation and the user’s decisions.

Notes are designed to be short: a couple of sentences at most, often fragments.
Prioritize understanding sparse wording, shorthand, and recurring subjects. Long
documents and paragraph-level analysis are outside the main use case. This is a
design assumption, not a proposed hard limit on note length.

This roadmap includes both remaining proposals and the first implementation pass.
Rules version 9 implements coherent cores with fixed representatives, recurring
names throughout short notes, consolidation of redundant provisional groups, and
semantic ranking of purpose labels with supporting-note explanations. See
[model behavior](../MODEL.md#short-note-discovery-rules-version-9) for the implemented
rules and limitations. Stability margins, topic splitting, stronger shorthand
handling, feedback refinements, and alternative models remain future work.

## 1. Group around a coherent core

The previous greedy grouping algorithm considered notes sequentially, allowing early
choices to influence the resulting groups. The first pass now constructs cores in
deterministic strongest-match order and evaluates membership against representatives.

- Identify small sets of strongly related notes first.
- Evaluate additional notes against these representative cores.
- Allow a note to belong to several topics without merging those topics together.
- Leave uncertain notes ungrouped rather than forcing an assignment.
- Compare local-community clustering with the current approach.

For example, a note about buying running shoes could belong to both Running and
Shopping without pulling all shopping notes into Running.

Reference: [Sentence Transformers clustering documentation](https://sbert.net/examples/sentence_transformer/applications/clustering/README.html).

## 2. Understand short notes with little explicit context

Use the whole short note as the primary unit of meaning. The main challenge is
recognizing its subject from few words, rather than splitting a long document.

- Handle fragments, abbreviations, typos, and compact notation such as
  “run 7km thursday” or “bench 80kg 3x5.”
- Combine meaning similarity with specific evidence such as project names,
  distinctive phrases, and meaningful units or terms. Shared dates or numbers
  alone should not establish a topic.
- Extract recurring project names and phrases wherever they appear, extending the
  current subject detector beyond the first word.
- Use coherent neighboring notes and explicit user feedback as supporting context,
  while keeping ambiguous notes ungrouped when evidence is insufficient.
- Preserve overlapping membership when even a short note has two subjects, such
  as buying running shoes. Consider clause-level analysis only if whole-note
  matching repeatedly misses these cases.

For example, “Orion release slipped. Retry bug still open.” should have a chance
to connect with both Orion planning and relevant debugging notes. “Friday again”
should not acquire a confident topic merely because nearby notes mention Friday.

Paragraph embeddings and document segmentation are low priority for this app.

## 3. Keep groups stable without freezing them

- Require stronger evidence to add a note than to retain an existing, sensible
  assignment.
- Avoid topic disappearance, renaming, or reassignment after minor wording changes.
- Track topic identity through representative notes and meaning.
- Preserve explicit merges and other user decisions across reorganization.
- Allow reassignment when a note’s meaning changes substantially.

Stability should reduce unnecessary changes while still allowing topics to evolve.

## 4. Improve topic names and explanations

The original naming rules included special cases for Dogs and Running, followed by
repeated-phrase selection. Purpose-label ranking now complements repeated phrases;
the old rules remain a fallback.

- Replace subject-specific naming rules with a general approach.
- Prefer phrases that distinguish a group from the rest of the notebook.
- Draw naming evidence from the group as a whole; a useful subject may be clear
  across several short notes without appearing as a repeated phrase in each one.
- Favor clear, specific names over generic labels such as “Related notes.”
- Provide short explanations grounded in the evidence used for grouping.

Example explanation: “Grouped with your other Orion release notes.”

Names and explanations should help users recognize useful groups and correct
mistakes. They should not claim certainty unsupported by the grouping evidence.

## 5. Make corrections more precise

Treat different user actions as different kinds of feedback:

| Action | Intended effect |
| --- | --- |
| Remove this note | Remember the specific note–topic exclusion. |
| These belong together | Preserve an explicit merge or confirmed memberships. |
| This topic mixes different things | Allow the user to split the topic. |

Removing a note currently also supplies a negative example that can affect similar
notes. Make that generalization more conservative so one correction does not
accidentally suppress an entire subject.

Manual decisions remain authoritative. Automatic memberships must never become
user-confirmed training examples on their own.

## 6. Evaluate alternative offline embedding models

- Compare candidate models with the bundled MiniLM model before replacing it.
- Prioritize performance on short fragments, shorthand, and one- or two-sentence
  notes over long-document understanding.
- Consider grouping quality, physical-device latency, memory use, and app size.
- Evaluate clustering suitability directly; retrieval leaderboard scores alone
  do not establish how useful a model will be for grouping notes.
- Preserve on-device inference and offline availability.

A stronger model is a possible improvement, not an assumed solution to weaknesses
in grouping, naming, or feedback handling.

Reference: [MTEB research](https://arxiv.org/abs/2210.07316).

## Suggested implementation order

1. Improve short-note matching, group formation, and membership stability.
2. Replace the naming heuristics and add evidence-based explanations.
3. Strengthen shorthand handling and recurring-subject detection across notes.
4. Refine correction handling and introduce topic splitting.
5. Assess whether an alternative offline model provides worthwhile gains.

## Rules 11 implementation update

Implemented clause-supported overlaps, inflection-aware observed labels, label
scope gates, conservative automatic subgroup splitting, and abstention when no
name is supported. See [evaluation and remaining gaps](GROUPING_V11_REVIEW.md).
Interactive splitting and broader correction handling remain future work.

## Optional hybrid engine

Keep the existing MiniLM/rules path as the default download. A separately downloaded
local generative model may enable an optional enhanced mode after quality validation.
The [three-way comparison](OPTIONAL_HYBRID_COMPARISON.md) is prepared and its
[first run](HYBRID_COMPARISON_FIRST_PASS.md) completed. Qwen3-0.6B is not yet reliable
enough to apply its grouping proposals to the journal.
