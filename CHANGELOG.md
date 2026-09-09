# Changelog

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

## Planned v2 — Tags and manual organization

- Create and display tags.
- Attach multiple tags to a note and remove them.
- Filter the notes list by tag and browse all notes belonging to a tag.
- Keep capture quick and preserve v1 notes through a tested Room migration.

Note-to-note links are stage 3 in the product brief; AI and synchronization remain
later stages.
