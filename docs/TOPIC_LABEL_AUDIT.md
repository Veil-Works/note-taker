# Topic-label audit

Audited the connected phone on 2026-09-15, before organization rules version 10.
The notebook contains 72 notes and 14 topics. The analysis uses actual saved
memberships, including overlapping memberships; counts below are not additive.
Judgments concern whether a name describes its members, not a measured accuracy score.

## All current labels

| Label | Notes | Assessment | Evidence and recommendation |
| --- | ---: | --- | --- |
| Atlas | 11 | Strong | Every member explicitly mentions Atlas. Search, indexing, performance, and mixed-project tasks belong here. Keep. |
| Balcony rosemary | 3 | Strong | Buying a pot/soil, diagnosing watering/sunlight, and repotting all concern the rosemary. One note also contains unrelated errands, which does not invalidate its membership. Keep. |
| Beacon | 7 | Strong | Billing, subscriptions, webhooks, receipts, and the multi-project work plan all explicitly mention Beacon. Keep. |
| Maya | 3 | Accurate but potentially redundant | All three involve Maya and Orion work; one also mentions Jonas and Atlas. A person-based topic is legitimate, but provides little separation from Orion in this small notebook. Do not suppress named people categorically. |
| Meal planning | 2 | Reasonable | Ingredients for next week's lunches and batch-roasting vegetables share meal preparation. Meal prep would be more precise, but the current label is not misleading. |
| Meeting planning | 3 | Too narrow | Reserving a workshop room is planning; recording decisions/owners and capturing meeting notes concern meeting outcomes. Meetings would cover all three more accurately. |
| Note-taking | 9 | Broad, with one questionable member | Six notes concern the notebook app; two concern writing/remembering short notes. A general button-wording observation is the weakest member. This is partly a membership/granularity issue, not something a better name alone solves. |
| Oats | 2 | Misleading | Oats is one shopping-list item and a background location in the coffee reminder. The actual common purpose is groceries/pantry replenishment. Groceries is a better label. |
| Orion | 11 | Strong | All members explicitly mention Orion. Onboarding, activation, release work, and mixed-project tasks fit. Keep. |
| Running | 9 | Strong broad topic | Training, a rainy run, recovery/nutrition, equipment purchases, and ideas while running all connect to running. Broad membership is appropriate; nutrition and equipment can additionally belong elsewhere. |
| Running shoes | 3 | Too narrow for final membership | Two notes concern shoe fit; the third buys socks and a water bottle. Running gear would cover all three, or the shoe topic should retain only shoe-focused notes. |
| Strength training | 5 | Strong | Bench press, squats/deadlifts, pull-ups, and the interaction between squats and running all fit. Keep. |
| Vienna | 3 | Strong | Train tickets, accommodation, transport, and a weekend itinerary share an explicit destination. Keep. |
| Workout gear | 2 | Strong | Packing lifting shoes/bottle/towel and buying running socks/bottle are both equipment preparation. A broader equipment topic can legitimately overlap Running. |

Eight labels are strong. Maya and Meal planning are defensible with qualifications.
Three labels have clear scope problems, and Note-taking needs a membership/granularity
review. These categories are editorial judgments, not calibrated quality metrics.

## Causes that generalize

1. **Incidental overlap:** repeated vocabulary can dominate a whole-note embedding
   even when it appears only in an aside. Matching a word is not sufficient evidence
   that the word names the shared purpose.
2. **Core-only naming:** the previous naming stage used initial core notes. Expansion
   and consolidation could add notes that no longer fit the core's narrow label.
3. **Average-only scoring:** strong support from some members can hide weak support
   from another member. Evaluate coverage as well as average relevance.
4. **Candidate limitations:** the existing fixed purpose vocabulary can offer a label
   such as Meeting planning when the simpler Meetings would better describe the group.
   Better scoring cannot always select a name absent from its candidate set.
5. **Membership versus naming:** the general button-wording observation in Note-taking
   needs a membership decision. Renaming the group would merely hide that uncertainty.

## Membership gaps, separate from label quality

- The post-run lunch reminder is in Running but misses Meal planning.
- The ingredients-for-lunches note belongs to Meal planning and could also fit Groceries.
- The household-supplies shopping list remains ungrouped.
- The basil-pot observation remains ungrouped, although a broader gardening topic
  could connect it to the rosemary notes.

These do not justify indiscriminately merging shopping, running, gardening, and meal
preparation. Multiple memberships should represent the overlap.

## General first-pass implementation

Rules version 10 evaluates labels using both whole-note and meaningful opening-sentence
embeddings from the existing MiniLM model. Vague openings fall back to the full note.
Scores consider the weakest-supported quarter of members as well as the average.
The lexical bonus is capped; phrase recurrence counts distinct notes rather than
repetitions within one note, and matching uses word boundaries.

Descriptive labels are also re-evaluated after membership expansion and consolidation.
Explicit recurring names and user-retained topic names remain protected. No additional
model, model download, noun-specific rule, or automatic user-data population is added.

The existing grocery pair is a regression case, alongside travel notes with incidental
camera mentions and debugging notes with incidental notebook mentions. The first
real-model run chooses Groceries, Travel planning, and Payment respectively. This
supports the specific changes, not a claim of general semantic understanding.

## Validated outcome

The final real-model run changes Oats to Groceries. The other 13 labels and all
memberships stay unchanged on the 72-note fixture. Thus Running shoes and Meeting
planning remain too narrow, and Note-taking remains a mixed group; evaluating final
members improves the design but has not resolved these examples. Broader candidate
generation and membership/granularity checks remain follow-up work.

All 54 unit tests and 55 emulator tests pass, including the two cross-domain examples,
custom-name protection, identity persistence, merging, and existing grouping checks.
Build and lint pass. The tested update was installed in place on the phone and emulator.

## Follow-up

Rules 11 address scope and mixed-subject matching. See the
[emulator-only follow-up](GROUPING_V11_REVIEW.md) for all resulting labels.
