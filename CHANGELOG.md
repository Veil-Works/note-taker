# Changelog

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
