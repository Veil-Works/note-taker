# Note-taker

Note-taker is a small, private, local-first Android notebook. Version 5 adds predictable tag suggestions and editable keywords/aliases to the local notebook described in [`note-taker-product-brief.md`](note-taker-product-brief.md).

## Current features

- Suggest existing tags from saved text using tag names and user-configured words/phrases
- Accept or dismiss suggestions in the editor; dismissals persist until saved note text changes
- Add, edit, and remove aliases through **Manage tag keywords** in the note editor

- Floating capture field over the bottom of the notes list, with scroll space to keep the final note accessible
- New notes save only when the checkmark (Complete note) is tapped; successful completion clears the field
- Existing-note edits autosave after 600 ms and when leaving/backgrounding the editor
- Tap a saved note to edit it in a compact popup over the list; Done saves pending changes and closes it
- Create tags and attach/remove them using selectable chips in the note popup
- Browse tags with the top-bar Tags button; select multiple tags to require all of them alongside text search
- Delete a tag from the library using its trash button; confirmation removes it from every note and deletes its keywords
- Long-press a note or tap Select, then select multiple notes to delete, add/remove tags, or connect every selected pair
- Bulk deletion requires confirmation; bulk tagging shows checked tags for all selected notes, a dash and count for partially applied tags, and empty boxes for unapplied tags. Toggle on to add to all or off to remove from all; untouched tags are preserved. Bulk connections are manual links
- Notes with shared tags appear automatically in Connected notes; the connection disappears when the last shared tag is removed
- Tap Keep linked to preserve an automatic connection as a manual link, or connect other notes from the editor by searching content or tags
- Open a connected note or remove a connection from either endpoint; links have no relationship types
- Combine text and tag search with Today, Last 7 days, Last month, or an inclusive custom date range
- Filter by creation or edit date; sort by recently edited, newest created, oldest created, or alphabetical preview
- Access the five most-used tags with note counts and a Last 7 days shortcut
- New empty drafts are discarded; existing notes may be empty
- Most recently edited notes first by default
- Case-insensitive literal content search
- Permanent deletion with a short Undo action
- Local Room database with stable UUID identifiers
- System light/dark colors and basic accessibility semantics

The application declares no internet or other runtime permissions. Android cloud backup and device transfer are disabled, so note data stays in the app's private on-device database.

> **Data ownership warning:** Uninstalling the application or clearing its storage permanently removes all notes. Export and backup are intentionally deferred from this MVP; do not use this version as the only copy of irreplaceable information.

## Requirements

- Android Studio with Android SDK 36.1
- JDK 17 or newer supported by Gradle 9.4.1
- Android 8.0 (API 26) or newer device/emulator

This checkout's ignored `local.properties` points to the local SDK. On another machine, open the project in Android Studio or create it with:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

## Build and test

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

With the phone connected, install both APKs in place and run the tests:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w app.notetaker.test/androidx.test.runner.AndroidJUnitRunner
```

Tests use isolated in-memory or temporary databases. In-place installation preserves
the phone's notebook. Use the connected phone for device verification.

Install the debug build for personal testing:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Signed release

Release credentials never belong in the repository. Generate and safely back up a private keystore, copy `release-signing.properties.example` to the ignored `release-signing.properties`, fill in the four values, and run:

```bash
./gradlew clean assembleRelease
```

The signed APK is written under `app/build/outputs/apk/release/`. Without the private properties file, Gradle deliberately produces an unsigned release APK; the debug APK remains installable with Android's standard debug key.

## Architecture

```text
Compose screens
    ↓
ViewModels and immutable UI state
    ↓
NoteRepository
    ↓
RoomNoteRepository
    ↓
Room / SQLite
```

The single `app` module is grouped by responsibility:

- `model`: persistence-independent note model and display helpers
- `data/local`: Room entity, DAO, and versioned database
- `data`: repository contract and Room implementation
- `feature/notes`: ordered list and debounced search
- `feature/editor`: loading, autosave, deletion, and error handling
- `ui/theme`: Material 3 theme

Dependencies are assembled by `DefaultAppContainer`; no dependency-injection framework or future cloud abstraction is included.

## Behavioral details

- Tags are attached manually or by accepting a suggestion. Capture a note as usual, then open its popup to organize it.
- Tag names are trimmed, limited to 40 characters, and deduplicated without case differences. Each note can have multiple tags; removing a tag from a note does not remove it from the library.
- Completing a new note clears text, tag, and date filters and restores the default sort so it appears at the top.
- Date ranges use local calendar days, including the entire end date. Last 7 days includes today and six prior days; Last month means the previous calendar month. Date presets refresh when the screen resumes.
- Filter/sort selections survive screen recreation. Frequently used tags count notes across the whole notebook, independently of the active filters.
- V5 uses Room schema version 4; migration preserves existing notes, tags, and connections.
- Suggestions match case-insensitive literal words/phrases at Unicode word boundaries. There is no built-in vocabulary or automatic attachment.
- Aliases are trimmed and deduplicated per tag without case differences. The same alias can suggest multiple tags.
- Suggestions evaluate saved text off the UI thread. Tag-only edits and alias changes preserve dismissals; saved text changes reset them.
- Room schema version 3 migrates v1/v2 data in place. Deleting a note removes its tags and connections; Undo restores its tags and connections to notes that still exist.
- Manual connections are bidirectional and unique. A note cannot connect to itself. Content/tag edits preserve manual connections. Shared-tag connections are derived from saved tags and never stored as manual links; removing a manual link leaves the automatic connection visible while tags are still shared.
- Opening a connected note saves pending edits and replaces the current popup. Done/Back returns to the notes list.

- Note IDs are UUID strings. Timestamps are UTC epoch milliseconds and formatted in the device locale/time zone.
- Search trims its query and escapes SQLite `%`, `_`, and `\`, so user input is treated literally.
- A new database row is created only on completion of a non-blank draft. Failed completion retains the text for retry. Completing during search clears the filter so the new note appears in the list.
- Autosave failures keep the editor open and show an error. A failed delete also leaves the user in the editor.
- Undo reinserts the exact deleted record, preserving its UUID and timestamps.

## Deferred roadmap

Entities, semantic search, AI, import/export, synchronization, accounts, and cloud services remain deferred. They should be introduced one independently useful stage at a time, in the order and under the constraints defined by the product brief.
