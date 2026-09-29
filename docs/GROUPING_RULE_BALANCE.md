# Finding a better balance in automatic grouping

## Conclusion

Rules 11 have several independent acceptance paths, followed by naming and identity
rules that can contradict the original evidence. Raising a global threshold reduces
some errors but also destroys good groups. Lowering it cannot recover all broad
subjects without admitting unrelated notes.

**Recommended direction:** retain semantic candidate discovery, but require agreement
between subject evidence, member evidence, and label scope before publishing a topic.
Handle saved identity separately. Use uncertainty to avoid misleading topics while
keeping useful note connections. No tested parameter combination is a validated
replacement yet.

This review analyzes all 172 note contents and their recorded groups, traces the
rules, and compares 11 experimental policies at both 72 and 172 notes. Experiments
ran only in the emulator, with the existing MiniLM model. Application rules and the
saved journal were not changed by the experiments.

## 1. What the notes actually contain

The corpus has several distinct kinds of evidence:

| Content pattern | Example | Desired behavior |
| --- | --- | --- |
| Explicit recurring entity | Atlas search/indexing tasks | Strong grouping around that entity; allow multiple projects in one note. |
| Coherent recurring activity | Runs, bench press, piano practice | Group paraphrases and measurements even without identical vocabulary. |
| Related activities under a broader subject | Camera exposure, photo walks, camera-card backup | A broader photography topic can be useful despite modest pairwise similarity. |
| Multiple independent subjects | Repot rosemary and return a library book | Both topics can apply to the note; the topics must remain separate. |
| Related but optional overlap | Ingredient shopping and meal preparation | Defensible either way; do not treat every omitted secondary topic as an error. |
| Same operation, different subject | Cancel a subscription / cancel a meeting | A repeated verb is insufficient to create a subject topic. |
| Same word, different sense | Musical phrase / French phrase; work runs late / running | Require contextual agreement. |
| Isolated broad-life task | Repair a tap, fix a cabinet, plan a birthday outing | Leaving it ungrouped can be appropriate until there is better evidence. |

The last two categories explain why “more notes grouped” is not an adequate target.
The broad-category cases also mean that low pairwise similarity does not prove two
notes are unrelated. Some judgments concern useful granularity, not factual errors.

The corpus has a bias: many project notes repeat a proper name, and several exercise
notes deliberately resemble earlier entries. All 68 memberships across the six named
project/person/destination groups match explicit mentions exactly, but those easy
cases should not dominate assessment of generic-subject quality.

## 2. Rules that generate the present results

### Candidate cores

[TopicDiscovery.kt](../app/src/main/java/app/notetaker/organization/TopicDiscovery.kt)
first separates recurrent capitalized subjects. Notes belonging to those subjects
are excluded from generic core construction, though they may join generic groups
later. Remaining notes are greedily merged using the strongest eligible pair:

- Two single notes need cosine similarity at least 0.42, or 0.40 with shared words.
- Larger merges allow a minimum cross-pair score of 0.20 if the average reaches 0.42.
- Two existing cores may merge with shared words, average 0.30, and one pair at 0.46.
- A shared word is considered useful if it appears in at most 40% of the notebook.
  At 172 notes that admits many broad words; rarity does not establish subject identity.

These are alternative routes. Strong support for one aspect can mask disagreement
on the actual subject. Greedy initial membership matters: core members are admitted
automatically and exempt from the later admission-pruning step.

### Expansion

Each core selects up to five central representatives. An outside note can qualify
through several representative thresholds, a weaker lexical route, or a clause
matching two representatives. The clause path accepts a whole-note best match as low
as 0.30. Two representatives need not supply different kinds of subject evidence.
Near-duplicate examples can therefore reinforce one misleading association.

The frozen representatives prevent unlimited chaining during expansion. They do not
prevent a mixed or overly broad core from expanding around its wrong aspect.

### Names

Names combine a fixed list of purposes with repeated 1–3-word phrases. Scoring blends
whole-note and opening-sentence similarity, then group average and weakest-quarter
support. Important exceptions and interactions remain:

- A note containing the label's stems bypasses the individual 0.20 semantic floor.
- The stronger scope check applies only to **observed multiword phrases**. Preset
  compound labels such as Workout gear may avoid that check when never literally
  repeated. Single-word specific labels such as Basil also avoid it.
- A preset purpose may replace the best candidate when its score is within 0.12.
- Labels already used by another group are excluded before selecting the next name.
- If the final group has no eligible name, the whole group is dropped. It is not
  repaired by selectively removing the unsupported member at that stage.

Those inconsistencies matter more than a small adjustment to any single cutoff.

### Saved identities

[NotebookOrganizer.kt](../app/src/main/java/app/notetaker/organization/NotebookOrganizer.kt)
matches groups to existing topics if at least half of either membership set overlaps.
Retained topics can be claimed repeatedly. The text-continuity check compares a note
to its own previous text, not the new group's meaning to the existing topic's meaning.
An unchanged mixed errand therefore passes while bridging unrelated topics.

This explains why fresh Balcony, Basil, and Library groups end up under the retained
Balcony rosemary identity. Protecting a name must not imply allowing unrelated new
memberships. Name reservation during sequential updates also produces Meetings (2).

## 3. Measured evidence behind representative errors

Cosine scores below are model similarities, not probabilities.

### Shared words can overrule strong contrary evidence

The soup note (#118) has whole-note similarity only **0.112 to Running**, but contains
“work runs late.” Its clause passes the expansion route, and the run stem bypasses
the individual label floor. Removing only that lexical exemption removes this false
Running membership in the experiment.

### The model confuses neighboring concepts

A straightforward bench-press note (#29) scores **0.443 against Workout gear**. An easy
run (#35) scores **0.423 against Running gear**. These are related semantic areas, but
the notes do not describe equipment. A higher universal threshold will not reliably
separate relevance to exercise from evidence of gear.

A language-practice note (#153) scores **0.347 against Note-taking**. Similarity to a
neighboring activity needs qualification, especially when the note says what it is
actually about.

### A preset-label preference can beat a better observed name

For the piano pair, the label score is **0.440 for Piano** and **0.333 for Note-taking**.
The 0.12 preset-purpose allowance is large enough to choose Note-taking when that
label is available. Turning off label reservation alone actually exposes this wrong
name. Thus reservation is not the sole cause of bad labels, and removing it alone is
not a fix.

For the mixed bench/pull-up group, Pull-ups scores **0.483** and Workout gear **0.392**.
The latter wins the preset-purpose preference. Neither adequately describes a mixed
set unless membership is reconsidered; Strength training or a genuine pull-up subset
would be more defensible. The preference and scope checks need to agree.

### Some missing groups fail before naming

Baseline traces show all four dedicated photography notes (#139–142) remaining
singletons. Their pair scores range **0.069–0.374**, below the ordinary 0.42 seed floor.
The photo-walk note (#142) scores **0.414** against the Vienna photo-walk note (#167),
but the Vienna note is excluded from generic core seeding because it has a named
subject. This removes a plausible starting connection.

Home-repair pairs score **0.014–0.186**, and the recipe pairs **0.125–0.243**. Those
broad topics need shared-purpose evidence; simply lowering the ordinary seed floor
that far would be indiscriminate. Conversely, the unrelated cancellation pair scores
**0.434**, already enough to seed a group. These observations correct the simpler
hypothesis that missing labels alone explain all the absent subjects.

## 4. Controlled rule comparisons

A frozen experimental copy of rules 11 checks baseline parity against production.
The [test](../app/src/androidTest/java/app/notetaker/audit/RuleBalanceAuditTest.kt)
changes one policy at a time and several combinations. It never writes notebook
memberships. Full outputs: [RULE_BALANCE_RESULTS.json](RULE_BALANCE_RESULTS.json).

Before running the variants, I recorded [33 desired and 13 undesirable note pairs](../fixtures/grouping-review-pairs.json).
A pair is counted when any group contains both notes. Explicit-name pairs are omitted
so easy project matches do not overwhelm the comparison. These are editorial spot
checks at the current subject granularity, not exhaustive ground truth. Some pairs
could belong together under a broader, properly named subject.

| Policy, 172 notes | Desired pairs retained / 33 | Undesirable pairs present / 13 | Notes grouped |
| --- | ---: | ---: | ---: |
| Current rules | 25 | 10 | 139 |
| Disable clause expansion | 25 | 7 | 139 |
| Disable lexical relaxation | 25 | 5 | 136 |
| Remove literal-label exemption | 25 | 8 | 139 |
| Require clause/whole-note similarity ≥ 0.70 | 25 | 7 | 139 |
| Remove preset-purpose preference | 25 | 8 | 139 |
| Stop reserving labels | 25 | 10 | 139 |
| Raise member floor to 0.30, no literal exemption | 18 | 1 | 121 |
| Combined stricter policy | 23 | 1 | 134 |
| Combined stricter policy, no weak core-merge route | 23 | 1 | 134 |
| Moderate combination | 24 | 6 | 138 |

The stricter combination disables lexical relaxation and label reservation, removes
the literal exemption, uses a 0.26 member floor, and requires clause/whole similarity
of 0.70. The moderate combination retains lexical relaxation, uses a 0.24 floor,
removes the exemption/reservation/purpose preference, and adds the same clause check.
These values are exploratory probes, not calibrated settings.

### Why none of these rows establishes a winner

- The 0.30 floor drops Running and useful notebook memberships. Its remaining
  Running gear group still contains ordinary runs: fewer pair errors can hide bad names.
- The strict combination removes the coherent French group and calls the piano pair
  Note-taking. It produces three Running groups and two Workout gear groups.
- The clause-context check preserves all selected positive pairs, but loses the
  useful run/idea-capture note from Note-taking. The spot checks did not include that
  positive membership. A clause can be a valid second subject while differing from
  the whole-note average.
- Removing preset preference gives new overly narrow names, including Pull-ups for
  mixed strength exercises and Socks for a shoe-fit group.
- Removing label reservation alone does not fix Workout gear. That earlier suspected
  explanation is insufficient; its score and preset preference still select it.
- Disabling the weak core-merge route on top of the strict combination adds no benefit
  here. That result is conditional on the other changes, not proof the route is safe.
- All variants retain the unrelated Cancel pair. A core and a repeated verb can pass
  several numerical checks without identifying a useful subject.
- None recovers the missed photography pairs. Tighter admission cannot create a
  missing candidate core.

On the original 72 notes, baseline passes all nine applicable desired-pair checks.
Disabling clauses loses the post-run lunch/meal-prep connection; disabling lexical
relaxation or tightening scope loses another valid check. The stricter combination
passes eight of nine; the moderate one passes nine. This illustrates why both small
and grown notebooks must be evaluated. Both diagnostic test executions completed
successfully; that is not a claim that any policy passed quality acceptance.

## 5. Recommended rule combination

### A. Separate candidate generation from permission to publish

Generate candidates broadly from semantic neighborhoods, recurring entities, observed
subject phrases, and meaningful clauses. Let named-project notes also contribute to
other subject candidates: a Vienna photography note can support Photography without
losing Vienna.

Candidate generation should be sensitive. Publishing a named topic should require
converging evidence. This makes it possible to recover weakly similar photography
notes without lowering every admission threshold.

### B. Require subject support AND coherent member support

For automatic, descriptive topics, use this conceptual gate:

`subject evidence AND corroborated members AND supported label scope`

A shared generic word or matching action should strengthen an already plausible
subject; it should not independently authorize membership. Count independent
representative evidence, discounting near-duplicates. Apply the same scope contract
to seed members and later arrivals. Strong manual assignments remain authoritative.
Explicit recurring names can use their own calibrated route with context checks.

There can still be more than one evidence route, but each must satisfy the same
subject and scope requirements. Avoid accumulating new bypasses for individual cases.

### C. Validate each topic against the relevant part of a short note

Keep whole-note context and at most a few meaningful fragments. When a fragment
supports a second topic, compare it to that topic's subject and qualified members.
Do not require it to be almost identical to the whole-note embedding: that loses
legitimate second subjects. Whole-note evidence should detect contradiction or
word-sense mismatch, not insist that the candidate is the note's only main subject.

Example: “prepare lunch” can justify Meal preparation alongside Running. “Work runs
late” cannot justify Running merely because it contains the same stem.

### D. Select label and membership together

Use the same coverage criteria for preset, observed, single-word, and multiword
labels. For a qualified label such as Running gear, require evidence of the qualifier
as well as the broad activity. Semantic resemblance to running is insufficient.

Evaluate a small number of candidate `(label, supported members)` alternatives:

1. Keep a specific label when its members support that specificity.
2. Broaden it when the broader subject genuinely covers the group.
3. Remove weak automatic members if that preserves a useful coherent topic.
4. Suppress the topic when neither a reliable name nor a useful cohort remains.

Limit this reconciliation to a small number of passes using fixed evidence to avoid
self-reinforcing relabeling. A failed member must not automatically delete a good group.

Use preset purposes as candidates, not privileged winners. Prefer informative subject
phrases over action verbs or context words. Extract candidates from meaningful local
phrases even when wording appears in only one note, then demand independent semantic
support from other notes before accepting them. This can broaden naming without a
handwritten growing catalog of hobbies and shopping items.

### E. Resolve overlap by meaning, not by forcing distinct names

Multiple topics per note are useful. Near-duplicate topics are not. If two groups have
the same best label, determine whether they are one topic, meaningful subtopics, or
uncertain fragments. Do not assign the second group its next available name solely
to make the labels unique. Merge only when the union still passes subject and scope
checks; a mixed note alone never proves two topics are equivalent.

### F. Fix identity matching independently

Match discovered groups and existing inferred identities as a batch using both
membership continuity and subject compatibility. One retained identity must not
absorb unrelated inferred groups because one shared note contains two subjects.

Preserve manual memberships, retained names, and explicit merge intent. Validate new
automatic memberships against the retained subject. Resolve final display-name
collisions only after all identity assignments and renames are known.

This correctness fix should come before further threshold tuning: otherwise clean
discovery can still turn into misleading saved groups.

### G. Prefer cautious omissions without losing useful discovery

A wrong confident topic costs more trust than a temporarily ungrouped ambiguous note.
But systematically missing a coherent recurring subject also matters. Keep independent
note connections available when topic confidence is insufficient. Do not surface new
confidence categories or require the user to classify notes manually.

Stability should come from supported subject continuity, not protecting every previous
guess. Modest evidence changes should not rename a topic; strong contradictory
content or lost scope should still remove an automatic membership. Any retention
margin must not turn previous guesses into positive training examples.

## 6. How to choose the eventual settings

Evaluate membership, naming, and persistence separately, then combine the results:

1. **Clear false memberships:** unrelated subjects or contradicted qualifiers.
2. **Clear missed memberships/topics:** repeated subject with sufficient evidence.
3. **Label fit:** does the displayed label describe each actual member?
4. **Redundancy:** duplicate or nearly duplicate topics with no useful distinction.
5. **Growth stability:** add unrelated notes, add a same-subject note, edit a note,
   and compare changes in old memberships, names, and IDs.
6. **Optional overlap:** record it separately rather than treating every defensible
   grouping preference as a binary mistake.
7. **Cost:** full organization latency and memory on realistic journal sizes.

Use the existing 72→172 growth case for diagnosis. Before selecting thresholds, add
held-out, independently worded cases spanning the same failure classes and different
subjects. Keep some whole domains and later note batches out of tuning. Test both
fresh discovery and updates to retained/customized topics. Do not count explicit
project matches as evidence that all generic grouping improved.

Prefer a policy with fewer clear false memberships and no large loss of coherent
subjects or label quality. The exact error-cost weights need product feedback; these
small experiments cannot establish a global optimum. We should not call a revision
better simply because it passes known examples or groups more notes.

## Implementation order

1. Repair identity matching/name resolution and add growth/persistence regressions.
2. Unify scope validation, remove lexical exemptions, and reconcile label with members.
3. Add contextual clause evidence and candidate discovery for missed broader subjects.
4. Consolidate redundant groups by subject compatibility.
5. Calibrate the resulting smaller rule set on held-out cases and growth scenarios.

These changes use the existing embedding model plus deterministic processing. They
are general rules, not exceptions for oats, running, French, or any one topic.

## Reproducing the experiments

The experimental implementation lives only under `androidTest`; it is not part of
the app. The diagnostic is opt-in so ordinary test runs do not repeatedly execute
the full comparison:

```sh
./scripts/dev gradle assembleDebugAndroidTest
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w \
  -e class app.notetaker.audit.RuleBalanceAuditTest -e auditRules true \
  app.notetaker.test/app.notetaker.IsolatedTestRunner
```

Optional `-e policies baseline,no_clauses` selects a subset. The test writes
`files/rule-audit-results.json`, `files/rule-audit-trace.txt`, and
`files/rule-audit-vectors.json` in the debug app sandbox, readable through `run-as`.
The committed results combine the two executions; the second execution's files
replace the first on the emulator. Raw first-run trace/embeddings were retained in
`.local/audits/rule-balance/` for this investigation.
