# React Frontend (`web-react`) , Roadmap & Backlog

Running list of deferred work, polish, and "handle this later" notes for the React
frontend rewrite. Add to it whenever a detail surfaces mid-task so it isn't lost.

Legend: [ ] todo   [~] in progress   [x] done

---

## Core flow (build order)
- [x] react-i18next foundation + locale hierarchy (en / es / es-AR / nl) + fallback chains
- [x] Component library: Button, Field, Card, Select, Modal (design tokens) + Storybook stories
      (Button/Field/Card/Select have stories; Modal story still TODO)
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
- [ ] Dashboard , real modules (quick stats, current routine, sets-per-muscle). Currently a stub
      showing name + locale only.
- [ ] Routine (day-by-day editor: per-muscle pools / manual days , mirror the web app)
- [ ] Calendar (per-day workout-log table)
- [ ] Workout / Log WOD (generate a routine workout or log one manually)

## i18n showcases (high interview value , do once core flow exists)
- [ ] Pseudo-localization mode ([!!! Ëxpändëd têxt !!!]) , catch hardcoded strings + layout breaks. HIGHEST differentiator.
- [ ] Arabic / RTL showcase , add `ar` locale + Arabic-capable font + dir switching. Logical-properties prep is done.
- [ ] Storybook global locale toolbar (switch ALL stories between locales from a dropdown)
- [ ] CLDR pluralization via react-i18next ("1 set" vs "2 sets", not "add an s")
- [ ] Localized date/number formatting via Intl (replace any hand-rolled formatting)

## Design system
- [x] Select and Modal extracted into the library (Select has a Storybook story)
- [ ] Toggle / ChipToggle component (+ story) , the notebook toggle chips (sex / units)
- [ ] Rebuild Profile from Select + Toggle (proof the library composes; grows the documented design system)
- [ ] Modal Storybook story
- [ ] Consider a PrimaryButton/SecondaryButton/etc. story gallery as a documented design system

## Theming / design
- [ ] Light mode: add -light logo assets + a theme toggle (tokens already structured for it)

## Robustness / correctness (accumulated details)
- [x] apiPost missing auth header , FIXED. It was written for the public login/register endpoints, so
      the first authenticated POST (adding an exercise) 401'd. All four verbs now attach the JWT where a
      token exists (apiPost adds it conditionally so login/register still work token-less).
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
- API base URL comes from Vite env (.env.development=local, .env.production=prod).
- All user-facing text routes through t(); values may match English where natural (e.g. Dutch "Dashboard").
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