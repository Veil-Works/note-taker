# Development and emulator testing

## Project map

This is a single-module native Android app (`app.notetaker`), version 8.0.0.
Kotlin and Jetpack Compose implement the UI; ViewModels expose state through
coroutines/Flow. `NoteRepository` separates screens from the Room/SQLite storage
implementation. `DefaultAppContainer` wires production dependencies.

- `feature/`: notes, editor, graph, concepts/topics, and settings screens.
- `data/local/`: Room schema version 7, DAOs, and migrations from versions 1–6.
- `organization/`: durable WorkManager processing, WordPiece tokenization,
  bundled MiniLM inference through ONNX Runtime, and grouping/matching rules.
- `app/src/test/`: JVM tests for ViewModels, filters, graph layout, and rules.
- `app/src/androidTest/`: Compose flows, SQLite persistence/migrations, and
  real-model inference. `IsolatedTestRunner` substitutes a plain Application;
  test fixtures provide in-memory or temporary databases.
- `app/schemas/`: committed Room schemas; retain migration history when editing storage.

The app needs no server, API keys, runtime model download, or network permission.
The model and vocabulary are committed; provenance and checksums are in `MODEL.md`.
The current toolchain is pinned in `gradle/libs.versions.toml` and the Gradle wrapper:
AGP 9.2.1, Gradle 9.4.1, Compose BOM 2026.02.01. Compilation targets Java 17,
Android SDK 36.1, with target API 36 and minimum API 26. Preserve the existing
KSP compatibility setting until a separately tested dependency upgrade replaces it.

## Mac setup

Install [Android Studio](https://developer.android.com/studio) and command-line tools:

```bash
brew install --cask android-studio android-commandlinetools
./scripts/dev setup
./scripts/dev doctor
```

`setup` installs SDK platform 36.1, build tools 36.0.0, platform tools, the emulator,
and an API 36 Google APIs system image matching the host CPU. Accept SDK licenses
when prompted. It creates `NoteTaker_API_36` with a Pixel 8 hardware profile, and
writes ignored `local.properties` only if it does not already exist. It never
replaces an existing AVD.

The scripts use Android Studio's bundled JDK, then an installed macOS JDK.
SDK discovery prefers `ANDROID_HOME`, `local.properties`, `~/Library/Android/sdk`,
then Homebrew's `/opt/homebrew/share/android-commandlinetools` on Apple Silicon.
Set `JAVA_HOME` and `ANDROID_HOME` to override discovery. No shell profile edits
are necessary. Other hosts need a compatible JDK and Android command-line tools
installed before using the Bash scripts.

Open the repository root in Android Studio and allow Gradle sync. Select the
bundled JDK in **Settings → Build, Execution, Deployment → Build Tools → Gradle**.
If Studio asks for an SDK location, use the path printed by `./scripts/dev doctor`.
Choose the `app` run configuration and `NoteTaker_API_36` device.

## Daily workflow

```bash
./scripts/dev check                 # JVM tests, lint, and both debug APKs
./scripts/dev emulator              # visible emulator, waits for boot
./scripts/dev run                   # build, install in place, launch
./scripts/dev test                  # all instrumentation tests
./scripts/dev stop
```

For terminal-only tests use `./scripts/dev emulator --headless`. Emulator startup
uses cold boots and disables snapshots for repeatability; notebook storage still
persists between runs. Logs are saved to `.local/logs/emulator.log`.
For process supervisors or agent terminals that clean up background children, run
`./scripts/dev emulator --foreground` in a separate terminal/session and keep it
running during tests. It can be combined with `--headless`.

Commands select `emulator-5554` and verify the AVD name before device operations.
A connected physical phone is not the target. If port 5554 is occupied, use an
unused even port consistently, for example `EMULATOR_PORT=5556 ./scripts/dev emulator`
and `EMULATOR_PORT=5556 ./scripts/dev test`.

Run a single test class or a JVM test:

```bash
./scripts/dev test -Pandroid.testInstrumentationRunnerArguments.class=app.notetaker.NoteCaptureFlowTest
./scripts/dev gradle testDebugUnitTest --tests '*EditorViewModelTest'
./scripts/dev gradle assembleRelease  # unsigned unless private signing is configured
```

Reports and outputs:

- JVM tests: `app/build/reports/tests/testDebugUnitTest/index.html`
- Lint: `app/build/reports/lint-results-debug.html`
- Emulator tests: `app/build/reports/androidTests/connected/debug/index.html`
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- Device diagnostics: `./scripts/dev logs` (Ctrl-C exits).

## Development checkpoints

Run `check` after changes. Run `test` for UI, persistence, migrations, and organization
changes; it includes the bundled model benchmark. Preserve schema history and add
migration coverage when changing Room entities. Keep model assets/checksums in sync
if intentionally replacing the inference model.

The baseline emulator validates target API 36 on ARM64 for this Mac. API 26 support,
other screen sizes, accessibility, lifecycle/reboot persistence, and physical-device
performance need additional verification. Follow `MVP_ACCEPTANCE.md` for manual
checks; historical checked items there do not prove the current checkout passes.
The notes screen, repository responsibilities, and pairwise organization scan
are areas to watch as features and notebook sizes grow. See [Unified topics](TOPICS.md) for membership, merge, and discovery rules.

Debug installation preserves emulator notes. Gradle's connected-test task manages
test APK installation and cleanup; keep emulator data disposable. There is no
export/backup feature, so use synthetic notes for development.

## Troubleshooting

- Missing Java: install Studio or set `JAVA_HOME` to a supported JDK.
- Missing SDK/licenses: rerun `setup`; use `sdkmanager --licenses` with the same SDK
  if packages report unaccepted licenses.
- Boot failure: inspect `.local/logs/emulator.log` and run `doctor` to check hardware
  acceleration. Close an emulator occupying the selected port or choose another port.
- Gradle download failures: check network access to Google Maven, Maven Central,
  and Gradle distributions; use `./scripts/dev gradle --stop` before retrying if needed.
- UI failure: inspect the connected test report and logcat before retrying; avoid
  assuming a timeout is an emulator problem.

## Verified baseline — 2026-09-15

Verified on Apple Silicon macOS with Android Studio 2026.1.4.7, its bundled
JetBrains JDK 25.0.3, and `NoteTaker_API_36` (Android 16 / API 36, ARM64, Pixel 8).
Hardware acceleration uses macOS Hypervisor.Framework.

- `./scripts/dev check`: passed; **45 JVM tests**, zero failures/skips;
  debug app and instrumentation APKs built.
- Lint: **zero errors, 24 warnings**. Existing warnings include dependency/API
  updates, screen-size configuration access, redundant resource qualifiers, and
  SharedPreferences style suggestions.
- `./scripts/dev test`: **41 instrumentation tests passed**, zero failures/skips,
  including real-model inference and light/dark UI walkthroughs.
- `./scripts/dev run`: installed successfully; normal MainActivity cold launch
  returned `Status: ok` and displayed the empty notebook.
- Bundled model and vocabulary SHA-256 values match `MODEL.md`.

The pinned Kotlin tooling reports a Java 25 target fallback warning during KSP
configuration with Studio's bundled JDK. The configured application bytecode
remains Java 17 and the full build/test suite passes. The existing KSP opt-out
and deprecated source-set API also produce warnings; these are follow-up
maintenance items, not setup blockers.

## Unified Topics validation — 2026-09-15

Version 8.0.0 / Room schema 7 passes **45 unit tests and 49 emulator tests** on
`NoteTaker_API_36`. Debug APKs build and lint reports zero errors (24 warnings).
The eight new emulator tests cover migration collisions, topic lifetime/growth,
many-to-many assignment, merge conflict handling and lineage, and the complete
browser → merge → Keep/Remove → inherited filter → editor flow. Existing
migration paths 1–6 and light/dark UI walkthroughs pass.

## Sample notebook

With the debug app installed and the dedicated emulator running:

```bash
./scripts/dev seed
```

This adds the 72 short notes in `fixtures/sample-notes.json`: work projects and
todos, fitness, running, shopping, ideas, observations, gardening, and travel.
Several notes cross subjects. No topics are assigned manually; the normal offline
organizer discovers them after launch. Dates span the preceding three weeks.

The command stops the app, backs up its database and journal under `.local/backups/`,
and inserts only missing sample IDs into a verified database copy. Existing notes
are preserved, and repeating the command does not duplicate or overwrite samples.
The original database files are also retained in the app's private files directory.
The script requires Python 3, the debuggable app, and Room schema 7.
