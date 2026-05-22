# FitJournal Roadmap

FitJournal is a personal fitness tracking application I'm building in parallel with my MS in Computer Science. This roadmap tracks planned features, technical debt, and the path from local development to AWS production deployment.

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
| Migrate FastAPI backend → Lambda + API Gateway | 🟠 P1 | L | In progress. Done: Mangum adapter, deployment package, Lambda function tested. Remaining: API Gateway HTTP API, custom domain via Cloudflare DNS + ACM. |
| Migrate HTTP → HTTPS with custom domain | 🟠 P1 | M | Bundled with Lambda deployment above. Path: ACM cert for fit-journal.com + *.fit-journal.com → API Gateway custom domain (app.fit-journal.com) → Cloudflare DNS CNAME (DNS-only mode) |
| Network hardening: close RDS to internet, move Lambda inside VPC | 🟠 P1 | M | Currently RDS security group allows 0.0.0.0/0 on port 3306 (added for Lambda → RDS connectivity during initial deployment). Post-deployment: move Lambda inside the same VPC as RDS, remove the 0.0.0.0/0 rule, restrict RDS to VPC-internal traffic only. |

---

## 📋 Backlog

Lower priority, but tracked so they're not forgotten.

### Functional

| Item | Priority | Effort | Notes |
|---|---|---|---|
| Manual day override — train any day out of order | 🟡 P2 | M | Let user pick which routine day to do today (e.g. do Day 1 even though current is Day 3); workout is logged under the chosen day, and the `current_day_number` cursor advances correctly afterward |
| Reevaluate calendar display when routine changes | 🟡 P2 | L | Historical workouts hidden when routine restructured; consider per-session routine snapshot |
| Support user-defined / dynamic muscle groups | 🟢 P3 | L | Replace `MuscleGroupEnum` with a `muscle_groups` table; no more schema migrations to add groups |
| "Always select" exercise flag in Calendar | 🟢 P3 | S | Pin/sticky-select an exercise so it's auto-selected every workout |
| Calendar "All Days" — per-day WOD column strips | 🟢 P3 | M | Currently all days share the same set of 10 WOD column dates, so empty columns appear for days that weren't trained on those dates. Better: each day section shows the last 10 sessions *for that specific day*. Likely renders as multiple stacked tables under each Day header rather than one wide table. |

### UI / UX

| Item | Priority | Effort | Notes |
|---|---|---|---|
| Create a landing page for fit-journal.com | 🟡 P2 | M | Marketing page at the bare domain. Hero, features, screenshots, link to "Sign up" → app.fit-journal.com/web/register. Notebook aesthetic consistent with the app. Likely a single static HTML page hosted on S3 + CloudFront (cheap, fast, HTTPS-ready via ACM). Separate from the FastAPI app. |
| Re-evaluate modal visibility issue | 🟡 P2 | S | Deferred earlier; noted in code TODO |
| Audit hardcoded greys (`#ccc`, `#aaa`, `#e0e0e0`) | 🟢 P3 | XS | Replace with `var(--text)` / `var(--muted)` for proper light-mode adaptation |

### Deployment & Security

| Item | Priority | Effort | Notes |
|---|---|---|---|
| Move static assets to S3 | 🟡 P2 | S | Logo, CSS, JS — serve via CloudFront for performance & HTTPS |

---

## ✅ Done

Recent shipped work, for context.

### May 2026
- Upgraded RDS MySQL 8.0.45 → 8.4.9 (ahead of the 8.0 end-of-standard-support deadline; migrated fitjournal_admin from mysql_native_password to caching_sha2_password for 8.4 compatibility; snapshot taken pre-upgrade)
- Login/Register pages redesigned with notebook-style card; auth helpers refactored into reusable api.js functions
- SECRET_KEY moved from hardcoded value to environment variable (eliminates JWT signing secret from public repo)
- Calendar "All Days" and multi-day modes — exercises grouped by day with muscle subheaders
- Migrate database from Aiven MySQL to AWS RDS MySQL (MySQL 8.0.45, db.t4g.micro, free tier; Aiven retained as standby through early June)
Per-exercise checkoff in Get WOD (matches mobile UX, fixes BUG-001 by preventing empty workout submissions)
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

- **Schema changes need backups first.** Snapshot Aiven (or RDS) before any `ALTER TABLE`.
- **Branch per feature.** Don't commit unrelated changes together.
- **Theme variables, not hardcoded colors.** Any `color: #ccc` is a future bug.
- **Migrations over recreations.** Preserve user data wherever possible.
- **Document the why, not the what.** Code shows what; commits and ROADMAP show why.