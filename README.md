# Note-taker

Note-taker is a private, local-first Android notebook. Version 7 organizes saved
notes in the background: it finds related notes by meaning, groups recurring
topics, and attaches existing concepts when the evidence is strong. Capture needs
no tags, aliases, or review steps.

## Current features

- Save immediately; a durable background queue organizes the latest text without blocking capture
- Find **Related notes** in the editor and browse automatically discovered groups through **Topics**
- Create provisional topic names from phrases in the notes; rename groups or merge them explicitly
- Automatically attach strong, unambiguous existing-concept matches and learn from manually tagged examples
- Remove an automatic association with one tap; the correction survives text edits and app restarts. **Keep** makes it manual
- Keep manual tags and links intact; automatic organization does not change note edit timestamps or ordering
- Access advanced manual controls through **Tags and connections** in the editor
- Run semantic inference entirely offline using a bundled quantized sentence model; see [model provenance and evaluation](MODEL.md)

- Upgrade any tag to an entity by assigning one optional type; remove its type to return it to an ordinary tag
- Open **Tags → Browse concepts and types**, or **Details** in a note's tag picker, to browse concepts and all their associated notes
- Rename concepts and manage aliases from their detail pages; concept pages ignore the notes list's active filters
- Manage editable starter types (Exercise, Project, Research topic, Algorithm, Person, Place, Task, Idea), add custom types, or delete a type while preserving its concepts as ordinary tags
- Search one tag/entity picker and see types in suggestion chips, note labels, and shared-connection reasons

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

The application declares no internet permission or user-requested runtime permissions. WorkManager supplies normal system permissions for background scheduling, wake locks, and reboot recovery. Android cloud backup and device transfer are disabled, so note data stays in the app's private on-device database.

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
adb shell am instrument -w app.notetaker.test/app.notetaker.IsolatedTestRunner
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

- Tags can be attached manually, by accepting a suggestion, or automatically. Automatic and manual associations are stored separately; user corrections take priority.
- Tag names are trimmed, limited to 40 characters, and deduplicated without case differences. Each note can have multiple tags; removing a tag from a note does not remove it from the library.
- Completing a new note clears text, tag, and date filters and restores the default sort so it appears at the top.
- Date ranges use local calendar days, including the entire end date. Last 7 days includes today and six prior days; Last month means the previous calendar month. Date presets refresh when the screen resumes.
- Filter/sort selections survive screen recreation. Frequently used tags count notes across the whole notebook, independently of the active filters.
- V7 uses Room schema version 6; migration preserves existing notes, tags, and connections.
- Suggestions ignore case and match words/phrases, including word prefixes in either direction when the shorter word has at least three letters (`run` matches `runs` and `running`). Short words, numbers, and punctuation-bearing terms such as `C++` require literal matches. Prefix matches remain suggestions; automatic attachment requires stronger evidence.
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

Structured queries, semantic search in the search box, generative AI, import/export, synchronization, accounts, and cloud services remain deferred. Semantic related notes and automatic topic grouping are available in v7. They should be introduced one independently useful stage at a time, in the order and under the constraints defined by the product brief.

## Automatic organization behavior

Existing notebooks are queued for analysis during the schema upgrade. Note saves
and changes to manual organization enqueue work atomically through SQLite triggers.
A WorkManager job computes results off the UI thread and commits only if the
notebook generation still matches. Failures keep work pending, retry with backoff,
and expose **Retry organization** in the editor and Topics page.

The organizer caches embeddings and ranks at most five semantic neighbors per
note. Topics require at least two related notes unless customized. Names are
extracted phrases, not generated summaries. Renaming preserves the name; merging
also preserves the combined membership. Related notes and topic groups can update
as content changes. Pending analysis may temporarily display the previous results.

Removing a concept records a durable per-note exclusion and a negative example
of that saved text. Manually adding it again clears the exclusion. Similar future
notes use positive and negative examples; one correction is not a global ban.
Existing suggestion dismissals are respected until their usual text-change reset.
Automatic tags are capped at five and never used as positive training examples.

The initial model is English-focused. Meaning-based organization is approximate,
particularly for very short or ambiguous notes. Analysis currently scans notebook
pairs; embedding inference is cached, but very large notebooks will need a more
efficient neighbor index. All note content stays on the device.
