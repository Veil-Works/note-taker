# MVP acceptance checklist

## Automated gates

- [x] Debug source compiles
- [x] Unit tests pass
- [x] Debug lint reports zero errors
- [x] Debug APK builds
- [x] Minified release APK builds (unsigned without private credentials)
- [x] Instrumentation test APK compiles
- [x] Instrumentation tests pass on Pixel_8 (Android 17 / API 37): 4 tests passed on 2026-09-09

## Device acceptance

- [x] Type directly into the floating capture field and complete to save, then verify the field clears
- [x] Scroll the last note fully above the capture overlay with a multiline draft and keyboard open
- [ ] Verify autosave while typing and when immediately leaving the editor
- [ ] Force-stop and reopen; verify saved content remains
- [ ] Reboot the device; verify saved content remains
- [ ] Edit a note without creating a duplicate
- [ ] Search case-insensitively and search literal `%`, `_`, and `\` characters
- [ ] Delete a note, undo it, and verify its content returns
- [ ] Delete a note without undo and verify it remains absent after restart
- [ ] Leave a new empty draft and verify no empty row is created
- [ ] Clear an existing note and verify an “Empty note” row remains
- [ ] Exercise rotation, backgrounding, large fonts, TalkBack, light mode, and dark mode
- [ ] Verify the app works in airplane mode
- [ ] Install and launch a privately signed release APK

Remaining device checks require separate verification; passing the instrumentation tests does not establish all manual acceptance criteria.

## V5 automated acceptance — 2026-09-09

- [x] Case-insensitive tag-name and literal alias matching, including Unicode word boundaries
- [x] Shared aliases suggest multiple existing tags; attached/dismissed tags are excluded
- [x] Add and edit aliases, accept a suggestion, dismiss it, reopen, and reevaluate saved edits in the phone UI
- [x] Alias removal/deduplication and dismissal persistence after database reopen
- [x] Tag-only and alias edits preserve dismissals; saved text changes reset them
- [x] v1/v2/v3 database migrations preserve existing data
- [x] All 18 app instrumentation tests pass on the connected phone; capture tests now use isolated databases
- [x] 27 unit tests pass and debug lint reports zero errors

## Phone feature requests — 2026-09-09

- [x] Delete tags globally with confirmation, including aliases and dismissal records; retain notes and manual links
- [x] Long-press or Select to choose multiple notes; bulk delete with confirmation, add tags, or connect every pair manually
- [x] Shared-tag connections appear alongside manual connections and disappear after the last common tag is removed; Keep linked makes them permanent
- [x] Multiple tag filters require all selected tags and survive state recreation
- [x] Bulk database operations are atomic; failed actions preserve selection for retry
- [x] 30 unit tests and all 22 phone instrumentation tests pass; debug lint completes with zero errors
- [x] Updated debug app installed on CPH2653; original four feature notes restored and verified against backup

## Bulk tag editing follow-up — 2026-09-09

- [x] Load tags from every selected note, including notes outside the current filter
- [x] Show checked tags for all selected notes, indeterminate tags with partial counts, and unchecked tags for none
- [x] Apply additions and removals atomically; preserve untouched partial tags and manual links
- [x] Allow removal-only changes, including removing every tag, without deleting tags from the library
- [x] 30 unit tests and all 24 phone instrumentation tests pass; debug lint completes with zero errors
- [x] Updated app installed in place on CPH2653

## Partial tag suggestions — 2026-09-09

- [x] Case-insensitive word-prefix matching in either direction for tag names and aliases, with a three-letter minimum
- [x] Saving “run 7km thursday” suggests the existing “runs” tag and accepting attaches it
- [x] Interior substrings, short partial words, and partial numeric identifiers do not match; punctuation-bearing terms remain literal
- [x] 31 unit tests and four targeted phone suggestion UI/persistence tests pass; debug build and lint pass
- [x] Updated app installed in place on CPH2653


## Version 6 — Entities and entity types — 2026-09-09

- [x] Upgrade a tag with one optional type and revert it without changing IDs, note associations, aliases, or dismissals
- [x] Rename concepts and manage their aliases; concept pages show all associated notes independently of list filters
- [x] Seed eight editable starter types; add and rename custom types; deleting a type preserves concepts as ordinary tags
- [x] Search tags and entities together and show types in suggestions, note labels, and shared-connection reasons
- [x] Upgrade Bench press to Exercise, add its alias, accept the typed suggestion, and open both old and new notes from its page
- [x] Preserve ambiguous alias candidates for explicit acceptance, and preserve dismissed suggestions across type/name edits
- [x] Migrate v1–v4 schemas to schema 5 without losing notes, tags, links, aliases, or dismissals
- [x] 31 unit tests and all 28 phone instrumentation tests pass; debug build and lint complete with zero errors
- [x] Updated v6 debug app installed in place on the connected phone


## Version 7 — Automatic organization — 2026-09-11

- [x] Save notes immediately and queue organization atomically; resume pending work with WorkManager
- [x] Bundle an offline sentence model and cache unchanged embeddings; reject stale-generation analysis results
- [x] Find related notes by meaning and create phrase-named topic groups without initial tags or aliases
- [x] Attach strong existing-concept matches and learn only from manually attached examples and saved corrections
- [x] Persist removal exclusions across text edits, reopening, and note Undo; Keep promotes automatic associations to manual
- [x] Preserve manual tags and note timestamps through background processing and concurrent text edits
- [x] Rename automatic topics without locking membership; explicit merges preserve the combined membership
- [x] Keep manual controls behind Tags and connections; show automatic organization and semantic related notes in the editor
- [x] Migrate the previous schema in place and queue existing notes; preserve types, aliases, links, and dismissals
- [x] Match the app's keyboard-resize setting in the isolated Compose test host
- [x] 35 unit tests and all 34 phone instrumentation tests pass; build and lint complete with zero errors
- [x] Install v7 in place on the connected phone; verify normal launch and successful background organization of the existing notebook
