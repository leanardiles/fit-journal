# Architecture

This document describes how FitJournal is structured and how it's deployed. For the data model, see [SCHEMA.md](SCHEMA.md).

## System overview

FitJournal is a single FastAPI backend that serves two clients — a server-rendered web app and a native Android app — and is deployed serverless on AWS.

```
   Web browser ─┐
                ├─► https://app.fit-journal.com
 Android app ───┘            │
                             ▼
                  ┌────────────────────┐
                  │   Cloudflare DNS    │  CNAME (DNS-only) → API Gateway
                  └─────────┬───────────┘
                            ▼
                  ┌────────────────────┐
                  │  API Gateway        │  HTTP API, ANY /{proxy+}
                  │  (HTTPS via ACM)    │  custom domain app.fit-journal.com
                  └─────────┬───────────┘
                            ▼
              ┌─────────────────────────────────┐
              │  VPC (default)                  │
              │  ┌────────────────────┐         │
              │  │  AWS Lambda         │  FastAPI via Mangum
              │  │  Python 3.11, x86   │  (in VPC, no NAT)
              │  └─────────┬──────────┘          │
              │            ▼ private (SG → SG)   │
              │  ┌────────────────────┐         │
              │  │  AWS RDS (MySQL)    │  MySQL 8.4.9, db.t4g.micro
              │  │  inbound 3306 from  │  reachable only by the Lambda SG
              │  │  the Lambda SG only │         │
              │  └────────────────────┘         │
              └─────────────────────────────────┘
```

Both clients talk to the same backend over HTTP/JSON, authenticated with JWT. The backend is the only component that knows about the database; clients never connect to it directly. Lambda reaches RDS over a private path inside the VPC — RDS is not reachable from the internet.

## Components

### Backend — FastAPI on AWS Lambda
The backend is a FastAPI application that serves both JSON API endpoints (consumed by both clients) and server-rendered HTML pages via Jinja2 templates (the web app). It runs on AWS Lambda using [Mangum](https://github.com/jordaneremieff/mangum), an adapter that translates API Gateway events into the ASGI requests FastAPI expects. The same codebase runs unchanged under `uvicorn` locally and under Lambda in production.

Lambda is attached to the default VPC (across three availability-zone subnets) so it can reach RDS privately. It needs no outbound internet access, so there is no NAT Gateway — keeping the setup at $0.

### API layer — API Gateway HTTP API
An API Gateway **HTTP API** (chosen over REST API for lower cost and simpler config) sits in front of Lambda. A single catch-all route — `ANY /{proxy+}` plus `ANY /` — forwards every request to Lambda, letting FastAPI handle all routing internally. The `$default` stage serves the API at the domain root.

### Database — AWS RDS MySQL
A managed MySQL 8.4.9 instance (`db.t4g.micro`, single-AZ, free tier). The backend connects via SQLAlchemy + PyMySQL with SSL. Network access is restricted to Lambda only (see [Networking & security](#networking--security)). See [SCHEMA.md](SCHEMA.md) for the data model.

### Web client — server-rendered, no framework
The web app is rendered server-side with Jinja2 and styled with a custom `notebook.css` (no CSS framework). Client-side logic is vanilla JavaScript. All pages extend a shared `base.html` with a single sidebar partial, so navigation is defined once. `API_URL` is derived from `window.location.origin`, so the same code works at `localhost`, the raw API Gateway URL, or the custom domain with no changes.

### Mobile client — native Android (Kotlin)
A native Android app built with Jetpack Compose and an MVI architecture, sharing the same FastAPI backend. JWTs are stored encrypted via the Android Keystore and injected into requests by an OkHttp interceptor.

## Authentication

Stateless JWT across both platforms:

1. Login issues a JWT (30-day expiry), signed with a server-side secret (`SECRET_KEY`, supplied via environment variable).
2. **Web** stores the token in `localStorage` and attaches it to API calls.
3. **Mobile** stores the token encrypted on-device and injects it via an OkHttp `AuthInterceptor`.
4. Protected endpoints require a valid Bearer token; there is no server-side session to invalidate.

Unauthenticated visitors are routed to the login page; clients check for a valid token before loading protected pages and redirect cleanly if none is present.

## Networking & security

RDS is **not reachable from the internet**. Access is controlled entirely with security-group referencing inside the VPC:

- **Lambda** runs in the default VPC with its own security group (`fitjournal-lambda-sg`).
- **RDS's** security group allows inbound on port 3306 *only* from the Lambda security group — not from any IP range.
- There is no NAT Gateway; Lambda has no route to the public internet, only the private path to RDS. A passing connection from Lambda therefore proves the private path works.

This replaced an earlier interim setup where RDS allowed inbound `0.0.0.0/0` (added during initial deployment so Lambda, then outside the VPC, could connect). That public rule and a stale home-IP rule have both been removed.

> RDS still has "Publicly accessible: Yes" set, so it retains a public IP — but with no security-group rule permitting internet traffic, nothing can use it. Flipping it to "No" to drop the public IP entirely is tracked as a low-priority defense-in-depth tidy-up in [ROADMAP.md](../ROADMAP.md).

## Configuration

The backend reads all configuration from environment variables: database connection (`DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`, `DB_SSL`) and the JWT signing secret (`SECRET_KEY`).

- **Locally**, these come from a gitignored `src/.env` file (loaded via `python-dotenv`) and point at a **local MySQL** instance (`127.0.0.1`). Developing against local MySQL keeps the dev environment location-independent — it does not depend on whitelisting a changing home IP against RDS, and never touches production data.
- **On Lambda**, the same variables are set as Lambda environment variables and point at RDS.

`load_dotenv()` is a no-op when no `.env` is present, so the identical code path works in both environments. Local and production are fully separate databases; the same `SECRET_KEY` is used in both so tokens are consistent.

## Deployment

Deployment is automated with **GitHub Actions** (`.github/workflows/deploy.yml`). The pipeline builds the Lambda package on a clean Linux runner, zips it, and updates the function — authenticated to AWS via **OpenID Connect (OIDC)**, so no AWS credentials are stored in GitHub.

**Pipeline steps:**
1. Check out the repo and set up Python 3.11.
2. Install dependencies into `package/` (see the manylinux note below).
3. Copy application code (`src/*.py`), templates, and static assets into `package/`.
4. Zip the package.
5. Assume the deploy IAM role via OIDC (`aws-actions/configure-aws-credentials`).
6. `aws lambda update-function-code` with the zip, then wait for the update to finish.

**Authentication (OIDC + least privilege):** an IAM OIDC identity provider trusts GitHub's token issuer, and a role (`fitjournal-github-actions-deploy`) is scoped — via its trust policy — to this repository's `main` branch only. The role's permissions are limited to `lambda:UpdateFunctionCode`, `lambda:GetFunction`, and `lambda:GetFunctionConfiguration` on the single `fitjournal-api` function. If the credentials ever leaked, the worst possible action is updating that one function's code.

**Trigger:** currently manual (`workflow_dispatch` — a "Run workflow" button), run from `main`. Switching to auto-deploy on every push to `main` is a one-line change (uncommenting a `push:` block) and is planned once a backend test gate exists to gate deploys.

**The manylinux requirement (important):** Lambda's Python 3.11 runtime is Amazon Linux 2 (glibc 2.26). Packages with compiled binaries (`bcrypt`, `cryptography`, `pydantic-core`) must be installed as **manylinux2014** wheels (glibc 2.17), which load on that runtime. A plain `pip install` on a modern Linux runner grabs newer `manylinux_2_28` wheels (glibc 2.28+) that fail to load on Lambda — surfacing, for example, as `bcrypt: no backends available` on the login path. The workflow therefore forces compatible wheels:

```bash
pip install -r requirements.txt -t package/ \
  --platform manylinux2014_x86_64 \
  --implementation cp \
  --python-version 3.11 \
  --only-binary=:all:
```

**Manual fallback:** the package can also be built locally with Docker against the Lambda image (`public.ecr.aws/lambda/python:3.11`), which naturally produces runtime-compatible binaries, then zipped and uploaded via the console. This was the original deployment method and remains a useful fallback for emergency restores.

The Lambda handler is `main.handler` (the Mangum-wrapped FastAPI app). GitHub is the source of truth for code — deployed artifacts always derive from a commit, and code is never edited directly in the Lambda console. Local development is unaffected — `uvicorn main:app` runs the same code.

## Key decisions and trade-offs

- **HTTP API over REST API** — cheaper and simpler; FitJournal needs none of REST API's advanced features. (HTTP APIs support only TLS 1.0/1.2 policies on custom domains, which is why the domain uses `TLS_1_2`.)
- **Mangum adapter instead of rewriting for Lambda** — keeps a single codebase that runs identically locally and in production.
- **DNS at Cloudflare, not Route 53** — Cloudflare is free and already hosts the domain; the `app` CNAME is set to *DNS-only* (not proxied) so API Gateway's certificate validation and SNI work correctly.
- **OIDC over stored AWS keys** — GitHub Actions assumes a scoped IAM role via short-lived tokens; no long-lived secrets live in the repo.
- **Lambda in VPC, no NAT Gateway** — Lambda needs only the private path to RDS, not the public internet, so it can join the VPC without a (paid) NAT Gateway. Security improves at no added cost.
- **`bcrypt` pinned to 4.0.1, installed as a manylinux2014 wheel** — newer 5.x releases are incompatible with the (unmaintained) `passlib` 1.7.4 backend initialization. Separately, the wheel must target Lambda's older glibc (see the deployment section). Both are required to avoid a runtime crash on the login path.
- **Local MySQL for development** — keeps dev location-independent and lets RDS be locked down to Lambda only, rather than whitelisting a changing developer IP.

## Cost

The architecture is designed to run at effectively $0/month at personal scale — a deliberate constraint that drove several decisions (HTTP API over REST, no NAT Gateway, db.t4g.micro). All components sit within AWS free-tier limits.