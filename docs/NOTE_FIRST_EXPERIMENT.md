# Note-first retrieval experiment

Branch: experiment/note-first-retrieval, based on ba941e3.

The goal is immediate plain-text capture and useful retrieval without requiring
titles, named groups, folders, or a history. Standalone information is a normal
note. Connections provide another way to browse; they do not assign a home to a
note or establish a taxonomy. No summaries, charts, domain-specific rules, new
models, network calls, or dependencies are part of this slice.

## Smallest coherent slice

1. Put a composer directly on the notes screen. Autosave after a short pause;
   reuse one note ID while typing, retrying and restoring saved state. Done or
   leaving the composer saves and starts a fresh thought. Preserve failed text.
   Plain text stays literal, including leading # characters. Keep the existing
   explicit New note dialog as an optional legacy capture path.
2. Keep every completed note in the normal feed whether it has neighbors or not.
   Exclude the active composer note from duplicate display. Preserve existing
   editing, search, filters and optional manual organization.
3. Open a chronological view around an individual note. Include only direct
   semantic neighbors or explicit manual links, never shared-topic membership
   or transitive expansion. Show original text and creation timestamps, oldest
   first by default; permit reversing the order and broadening connections.
4. Run the background organizer in connections-only mode. Recompute derived
   links and retire inferred memberships/topics, preserving manual topics,
   links, aliases and rejection records. Keep the old discovery implementation
   for historical tests/comparison. No database schema change is needed.

## Visual plan

Primary: a compact multiline writing field immediately below the existing Notes
toolbar. Secondary: the existing chronological note feed and a Related action
on notes with closer neighbors. Tertiary: filters and existing optional tools.
Reuse the current blue palette, serif headings, Material typography and shapes,
existing navigation and note surfaces. Use plain separated rows for the related
timeline; do not add generated category cards, dashboards or summary panels.
Support empty/saved/saving/error capture, loading/missing/standalone history,
keyboard insets and long text. Inspect the rendered emulator UI.

## Acceptance and verification

- Save literal standalone text without opening a dialog or providing metadata.
- Autosaves, retries and restoration update one ID; finishing starts another.
- Typing during an in-flight save preserves the latest text. A failed save keeps
  text and an actionable retry; erasing the active thought removes its autosave.
- Creation time survives updates; editing an older note does not reorder history.
- Multiple anchors can share a neighbor, without exclusive grouping.
- A weak A–B–C chain does not pull C into A's direct history. Same-tag membership
  alone cannot create a connection; explicit manual links remain visible.
- Connections-only processing removes old derived groups without removing
  authored notes or manual organization. It remains generation-guarded.
- Run JVM tests, lint, APK builds, relevant emulator tests and independent review.

## Deliberate limits

The app still uses its bundled commercially usable MiniLM model. Laptop Granite
benchmark results are not Android integration results. Closer (cosine >= .60)
and broader (>= .42) are provisional retrieval controls, not probabilities or
validated subject boundaries. Recurring-subject boosts are disabled in the new
worker mode. This experiment tests direct retrieval and interaction, not an
automatic entity/type ontology or complete project membership inference.

The feed retains its existing recently-edited default and optional filters.
The related timeline orders by creation time. It may omit relevant notes when
pairwise similarity is weak. It makes no claim that every project history is
complete or that arbitrary old standalone notes can be retrieved without search.
The existing manual connection control can retain a useful connection.

SavedStateHandle restores an interrupted composer through ordinary Android
saved-state recreation; Room stores autosaved content. A process killed before
either mechanism captures the newest keystrokes cannot be guaranteed lossless.

## Verification — 2026-10-16

| Requirement | Implementation | Executed evidence | Status |
| --- | --- | --- | --- |
| Inline literal capture; stable identity, retry, erasure, restoration | QuickCaptureViewModel / QuickCapture | 6 new JVM cases; light/dark capture flows with Room | PASS |
| Save when backgrounded; fresh composer on return | QuickCapture LifecycleEventEffect | Light/dark lifecycle-owner transitions through ON_STOP with Room assertions | PASS |
| Direct overlapping connections and creation-time order | RelatedHistory / RelatedHistoryScreen | 3 new JVM cases; light/dark closer/broader/reverse-order UI flows | PASS |
| Retire inferred groups; keep authored notes and manual organization | NotebookOrganizer connectionsOnly / OrganizationWorker | Room test seeds old inferred group and preserves manual topic, alias and link after cleanup | PASS |
| Existing capture, navigation, storage and tools | Existing application paths | Full suite before final metadata layout change: 64 passed, 2 skipped; final affected suite: 11 passed, 0 skipped | PASS |
| Build, JVM tests, lint, APKs | scripts/dev check | 84 JVM tests, zero failures/skips; lint zero errors; both debug APKs built | PASS |
| Visual hierarchy and keyboard usability | Composer, feed, related timeline | Actual emulator light/dark screenshots; independent visual review after metadata-row correction | PASS |

Independent source review found no confirmed functional defects. The empty-feed
copy finding was fixed. Independent visual review initially identified excessive
card height; moving Related onto the timestamp row resolved it, and final renders
received PASS. Screenshots use synthetic notes and remain in ignored
.local/note-first-qa. The two ignored full-suite cases are optional historical
hybrid-export and rule-balance audit tools; they are not executed passes.

The first cleanup fixture accidentally allowed the old organizer to reuse a
manual topic, so it did not establish the intended provisional-topic precondition.
The corrected fixture tests that precondition explicitly and passes. The initial
lifecycle UI assertion included placeholder text; the corrected assertion checks
EditableText directly, with persistence and reset assertions retained.

Validation used the dedicated ARM64 API 36 emulator on this Mac. Actual Activity
recreation/process death, API 26, other display sizes, enlarged accessibility text,
and physical-device performance remain unverified. Saved-state restoration has
JVM coverage; lifecycle-event handling has emulator coverage. Product usefulness
and model retrieval quality still require human review.
