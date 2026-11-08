# FitJournal documentation

Four kinds of document live here, and they are kept apart on purpose. Before
adding a file, ask which one it is.

> **Is it true forever, true on a date, or going to stop being true?**

| Kind | Lifecycle | Where |
|---|---|---|
| **Reference** | Edited in place, always current | `ARCHITECTURE.md`, `SCHEMA.md`, `design-system-web.md`, `design-system-mobile.md` |
| **Decisions** | Written once, never edited | `adr/` |
| **Measurements** | Appended, never edited | `ui-architecture.md` |
| **Plans** | Deleted when done | not tracked here |

## Reference

What is true now. If a rule changes, the file changes; there is no history to
preserve. One file per subject:

- `ARCHITECTURE.md` — the system: clients, API, Lambda, hosting, data flow
- `SCHEMA.md` — the database
- `design-system-web.md` — the React app: tokens, breakpoints, buttons,
  feedback, i18n, layout and accessibility rules
- `design-system-mobile.md` — the Android app

## Decisions

`adr/NNNN-slug.md`, in [Michael Nygard's format](https://clan.lol/docs/26.05/decisions/template):
context, decision, consequences, and when to revisit.

**An ADR is immutable in substance.** It records what was known and chosen on
a date, which is the whole point. When a decision changes, write a new ADR
that supersedes the old one; never revise the reasoning and never merge two
ADRs into one. Housekeeping is fair game: a path that has moved may be
corrected, as happened to 0002 when its plan was harvested. Reference files
link here rather than repeating the reasoning, so a rule is stated once and
argued once.

- `0001` plurals via built-in i18next suffixes, ICU deferred
- `0002` success feedback by evidence, not by uniformity

## Measurements

`ui-architecture.md` holds a frozen baseline and dated progress sections using
the same headings, so columns stay comparable. A number appears there only if
it was measured, and where something is believed fixed but not re-scanned, it
says so.

## Plans

A plan has an expiry date, which is what separates it from everything above.
Keep it local and gitignored while the work runs, then on completion **harvest
and delete it**: durable rules go to the matching reference file, decisions
worth remembering become an ADR, and the rest is history the git log already
holds. A finished plan left in `docs/` is how a documentation folder turns
into an archive nobody trusts.

`docs/FitJournal_Adaptive_UI_Plan_FINAL.md` is the current one. The toast
consistency plan was the first to go through this: its rule became ADR 0002,
its audit table became section 4 of `design-system-web.md`, and the file was
deleted.
