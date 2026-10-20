# FitJournal UI architecture

Living notes on the adaptable and scalable UI revamp: goals, responsive versus adaptive definitions, breakpoints, tokens, components, i18n and accessibility rules, testing and localization links, trade-offs, and known gaps. Filled in as the work lands (plan section 8.1).

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
