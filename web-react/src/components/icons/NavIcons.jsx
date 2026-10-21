/* Navigation glyphs, drawn on a shared 24x24 grid.
 *
 * Inline SVG rather than .svg files in assets/ for three reasons:
 *   1. stroke="currentColor" lets each icon inherit the link colour, so the
 *      muted / hover / active states come from CSS with no extra assets.
 *   2. No extra network request and no sprite build step.
 *   3. The 24x24 grid is the contract: every glyph lines up optically without
 *      per-icon nudging.
 *
 * Fill, stroke, stroke-width and line caps live in the consuming stylesheet
 * (see .bn-icon), so these components carry geometry only.
 *
 * Deliberately slightly irregular: small rotations and off-grid coordinates
 * keep them in the hand-drawn notebook register instead of reading as a
 * geometric icon set.
 */

function Glyph({ className, children }) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      {children}
    </svg>
  );
}

// Four panels: the conventional dashboard metaphor, so it needs no learning.
export function IconDashboard({ className }) {
  return (
    <Glyph className={className}>
      <rect x="3.6" y="3.8" width="7.6" height="6.6" rx="2" transform="rotate(-1.5 7.4 7.1)" />
      <rect x="12.8" y="3.8" width="7.6" height="6.6" rx="2" transform="rotate(1.2 16.6 7.1)" />
      <rect x="3.6" y="13.6" width="7.6" height="6.6" rx="2" transform="rotate(1 7.4 16.9)" />
      <rect x="12.8" y="13.6" width="7.6" height="6.6" rx="2" transform="rotate(-1.2 16.6 16.9)" />
    </Glyph>
  );
}

// Head and shoulders.
export function IconProfile({ className }) {
  return (
    <Glyph className={className}>
      <circle cx="12" cy="8.2" r="3.8" />
      <path d="M4.8 20.2c.5-4 3.6-6.2 7.2-6.2s6.7 2.2 7.2 6.2" />
    </Glyph>
  );
}

// Checklist: a routine is a plan you work through.
export function IconRoutine({ className }) {
  return (
    <Glyph className={className}>
      <path d="M4.4 7.4l1.9 1.9 3.3-3.5" />
      <path d="M4.4 16.4l1.9 1.9 3.3-3.5" />
      <path d="M12.4 7.6h7.2M12.4 16.6h7.2" />
    </Glyph>
  );
}

// Dumbbell. Kept visually distinct from the kettlebell brand mark.
export function IconExercises({ className }) {
  return (
    <Glyph className={className}>
      <path d="M3.4 9.8v4.4M6.2 7.6v8.8M17.8 7.6v8.8M20.6 9.8v4.4" />
      <path d="M6.2 12h11.6" />
    </Glyph>
  );
}

// Calendar. The two top tabs are what make it read as a calendar at 24px.
export function IconCalendar({ className }) {
  return (
    <Glyph className={className}>
      <rect x="3.6" y="5.4" width="16.8" height="15" rx="2.4" />
      <path d="M3.6 10.2h16.8" />
      <path d="M8.4 3.4v3.6M15.6 3.4v3.6" />
      <path d="M7.8 14h2.2M12.4 14h2.2M7.8 17.2h2.2" />
    </Glyph>
  );
}

// Workout has no icon: a flexed arm's outline is, at 24px, indistinguishable
// from a boot, and the detail that separates them (knuckles, the bicep crease)
// disappears at that size. The tab is set as the word WOD in the brand's
// handwriting instead, which is unambiguous and already the term the app uses
// in every locale. See .bn-text in BottomNav.css.
