# UI polish audit — 12 September 2026

Tested on the connected CPH2653 phone running Android 16 (API 36), at 1440 × 3168 and 640 dpi. The updated debug APK is installed in place. Tests use disposable databases; the existing notebook is preserved.

## Changes

- Editor title and Done/Delete actions stay visible while its contents scroll. Capture keeps Close/Save visible above a docked keyboard, including long notes.
- Consistent opaque glass panels and readable theme colors across confirmation, tag, type, keyword, connection, and topic dialogs.
- Short fades between main screens, animated navigation tint, and a gentle editor size transition when revealing more controls.
- Enlarged text no longer splits the Notes heading or the Connections navigation label. Appearance choices and concept actions wrap to fit.
- Long concept/topic names remain readable in the detail page even when the toolbar title is shortened.
- Date-range headings use a compact From/To layout with localized dates. Counts use singular/plural forms correctly.
- Graph search has an explicit no-results state and accommodates the keyboard. Graph labels follow font scaling; the strength value includes a percent sign.
- Landscape uses a compact graph header and navigation bar, leaving substantially more room for the graph.
- The whole Compact notes row is tappable. System navigation icon appearance follows the selected theme.

## Verification

- 45 unit tests passed.
- 41 instrumented tests passed on the phone after the final code changes.
- Build and Android lint passed, with no lint errors. Remaining lint warnings concern dependency/SDK updates and existing style recommendations.
- Both additional system-font walkthrough tests passed at 150% after the final build.
- The visual walkthrough covers 27 states in each of light and dark themes, at normal and 150% system font size. See the [screenshot gallery](index.html).
- Coverage includes the list, search, selection, bulk tagging and deletion confirmation, filters and custom dates, editor, related notes, manual controls, keyword and link dialogs, graph and its browser, concepts and types, topic details/rename/merge, settings, an organization error, long capture with keyboard, and empty states.
- Manual checks include repeated tab changes, search transitions, scrolling, and landscape list/graph/capture.

## Motion and scope

In a short warmed debug-build interaction sample, Android reported 377 frames, an 8 ms median, 10 ms 90th percentile, and 6 missed frame deadlines (1.59%). This supports responsive typical interaction, but does not establish stutter-free performance or replace a release-build benchmark. Direct video recording was unavailable on this phone, so motion was checked through interactions, resulting screens, and Android frame statistics. The native screen-recording helper stalled without producing video, and Android denied both interrupt and kill signals over ADB. It remains a cleanup item for the next phone restart; the phone was not restarted during the active call.

The phone's floating landscape keyboard can overlap a dialog because it does not reserve layout space; hiding or moving that keyboard exposes the controls. Docked portrait keyboard layouts were checked at both text sizes. This audit covers this phone and these configurations, not every Android device or a full TalkBack audit.

Original font size and portrait rotation are restored after testing.
