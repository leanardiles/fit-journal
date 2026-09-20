import { StickyNote } from "./StickyNote";

export default {
  title: "Components/StickyNote",
  component: StickyNote,
};

export const Default = {
  render: () => (
    <StickyNote tilt={-1.5}>
      <div style={{ fontFamily: "var(--font-body)", color: "var(--text)" }}>
        <div style={{ fontSize: 18, fontWeight: "bold", marginBottom: 8 }}>Quick Stats</div>
        <div>
          Workouts this week: <span style={{ color: "var(--red)" }}>3</span>
        </div>
      </div>
    </StickyNote>
  ),
};

export const Straight = {
  render: () => (
    <StickyNote>
      <div style={{ fontFamily: "var(--font-body)", color: "var(--text)" }}>No tilt.</div>
    </StickyNote>
  ),
};