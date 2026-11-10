# Security Policy

FitJournal is a personal fitness-tracking application: a FastAPI backend on AWS
Lambda, a React web app at [fit-journal.com](https://fit-journal.com), and an
Android client. It stores an email address, a password hash, and the workout
data a user enters.

## Reporting a vulnerability

Email **info@fit-journal.com** with a description of the issue and the steps to
reproduce it. Please do not open a public GitHub issue for a security report.

Expect an acknowledgement within 5 working days. This is a single-maintainer
project, so there is no bounty programme and no guaranteed remediation window,
but reports are taken seriously and fixed in priority order.

Please do not run automated scanners, load tests, or brute-force attempts
against the production hosts. If you want to test actively, say so in your
first email and run it locally instead; the repository includes everything
needed to stand the stack up.

### In scope

- The API at `app.fit-journal.com`
- The web app at `fit-journal.com`
- This repository

### Out of scope

- The AWS, Cloudflare and Resend services the project runs on; report those to
  the providers
- Findings that require a compromised user device or browser extension
- Missing hardening that carries no exploit path, unless you can show the path

## What the application does today

Stated so a reviewer knows what to expect, and what not to.

**Authentication.** Passwords are hashed with bcrypt through passlib; plaintext
is never stored or logged. Sign-in requires a verified email address. Sessions
are JWTs signed with HS256, and the signing key is required configuration, so
the application refuses to start without it.

**Authorization.** Every user-scoped endpoint depends on a guard that compares
the `user_id` in the request against the authenticated user and returns 403 on
a mismatch, so one account cannot read or write another's data. This is covered
by automated tests (`tests/test_authorization.py`).

**Email verification and password reset.** Codes and reset tokens are stored
hashed, expire on a fixed TTL, are single-purpose, and are limited to five
attempts before being refused. The endpoints that accept an email address
return the same response whether or not the account exists, so they cannot be
used to discover which addresses are registered.

**Transport and data.** HTTPS everywhere, with a TLS 1.2 minimum on the API's
custom domain. The database is a private RDS instance with no public endpoint,
reachable only from the application's VPC.

**Input handling.** All request bodies are validated by Pydantic models, and
all database access goes through the SQLAlchemy ORM with bound parameters.

**Account deletion.** A user can delete their account and all associated data,
re-authenticating with their password; related records are removed by database
cascade.

**Secrets.** Configuration comes from the environment. No credentials are
committed to this repository, and deployment credentials are issued to CI
through GitHub OIDC rather than stored as long-lived keys.

**Dependencies.** Pinned in `requirements.txt` and `package-lock.json`, with
automated update pull requests enabled.

## Not claimed

This is a portfolio project maintained by one person. It has not had a
third-party penetration test or a formal security review, and it is not
certified against any standard. Treat the data in it accordingly.
