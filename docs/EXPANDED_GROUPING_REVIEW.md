# Expanded journal: 172-note grouping review

## Outcome

Added exactly 100 short, untagged notes to the dedicated emulator journal, expanding
72 existing notes to 172. Existing note rows were preserved exactly, including IDs,
content, and timestamps. The phone was not accessed. Organization rules remain 11;
no grouping fixes were made during this evaluation.

The larger sample exposes significant regressions. Explicit names work reliably,
but generic groups suffer from overly broad admissions, misleading labels, and
saved-topic identity merges. The favorable 72-note result did not generalize well.

## Method and counts

- Original corpus: [72 notes](../fixtures/sample-notes.json).
- Additions: [100 notes](../fixtures/expanded-notes.json), mostly one or two sentences.
  These extend work projects, exercise, food, gardening, travel, and notebook ideas,
  and add cycling, repairs, photography, reading, music, language study, and everyday
  observations. Several notes intentionally have two natural subjects.
- No manual topics or leading hashtags were supplied for the additions.
- Read every saved group and all ungrouped notes after organization completed.
- Ran a separate isolated emulator audit with the bundled model and all 172 notes,
  without existing topic history, to distinguish discovery from persistence.
- Note numbers below are 1–72 in the original fixture and 73–172 in the additions.
- [Machine-readable memberships](EXPANDED_GROUPING_RESULTS.json) record both runs.

| Measure | Before | After |
| --- | ---: | ---: |
| Notes | 72 | 172 |
| Saved topics | 14 | 25 |
| Automatic memberships, including overlaps | 75 | 194 |
| Notes with at least one topic | 63 | 139 |
| Notes with no topic | 9 | 33 |

71/100 added notes received a topic; 29 did not. Of the original notes, 68/72 are
now grouped. Coverage is **not accuracy**: several memberships are clearly wrong.
No complete human ground-truth taxonomy was assigned in advance, so this review
uses concrete editorial judgments rather than an overall precision/recall claim.

Fresh discovery produced 27 groups and 197 memberships covering the same 139 notes.
Its grouping stage took 2.872 seconds in one emulator audit, excluding initial
whole-note embeddings and model loading. This is not an end-to-end latency benchmark.

## Every saved topic

| Topic | Notes | Assessment |
| --- | ---: | --- |
| Atlas | 19 | Strong: exactly the 19 explicit mentions, with relevant search/indexing work. |
| Orion | 19 | Strong: exactly the 19 explicit mentions, including mixed-project tasks. |
| Beacon | 14 | Strong: exactly the 14 explicit mentions; receipt-export meeting is included. |
| Vienna | 7 | Strong: all seven mentions, including packing and photography overlap. |
| Maya | 5 | Accurate person grouping, though overlapping with projects. All five mentions. |
| Jonas | 4 | Accurate person grouping. All four mentions. |
| Running | 18 | Mostly useful, but admits soup preparation (#118) and a squat-only note (#112). |
| Running gear | 17 | Serious scope failure: only five clearly concern running equipment (#39, #40, #46, #109, #110). Other members include regular runs, lunch, gym packing, and Vienna packing. |
| Strength training | 8 | Members fit; misses the existing squat/long-run scheduling note (#34), previously included. |
| Workout gear | 4 | Misleading: all four are bench/pull-up performance logs (#29, #33, #111, #113), not equipment notes. |
| Groceries | 8 | Useful coverage improvement: now includes meal ingredients and household shopping. “Shopping” would cover household supplies more precisely. |
| Meal preparation | 10 | Good central subject, including both post-run lunch reminders. Generic grocery lists (#43, #115) and cupboard inventory (#116) are weak additions without explicit preparation intent. |
| Balcony rosemary | 8 | Four rosemary notes, two basil notes, a pot-tray note, and an unrelated library return. Existing retained identity absorbs separate subjects. |
| Note-taking | 19 | Mostly useful; gains reading/notebook and run/idea capture overlaps. Language conversation practice (#153) is a false positive; generic button wording (#59) remains weak. |
| Meeting planning | 4 | Coherent decisions/owners/review group, but “Meeting decisions” would describe much of it better. Overlaps other meeting groups. |
| Meetings (2) | 3 | Mixes meeting-note capture with two walking/break reminders. Broadly related, weak as a separate group; numeric suffix adds clutter. |
| Project planning | 3 | Workshop agenda, planning decisions, and meeting notes. Reasonable association but largely another meeting group. |
| Bike | 4 | Members concern cycling/commuting, but misses explicit brake repair (#126), puncture kit (#130), and route planning (#129). |
| Phrases | 6 | Three language-learning notes mixed with piano phrasing and two note/idea-capture observations. Word-sense confusion. |
| Chapter | 3 | Reading and reading-note ideas are related; “Reading notes” would be clearer than a repeated fragment. Misses the finished-books list (#146). |
| Coffee | 3 | Useful grouping of bean inventory, grinder cleaning, and brewing experiments. |
| Piano | 2 | Coherent practice pair; useful modest group. |
| Chat | 2 | Strong pair about protecting focused work by closing chat. Name emphasizes the distraction rather than the purpose. |
| Cancel | 2 | Invalid topic: cancelling a cloud trial and cancelling a weekly meeting share an action verb, not a subject. |
| Path | 2 | Cycling and running route changes have a defensible route-planning connection; the name is vague and cycling coverage is fragmented. |

The six explicit-name groups have no missing or extra notes relative to literal
mentions in this corpus. That is a narrow, checkable result, not evidence that
arbitrary names or ambiguous entities will always work.

## What the saved explanations reveal

1. **A polysemous verb overrides the real subject.** Soup preparation (#118) joins
   Running with the explanation “freeze two portions for evenings when work runs
   late” matches multiple notes. “Runs late” is not running as exercise.
2. **Related activities are mistaken for equipment.** Running gear admits an easy
   run (#35) through similarity to the socks-and-bottle shopping note. The group
   describes a broad exercise neighborhood, not a shared equipment subject.
3. **Shared vocabulary bridges unrelated senses.** Piano practice (#147) joins
   Phrases through the French-practice note. “Phrase” means different things here.
4. **Mixed errands bridge entire saved topics.** The library return (#144) is
   explained by the old rosemary/library errand (#66). It makes sense in a Library
   group, but not under Balcony rosemary.

## Discovery versus saved-topic updates

The isolated discovery run reproduces Running gear, Workout gear, Cancel, Phrases,
and the questionable Running additions. Those defects are in discovery/naming,
not stale stored memberships.

Fresh discovery creates these separate plant/library groups:

| Fresh name | Note numbers | Saved destination |
| --- | --- | --- |
| Balcony | 48, 64, 66, 120, 121, 122 | Balcony rosemary |
| Basil | 65, 120, 121 | Balcony rosemary |
| Library | 66, 144 | Balcony rosemary |

The original Balcony rosemary topic is marked retained (`provisional = false`).
Preserving its name is intentional. However, matching new groups to it by membership
overlap allows several different discovered topics to reuse that identity. The
Library pair shares one mixed errand, enough for the 50% candidate overlap rule.
The resulting union includes an unrelated library return. Basil is also imperfect
before saving: it includes the rosemary watering note (#120).

Fresh “Meetings” becomes saved “Meetings (2)”. Code inspection indicates name
reservation includes the old Meetings identity while groups are processed, even
though that old identity is subsequently renamed. This creates a transient name
collision that survives in the displayed result.

## Missed subjects and overlaps

- **Photography:** all four dedicated notes (#139–142) remain ungrouped. The Vienna
  photo walk (#167) belongs only to Vienna. This is a clear missed recurring subject.
- **Home maintenance:** all four repairs/furnishing notes (#131–134) remain ungrouped.
  They share a broad purpose, although individual repairs are semantically diverse.
- **Recipes:** all three cooking notes (#154–156) remain ungrouped despite an active
  Meal preparation group. A separate cooking/recipe group would also be reasonable.
- **Cycling:** brake work and the puncture kit remain ungrouped despite Bike existing.
- **Design:** explicit button wording, accessibility errors, and form-retry behavior
  (#101, #163, #164) remain ungrouped while weaker button wording (#59) stays in
  Note-taking.
- **Reading:** the finished-books list (#146) remains ungrouped; the library return
  receives the wrong saved topic instead of its fresh Library identity.
- **Strength/running overlap:** #34 loses Strength training while retaining Running.
- Some isolated observations, such as Dad's birthday outing (#172), reasonably
  remain ungrouped. More coverage should not mean forcing every note into a topic.

All 33 ungrouped note IDs are in the machine-readable results. The remaining
unassigned notes concern cables, a cafe, work shutdown, expenses/receipts, guitar
and playlists, sleep/free time, account security, and backup restoration.

## Recommended next changes, in priority order

1. **Fix topic identity matching before more scoring tweaks.** Match each inferred
   group to a compatible existing identity; do not let one retained group absorb
   several different subjects through a mixed-note bridge. Keep existing manual
   choices, while allowing distinct new inferred topics. Resolve final names after
   identity matching to avoid temporary numeric suffixes.
2. **Require evidence for the qualifier in every membership.** Running similarity
   alone must not justify Running gear; strength similarity must not justify Workout
   gear. Apply checks to core members too, not only notes added during expansion.
3. **Treat label conflicts as structural evidence.** The current distinct-label
   filter can push an overlapping strength group toward another eligible label.
   Merge, retain a genuinely distinct subtype, or suppress a redundant group instead
   of selecting a weaker available name just to make it unique.
4. **Guard clause matching with subject consistency.** Require the chosen clause,
   whole-note context, and candidate subject to agree. Do not pool action verbs or
   ambiguous terms as evidence of subject identity. Validate general examples such
   as software “runs,” musical phrases, and unrelated cancellations; avoid word-
   specific exceptions.
5. **Separate coherent membership from naming failure.** Inspect ungrouped coherent
   neighborhoods such as photography. Broaden candidate generation beyond the fixed
   purpose vocabulary and exactly repeated words, without relaxing admission just
   to increase coverage. Current results alone do not prove which gate rejected
   each missing subject.
6. **Keep this corpus as a regression alongside the original.** Check false positives,
   missing overlaps, label relevance, and saved identity behavior—not just group count.
   Test both clean discovery and growth of an already organized notebook.

## Reproduction and validation

```sh
./scripts/dev seed --fixture fixtures/expanded-notes.json
```

Seeding uses stable content-derived IDs, so rerunning the same fixture does not add
duplicates. It refuses a device other than the dedicated emulator AVD and preserves
existing notes. The default 72-note fixture remains unchanged.

Built the Android test APK and ran
`ExpandedNotebookAuditTest.report_expanded_notebook_discovery_without_existing_topic_history`
on `emulator-5554`: one diagnostic test passed. This checks audit completion, not
quality acceptance. The app's saved organization state completed without an error.
All 72 pre-existing note rows compare equal to the pre-insertion snapshot.

## Rule-combination investigation

The [follow-up analysis](GROUPING_RULE_BALANCE.md) traces concrete failures through
the code and compares 11 policies on both journal sizes. It qualifies earlier
hypotheses: missing photography cores fail before naming, and removing label
reservation alone does not fix equipment labels. No tested combination is ready
to replace the current rules.
