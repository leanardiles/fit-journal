import { useState, useEffect, Fragment } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { apiGet, apiPost, apiDelete } from "../../api/client";
import { useToast } from "../../context/ToastContext";
import "./Calendar.css";

const COLUMNS_TO_SHOW = 10;

// Parse a stored YYYY-MM-DD as a LOCAL date. new Date("2025-09-06") parses as
// midnight UTC, a day earlier in timezones behind UTC. Build from parts.
function parseLocalDate(dateStr) {
  const [y, m, d] = String(dateStr).split("-").map(Number);
  return new Date(y, m - 1, d);
}

function LockIcon() {
  return (
    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <rect x="5" y="11" width="14" height="10" rx="2" />
      <path d="M8 11V7a4 4 0 0 1 8 0v4" />
    </svg>
  );
}

export function Calendar() {
  const { t, i18n } = useTranslation();
  const { showToast } = useToast();
  const userId = localStorage.getItem("user_id");

  const [routine, setRoutine] = useState(null);
  const [exercises, setExercises] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [logs, setLogs] = useState([]);
  const [selections, setSelections] = useState({}); // { [exercise_id]: bool }
  const [currentDay, setCurrentDay] = useState(1);
  const [loading, setLoading] = useState(true);

  const [filterMode, setFilterMode] = useState("specific"); // 'all' | 'specific'
  const [selectedDays, setSelectedDays] = useState([]);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const rt = await apiGet(`/routine/${userId}`).then((r) => (r.ok ? r.json() : null));
        if (cancelled) return;
        setRoutine(rt);
        if (!rt || !rt.days_per_week) {
          setLoading(false);
          return;
        }

        const [st, ex, ss, sel] = await Promise.all([
          apiGet(`/workout/state/${userId}`).then((r) => (r.ok ? r.json() : null)),
          apiGet(`/exercises?user_id=${userId}`).then((r) => (r.ok ? r.json() : [])),
          apiGet(`/workout/sessions/${userId}?limit=${COLUMNS_TO_SHOW}`).then((r) => (r.ok ? r.json() : [])),
          apiGet(`/next-workout/selections/${userId}`).then((r) => (r.ok ? r.json() : [])),
        ]);
        if (cancelled) return;

        const day = st?.current_day_number ?? 1;
        setCurrentDay(day);
        setSelectedDays((prev) => (prev.length ? prev : [day]));
        setExercises(Array.isArray(ex) ? ex : []);
        const sess = Array.isArray(ss) ? ss : [];
        setSessions(sess);
        setSelections(selectionsToMap(sel));

        if (sess.length) {
          const ids = sess.map((s) => s.session_id);
          const lg = await apiPost(`/workout/logs-by-sessions/${userId}`, { session_ids: ids }).then((r) =>
            r.ok ? r.json() : []
          );
          if (cancelled) return;
          setLogs(Array.isArray(lg) ? lg : []);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  const selectionsToMap = (arr) => {
    const obj = {};
    (Array.isArray(arr) ? arr : []).forEach((i) => {
      obj[i.exercise_id] = i.is_selected;
    });
    return obj;
  };

  const refetchSelections = async () => {
    const sel = await apiGet(`/next-workout/selections/${userId}`).then((r) => (r.ok ? r.json() : []));
    setSelections(selectionsToMap(sel));
  };

  // --- routine-shape helpers (mirrors the legacy page) ----------------------
  const getDayObj = (n) => (routine?.days || []).find((d) => d.day_number === n);

  const dayMuscles = (n) => {
    const day = getDayObj(n);
    if (!day) return [];
    if (day.day_type === "manual") {
      const seen = [];
      (day.exercises || []).forEach((e) => {
        if (!seen.includes(e.muscle_group)) seen.push(e.muscle_group);
      });
      return seen;
    }
    return (day.muscles || []).map((m) => m.muscle_group);
  };

  // Pool exercises for a day. per_muscle days are selectable (togglable for the
  // next session); manual-day exercises are in the pool but locked (fixed).
  const dayPoolExercises = (n) => {
    const day = getDayObj(n);
    if (!day) return [];
    const perMuscle = day.day_type !== "manual";
    return (day.exercises || []).map((e) => ({
      exercise_id: e.exercise_id,
      exercise_name: e.exercise_name,
      muscle_group: e.muscle_group,
      inPool: true,
      selectable: perMuscle,
    }));
  };

  // Rows for a day: its pool PLUS any logged exercise whose library muscle the
  // day trains (off-routine / manual logs). Logged-only rows are history:
  // inPool / selectable false, so they get no Select control.
  const rowsForDay = (n) => {
    const byId = new Map();
    dayPoolExercises(n).forEach((ex) => byId.set(ex.exercise_id, ex));
    const muscles = new Set(dayMuscles(n));
    new Set(logs.map((l) => l.exercise_id)).forEach((id) => {
      if (byId.has(id)) return;
      const lib = exercises.find((ex) => ex.exercise_id === id);
      if (!lib || !muscles.has(lib.exercise_muscle_group)) return;
      byId.set(id, {
        exercise_id: lib.exercise_id,
        exercise_name: lib.exercise_name,
        muscle_group: lib.exercise_muscle_group,
        inPool: false,
        selectable: false,
      });
    });
    return Array.from(byId.values());
  };

  const dayLabel = (n) => {
    const day = getDayObj(n);
    if (!day) return "";
    if (day.name) return day.name;
    return dayMuscles(n)
      .map((m) => t(`muscles.${m}`))
      .join(", ");
  };

  const allViewRows = () => {
    const byId = new Map();
    (routine?.days || []).forEach((day) => {
      (day.exercises || []).forEach((e) => {
        if (!byId.has(e.exercise_id)) {
          byId.set(e.exercise_id, {
            exercise_id: e.exercise_id,
            exercise_name: e.exercise_name,
            muscle_group: e.muscle_group,
          });
        }
      });
    });
    new Set(logs.map((l) => l.exercise_id)).forEach((id) => {
      if (byId.has(id)) return;
      const lib = exercises.find((ex) => ex.exercise_id === id);
      if (!lib) return;
      byId.set(id, {
        exercise_id: lib.exercise_id,
        exercise_name: lib.exercise_name,
        muscle_group: lib.exercise_muscle_group,
      });
    });
    return Array.from(byId.values());
  };

  const filteredExercises = (days) => {
    const byId = new Map();
    days.forEach((d) =>
      rowsForDay(d).forEach((ex) => {
        const cur = byId.get(ex.exercise_id);
        if (!cur) byId.set(ex.exercise_id, { ...ex });
        else {
          cur.inPool = cur.inPool || ex.inPool;
          cur.selectable = cur.selectable || ex.selectable;
        }
      })
    );
    return Array.from(byId.values());
  };

  const sortRows = (arr) =>
    [...arr].sort((a, b) =>
      a.muscle_group !== b.muscle_group
        ? a.muscle_group.localeCompare(b.muscle_group)
        : a.exercise_name.localeCompare(b.exercise_name)
    );

  const cellValue = (exId, sessionId) => {
    const log = logs.find((l) => l.exercise_id === exId && l.session_id === sessionId);
    return log ? log.sets_completed || "—" : null; // null => empty cell
  };

  const toggleDay = (day) => {
    if (filterMode === "specific" && selectedDays.includes(day)) {
      const next = selectedDays.filter((d) => d !== day);
      if (next.length === 0) {
        setFilterMode("all");
        setSelectedDays([]);
      } else {
        setSelectedDays(next);
      }
    } else {
      const base = filterMode === "specific" ? selectedDays : [];
      setFilterMode("specific");
      setSelectedDays([...new Set([...base, day])].sort((a, b) => a - b));
    }
  };

  if (loading) return <p style={{ color: "var(--text)" }}>{t("common.loading")}</p>;

  if (!routine || !routine.days_per_week) {
    return (
      <div>
        <h1 className="cal-title">{t("calendar.title")}</h1>
        <p className="cal-empty-block">
          {t("calendar.noRoutine")}
          <Link to="/routine" className="cal-link">
            {t("calendar.createRoutine")}
          </Link>
        </p>
      </div>
    );
  }

  const isAllView = filterMode === "all";
  const showSelect = !isAllView;
  const activeDays = isAllView
    ? Array.from({ length: routine.days_per_week }, (_, i) => i + 1)
    : selectedDays;

  // --- next-workout planner actions -----------------------------------------
  const handleToggle = async (exerciseId) => {
    const current = !!selections[exerciseId];
    const nextVal = !current;
    setSelections((prev) => ({ ...prev, [exerciseId]: nextVal })); // optimistic
    try {
      const res = await apiPost(`/next-workout/toggle`, {
        user_id: Number(userId),
        exercise_id: exerciseId,
        is_selected: nextVal,
      });
      if (!res.ok) throw new Error("toggle failed");
    } catch {
      setSelections((prev) => ({ ...prev, [exerciseId]: current })); // revert
      showToast(t("calendar.toggleError"), "error");
    }
  };

  const handleAutoGenerate = async () => {
    try {
      let total = 0;
      for (const day of activeDays) {
        const res = await apiPost(`/next-workout/generate/${userId}?day_number=${day}`, {});
        if (res.ok) {
          const r = await res.json();
          total += r.exercises_selected ?? 0;
        }
      }
      await refetchSelections();
      showToast(t("calendar.generated", { count: total }), "success");
    } catch {
      showToast(t("calendar.generateError"), "error");
    }
  };

  const handleClear = async () => {
    try {
      for (const day of activeDays) {
        await apiDelete(`/next-workout/clear/${userId}?day_number=${day}`);
      }
      await refetchSelections();
      showToast(t("calendar.cleared"), "success");
    } catch {
      showToast(t("calendar.clearError"), "error");
    }
  };

  const rows = isAllView ? allViewRows() : filteredExercises(activeDays);
  const rowIds = new Set(rows.map((r) => r.exercise_id));
  const columns = sessions
    .filter((s) => logs.some((l) => l.session_id === s.session_id && rowIds.has(l.exercise_id)))
    .sort((a, b) => parseLocalDate(b.workout_date) - parseLocalDate(a.workout_date));

  const colSpan = (showSelect ? 2 : 1) + columns.length;

  // Localized, hand-formatting-free date headers — the whole point of the page.
  const weekdayFmt = new Intl.DateTimeFormat(i18n.language, { weekday: "short" });
  const dateFmt = new Intl.DateTimeFormat(i18n.language, { month: "short", day: "numeric" });

  const viewInfo = isAllView
    ? t("calendar.showingAll")
    : t("calendar.showing", {
        days: selectedDays
          .map((d) => {
            const lbl = dayLabel(d);
            return lbl ? `${t("common.day", { day: d })} (${lbl})` : t("common.day", { day: d });
          })
          .join(", "),
      });

  const renderRow = (ex, keyPrefix) => (
    <tr key={`${keyPrefix}-r-${ex.exercise_id}`}>
      <td className="left cal-exname sticky-col sticky-name">{ex.exercise_name}</td>
      {showSelect && (
        <td className="center sticky-col sticky-select">
          {ex.selectable ? (
            <button
              className={`cal-next-btn${selections[ex.exercise_id] ? " selected" : ""}`}
              onClick={() => handleToggle(ex.exercise_id)}
              aria-pressed={!!selections[ex.exercise_id]}
            >
              {selections[ex.exercise_id] ? "✓" : ""}
            </button>
          ) : ex.inPool ? (
            <span className="cal-lock" title={t("calendar.lockedHint")}>
              <LockIcon />
            </span>
          ) : null}
        </td>
      )}
      {columns.map((s) => {
        const v = cellValue(ex.exercise_id, s.session_id);
        return (
          <td key={s.session_id} className={`cal-wod-cell ${v == null ? "empty" : "completed"}`}>
            {v == null ? "—" : v}
          </td>
        );
      })}
    </tr>
  );

  const renderMuscleGrouped = (arr, keyPrefix) => {
    const out = [];
    let current = null;
    sortRows(arr).forEach((ex) => {
      if (ex.muscle_group !== current) {
        current = ex.muscle_group;
        out.push(
          <tr key={`${keyPrefix}-m-${current}`} className="cal-muscle-row">
            <td colSpan={colSpan}>{t(`muscles.${current}`)}</td>
          </tr>
        );
      }
      out.push(renderRow(ex, keyPrefix));
    });
    return out;
  };

  let bodyRows;
  if (rows.length === 0) {
    bodyRows = (
      <tr>
        <td colSpan={colSpan} className="cal-empty">
          {isAllView ? t("calendar.noExercisesAll") : t("calendar.noExercises")}
        </td>
      </tr>
    );
  } else if (isAllView) {
    bodyRows = renderMuscleGrouped(rows, "all");
  } else if (selectedDays.length > 1) {
    bodyRows = [];
    activeDays.forEach((d) => {
      const dayRows = rowsForDay(d);
      if (dayRows.length === 0) return;
      const lbl = dayLabel(d);
      bodyRows.push(
        <tr key={`day-${d}`} className="cal-day-row">
          <td colSpan={colSpan}>{lbl ? `${t("common.day", { day: d })} — ${lbl}` : t("common.day", { day: d })}</td>
        </tr>
      );
      bodyRows.push(...renderMuscleGrouped(dayRows, `day-${d}`));
    });
    if (bodyRows.length === 0) {
      bodyRows = (
        <tr>
          <td colSpan={colSpan} className="cal-empty">
            {t("calendar.noExercises")}
          </td>
        </tr>
      );
    }
  } else {
    bodyRows = renderMuscleGrouped(rows, "single");
  }

  const dayNumbers = Array.from({ length: routine.days_per_week }, (_, i) => i + 1);

  return (
    <div>
      <div className="cal-header">
        <h1 className="cal-title">{t("calendar.title")}</h1>
        <div className="cal-actions">
          <button className="cal-action-btn" onClick={handleAutoGenerate}>
            🤖 {t("calendar.autoSelect")}
          </button>
          <button className="cal-action-btn" onClick={handleClear}>
            {t("calendar.clearSelection")}
          </button>
        </div>
      </div>

      <div className="cal-day-filter">
        <button
          className={`cal-filter-btn${isAllView ? " active" : ""}`}
          onClick={() => {
            setFilterMode("all");
            setSelectedDays([]);
          }}
        >
          {t("calendar.allDays")}
        </button>
        {dayNumbers.map((d) => (
          <button
            key={d}
            className={`cal-filter-btn${filterMode === "specific" && selectedDays.includes(d) ? " active" : ""}`}
            onClick={() => toggleDay(d)}
          >
            {t("common.day", { day: d })}
            {d === currentDay && <span className="cal-current-marker">{t("calendar.currentDay")}</span>}
          </button>
        ))}
      </div>

      <div className="cal-view-info">{viewInfo}</div>

      <div className="cal-wrapper">
        <table className="cal-table">
          <thead>
            <tr>
              <th className="left sticky-col sticky-name">{t("calendar.exercise")}</th>
              {showSelect && <th className="sticky-col sticky-select">{t("calendar.select")}</th>}
              {columns.map((s) => {
                const date = parseLocalDate(s.workout_date);
                return (
                  <th key={s.session_id}>
                    <div className="cal-wod-day">{weekdayFmt.format(date)}</div>
                    <div className="cal-wod-date">{dateFmt.format(date)}</div>
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody>{bodyRows}</tbody>
        </table>
      </div>
    </div>
  );
}