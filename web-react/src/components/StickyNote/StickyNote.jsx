import "./StickyNote.css";

/**
 * Sticky-note surface for dashboard modules. Yellow paper with a tape strip
 * and a slight hand-placed tilt. Rebinds the design tokens locally so any
 * standard token-based content inside reads correctly on the note.
 * Props:
 *   tilt     - rotation in degrees (default 0), for a hand-placed feel
 *   children - note content
 */
export function StickyNote({ tilt = 0, children }) {
  return (
    <div className="sticky-note" style={{ transform: `rotate(${tilt}deg)` }}>
      {children}
    </div>
  );
}