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
                  ┌────────────────────┐
                  │  AWS Lambda         │  FastAPI app via Mangum adapter
                  │  Python 3.11, x86   │  (Python 3.11 runtime)
                  └─────────┬───────────┘
                            ▼
                  ┌────────────────────┐
                  │  AWS RDS (MySQL)    │  MySQL 8.4.9, db.t4g.micro
                  └────────────────────┘
```

Both clients talk to the same backend over HTTP/JSON, authenticated with JWT. The backend is the only component that knows about the database; clients never connect to it directly.

## Components

### Backend — FastAPI on AWS Lambda
The backend is a FastAPI application that serves both JSON API endpoints (consumed by both clients) and server-rendered HTML pages via Jinja2 templates (the web app). It runs on AWS Lambda using [Mangum](https://github.com/jordaneremieff/mangum), an adapter that translates API Gateway events into the ASGI requests FastAPI expects. The same codebase runs unchanged under `uvicorn` locally and under Lambda in production.

### API layer — API Gateway HTTP API
An API Gateway **HTTP API** (chosen over REST API for lower cost and simpler config) sits in front of Lambda. A single catch-all route — `ANY /{proxy+}` plus `ANY /` — forwards every request to Lambda, letting FastAPI handle all routing internally. The `$default` stage serves the API at the domain root.

### Database — AWS RDS MySQL
A managed MySQL 8.4.9 instance (`db.t4g.micro`, single-AZ, free tier). The backend connects via SQLAlchemy + PyMySQL with SSL. See [SCHEMA.md](SCHEMA.md) for the data model.

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

## Configuration

The backend reads all configuration from environment variables: database connection (`DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`, `DB_SSL`) and the JWT signing secret (`SECRET_KEY`).

- **Locally**, these come from a gitignored `src/.env` file (loaded via `python-dotenv`).
- **On Lambda**, the same variables are set as Lambda environment variables.

`load_dotenv()` is a no-op when no `.env` is present, so the identical code path works in both environments.

## Deployment

The backend is deployed as a Lambda zip package. Because some dependencies (`bcrypt`, `cryptography`, `pydantic-core`) ship compiled binaries, the package must be built against Lambda's Linux x86_64 runtime — not the local Windows environment. This is done with Docker:

```bash
# 1. Build dependencies inside a Lambda-compatible Linux image
docker run --rm --entrypoint /bin/bash \
  -v "$(pwd):/var/task" -w /var/task \
  public.ecr.aws/lambda/python:3.11 \
  -c "pip install -r requirements.txt -t package/"

# 2. Add application code, templates, and static assets
cp src/*.py package/
cp -r templates package/
cp -r static package/

# 3. Zip the package contents (everything at the zip root)
cd package && zip -r ../deployment.zip . && cd ..

# 4. Upload deployment.zip to Lambda (console or AWS CLI)
```

The Lambda handler is `main.handler` (the Mangum-wrapped FastAPI app). Local development is unaffected — `uvicorn main:app` runs the same code.

> GitHub remains the source of truth for code; deploying to Lambda is a separate step from `git push`. A `deploy.sh` script to automate the build-and-upload is a planned improvement.

## Key decisions and trade-offs

- **HTTP API over REST API** — cheaper and simpler; FitJournal needs none of REST API's advanced features. (HTTP APIs support only TLS 1.0/1.2 policies on custom domains, which is why the domain uses `TLS_1_2`.)
- **Mangum adapter instead of rewriting for Lambda** — keeps a single codebase that runs identically locally and in production.
- **DNS at Cloudflare, not Route 53** — Cloudflare is free and already hosts the domain; the `app` CNAME is set to *DNS-only* (not proxied) so API Gateway's certificate validation and SNI work correctly.
- **`bcrypt` pinned to 4.0.1** — newer 5.x releases are incompatible with the (unmaintained) `passlib` 1.7.4 backend initialization. Pinning avoids a runtime crash on the login path.
- **RDS temporarily open to the internet** — the RDS security group currently allows inbound `0.0.0.0/0` on 3306 so Lambda (running outside the VPC) can reach it. This is tracked as a priority hardening task (see [ROADMAP.md](../ROADMAP.md)): move Lambda into the VPC and restrict RDS to VPC-internal traffic.

## Cost

All components run within the AWS free tier at current usage — effectively $0/month. Lambda (1M requests/mo free), API Gateway HTTP API (1M requests/mo free for the first year), ACM certificates (free), and RDS `db.t4g.micro` (free tier for the first year) all fall under their respective limits for a personal-scale workload.