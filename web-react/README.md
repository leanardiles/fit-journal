# FitJournal: React Frontend (`web-react`)

The internationalized React frontend for FitJournal, built with Vite and
`react-i18next`. This is the **live production web app**, served at
[fit-journal.com](https://fit-journal.com) from S3 + CloudFront. It lives
alongside the Python backend and the legacy Jinja frontend in the same
monorepo.

> Monorepo note: there are two `src/` folders. The backend Python code is at
> the repo-root `src/`, and this React app's code is in `web-react/src/`.

## Prerequisites

- Node.js 18+ (built with Node 24)
- npm

## Getting started

```bash
cd web-react
npm install        # install dependencies (first time only)
npm run dev        # start the app dev server  -> http://localhost:5173
```

The dev server reads its API base URL from `.env.development`
(`http://localhost:8000/v1` by default), so start the backend alongside it.
See the [root README](../README.md) for backend setup.

## Scripts

| Command                   | What it does                                   |
|---------------------------|------------------------------------------------|
| `npm run dev`             | App dev server (Vite)      -> localhost:5173    |
| `npm run build`           | Production build of the app                    |
| `npm run preview`         | Preview the production build                   |
| `npm run lint`            | Run ESLint                                     |
| `npm test`                | Vitest unit tests (run once)                   |
| `npm run test:watch`      | Vitest unit tests in watch mode                |
| `npm run test:e2e`        | Playwright end-to-end tests                    |
| `npm run test:e2e:ui`     | Playwright tests in the interactive UI runner  |
| `npm run storybook`       | Component explorer (Storybook) -> localhost:6006 |
| `npm run build-storybook` | Static Storybook build                         |

The app (5173) and Storybook (6006) run independently, on different ports.

## Testing

Two layers of tests:

- **Unit (Vitest):** the unit-conversion helpers in `src/constants/units.js`
  (kg to display, display to kg, kg/lb, cm to feet/inches and back), which
  guard the canonical-kg storage model. Run with `npm test`.
- **End-to-end (Playwright):** a happy path in `e2e/` that seeds a throwaway
  user via the API, logs in through the UI, and verifies every protected page
  renders. Run with `npm run test:e2e` (needs the backend running; it reads
  `API_URL`, default `http://localhost:8000/v1`).

```bash
npm test           # Vitest unit tests
npm run test:e2e   # Playwright E2E (backend must be running)
```

Vitest is configured with two projects in `vite.config.js`: a `unit` project
(node environment, `src/**/*.{test,spec}.{js,jsx}`) and the Storybook browser
project.

## Internationalization (i18n)

i18n is embedded from the start with `react-i18next`. Adding a language is a
data change (a new catalog file), not a code change.

### Locale model

Base `es` holds general Latin American Spanish; regions override from it and
inherit the rest via an explicit fallback chain (mirrors the backend model).

| Locale | Role                              | Fallback chain      |
|--------|-----------------------------------|---------------------|
| `en`   | Base + ultimate fallback          | `en`                |
| `es`   | Base Spanish (general LatAm)      | `es -> en`          |
| `es-AR`| Argentina overrides (e.g. voseo)  | `es-AR -> es -> en` |
| `nl`   | Dutch                             | `nl -> en`          |

Regional files are THIN: they contain only the strings that genuinely differ;
everything else cascades down the chain. This keeps overrides small and makes
the catalogs friendly to translation tools (CAT/TMS).

### Structure

```
web-react/src/i18n/
  config.js            # i18next init: resources + explicit fallback chains
  locales/
    en/common.json     # base (full)
    es/common.json     # LatAm Spanish (full)
    es-AR/common.json  # Argentina (thin overrides only)
    nl/common.json     # Dutch (full)
```

### Using translations in a component

```jsx
import { useTranslation } from "react-i18next";

function Example() {
  const { t } = useTranslation();
  return <p>{t("login.title")}</p>;
}
```

Components take a translation KEY (e.g. `labelKey="login.button"`) rather than
literal text, so localization is inherited by composition.

### Adding a locale

1. Create `src/i18n/locales/<tag>/common.json` (thin if it's a regional variant).
2. Register it in `src/i18n/config.js` (`resources` + `fallbackLng` chain).

No component changes required.

## Components & Storybook

Reusable, i18n-aware components live in `src/components/`. Each is documented in
Storybook, including rendering across locales to verify layout holds under text
expansion, which is the core "scalable UI" check.

Run `npm run storybook` and open http://localhost:6006.

## Production & deployment

The app is a static Vite build hosted on S3 and served through CloudFront
(ACM TLS, Cloudflare DNS) at [fit-journal.com](https://fit-journal.com). It
calls the backend API cross-origin at `app.fit-journal.com`, allowed via CORS
on the backend.

```bash
npm run build      # outputs the static site to dist/
```

The build reads its API base URL from `.env.production`. CloudFront is
configured to rewrite 403/404 responses to `/index.html` with a 200 so client
side routing works on deep links. Full deployment reasoning is in
[docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md).