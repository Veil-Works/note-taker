# Laptop topic review findings

Reviewed on 2026-09-30. All 43 groups were completed: 32 development and 11 transfer. These are fixed Granite-small + complete-link groups at threshold 0.775; the review compares labels, not different grouping pipelines. The reviewer also assessed coherence and flagged incorrect memberships.

The hybrid labeler has a small advantage, but neither labeling approach is satisfactory overall. Group discovery is the larger immediate bottleneck. Keep the hybrid as an experimental challenger; this review does not justify integrating the current pipeline into Android or declaring it the best grouping approach.

## Label comparison

A is the current c-TF-IDF unigram/bigram labeler. D is the hybrid of source phrases and noun phrases with support filtering, semantic reranking and MMR alternatives. Slots were randomized independently per group; the saved blind key was used to recover the methods.

| Result | Current A | Hybrid D |
| --- | ---: | ---: |
| Available proposals | 43/43 | 38/43 |
| Preferred actual label | 3 | 7 |
| Clarity, same 38 groups, scale 1–3 | 1.84 | 1.97 |
| Fit, same 38 groups, scale 1–3 | 1.87 | 2.00 |

There were 14 ties and 18 “neither” decisions. The raw summary reports eight hybrid preferences, but one is **an empty hybrid proposal preferred over “day”** (development-014). That is evidence for abstaining, not a successful hybrid name. Among the 38 groups with both proposals available: hybrid wins 7, current wins 3, ties 14, neither 14. Among the 12 groups with different top labels and both proposals available: hybrid wins 7, current wins 3, neither 2. All 14 ties have identical top labels, so they provide no evidence that the changed ranking helped.

The full-cohort raw means are not comparable: A has 43 ratings and D has 38. The table uses the same 38 groups and leaves missing ratings missing. The paired fit advantage is only 0.13 points. All five missing hybrid proposals belong to groups marked mixed; this supports investigating abstention, but missing proposals alone are not a calibrated quality gate.

| Corpus | Current preferred | Hybrid actual label preferred | Empty hybrid preferred | Tie | Neither | Good / mixed / unsure |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| Development | 3 | 6 | 1 | 9 | 13 | 15 / 16 / 1 |
| Transfer | 0 | 1 | 0 | 5 | 5 | 8 / 3 / 0 |

On groups marked good, preferences were hybrid 4, current 2, tie 14, neither 3. The advantage is therefore modest even when membership is broadly acceptable. Transfer evidence is only one hybrid win, for “pottery” over “glaze”; it does not establish generalization.

## What the review revealed

**Grouping:** 23 groups were marked good, 19 mixed and one unsure. There are 25 flagged memberships across 13 groups, involving 25 distinct notes when IDs are scoped to their corpus. Four groups marked good still contain a flagged note, leaving 19 groups marked good with no flags. This is a useful review count, not an exhaustive membership accuracy score. Several mixed groups have no individual flags because the reviewer rejected the whole topic.

The recurring failure is grouping by incidental context, action words or shared objects rather than a useful common subject: train, chat, cancel, path, continue, error, compare and similar groups. Some groups need a broader shared label, while others should split or remain ungrouped. The comments explicitly support both possibilities; changing only the name cannot resolve them.

**Candidate generation:** three groups marked good rejected both labels: street → Photography/photo, fabric → Sewing, and loaf → Baking/Bread. Source-only phrases with a hard two-note exact-support requirement miss category names expressed in different words. Photography/photo and sewing/sew illustrate variation in wording; baking occurs in only one note, despite all three being about bread preparation. Expanding the semantic concept pool is a general candidate-generation problem, not a reason to hard-code these topics.

**Candidate ranking:** useful alternatives already exist in the candidate lists for the three current-label wins. Bike is present but D ranks bike light first; French is present but D ranks phrases first; running socks is present but D ranks running shoes first. Candidate expansion alone cannot fix these regressions. Reranking needs to balance specificity with member coverage and topic identity. The socks group is also marked unsure and has a flagged shoe note, so its label and membership problems are linked.

**Hybrid gains:** atlas search, meal prep, short notes, pottery and pull-ups improved over the current alternatives. Planning and balcony rosemary were preferred within mixed groups, so they do not establish satisfactory topics. The balcony comment explicitly asks for broader “Balcony pots” if basil and rosemary remain merged, despite giving the rosemary label high scores. Similarly, the piano group has high label scores but a flagged guitar note and a suggestion that “Music instruments” could cover it. Preserve the comments alongside scores rather than interpreting high scores as blanket membership approval.

## Proposed next experiment

1. Freeze this review as an evaluation set. Retain corpus-scoped exclusions, whole-topic rejection, ratings and comments separately. Do not convert every unflagged note into an approved membership or every suggested name into an exact required string. The transfer corpus has now been reviewed and is no longer an untouched evaluation set.
2. Evaluate a general topic acceptance/abstention stage on the laptop: compare the existing complete-link baseline with generic evidence from separation from neighboring notes, member semantic consistency, and cross-note subject support. Permit ungrouped notes. Measure rejected incoherent groups and retained coherent groups together; a method that suppresses everything is not an improvement. Test splitting only as a general clustering approach, without note-specific exclusions.
3. Run two controlled labeling comparisons. For candidates, compare the current hybrid pool against lemmatized/semantic support aggregation and a commercially usable concept/category pool that can produce a shared subject absent as an exact repeated phrase. Keep grouping and ranking fixed for that comparison. Separately, hold the candidate pool fixed and compare the current semantic reranker against a coverage-aware alternative with an explicit specificity tradeoff.
4. Use new English notebooks for confirmation, including unrelated notes and multiple nearby subtopics. Compare the selected Granite + complete-link pipeline with the existing BGE alternatives using the same review protocol. Report topic quality, coverage, naming quality and stability separately; the current review cannot rerank those unreviewed alternatives.

Keep all experiments on the laptop for now. Verify the license of any newly introduced model, concept resource or parser before use in a sellable product. Do not change Android grouping or identity reconciliation based solely on this review. Identity reconciliation preserves topic identity across updates; it does not repair an incoherent discovered topic.

## Reproducibility and limits

The real saved review is .local/embeddings/reviews/d85eb59f1bf200c3/reviews.json, with its adjacent blind-key.json. The source comparison is docs/LABEL_COMPARISON_RESULTS.json. QA reviews were excluded. The checked-in LAPTOP_TOPIC_REVIEW_RESULTS.json stores normalized method names, all reviewed notes, comments, flags and source SHA-256 hashes. Study ID: d85eb59f1bf200c32d7e06627706103ea28af7019cc8c91ee2881c0ff5f4dd8a.

This is one reviewer on 43 already-observed groups. It establishes concrete failures and a modest preference in this sample, not statistically secure superiority or fresh-notebook performance. Clarity and fit are ordinal ratings; their means are descriptive.

## Complete review ledger

A/D ratings below are clarity/fit. A dash denotes a missing proposal. Suggested labels and comments are retained verbatim. IDs are local to their corpus.

| Group | Current A | Hybrid D | Preference | Coherence | A / D ratings | Flagged note IDs |
| --- | --- | --- | --- | --- | --- | --- |
| development-001 | orion | orion | Tie | good | 3/3 / 3/3 | — |
| development-002 | search | atlas search | Hybrid | good | 1/1 / 3/3 | — |
| development-003 | beacon | beacon | Tie | good | 3/3 / 3/3 | — |
| development-004 | train | train | Neither | mixed | 1/1 / 1/1 | 24, 138, 149 |
| development-005 | chat | chat | Neither | mixed | 1/1 / 1/1 | 25, 28, 72, 94, 159, 160, 161 |
| development-006 | decision | planning | Hybrid | mixed | 1/1 / 1/2 | 92 |
| development-007 | kg | 80 kg | Neither | mixed | 1/1 / 1/1 | 34 |
| development-008 | gym | gym | Neither | mixed | 1/2 / 1/2 | 110 |
| development-009 | rest | pull-ups | Hybrid | mixed | 1/1 / 3/2 | 38 |
| development-010 | run | run | Tie | good | 3/3 / 3/3 | 63 |
| development-011 | socks | running shoes | Current | unsure | 2/2 / 1/1 | 40 |
| development-012 | bowls | meal prep | Hybrid | good | 1/1 / 3/3 | — |
| development-013 | shopping | shopping | Tie | good | 3/3 / 3/3 | — |
| development-014 | day | — | Empty hybrid | mixed | 1/1 / — | 47, 70, 132, 133 |
| development-015 | pot | balcony rosemary | Hybrid | mixed | 1/1 / 3/3 | 65, 121 |
| development-016 | coffee | coffee | Tie | good | 3/3 / 3/3 | — |
| development-017 | notebook | notebook | Tie | good | 2/2 / 2/2 | — |
| development-018 | concrete | short notes | Hybrid | good | 1/1 / 2/2 | — |
| development-019 | continue | continue | Neither | mixed | 1/1 / 1/1 | — |
| development-020 | library | library | Tie | good | 3/3 / 3/3 | 162 |
| development-021 | vienna | vienna | Tie | good | 3/3 / 3/3 | — |
| development-022 | bike | bike light | Current | good | 3/3 / 1/1 | — |
| development-023 | cancel | cancel | Neither | mixed | 1/1 / 1/1 | — |
| development-024 | path | path | Neither | mixed | 1/1 / 1/1 | — |
| development-025 | drip | — | Neither | mixed | 1/1 / — | — |
| development-026 | street | street | Neither | good | 1/1 / 1/1 | — |
| development-027 | card | — | Neither | mixed | 1/1 / — | — |
| development-028 | argument | argument | Neither | mixed | 1/1 / 1/1 | — |
| development-029 | piano | piano | Tie | good | 3/3 / 3/3 | 150 |
| development-030 | french | phrases | Current | good | 3/3 / 1/1 | — |
| development-031 | pasta | — | Neither | mixed | 1/1 / — | — |
| development-032 | error | error | Neither | mixed | 1/1 / 1/1 | — |
| transfer-001 | telescope | telescope | Tie | good | 2/2 / 2/2 | — |
| transfer-002 | fabric | fabric | Neither | good | 1/1 / 1/1 | — |
| transfer-003 | aquarium | aquarium | Tie | good | 3/3 / 3/3 | — |
| transfer-004 | glaze | pottery | Hybrid | good | 2/2 / 3/3 | — |
| transfer-005 | configuration | failed | Neither | mixed | 1/1 / 1/1 | — |
| transfer-006 | cancel | — | Neither | mixed | 1/1 / — | — |
| transfer-007 | loaf | loaf | Neither | good | 1/1 / 1/1 | — |
| transfer-008 | japanese | japanese | Tie | good | 3/3 / 3/3 | — |
| transfer-009 | compare | compare | Neither | mixed | 1/1 / 1/1 | — |
| transfer-010 | bedtime | bedtime | Tie | good | 3/3 / 3/3 | 39 |
| transfer-011 | chess | chess | Tie | good | 3/3 / 3/3 | — |

## Review comments, suggestions and excluded notes

### development-002

Suggested label: Atlas


### development-004

Comment: all three notes refer to different things, the first two are different todos, the last is a quick little idea/reminder

- Excluded note 24: Team todo: reserve a quiet room for the roadmap workshop and send the agenda before Tuesday.
- Excluded note 138: Expense todo: submit the train receipt from the client visit and attach the meeting date.
- Excluded note 149: Music idea: make a short playlist of calm instrumental tracks for the train, downloaded before leaving home.

### development-005

Comment: the notes are a mix of everything, some dont even mention chats

- Excluded note 25: Our standup runs long because we solve bugs during status updates. Move debugging into a separate follow-up.
- Excluded note 28: I get more coding done before opening chat. Try protecting the first ninety minutes of the workday.
- Excluded note 72: Try a short walk after lunch instead of another coffee. Afternoon meetings feel harder when I stay at the desk all day.
- Excluded note 94: Protect a morning coding block by closing chat until the first task is finished. Record whether interruptions actually decrease.
- Excluded note 159: Sleep observation: I stayed up later on the nights I started a new episode after ten. Choose the stopping time before pressing play.
- Excluded note 160: Charge the phone outside the bedroom tonight. Reaching for the alarm has turned into twenty minutes of scrolling.
- Excluded note 161: A short walk between calls helped me return to the next meeting with more attention. Put a gap between the two afternoon reviews.

### development-006

Comment: fifteen-minute agenda note doesnt necessarily refer to the same planning meetings as the rest of the notes

Suggested label: Planning meeting

- Excluded note 92: Try a fifteen-minute agenda for the design review: context, decision, owner. Put detailed status updates in the document beforehand.

### development-007

Comment: the group has a mix of exercises, splitting them into separate topics each for its own exercise would have been better

Suggested label: Strength session

- Excluded note 34: A heavy squat session the night before a long run makes my legs feel flat. Move lower-body training earlier in the week.

### development-008

Suggested label: Gym gear

- Excluded note 110: Running gear todo: compare small waist belts that hold a phone and keys without bouncing.

### development-009

- Excluded note 38: Intervals tomorrow: warm up, run six short repeats, then cool down. Keep the first repeat controlled.

### development-010

- Excluded note 63: The best ideas on my runs usually disappear before I get home. Capture one sentence as soon as I stop.

### development-011

Suggested label: Running socks

- Excluded note 40: Look for running shoes with a wider toe box. Try them on after work when my feet are less fresh.

### development-014

Comment: This is just a mix of notes, no need to form a group out of it

- Excluded note 47: Need a USB-C cable for the work bag. The spare one at home only charges and cannot connect the monitor.
- Excluded note 70: The cafe near the station is quiet in the morning and has enough space to open a laptop.
- Excluded note 132: The kitchen cabinet door catches the drawer below it. Mark the hinge position before trying a small adjustment.
- Excluded note 133: Buy felt pads for the dining chairs; the floor scratches line up with the two chairs used every day.

### development-015

Comment: Mixes basil with rosemary based on balcony/pot connection. Balcony pots would have been more fitting for this merged topic

- Excluded note 65: Turn the basil pot every few days so it does not keep leaning toward the window.
- Excluded note 121: Basil has new leaves on the window side. Rotate the pot and trim enough for tonight's pasta.

### development-017

Suggested label: Notebook app, notebook idea


### development-019

Comment: This shouldnt be a topic


### development-020

- Excluded note 162: On Sunday, plan one evening with no errands or projects. A free slot disappears unless I leave it visible in the calendar.

### development-023

Comment: Shouldnt be a topic


### development-024

Comment: Shouldnt be a topic


### development-025

Comment: shouldnt be a topic


### development-026

Suggested label: Photography, photo


### development-027

Comment: not a coherent topic


### development-028

Comment: not a coherent topic


### development-029

Comment: one note is about a guitar, a broader topic like Music instruments would've been fine

- Excluded note 150: Replace the worn guitar strings before recording the chord progression. The third string drifts out of tune after a few minutes.

### development-031

Comment: not a coherent topic


### development-032

Comment: not a coherent topic


### transfer-001

Suggested label: Stargazing


### transfer-002

Suggested label: Sewing


### transfer-005

Comment: not a coherent topic


### transfer-006

Comment: not a coherent topic


### transfer-007

Suggested label: Baking, Bread


### transfer-009

Comment: not a coherent topic


### transfer-010

- Excluded note 39: An easy run around the lake felt comfortable today. Keep the next outing short too.
