# Changelog

## Unreleased — short-note grouping and label scope

- Recover multiple subjects using corroborated sentence/clause matches.
- Generate inflection-aware observed labels, broaden weak qualifiers, and check
  individual-member coverage before publishing a topic.
- Split only strongly separated, independently labelable cohorts; leave uncertain
  groups unnamed rather than forcing a generic topic.
- Reprocess automatically with rules version 11; retain the bundled model.
- Validate only on the emulator; see [results](docs/GROUPING_V11_REVIEW.md).

## Unreleased — general topic-label coverage

- Score labels against whole notes and meaningful opening sentences with the existing
  embedding model, including weaker member coverage rather than average similarity alone.
- Reduce incidental-word influence, count phrase recurrence across distinct notes,
  and use word-boundary matching for lexical evidence.
- Re-evaluate descriptive names after membership expansion and consolidation.
- Preserve explicit subjects and customized names; reprocess on rules version 10.
- Audit all 14 phone groups in [the label review](docs/TOPIC_LABEL_AUDIT.md).

## Unreleased — simpler organization controls

- Remove Topics and Browse topics from the list header; move topic selection into
  Filters while retaining selected-topic and frequent-topic shortcuts.
- Show semantic matches, shared-topic connections, and manual links in one
  deduplicated Connections list, with reasons and Keep linked/Remove link controls.
- Remove manual alias and keyword editors; retain existing aliases internally for
  matching and preserve merged topic names.

Validation: 52 unit tests pass. All 54 emulator tests pass across the full run and
the targeted rerun after updating the topic-deletion test to use the Topics view.
Debug builds and lint pass; light/dark walkthroughs cover the simplified screens.

## Unreleased — retire topic types

- Remove type management, selection, and type suffixes from all topic displays.
- Simplify topic editing to Rename topic and the editor entry point to Manage topics.
- Allow merges regardless of legacy type differences, preserving topic membership,
  aliases, and exclusions. Keep legacy storage compatible without a schema migration.

## Unreleased — capture shortcuts and focused note details

- Assign topics when capturing with leading `#Atlas #orion` prefixes; match names
  case-insensitively, deduplicate, and create missing topics atomically.
- Support quoted multiword topics and retain explicit assignments as manual choices.
- Keep prefixes out of the saved body and preserve drafts that contain only topics.
- Replace the expanding Topics and connections section with separate Topics and
  Connections panels and a compact topic summary in the editor.

Validation: 52 unit tests and 54 isolated emulator tests pass, including prefix
capture, case-insensitive reuse, retained memberships, invalid drafts, and the
updated topic/link editing flows. Debug builds and lint pass with zero errors;
light and dark editor screenshots were inspected.

## Unreleased — short-note grouping

- Recognize recurring project names anywhere in short notes, including multiple
  projects in one note.
- Evaluate new memberships against fixed representatives from coherent cores.
- Consolidate redundant inferred groups and preserve redirects from retired topics.
- Rank purpose labels and repeated phrases with the local model; protect custom names.
- Store representative explanations and actual similarity scores for discovered memberships.
- Reprocess existing notebooks automatically when organization rules upgrade to version 9.

Validation: 48 unit tests and 51 isolated API 36 ARM64 emulator tests pass; debug
build and lint complete with zero errors. The existing 72-note notebook upgrades
without note or timestamp changes. See [grouping review](docs/GROUPING_REVIEW.md)
for measured results and remaining limitations.

## 8.0.0 — 2026-09-15

- Unify tags and automatic groups into one Topics browser, picker, and detail page.
- Use the same topics for note labels, filters, list grouping, and graph labels.
- Allow multiple topics per note; preserve manual membership and durable exclusions.
- Add per-note Keep/Remove controls and Add notes on topic pages.
- Merge topics transactionally, preserving aliases, compatible types, manual/automatic
  membership, exclusions, and discovery lineage. Active filters follow merged IDs
  and clear references to deleted or expired topics.
- Keep renamed/customized topics stable while allowing automatic memberships to change;
  never train on generated names or automatic memberships.
- Migrate Room schema 6 to 7, preserving old tags and discovered/customized groups,
  disambiguating collisions, and retaining all earlier migration paths.
- Compact the floating navigation, enlarge its labels, and remove icon frames.


Validation: 45 unit tests and all 49 API 36 ARM64 emulator tests pass; debug
build and lint complete with zero errors (24 existing warnings). New coverage
includes migration collisions, upgrade paths 1–6, topic lifetime and identity,
multiple automatic memberships, merge conflicts and lineage, and the unified
browser/editor/filter flow. Light and dark walkthroughs pass.

## 7.0.0 — 2026-09-11

- Organize saved notes through a durable background queue and bundled offline sentence model.
- Find semantic related notes and discover phrase-named topics without initial tags or setup.
- Reuse strong existing-concept matches and learn from manual examples and durable corrections.
- Keep automatic and manual associations separate; preserve user choices and note edit timestamps.
- Add topic rename/merge controls and move manual organization behind Tags and connections.
- Migrate schema 5 to 6, queue existing notes, cache embeddings, and guard analysis commits against concurrent edits.
- Isolate instrumentation startup from the phone notebook and background organizer.

Validation: 35 unit tests and all 34 isolated phone instrumentation tests pass;
debug build and lint complete with zero errors. Coverage includes automatic
capture-to-topic organization, semantic neighbors, corrections and Undo, learning
from manual examples only, generation guards, cached embeddings, schema 5→6
migration, editor restoration, and existing capture/tag/link/filter regressions. The normal
app launched successfully after the in-place upgrade and WorkManager reported
SUCCESS for organization of the existing notebook.

## 6.0.0 — 2026-09-09

- Upgrade existing tags with an optional entity type while retaining IDs, note associations, aliases, and suggestion dismissals.
- Add concept pages with name/type editing, alias management, and every associated note regardless of list filters.
- Add editable starter types and custom types; deleting a type returns its concepts to ordinary tags.
- Show types in the searchable tag/entity picker, suggestions, note labels, and shared-connection reasons.
- Preserve explicit suggestion acceptance and existing capture behavior.
- Migrate Room schema 4 to 5 in place, including starter types and a joined concept view.


Validation: 31 unit tests and all 28 phone instrumentation tests passed; debug
build and lint completed with zero errors. Tests cover the Bench press upgrade,
typed suggestion acceptance, concept-note browsing, ambiguous aliases, type
renaming/removal, restoration after type deletion, and v1–v4 migrations. Installed
in place on the connected phone.

## 5.0.0 — 2026-09-09

- Suggest existing tags from saved note text with case-insensitive whole-word/phrase matching.
- Add, edit, and remove per-tag keywords/aliases in the editor; shared aliases can suggest multiple tags.
- Accept suggestion chips explicitly or dismiss them persistently for unchanged text.
- Reevaluate after saved text changes; preserve dismissals through tag-only edits and alias edits.
- Evaluate suggestions off the UI thread without adding work to quick capture.
- Migrate Room schema 3 to 4, preserving notes, tags, and connections.
- Isolate capture UI tests from the phone’s notebook.

Validation: 27 unit tests and lint passed; all 18 app instrumentation tests passed
on the connected phone. Coverage includes aliases, matching boundaries, saved-edit
reevaluation, acceptance, dismissal persistence after database reopen, v1/v2/v3
migrations, and capture/tag/link/filter regressions.

## 4.0.0 — 2026-09-09

- Combine text and tag search with Today, Last 7 days, Last month, or an inclusive custom calendar range.
- Choose creation or edit dates, and sort by recently edited, newest created, oldest created, or alphabetical preview.
- Add shortcuts for the last seven days and the five most-used tags, with notebook-wide note counts.
- Preserve applied filter/sort choices across screen recreation; refresh relative dates when the screen resumes.
- Clear filters and restore default sorting after capture; reset list position when filters or the first result change.
- Keep the version 3 database schema and all existing notes, tags, and connections.

Validation: 25 unit tests and lint passed; 12 isolated device tests passed across
filter/calendar/capture flows and existing tag/link/database coverage. Date tests
cover inclusive boundaries, DST, calendar-month transitions, combined filters,
sorting, saved preferences, and live tag counts. App updated in place on the phone.

## 3.0.0 — 2026-09-09

- Connect notes with simple, bidirectional links; no relationship types.
- Search connection candidates by content or tag, with shared-tag notes first.
- Show connected notes in the editor, save before navigating, and remove links from either note.
- Prevent self-links and duplicate connections; preserve connections during content/tag edits.
- Migrate v1/v2 databases in place. Deletion removes connections; Undo restores links to surviving notes.

Validation: unit tests and lint passed; nine isolated database and UI tests passed
on the connected phone, covering migration, persistence, link creation/navigation/removal,
and tag regression. App updated in place without clearing phone storage.

## 2.0.0 — 2026-09-09

- Manual tag creation, multi-tag assignment, and removal in the note popup.
- Clear the tag-name field after successful creation so the next tag can be entered immediately; retain input on failure.
- Tag labels on notes and a tag browser/filter above the list.
- Combined tag and text filtering; completion clears filters to reveal new notes.
- Case-insensitive tag deduplication and atomic note/tag updates.
- Room migration from version 1 preserving existing note records.
- Delete/Undo restores tag associations as well as note content.

Validation: unit tests and lint passed; five isolated database and UI tests passed
on the connected phone, covering migration and creating, attaching, filtering,
and removing tags. App updated in place without clearing phone storage.

## 1.0.0 — 2026-09-09

First local Android notebook release.

- Capture short notes in a floating field over the main list.
- Complete a note to save it and clear the field for the next thought.
- Edit existing notes in a compact popup with Done and Delete actions.
- Search note content, and undo a deletion.
- Store notes locally using Room, stable UUIDs, and creation/modification timestamps.
- Keep the final list entry accessible above the capture field and keyboard.

Validation: unit tests passed; capture and overlay tests passed on the emulator;
popup editing passed on the connected phone. Broader manual acceptance items
remain listed in MVP_ACCEPTANCE.md. The installable APK uses debug signing;
private release signing requires the owner's keystore.

## Roadmap reference — Tags and manual organization (delivered in v2)

- Create and display tags.
- Attach multiple tags to a note and remove them.
- Filter the notes list by tag and browse all notes belonging to a tag.
- Keep capture quick and preserve v1 notes through a tested Room migration.

Note-to-note links are stage 3 in the product brief; AI and synchronization remain
later stages.
