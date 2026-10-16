# Note-taker

A private, local-first Android notebook built with Kotlin, Jetpack Compose, and
Room. This branch experiments with **note-first retrieval**: write directly on the
notes screen, autosave without a title or category, and browse direct related
notes chronologically. Standalone notes need no companions. There are no generated
summaries. Connections use the existing bundled offline sentence model.

See [Note-first experiment](docs/NOTE_FIRST_EXPERIMENT.md) for behavior and limits.
The background worker now computes connections only: it retires inferred topic
memberships and untouched inferred topics while preserving manual organization.
The topic tools below remain optional; the earlier discovery algorithm is retained
for comparison tests, not enabled in the worker.

## Topics

- One Topics browser and editor picker for manually created and discovered topics.
- A note can belong to several topics. All topics work in filters, list grouping,
  note labels, concept-style detail pages, and graph labels.
- Create, rename, merge, and delete topics; filter notes by topics under Filters.
- Each membership is either **Added by you** or **Added automatically**. **Keep**
  makes an automatic membership manual. **Remove** records a durable exclusion.
- Topic pages show all their notes and offer Add notes, Keep, and Remove.
- Manual assignments survive text edits. Automatic assignments can change.
  Renaming preserves the topic identity/name without freezing its memberships.
- Merging retains the target name, combines aliases and memberships, and redirects
  the source's discovery history and active filters to the target.
- Legacy discovered topics expire when their derived memberships are retired;
  topics you create, edit, merge, confirm, or correct remain available.
- Automatic assignments and generated names never become user training examples.

See [Unified topics](docs/TOPICS.md) for conflict rules, migration details, and
how the organizer reuses identities without merging on name similarity alone.

## Notebook features

- Five destinations: List, Net, New note, Topics, Settings; compact floating
  navigation with larger labels and unframed icons.
- Inline plain-text capture autosaves after 500 ms and finishes on backgrounding
  or leaving the composer. Done starts a fresh thought; failed saves retain text.
  The separate New note dialog retains its explicit-save/topic-prefix behavior.
  Existing-note edits autosave after 600 ms and on backgrounding.
  Done commits the editing session; outside taps discard that session's edits.
- Literal case-insensitive content search, multiple-topic filters, date ranges,
  sorting, frequently used topics, and optional list grouping.
- Bulk deletion, topic assignment/removal, and manual note connections.
- Related notes inferred by meaning, shared-topic connections, and independent
  manual links. Keep linked preserves a shared-topic connection as a manual link.
- The new related timeline uses direct semantic/manual connections only, orders
  by creation time, and offers closer/broader matches without assigning groups.
- Interactive graph with pan/zoom, topic labels, searchable note browser, and
  connection-strength filtering.
- System/light/dark appearance and compact-list preferences.
- Permanent note deletion with a short Undo action; UUID identifiers and UTC
  timestamps. New empty drafts are discarded; existing notes may be empty.

The application declares no internet permission. WorkManager supplies normal
system permissions for scheduling, wake locks, and reboot recovery. Android cloud
backup and device transfer are disabled. Uninstalling or clearing storage removes
all notes; export/backup is not available, so retain separate copies of important data.

## Development and testing

See [Development and emulator testing](docs/DEVELOPMENT.md) for architecture,
setup, Android Studio configuration, test reports, and troubleshooting.

Requirements: Android Studio (bundled JDK), Android SDK 36.1, and an Android
8.0 (API 26) or newer device/emulator. On this Apple Silicon Mac:

```bash
brew install --cask android-studio android-commandlinetools
./scripts/dev setup
./scripts/dev check
./scripts/dev emulator
./scripts/dev test
./scripts/dev run
```

The scripts create and explicitly select a dedicated ARM64 API 36 emulator.
Use `./scripts/dev emulator --headless` for terminal-only testing and
`./scripts/dev stop` to shut it down. `./scripts/dev doctor` checks the toolchain.

## Signed release

Release credentials never belong in the repository. Generate and safely back up a private keystore, copy `release-signing.properties.example` to the ignored `release-signing.properties`, fill in the four values, and run:

```bash
./gradlew clean assembleRelease
```

The signed APK is written under `app/build/outputs/apk/release/`. Without the private properties file, Gradle deliberately produces an unsigned release APK; the debug APK remains installable with Android's standard debug key.

## Architecture

```text
Compose screens → ViewModels / immutable state → NoteRepository → Room / SQLite
                                                  ↓
                            WorkManager → offline embeddings + topic discovery
```

`DefaultAppContainer` assembles dependencies. The single `app` module contains
`feature/` screens, `model/` values, `data/` repositories, `data/local/` schema and
DAOs, `organization/` inference and matching, and `ui/` shared components.

Room schema 8 migrates versions 1–7 without deleting the notebook. Internal `Tag`
names and the `tags` table remain as the storage identity for unified topics;
there is no separate automatic-topic membership store. Schema files are committed.

The organizer uses a durable queue and cached embeddings. Database triggers enqueue
input changes atomically. Inference runs off the UI thread and commits only if the
notebook generation still matches. Failures leave work pending with retry controls.
Automatic changes do not update note edit timestamps.

## Model and limitations

The bundled quantized MiniLM model uses ONNX Runtime Android and never downloads
models at runtime. See [model provenance and inference](MODEL.md).

Meaning-based organization is approximate, particularly for short or ambiguous
notes. The model is English-focused. Pairwise analysis currently scans the notebook;
large notebooks will need a more efficient neighbor index. Emulator performance is
not a substitute for physical-device evaluation.

## Deferred roadmap

Export/backup, import, synchronization, accounts, semantic search in the search box,
structured queries, and generative AI remain deferred. See the product brief and
[MVP acceptance history](MVP_ACCEPTANCE.md) for context and manual acceptance checks.
