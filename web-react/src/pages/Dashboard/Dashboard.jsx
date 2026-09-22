import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { apiGet } from "../../api/client";
import { StickyNote } from "../../components/StickyNote/StickyNote";
import { Toggle } from "../../components/Toggle/Toggle";
import "./Dashboard.css";

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

  useEffect(() => {
    const userId = localStorage.getItem("user_id");
    Promise.all([
      apiGet(`/workout/state/${userId}`).then((r) => (r.ok ? r.json() : null)),
      apiGet(`/workout/sessions/${userId}?limit=20`).then((r) => (r.ok ? r.json() : [])),
      apiGet(`/routine/${userId}`).then((r) => (r.ok ? r.json() : null)),
      apiGet(`/exercises?user_id=${userId}`).then((r) => (r.ok ? r.json() : [])),
      apiGet(`/workout/logs/${userId}?limit=100`).then((r) => (r.ok ? r.json() : [])),
    ])
      .then(([st, ss, rt, ex, lg]) => {
        setState(st);
        setSessions(Array.isArray(ss) ? ss : []);
        setRoutine(rt);
        setExercises(Array.isArray(ex) ? ex : []);
        setLogs(Array.isArray(lg) ? lg : []);
      })
      .finally(() => setLoading(false));
  }, []);

  const currentDay = state?.current_day_number ?? 1;
  const monday = mondayOfThisWeek();
  const workoutsThisWeek = sessions.filter((s) => parseLocalDate(s.workout_date) >= monday).length;
  const hasRoutine = routine && routine.days_per_week > 0;

  const muscleList = (day) => {
    const groups =
      day.day_type === "manual"
        ? [...new Set((day.exercises || []).map((e) => e.muscle_group))]
        : (day.muscles || []).map((m) => m.muscle_group);
    return groups.map((g) => t(`muscles.${g}`)).join(", ");
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

  return (
    <div>
      <h1 className="dash-title">{t("dashboard.title")}</h1>

      <div className="dashboard-grid">
        {/* Quick stats */}
        <StickyNote tilt={-1.5}>
          <div className="note-heading">{t("dashboard.quickStats")}</div>
          <div className="note-line">
            {t("dashboard.thisWeek")}: <span className="note-value">{workoutsThisWeek}</span>
          </div>
          <div className="note-line">
            {t("dashboard.currentDay")}:{" "}
            <span className="note-value">{t("common.day", { day: currentDay })}</span>
          </div>
        </StickyNote>

        {/* Current routine */}
        <StickyNote tilt={1}>
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
                    ? `${t("common.day", { day: day.day_number })} — ${day.name}`
                    : t("common.day", { day: day.day_number });
                  const detail = muscleList(day) || t("dashboard.restDay");
                  return (
                    <div key={day.day_number} className={`note-day${isToday ? " today" : ""}`}>
                      <span className="note-day-label">{label}:</span>{" "}
                      <span className="note-day-muscles">{detail}</span>
                      {isToday && <span className="note-today">{t("dashboard.today")}</span>}
                    </div>
                  );
                })}
              <div style={{ marginTop: 12 }}>
                <Link to="/routine" className="note-link">{t("dashboard.editRoutine")}</Link>
              </div>
            </>
          )}
        </StickyNote>

        {/* Sets per muscle */}
        <StickyNote tilt={-0.5}>
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
      </div>
    </div>
  );
}