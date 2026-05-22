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
| CI/CD pipeline | 🟠 P1 | M | Automate deployment. Start with a `deploy.sh` script (build zip + upload to Lambda in one command), then move to GitHub Actions triggered on push to `main`, then add a test gate so broken commits don't ship. New skill — never implemented before. |
| Point mobile app at production backend | 🟠 P1 | S | Update `BASE_URL` in mobile `ApiConfig.kt` → `https://app.fit-journal.com`, rebuild APK, test. Web app already uses `window.location.origin` so it needs no change. |
| Add screenshots + LICENSE to repo | 🟡 P2 | XS | Capture 1–3 screenshots (calendar / Get WOD most distinctive) into `docs/images/`, uncomment README screenshot line. Add MIT `LICENSE` file via GitHub UI. High portfolio impact, low effort. |
| Decommission Aiven | 🟡 P2 | XS | Past its standby window. Final backup, then power off / delete. |

---

## 📋 Backlog

Lower priority, but tracked so they're not forgotten.

### Functional

| Item | Priority | Effort | Notes |
|---|---|---|---|
| Routine page breaks for users with no profile/routine set up | 🟡 P2 | S | A newly-registered user who navigates directly to Routine (before setting up their profile/routine) hits a broken page. Likely cause: the page or its API assumes data that doesn't exist yet for a fresh user — most probably a missing `workout_state` row or empty `routine_days`/`routine_muscles_per_day`, causing a null/missing-row error (check the uvicorn traceback to confirm). Fix should handle the empty state gracefully: redirect to setup, or show an empty-state prompt, rather than erroring. Reproduce: register fresh user → go straight to Routine. |
| Manual day override — train any day out of order | 🟡 P2 | M | Let user pick which routine day to do today (e.g. do Day 1 even though current is Day 3); workout is logged under the chosen day, and the `current_day_number` cursor advances correctly afterward |
| n8n email automation | 🟡 P2 | M | Send welcome/notification emails (e.g. on registration) via an n8n workflow. New skill. NOTE: requires Lambda to make an outbound call, which the current VPC setup (no NAT Gateway) blocks. Solve by either having n8n call *in* to the API, using SES via a VPC endpoint, or adding a NAT Gateway (~$32/mo — avoid). Needs a webhook + a real email service (SES/SendGrid) with deliverability setup. |
| Reevaluate calendar display when routine changes | 🟡 P2 | L | Historical workouts hidden when routine restructured; consider per-session routine snapshot |
| Support user-defined / dynamic muscle groups | 🟢 P3 | L | Replace `MuscleGroupEnum` with a `muscle_groups` table; no more schema migrations to add groups |
| "Always select" exercise flag in Calendar | 🟢 P3 | S | Pin/sticky-select an exercise so it's auto-selected every workout |
| Calendar "All Days" — per-day WOD column strips | 🟢 P3 | M | Currently all days share the same set of 10 WOD column dates, so empty columns appear for days that weren't trained on those dates. Better: each day section shows the last 10 sessions *for that specific day*. Likely renders as multiple stacked tables under each Day header rather than one wide table. |
| Add Abs & Forearms exercises to `default_exercises` seed | 🟢 P3 | XS | The shared default exercise catalog has no Abs or Forearms entries (these muscle groups were added to the ENUM later, and exercises were only added to personal `exercises` tables, not the seed). New users therefore get no starter exercises for these two groups. Add a handful of each to `default_exercises` (on RDS prod + local dev) so new registrations include them. |

### UI / UX

| Item | Priority | Effort | Notes |
|---|---|---|---|
| Redesign login & register page layout | 🟡 P2 | S | Current notebook-card layout is functional but unsatisfying. Rework the visual layout. Gather reference examples of auth pages worth emulating before starting. |
| Create a landing page for fit-journal.com | 🟡 P2 | M | Marketing page at the bare domain. Hero, features, screenshots, link to "Sign up" → app.fit-journal.com/web/register. Notebook aesthetic consistent with the app. Likely a single static HTML page hosted on S3 + CloudFront (cheap, fast, HTTPS-ready via ACM). Separate from the FastAPI app. Trigger: when about to put fit-journal.com on CV. |
| Re-evaluate modal visibility issue | 🟡 P2 | S | Deferred earlier; noted in code TODO |
| Audit hardcoded greys (`#ccc`, `#aaa`, `#e0e0e0`) | 🟢 P3 | XS | Replace with `var(--text)` / `var(--muted)` for proper light-mode adaptation |

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
- **Network hardening** — moved Lambda into the VPC; RDS now reachable *only* via Lambda's security group (removed the `0.0.0.0/0` internet rule and the stale home-IP rule). No NAT Gateway needed since Lambda only talks to RDS. Created `fitjournal-lambda-sg`, added VPC permissions to Lambda's execution role.
- **Local MySQL dev environment** — set up local MySQL so development is location-independent (no longer dependent on whitelisting a home IP against RDS). Dedicated `fitjournal_local` user; schema built via the app; `default_exercises` seeded from RDS. Local `.env` points at `127.0.0.1`.
- **Upgraded RDS MySQL 8.0.45 → 8.4.9** — ahead of the 8.0 end-of-standard-support deadline (31 Jul 2026); migrated `fitjournal_admin` from `mysql_native_password` to `caching_sha2_password` for 8.4 compatibility; snapshot taken pre-upgrade.
- **Deployed backend to AWS Lambda + API Gateway** — FastAPI runs serverless via Mangum, live at `https://app.fit-journal.com` over HTTPS (ACM cert + API Gateway custom domain + Cloudflare DNS). Fixed deployment-specific issues along the way: bcrypt/passlib version conflict (pinned bcrypt 4.0.1), static/template relative paths, `window.location.origin` for the API URL, HTTP-API TLS policy.
- **README restructured** as an industry-standard front door; added `docs/ARCHITECTURE.md` and `docs/SCHEMA.md`.
- Login/Register pages redesigned with notebook-style card; auth helpers refactored into reusable api.js functions
- SECRET_KEY moved from hardcoded value to environment variable (eliminates JWT signing secret from public repo)
- Calendar "All Days" and multi-day modes — exercises grouped by day with muscle subheaders
- Migrate database from Aiven MySQL to AWS RDS MySQL (db.t4g.micro, free tier; Aiven retained as standby)
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
- **Branch per feature.** Don't commit unrelated changes together.
- **Theme variables, not hardcoded colors.** Any `color: #ccc` is a future bug.
- **Migrations over recreations.** Preserve user data wherever possible.
- **Document the why, not the what.** Code shows what; commits and ROADMAP show why.