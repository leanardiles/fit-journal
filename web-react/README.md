# FitJournal , React Frontend (`web-react`)

Internationalized React frontend for FitJournal, built with Vite and
`react-i18next`. Lives alongside the Python backend and the legacy Jinja
frontend in the same monorepo.

> Monorepo note: there are two `src/` folders , the backend Python code is at
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

## Scripts

| Command                   | What it does                                   |
|---------------------------|------------------------------------------------|
| `npm run dev`             | App dev server (Vite)      -> localhost:5173    |
| `npm run build`           | Production build of the app                    |
| `npm run preview`         | Preview the production build                   |
| `npm run lint`            | Run ESLint                                     |
| `npm run storybook`       | Component explorer (Storybook) -> localhost:6006 |
| `npm run build-storybook` | Static Storybook build                         |

The app (5173) and Storybook (6006) run independently, on different ports.

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
expansion , the core "scalable UI" check.

Run `npm run storybook` and open http://localhost:6006.