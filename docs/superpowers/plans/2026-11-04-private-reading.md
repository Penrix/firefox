# Private reading implementation plan

> Execute inline with superpowers:executing-plans. The owner approved the extension/native split in this chat and requested implementation without further design rounds.

**Goal:** Install the independent Firefox fork on K20 and read TWKAN with inline promotions removed, simplified Chinese, chapter navigation and persistent progress. Cool18 follows using the same reader.

**Architecture:** Bundle one content extension using OpenCC and the existing TWKAN rules. Android owns library/reader/progress; Firefox owns tabs, requests and sessions. Retain source text before cleaning. No typo repair or generated text.

**Baseline:** Penrix/firefox PR 1, c6d5c0257a355756431630441512d291520dd1bd. Existing shell has not been compiled. Preserve official Firefox, old app.penrix.browser data, AutoJs6 schedules and physical resolution.

## Tasks

- [ ] Build the existing isolated debug variant using Mozilla artifact mode. Fix actual compile errors only.
- [ ] Add `assets/extensions/penrix/text.js`: `cleanTwkan(raw)` returns original/clean text and removed ranges; `simplify(text)` uses bundled OpenCC. Replay observed inline watermarks and normal prose with Node tests.
- [ ] Add the extension manifest/content handler for TWKAN chapters and Cool18 posts; capture source before DOM changes. Provide webpage simplify/restore controls, emit chapter data over the existing Android Components built-in extension API.
- [ ] Add `PenrixReadingFeature` and `PenrixReaderActivity`: load extracted content, previous/next/catalog, save work/chapter/scroll in app-private storage and expose continue reading from the existing home. Register in Core and Android manifest.
- [ ] Limit first-class modules to TWKAN and Cool18. Keep ordinary Firefox browsing available.
- [ ] Build/install on serial 944b5584; exercise real TWKAN content, next chapter, return and reopen progress. Record the actual result and remaining failures. Do not treat parser tests as device acceptance.

## Current reality

- Mozilla supports bundled WebExtensions and Native Messaging; Android Components `BuiltInWebExtensionController` is used by its existing ReaderViewFeature.
- Old Kotlin cleaner uses detection-only Unicode folding and maps matched promotions back to original offsets. Preserve this behavior, including normal dialogue and names.
- Current TWKAN sample `/txt/88637/50790315` contains a stylized domain and promotion suffix `輕鬆讀`; include it in the migrated rules.
- OpenCC JS supports offline browser conversion with bundled dictionaries. Pin a stable package and include its license.
- K20 connected; no MozillaBuild environment initially. Installed official MozillaBuild. Git command-local proxy clearing required; do not change global proxy settings.
