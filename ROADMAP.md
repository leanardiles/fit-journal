# FitJournal Roadmap

This document tracks planned enhancements, technical debt, and the path to production deployment.

**Last updated:** May 2026
**Current branch:** `ui-redesign` (Jinja2 migration + notebook design system)

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
| Fix BUG-001 (empty workout submissions) | 🟠 P1 | S | Creates blank calendar entries; visible on calendar page |
| Migrate database from Aiven MySQL → AWS RDS MySQL | 🟠 P1 | M | Connection-string swap + `mysqldump` import |
| Migrate FastAPI backend → Lambda + API Gateway | 🟠 P1 | L | Uses Mangum adapter; pairs with HTTPS |
| Migrate HTTP → HTTPS | 🟠 P1 | M | ACM cert + custom domain; mostly bundled into Lambda + API Gateway work |
| Switch RDS public access to NO | 🟠 P1 | XS | Post-deploy lockdown; private subnet only |

---

## 📋 Backlog

Lower priority, but tracked so they're not forgotten.

### Functional

| Item | Priority | Effort | Notes |
|---|---|---|---|
| Reevaluate calendar display when routine changes | 🟡 P2 | L | Historical workouts hidden when routine restructured; consider per-session routine snapshot |
| Manual day override — train any day out of order | 🟡 P2 | M | Let user pick which routine day to do today (e.g. do Day 1 even though current is Day 3); workout is logged under the chosen day, and the `current_day_number` cursor advances correctly afterward |
| Support user-defined / dynamic muscle groups | 🟢 P3 | L | Replace `MuscleGroupEnum` with a `muscle_groups` table; no more schema migrations to add groups |
| "Always select" exercise flag in Calendar | 🟢 P3 | S | Pin/sticky-select an exercise so it's auto-selected every workout |

### UI / UX

| Item | Priority | Effort | Notes |
|---|---|---|---|
| Revamp login & register templates | 🟡 P2 | M | Bring into notebook design system (currently inconsistent) |
| Re-evaluate modal visibility issue | 🟡 P2 | S | Deferred earlier; noted in code TODO |
| Audit hardcoded greys (`#ccc`, `#aaa`, `#e0e0e0`) | 🟢 P3 | XS | Replace with `var(--text)` / `var(--muted)` for clean light/dark theming |

### Deployment & Security

| Item | Priority | Effort | Notes |
|---|---|---|---|
| Move static assets to S3 | 🟡 P2 | S | Logo, CSS, JS — serve via CloudFront for performance & HTTPS |

---

## ✅ Done (recent)

For context — what's shipped recently.

- Calendar page rebuild with notebook design system
- Add Forearms muscle group (DB enum + templates)
- Clean up legacy `frontend/` folder
- Light-mode color fixes for active button states (saved button, selected toggle, active muscle tab, view info text)
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
