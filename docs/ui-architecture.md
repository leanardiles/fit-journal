# FitJournal UI architecture

Living notes on the adaptable and scalable UI revamp: goals, responsive versus adaptive definitions, breakpoints, tokens, components, i18n and accessibility rules, testing and localization links, trade-offs, and known gaps. Filled in as the work lands (plan section 8.1).

## How to read this file

`Baseline` below is a frozen snapshot from 2026-10-20, before any revamp work. It is never edited. Each later pass appends a dated `Progress` section using the same headings, so columns stay comparable over time.

A number is only recorded here if it was measured. Where something is believed fixed but has not been re-scanned, it says so.

---

## Progress, 2026-10-21

### Headline

| Metric | Baseline 2026-10-20 | Now 2026-10-21 |
|---|---|---|
| en source keys | 252 | 256 |
| String assemblies in JSX (`` `${t(...)}` ``) | 20 | **0** |
| Hardcoded user-visible attributes | 3 | **0** |
| Media-query breakpoints in use | 480, 520, 760, 1024 | **600**, plus one 760 holdout |
| ESLint problems | 32 | **8** |
| JS, gzipped | 131.31 kB | 132.34 kB (+1.03) |
| CSS, gzipped | 6.16 kB | 6.91 kB (+0.75) |
| Largest single asset | 408 kB PNG | **59 kB WebP** (-85%) |

The cost line is the one worth remembering: the entire responsive and adaptive layer, a bottom tab bar with six drawn icons, a dev-only pseudo-locale, a dropdown identity, a shared button system and a locale-aware list formatter came to **under 2 kB gzipped**. The single largest win was unrelated to any of it: one wordmark PNG, 1205 by 363 but never rendered wider than 200 CSS pixels, resized and converted.

That is the lesson, not the byte count. Measuring changed the diagnosis. The problem was oversized dimensions, not the file format, and it was found by asking what the asset was actually used for rather than by looking at the build output alone.

### The four accessibility root causes

The baseline's 35 axe findings collapsed to four causes. Status, verified in source:

| Cause | Status | How |
|---|---|---|
| Logout-button contrast, all six screens | Fixed | `--muted` raised from `#555555` (2.4:1) to `#8a8a8a` dark and `#6b6b6b` light, both above 4.5:1 |
| Calendar, 24 cell buttons with no name | Fixed | The button renders empty when unselected, so it had no accessible name at all; `calendar.selectFor` now names each one with its exercise |
| Profile, 4 form controls with no name | Fixed | Each input carries an `aria-label`. Note this diverges from the baseline's proposal of adopting the shared Field and Select components, which remains the better long-term fix (plan 3.2) |
| Routine `.rt-cols` not keyboard reachable | **Open** | Still needs `tabindex`, `role` and a label on the scroll container |

**Not re-measured.** Lighthouse and axe have not been re-run since 2026-10-20. Three of the four causes are fixed in source but unconfirmed by scan, and the Calendar number is the one to watch: if the 24 criticals drop to zero, that confirms the diagnosis. Re-running both is the next measurement, not a formality.

### Internationalization

All three defect classes the baseline named are now closed.

Sentence fragments assembled in JSX are gone. Five day headings built as a translated fragment plus an em dash plus a name became two keys carrying the whole sentence, so the separator and the word order belong to the translator. Thirteen `aria-label`s built the same way became nine spoken-phrase keys: an `aria-label` is heard rather than read, so `Sets — Bench press` was wrong twice, and it now reads `Sets for Bench press`. Six hardcoded English network errors on the auth pages moved into a new `errors.*` namespace, which also seeds the one plan 1.7 will fill.

Six `.join(", ")` calls became `formatList`, wrapping `Intl.ListFormat`. The comma and space were an English convention the code had no business deciding; Spanish, French and Dutch add a word before the final item and Arabic uses a different comma entirely.

Unit symbols now come from `Intl.NumberFormat` rather than hardcoded strings, so French renders `pi` and `po` for feet and inches without a translation key existing for either.

### What coverage did not measure

The baseline recorded French at 98.0% coverage. That number was accurate and nearly meaningless, because **coverage measures presence, not correctness.**

An audit of the four plural keys found every one wrong in French and none wrong in Spanish or Dutch. One rendered "3 sets" as "3 définit", reading the English noun as a verb. Checking the muscle names afterwards found `Back` as "Retour" (go back) and `Calves` as "Veaux" (baby cows). Eight defects, all in the only locale routed through machine translation, all in strings short enough to carry no disambiguating context, and five of the six plural errors in the singular form specifically, where a bare count plus a noun reads as a label rather than a quantity.

None of this was visible to a coverage metric, and none of it would have been caught by the key-check script in plan 4.1 either, which compares structure rather than meaning. It took reading the strings. That is the argument for human review of short strings, and the limit of automation stated honestly.

Full write-up in the local case-study notes.

### Layout and breakpoints

The shell is mobile-first. Below 600 px the margin rule and punch holes drop and navigation moves to a bottom tab bar of six icon-only links, each with a translated `aria-label`, which is itself the localization case for icons: a glyph is the same width in every language while a label expands up to 100%.

Selection in that bar is signalled by stroke weight, contrast and an underline rather than hue, because colour alone fails WCAG 1.4.1.

Breakpoint consolidation is nearly done: 11 media queries now use 600 px, against the baseline's spread of 480, 520, 760 and 1024. One 760 px query remains in `Workout.css`.

Two defects worth recording because the cause is reusable. A cell spanning a whole table via `colSpan` can never be pinned, because a sticky box is never offset beyond its containing block. And a percentage in `background-position` is an alignment, not an offset, so it resolves to zero when the image matches the box width, which is why `calc(100% - 90px)` silently placed the RTL margin rule off-screen.

---

## Baseline

Captured 2026-10-20 on branch `adaptable-scalable-ui-revamp`, before any revamp work. Screenshots of the key screens at 360, 768 and 1280 px in en and fr are under `docs/baseline/`.

### Internationalization

Source (en) keys: 252, across the frontend `common` namespace and the backend `common` and `errors` namespaces. Coverage against the 252 source keys:

| Locale | Coverage |
|--------|----------|
| en     | 100.0% (252/252), source |
| es-419 | 99.6% (251/252) |
| fr     | 98.0% (247/252) |
| nl     | 97.6% (246/252) |
| es-AR  | 14.7% (37/252), thin voseo override by design |

The fr and nl gaps are the five backend `common` keys that exist only for en and es-419. Authoritative key extraction and missing-key detection come later via i18next-parser (plan 4.1).

i18n discipline is already high: 315 `t()` call sites in JSX and zero hardcoded literal text nodes. The grep flagged 20 `t()` concatenations, but all join a translated label to dynamic data (a unit, a proper noun, or a decorative emoji), not sentence fragments split across keys, so the word-order bug class (1.2) is not present. Optional polish: fold the glue punctuation (the parenthetical and dash separators) into the keys so translators control it. The 3 hardcoded attributes are unit placeholders (`cm`/`lb`/`kg`) on the Profile height and weight inputs, which sit on the same controls flagged for missing labels, so adopting the shared Field component fixes both.

### CSS and layout

Fixed, non-adaptive values remaining in `web-react/src`:

- Hardcoded `font-size` in px: about 84 in app code (93 counting the `src/stories/` Storybook scaffold, which is not shipped). The distinct values form a type scale (11, 12, 13, 14, 15, 16, 18, 20, 22, 24, 32, 36, 56); 14px is the de-facto body size although the `--fs` token is 17px. Plan 3.1 turns these into rem tokens and rationalizes the scale.
- Fixed px widths: 49 matches, but 6 are media-query breakpoints (480, 520, 760, 1024) and 2 are stories scaffold. Most real element widths are fine (small control and icon sizes, `max-width` caps, a `min-width: 44px` touch target). Actionable: `AuthLayout` `width: 420px` (overflows at 360, make it a `max-width`) and the Calendar frozen first column pinned at 180px (needs a compact value below the small breakpoint, plan 2.3).
- Physical-direction properties: effectively zero in app code. The only hit is `StickyNote` `left: 50%`, an RTL-safe centering offset; the 5 `margin`/`padding-left|right` the grep found are all in the stories scaffold.

Logical CSS is already the norm in app code (28 `margin`/`padding-inline`, 5 `inset-inline`, 4 `text-align: start|end`), so the 1.6 logical-property migration is essentially done. Adding an RTL language later is mostly a font and `dir` job. Breakpoints in use are 480, 520, 760 and 1024; plan 2.2 consolidates them to 600 and 1024.

### Bundle (production build, 2026-10-20)

Vite production build (`npm run build`):

| Asset | Raw | Gzip |
|-------|-----|------|
| JS (index chunk) | 440.05 kB | 131.31 kB |
| CSS (index) | 30.30 kB | 6.16 kB |
| index.html | 0.52 kB | 0.31 kB |
| logo PNG | 408.02 kB | n/a (image) |

Code payload is about 137 kB gzipped (JS plus CSS) in a single chunk. The largest single asset is the 408 kB logo PNG, the obvious first optimization target (resize or recompress), which is separate from the JS budget. This sets the reference for the bundle budget in 4.4, for example a gzip ceiling a little above 131 kB on the main chunk, and suggests route-level code splitting as a later option.

### Lighthouse (2026-10-20)

Scores out of 100, measured on the production site (fit-journal.com), 2026-10-20:

| Screen | Performance | Accessibility |
|--------|-------------|---------------|
| Dashboard | 98 | 95 |
| Profile | 99 | 84 |
| Routine | 98 | 95 |
| Exercises | 99 | 96 |
| Calendar | 99 | 88 |
| Workout | 98 | 95 |

Performance is strong across the board, so the revamp's job there is to preserve it (budget, 4.4). Accessibility is the signal: Profile (84) and Calendar (88) trail the rest and are the priority screens for the a11y work. The Lighthouse accessibility score is automated-only, so it is a floor, not proof of full accessibility.

### axe DevTools (2026-10-20, en, production)

Automated scan per screen, counts by impact:

| Screen | Critical | Serious | What |
|--------|----------|---------|------|
| Dashboard | 0 | 1 | logout-button contrast |
| Profile | 4 | 1 | 2 inputs with no label, 2 selects with no accessible name; plus logout contrast |
| Routine | 0 | 2 | logout contrast; `.rt-cols` scrollable region not keyboard-focusable |
| Exercises | 0 | 1 | logout-button contrast |
| Calendar | 24 | 1 | 24 cell buttons with no discernible text; plus logout contrast |
| Workout | 0 | 1 | logout-button contrast |
| Total | 28 | 7 | |

The 35 findings collapse to four root fixes:

1. Logout-button contrast (serious, all six screens). `.app-logout-btn` uses the `--muted` token (`#555555`) on the dark background at 2.4:1 against a 4.5:1 requirement. One token or color change in the shared AppLayout header clears it everywhere. Plan 3.1.
2. Calendar cell buttons (24 critical). The tap-to-select cells are buttons with no accessible name. One `aria-label` pattern on the cell component clears all 24. Workout already builds exactly this kind of cell aria-label (Workout.jsx around lines 467, 480, 493), so copy that pattern. Plan 2.3.
3. Profile form controls (4 critical). Raw `.pf-input` inputs and inline-styled selects with no label association, because Profile bypasses the shared Field and Select components. Adopting those components fixes them. Plan 3.2.
4. Routine scrollable region (serious). The horizontally scrolling `.rt-cols` is not keyboard-reachable; make the container focusable. The rebuilt Calendar grid will need the same. Plan 2.3 and 2.5.

This explains the Lighthouse accessibility dips exactly: Profile (84) carries the four form-label criticals, Calendar (88) the 24 unnamed buttons.
