# FitJournal

A self-hosted fitness tracker with a paper-notebook aesthetic, available as a web app and a native Android app, sharing one serverless backend. The interface is fully internationalized (English, Spanish, Argentine Spanish, Dutch).

> 🎥 **Demo video coming soon.**
>
> 🔗 **Live app:** [fit-journal.com](https://fit-journal.com)

<br>

> _Fresh screenshots of the React app are on the way. In the meantime, the app is live at [fit-journal.com](https://fit-journal.com)._

---

## Overview

FitJournal lets you build a weekly training routine, generate daily workouts, log what you actually did, and review your history. It is a full-stack project: a React single-page web app and a native Android client, both talking to one FastAPI backend deployed serverless on AWS.

Two things set it apart. The interface is a "paper notebook" design system (ruled lines, handwritten font, red margin line) applied consistently across the app, with light and dark modes. And it is internationalized from the ground up: an explicit BCP 47 locale model with fallback chains (`es-AR` to `es` to `en`), so adding a language is a data change, not a code change.

## Tech Stack

**Web (React SPA, live at [fit-journal.com](https://fit-journal.com))**
- React 19 + Vite, React Router
- `react-i18next` internationalization (en / es / es-AR / nl) with explicit fallback chains
- Reusable component library documented in Storybook
- Hosted on S3 + CloudFront, see [web-react/README.md](web-react/README.md)

**Legacy web (Jinja, being retired)**
- Jinja2 + vanilla JavaScript, custom `notebook.css` (no framework)
- Still served by the backend at `app.fit-journal.com` until the React app is cut over and it is removed

**Backend**
- FastAPI (Python), SQLAlchemy 2.0 ORM, Pydantic
- MySQL (PyMySQL driver), JWT auth, bcrypt password hashing
- Backend i18n runtime (BCP 47 locale model, UTF-8 end to end)

**Android**
- Kotlin, Jetpack Compose, MVI architecture
- Retrofit + OkHttp, Room, encrypted token storage (Android Keystore)

**Infrastructure (AWS + Cloudflare)**
- Frontend: S3 + CloudFront + ACM (TLS) at `fit-journal.com`
- Backend: Lambda (FastAPI via Mangum), API Gateway (HTTP API), RDS MySQL at `app.fit-journal.com`
- Cloudflare DNS (apex plus a `www` redirect); the SPA and API are separate origins with CORS

## Architecture

```
Browser (React SPA) ──►  fit-journal.com  ──►  CloudFront ──► S3 (static SPA)
                                │
                                └── fetch, CORS ──►  app.fit-journal.com
Android app ────────────────────────────────────►  app.fit-journal.com
                                                     ──► API Gateway (HTTP API)
                                                     ──► Lambda (FastAPI via Mangum)
                                                     ──► RDS MySQL
```

Both clients talk to the same JWT-authenticated API; only the backend touches the database. Full detail in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Features

- Fully internationalized UI (English, Spanish, Argentine Spanish, Dutch) with locale fallback chains
- Custom training routines (1 to 7 days/week, multiple muscle groups per day)
- Automatic workout generation that rotates through least-used exercises
- Per-exercise workout logging with weight tracking over time (canonical kg storage, metric or imperial display)
- Workout history calendar with multi-day filtering and per-day grouping
- Personal exercise library (101 starter exercises, fully editable per user)
- JWT authentication shared across web and mobile
- Timezone-aware logging; metric/imperial unit preferences
- Notebook-style UI with light and dark modes
- Android app with drag-to-reorder, inline editing, and offline-aware token handling

## Running Locally

**Prerequisites:** Python 3.8+, Node.js 18+, a MySQL 8.0+ instance, Git. (Android Studio for the mobile app, see [README_mobile.md](README_mobile.md).)

**Backend (API)**

```bash
# 1. Clone
git clone https://github.com/leanardiles/fit-journal.git
cd fit-journal

# 2. Virtual environment
python -m venv venv
source venv/Scripts/activate     # Windows (Git Bash)
# source venv/bin/activate        # macOS / Linux

# 3. Dependencies
pip install -r requirements.txt

# 4. Environment: create a .env in the project root
#    DB_HOST, DB_PORT, DB_USER, DB_PASSWORD, DB_NAME, DB_SSL, SECRET_KEY

# 5. Run
cd src
uvicorn main:app --reload
```

API docs: `http://127.0.0.1:8000/docs` · legacy Jinja web: `http://127.0.0.1:8000/web/login`

**Web frontend (React)**

```bash
cd web-react
npm install
npm run dev        # http://localhost:5173
```

The React dev server reads its API base URL from `web-react/.env.development` (`http://localhost:8000/v1` by default), so run the backend alongside it.

> The `.env` files are gitignored. Any MySQL 8.0+ instance works (local or managed).

## Deployment

**Backend** runs on AWS Lambda behind API Gateway, deployed via a GitHub Actions pipeline: it builds the package (forcing Lambda-compatible `manylinux2014` wheels), runs the test suite, and only if tests pass deploys to Lambda using short-lived OIDC credentials (no stored AWS keys).

**Frontend** is a static Vite build hosted on S3 and served through CloudFront (ACM TLS, Cloudflare DNS) at `fit-journal.com`. It calls the API cross-origin at `app.fit-journal.com`, allowed via CORS.

Full pipeline and architecture reasoning in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Testing

**Backend:** a pytest suite (64 tests) covers authentication, authorization, exercise and routine CRUD (including exercise updates and canonical-kg weight round-trips), profile, workout generation and completion, next-workout selection toggle and clear, session and calendar reads, the routine reconcile logic, manual-day exercise grouping, manual (off-routine) workout logging, day-cursor handling, account deletion, and page loads, all run against a disposable MySQL test database. The suite runs in CI (GitHub Actions) against a MySQL service container before every deploy; a failing test blocks the deploy.

```bash
pip install pytest httpx
pytest -v        # backend suite (needs a MySQL test DB)
```

**Web (React):** Vitest unit tests for the unit-conversion helpers, plus a Playwright end-to-end happy path (seed a user via the API, log in through the UI, and verify every protected page renders).

```bash
cd web-react
npm test           # Vitest unit tests
npm run test:e2e   # Playwright E2E (needs the backend running)
```

**Android:** 83% unit test coverage (JaCoCo), covering ViewModels and repository logic through fake implementations, plus two instrumented UI tests (login happy path and error path).

```bash
cd mobile
./gradlew testDebugUnitTest jacocoTestReport     # unit tests + coverage
./gradlew connectedDebugAndroidTest              # UI tests (needs an emulator)
```

## Documentation

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): system design, deployment, key decisions
- [docs/SCHEMA.md](docs/SCHEMA.md): database schema
- [docs/design-system.md](docs/design-system.md): UI/UX design system
- [web-react/README.md](web-react/README.md): React frontend setup and i18n
- [README_mobile.md](README_mobile.md): Android app setup
- [ROADMAP.md](ROADMAP.md): planned features and technical debt

## Status

Actively developed. The React web app is **live in production** at [fit-journal.com](https://fit-journal.com) (S3 + CloudFront), internationalized in English, Spanish, Argentine Spanish, and Dutch, with a Storybook component library. The FastAPI backend runs on AWS Lambda with a CI/CD pipeline (GitHub Actions) that runs the backend test suite before each deploy.

The legacy Jinja web frontend is being retired now that the React app is live; the backend still serves it at `app.fit-journal.com` until the cutover is complete. The Android app is being migrated to the new routine model (the web app now builds routines day-by-day, each day either a rotating per-muscle pool or a fixed manual list with optional supersets, plus off-routine manual logging), then Play Store launch prep. See [ROADMAP.md](ROADMAP.md).

## License

MIT license. See [LICENSE](LICENSE).