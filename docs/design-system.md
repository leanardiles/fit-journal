# FitJournal Design System

> **Version:** 1.1 — May 2026  
> **Author:** Leandro Ardiles  
> **Status:** Living document — updated as the product evolves

---

## 1. Design Philosophy

FitJournal's visual identity is rooted in the metaphor of a **physical workout notebook** — the kind athletes and coaches have used for decades. The interface should feel familiar, tactile, and free of unnecessary decoration.

**Core principles:**
- **No frills** — every element earns its place. If it doesn't help the user track their workout, it doesn't exist.
- **Analog warmth** — ruled lines, a handwritten font, and a red margin line create the feeling of a real notebook.
- **Readable first** — the handwritten aesthetic must never compromise legibility.
- **Consistency over creativity** — all elements align to a strict baseline grid. Nothing floats freely.

---

## 2. Layout

### 2.1 Web Layout

The web app uses a **two-column layout** that mirrors the structure of a physical notebook:

```
┌─────────────────────────────────────────────────────┐
│                    HEADER (3 lines tall)             │
│   [blank — no ruled lines]   logo center  user right │
├──────────────┬──────────────────────────────────────┤
│              │                                       │
│   SIDEBAR    │           MAIN CONTENT                │
│   200px      │           flex: 1                     │
│              │           [ruled lines]               │
│  [nav links] │                                       │
│  [○ toggle]  │                                       │
│              │                                       │
└──────────────┴──────────────────────────────────────┘
```

| Zone | Size | Notes |
|---|---|---|
| Header | `3 × line-height` (102px at default) | Blank — no ruled lines. Logo centered over right column. User name + logout top right. |
| Sidebar | `200px` fixed | Ruled lines continue from body. Separated from main by vertical red margin line. |
| Main content | `flex: 1` | Ruled lines. Scrollable. Padding `var(--lh) 48px calc(var(--lh) * 3)`. |
| Mode toggle | `18px circle` | Inside sidebar nav, below Get WOD link. |

### 2.2 Web — Login / Register Layout

Login and Register pages use a **centered full-page layout** — no sidebar, no header. Currently using a functional placeholder design. Full notebook-style redesign planned.

```
┌─────────────────────────────────┐
│                                 │
│         [logo + wordmark]       │
│                                 │
│         Email ___________       │
│         Password _________      │
│                                 │
│         [ Sign In ]             │
│                                 │
└─────────────────────────────────┘
```

### 2.3 Mobile Layout

The mobile app uses a **bottom navigation bar** — the sidebar pattern does not translate to small screens.

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
| Top bar | `ProfileTopBar.kt` — user name, back button on sub-screens, dropdown menu (Routine, Settings, Logout) |
| Content | Jetpack Compose `Column` or `LazyColumn`, dark background `#1B1B1E` |
| Bottom nav | 5 items: Calendar, Exercises, Home, Timer, WOD |

---

## 3. Baseline Grid

All vertical spacing on the web app is governed by a **baseline grid**. Every element's height must be a multiple of `--lh`.

| Variable | Default | Notes |
|---|---|---|
| `--fs` | `17px` | Base font size. User-adjustable in future. |
| `--lh` | `34px` | Line height = `--fs × 2`. One grid unit. |
| `--header-h` | `calc(--lh × 3)` | Header is always 3 grid units tall. |

**Rule:** Text must sit at the **bottom** of each grid row, not the top. All elements use `align-items: flex-end` with `padding-bottom: 4-5px` to achieve this.

**Why:** This ensures text baselines visually align with the ruled notebook lines, creating the illusion that the user is writing on the page.

---

## 4. Color Palette

### 4.1 Dark Mode (default)

| Token | Value | Usage |
|---|---|---|
| `--bg` | `#171717` | Page background |
| `--line-color` | `#242424` | Horizontal ruled lines |
| `--margin` | `rgba(150,30,30,0.40)` | Vertical red margin line (sidebar separator) |
| `--text` | `#cccccc` | Primary text |
| `--muted` | `#555555` | Secondary text, labels, inactive nav items |
| `--red` | `rgba(192,57,43,0.85)` | Accent — muscle group tags, today indicator, active dot |
| `--red-border` | `rgba(150,30,30,0.40)` | Button borders, underlines, input focus |
| `--active-bg` | `rgba(139,32,32,0.09)` | Selected state background (subtle) |
| `--border` | `rgba(255,255,255,0.06)` | Subtle borders, toggle button default state |

### 4.2 Light Mode

| Token | Value | Usage |
|---|---|---|
| `--bg` | `#f5f0e8` | Cream paper background |
| `--line-color` | `#a0b8d0` | Light blue-grey ruled lines (notebook paper feel) |
| `--margin` | `rgba(150,30,30,0.30)` | Red margin line (slightly more transparent) |
| `--text` | `#2a2a2a` | Primary text |
| `--muted` | `#999999` | Secondary text |
| `--red` | `rgba(160,40,40,0.85)` | Accent |
| `--red-border` | `rgba(150,30,30,0.35)` | Borders and underlines |
| `--active-bg` | `rgba(139,32,32,0.07)` | Selected state |
| `--border` | `rgba(0,0,0,0.08)` | Subtle borders |

### 4.3 Mobile Color Palette (dark only, current)

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

## 5. Typography

### 5.1 Web

**Font family:** [Patrick Hand](https://fonts.google.com/specimen/Patrick+Hand) (Google Fonts)  
**Rationale:** Handwritten feel, highly legible at small sizes, free and widely available.

| Level | Size | Color | Usage |
|---|---|---|---|
| Section label | `11px` | `--muted` | Uppercase, 1.5px letter-spacing. Category headers (e.g. "QUICK STATS") |
| Body / nav | `17px` (`--fs`) | `--text` | Default text, nav links, form values |
| Nav active | `17px` | `#e0e0e0` | Active nav item, red underline |
| Muted / label | `17px` | `--muted` | Form field labels, secondary info |
| Small tag | `11–12px` | `--red` | "← today", muscle group tags |
| Logo | `22px` | `#ffffff` | FitJournal wordmark in header |

**Note:** There is no `h1/h2/h3` hierarchy in the traditional sense. The notebook aesthetic uses section labels and body text only — titles are implied by position (top of page) rather than size.

### 5.2 Mobile

**Font family:** Custom — `myCustomFont` (loaded as TTF asset)  
**Fallback:** System sans-serif

| Level | Size | Usage |
|---|---|---|
| Screen title | `28sp` | Page titles (e.g. "Dashboard 💪") |
| Section header | `18sp` | Card titles |
| Body | `15–16sp` | Exercise names, routine days |
| Label/tag | `11–13sp` | Muscle group tags, section labels |
| Caption | `12sp` | Timestamps, secondary info |

---

## 6. Components

### 6.1 Web Components

#### Buttons

All buttons are transparent-background with a subtle red border. No filled backgrounds unless in saved state.

```css
/* Default */
border: 1px solid rgba(150,30,30,0.40)
color: #888
border-radius: 10px
height: calc(--lh - 8px)

/* Hover */
border-color: rgba(150,30,30,0.70)
color: #ccc
transition: all 0.12s

/* Active (click moment) */
transform: scale(0.97)
border-color: rgba(150,30,30,0.90)

/* Focus (keyboard) */
border-color: rgba(150,30,30,0.60)
outline: none

/* Saved state (post-action, session only) */
/* Applied via JS: button.classList.add('nb-btn-saved') */
background: rgba(139,32,32,0.20)
border-color: rgba(150,30,30,0.70)
color: #ccc
```

**Saved state behavior:** After a successful save action, the button receives the `nb-btn-saved` class. This persists for the current page session only — navigating away and returning resets the button to its default state. Applied to action buttons (Save, Generate, Complete) but not navigation buttons (Cancel, Edit).

#### Form Inputs

Inputs are borderless except for a bottom border — mimicking writing on a line.

```css
/* Default */
background: transparent
border: none
border-bottom: 1px solid rgba(255,255,255,0.10)
padding: calc(--lh - 22px) 4px 4px   /* pushes text to baseline */
cursor: text

/* Hover (editable fields only) */
border-bottom-color: rgba(255,255,255,0.20)

/* Focus */
border-bottom-color: rgba(150,30,30,0.50)

/* Readonly */
color: --muted
cursor: not-allowed
```

#### Toggle Buttons (Sex, Unit, Day numbers)

Small pill or rounded-square buttons for selection states.

```css
/* Default */
border: 1px solid rgba(255,255,255,0.06)
color: --muted
border-radius: 8px

/* Hover */
color: --text

/* Selected */
border-color: rgba(150,30,30,0.40)
color: #ccc
background: rgba(139,32,32,0.09)
```

#### Navigation Links

```css
/* Default */
color: --muted
height: var(--lh)
padding-left: 32px
font-size: calc(--fs + 2px)

/* Hover */
color: #aaa

/* Active */
color: #e0e0e0
text-decoration: underline
text-decoration-color: rgba(150,30,30,0.40)
text-underline-offset: 4px
```

#### Mode Toggle

```css
width: 18px
height: 18px
border-radius: 50%
margin: 8px 0 0 32px   /* aligns with nav padding */

/* Dark mode */
background: rgba(255,255,255,0.35)
border: 1px solid rgba(255,255,255,0.30)

/* Light mode */
background: #171717   /* exact dark mode bg color */
border: 1px solid rgba(0,0,0,0.30)
```

### 6.2 Mobile Components (Jetpack Compose)

| Component | Implementation | Notes |
|---|---|---|
| Cards | `Card` with `RoundedCornerShape(12dp)`, `containerColor = #2C2C2E` | Exercises, Dashboard modules |
| Primary button | Yellow background `#FFEB3B`, black text, `RoundedCornerShape(12dp)` | Create Workout, Mark Complete |
| Outlined button | Transparent, red border `rgba(150,30,30,0.40)` | Edit Routine, Cancel |
| Toggle buttons | Custom row of outlined buttons, selected = yellow border + bg | Sex, Unit preference |
| Drag handle | `Icons.Filled.DragHandle`, grey, long-press activates | Workout exercise reorder |
| Checkbox | `Icons.Outlined.Circle` → `Icons.Filled.CheckCircle` | Exercise completion |
| Loading | `CircularProgressIndicator`, yellow color | All loading states |
| Error state | Red text + Retry button | Exercises, Routine, Workout |

---

## 7. Spacing

### 7.1 Web — Baseline Grid Units

All spacing is expressed as multiples of `--lh` (34px at default size):

| Usage | Value |
|---|---|
| Between sections | `1 × --lh` (one blank line) |
| Header height | `3 × --lh` |
| Nav item height | `1 × --lh` (single-spaced) |
| Single content row | `1 × --lh` |
| Main content padding | `var(--lh) 48px calc(var(--lh) * 3)` |
| Sidebar padding left | `32px` |
| Bottom padding (main) | `3 × --lh` — prevents content from being cut off at bottom |

### 7.2 Mobile — Material Spacing

| Usage | Value |
|---|---|
| Screen padding | `16dp` |
| Card padding | `16dp` |
| Between cards | `16dp` |
| Between elements | `8–12dp` |
| Icon size | `20–24dp` |

---

## 8. Imagery & Icons

### 8.1 Logo

- **File:** `logo_only.png` (kettlebell icon)
- **Web usage:** Header, centered above main content area. Height `calc(--lh × 1.6)`. Stacked vertically with wordmark below.
- **Mobile usage:** Login screen, app launcher icon (all mipmap densities)
- **Background:** Always transparent
- **Opacity:** `0.88` in dark mode header (slight transparency softens the white)

### 8.2 Icons (Mobile)

- **Library:** Material Icons (`androidx.compose.material.icons`)
- **Extended pack:** `material-icons-extended` for additional icons
- **Style:** Outlined preferred, Filled for active/completed states
- **Examples:**
  - `Icons.Outlined.Circle` → unchecked exercise
  - `Icons.Filled.CheckCircle` → checked exercise
  - `Icons.Filled.DragHandle` → reorder handle
  - `Icons.Filled.AccountCircle` → profile

### 8.3 Web — No Icon Library

The web app uses **Unicode characters** as lightweight icons:
- `⊕` Add
- `✕` Close / checked
- `✎` Edit
- `✓` Save / complete
- `←` Back indicator

---

## 9. Motion & Transitions

### 9.1 Web

All interactive elements use a single transition duration:

```css
transition: all 0.12s ease
```

Applied to: button hover, nav link hover, input focus, toggle selection, mode toggle.

**No animations** — the notebook aesthetic is static. Motion is reserved for functional feedback only.

### 9.2 Mobile

- `CircularProgressIndicator` — loading states
- Compose default animations for navigation transitions
- No custom page transitions — keep it simple

---

## 10. Dark / Light Mode

### 10.1 Web

Mode is stored in `localStorage` under the key `nb-mode`.

```javascript
// Apply on every page load (before any rendering)
if (localStorage.getItem('nb-mode') === 'light') {
    document.body.classList.add('light');
}
```

Toggle is a small circle inside the sidebar nav, below the Get WOD link. No label needed — the visual change is immediate. The circle is lighter in dark mode and dark (`#171717`) in light mode.

**Key differences between modes:**

| Element | Dark | Light |
|---|---|---|
| Background | Near-black `#171717` | Cream `#f5f0e8` |
| Ruled lines | Dark grey `#242424` | Light blue-grey `#a0b8d0` |
| Text | `#cccccc` | `#2a2a2a` |
| Margin line | `rgba(150,30,30,0.40)` | `rgba(150,30,30,0.30)` |
| Toggle circle | `rgba(255,255,255,0.35)` | `#171717` |

### 10.2 Mobile

Dark mode only in current version. Light mode planned as future enhancement.

---

## 11. Platform Differences

| Aspect | Web | Mobile |
|---|---|---|
| Navigation | Left sidebar, always visible | Bottom nav bar, 5 tabs |
| Header | Full-width, 3 lines tall, blank | `ProfileTopBar` with dropdown |
| Font | Patrick Hand (Google Fonts) | Custom TTF asset |
| Theme | Dark + Light mode | Dark only |
| Auth mechanism | JWT in `localStorage` | JWT in `EncryptedSharedPreferences` |
| Token injection | Manual `authHeaders()` per fetch | Automatic via `AuthInterceptor` |
| Layout system | CSS baseline grid (`--lh: 34px`) | Jetpack Compose `Column`/`LazyColumn` |
| Color accent | Red `rgba(192,57,43,0.85)` | Yellow `#FFEB3B` |
| Template engine | Jinja2 (server-side) | N/A |

> **Note on accent color difference:** The web uses red as the primary accent (consistent with the notebook margin line metaphor). The mobile app uses yellow, which was established early in development before the notebook redesign. A future enhancement would unify these.

---

## 12. Web Tech Stack

| Layer | Technology | Notes |
|---|---|---|
| Backend | FastAPI (Python) | Serves both API and HTML |
| Templating | Jinja2 | Server-side HTML assembly |
| Static assets | FastAPI `StaticFiles` | Mounted at `/static` |
| CSS | Custom `notebook.css` | No CSS framework |
| JavaScript | Vanilla JS `api.js` | No framework |
| Auth (web) | JWT in `localStorage` | Manual header injection via `authHeaders()` |
| Database | MySQL on Aiven | SQLAlchemy ORM |
| Template structure | `base.html` + page templates | Sidebar in `partials/sidebar.html` |

**Template folder structure:**
```
templates/
├── base.html              ← shared layout (header, sidebar, scripts)
├── partials/
│   └── sidebar.html       ← sidebar partial, included in base.html
├── dashboard.html         ← extends base.html
├── profile.html           ← extends base.html
├── routine.html           ← extends base.html
├── exercises.html         ← extends base.html (pending)
├── getwod.html            ← extends base.html (pending)
├── login.html             ← standalone, no sidebar (redesign pending)
└── register.html          ← standalone, no sidebar (redesign pending)
```

---

## 13. Future Considerations

- [ ] Login and Register page redesign — full notebook aesthetic (currently functional placeholder)
- [ ] Unify accent color between web (red) and mobile (yellow)
- [ ] Light mode for mobile
- [ ] User-adjustable font size on web (CSS variable already supports it)
- [ ] Google Sheets integration for Calendar page
- [ ] Google Cloud deployment (Cloud Run + Cloud SQL)
- [ ] Figma component library for handoff to professional designer
- [ ] Accessibility audit — contrast ratios in dark mode for `--muted` text
- [ ] Tablet layout — sidebar on mobile tablets
- [ ] HTTPS and rate limiting for production

---

*Last updated: May 2026*