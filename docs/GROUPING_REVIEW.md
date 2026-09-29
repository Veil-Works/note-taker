# Short-note grouping: implementation review

2026-09-15, organization rules version 9, API 36 ARM64 emulator.
Compared the actual saved notebook before and after the upgrade using the existing
72 short notes in `fixtures/sample-notes.json`. Note numbers below are one-based
positions in that file. No manual labels or exclusions were present in this notebook.

## Results

| Measure | Before | After |
| --- | ---: | ---: |
| Notes | 72 | 72 |
| Topics | 14 | 14 |
| Notes with an automatic topic | 57 | 63 |
| Ungrouped notes | 15 | 9 |
| Automatic memberships | 61 | 73 |
| Notes with multiple topics | 4 | 7 |

Coverage is not accuracy. The useful improvements are specific recovered connections
and clearer topic boundaries:

- Orion now includes the sample-workspace idea (#8).
- The work plan (#23) belongs to Orion, Atlas, and Beacon.
- The review request (#27) and shared empty-state idea (#16) belong to both Orion and Atlas.
- The rainy run (#41) joins Running, alongside the previously fragmented run notes.
- Running shoes remains a distinct topic; the socks purchase (#46) spans Running,
  Running shoes, and Workout gear. The grocery note (#44) stays outside Running.
- The squat/long-run observation (#34) belongs to both Strength training and Running.
- The notebook idea (#51) joins Note-taking, which consolidates the overlapping
  Notebook and Observation groups.
- Session becomes Strength training, Rice becomes Meal planning, and Related notes
  becomes Meeting planning. Running (2) becomes the more specific Running shoes.

Two retired provisional topic IDs redirect to surviving topics. All 72 note IDs,
contents, creation times, and edit times are unchanged. The organization queue is
complete with no error, and the database passes its foreign-key check.

## Remaining limitations

- Oats is still too literal a label for the two coffee/grocery notes (#43, #50).
- The post-run lunch reminder (#42) belongs to Running but misses Meal planning.
- Household shopping (#45) remains ungrouped; there is no broad Shopping topic yet.
- Note-taking includes a button-wording observation (#59), which is relevant to the
  app but could eventually be better represented by a separate interface-design topic.
- Nine notes remain ungrouped. Several concern isolated errands or observations;
  forcing those into existing groups would not demonstrate better organization.
- Label candidates, capitalization cues, and similarity thresholds remain English
  heuristics. These results do not establish general accuracy or large-notebook speed.

## Validation

- 48 JVM unit tests pass.
- 51 isolated emulator instrumentation tests pass, including real-model membership
  checks, provisional consolidation and redirect persistence, user-name protection,
  corrections, migrations, capture, filtering, and UI regressions.
- Debug app and test APKs build; lint reports zero errors and 24 existing warnings.
- The updated app was installed in place and the populated notebook reorganized
  successfully. The tests did not replace or reset that notebook.
