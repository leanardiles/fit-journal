import { Fragment, useState, useEffect } from "react";
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
  { id: "quickStats", nameKey: "dashboard.quickStats", descKey: "dashboard.noteDesc.quickStats", defaultOn: true },
  { id: "currentRoutine", nameKey: "dashboard.currentRoutine", descKey: "dashboard.noteDesc.currentRoutine", defaultOn: true },
  { id: "setsPerMuscle", nameKey: "dashboard.setsPerMuscle", descKey: "dashboard.noteDesc.setsPerMuscle", defaultOn: true },
  { id: "weeklyGoals", nameKey: "dashboard.weeklyGoals", descKey: "dashboard.noteDesc.weeklyGoals", defaultOn: false },
];

// Three notes fill one row on a wide screen and stack cleanly on a phone. A
// fourth starts a second row, and because a grid row is as tall as its tallest
// item, the short notes in the first row leave a hole above it. The cap is a
// layout constraint stated as a product rule rather than a layout bug left for
// the user to discover.
const MAX_VISIBLE_NOTES = 3;

// The order goals are listed in. Fixed rather than sorted by progress, which
// would rearrange the note every time a set is logged and make it impossible
// to find a muscle by where it was last time.
const MUSCLE_ORDER = [
  "Chest", "Back", "Shoulders", "Biceps", "Triceps",
  "Legs", "Glutes", "Calves", "Abs", "Forearms",
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

/**
 * Which notes to hide, from what the server returned.
 *
 * Three cases, and they are not the same:
 *
 * 1. No preference at all. An empty stored ORDER is the signal, because any
 *    save writes the whole list. These users get the catalogue's own defaults,
 *    which is how a new note ships switched off rather than appearing on every
 *    existing dashboard uninvited.
 * 2. A preference that predates the cap, or any that exceeds it. The first
 *    MAX_VISIBLE_NOTES in the user's own order survive and the rest are hidden,
 *    so an old preference degrades in the direction the user chose rather than
 *    being discarded.
 * 3. An ordinary preference, used as given.
 */
function resolveVisibility(storedOrder, storedHidden) {
  const order = resolveOrder(storedOrder);
  const hasPreference = Array.isArray(storedOrder) && storedOrder.length > 0;

  let hidden = hasPreference
    ? (Array.isArray(storedHidden) ? storedHidden : []).filter((id) => DEFAULT_ORDER.includes(id))
    : NOTE_CATALOGUE.filter((n) => !n.defaultOn).map((n) => n.id);

  const shown = order.filter((id) => !hidden.includes(id));
  if (shown.length > MAX_VISIBLE_NOTES) {
    hidden = [...hidden, ...shown.slice(MAX_VISIBLE_NOTES)];
  }

  return { order, hidden };
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
  const [goals, setGoals] = useState({});
  const [goalRange, setGoalRange] = useState("week");
  const [editingGoals, setEditingGoals] = useState(false);
  const [goalDraft, setGoalDraft] = useState({});
  const [goalError, setGoalError] = useState("");
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
      apiGet(`/goals/${userId}`).then((r) => (r.ok ? r.json() : null)),
    ])
      .then(([st, ss, rt, ex, lg, dn, gl]) => {
        setState(st);
        setSessions(Array.isArray(ss) ? ss : []);
        setRoutine(rt);
        setExercises(Array.isArray(ex) ? ex : []);
        setLogs(Array.isArray(lg) ? lg : []);
        const visibility = resolveVisibility(dn?.order, dn?.hidden);
        setOrder(visibility.order);
        setHidden(visibility.hidden);
        setGoals(gl?.goals && typeof gl.goals === "object" ? gl.goals : {});
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

  // Extracted so the two notes can ask for different windows. They carry the
  // same control but keep their own state, so changing one does not silently
  // change the other: each note answers for itself.
  const totalsSince = (start) => {
    const totals = {};
    logs.forEach((log) => {
      const dateStr = log.workout_date || sessionDate[log.session_id];
      if (!dateStr) return;
      if (parseLocalDate(dateStr) < start) return;
      const muscle = log.muscle_group || exerciseMuscle[log.exercise_id];
      if (!muscle) return;
      totals[muscle] = (totals[muscle] || 0) + (Number(log.sets_completed) || 0);
    });
    return totals;
  };

  const muscleTotals = totalsSince(range === "week" ? monday : sevenDaysAgo());

  // A weekly target is a target per seven days, and a rolling window is as
  // valid a seven days as a calendar one. The tick means something slightly
  // different in each: met this calendar week, or met across any seven days.
  const goalTotals = totalsSince(goalRange === "week" ? monday : sevenDaysAgo());

  const goalRows = MUSCLE_ORDER.filter((m) => goals[m] > 0)
    .map((muscle) => {
      const target = goals[muscle];
      const done = goalTotals[muscle] || 0;
      return {
        muscle,
        done,
        target,
        met: done >= target,
        remaining: target - done,
        // Capped at 100 so an exceeded goal does not overflow its track. The
        // ratio still reads 11 / 9, so nothing is hidden by the cap.
        pct: Math.min(100, Math.round((done / target) * 100)),
      };
    })
    // Most work left at the top, finished goals at the bottom. The note's job
    // is what to train next, so a ticked muscle sitting above three untouched
    // ones was exactly backwards.
    //
    // Remaining is absolute sets, not a percentage: 10 sets short of 20 is more
    // work than 6 short of 6, even though the second is further behind as a
    // fraction, and sets are what the user actually has to go and do.
    //
    // Returning 0 for ties leaves them in MUSCLE_ORDER, because Array sort is
    // stable. So the done pile and any equally-behind muscles keep the
    // anatomical order rather than shuffling between renders.
    .sort((a, b) => {
      if (a.met !== b.met) return a.met ? 1 : -1;
      if (a.met) return 0;
      return b.remaining - a.remaining;
    });

  // The editor offers every muscle group, not just the ones the routine
  // trains. Filtering by the routine looked tidier and was wrong: a workout
  // can be logged manually against any muscle, so a goal for something the
  // routine does not cover is a thing a user can legitimately want, and a
  // missing row gives them no way to find out why. Ten rows is a short form
  // either way, so the filtering bought almost nothing and cost the ability
  // to say something.
  const goalEditMuscles = MUSCLE_ORDER;

  const startEditGoals = () => {
    const draft = {};
    goalEditMuscles.forEach((m) => {
      draft[m] = goals[m] ? String(goals[m]) : "";
    });
    setGoalDraft(draft);
    setGoalError("");
    setEditingGoals(true);
  };

  const cancelEditGoals = () => {
    setEditingGoals(false);
    setGoalError("");
  };

  const saveGoals = async () => {
    const next = {};
    for (const [muscle, raw] of Object.entries(goalDraft)) {
      const value = String(raw).trim();
      // An empty field is how a goal is removed. There is no separate delete,
      // and no zero: a muscle with no goal is simply absent from the map,
      // which is the same rule the API enforces.
      if (value === "") continue;
      const n = Number(value);
      if (!Number.isInteger(n) || n < 1 || n > 100) {
        setGoalError(t("dashboard.goalsInvalid"));
        return;
      }
      next[muscle] = n;
    }

    const previous = goals;
    setGoals(next);
    setEditingGoals(false);
    setGoalError("");
    try {
      const userId = localStorage.getItem("user_id");
      const res = await apiPut(`/goals/${userId}`, { goals: next });
      if (!res.ok) throw new Error("save failed");
    } catch {
      setGoals(previous);
      showToast(t("dashboard.goalsSaveError"), "error");
    }
  };
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
    weeklyGoals: (
      <StickyNote key="weeklyGoals">
        <div className="note-heading">{t("dashboard.weeklyGoals")}</div>
        {/* Hidden while editing: the window is irrelevant when you are setting
            the targets themselves, and it would sit above fields it does not
            affect. */}
        {!editingGoals && (
          <Toggle value={goalRange} onChange={setGoalRange} options={rangeOptions} />
        )}
        {editingGoals ? (
          /* Edit mode replaces the ratio with a field in the same place, and
             drops the bars: while the target is being typed, a bar that
             recalculates on every keystroke is noise, and progress is not what
             the user is looking at. */
          <div className="goal-edit-rows">
            {goalEditMuscles.map((muscle) => (
              <div key={muscle} className="goal-edit-row">
                <label className="muscle-name" htmlFor={`goal-${muscle}`}>
                  {t(`muscles.${muscle}`)}
                </label>
                <input
                  id={`goal-${muscle}`}
                  className="goal-input"
                  type="number"
                  min="1"
                  max="100"
                  inputMode="numeric"
                  value={goalDraft[muscle] ?? ""}
                  onChange={(e) => {
                    setGoalError("");
                    setGoalDraft((prev) => ({ ...prev, [muscle]: e.target.value }));
                  }}
                />
              </div>
            ))}
          </div>
        ) : goalRows.length === 0 ? (
          <div className="note-line">{t("dashboard.noGoals")}</div>
        ) : (
          /* One grid for the whole list rather than a grid per row: the
             tracks resolve once, so every bar is the same width and the three
             columns line up down the note. Per-row containers would size each
             bar to whatever its muscle name left over, and fills of different
             widths cannot be compared at a glance, which is the bar's only
             job. */
          <div className="goal-rows">
            {goalRows.map((row) => (
              <Fragment key={row.muscle}>
                <span className="muscle-name">{t(`muscles.${row.muscle}`)}</span>
                <div className="goal-track">
                  <div className="goal-fill" style={{ width: `${row.pct}%` }} />
                </div>
                <span className="goal-count">
                  {t("dashboard.goalCount", { done: row.done, target: row.target })}
                  {row.met && (
                    <svg className="goal-tick" width="14" height="14" viewBox="0 0 24 24"
                         fill="none" stroke="currentColor" strokeWidth="3"
                         strokeLinecap="round" strokeLinejoin="round"
                         role="img" aria-label={t("dashboard.goalMet")}>
                      <path d="M4 12.5l5 5L20 6.5" />
                    </svg>
                  )}
                </span>
              </Fragment>
            ))}
          </div>
        )}
        {goalError && <div className="goal-error">{goalError}</div>}

        <div className="goal-actions">
          {editingGoals ? (
            <>
              <button type="button" className="note-btn" onClick={saveGoals}>
                {t("dashboard.goalsSave")}
              </button>
              <button type="button" className="note-btn-plain" onClick={cancelEditGoals}>
                {t("common.cancel")}
              </button>
            </>
          ) : (
            <button type="button" className="note-btn" onClick={startEditGoals}>
              {t("dashboard.setGoals")}
            </button>
          )}
        </div>
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
        max={MAX_VISIBLE_NOTES}
        order={order}
        hidden={hidden}
        onCancel={() => setNotesOpen(false)}
        onSave={saveNotes}
      />
    </div>
  );
}
