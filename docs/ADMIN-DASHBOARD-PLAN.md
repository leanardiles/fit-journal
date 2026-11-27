# Admin dashboard, build checklist

Working checklist for the `admin-dashboard` branch. The reasoning lives in the
ROADMAP item (**Admin dashboard: registration, activation and retention
metrics**); this file is only the order of operations.

Per `docs/README.md` this is a plan, so when the boxes are ticked, harvest
anything durable (the metric definitions, the header contract) into the
places that keep it and delete this file.

---

## 0. Branch

- [x] `git checkout -b admin-dashboard`

---

## 1. Client platform, shipped first and alone

This is the only part of the feature with a deadline. Platform cannot be
reconstructed later, so it has to be capturing data before the Play Store
launch even if everything below slips. Ship and deploy this step on its own.

**The contract:** header `X-FitJournal-Client`, lowercase, today `web` or
`android`.

**Validate the shape, not the membership.** Lowercase it, accept
`^[a-z][a-z0-9-]{0,19}$`, store whatever passes, and keep a known list only
for ordering and labelling buckets on the dashboard. A strict allowlist of two
would mean that the day an iOS app, a watch app or anything else ships, every
row from it is stored as NULL until a backend deploy catches up, which is
exactly the data loss this step exists to prevent, at exactly the moment the
data matters most. The cost is that a buggy client could create a stray
bucket; that is bounded by the pattern and the column width, visible the
moment anyone looks, and fixable with one `UPDATE`. Losing a platform's first
weeks is not.

NULL still means unknown, which is what every row written before this step
will say forever.

- [x] `users.signup_source`, nullable `VARCHAR(20)`
- [x] `workout_sessions.source`, nullable `VARCHAR(20)`
- [x] `scripts/add_client_source_columns.sql`, run on local and RDS
- [x] A helper in the backend that reads the header, lowercases it, returns it
      if it matches the pattern and `None` otherwise. Lives next to
      `resolve_request_locale`, which does the same job for `Accept-Language`
- [x] Write it on register and on session creation
- [x] React: send the header from `api/client.js`, in all four verbs
- [x] Android: send it from the Retrofit layer
- [x] Tests: `web` and `android` are stored; an unrecognised but well-formed
      value such as `ios` is stored too rather than discarded; a malformed
      value becomes NULL; a missing header becomes NULL; case is normalised
- [ ] Deploy API, then both clients

---

## 2. The rest of the schema

- [x] `users.is_admin`, `BOOLEAN NOT NULL DEFAULT FALSE`
- [x] `daily_stats`, a new table so `create_all` makes it, no migration needed.
      Columns fixed now, because one added later is empty for every day before
      it existed: `stat_date` (date, primary key), `registered`, `verified`,
      `with_routine`, `activated`, `new_signups`, `sessions_logged`, `dau`,
      `wau`, `mau`, and a JSON `by_source` holding the same counters split per
      client
- [x] `scripts/add_user_is_admin.sql`, run locally for now
- [x] Set your own flag locally

---

## 3. `src/stats.py`

- [x] `compute_today(db)`: the counters, activation and the funnel, as a dict
- [x] `run_daily_stats(db)`: upserts today's row, idempotent on re-run
- [x] `compute_cohorts(db)`: weekly grid from the raw tables. Cohort by signup
      week from `user_created_at`, active if a workout was logged in week N,
      denominator frozen at cohort size, day 0 excluded
- [x] Everything called activity reads `created_at`, never `workout_date`,
      which can be backdated
- [x] Tests: two logs in one day count as one active user; a stored row does
      not change when an account is deleted, which is the whole reason the
      table exists; the cohort denominator never moves

---

## 4. The endpoint

- [x] `require_admin`, built on `get_current_user`, 403 when the flag is false
- [x] `GET /v1/admin/stats` returning today (live), history (from
      `daily_stats`) and cohorts (live)
- [x] Tests: admin 200, ordinary user 403, no token 401
- [x] Audit item 6, the parametrized authorization sweep, which is the only
      thing that will catch the next privileged route added without thinking

---

## 5. The page

- [x] `/admin` route with a guard that redirects non-admins to the dashboard.
      Convenience only: the 403 is the real boundary, and the repository is
      public so the route is readable in `main.py` regardless
- [x] Counters, funnel, two trend lines (registrations and WAU), cohort grid
- [x] A link at the foot of Profile, rendered only when `is_admin`. Not in the
      main nav, which is for the product and already has six items
- [x] English strings only

---

## 6. Production

- [x] Run `add_user_is_admin.sql` on RDS, snapshot first
- [x] Set your own `is_admin` there
- [x] Deploy API, then frontend
- [x] Close the temporary access in the same sitting. Closed 2026-11-26

---

## 7. The daily schedule

Last on purpose. Until it exists the page still works from the live half, so
nothing is blocked on infrastructure.

- [x] Branch in the handler, before Mangum sees the event. Dispatch is on
      `event["source"] == "aws.events"`, which EventBridge always sets and no
      API Gateway event carries. Two tests in `test_stats.py`: a schedule
      event writes the row, anything else still reaches the web app
- [x] Daily schedule in the console, pointed at the existing Lambda. Built as
      an EventBridge Scheduler schedule rather than a rule, since the console
      now marks scheduled rules legacy. Scheduler sends the payload you
      configure rather than an envelope, so the target input carries
      `{"source": "aws.events"}` by hand. 23:50 UTC, flexible time window off,
      retries capped at 1 hour so a retry cannot land after midnight and
      compute a row for the wrong date
- [x] Confirm a row appears, then confirm a second run the same day updates it
      rather than duplicating it. Done 2026-11-26 by invoking the function
      with the schedule payload twice: same `stat_date` both times, and the
      page reported one day of snapshots

---

## When the boxes are ticked

- [x] Harvest the metric definitions and the `X-FitJournal-Client` contract
      into `docs/ARCHITECTURE.md`
- [x] Harvest two decisions that outlive this file: the admin page is a
      ledger rather than sticky notes, because its figures are read by
      scanning one aligned column; and it carries inline English rather
      than translation keys, so a back-office vocabulary never reaches
      Stringherd
- [x] Note in `docs/design-system-web.md` that `is_admin` rides on the
      profile response, and that the SPA guard is convenience while the
      403 is the boundary
- [ ] Delete this file
