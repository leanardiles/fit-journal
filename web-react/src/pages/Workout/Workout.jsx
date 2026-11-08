import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { apiGet, apiPost } from "../../api/client";
import { useUser } from "../../context/UserContext";
import { useToast } from "../../context/ToastContext";
import { formatList } from "../../i18n/format";
import { kgToDisplay, displayToKg, unitSymbol } from "../../constants/units";
import { MUSCLE_GROUPS } from "../../constants/muscles";
import { Modal } from "../../components/Modal/Modal";
import { Select } from "../../components/Select/Select";
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
  // Symbol comes from CLDR via Intl rather than being hardcoded, so a locale
  // that abbreviates differently gets its own form without a new key. The
  // literal is a fallback: unitSymbol returns an empty string if the runtime
  // lacks the unit, and an empty weight header is worse than an English one.
  const weightUnit =
    unitSymbol(i18n.language, isImperial ? "pound" : "kilogram") || (isImperial ? "lb" : "kg");

  const [routine, setRoutine] = useState(null);
  const [currentDay, setCurrentDay] = useState(1);
  const [exercises, setExercises] = useState([]);
  const [loading, setLoading] = useState(true);

  const [mode, setMode] = useState("generate"); // "generate" | "workout" | "manual"
  const [generating, setGenerating] = useState(false);
  const [saving, setSaving] = useState(false);
  // The date the workout is logged under. Defaults to today, because that is
  // the common case, but a workout done yesterday and logged this morning
  // should not be filed under today.
  const [workoutDate, setWorkoutDate] = useState(todayStr());
  const [prompt, setPrompt] = useState(null); // { type, muscles? }
  const [gapNote, setGapNote] = useState(null); // string[] | null
  const [workout, setWorkout] = useState(null); // built workout data
  const [rows, setRows] = useState({}); // { [exId]: { weight, sets, reps, weightTouched } }
  // Edits to the generated list, kept locally: nothing is written back to the
  // routine, so regenerating starts clean.
  const [removedIds, setRemovedIds] = useState([]);
  const [extraIds, setExtraIds] = useState([]);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [pickMuscle, setPickMuscle] = useState("all");
  const [pickQuery, setPickQuery] = useState("");
  // Validation lives next to the thing being validated and stays until it is
  // fixed. A toast cannot: it expires while the user is still reading the row
  // it was complaining about.
  const [woError, setWoError] = useState(null);
  const [mlError, setMlError] = useState(null);
  const [mlMissing, setMlMissing] = useState([]); // exercise_ids lacking sets

// A blank optional field is absent, not zero: || 0 would record a number the
// user never entered and the row could no longer say it was not measured.
const numOrNull = (v) => {
  const n = parseInt(v, 10);
  return Number.isFinite(n) ? n : null;
};

// An exercise counts as done once it has a positive set count. Derived, not
// stored: clearing the field or typing 0 undoes it with no second piece of
// state to keep in sync. Sets is the right signal because nothing prefills
// it, unlike weight, which is seeded from the exercise's current weight.
const isDone = (row) => {
  const sets = parseInt(row?.sets, 10);
  return Number.isFinite(sets) && sets > 0;
};

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
  const muscleList = (arr) => formatList((arr || []).map((m) => t(`muscles.${m}`)));
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

  // Logging mutates server state the page is holding a copy of: the routine
  // cursor advances, and each logged exercise's current weight is overwritten,
  // which is what seeds a prefilled weight next time. Returning to the chooser
  // without refetching leaves the day strip on the day just finished and
  // Generate WOD regenerating it. The old success screen hid this by linking
  // to the dashboard, which unmounted the page.
  const refreshAfterLog = async () => {
    try {
      const userId = localStorage.getItem("user_id");
      const [st, ex] = await Promise.all([
        apiGet(`/workout/state/${userId}`).then((r) => (r.ok ? r.json() : null)).catch(() => null),
        apiGet(`/exercises?user_id=${userId}`).then((r) => (r.ok ? r.json() : [])).catch(() => []),
      ]);
      if (st?.current_day_number) setCurrentDay(st.current_day_number);
      if (Array.isArray(ex)) setExercises(ex);
    } catch {
      // The workout is already saved. A stale day strip is worth far less than
      // a false alarm, so swallow this rather than letting the caller's catch
      // report a save that succeeded as a failure.
    }
  };

  const goGenerate = () => {
    setMode("generate");
    setWorkout(null);
    setPrompt(null);
    setGapNote(null);
  };

  const selectDay = async (n) => {
    if (n === currentDay) return;
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
          weightTouched: false,
        };
      });
      setWorkout(wd);
      setRows(initRows);
      setRemovedIds([]);
      setExtraIds([]);

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

  const setRow = (exId, patch) => {
    setWoError(null);
    setRows((prev) => ({ ...prev, [exId]: { ...prev[exId], ...patch } }));
  };

  // What the user is actually looking at: the generated list minus what they
  // removed, plus anything they added. Rendering and the payload both read
  // this, so the two cannot drift apart.
  const activeExercises = () => {
    if (!workout) return [];
    const gone = new Set(removedIds);
    const extras = extraIds
      .map((id) => exercises.find((ex) => ex.exercise_id === id))
      .filter(Boolean);
    return [...workout.exercises.filter((ex) => !gone.has(ex.exercise_id)), ...extras];
  };

  const removeFromWorkout = (exId) => {
    setExtraIds((prev) => prev.filter((id) => id !== exId));
    setRemovedIds((prev) => (prev.includes(exId) ? prev : [...prev, exId]));
    setRow(exId, { sets: "", reps: "" });
  };

  // Adding back something removed restores it in place rather than appending
  // a second copy of the same exercise.
  const addToWorkout = (ex) => {
    const id = ex.exercise_id;
    setRemovedIds((prev) => prev.filter((x) => x !== id));
    if (!(workout?.exercises || []).some((e) => e.exercise_id === id)) {
      setExtraIds((prev) => (prev.includes(id) ? prev : [...prev, id]));
    }
    setRows((prev) => ({
      ...prev,
      [id]: prev[id] ?? {
        weight: kgToDisplay(ex.exercise_user_current_weight, isImperial),
        sets: "",
        reps: "",
        weightTouched: false,
      },
    }));
  };

  const completeWorkout = async () => {
    if (!workout) return;
    const done = activeExercises().filter((ex) => isDone(rows[ex.exercise_id]));
    if (done.length === 0) { setWoError(t("workout.noSetsEntered")); return; }
    setWoError(null);

    // Only the done rows; reps and weight are null when blank, weight in kg.
    const payloadExercises = done.map((ex) => {
      const r = rows[ex.exercise_id] || {};
      return {
        exercise_id: ex.exercise_id,
        sets_completed: parseInt(r.sets, 10) || 0,
        reps_completed: numOrNull(r.reps),
        weight_used: r.weight === "" || r.weight == null ? null : displayToKg(r.weight, isImperial),
      };
    });

    const userId = localStorage.getItem("user_id");
    setSaving(true);
    try {
      const res = await apiPost(`/workout/complete/${userId}`, {
        day_number: workout.day_number,
        exercises: payloadExercises,
        workout_date: workoutDate,
      });
      if (!res.ok) { showToast(t("workout.saveError"), "error"); return; }
      showToast(
        t("workout.loggedWithDay", {
          count: done.length,
          day: t("common.day", { day: workout.day_number }),
        }),
        "success",
        4000,
      );
      await refreshAfterLog();
      goGenerate();
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
  const clearMlError = () => { setMlError(null); setMlMissing([]); };

  const addManual = (ex) => {
    clearMlError();
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
  };

  const removeManual = (exId) => {
    clearMlError();
    setManualRows((prev) => prev.filter((r) => r.exercise_id !== exId));
  };

  const setManualField = (exId, field, value) => {
    clearMlError();
    setManualRows((prev) => prev.map((r) => (r.exercise_id === exId ? { ...r, [field]: value } : r)));
  };

  const saveManual = async () => {
    if (manualRows.length === 0) { setMlError(t("workout.manualNoExercises")); return; }
    if (!manualDate) { setMlError(t("workout.manualNoDate")); return; }
    const missing = manualRows.filter((r) => !r.sets || parseInt(r.sets, 10) <= 0);
    if (missing.length) {
      // Naming the rows is not enough on a long list, so the offending inputs
      // are marked too and carry aria-invalid.
      setMlMissing(missing.map((r) => r.exercise_id));
      setMlError(
        t("workout.missingSets", {
          names: formatList(missing.map((r) => r.name), { type: "conjunction" }),
        }),
      );
      return;
    }
    setMlError(null);
    setMlMissing([]);

    const userId = localStorage.getItem("user_id");
    setMlSaving(true);
    try {
      const res = await apiPost(`/workout/log-manual/${userId}`, {
        workout_date: manualDate,
        exercises: manualRows.map((r) => ({
          exercise_id: r.exercise_id,
          sets_completed: parseInt(r.sets, 10) || 0,
          reps_completed: numOrNull(r.reps),
          weight_used: r.weight === "" || r.weight == null ? null : displayToKg(r.weight, isImperial),
        })),
      });
      if (!res.ok) { showToast(t("workout.saveError"), "error"); return; }
      showToast(t("workout.logged", { count: manualRows.length }), "success", 4000);
      await refreshAfterLog();
      goGenerate();
    } catch {
      showToast(t("workout.saveError"), "error");
    } finally {
      setMlSaving(false);
    }
  };

  // Ordered rows: manual grouped days as A1/A2/B1..., otherwise grouped by muscle.
  const renderRows = () => {
    if (!workout) return [];
    const exs = activeExercises();
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
      // Hand-added exercises are not part of the day, so they have no group
      // letter. They follow the grouped rows, labelled by muscle.
      const placed = new Set(out.map((o) => o.ex.exercise_id));
      exs
        .filter((ex) => !placed.has(ex.exercise_id))
        .forEach((ex) => out.push({ ex, label: t(`muscles.${ex.exercise_muscle_group}`) }));
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

  const fold = (s) =>
    (s || "")
      .toLocaleLowerCase(i18n.language)
      .normalize("NFD")
      .replace(/\p{Diacritic}/gu, "");

  const renderPicker = () => {
    const onScreen = new Set(activeExercises().map((ex) => ex.exercise_id));
    const q = fold(pickQuery.trim());
    const matches = exercises.filter(
      (ex) =>
        (pickMuscle === "all" || ex.exercise_muscle_group === pickMuscle) &&
        (q === "" || fold(ex.exercise_name).includes(q))
    );

    // Three ways to come up empty, and they are not the same problem.
    let emptyMsg;
    if (q) emptyMsg = t("workout.noSearchResults", { query: pickQuery.trim() });
    else if (pickMuscle === "all") emptyMsg = t("workout.libraryEmpty");
    else emptyMsg = t("workout.noMuscleExercises", { muscle: t(`muscles.${pickMuscle}`) });

    return (
      <Modal open={pickerOpen} onClose={() => setPickerOpen(false)} titleKey="workout.addExerciseTitle">
        <input
          type="search"
          className="wo-pick-search"
          value={pickQuery}
          autoFocus
          placeholder={t("workout.searchPlaceholder")}
          aria-label={t("workout.searchLabel")}
          onChange={(e) => setPickQuery(e.target.value)}
        />
        <Select
          inline
          labelKey="exercises.fieldMuscle"
          value={pickMuscle}
          onChange={setPickMuscle}
          options={[
            { value: "all", label: t("workout.allMuscles") },
            ...MUSCLE_GROUPS.map((m) => ({ value: m, label: t(`muscles.${m}`) })),
          ]}
        />

        <div className="wo-pick-list">
          {matches.length === 0 ? (
            <p className="wo-ml-none">
              {emptyMsg}{" "}
              <Link to="/exercises" className="wo-prompt-link">{t("workout.linkAddExercises")}</Link>
            </p>
          ) : (
            matches.map((ex) => {
              const added = onScreen.has(ex.exercise_id);
              return (
                <div className="wo-ml-pick-row" key={ex.exercise_id}>
                  <button
                    type="button"
                    className="wo-ml-add"
                    disabled={added}
                    onClick={() => addToWorkout(ex)}
                  >
                    {added ? t("workout.added") : `+ ${t("workout.addBtn")}`}
                  </button>
                  <span className="wo-ml-name">{ex.exercise_name}</span>
                  {pickMuscle === "all" && (
                    <span className="wo-pick-muscle">{t(`muscles.${ex.exercise_muscle_group}`)}</span>
                  )}
                </div>
              );
            })
          )}
        </div>

        <div className="wo-actions wo-pick-actions">
          <button type="button" className="btn btn--secondary" onClick={() => setPickerOpen(false)}>
            {t("workout.doneBtn")}
          </button>
        </div>
      </Modal>
    );
  };

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

  // The header date control. Both flows log to a date, so they share one
  // field rather than each screen placing its own.
  const dateField =
    mode === "manual"
      ? { id: "wo-ml-date", value: manualDate, set: setManualDate }
      : { id: "wo-date", value: workoutDate, set: setWorkoutDate };
  const showHeadDate = mode === "manual" || (mode === "workout" && !!workout);

  return (
    <div className={`wo${mode === "manual" ? " wo--fill" : ""}`}>
      <div className={`wo-head${mode === "manual" ? " wo-head--center-date" : ""}`}>
        <h1 className="wo-title">
          {t("workout.title")}{" "}
          {/* Kept in the DOM when idle so the date never shifts between the
              generated and the manual screen. Hidden from screen readers too. */}
          <span className={mode === "manual" ? "wo-title-tag" : "wo-title-tag is-idle"}>
            ({t("workout.manual")})
          </span>
        </h1>
        {showHeadDate && (
          <div className="wo-head-date">
            <label className="wo-date-label" htmlFor={dateField.id}>{t("workout.date")}</label>
            <input
              id={dateField.id}
              type="date"
              className="wo-date-input"
              value={dateField.value}
              max={todayStr()}
              onChange={(e) => { clearMlError(); dateField.set(e.target.value); }}
            />
          </div>
        )}
        {mode === "workout" && workout && (
          <span className="wo-dayinfo">
            {t("common.dayNamed", {
              day: workout.day_number,
              name: workout.day_name || muscleList(workout.muscle_groups),
            })}
          </span>
        )}
      </div>

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
              className="btn btn--primary"
              onClick={() => generateWorkout(false)}
              disabled={generating}
            >
              {generating ? t("workout.loading") : `🏋️ ${t("workout.generateBtn")}`}
            </button>
            <button
              type="button"
              className="btn btn--secondary"
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
                <th className="center">{t("workout.colSets")}</th>
                <th className="center wo-reps-col">{t("workout.colReps")}</th>
                <th className="center">
                  <span className="wo-col-main">{t("workout.colWeight")}</span>
                  <span className="wo-col-unit">{weightUnit}</span>
                </th>
                <th className="wo-remove-col">
                  <span className="sr-only">{t("workout.colRemove")}</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {renderRows().map(({ ex, label }) => {
                const r = rows[ex.exercise_id] || {};
                const done = isDone(r);
                return (
                  <tr key={ex.exercise_id} className={done ? "done-row" : ""}>
                    <td className="wo-muscle-cell">{label}</td>
                    <td className="wo-exname-cell" title={ex.exercise_name}>{ex.exercise_name}</td>
                    <td className="center">
                      <input
                        type="number"
                        inputMode="numeric"
                        min="1"
                        max="10"
                        className="wo-input"
                        value={r.sets ?? ""}
                        placeholder="–"
                        aria-label={t("workout.setsFor", { name: ex.exercise_name })}
                        onChange={(e) => setRow(ex.exercise_id, { sets: e.target.value })}
                      />
                    </td>
                    <td className="center wo-reps-col">
                      <input
                        type="number"
                        inputMode="numeric"
                        min="1"
                        max="50"
                        className="wo-input"
                        value={r.reps ?? ""}
                        placeholder="–"
                        aria-label={t("workout.repsFor", { name: ex.exercise_name })}
                        onChange={(e) => setRow(ex.exercise_id, { reps: e.target.value })}
                      />
                    </td>
                    <td className="center">
                      <input
                        type="number"
                        inputMode="decimal"
                        min="0"
                        step="0.5"
                        className={`wo-input wo-weight${r.weightTouched ? " modified" : ""}`}
                        value={r.weight ?? ""}
                        placeholder="0"
                        aria-label={t("workout.weightFor", { unit: weightUnit, name: ex.exercise_name })}
                        onChange={(e) => setRow(ex.exercise_id, { weight: e.target.value, weightTouched: true })}
                      />
                    </td>
                    <td className="wo-remove-col">
                      <button
                        type="button"
                        className="btn-remove"
                        aria-label={t("workout.remove", { name: ex.exercise_name })}
                        onClick={() => removeFromWorkout(ex.exercise_id)}
                      >
                        ✕
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>

          {/* Plans survive contact with the gym only so often. */}
          <div className="wo-add-row">
            <button type="button" className="btn btn--secondary" onClick={() => setPickerOpen(true)}>
              {`+ ${t("workout.addExerciseBtn")}`}
            </button>
          </div>

          {renderPicker()}

          {woError && <p className="wo-error" role="alert">{woError}</p>}

          <div className="wo-actions">
            <button type="button" className="btn btn--primary" onClick={completeWorkout} disabled={saving}>
              {saving ? t("workout.saving") : `✓ ${t("workout.markComplete")}`}
            </button>
            <button type="button" className="btn btn--negative" onClick={goGenerate} disabled={saving}>
              {`✕ ${t("workout.cancel")}`}
            </button>
          </div>
        </div>
      )}

      {mode === "manual" && (
        <div className="wo-manual">
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
                    <span className="wo-ml-h wo-reps-col">{t("workout.colReps")}</span>
                    <span className="wo-ml-h">{t("workout.wtWithUnit", { unit: weightUnit })}</span>
                    <span className="wo-ml-delcol" />
                  </div>
                  {manualRows.map((r) => (
                    <div className={`wo-ml-row${isDone(r) ? " done" : ""}`} key={r.exercise_id}>
                      <span className="wo-ml-muscle">{t(`muscles.${r.muscle}`)}</span>
                      <span className="wo-ml-name">{r.name}</span>
                      <input
                        type="number"
                        min="1"
                        max="20"
                        className={`wo-ml-input${mlMissing.includes(r.exercise_id) ? " invalid" : ""}`}
                        value={r.sets}
                        placeholder="–"
                        aria-label={t("workout.setsFor", { name: r.name })}
                        aria-invalid={mlMissing.includes(r.exercise_id) || undefined}
                        onChange={(e) => setManualField(r.exercise_id, "sets", e.target.value)}
                      />
                      <input
                        type="number"
                        min="1"
                        max="50"
                        className="wo-ml-input wo-reps-col"
                        value={r.reps}
                        placeholder="–"
                        aria-label={t("workout.repsFor", { name: r.name })}
                        onChange={(e) => setManualField(r.exercise_id, "reps", e.target.value)}
                      />
                      <input
                        type="number"
                        min="0"
                        step="0.5"
                        className="wo-ml-input"
                        value={r.weight}
                        placeholder="–"
                        aria-label={t("workout.weightFor", { unit: weightUnit, name: r.name })}
                        onChange={(e) => setManualField(r.exercise_id, "weight", e.target.value)}
                      />
                      <button
                        type="button"
                        className="btn-remove"
                        aria-label={t("workout.remove", { name: r.name })}
                        onClick={() => removeManual(r.exercise_id)}
                      >
                        ×
                      </button>
                    </div>
                  ))}
                </div>
              )}
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
                <div className="wo-ml-picklist">
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

          {mlError && <p className="wo-error" role="alert">{mlError}</p>}

          <div className="wo-actions">
            <button type="button" className="btn btn--primary" onClick={saveManual} disabled={mlSaving}>
              {mlSaving ? t("workout.saving") : `✓ ${t("workout.logWod")}`}
            </button>
            <button type="button" className="btn btn--negative" onClick={goGenerate} disabled={mlSaving}>
              <span className="mirror-rtl" aria-hidden="true">←</span> {t("workout.back")}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}