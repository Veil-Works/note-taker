# Changelog

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
