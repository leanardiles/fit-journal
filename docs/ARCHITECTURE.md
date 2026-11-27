# Architecture

This document describes how FitJournal is structured and how it is deployed. For the data model, see [SCHEMA.md](SCHEMA.md).

## System overview

FitJournal is a FastAPI backend that serves JSON to two clients, a React single-page web app and a native Android app, deployed serverless on AWS. The backend served a Jinja web app until November 2026; it is now the API only and renders no pages.

```
  fit-journal.com ──► S3 + CloudFront ──► React SPA (static assets)

  app.fit-journal.com  (JSON API; used by the React SPA and the Android app)
        │
        ▼
  Cloudflare DNS  (CNAMEs, DNS-only)
        │
        ▼
  API Gateway  (HTTP API, ANY /{proxy+}, custom domain via ACM)
        │
        ▼
  ┌──────────────── VPC (default, 172.31.0.0/16) ────────────────┐
  │                                                               │
  │  AWS Lambda  (FastAPI via Mangum, Python 3.11)                │
  │  in private subnet 172.31.128.0/20                            │
  │     │                                    │                    │
  │     │ private path (SG → SG)             │ 0.0.0.0/0          │
  │     ▼                                    ▼                    │
  │  RDS MySQL                          NAT Gateway (+ EIP)       │
  │  (private, 3306 from                in a public subnet        │
  │   the Lambda SG only)                    │                    │
  └───────────────────────────────────────── │ ──────────────────┘
                                              ▼
                                     Internet Gateway ──► Resend API (email)
```

The React SPA is static and talks to the backend over HTTPS/JSON. Both clients authenticate with JWT. The backend is the only component that touches the database; clients never connect to it directly. The Lambda reaches RDS over a private path inside the VPC (RDS is not reachable from the internet) and reaches external services such as the email provider through the NAT Gateway.

## Components

### Web client: React SPA (current)
The primary web client is a React single-page app built with Vite (react-router for routing, react-i18next for localization). It is a static bundle hosted in an S3 bucket (`fitjournal-frontend-prod`) and served through CloudFront at `fit-journal.com`. CloudFront is configured for SPA routing: 403/404 responses are rewritten to `/index.html` so client-side routes resolve. The SPA calls the API at `app.fit-journal.com`, with the base URL injected at build time via `VITE_API_URL`.

### Backend: FastAPI on AWS Lambda
The backend is a FastAPI application exposing a JSON API under the `/v1` prefix. It renders no HTML: the Jinja frontend it used to serve under `/web/*` was removed in November 2026, along with `templates/` and `static/`, and `/` now returns a small JSON document pointing at the React app. It runs on AWS Lambda using [Mangum](https://github.com/jordaneremieff/mangum), which adapts API Gateway events into the ASGI requests FastAPI expects. The same codebase runs under `uvicorn` locally and under Lambda in production. The Lambda is attached to the default VPC so it can reach RDS privately, and it sits in a private subnet with a route to a NAT Gateway so it can also make outbound calls to the internet (needed for the email provider).

### API layer: API Gateway HTTP API
An API Gateway HTTP API (chosen over REST API for lower cost and simpler config) sits in front of Lambda. A single catch-all route (`ANY /{proxy+}` plus `ANY /`) forwards every request to Lambda, letting FastAPI handle all routing internally. The `$default` stage serves the API, and a custom domain (`app.fit-journal.com`) is bound with an ACM certificate.

### Database: AWS RDS MySQL
A managed MySQL instance (`db.t4g.micro`, single-AZ). The backend connects via SQLAlchemy + PyMySQL with SSL. Network access is restricted to the Lambda security group only (see Networking & security). See [SCHEMA.md](SCHEMA.md) for the data model.

### Email: Resend
Transactional email (a verification code at signup and password-reset links) is sent through [Resend](https://resend.com) over its HTTPS API. Sending is abstracted behind `src/emailer.py`, so the provider can be swapped without touching the endpoints. The sending domain's SPF, DKIM, and DMARC records live in Cloudflare DNS. Resend was adopted after AWS SES production access was declined; see Key decisions.

### Mobile client: native Android (Kotlin)
A native Android app built with Jetpack Compose and an MVI architecture, sharing the same backend. JWTs are stored encrypted via the Android Keystore and injected into requests by an OkHttp interceptor.

## Authentication and account flows

Stateless JWT across both platforms:

1. Registration creates the account and emails a 6-digit verification code. The account cannot log in until the email is verified.
2. Login issues a JWT (30-day expiry) signed with a server-side secret (`SECRET_KEY`). It is refused with a distinct response while the email is unverified.
3. Web stores the token in `localStorage`; mobile stores it encrypted and injects it via an OkHttp `AuthInterceptor`.
4. Password reset emails a single-use, time-limited tokenized link; completing it also marks the email verified.
5. Verification codes and reset tokens are stored hashed in the `auth_tokens` table with an expiry and single-use tracking.

Protected endpoints require a valid Bearer token; there is no server-side session to invalidate.

## Internationalization

Two independent catalogs, connected by the `Accept-Language` header:

- The React SPA localizes with react-i18next (English, Spanish on a LatAm base, an es-AR voseo override, and Dutch) and sends the active language as `Accept-Language` on every request.
- The backend keeps its own catalogs under `locales/` for server-side messages such as API error text, selected from that same header.

## Product metrics and the admin page

A single private page at `/admin`, served by one endpoint, `GET /v1/admin/stats`.

**The client contract.** Every request from every client carries
`X-FitJournal-Client`, lowercase, today `web` or `android`. The backend
validates its *shape*, not its membership: `^[a-z][a-z0-9-]{0,19}$`, store
whatever passes (`src/clients.py`). An allowlist of two would mean that the day
an iOS or watch client ships, every row from it is stored as NULL until a
backend deploy catches up, which is exactly the loss the field exists to
prevent, at the moment the data matters most. A stray bucket from a buggy
client is bounded by the pattern and fixable with one `UPDATE`; a platform's
first weeks are not recoverable. The value lands on `users.signup_source` at
registration and on `workout_sessions.source` at every log. NULL means unknown,
which is what every row written before the header shipped says permanently.

**What the figures mean.**

- Registered, verified, built a routine, logged a workout: cumulative counts of
  people. The last is the activation number and the denominator for the
  activity figures.
- DAU, WAU, MAU: distinct users with a session created in the last 1, 7 or 30
  days. Rolling windows, not calendar periods.
- Everything called activity reads `WorkoutSession.created_at`, never
  `workout_date`, which the user can backdate.
- Cohorts: a cohort is everyone who signed up in one UTC week. Week N is how
  many of *that* cohort logged a workout in it. The denominator is frozen at
  the cohort's size and never shrinks to the still-active, which is the mistake
  that makes retention look like it is improving while users leave. Week 0 is
  the signup week itself and is not automatically 100 percent: it answers
  activation rather than retention. A week that has not elapsed is absent from
  the row rather than zero, because a zero for the future reads as churn.

**Why `daily_stats` exists** when every figure is recomputable: account
deletion cascades, so a user who leaves silently rewrites history. The
snapshots are the only record that cannot be edited by a later deletion. The
page therefore reads `today` live, so it works before the job has ever run, and
`history` from the table.

**The schedule.** One EventBridge Scheduler schedule at 23:50 UTC, targeting
the same Lambda. The time is not arbitrary: `compute_today` counts from UTC
midnight and stamps the row with that same UTC date, so the row is only
complete at the end of its own day. The flexible time window is off and retries
are capped at one hour for the same reason, since a retry landing after
midnight computes a row for the new date and leaves the previous day missing.

The gotcha worth remembering: EventBridge **Scheduler** sends the payload you
configure, while a classic EventBridge **rule** sends an envelope containing
`source: aws.events`. The handler dispatches on that field, so the Scheduler
target input carries `{"source": "aws.events"}` by hand. `main.handler` checks
for it before Mangum sees the event, and raises a named error for anything that
is neither an HTTP request nor the schedule.

**The boundary.** `users.is_admin`, checked per request by `require_admin`,
which is a separate dependency from `verify_user_access` on purpose: that one
answers whether the data is yours, this one whether you are staff, and
conflating them is how a route ends up checking neither. The flag is read from
the database on every request rather than baked into the token, so revoking it
does not depend on the user logging out. It also rides on the profile response
so the SPA can render the link and guard the route, but that guard is
convenience: the repository is public, the route is readable in `main.py`, and
the 403 is what protects the numbers.

## Networking & security

RDS is not reachable from the internet. Access is controlled with security-group referencing inside the VPC:

- The Lambda runs in the default VPC with its own security group (`fitjournal-lambda-sg`).
- RDS's security group allows inbound 3306 only from the Lambda security group, not from any IP range.
- The Lambda sits in a private subnet (`172.31.128.0/20`) whose route table sends `0.0.0.0/0` to a NAT Gateway in a public subnet. That is the Lambda's only route to the internet, and it exists so the Lambda can call the Resend API. An Internet Gateway alone does not give a Lambda outbound access, because Lambda network interfaces have no public IP.
- RDS is not publicly accessible: it has no public IP and is reachable only from inside the VPC. On top of that, its security group restricts inbound 3306 to the Lambda security group only.

For one-off manual database access (the Lambda is the only in-VPC client), temporarily set RDS to publicly accessible, add your current IP to its security group on 3306, connect, then set public access back off and remove the rule. A cleaner alternative is to tunnel in through an SSM-managed instance in the VPC, which needs no public IP.

## Configuration

The backend reads all configuration from environment variables: database connection (`DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`, `DB_SSL`), the JWT signing secret (`SECRET_KEY`), and email settings (`RESEND_API_KEY`, `EMAIL_ENABLED`, `EMAIL_FROM`, `EMAIL_REPLY_TO`, `FRONTEND_BASE_URL`).

- Locally, non-secret values come from a gitignored root `.env` (loaded via `python-dotenv`) pointing at a local/dev MySQL, and secrets are injected from 1Password via `op run --env-file=.env.op`, which resolves `op://` references at launch. This keeps real secret values out of the repo and off disk.
- On Lambda, the same variables are set as Lambda environment variables. `EMAIL_ENABLED` defaults to true so production sends real email; locally it is kept false unless testing.

`load_dotenv()` is a no-op when no `.env` is present, so the identical code path works in both environments. Local and production are fully separate databases.

## Deployment

Two GitHub Actions workflows, both authenticated to AWS via OpenID Connect (OIDC) so no AWS credentials are stored in GitHub. Both are currently manual (`workflow_dispatch`).

### Backend: `.github/workflows/deploy.yml`
Builds the Lambda package on a clean Linux runner, zips it, and runs `aws lambda update-function-code` against `fitjournal-api`.

The manylinux requirement (important): Lambda's Python 3.11 runtime is Amazon Linux 2 (glibc 2.26). Packages with compiled binaries (`bcrypt`, `cryptography`, `pydantic-core`) must be installed as manylinux2014 wheels (glibc 2.17). A plain `pip install` on a modern runner grabs newer manylinux_2_28 wheels that fail to load on Lambda, surfacing for example as `bcrypt: no backends available` on the login path. The workflow forces compatible wheels:

```bash
pip install -r requirements.txt -t package/ \
  --platform manylinux2014_x86_64 \
  --implementation cp \
  --python-version 3.11 \
  --only-binary=:all:
```

The handler is `main.handler` (the Mangum-wrapped FastAPI app). A local Docker build against `public.ecr.aws/lambda/python:3.11` is a fallback that naturally produces runtime-compatible binaries.

### Frontend: `.github/workflows/deploy-frontend.yml`
Builds the React app (`npm ci`, then `npm run build` with `VITE_API_URL` set to the API domain), syncs `dist/` to the `fitjournal-frontend-prod` bucket with `--delete`, then creates a CloudFront invalidation so the new build is served immediately. It uses the same OIDC deploy role, extended with the S3 and CloudFront permissions it needs.

### Deploy role
An IAM OIDC identity provider trusts GitHub's token issuer, and the role (`fitjournal-github-actions-deploy`) is scoped by its trust policy to this repository's `main` branch. Its permissions are limited to updating the one Lambda function and to syncing and invalidating the frontend bucket and distribution. GitHub is the source of truth for code; deployed artifacts always derive from a commit, and code is never edited directly in the console.

## Operational notes and gotchas

- Schema changes: at startup the backend calls `Base.metadata.create_all`, which creates missing tables but does not alter existing ones. Adding a column or changing a constraint on an existing table must be applied manually with a one-off SQL migration against the target database.
- Foreign keys on the live DB: several user-referencing foreign keys are not `ON DELETE CASCADE` in production (they predate the cascade rules in the models, and `create_all` does not alter them). Deleting a user therefore requires deleting the child rows first, or a migration to add the cascades. This is a known cleanup item.
- Email egress depends on the NAT Gateway. If it is removed or its route breaks, email sends fail with a socket error (`Cannot assign requested address`) while the rest of the app keeps working, because the send is best-effort and logged as a warning rather than failing the request.

## Key decisions and trade-offs

- React SPA on S3 + CloudFront for the web client, replacing the server-rendered Jinja app, which was removed once parity was reached. Static hosting is cheap and fast and cleanly separates the web UI from the API.
- `locales/` is packaged into the Lambda bundle and resolved relative to the module directory. The backend's own i18n (`src/i18n.py`, used for localized API error messages) reads catalogs from disk, and the bundle flattens `src/*.py` into the task root, so a path resolved only as a parent sibling silently finds nothing and `t()` falls back to returning the key.
- HTTP API over REST API: cheaper and simpler; FitJournal needs none of REST API's advanced features. (HTTP APIs support only TLS 1.0/1.2 policies on custom domains, which is why the domain uses `TLS_1_2`.)
- Mangum adapter instead of rewriting for Lambda: one codebase runs identically locally and in production.
- DNS at Cloudflare, not Route 53: Cloudflare is free and already hosts the domain; the API CNAME is DNS-only (not proxied) so API Gateway certificate validation and SNI work correctly.
- OIDC over stored AWS keys: GitHub Actions assumes a scoped IAM role via short-lived tokens; no long-lived secrets live in the repo.
- Resend for transactional email, chosen after AWS SES production access was declined twice. Sending is abstracted in `src/emailer.py`, so switching providers (including back to SES later) is a one-file change.
- NAT Gateway for Lambda internet egress (temporary): a VPC-attached Lambda has no route to the internet by default, and it needs one to call Resend. The NAT Gateway is the one non-free component. If SES production access is later approved, SES can be reached from inside the VPC through a VPC endpoint with no NAT, at which point the NAT Gateway can be removed.
- `bcrypt` pinned to 4.0.1, installed as a manylinux2014 wheel: newer 5.x releases are incompatible with the (unmaintained) `passlib` 1.7.4 backend initialization, and the wheel must target Lambda's older glibc. Both are required to avoid a runtime crash on the login path.
- Local MySQL for development: keeps dev location-independent and lets RDS stay locked to the Lambda only.

## Cost

The stack was designed to run near $0/month at personal scale (HTTP API over REST, `db.t4g.micro`, a static frontend on free-tier S3 and CloudFront). The one exception now is the NAT Gateway, which bills roughly $32/month plus data and is the price of giving the VPC Lambda internet egress for email. It is intended as a temporary measure: if SES production access is approved, the NAT Gateway can be dropped (SES is reachable through a VPC endpoint) and the stack returns to near $0.
