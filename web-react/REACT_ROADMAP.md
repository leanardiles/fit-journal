# React Frontend (`web-react`) , Roadmap & Backlog

Running list of deferred work, polish, and "handle this later" notes for the React
frontend rewrite. Add to it whenever a detail surfaces mid-task so it isn't lost.

Legend: [ ] todo   [~] in progress   [x] done

---

## Core flow (build order)
- [x] react-i18next foundation + locale hierarchy (en / es / es-AR / nl) + fallback chains
- [x] Component library: Button, Field, Card, Select, Modal, Toggle, StickyNote (design tokens) + Storybook
      stories (Button/Field/Card/Select/Toggle/Modal/StickyNote have stories)
- [x] Design tokens (tokens.css) ported from notebook.css (dark; light-ready structure)
- [x] Login page (form state, logo, backend-wired /v1/login, env-based API config)
- [x] Auth: token storage, logout, ProtectedRoute
- [x] Routing (react-router) + placeholder pages
- [x] AppLayout with notebook aesthetic (ruled lines, red margin, header separator)
- [x] User Context (fetch profile once, share to header + pages)
- [x] Register page + extract AuthLayout (shared centered shell for login/register)
- [x] Language switcher in the app UI (header dropdown, live app-wide, persists user_locale to backend)
- [x] Logical-properties / RTL prep: physical , logical CSS across AppLayout, Toast, Auth; new pages
      authored logical-first; explicit [dir="rtl"] override for the notepad margin-line background.
      (Arabic locale + dir switching not wired yet , see i18n showcases.)

### Real pages (per-page status)
- [x] Profile (full CRUD: edit/save PUT, unit-converted height/weight, delete account + password re-auth)
- [x] Exercises (list by muscle, add via modal, delete w/ inline confirm, inline weight edit on blur,
      muscle filter persisted in the URL via useSearchParams)
- [x] Dashboard , real modules: quick stats (workouts this week + current day), current routine, and
      sets-per-muscle with a week / last-7-days Toggle. Yellow StickyNote component + fluid auto-fit grid.
- [x] Calendar , history matrix (exercises x last-10 sessions, sets_completed in the cells) with a day
      filter, PLUS the next-workout planner (select toggles, Automatic selection, Clear). Localized date
      column headers via Intl.DateTimeFormat.
- [x] Routine , view + editor. View: columns per day, per-muscle summary with expandable pool, manual list.
      Editor (create + edit): days selector, per-day type toggle (per_muscle / manual), per-muscle pools
      with per-session steppers + exercise checkboxes, manual muscle-tab picker, inline validation, toast on
      save. Edit mode prefills the saved routine, locks the day count, and adds add/delete-day controls.
      Superset grouping (exercise_groups) deferred , see Routine follow-ups.
- [ ] Workout / Log WOD , STILL A PLACEHOLDER. Needs a full revamp: generate a workout from the routine or
      log one manually (mirror the legacy page). Last remaining page to rebuild.

### Routine follow-ups
- [ ] Superset / manual grouping (slice 4b). The optional grouping panel on manual days: tick exercises and
      order them into groups (A1, A2, B1, ...), serialized to `exercise_groups` [{exercise_id, group_index,
      position}] in the save payload. Its own state machine (order / groups / groupSeq / picked /
      buildSequence). Legacy logic is in routine.html. Until this exists, manual days save as a flat
      exercise_ids list, and re-saving a previously grouped manual day drops its grouping.
- [ ] Delete Routine look. Right now, to delete a routine day there is a small X at the end of the routine day line. I want to add a clear delete symbol, consistent to the rest of the app (e.g. Exercises).      

## i18n showcases (high interview value , do once core flow exists)
- [ ] Pseudo-localization mode ([!!! Ëxpändëd têxt !!!]) , catch hardcoded strings + layout breaks. HIGHEST
      differentiator. Also a QA sweep of everything built so far (muscle-label column widths, etc.).
- [ ] Arabic / RTL showcase , add `ar` locale + Arabic-capable font + dir switching. Logical-properties prep
      is done. Remember to flip literal direction glyphs (e.g. the Calendar current-day marker arrow).
- [ ] Storybook global locale toolbar (switch ALL stories between locales from a dropdown)
- [x] CLDR pluralization via react-i18next , live in dashboard.sets (N sets), calendar.generated
      (N exercises selected), routine.exercises (N exercises). Uses _one/_other keys, not string concat.
- [~] Localized date/number formatting via Intl , Calendar date column headers use
      Intl.DateTimeFormat(i18n.language). Number formatting (weights, counts) via Intl.NumberFormat still TODO.

## Design system
- [x] Select and Modal extracted into the library (Select + Modal both have Storybook stories)
- [x] Toggle / ChipToggle component (+ story) , the notebook toggle chips (sex / units, Dashboard range)
- [x] Rebuild Profile from Select + Toggle (proof the library composes; timezone is a grouped Select)
- [x] Modal Storybook story
- [x] StickyNote component (+ story) , yellow note wrapper that locally rebinds design tokens so token-based
      children theme onto it; used by the Dashboard
- [ ] Consider a PrimaryButton/SecondaryButton/etc. story gallery as a documented design system

## Theming / design
- [ ] Light mode: add -light logo assets + a theme toggle (tokens already structured for it)

## Robustness / correctness (accumulated details)
- [x] apiPost missing auth header , FIXED. It was written for the public login/register endpoints, so
      the first authenticated POST (adding an exercise) 401'd. All four verbs now attach the JWT where a
      token exists (apiPost adds it conditionally so login/register still work token-less).
- [x] Dashboard sets-per-muscle read the wrong muscle field , FIXED. Logs carry no muscle_group and the
      /exercises library uses exercise_muscle_group (not muscle_group), so the map was built on an
      undefined field and the module always showed "No sets logged". Now reads exercise_muscle_group.
- [ ] BUG , Profile height input (imperial) resets to 0 while typing. With imperial units selected, typing
      the first digit of height (e.g. "1") instantly collapses the field to 0, so the whole number can't be
      entered. Likely the field converts the display value to canonical cm on EVERY keystroke (round-tripping
      an incomplete feet/inches value through feetInchesToCm), which zeroes a partial entry. Fix: hold the raw
      typed string in local state and convert only on blur (the same on-blur pattern the inline weight edit
      uses), instead of converting on each change. (Reported while testing Routine, but the field lives on
      Profile , Routine has no height input.)
- [ ] Validate stored user_locale against SUPPORTED_LOCALES on read; fall back if unsupported.
      (Why: dropping a locale from config doesn't fix DB rows that still hold it, e.g. user 3
      showed es-419 after es-419 was removed from the model. Config/schema changes don't
      retroactively clean existing data.)
- [ ] Handle expired/invalid JWT (401 from an authenticated call) , clear token + redirect to login.
- [ ] Duplicate-fetch avoidance: confirm Context fetches profile once (done); watch for similar
      patterns as more pages fetch data.
- [ ] Loading / error states: standardize a pattern (currently ad-hoc "Loading..." / "Error").

## Testing (JD requirement , front-end testing)
- [ ] Component tests (Storybook already installed addon-vitest): render correct string per locale;
      fail if a translation key is missing.
- [ ] A basic route/auth test (protected route redirects when logged out).

## Deployment (later)
- [ ] Host built static files on S3 + CloudFront (NOT Lambda); .env.production -> prod API.
- [ ] Add the deployed frontend origin to the backend CORS allowlist.
- [ ] CI: build web-react in GitHub Actions (separate from the Lambda backend deploy).

## Backend follow-ups surfaced during frontend work (track here, do on backend branch)
- [x] Persist user_locale in update_profile , it was silently dropped (endpoint updated 7 fields but not
      locale). Fixed; this also repaired the Profile page's locale save, which had only ever "stuck"
      via the client-side i18next cache.
- [ ] Weight-precision migration to prod: `exercise_user_current_weight` widened to DECIMAL(6,3) on LOCAL
      (needed so imperial 2-decimal weights round-trip exactly through canonical kg storage). Run the same
      ALTER on prod at ship time. Consider widening `weight_used` (logs) and `user_weight` (profile) too,
      and update the SQLAlchemy models to DECIMAL(6,3) to match.
- [ ] Delete orphaned locales/es-419/ folder on the backend.
- [ ] Externalize the remaining ~25 HTTPException strings (only login/register wired so far).
- [ ] Post-auth locale resolution: use stored user_locale first (resolve_request_locale supports it).
- [ ] Move DB migrations into the CI pipeline (the prod es-419 / user_locale / weight-precision pain proved why).
- [ ] Data cleanup: user 3's stored user_locale = es-419 (stale); set to es-AR or NULL.

## Notes / conventions
- Two src/ folders (backend src/ vs web-react/src/). Two sets of locales.
- Frontend catalogs are thin regional overrides (es-AR only holds what differs from es).
- Base es is tuteo (broad LatAm); voseo lives in es-AR. FOLLOW-UP: Calendar base es shipped a voseo
  imperative ("Agregá"); tuteo-ize it (or push to es-AR) during the i18n polish pass so the base stays neutral.
- API base URL comes from Vite env (.env.development=local, .env.production=prod).
- All user-facing text routes through t(); values may match English where natural (e.g. Dutch "Dashboard").
- Shared i18n key: "Day {{day}}" lives in common.day (used by Dashboard, Calendar, Routine); do not
  re-add per-page day keys.
- Feedback convention: form-validation errors the user must act on show inline (persist while correcting);
  action confirmations and system/transient feedback use toasts (auto-dismiss). Exception: silent autosave
  (inline weight edit) shows nothing on success , the persisted value is the confirmation; errors still toast.
- Muscle names: canonical English value is stored/sent to the backend; the DISPLAY label is translated via
  the `muscles.*` catalog namespace. `constants/muscles.js` is the single source-of-truth list (MUSCLE_GROUPS).
- Weights: stored canonical in kg; convert at the display boundary (kgToDisplay / displayToKg). Imperial
  needs DECIMAL(6,3) kg storage to round-trip 2-decimal pounds exactly (2-decimal kg was lossy).
- View / filter state lives in the URL (useSearchParams), e.g. the Exercises muscle filter , refresh-safe,
  bookmarkable, back/forward aware. Prefer this over local state or localStorage for "what am I looking at".
- Shared constant lists live in their own module (LOCALE_OPTIONS in i18n/locales.js, MUSCLE_GROUPS in
  constants/muscles.js) so "add a language / muscle" is a one-line data change used everywhere.