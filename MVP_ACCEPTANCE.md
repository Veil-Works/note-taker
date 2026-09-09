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
