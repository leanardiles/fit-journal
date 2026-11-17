import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { apiGet, apiPut } from "../../api/client";
import { StickyNote } from "../../components/StickyNote/StickyNote";
import { formatList } from "../../i18n/format";
import { Toggle } from "../../components/Toggle/Toggle";
import { DashboardNotesModal } from "../../components/DashboardNotesModal/DashboardNotesModal";
import { useToast } from "../../context/ToastContext";
import "./Dashboard.css";

// The catalogue of notes, in the order a user who has never customized
// anything sees them. This list, not the database, is the source of truth for
// what a note IS: the stored preference only says which of these to show and
// in what sequence.
const NOTE_CATALOGUE = [
  { id: "quickStats", nameKey: "dashboard.quickStats", descKey: "dashboard.noteDesc.quickStats" },
  { id: "currentRoutine", nameKey: "dashboard.currentRoutine", descKey: "dashboard.noteDesc.currentRoutine" },
  { id: "setsPerMuscle", nameKey: "dashboard.setsPerMuscle", descKey: "dashboard.noteDesc.setsPerMuscle" },
];

const DEFAULT_ORDER = NOTE_CATALOGUE.map((n) => n.id);

/**
 * A stored order is a preference LAYERED OVER the catalogue, never a
 * replacement for it. Ids this build no longer knows are dropped, and notes
 * the preference never mentioned are appended in default order.
 *
 * Both halves matter across releases: without the first, deleting a note
 * leaves old preferences pointing at nothing; without the second, a note
 * added later would be invisible to every user who had saved a preference
 * before it existed, which is the kind of bug nobody reports because the
 * feature simply never appears.
 */
function resolveOrder(stored) {
  const saved = (Array.isArray(stored) ? stored : []).filter((id) => DEFAULT_ORDER.includes(id));
  return [...saved, ...DEFAULT_ORDER.filter((id) => !saved.includes(id))];
}

function resolveHidden(stored) {
  return (Array.isArray(stored) ? stored : []).filter((id) => DEFAULT_ORDER.includes(id));
}

// Parse a stored YYYY-MM-DD as a LOCAL date. new Date("2025-09-06") parses as
// midnight UTC, which lands a day earlier in US timezones and would push today's
// workout into last week. Build from parts to keep it local.
function parseLocalDate(dateStr) {
  const [y, m, d] = String(dateStr).split("-").map(Number);
  return new Date(y, m - 1, d);
}

function mondayOfThisWeek() {
  const now = new Date();
  const day = now.getDay();
  const diff = now.getDate() - day + (day === 0 ? -6 : 1); // Monday-based
  const monday = new Date(now);
  monday.setDate(diff);
  monday.setHours(0, 0, 0, 0);
  return monday;
}

// Start of a rolling 7-day window (today plus the previous 6 days).
function sevenDaysAgo() {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() - 6);
  return d;
}

export function Dashboard() {
  const { t } = useTranslation();

  const [state, setState] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [routine, setRoutine] = useState(null);
  const [exercises, setExercises] = useState([]);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [range, setRange] = useState("week");
  const [order, setOrder] = useState(DEFAULT_ORDER);
  const [hidden, setHidden] = useState([]);
  const [notesOpen, setNotesOpen] = useState(false);
  const { showToast } = useToast();

  useEffect(() => {
    const userId = localStorage.getItem("user_id");
    Promise.all([
      apiGet(`/workout/state/${userId}`).then((r) => (r.ok ? r.json() : null)),
      apiGet(`/workout/sessions/${userId}?limit=20`).then((r) => (r.ok ? r.json() : [])),
      apiGet(`/routine/${userId}`).then((r) => (r.ok ? r.json() : null)),
      apiGet(`/exercises?user_id=${userId}`).then((r) => (r.ok ? r.json() : [])),
      apiGet(`/workout/logs/${userId}?limit=100`).then((r) => (r.ok ? r.json() : [])),
      apiGet(`/dashboard/notes/${userId}`).then((r) => (r.ok ? r.json() : null)),
    ])
      .then(([st, ss, rt, ex, lg, dn]) => {
        setState(st);
        setSessions(Array.isArray(ss) ? ss : []);
        setRoutine(rt);
        setExercises(Array.isArray(ex) ? ex : []);
        setLogs(Array.isArray(lg) ? lg : []);
        setOrder(resolveOrder(dn?.order));
        setHidden(resolveHidden(dn?.hidden));
      })
      .finally(() => setLoading(false));
  }, []);

  const currentDay = state?.current_day_number ?? 1;
  const monday = mondayOfThisWeek();
  const workoutsThisWeek = sessions.filter((s) => parseLocalDate(s.workout_date) >= monday).length;
  const hasRoutine = routine && routine.days_per_week > 0;

  // When the user last TRAINED, which is workout_date, not created_at. The two
  // differ because the log date can be backdated: created_at would answer
  // "when did you last open the app", and nobody wants that on a dashboard.
  const lastWorkoutDate = sessions.length
    ? sessions.map((s) => parseLocalDate(s.workout_date)).sort((a, b) => b - a)[0]
    : null;

  // Whole days between two local midnights. Rounded rather than floored
  // because a DST boundary inside the span makes it 23 or 25 hours, and
  // flooring would report yesterday as today twice a year. A backdated future
  // date clamps to 0 rather than counting down.
  const daysSinceLastWorkout = (() => {
    if (!lastWorkoutDate) return null;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return Math.max(0, Math.round((today - lastWorkoutDate) / 86400000));
  })();

  const lastWorkoutLabel =
    daysSinceLastWorkout === null
      ? t("dashboard.lastWorkoutNever")
      : daysSinceLastWorkout === 0
        ? t("dashboard.lastWorkoutToday")
        : daysSinceLastWorkout === 1
          ? t("dashboard.lastWorkoutYesterday")
          : t("dashboard.lastWorkoutDaysAgo", { count: daysSinceLastWorkout });

  const muscleList = (day) => {
    const groups =
      day.day_type === "manual"
        ? [...new Set((day.exercises || []).map((e) => e.muscle_group))]
        : (day.muscles || []).map((m) => m.muscle_group);
    return formatList(groups.map((g) => t(`muscles.${g}`)));
  };

  // --- Sets per muscle (windowed) -------------------------------------------
  // Resolve each log's muscle and date defensively: prefer fields on the log
  // itself, fall back to the exercise map / session dates we already hold.
  const exerciseMuscle = {};
  exercises.forEach((ex) => {
    exerciseMuscle[ex.exercise_id] = ex.exercise_muscle_group ?? ex.muscle_group;
  });
  const sessionDate = {};
  sessions.forEach((s) => {
    sessionDate[s.session_id ?? s.id] = s.workout_date;
  });

  const windowStart = range === "week" ? monday : sevenDaysAgo();
  const muscleTotals = {};
  logs.forEach((log) => {
    const dateStr = log.workout_date || sessionDate[log.session_id];
    if (!dateStr) return;
    if (parseLocalDate(dateStr) < windowStart) return;
    const muscle = log.muscle_group || exerciseMuscle[log.exercise_id];
    if (!muscle) return;
    muscleTotals[muscle] = (muscleTotals[muscle] || 0) + (Number(log.sets_completed) || 0);
  });
  const muscleRows = Object.entries(muscleTotals)
    .map(([muscle, sets]) => ({ muscle, sets }))
    .filter((r) => r.sets > 0)
    .sort((a, b) => b.sets - a.sets);

  const rangeOptions = [
    { value: "week", label: t("dashboard.thisWeek") },
    { value: "7days", label: t("dashboard.last7Days") },
  ];

  if (loading) return <p style={{ color: "var(--text)" }}>{t("common.loading")}</p>;

  // Every note keyed by its stable id. The dashboard renders an ORDER rather
  // than a fixed sequence of blocks, which is the whole of what made this
  // customizable: the notes themselves did not have to change.
  const noteById = {
    quickStats: (
      <StickyNote key="quickStats">
        <div className="note-heading">{t("dashboard.quickStats")}</div>
        <div className="note-line">
          {t("dashboard.thisWeek")}:{" "}
          <span className="note-value">
            {hasRoutine
              ? t("dashboard.ofTarget", { done: workoutsThisWeek, target: routine.days_per_week })
              : workoutsThisWeek}
          </span>
        </div>
        <div className="note-line">
          {t("dashboard.lastWorkout")}: <span className="note-value">{lastWorkoutLabel}</span>
        </div>
      </StickyNote>
    ),
    currentRoutine: (
      <StickyNote key="currentRoutine">
        <div className="note-heading">{t("dashboard.currentRoutine")}</div>
        {!hasRoutine ? (
          <div className="note-line">
            <span>{t("dashboard.noRoutine")}</span>{" "}
            <Link to="/routine" className="note-link">{t("dashboard.createRoutine")}</Link>
          </div>
        ) : (
          <>
            {[...routine.days]
              .sort((a, b) => a.day_number - b.day_number)
              .map((day) => {
                const isToday = day.day_number === currentDay;
                const label = day.name
                  ? t("common.dayNamed", { day: day.day_number, name: day.name })
                  : t("common.day", { day: day.day_number });
                const detail = muscleList(day) || t("dashboard.restDay");
                return (
                  <div key={day.day_number} className={`note-day${isToday ? " today" : ""}`}>
                    <span className="note-day-label">{label}:</span>
                    {isToday && <span className="sr-only"> ({t("dashboard.today")})</span>}{" "}
                    <span className="note-day-muscles">{detail}</span>
                  </div>
                );
              })}
            <div style={{ marginTop: 12 }}>
              <Link to="/routine" className="note-btn">{t("dashboard.editRoutine")}</Link>
            </div>
          </>
        )}
      </StickyNote>
    ),
    setsPerMuscle: (
      <StickyNote key="setsPerMuscle">
        <div className="note-heading">{t("dashboard.setsPerMuscle")}</div>
        <Toggle value={range} onChange={setRange} options={rangeOptions} />
        {muscleRows.length === 0 ? (
          <div className="note-line" style={{ marginTop: 10 }}>{t("dashboard.noSets")}</div>
        ) : (
          <div className="muscle-rows">
            {muscleRows.map((row) => (
              <div key={row.muscle} className="muscle-row">
                <span className="muscle-name">{t(`muscles.${row.muscle}`)}</span>
                <span className="muscle-sets">{t("dashboard.sets", { count: row.sets })}</span>
              </div>
            ))}
          </div>
        )}
      </StickyNote>
    ),
  };

  const visible = order.filter((id) => !hidden.includes(id));

  // Optimistic, and reverted on failure. Per ADR 0002 there is no success
  // toast: saving closes the dialog and the dashboard reorders in front of
  // the user, so the change proves itself. A failure shows nothing on its
  // own, so that one speaks.
  const saveNotes = async (nextOrder, nextHidden) => {
    const prevOrder = order;
    const prevHidden = hidden;
    setOrder(nextOrder);
    setHidden(nextHidden);
    setNotesOpen(false);
    try {
      const userId = localStorage.getItem("user_id");
      const res = await apiPut(`/dashboard/notes/${userId}`, {
        order: nextOrder,
        hidden: nextHidden,
      });
      if (!res.ok) throw new Error("save failed");
    } catch {
      setOrder(prevOrder);
      setHidden(prevHidden);
      showToast(t("dashboard.notesSaveError"), "error");
    }
  };

  return (
    <div>
      <div className="dash-head">
        <h1 className="dash-title">{t("dashboard.title")}</h1>
        <button
          type="button"
          className="dash-notes-btn"
          aria-label={t("dashboard.notesButton")}
          onClick={() => setNotesOpen(true)}
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <path d="M4 6h10" /><path d="M18 6h2" />
            <path d="M4 12h3" /><path d="M11 12h9" />
            <path d="M4 18h11" /><path d="M19 18h1" />
            <circle cx="16" cy="6" r="2" />
            <circle cx="9" cy="12" r="2" />
            <circle cx="17" cy="18" r="2" />
          </svg>
        </button>
      </div>

      {visible.length === 0 ? (
        <p className="dash-empty">{t("dashboard.notesAllHidden")}</p>
      ) : (
        <div className="dashboard-grid">
          {visible.map((id) => noteById[id])}
        </div>
      )}

      <DashboardNotesModal
        open={notesOpen}
        notes={NOTE_CATALOGUE}
        order={order}
        hidden={hidden}
        onCancel={() => setNotesOpen(false)}
        onSave={saveNotes}
      />
    </div>
  );
}
