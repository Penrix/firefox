# Private reading implementation — 2026-11-04

Owner scope: TWKAN and Cool18 only; rule-based cleanup and simplified Chinese. No typo correction or generated missing text.

Baseline: PR 1 `c6d5c0257a355756431630441512d291520dd1bd`; K20 `944b5584`, Android 9. Independent application ID `org.mozilla.penrix.private`.

Implemented locally: bundled content extension, migrated TWKAN span rules, OpenCC JS 1.4.2 Taiwan-phrase conversion, page simplify/restore, native reader bridge, local chapter/library/progress store and home continue-reading. These are not yet Android-compiled or device-verified.

Checks: 7 Node tests passed, covering observed Unicode promotions, dialogue/name/whitespace preservation, standalone site watermarks, region phrases, extraction before mutation, dynamic text conversion/restoration and unchanged input fields. JSDOM is a development-only dependency; no browser runtime dependency. A single read-only implementation review found stale home library state; changed it to refresh on ON_RESUME. No further concrete blockers found in that code review.

Build: official MozillaBuild installed on Windows. Initial Git configured a dead localhost proxy; direct large transfers failed. Reused the existing K20 Clash proxy via temporary ADB forward `tcp:18790 → tcp:7890`, then fetched the exact feature branch. Sparse source omitted configure dependencies; full lazy blob fetch was excessively slow. Downloaded the exact same commit's complete GitHub source archive (1,074,479,436 bytes), restoring missing files while preserving implementation edits. SDK/JDK bootstrap completed. Configure's Git diff must run after extraction/index refresh to avoid triggering partial-clone fetches.

Device inspection: installed browser is old app.penrix.browser, not official Firefox. An ADB ACTION_VIEW command to open TWKAN was rejected by automatic approval with only `blocked by policy`; no technical reason supplied. Normal launcher/UI operations work. Use normal UI navigation for actual site testing after installing the new APK. Do not repackage the rejected navigation in another command.

Ruling: Keep existing registry entries for route compatibility, but show only TWKAN/Cool18 on the private homepage. Do not remove unrelated source support to satisfy homepage scope.

Preserve: official Firefox and old Penrix installation/data, AutoJs6 schedules, physical display resolution. Never upload account/session captures.

Next: finish artifact bootstrap, compile independent debug APK, exercise real TWKAN chapter navigation/reading/progress on K20, record actual outcome here.
