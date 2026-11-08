# FitJournal Design System: Web

The one place to look up how the React app is built. Rules that are true now,
edited in place as they change. Where a rule came from a decision with
alternatives worth remembering, it links to the ADR instead of re-arguing it.

Android has its own file, `design-system-mobile.md`. Measured numbers
(bundle size, Lighthouse, axe) live in `ui-architecture.md`.

---

## 1. Tokens

Every value comes from `src/styles/tokens.css`. A literal colour in a component
is a bug: it will not follow the theme.

Light mode is the same token names with light values, under `body.light`.
Components never branch on theme.

| Group | Tokens |
|---|---|
| Type | `--font-body` (Patrick Hand), `--fs` 17px, `--lh` 34px |
| Scale (rem) | `--fs-xs` 12, `--fs-sm` 13, `--fs-base` 14, `--fs-md` 16, `--fs-lg` 18, `--fs-xl` 20, `--fs-2xl` 24, `--fs-3xl` 36, `--fs-display` 56 |
| Paper | `--bg`, `--line-color`, `--border`, `--hole-core/-mid/-soft` |
| Ink | `--text`, `--muted` |
| Controls | `--control-bg`, `--control-bg-hover`, `--control-border`, `--control-chevron` |
| Structure | `--divider` (heavier than `--border`), `--radius` 10px |
| Accent | `--red`, `--red-border`, `--margin`, `--active-bg`, `--yellow` |
| State | `--success`, `--error`, `--pending-bg` (yellow wash: planned, not done) |
| Space | `--space-xs` 4, `--space-sm` 8, `--space-md` 16, `--space-lg` 24 |

**The `--lh` trap.** `.app-content` sets `line-height: var(--lh)`, 34px, and it
inherits. Anything that sizes itself from its line box gets 34px unless it says
otherwise. This has caused three separate bugs: oversized headings, a
strikethrough that sat below the text, and buttons whose padding looked like it
set their height when it did not. Any component with a deliberate height
declares its own `line-height`.

## 2. Breakpoints

Mobile first. Base rules are the narrow layout; wider tiers add to them. There
is no `max-width` query in the app.

| Query | Count | Meaning |
|---|---|---|
| `min-width: 600px` | 16 | The main tier. Narrow below it, roomy above. |
| `min-width: 760px` | 5 | Only where content needs it: the manual log's two columns, which cannot hold a muscle-chip list and three number inputs in half the width at 600. |

Read a breakpoint in JS with `useMediaQuery` (built on `useSyncExternalStore`,
because the repo's ESLint forbids setState in an effect). Pass the complement
of the CSS tier, `(max-width: 599.98px)`, so the two cannot disagree.

CSS can restyle a control but cannot change what it is: a `<select>` and a chip
group are different widgets, so a swap between them is a JS decision, not a
media query.

## 3. Buttons

One family, `src/styles/buttons.css`. Named by intent, not by colour.

| Class | Look | Use |
|---|---|---|
| `.btn` | transparent, thin border | neutral action |
| `.btn--primary` | solid yellow | the affirmative action of the view |
| `.btn--secondary` | yellow outline | affirmative, not the main one |
| `.btn--negative` | red outline, muted ink | dismissive or destructive |
| `.btn-remove` | bare red glyph, 20px | drop a row from a list or table |

Geometry is declared, not inherited: `--fs-md` label, `line-height: 1.2`,
`min-height: 44px` for the touch target, `padding: 8px 20px`, 6px radius.

Three sizes exist and no more. Actions use `.btn` at 16px. Chips and toggles
are 14px with an 8px radius. Micro buttons inside dense rows are 12px with a
6px radius and a 24px minimum target.

**Yellow means affirmative. Red means remove, dismiss or warn, and nothing
else.** Adding is affirmative, so an add button is yellow even when it is
small. `--ghost`, a dashed red "add another", was deleted for teaching the
opposite.

The `Button` component renders these classes and nothing else. It used to
carry inline styles, which outranked the stylesheet and kept nine buttons
outside the scale.

## 4. Feedback

Decided in [ADR 0002](adr/0002-success-feedback.md).

> Show a success toast when nothing on screen would otherwise prove the change
> persisted. Stay silent when the result is already visible, or when the
> destination the user lands on reflects the change.

Three legitimate kinds: **optimistic with rollback** (the visible value is the
confirmation; `Exercises.jsx` weight editing is the reference), **navigation as
proof** (valid only when the destination reflects the change), and **toast
required** (something persisted and nothing shows it).

Toast mechanics, from `ToastContext.jsx`: the caller passes an already
translated string, `showToast(message, type, duration)` defaults to 3000ms,
one at a time, `role="status"` so it is announced without stealing focus.
Content reports what happened, with a count where there is one. "Logged 5
exercises for Day 2", not "Well done".

Current state of every mutating action:

| Action | Page | Feedback |
|---|---|---|
| Log generated workout | Workout | Toast with count and day, returns to the chooser |
| Log manual workout | Workout | Toast with count, returns to the chooser |
| Save routine | Routine | Toast |
| Save profile | Profile | Toast |
| Add exercise | Exercises | Toast (the modal closes, so the row alone is ambiguous) |
| Delete exercise | Exercises | Toast (a row vanishing could be optimistic) |
| Delete account | Profile | Toast, then navigate to login |
| Generate next-workout selection | Calendar | Toast with a count |
| Clear selection | Calendar | Toast |
| Register / Forgot password / Resend code | auth | Toast |
| Reset password | Reset | Toast, outlives the route change to the login page |
| Edit exercise weight | Exercises | Silent, optimistic with rollback |
| Toggle a day's selection | Calendar | Silent, the checkbox is the feedback |
| Change language / units | Profile | Silent, the interface itself changes |
| Set current day | Workout | Silent, the day strip highlight moves |
| Delete a routine day | Routine | Silent, the row disappears |
| Login / Verify email | auth | Silent, the dashboard proves it |

**Validation is inline, never a toast.** A toast expires while the user is
still reading the field it complained about. Validation sits next to the
action (`.wo-error`, `.rt-error`), stays until the input changes, carries
`role="alert"`, and marks the offending fields with `aria-invalid` plus a
visible cue. Never colour alone (WCAG 1.4.1): the message says the same thing
in words.

**Questions** come in two shapes, both themed. A row-level delete swaps its
trigger for the answer in place (`.ex-confirm`). Anything whose trigger cannot
hold an answer, or whose consequence needs a sentence, uses `ConfirmModal`.
A reversible action with nothing to lose asks nothing at all. `no-alert` is an
ESLint error, so `alert`, `confirm` and `prompt` cannot come back.

## 5. Internationalization

Five locales: en, es-419, es-AR, nl, fr. `es-AR` is a partial overlay on
`es-419`, so it holds only the keys that differ.

New strings go into `en` only. `fr` and `nl` arrive through the Stringherd
sync; Spanish is maintained by hand. Adding a translation by hand defeats the
point of testing the pipeline.

**Plurals** use i18next's built-in suffixes, `_one` and `_other`, resolved
through `Intl.PluralRules`. ICU MessageFormat is deferred: its braces would
have to survive machine translation. See [ADR 0001](adr/0001-plurals-builtin-suffixes.md).

**The unit of translation is the sentence, not the word.** Do not build a
sentence from fragments to avoid duplication: translation is the exception to
DRY, because word order, agreement and article forms differ per language.

**Lists** go through `formatList` in `src/i18n/format.js`, which wraps
`Intl.ListFormat`. Pass `type: "unit"` for a set of things and
`"conjunction"` for a sentence that reads as prose. In fr, es and nl the two
are identical; the difference shows in en and ar, so the type declares intent
rather than punctuation.

**Units** read their symbol from `Intl.NumberFormat` with `style: "unit"` via
`formatToParts`, so French gets `pi` and `po` rather than hardcoded `ft` and
`in`.

**`aria-label` is speech, not layout.** An em dash has no spoken form, so
labels get words.

**RTL** uses logical properties throughout: `padding-inline`, `margin-inline-start`,
`border-inline-start`, `inset-inline-end`. A glyph that points somewhere gets
`.mirror-rtl`. Test with the dev l10n panel, which also offers `en-XA`
pseudo-localization for finding hardcoded strings and tight layouts.

**Coverage is not correctness.** French was 98% covered and held eight
meaning-level defects, six of them plural forms and five of those in the
singular. A key that exists says nothing about whether it says the right thing.

## 6. Layout and accessibility notes

**The shell owns the viewport.** `.app-shell` is `height: 100dvh` with
`overflow: hidden`, and `.app-content` is the scroller. `dvh` rather than `vh`
so collapsing mobile browser chrome does not clip the page.

**`min-height: 0` and `min-width: 0` are load-bearing.** A flex or grid item
defaults to its content size and refuses to shrink, so `overflow` silently
does nothing. Every link in a chain that is supposed to scroll internally
needs it. This is why the dashboard grid overflowed by 3px and why the manual
log's panes would not scroll.

**Utilities:** `.sr-only` for text that should be spoken but not seen,
`.mirror-rtl` for direction-sensitive glyphs.

Selection state never relies on hue alone (WCAG 1.4.1). The bottom nav marks
the current item with weight and an underline, not colour. `NavLink` already
emits `aria-current="page"`.

Interactive targets clear 24px minimum, and standalone buttons clear 44px.
