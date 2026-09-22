import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { apiGet, apiPost } from "../../api/client";
import { useUser } from "../../context/UserContext";
import { useToast } from "../../context/ToastContext";
import { kgToDisplay, displayToKg } from "../../constants/units";
import { MUSCLE_GROUPS } from "../../constants/muscles";
import "./Workout.css";

// Today's date as YYYY-MM-DD in the browser's local time (not UTC).
function todayStr() {
  const d = new Date();
  const local = new Date(d.getTime() - d.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

export function Workout() {
  const { t, i18n } = useTranslation();
  const { user } = useUser();
  const { showToast } = useToast();
  const isImperial = user?.user_unit_preference === "imperial";
  const weightUnit = isImperial ? "lb" : "kg";

  const [routine, setRoutine] = useState(null);
  const [currentDay, setCurrentDay] = useState(1);
  const [exercises, setExercises] = useState([]);
  const [loading, setLoading] = useState(true);

  const [mode, setMode] = useState("generate"); // "generate" | "workout" | "success"
  const [generating, setGenerating] = useState(false);
  const [saving, setSaving] = useState(false);
  const [prompt, setPrompt] = useState(null); // { type, muscles? }
  const [gapNote, setGapNote] = useState(null); // string[] | null
  const [workout, setWorkout] = useState(null); // built workout data
  const [rows, setRows] = useState({}); // { [exId]: { weight, sets, reps, checked, weightTouched } }

  // Manual (off-routine) logging
  const [manualDate, setManualDate] = useState("");
  const [manualRows, setManualRows] = useState([]); // { exercise_id, name, muscle, sets, reps, weight }
  const [mlActiveMuscle, setMlActiveMuscle] = useState(null);
  const [mlSaving, setMlSaving] = useState(false);

  useEffect(() => {
    const userId = localStorage.getItem("user_id");
    let cancelled = false;
    Promise.all([
      apiGet(`/routine/${userId}`).then((r) => (r.ok ? r.json() : null)).catch(() => null),
      apiGet(`/workout/state/${userId}`).then((r) => (r.ok ? r.json() : null)).catch(() => null),
      apiGet(`/exercises?user_id=${userId}`).then((r) => (r.ok ? r.json() : [])).catch(() => []),
    ]).then(([rt, st, ex]) => {
      if (cancelled) return;
      setRoutine(rt && (rt.days || []).length ? rt : null);
      if (st?.current_day_number) setCurrentDay(st.current_day_number);
      setExercises(Array.isArray(ex) ? ex : []);
    }).finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  const hasRoutine = routine && (routine.days || []).length > 0;
  const muscleList = (arr) => (arr || []).map((m) => t(`muscles.${m}`)).join(", ");
  const dayLabel = (n) => {
    const d = (routine?.days || []).find((x) => x.day_number === n);
    const base = t("common.day", { day: n });
    return d && d.name ? `${base} — ${d.name}` : base;
  };
  const groupsForDay = (dayObj) => {
    if (!dayObj) return [];
    return dayObj.day_type === "manual"
      ? [...new Set((dayObj.exercises || []).map((e) => e.muscle_group))]
      : (dayObj.muscles || []).map((m) => m.muscle_group);
  };

  const goGenerate = () => {
    setMode("generate");
    setWorkout(null);
    setPrompt(null);
    setGapNote(null);
  };

  const selectDay = async (n) => {
    if (n === currentDay) return;
    if (!window.confirm(t("workout.confirmSetDay", { day: dayLabel(n) }))) return;
    const userId = localStorage.getItem("user_id");
    try {
      const res = await apiPost(`/workout/state/${userId}/current-day?day_number=${n}`, {});
      if (!res.ok) throw new Error();
      setCurrentDay(n);
      goGenerate();
    } catch {
      showToast(t("workout.setDayError"), "error");
    }
  };

  const generateWorkout = async (afterAuto = false) => {
    const userId = localStorage.getItem("user_id");
    setGenerating(true);
    setPrompt(null);
    setGapNote(null);
    try {
      if (!hasRoutine) { setPrompt({ type: "noRoutine" }); return; }

      // Read the current day fresh (it may have changed via the day strip).
      const state = await apiGet(`/workout/state/${userId}`).then((r) => (r.ok ? r.json() : null)).catch(() => null);
      const day = state?.current_day_number ?? currentDay;
      setCurrentDay(day);

      const dayObj = (routine.days || []).find((d) => d.day_number === day);
      const todaysIds = new Set((dayObj?.exercises || []).map((e) => e.exercise_id));
      const muscleGroups = groupsForDay(dayObj);
      const isManual = dayObj && dayObj.day_type === "manual";

      // Manual day: the WOD is exactly the day's list, so regenerate on arrival.
      if (isManual && !afterAuto) {
        await apiPost(`/next-workout/generate/${userId}?day_number=${day}`, {});
      }

      const selections = await apiGet(`/next-workout/selections/${userId}`).then((r) => (r.ok ? r.json() : [])).catch(() => []);
      const selectedIds = new Set((selections || []).filter((s) => s.is_selected).map((s) => s.exercise_id));
      const workoutExercises = exercises.filter((ex) => selectedIds.has(ex.exercise_id) && todaysIds.has(ex.exercise_id));

      if (workoutExercises.length === 0) {
        if (isManual) setPrompt({ type: "manualEmpty" });
        else if (afterAuto) setPrompt({ type: "noLibrary", muscles: muscleGroups });
        else setPrompt({ type: "noSelection", muscles: muscleGroups });
        return;
      }

      const wd = {
        day_number: day,
        day_name: dayObj?.name || null,
        day_type: dayObj?.day_type || null,
        day_exercises: dayObj?.exercises || [],
        muscle_groups: muscleGroups,
        exercises: workoutExercises,
      };
      const initRows = {};
      workoutExercises.forEach((ex) => {
        initRows[ex.exercise_id] = {
          weight: kgToDisplay(ex.exercise_user_current_weight, isImperial),
          sets: "",
          reps: "",
          checked: false,
          weightTouched: false,
        };
      });
      setWorkout(wd);
      setRows(initRows);

      // Empty-group note only for auto-generation; manual selections are deliberate.
      if (afterAuto) {
        const covered = new Set(workoutExercises.map((ex) => ex.exercise_muscle_group));
        const empty = muscleGroups.filter((mg) => !covered.has(mg));
        if (empty.length) setGapNote(empty);
      }
      setMode("workout");
    } catch {
      showToast(t("workout.generateError"), "error");
    } finally {
      setGenerating(false);
    }
  };

  const autoGenerateSelection = async () => {
    const userId = localStorage.getItem("user_id");
    setPrompt(null);
    try {
      const res = await apiPost(`/next-workout/generate/${userId}?day_number=${currentDay}`, {});
      if (!res.ok) { showToast(t("workout.autoGenError"), "error"); return; }
      const result = await res.json();
      if ((result.exercises_selected || 0) > 0) {
        await generateWorkout(true);
      } else {
        const dayObj = (routine.days || []).find((d) => d.day_number === currentDay);
        setPrompt({ type: "noLibrary", muscles: groupsForDay(dayObj) });
      }
    } catch {
      showToast(t("workout.autoGenError"), "error");
    }
  };

  const setRow = (exId, patch) => setRows((prev) => ({ ...prev, [exId]: { ...prev[exId], ...patch } }));
  const toggleCheck = (exId) => setRows((prev) => ({ ...prev, [exId]: { ...prev[exId], checked: !prev[exId].checked } }));

  const completeWorkout = async () => {
    if (!workout) return;
    const checked = workout.exercises.filter((ex) => rows[ex.exercise_id]?.checked);
    if (checked.length === 0) { showToast(t("workout.noneChecked"), "error"); return; }

    // Every checked exercise needs a sets value.
    const missing = checked
      .filter((ex) => {
        const s = rows[ex.exercise_id]?.sets;
        return !s || parseInt(s, 10) <= 0;
      })
      .map((ex) => ex.exercise_name);
    if (missing.length) {
      showToast(t("workout.missingSets", { names: missing.join(", ") }), "error");
      return;
    }

    // Only the checked rows; weight is sent back as canonical kg (null when blank).
    const payloadExercises = checked.map((ex) => {
      const r = rows[ex.exercise_id] || {};
      return {
        exercise_id: ex.exercise_id,
        sets_completed: parseInt(r.sets, 10) || 0,
        reps_completed: parseInt(r.reps, 10) || 0,
        weight_used: r.weight === "" || r.weight == null ? null : displayToKg(r.weight, isImperial),
      };
    });

    const userId = localStorage.getItem("user_id");
    setSaving(true);
    try {
      const res = await apiPost(`/workout/complete/${userId}`, {
        day_number: workout.day_number,
        exercises: payloadExercises,
      });
      if (!res.ok) { showToast(t("workout.saveError"), "error"); return; }
      setMode("success");
    } catch {
      showToast(t("workout.saveError"), "error");
    } finally {
      setSaving(false);
    }
  };

  // --- Manual logging ---
  const openManual = () => {
    setPrompt(null);
    setGapNote(null);
    setManualDate(todayStr());
    setManualRows([]);
    const firstWith = MUSCLE_GROUPS.find((m) => exercises.some((ex) => ex.exercise_muscle_group === m));
    setMlActiveMuscle(firstWith || MUSCLE_GROUPS[0]);
    setMode("manual");
  };
  const addManual = (ex) =>
    setManualRows((prev) =>
      prev.some((r) => r.exercise_id === ex.exercise_id)
        ? prev
        : [
            ...prev,
            {
              exercise_id: ex.exercise_id,
              name: ex.exercise_name,
              muscle: ex.exercise_muscle_group,
              sets: "",
              reps: "",
              weight: kgToDisplay(ex.exercise_user_current_weight, isImperial),
            },
          ]
    );
  const removeManual = (exId) => setManualRows((prev) => prev.filter((r) => r.exercise_id !== exId));
  const setManualField = (exId, field, value) =>
    setManualRows((prev) => prev.map((r) => (r.exercise_id === exId ? { ...r, [field]: value } : r)));

  const saveManual = async () => {
    if (manualRows.length === 0) { showToast(t("workout.manualNoExercises"), "error"); return; }
    if (!manualDate) { showToast(t("workout.manualNoDate"), "error"); return; }
    const missing = manualRows.filter((r) => !r.sets || parseInt(r.sets, 10) <= 0).map((r) => r.name);
    if (missing.length) { showToast(t("workout.missingSets", { names: missing.join(", ") }), "error"); return; }

    const userId = localStorage.getItem("user_id");
    setMlSaving(true);
    try {
      const res = await apiPost(`/workout/log-manual/${userId}`, {
        workout_date: manualDate,
        exercises: manualRows.map((r) => ({
          exercise_id: r.exercise_id,
          sets_completed: parseInt(r.sets, 10) || 0,
          reps_completed: parseInt(r.reps, 10) || 0,
          weight_used: r.weight === "" || r.weight == null ? null : displayToKg(r.weight, isImperial),
        })),
      });
      if (!res.ok) { showToast(t("workout.saveError"), "error"); return; }
      setMode("success");
    } catch {
      showToast(t("workout.saveError"), "error");
    } finally {
      setMlSaving(false);
    }
  };

  // Ordered rows: manual grouped days as A1/A2/B1..., otherwise grouped by muscle.
  const renderRows = () => {
    if (!workout) return [];
    const exs = workout.exercises;
    const dayExs = workout.day_exercises || [];
    const isManualGrouped =
      workout.day_type === "manual" &&
      dayExs.some((e) => e.group_index !== null && e.group_index !== undefined);

    if (isManualGrouped) {
      const byId = {};
      exs.forEach((ex) => { byId[ex.exercise_id] = ex; });
      const letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
      const giLetter = {};
      const giCount = {};
      let nextL = 0;
      const out = [];
      dayExs.forEach((de) => {
        const ex = byId[de.exercise_id];
        if (!ex) return;
        const gi = de.group_index;
        if (!(gi in giLetter)) { giLetter[gi] = letters[nextL++] || "?"; giCount[gi] = 0; }
        giCount[gi] += 1;
        out.push({ ex, label: `${giLetter[gi]}${giCount[gi]}` });
      });
      return out;
    }

    const byMuscle = {};
    exs.forEach((ex) => {
      (byMuscle[ex.exercise_muscle_group] = byMuscle[ex.exercise_muscle_group] || []).push(ex);
    });
    const out = [];
    Object.keys(byMuscle).sort().forEach((mg) => {
      byMuscle[mg].forEach((ex) => out.push({ ex, label: t(`muscles.${mg}`) }));
    });
    return out;
  };

  const dateHeader = new Intl.DateTimeFormat(i18n.language, {
    weekday: "long",
    day: "numeric",
    month: "long",
  }).format(new Date());

  const renderPrompt = () => {
    if (!prompt) return null;
    const muscles = muscleList(prompt.muscles);
    switch (prompt.type) {
      case "noRoutine":
        return (
          <div className="wo-prompt">
            {t("workout.promptNoRoutine")}{" "}
            <Link to="/routine" className="wo-prompt-link">{t("workout.linkCreateRoutine")}</Link>
          </div>
        );
      case "noSelection":
        return (
          <div className="wo-prompt">
            {t("workout.promptNoSelection")}{" "}
            <Link to="/calendar" className="wo-prompt-link">{t("workout.linkCalendar")}</Link>
            {" · "}
            <button type="button" className="wo-prompt-link wo-linkbtn" onClick={autoGenerateSelection}>
              {t("workout.linkAutoSelect")}
            </button>
          </div>
        );
      case "noLibrary":
        return (
          <div className="wo-prompt">
            {t("workout.promptNoLibrary", { muscles })}{" "}
            <Link to="/exercises" className="wo-prompt-link">{t("workout.linkAddExercises")}</Link>
          </div>
        );
      case "manualEmpty":
        return (
          <div className="wo-prompt">
            {t("workout.promptManualEmpty")}{" "}
            <Link to="/routine" className="wo-prompt-link">{t("workout.linkEditRoutine")}</Link>
          </div>
        );
      default:
        return null;
    }
  };

  if (loading) return <p className="wo-muted">{t("common.loading")}</p>;

  return (
    <div className="wo">
      <h1 className="wo-title">{t("workout.title")}</h1>

      {mode === "generate" && (
        <div className="wo-generate">
          {hasRoutine && (
            <div className="wo-day-strip">
              {(routine.days || []).map((d) => {
                const isCurrent = d.day_number === currentDay;
                return (
                  <button
                    key={d.day_number}
                    type="button"
                    className={`wo-day-chip${isCurrent ? " current" : ""}`}
                    onClick={() => selectDay(d.day_number)}
                  >
                    {dayLabel(d.day_number)}
                    {isCurrent && <span className="wo-day-tag">{t("workout.current")}</span>}
                  </button>
                );
              })}
            </div>
          )}

          <p className="wo-instruction">{t("workout.generateInstruction")}</p>

          <div className="wo-actions">
            <button
              type="button"
              className="wo-btn"
              onClick={() => generateWorkout(false)}
              disabled={generating}
            >
              {generating ? t("workout.loading") : `🏋️ ${t("workout.generateBtn")}`}
            </button>
            <button
              type="button"
              className="wo-btn secondary"
              onClick={openManual}
              disabled={generating}
            >
              {`✎ ${t("workout.logManual")}`}
            </button>
          </div>

          {renderPrompt()}
        </div>
      )}

      {mode === "workout" && workout && (
        <div className="wo-workout">
          <div className="wo-dateline">
            <span className="wo-date">{dateHeader}</span>
            <span className="wo-dayinfo">
              {`${t("common.day", { day: workout.day_number })} — ${workout.day_name || muscleList(workout.muscle_groups)}`}
            </span>
          </div>

          {gapNote && (
            <div className="wo-gap-note">
              {t("workout.gapNote", { muscles: muscleList(gapNote) })}{" "}
              <Link to="/exercises" className="wo-prompt-link">{t("workout.linkAddExercises")}</Link>
            </div>
          )}

          <table className="wo-table">
            <thead>
              <tr>
                <th>{t("workout.colMuscle")}</th>
                <th>{t("workout.colExercise")}</th>
                <th className="center">{t("workout.colWeight")} ({weightUnit})</th>
                <th className="center">{t("workout.colSets")}</th>
                <th className="center">{t("workout.colReps")}</th>
                <th className="center wo-check-col">✓</th>
              </tr>
            </thead>
            <tbody>
              {renderRows().map(({ ex, label }) => {
                const r = rows[ex.exercise_id] || {};
                return (
                  <tr key={ex.exercise_id} className={r.checked ? "checked-row" : ""}>
                    <td className="wo-muscle-cell">{label}</td>
                    <td className="wo-exname-cell">{ex.exercise_name}</td>
                    <td className="center">
                      <input
                        type="number"
                        inputMode="decimal"
                        min="0"
                        step="0.5"
                        className={`wo-input wo-weight${r.weightTouched ? " modified" : ""}`}
                        value={r.weight ?? ""}
                        placeholder="0"
                        aria-label={`${t("workout.colWeight")} (${weightUnit}) — ${ex.exercise_name}`}
                        onChange={(e) => setRow(ex.exercise_id, { weight: e.target.value, weightTouched: true })}
                      />
                    </td>
                    <td className="center">
                      <input
                        type="number"
                        inputMode="numeric"
                        min="1"
                        max="10"
                        className="wo-input"
                        value={r.sets ?? ""}
                        placeholder="–"
                        aria-label={`${t("workout.colSets")} — ${ex.exercise_name}`}
                        onChange={(e) => setRow(ex.exercise_id, { sets: e.target.value })}
                      />
                    </td>
                    <td className="center">
                      <input
                        type="number"
                        inputMode="numeric"
                        min="1"
                        max="50"
                        className="wo-input"
                        value={r.reps ?? ""}
                        placeholder="–"
                        aria-label={`${t("workout.colReps")} — ${ex.exercise_name}`}
                        onChange={(e) => setRow(ex.exercise_id, { reps: e.target.value })}
                      />
                    </td>
                    <td className="center">
                      <button
                        type="button"
                        className={`wo-check${r.checked ? " checked" : ""}`}
                        aria-pressed={!!r.checked}
                        aria-label={t("workout.markExercise", { name: ex.exercise_name })}
                        onClick={() => toggleCheck(ex.exercise_id)}
                      />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>

          <div className="wo-actions">
            <button type="button" className="wo-btn" onClick={completeWorkout} disabled={saving}>
              {saving ? t("workout.saving") : `✓ ${t("workout.markComplete")}`}
            </button>
            <button type="button" className="wo-btn negative" onClick={goGenerate} disabled={saving}>
              {`✕ ${t("workout.cancel")}`}
            </button>
          </div>
        </div>
      )}

      {mode === "manual" && (
        <div className="wo-manual">
          <div className="wo-ml-title">{t("workout.manualTitle")}</div>
          <div className="wo-ml-dateline">
            <label className="wo-ml-datelabel" htmlFor="wo-ml-date">{t("workout.date")}</label>
            <input
              id="wo-ml-date"
              type="date"
              className="wo-ml-date"
              value={manualDate}
              max={todayStr()}
              onChange={(e) => setManualDate(e.target.value)}
            />
          </div>

          <div className="wo-ml-grid">
            <div className="wo-ml-col-left">
              {manualRows.length === 0 ? (
                <p className="wo-ml-empty">{t("workout.manualListEmpty")}</p>
              ) : (
                <div className="wo-ml-list">
                  <div className="wo-ml-head">
                    <span className="wo-ml-muscle" />
                    <span className="wo-ml-name" />
                    <span className="wo-ml-h">{t("workout.colSets")}</span>
                    <span className="wo-ml-h">{t("workout.colReps")}</span>
                    <span className="wo-ml-h">{`${t("workout.wtShort")} (${weightUnit})`}</span>
                    <span className="wo-ml-delcol" />
                  </div>
                  {manualRows.map((r) => (
                    <div className="wo-ml-row" key={r.exercise_id}>
                      <span className="wo-ml-muscle">{t(`muscles.${r.muscle}`)}</span>
                      <span className="wo-ml-name">{r.name}</span>
                      <input
                        type="number"
                        min="1"
                        max="20"
                        className="wo-ml-input"
                        value={r.sets}
                        placeholder="–"
                        aria-label={`${t("workout.colSets")} — ${r.name}`}
                        onChange={(e) => setManualField(r.exercise_id, "sets", e.target.value)}
                      />
                      <input
                        type="number"
                        min="1"
                        max="50"
                        className="wo-ml-input"
                        value={r.reps}
                        placeholder="–"
                        aria-label={`${t("workout.colReps")} — ${r.name}`}
                        onChange={(e) => setManualField(r.exercise_id, "reps", e.target.value)}
                      />
                      <input
                        type="number"
                        min="0"
                        step="0.5"
                        className="wo-ml-input"
                        value={r.weight}
                        placeholder="–"
                        aria-label={`${t("workout.colWeight")} (${weightUnit}) — ${r.name}`}
                        onChange={(e) => setManualField(r.exercise_id, "weight", e.target.value)}
                      />
                      <button
                        type="button"
                        className="wo-ml-del"
                        aria-label={t("workout.remove", { name: r.name })}
                        onClick={() => removeManual(r.exercise_id)}
                      >
                        ×
                      </button>
                    </div>
                  ))}
                </div>
              )}

              <div className="wo-actions">
                <button type="button" className="wo-btn" onClick={saveManual} disabled={mlSaving}>
                  {mlSaving ? t("workout.saving") : `✓ ${t("workout.logWod")}`}
                </button>
                <button type="button" className="wo-btn negative" onClick={goGenerate} disabled={mlSaving}>
                  {`← ${t("workout.back")}`}
                </button>
              </div>
            </div>

            <div className="wo-ml-col-right">
              <div className="wo-ml-picker">
                <div className="wo-ml-tabs">
                  {MUSCLE_GROUPS.map((m) => (
                    <button
                      key={m}
                      type="button"
                      className={`wo-ml-tab${m === mlActiveMuscle ? " active" : ""}`}
                      onClick={() => setMlActiveMuscle(m)}
                    >
                      {t(`muscles.${m}`)}
                    </button>
                  ))}
                </div>
                {(() => {
                  const exs = exercises.filter((ex) => ex.exercise_muscle_group === mlActiveMuscle);
                  const addedIds = new Set(manualRows.map((r) => r.exercise_id));
                  if (exs.length === 0) {
                    return (
                      <p className="wo-ml-none">
                        {t("workout.noMuscleExercises", { muscle: t(`muscles.${mlActiveMuscle}`) })}{" "}
                        <Link to="/exercises" className="wo-prompt-link">{t("workout.linkAddExercises")}</Link>
                      </p>
                    );
                  }
                  return exs.map((ex) => {
                    const added = addedIds.has(ex.exercise_id);
                    return (
                      <div className="wo-ml-pick-row" key={ex.exercise_id}>
                        <button
                          type="button"
                          className="wo-ml-add"
                          disabled={added}
                          onClick={() => addManual(ex)}
                        >
                          {added ? t("workout.added") : `+ ${t("workout.addBtn")}`}
                        </button>
                        <span className="wo-ml-name">{ex.exercise_name}</span>
                      </div>
                    );
                  });
                })()}
              </div>
            </div>
          </div>
        </div>
      )}

      {mode === "success" && (
        <div className="wo-success">
          <p className="wo-success-msg">🎉 {t("workout.successMsg")}</p>
          <div className="wo-actions">
            <Link to="/dashboard" className="wo-btn">← {t("workout.backToDashboard")}</Link>
          </div>
        </div>
      )}
    </div>
  );
}