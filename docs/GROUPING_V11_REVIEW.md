# Grouping rules 11: emulator evaluation

Evaluated on the existing 72 short notes using the bundled MiniLM model, on the
API 36 emulator only. No new model, benchmark population, or phone installation.

## Changes observed

| Rules 10 | Rules 11 | Reason |
| --- | --- | --- |
| Meeting planning (3) | Meetings (3) | Covers workshop planning, decisions, and meeting notes. |
| Running shoes (3) | Running gear (3) | Covers shoes plus socks and a water bottle. |
| Meal planning (2) | Meal planning (3) | Adds the post-run lunch reminder, retaining its Running membership. |
| Note-taking (9) | Note-taking (10) | Adds the idea to capture meeting decisions as notes. |

The other ten labels and their memberships are unchanged: Atlas (11), Orion (11),
Beacon (7), Running (9), Strength training (5), Balcony rosemary (3), Maya (3),
Vienna (3), Groceries (2), and Workout gear (2). Total: 14 topics, 75 memberships,
63 grouped notes, and 9 ungrouped notes. Overlaps make membership counts additive
across topics but not equivalent to unique notes.

## Regression coverage

The real-model notebook test checks these improvements and guards against food
shopping/afternoon-coffee notes entering Running, or idea capture entering Workout
gear. Separate travel and payment-debugging examples verify that incidental camera
and notebook mentions do not become labels. Unit checks cover fragment boundaries,
inflection candidates, coherent versus unsupported splits, and declining a topic
without a supported name.

## Remaining limitations

The broad Note-taking group still includes a weak general button-wording
observation. Its subgroups did not meet all split and naming requirements, so no
split was forced. Household shopping and isolated observations remain ungrouped;
meal ingredients do not additionally join Groceries. Maya remains a legitimate
but overlapping named-person topic. These are focused regressions and editorial
judgments, not a general accuracy score. Larger notebooks, shorthand, and other
languages need further evaluation.

## Validation

`./scripts/dev check` passed: 58 unit tests, debug/test APK builds, and lint
(0 errors, 24 existing warnings). All 55 isolated instrumentation tests passed on
`emulator-5554`, including the real-model regressions and UI/persistence suite.
After launching the updated app, saved groups matched the evaluation above;
organization completed without error. All 72 saved note rows had the identical
SHA-256 digest before and after regrouping. The phone was not accessed or updated.

## Larger-sample follow-up

The [172-note evaluation](EXPANDED_GROUPING_REVIEW.md) exposes significant failures
not visible in this 72-note regression, including equipment-label scope, ambiguous
word matching, and retained-topic identity merging. Read it before interpreting
the improvements above as evidence of general grouping quality.
