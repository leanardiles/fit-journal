# FitJournal Design System: Android

> **Version:** 2.0 — October 2026
> **Author:** Leandro Ardiles
> **Scope:** the Android app (Kotlin, Jetpack Compose) only.

**The web design system is not documented here.** Its values live in
`web-react/src/styles/tokens.css`, which is the single source of truth for
colour, type, spacing and radii, and its rules (breakpoints, responsive and
adaptive behaviour, accessibility) live in `docs/ui-architecture.md`.

> This file was split out of a combined `design-system.md` that tried to cover
> both platforms. The web half had drifted badly, because it restated values
> that `tokens.css` already owns, and a table in Markdown and a table in CSS
> always diverge. Values the code holds are not repeated here; this file
> documents the Android app, where Compose has no equivalent token file.

---

## 1. Design philosophy

Shared across both platforms. FitJournal's visual identity is the metaphor of a
**physical workout notebook**, the kind athletes and coaches have used for
decades. The interface should feel familiar, tactile and free of unnecessary
decoration.

- **No frills.** Every element earns its place. If it does not help the user
  track their workout, it does not exist.
- **Analog warmth.** Ruled lines, a handwritten font and a red margin line
  create the feeling of a real notebook.
- **Readable first.** The handwritten aesthetic must never compromise
  legibility.
- **Consistency over creativity.** Elements align to a grid. Nothing floats.

---

## 2. Layout

The mobile app uses a **bottom navigation bar**. The web sidebar pattern does
not translate to small screens.

```
┌─────────────────────────┐
│   TOP BAR               │
│   [← back]  User  [⚙]  │
├─────────────────────────┤
│                         │
│   SCREEN CONTENT        │
│   (scrollable)          │
│                         │
├─────────────────────────┤
│  📅  🏋  🏠  ⏱  WOD   │
│   BOTTOM NAV BAR        │
└─────────────────────────┘
```

| Zone | Notes |
|---|---|
| Top bar | `ProfileTopBar.kt`: user name, back button on sub-screens, dropdown menu (Routine, Settings, Logout) |
| Content | Compose `Column` or `LazyColumn`, dark background `#1B1B1E` |
| Bottom nav | 5 items: Calendar, Exercises, Home, Timer, WOD |

---

## 3. Colour palette

Dark only in the current version.

| Name | Value | Usage |
|---|---|---|
| Background | `#1B1B1E` | Screen background |
| Surface | `#2C2C2E` | Cards, input fields |
| Surface raised | `#3A3A3C` | Drag state, elevated cards |
| Accent yellow | `#FFEB3B` | Primary CTA, active states, current day highlight |
| Text primary | `#FFFFFF` | Primary text |
| Text secondary | `#8E8E93` | Labels, secondary info |
| Error | `#FF453A` | Error messages |
| Success | `rgba(100,180,100,0.8)` | Success messages |

---

## 4. Typography

**Font family:** custom `myCustomFont`, loaded as a TTF asset.
**Fallback:** system sans-serif.

| Level | Size | Usage |
|---|---|---|
| Screen title | `28sp` | Page titles |
| Section header | `18sp` | Card titles |
| Body | `15-16sp` | Exercise names, routine days |
| Label or tag | `11-13sp` | Muscle group tags, section labels |
| Caption | `12sp` | Timestamps, secondary info |

Sizes are in `sp`, so they scale with the user's system font setting. Layouts
must survive that scaling.

---

## 5. Components (Jetpack Compose)

| Component | Implementation | Notes |
|---|---|---|
| Cards | `Card`, `RoundedCornerShape(12dp)`, `containerColor = #2C2C2E` | Exercises, dashboard modules |
| Primary button | Yellow `#FFEB3B`, black text, `RoundedCornerShape(12dp)` | Create Workout, Mark Complete |
| Outlined button | Transparent, red border `rgba(150,30,30,0.40)` | Edit Routine, Cancel |
| Toggle buttons | Row of outlined buttons; selected adds a yellow border and fill | Sex, unit preference |
| Drag handle | `Icons.Filled.DragHandle`, grey, long press to activate | Workout exercise reorder |
| Checkbox | `Icons.Outlined.Circle` to `Icons.Filled.CheckCircle` | Exercise completion |
| Loading | `CircularProgressIndicator`, yellow | All loading states |
| Error state | Red text plus a Retry button | Exercises, Routine, Workout |

---

## 6. Spacing

| Usage | Value |
|---|---|
| Screen padding | `16dp` |
| Card padding | `16dp` |
| Between cards | `16dp` |
| Between elements | `8-12dp` |
| Icon size | `20-24dp` |

---

## 7. Imagery and icons

**Logo:** `logo_only.png`, the kettlebell mark, on a transparent background.
Used on the login screen and as the app launcher icon across all mipmap
densities.

**Icons:** Material Icons (`androidx.compose.material.icons`), with
`material-icons-extended` for the rest. Outlined by default, Filled for active
or completed states.

- `Icons.Outlined.Circle`: unchecked exercise
- `Icons.Filled.CheckCircle`: checked exercise
- `Icons.Filled.DragHandle`: reorder handle
- `Icons.Filled.AccountCircle`: profile

---

## 8. Motion

- `CircularProgressIndicator` for loading states.
- Compose defaults for navigation transitions.
- No custom page transitions. Keep it simple.

---

## 9. Platform differences

| Aspect | Web | Android |
|---|---|---|
| Navigation | Top header; a bottom tab bar replaces it below 600 px | Bottom nav bar, 5 tabs |
| Header | Logo, user name and logout; wraps under text expansion | `ProfileTopBar` with a dropdown |
| Font | Patrick Hand (Google Fonts) | Custom TTF asset |
| Theme | Dark and light | Dark only |
| Auth storage | JWT in `localStorage` | JWT in `EncryptedSharedPreferences` |
| Token injection | Through the shared `api/client` wrappers | Automatic, via `AuthInterceptor` |
| Layout system | CSS baseline grid (`--lh: 34px`), mobile-first at 600 and 1024 px | Compose `Column` and `LazyColumn` |
| Accent colour | Red `rgba(192,57,43,0.85)` | Yellow `#FFEB3B` |
| Locales | en, es-419, es-AR, nl, fr | Subset, migrating |

**On the accent colour difference:** web uses red, consistent with the
notebook margin line. Android uses yellow, settled early, before the notebook
redesign. Unifying them is a future enhancement, and the web app's use of
yellow for sticky notes and primary buttons is the likelier meeting point.

---

## 10. Known gaps

- Light mode is web only.
- The accent colour differs between platforms (see above).
- Android has no token file equivalent to `tokens.css`, so values here are
  documented rather than derived. A Compose theme object would let this file
  shrink the way the web half did.
