# First optional-LLM comparison: results

## Decision

Keep rules-based organization as the shipping default. The first Qwen3-0.6B Q8
prototype is **not ready to control journal grouping**. It produces some useful
labels and finds missed subjects, but also introduces unsupported memberships,
fragments good groups, and invents unhelpful topic names.

The experiment supports keeping the optional-download architecture open. It does
not yet demonstrate that enabling this particular model improves overall quality.
This is one pinned model, prompt, decoding configuration, and retrieval strategy;
it is not a conclusion about every possible small-model hybrid.

[Protocol and reproduction](OPTIONAL_HYBRID_COMPARISON.md) ·
[Complete results](HYBRID_COMPARISON_RESULTS.json)

## Completed comparison

All nine corpus/mode runs completed in the Android emulator. The 40 unseen notes
and their editorial references were fixed before inference and were not added to
the saved journal. Prompt text was not retuned against their results.

| Corpus | Mode | Topics | Notes grouped | Desired pairs recovered | Selected unwanted pairs |
| --- | --- | ---: | ---: | ---: | ---: |
| Original 72 | Rules | 14 | 63 | 9/9 | 0/1 |
| Original 72 | Labels only | 14 | 63 | 9/9 | 0/1 |
| Original 72 | Hybrid | 21 | 55 | 3/9 | 0/1 |
| Expanded 172 | Rules | 27 | 139 | 25/33 | 10/13 |
| Expanded 172 | Labels only | 27 | 139 | 25/33 | 10/13 |
| Expanded 172 | Hybrid | 51 | 136 | 18/33 | 2/13 |
| Held-out 40 | Rules | 6 | 18 | 20/69 | 0/5 |
| Held-out 40 | Labels only | 6 | 18 | 20/69 | 0/5 |
| Held-out 40 | Hybrid | 17 | 30 | 27/69 | 0/5 |

These are fresh-discovery results. The expanded journal's 25 **saved** topics differ
from its 27 discovery topics because of the previously documented identity-matching
bug. This experiment isolates inference and does not apply results to those identities.

Labels-only preserves every baseline member set by assertion. Its pair counts must
therefore equal Rules, even when labels improve or worsen. Its format failures use
the original name as fallback.

The unwanted-pair checks are deliberately small spot checks, not a false-positive
rate. In particular, 0/5 on the held-out set does **not** mean the hybrid made no
mistakes. It created numerous new false pairings outside those five preselected pairs.
Those failures are described below rather than retroactively changing the test set.

## Labels-only: some improvements, substantial limitations

Useful examples from the 172-note corpus:

| Existing label | Generated label | Assessment |
| --- | --- | --- |
| Workout gear | Strength Training Sessions | Clear improvement: these members are bench/pull-up logs. |
| Piano | Piano Practice and Recording | Accurate, slightly more descriptive. |
| Chat | Protecting coding efficiency during workday | Better captures purpose, but long for a topic name. |
| Groceries | Household Shopping | Broad enough to include food and household supplies. |

Failures and weak examples:

- Cancel becomes **Update and cancel syncs**, although one note cancels cloud storage.
- Path becomes **Cycling Route Idea for Recovery Runs**, conflating cycling with running.
- Library becomes **Library and Garden Tasks**, carrying the incidental gardening
  subject of one mixed note into the group label.
- Running gear becomes **Running Plan and Gear**; the underlying travel/gym/lunch
  admissions remain. A broader generated label does not repair bad membership.
- On held-out bread notes, **Baking Loaf Shaping and Photography** overemphasizes the
  incidental instruction to photograph the finished loaf.
- Two expanded-corpus labels exceed the allowed word count and fall back to the old
  name. The valid-output count is therefore 19/21 for that arm.

This model can replace an obviously bad name with a sensible one, but it is not a
reliable universal labeler with the current prompt.

## Hybrid: useful discoveries mixed with clear regressions

### Positive evidence

- It discovers **Photography** for the street-scene, sunset, and photo-walk notes
  (#139, #140, #142), which the current rules leave ungrouped.
- It links the brake-repair and chain-maintenance notes under **bike maintenance**
  (#126, #127), recovering a missed relationship.
- It produces useful strength subgroups for bench press, squats, and pull-ups.
- On held-out notes, it finds a coherent **Aquarium Maintenance** group (#7–9) and
  some useful sewing and pottery pairs. Desired pair recovery increases there.

### Failures visible in the same outputs

- **planting** includes a food-ingredient shopping note (#44) alongside gardening.
- **Payment** includes a general accessibility error-message note (#163) with Beacon
  payment warnings.
- **warranty** combines receipt organization (#137) with charging a phone outside the
  bedroom (#160).
- **note-58**, **note-147**, and **note-151** are generated as topic labels. They satisfy
  basic JSON/length constraints but do not explain a subject.
- Several groups are redundant variants of **run**, **planting**, and **shopping**.
  The 172-note count grows from 27 to 51, while fewer known useful pairs survive.
- Two hybrid responses repeat IDs; validation rejects them and falls back to the
  original Running gear and Meal preparation groups. Thus some old errors remain.

The held-out outputs make the quality problem especially clear:

- One coherent Aquarium Maintenance proposal is followed by another containing
  sewing-machine, pottery, and software-release notes.
- **glaze** combines a pottery test tile with a Japanese vocabulary sentence.
- **seam** includes a clothing-preparation bedtime note and a dentist cancellation.
- **Observation** combines telescope, fish-feeding, bread, and bedtime notes.

Many of these mistakes use IDs that are syntactically allowed but semantically wrong.
Restricting IDs to the supplied batch prevents fabrication of nonexistent records;
it cannot establish that the selected note belongs in the topic.

## Performance and resource observations

After the bounded runtime restart:

| Corpus | Label request median / p95 | Hybrid request median / p95 |
| --- | --- | --- |
| Original 72 | 0.449 s / 1.545 s | 1.111 s / 1.995 s |
| Expanded 172 | 0.603 s / 2.450 s | 1.305 s / 3.412 s |
| Held-out 40 | 0.490 s / 0.900 s | 1.206 s / 1.517 s |

- 101 logical requests across all arms; 100 distinct model calls because one identical
  request was reused from the request cache. Cached entries retain their original
  measured request latency; summaries above are not repeated-run confidence intervals.
- Four distinct format/contract failures: two overlong labels and two repeated-ID
  group responses. All are preserved in raw call logs and counted as fallbacks.
- Summed recorded model-request time is approximately 119 seconds for the full set.
  This excludes baseline generation, setup, model download/loading, and orchestration
  overhead. It does not represent the latency of saving one note in the future app.
- Model download: 639,446,688 bytes, approximately 610 MiB, outside the APK.
- Stripped experimental server: approximately 13 MB, separate from the app.
- Server resident memory after the run was approximately **897 MiB**, including mapped
  model pages. This is one end-of-run observation, not peak memory or the final app's
  incremental RAM requirement.
- The first unbounded-cache configuration exhausted emulator swap and was discarded
  for comparable timing. The bounded configuration completed all runs.

Emulator CPU/memory behavior cannot establish phone latency, battery drain, thermal
throttling, or compatibility with every device supported by the base app.

## Next comparison, before production integration

1. **Narrow the model's decision task.** Compare a small label-candidate task and
   one-note-to-one-topic judgments against the present multi-group JSON task. The
   current 0.6B model visibly struggles to retain correct note references across a batch.
2. **Keep deterministic protections around entities and identity.** Reject generated
   names that conflict with protected subjects, preserve manual decisions, and repair
   the existing identity bug independently. Do not disguise model errors with automatic
   unions of same-named groups.
3. **Measure naming separately.** Use per-member label fit and concise subject quality;
   pair metrics alone cannot compare the labeling arm.
4. **Compare model capacity and quantization.** If the narrow task still fails, evaluate
   a larger local model against its added download/RAM cost. This first run used Q8,
   so its mistakes cannot simply be blamed on aggressive four-bit quantization.
5. **Keep held-out evaluation honest.** This 40-note set is now observed; future prompt
   selection needs another untouched set. Retain current outputs as regression evidence.

There is no basis yet for enabling hybrid organization automatically after download.
The planned user-controlled optional mode and reliable Standard fallback remain the
right product boundary, but the enhanced engine must earn that name in further tests.

## Validation and artifacts

- Debug Android test APK built successfully.
- Emulator baseline-export instrumentation passed (one diagnostic test).
- Five Python contract/retrieval tests passed.
- Nine completed comparison runs; baseline memberships unchanged in every labeling arm.
- Full results committed in the linked JSON; individual requests/responses and timings
  remain under `.local/hybrid/comparison-v1-bounded/calls` for replay and inspection.
- No experimental grouping was written to the journal. The phone was not accessed.
