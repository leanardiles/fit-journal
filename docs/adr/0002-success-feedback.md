# ADR 0002: Success feedback by evidence, not by uniformity

**Status:** Accepted, November 2026
**Context item:** the toast consistency plan, since harvested into
`design-system-web.md` section 4 and deleted

## Context

Success feedback grew page by page. By late 2026 six pages fired success toasts,
one page (Workout) marked success with a dedicated screen, and several actions
said nothing at all. The inconsistency was visible, but "be consistent" does not
by itself say what to do, because the silent cases were not all the same kind of
silence.

An audit of all 21 mutating actions, now kept current in
`design-system-web.md` section 4, found three distinct situations:

1. The UI already shows the outcome, because the write was optimistic and a
   failure reverts it. `Exercises.jsx` weight editing is the reference: it writes
   to state immediately, and the `catch` restores the previous value and shows an
   error toast.
2. The user is navigated somewhere that demonstrates the outcome. Logging in and
   landing on the dashboard.
3. Something persisted and nothing on screen reflects it. Saving a routine,
   saving a profile, logging a workout.

## Decision

> Show a success toast when nothing on screen would otherwise prove the change
> persisted. Stay silent when the result is already visible, or when the
> destination the user lands on reflects the change.

Category 3 gets a toast. Categories 1 and 2 do not.

Category 2 carries a condition that is easy to get wrong, and did: the
destination has to actually reflect the change. Resetting a password navigated to
the login page, which says nothing about the password, so it looked like category
2 and was really category 3. That was the only such case in the audit, and it is
now a toast.

The rejected alternative was uniformity in either direction. Toasting every
success means confirming a language switch that just changed every word on
screen, and confirming a weight edit whose new value is already on the row.
Giving every flow a success screen means an interstitial on actions performed
daily. The Workout success screen was that pattern, and it was the one page the
rule removed something from.

Toast content follows `calendar.generated`: report what happened, with a count
where there is one, rather than praise. "Logged 5 exercises for Day 2" is
checkable. "Workout complete! Well done." is not.

## Consequences

Good:

1. The rule is decidable from the code: look at what the user sees after the
   action, not at what other pages happen to do.
2. No interstitials on daily actions.
3. Silence stops reading as an oversight, because it is now a documented
   category with a named reason.

Bad:

1. The rule needs judgement, so the audit table has to stay current as pages are
   added, or the next contributor re-litigates it per page.
2. An optimistic update that happens off-screen is invisible to everyone,
   including screen reader users, and the rule says stay silent. No case in the
   app does this today; one would need its own answer.
3. The toast is the only confirmation for category 3 now that the Workout screen
   is gone, so it inherits the placement problem below.

## Revisit when

1. **The toast proves missable.** It renders bottom-right, `z-index: 1000`, for 3
   to 4.5 seconds. On a wide desktop that corner is far from where the eye is,
   and it fires at the same moment the screen changes. It can also sit under a
   dev overlay. If it turns out to be missed in normal use, the answer is
   placement, bottom-centre on desktop, and it would change app-wide rather than
   per page.
2. **A flow earns a summary screen.** Strong and Hevy end a session with
   duration, volume and personal records. That is a destination worth stopping
   on, because it shows something. A reassurance is not, which is why this ADR
   removed one. If FitJournal ever computes those numbers, the decision here does
   not forbid a screen that carries them.
3. **Undo arrives.** Tracked in ROADMAP. A toast is where an Undo action lives,
   and that would make the toast load-bearing rather than merely informative,
   which raises the bar on it being seen.
