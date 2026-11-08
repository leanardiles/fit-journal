# Toast consistency plan

Status: proposed, not yet started
Branch: `toast-consistency-work`
Written: 2026-11-05

## Why this exists

Success feedback grew page by page. Six pages fire success toasts, one page uses a
dedicated success screen, and several actions say nothing at all. Some of that
silence is correct and some of it is a gap, so this document writes down the rule
that decides which, audits every mutating action against it, and sequences the work.

## The rule

> Show a success toast when nothing on screen would otherwise prove the change
> persisted. Stay silent when the result is already visible, or when the
> destination the user lands on reflects the change.

Three legitimate kinds of success feedback follow from it:

1. **Optimistic with rollback.** The UI updates immediately; on failure it reverts
   and shows an error toast. The visible value is the confirmation, so a success
   toast would be noise. `Exercises.jsx` weight editing is the reference
   implementation: it writes to state, and the `catch` restores the previous value
   and toasts.
2. **Navigation as proof.** The destination demonstrates the outcome, so no toast is
   needed. Valid only when the destination actually reflects the change. Logging in
   and landing on the dashboard qualifies; resetting a password and landing on the
   login page does not.
3. **Toast required.** The action persisted something and nothing on screen shows
   it. Saving a routine, saving a profile, logging a workout.

## Audit

Current state of every mutating action, and what this plan proposes for it.

| Action | Page | Current success feedback | Category | Suggestion |
|---|---|---|---|---|
| Log generated workout | Workout | Dedicated success screen (`mode === "success"`) | 3 | **Change.** Toast naming what was logged, refetch state, return to the generate/manual chooser. Delete the success mode. |
| Log manual workout | Workout | Dedicated success screen | 3 | **Change.** Same treatment, same toast. |
| Reset password | Reset | Silent, navigates to login | 2, broken | **Change.** The login page says nothing about the password. Add a success toast. |
| Set current day | Workout | `window.confirm`, then the day strip highlight moves | 1 | Keep. `selectDay` already refetches state, so the highlight is real feedback. |
| Save routine | Routine | Toast `routine.saved` | 3 | Keep. |
| Delete a routine day | Routine | `window.confirm`, the row disappears | 1 | Keep the silence. See "adjacent" on the confirm dialog. |
| Add exercise | Exercises | Toast `exercises.added` | 3 | Keep. The modal closes, so the new row alone is ambiguous. |
| Delete exercise | Exercises | Toast `exercises.deleted` | 3 | Keep. A row vanishing could be optimistic; the toast confirms the server agreed. |
| Edit exercise weight | Exercises | Silent, value updates in place | 1 | Keep. Best-built interaction in the app; do not add a toast. |
| Save profile | Profile | Toast `profile.saved` | 3 | Keep. |
| Change language | Profile | Silent, the interface changes language | 1 | Keep. The strongest possible confirmation. |
| Change units | Profile | Silent, values re-render | 1 | Keep. |
| Delete account | Profile | Toast, then navigate to login | 3 | Keep. |
| Generate next-workout selection | Calendar | Toast `calendar.generated` with a count | 3 | Keep. The count is the model for workout toasts: it reports what happened. |
| Clear selection | Calendar | Toast `calendar.cleared` | 3 | Keep. |
| Toggle a day's selection | Calendar | Silent, the checkbox is the feedback | 1 | Keep. |
| Register | Register | Toast `register.success` | 3 | Keep. |
| Forgot password | Forgot | Toast `forgot.sent` | 3 | Keep. |
| Resend verify code | Verify | Toast `verify.resendSent` | 3 | Keep. |
| Verify email | Verify | Silent, navigates to dashboard | 2 | Keep. The dashboard proves it. |
| Login | Login | Silent, navigates to dashboard | 2 | Keep. |

Two actions out of twenty-one need changing. The app is more consistent than it
looks; the exception is concentrated in one page.

## Toast mechanics worth keeping consistent

Established by `ToastContext.jsx` and `Toast.jsx`, and worth not drifting from:

- The caller passes an already-translated string. The component holds no text, so
  messages stay in the i18n catalogs.
- `showToast(message, type, duration)`, default 3000ms. Pass a longer duration for a
  message carrying content worth reading, such as a count.
- One toast at a time. The provider clears the pending timer, so a new toast cannot
  be cut short by the previous one's expiry.
- `role="status"` on the toast element, so it is announced to screen readers
  without stealing focus. Any replacement for a success screen must keep this,
  because a removed screen takes its announcement with it.
- `--success` is the one green. `.wo-success-msg` in `Workout.css` carries a second,
  hardcoded green and goes away with the success mode.

## Plan of work

### Phase 1: Workout (the reason this started)

The only page out of line, and the one with a user-visible payoff.

1. Toast on both log paths. Report what happened rather than praise, following
   `calendar.generated`: a count of exercises and the day. New key, five locales.
2. Refetch `/workout/state` before returning to the chooser. **This is load-bearing**:
   completing a workout advances `state.current_day_number` server-side in
   `cleanup_after_workout`, and `Workout.jsx` reads `currentDay` from a fetch that
   runs only on mount. Today the success screen's link to the dashboard unmounts the
   page, which is what hides the staleness. Return to the chooser without a refetch
   and the day strip highlights the day just completed, and Generate WOD regenerates
   it. `selectDay` already refetches this way; copy that.
3. Return to `mode === "generate"` via `goGenerate()`, which already clears
   `workout`, `prompt` and `gapNote`.
4. Delete `mode === "success"`, the `.wo-success` and `.wo-success-msg` rules, and
   `workout.successMsg` plus `workout.backToDashboard` from all five locales.
5. Accept knowingly: staying on the page puts a second log one tap away. Because the
   cursor has advanced, a second Generate WOD produces the next day's workout rather
   than a duplicate, which is a legitimate thing to want.

### Phase 2: Reset password

One `showToast` call and one new string. Closes the only case where navigation is
used as proof but proves nothing: the login page says nothing about the password
having changed. The toast survives the route change because `ToastProvider` sits
above `<Routes>` in `App.jsx`, so it lands on the login screen where it is needed.

New strings go into `en` only. `fr` and `nl` arrive through the Stringherd sync on
its next run, which is also how the pipeline gets tested; Spanish is maintained by
hand. The same applies to Phase 1's four plural keys.

### Phase 3: Write the rule down where it will be found

Recorded as `docs/adr/0002-success-feedback.md`, with the alternatives and the
conditions for revisiting it. Not `docs/design-system-mobile.md`, which is the
Android design system, nor `docs/ui-architecture.md`, which records only measured
numbers. This document keeps the audit table; the ADR keeps the rule.

## Adjacent, deliberately out of scope

Each is its own branch. Listed here so they are not rediscovered as part of this work.

- **Destructive confirmation is done three ways.** `window.confirm` in Routine and
  Workout, a styled inline confirm in Exercises (`.ex-confirm-yes` / `.ex-confirm-no`),
  and nothing elsewhere. `window.confirm` cannot be styled, ignores the notebook
  theme, and on some mobile browsers shows the origin. Unify on the Exercises pattern.
- **Validation errors are toasts in Workout and inline on the auth pages.** Inline is
  better for validation: a toast can disappear before the field is fixed. Affects
  `noSetsEntered`, `missingSets`, `manualNoExercises`, `manualNoDate`, `errMinOneDay`.
- **Undo after logging a workout.** Tracked in ROADMAP under Backlog, UI / UX. Needs a
  backend endpoint first: nothing currently deletes a session or its logs, and a log
  advances the routine cursor, bumps `exercise_times_performed` and the rotation
  counter, and overwrites `exercise_user_current_weight`.

## Acceptance checks

- Logging a generated workout and a manual workout both leave the user on the chooser
  with a toast, and the day strip shows the next day without a page reload.
- No occurrence of `mode === "success"` or `wo-success` remains.
- `workout.successMsg` and `workout.backToDashboard` are gone from all five locales,
  and no key is referenced that does not exist.
- Resetting a password shows a confirmation on the login page.
- `npx eslint src/pages/` reports no new problems.
