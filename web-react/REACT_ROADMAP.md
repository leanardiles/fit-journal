# React Frontend (`web-react`) , Roadmap & Backlog

Running list of deferred work, polish, and "handle this later" notes for the React
frontend rewrite. Add to it whenever a detail surfaces mid-task so it isn't lost.

Legend: [ ] todo   [~] in progress   [x] done

---

## Core flow (build order)
- [x] react-i18next foundation + locale hierarchy (en / es / es-AR / nl) + fallback chains
- [x] Component library: Button, Field, Card (design tokens) + Storybook multi-locale stories
- [x] Design tokens (tokens.css) ported from notebook.css (dark; light-ready structure)
- [x] Login page (form state, logo, backend-wired /v1/login, env-based API config)
- [x] Auth: token storage, logout, ProtectedRoute
- [x] Routing (react-router) + placeholder pages
- [x] AppLayout with notebook aesthetic (ruled lines, red margin, header separator)
- [x] User Context (fetch profile once, share to header + pages)
- [x] Dashboard shows real profile data (authenticated API call)
- [ ] Register page + extract AuthLayout (shared centered shell for login/register)
- [ ] Build out real pages: Dashboard (real modules), Routine, Exercises, Calendar, Workout, Profile
- [ ] Language switcher in the app UI (and have it set the request locale so backend errors localize live)

## i18n showcases (high interview value , do once core flow exists)
- [ ] Pseudo-localization mode ([!!! Ëxpändëd têxt !!!]) , catch hardcoded strings + layout breaks
- [ ] Storybook global locale toolbar (switch ALL stories between locales from a dropdown)
- [ ] CLDR pluralization via react-i18next ("1 set" vs "2 sets", not "add an s")
- [ ] Localized date/number formatting via Intl (replace any hand-rolled formatting)

## Theming / design
- [ ] Light mode: add -light logo assets + a theme toggle (tokens already structured for it)
- [ ] Consider a shared PrimaryButton/SecondaryButton/etc. story gallery as a documented design system

## Robustness / correctness (accumulated details)
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
- [ ] Delete orphaned locales/es-419/ folder on the backend.
- [ ] Externalize the remaining ~25 HTTPException strings (only login/register wired so far).
- [ ] Post-auth locale resolution: use stored user_locale first (resolve_request_locale supports it).
- [ ] Move DB migrations into the CI pipeline (the prod es-419 / user_locale migration pain proved why).
- [ ] Data cleanup: user 3's stored user_locale = es-419 (stale); set to es-AR or NULL.

## Notes / conventions
- Two src/ folders (backend src/ vs web-react/src/). Two sets of locales.
- Frontend catalogs are thin regional overrides (es-AR only holds what differs from es).
- API base URL comes from Vite env (.env.development=local, .env.production=prod).
- All user-facing text routes through t(); values may match English where natural (e.g. Dutch "Dashboard").
- Feedback convention: form-validation errors the user must act on show inline (persist while correcting); action confirmations and system/transient feedback use toasts (auto-dismiss). Rationale: auto-dismissing a validation error while the user is still fixing it is bad UX.