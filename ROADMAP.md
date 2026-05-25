# FitJournal Roadmap

FitJournal is a personal fitness tracking application. This roadmap tracks planned features, technical debt, and the path from local development to AWS production deployment.

**Last updated:** May 2026

---

## Priority & Effort Legend

| Priority | Meaning |
|---|---|
| 🔴 P0 | Critical — security, breaking bugs, blockers |
| 🟠 P1 | High — significant feature or blocker to deployment |
| 🟡 P2 | Medium — quality improvement, not urgent |
| 🟢 P3 | Low — someday, nice to have |

| Effort | Estimate |
|---|---|
| XS | Under 1 hour |
| S  | Half a day |
| M  | 1–2 days |
| L  | 3–5 days |
| XL | Week or more |

---

## 🎯 Up Next

The short list — what's most likely to be worked on in the next session or two.

| Item | Priority | Effort | Notes |
|---|---|---|---|
| Point mobile app at production backend | 🟠 P1 | S | Update `BASE_URL` in mobile `ApiConfig.kt` → `https://app.fit-journal.com`, rebuild APK, test. Web app already uses `window.location.origin` so it needs no change. |

---

## 📋 Backlog

Lower priority, but tracked so they're not forgotten.

### Functional

| Item | Priority | Effort | Notes |
|---|---|---|---|
| Routine page breaks for users with no profile/routine set up | 🟡 P2 | S | A newly-registered user who navigates directly to Routine (before setting up their profile/routine) hits a broken page. Likely cause: the page or its API assumes data that doesn't exist yet for a fresh user — most probably a missing `workout_state` row or empty `routine_days`/`routine_muscles_per_day`, causing a null/missing-row error (check the uvicorn traceback to confirm). Fix should handle the empty state gracefully: redirect to setup, or show an empty-state prompt, rather than erroring. Reproduce: register fresh user → go straight to Routine. |
| Harden `/next-workout/toggle` payload parsing | 🟢 P3 | XS | The toggle endpoint reads `data['user_id']`, `data['exercise_id']`, `data['is_selected']` via direct dict access, so a missing key throws a `KeyError` → 500 crash instead of a clean 400. Replace with validated access (a Pydantic schema, or `.get()` with explicit checks returning 422/400). Robustness, not security. Same endpoint could also adopt a proper request schema like the other endpoints (it currently takes a raw `dict`). |
| Manual day override — train any day out of order | 🟡 P2 | M | Let user pick which routine day to do today (e.g. do Day 1 even though current is Day 3); workout is logged under the chosen day, and the `current_day_number` cursor advances correctly afterward |
| n8n email automation | 🟡 P2 | M | Send welcome/notification emails (e.g. on registration) via an n8n workflow. New skill. NOTE: requires Lambda to make an outbound call, which the current VPC setup (no NAT Gateway) blocks. Solve by either having n8n call *in* to the API, using SES via a VPC endpoint, or adding a NAT Gateway (~$32/mo — avoid). Needs a webhook + a real email service (SES/SendGrid) with deliverability setup. n8n itself would run outside the VPC (n8n Cloud or a small separate host), so it can send/receive mail freely; the blocker is only when *Lambda* needs to trigger the outbound call. |
| Self-service password reset (real flow) | 🟢 P3 | L | Replace the placeholder forgot-password page with a real reset: request-reset page → emailed time-limited token → set-new-password page → token validation + hash update. Needs email sending (blocked by the same VPC/outbound issue as n8n above), a reset-token table, two endpoints, and security hardening (random single-use tokens, no email-existence leak, rate limiting). Only worth it once the app has real users. |
| Reevaluate calendar display when routine changes | 🟡 P2 | L | Historical workouts hidden when routine restructured; consider per-session routine snapshot |
| Support user-defined / dynamic muscle groups | 🟢 P3 | L | Replace `MuscleGroupEnum` with a `muscle_groups` table; no more schema migrations to add groups |
| "Always select" exercise flag in Calendar | 🟢 P3 | S | Pin/sticky-select an exercise so it's auto-selected every workout |
| Calendar "All Days" — per-day WOD column strips | 🟢 P3 | M | Currently all days share the same set of 10 WOD column dates, so empty columns appear for days that weren't trained on those dates. Better: each day section shows the last 10 sessions *for that specific day*. Likely renders as multiple stacked tables under each Day header rather than one wide table. |
| Add Abs & Forearms exercises to `default_exercises` seed | 🟢 P3 | XS | The shared default exercise catalog has no Abs or Forearms entries (these muscle groups were added to the ENUM later, and exercises were only added to personal `exercises` tables, not the seed). New users therefore get no starter exercises for these two groups. Add a handful of each to `default_exercises` (on RDS prod + local dev) so new registrations include them. |
| Enable auto-deploy on push to main | 🟢 P3 | XS | Uncomment the `push:` block in `deploy.yml` so pushes to `main` auto-deploy. Do *after* the backend test gate exists, so broken code can't auto-ship. |

### UI / UX

| Item | Priority | Effort | Notes |
|---|---|---|---|
| Create a landing page for fit-journal.com | 🟡 P2 | M | Marketing page at the bare domain. Hero, features, screenshots, link to "Sign up" → app.fit-journal.com/web/register. Notebook aesthetic consistent with the app. Likely a single static HTML page hosted on S3 + CloudFront (cheap, fast, HTTPS-ready via ACM). Separate from the FastAPI app. Trigger: when about to put fit-journal.com on CV. |
| Re-evaluate modal visibility issue | 🟡 P2 | S | Deferred earlier; noted in code TODO |
| Audit hardcoded greys (`#ccc`, `#aaa`, `#e0e0e0`) | 🟢 P3 | XS | Replace with `var(--text)` / `var(--muted)` for proper light-mode adaptation |
| Sweep templates for bare-path page links | 🟢 P3 | XS | Page links must use the `/web/` prefix (e.g. `/web/routine`, `/web/dashboard`), not bare `/routine` or `/dashboard` — bare paths 404 (the route is `/web/...`; bare `/routine/{id}` etc. are API endpoints, leave those alone). Several of these have been fixed ad hoc (dashboard routine links, getwod back-to-dashboard). A `grep -rn 'href="/' templates/` sweep would catch any remaining ones. |

### Deployment & Security

| Item | Priority | Effort | Notes |
|---|---|---|---|
| Move static assets to S3 | 🟡 P2 | S | Logo, CSS, JS — serve via CloudFront for performance & HTTPS |
| Set RDS "Publicly accessible: No" | 🟢 P3 | XS | Defense-in-depth tidy-up. RDS still has a public IP but no security group rule allows internet traffic, so it's already unreachable from outside. Removing the public IP entirely is optional cleanup. |
| Create dedicated private subnets for RDS/Lambda | 🟢 P3 | M | Currently using the default VPC's public subnets (functionally private for our purposes — Lambda has no public IP and no NAT route out). Dedicated private subnets + route tables would be the textbook setup. Do if/when the gold-standard VPC layout is wanted (also good cert learning). |

---

## ✅ Done

Recent shipped work, for context.

### May 2026
- **Fixed IDOR / added authorization checks** — endpoints took a `user_id` but only verified authentication (valid token), not ownership, so any logged-in user could read or modify another user's data by changing the ID. Added a `verify_user_access` dependency (confirms the request's `user_id` matches the authenticated user) across all 17 user-scoped endpoints, plus an inline ownership check on `/next-workout/toggle` (which takes `user_id` in the body). Surfaced by an authorization test in the new suite — a concrete example of tests catching a real vulnerability.
- **Backend test suite + CI gate** — built a pytest harness (separate MySQL test DB, per-test cleanup, auth fixture) and 21 tests covering auth, exercises, routine, profile, workout generation, page loads, and authorization. Wired a `test` job into GitHub Actions that runs against a MySQL service container before deploy; the deploy job is gated on it (`needs: test`), so failing tests block the deploy. Tests run both locally (fast feedback) and in CI (enforced gate).
- **Generate WOD — auto-generate selection + empty-state handling** — Generate WOD now guards the full flow: no routine → prompts to create one; no usable selection for the current day → persistent prompt to select in Calendar or run an automatic selection; clicking automatic selection calls the generate endpoint and re-runs generation. Manual selections are respected silently (no nagging about unselected muscle groups — deliberate intent), while auto-generated workouts show a red note for any of the day's muscle groups with no exercises in the user's library. Built on the `requireRoutine()` helper.
- **`requireRoutine()` refactor** — added a shared `requireRoutine()` helper in `api.js` (returns the routine only if `days_per_week > 0`, else null) and used it in dashboard and calendar, consolidating duplicated "does this user have a routine?" checks. Calendar gained an early empty-state guard, also fixing a pre-existing half-render bug for routine-less users.
- **No-rest-day rule** — routine save now requires every training day to have at least one muscle group, enforced both frontend (blocks save, names the empty day(s), red-highlights them) and backend (400 before the existing routine is deleted, so a rejected save doesn't wipe data).
- **Forgot-password page** — placeholder info page (reuses the auth card styling) at `/web/forgot-password`, pointing users to `info@fit-journal.com`; linked subtly under the login password field. (Real self-service reset is a P3 backlog item.)
- **Favicon** — kettlebell-on-dark-square favicon set (ico + PNG sizes + apple-touch-icon) under `static/images/favicon/`, linked in all page heads; silences the `/favicon.ico` 404 noise and adds a browser-tab icon.
- **Set up Cloudflare email** — `info@fit-journal.com` forwards to personal inbox via Cloudflare Email Routing; unblocks the forgot-password page's contact address.
- **Light/dark logo swap** — transparent-background logo (white strokes for dark mode) plus a charcoal-stroke variant for light mode, swapped via `body.light` CSS so the logo reads correctly in both themes across the header and auth pages.
- **Login screen polish** — transparent-background logo so the ruled lines show through; password show/hide toggle on login and both register fields; "Forgot password?" link added; extra left padding between the margin line and field labels.
- **Set up CI/CD with GitHub Actions** — automated build + deploy to Lambda (manual trigger), authenticated via OIDC federation (no stored AWS credentials), least-privilege IAM role scoped to the repo's main branch and single function. Resolved two deployment gotchas: added `lambda:GetFunctionConfiguration` for the update waiter, and forced `manylinux2014` wheels so compiled deps (bcrypt) load on Lambda's Amazon Linux 2 (glibc 2.26) runtime. Replaces the manual Docker→zip→upload process.
- **Fixed root routing + auth guards** — bare domain now redirects to `/web/login` (was sending everyone to `/web/dashboard`); `requireLogin()` redirects silently and gates page logic so unauthenticated visitors no longer get repeated "Please log in" alerts; login page redirects already-logged-in users straight to dashboard. Fixes the first impression for shared links.
- **Redesigned login & register pages** — notebook-style contained card with ruled lines on the `--lh` grid, red margin line, hand-drawn logo, and text resting on the lines (matching the dashboard aesthetic). Replaced the generic dark-card look.
- **Added screenshots + LICENSE to the repo** — captured app screenshots into `docs/images/` and embedded them in the README; added an MIT `LICENSE` file.
- **Decommissioned Aiven** — past its standby window; service deleted (data already migrated to RDS).
- **Network hardening** — moved Lambda into the VPC; RDS now reachable *only* via Lambda's security group (removed the `0.0.0.0/0` internet rule and the stale home-IP rule). No NAT Gateway needed since Lambda only talks to RDS. Created `fitjournal-lambda-sg`, added VPC permissions to Lambda's execution role.
- **Local MySQL dev environment** — set up local MySQL so development is location-independent (no longer dependent on whitelisting a home IP against RDS). Dedicated `fitjournal_local` user; schema built via the app; `default_exercises` seeded from RDS. Local `.env` points at `127.0.0.1`.
- **Upgraded RDS MySQL 8.0.45 → 8.4.9** — ahead of the 8.0 end-of-standard-support deadline (31 Jul 2026); migrated `fitjournal_admin` from `mysql_native_password` to `caching_sha2_password` for 8.4 compatibility; snapshot taken pre-upgrade.
- **Deployed backend to AWS Lambda + API Gateway** — FastAPI runs serverless via Mangum, live at `https://app.fit-journal.com` over HTTPS (ACM cert + API Gateway custom domain + Cloudflare DNS). Fixed deployment-specific issues along the way: bcrypt/passlib version conflict (pinned bcrypt 4.0.1), static/template relative paths, `window.location.origin` for the API URL, HTTP-API TLS policy.
- **README restructured** as an industry-standard front door; added `docs/ARCHITECTURE.md` and `docs/SCHEMA.md`.
- SECRET_KEY moved from hardcoded value to environment variable (eliminates JWT signing secret from public repo)
- Calendar "All Days" and multi-day modes — exercises grouped by day with muscle subheaders
- Migrate database from Aiven MySQL to AWS RDS MySQL (db.t4g.micro, free tier)
- Per-exercise checkoff in Get WOD (matches mobile UX, fixes BUG-001 by preventing empty workout submissions)
- Get WOD table redesign — Muscle column, narrower Exercise column, tighter Weight/Sets/Reps spacing
- Get WOD validation — sets required, reps optional, no exercises checked → error
- Get WOD UX polish — modified-weight highlight, theme-aware check circle, removed misleading placeholders, light-mode underline fix
- Add Forearms muscle group (extended `MuscleGroupEnum`, `ALTER TABLE` on three columns)
- Calendar page rebuild with notebook design system
- Calendar "Current Day" toggle replaced with red `← current day` marker; current day auto-selected on load
- Calendar — reversed date columns (newest left), Select column label
- Clean up legacy `frontend/` folder
- Light-mode color fixes for active states (saved button, selected toggle, active muscle tab, view info text)
- Jinja2 migration complete for all pages
- Dark/light mode toggle

---

## 🧭 Guiding Principles

A few standing rules for how this project evolves.

- **Schema changes need backups first.** Snapshot RDS before any `ALTER TABLE` or major version change.
- **Local dev uses local MySQL.** Start the MySQL80 service before running uvicorn; `.env` points at `127.0.0.1`. RDS is production-only now.
- **Repo is the source of truth.** Deployed code always derives from a commit — never edit code directly in the Lambda console.
- **Deploy is a separate, deliberate step.** `git push` saves to GitHub; deploying runs the GitHub Actions workflow (currently manual). Don't ship a link/feature before the page/endpoint it needs exists.
- **Compiled deps must target Lambda's runtime.** Lambda is Amazon Linux 2 (glibc 2.26); CI installs `manylinux2014` wheels so binaries like bcrypt load.
- **Page links use the `/web/` prefix.** `/web/routine`, `/web/dashboard`, etc. — bare `/routine` 404s. API endpoints (`/routine/{id}`) are separate; don't confuse the two. (Case-sensitivity bites here too — Linux/prod is case-sensitive, Windows-local isn't.)
- **Branch per feature.** Don't commit unrelated changes together.
- **Theme variables, not hardcoded colors.** Any `color: #ccc` is a future bug.
- **Migrations over recreations.** Preserve user data wherever possible.
- **Document the why, not the what.** Code shows what; commits and ROADMAP show why.