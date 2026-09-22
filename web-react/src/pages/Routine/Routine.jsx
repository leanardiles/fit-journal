import { useState, useEffect, useMemo } from "react";
import { useTranslation, Trans } from "react-i18next";
import { apiGet, apiPost } from "../../api/client";
import { useToast } from "../../context/ToastContext";
import { MUSCLE_GROUPS } from "../../constants/muscles";
import "./Routine.css";

const DEFAULT_COUNT = 3;
const emptyDay = () => ({ type: null, name: "", muscles: [], counts: {}, checked: {} });

export function Routine() {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const userId = localStorage.getItem("user_id");

  const [routine, setRoutine] = useState(null);
  const [exercises, setExercises] = useState([]);
  const [loading, setLoading] = useState(true);
  const [mode, setMode] = useState("view"); // 'view' | 'new' | 'edit'
  const [openPools, setOpenPools] = useState({}); // view: per-day pool expander

  // Editor state
  const [daysPerWeek, setDaysPerWeek] = useState(0);
  const [days, setDays] = useState({}); // { [n]: { type, name, muscles:[], counts:{}, checked:{} } }
  const [editorPools, setEditorPools] = useState({}); // per_muscle pool expander
  const [manualTabs, setManualTabs] = useState({}); // manual active muscle tab per day
  const [saveError, setSaveError] = useState(null); // { day, msg }

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      apiGet(`/routine/${userId}`).then((r) => (r.ok ? r.json() : null)),
      apiGet(`/exercises?user_id=${userId}`).then((r) => (r.ok ? r.json() : [])),
    ])
      .then(([rt, ex]) => {
        if (cancelled) return;
        setRoutine(rt);
        setExercises(Array.isArray(ex) ? ex : []);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const exercisesByMuscle = useMemo(() => {
    const map = {};
    MUSCLE_GROUPS.forEach((m) => (map[m] = []));
    exercises.forEach((ex) => {
      (map[ex.exercise_muscle_group] = map[ex.exercise_muscle_group] || []).push(ex);
    });
    return map;
  }, [exercises]);

  // ---------- view: exercise names grouped by muscle ----------
  const exRows = (day) => {
    const byMuscle = {};
    (day.exercises || []).forEach((ex) => {
      (byMuscle[ex.muscle_group] = byMuscle[ex.muscle_group] || []).push(ex.exercise_name);
    });
    const out = [];
    MUSCLE_GROUPS.forEach((m) => {
      const names = byMuscle[m];
      if (!names || !names.length) return;
      names.forEach((name, idx) => {
        out.push({ key: `${m}-${idx}`, muscle: idx === 0 ? t(`muscles.${m}`) : "", name });
      });
    });
    return out;
  };

  // ---------- enter editor ----------
  const startNew = () => {
    setDays({});
    setDaysPerWeek(0);
    setEditorPools({});
    setManualTabs({});
    setSaveError(null);
    setMode("new");
  };

  // Prefill the editor from the saved routine (mirrors the legacy edit flow).
  const enterEdit = () => {
    if (!routine || !routine.days_per_week) {
      startNew();
      return;
    }
    const nextDays = {};
    routine.days.forEach((d) => {
      const st = { type: d.day_type, name: d.name || "", muscles: [], counts: {}, checked: {} };
      if (d.day_type === "manual") {
        (d.exercises || []).forEach((e) => {
          st.checked[e.exercise_id] = true;
        });
      } else {
        const savedIds = new Set((d.exercises || []).map((e) => e.exercise_id));
        (d.muscles || []).forEach((m) => {
          st.muscles.push(m.muscle_group);
          st.counts[m.muscle_group] = m.exercise_count;
        });
        st.muscles.forEach((muscle) => {
          (exercisesByMuscle[muscle] || []).forEach((ex) => {
            st.checked[ex.exercise_id] = savedIds.has(ex.exercise_id);
          });
        });
      }
      nextDays[d.day_number] = st;
    });
    setDays(nextDays);
    setDaysPerWeek(routine.days_per_week);
    setEditorPools({});
    setManualTabs({});
    setSaveError(null);
    setMode("edit");
  };

  // ---------- editor state updates ----------
  const selectDaysPerWeek = (n) => {
    setDaysPerWeek(n);
    setDays((prev) => {
      const next = { ...prev };
      for (let d = 1; d <= n; d++) if (!next[d]) next[d] = emptyDay();
      return next; // days beyond n stay in memory; payload only uses 1..n
    });
  };

  const addDay = () => {
    if (daysPerWeek >= 7) return;
    const n = daysPerWeek + 1;
    setDays((prev) => ({ ...prev, [n]: emptyDay() }));
    setDaysPerWeek(n);
  };

  const deleteDay = (d) => {
    if (daysPerWeek <= 1) {
      showToast(t("routine.errMinOneDay"), "error");
      return;
    }
    const st = days[d];
    const hasContent = !!st && (st.muscles.length > 0 || Object.values(st.checked).some(Boolean));
    if (hasContent && !window.confirm(t("routine.confirmDeleteDay", { day: d }))) return;
    setDays((prev) => {
      const next = {};
      for (let i = 1; i < daysPerWeek; i++) next[i] = i < d ? prev[i] : prev[i + 1];
      return next;
    });
    setDaysPerWeek((n) => n - 1);
    setEditorPools({});
    setManualTabs({});
    setSaveError(null);
  };

  const setDayType = (day, newType) => {
    const st = days[day];
    if (!st || st.type === newType) return;
    const hasContent =
      st.type === "per_muscle"
        ? st.muscles.length > 0
        : st.type === "manual"
        ? Object.values(st.checked).some(Boolean)
        : false;
    if (hasContent && !window.confirm(t("routine.confirmSwitchType", { day }))) return;
    setDays((prev) => ({
      ...prev,
      [day]: { type: newType, name: prev[day].name, muscles: [], counts: {}, checked: {} },
    }));
  };

  const toggleMuscle = (day, muscle) => {
    setDays((prev) => {
      const st = prev[day] || emptyDay();
      const has = st.muscles.includes(muscle);
      const muscles = has ? st.muscles.filter((m) => m !== muscle) : [...st.muscles, muscle];
      const counts = { ...st.counts };
      const checked = { ...st.checked };
      if (has) {
        delete counts[muscle];
        (exercisesByMuscle[muscle] || []).forEach((ex) => delete checked[ex.exercise_id]);
      } else {
        counts[muscle] = DEFAULT_COUNT;
        (exercisesByMuscle[muscle] || []).forEach((ex) => {
          checked[ex.exercise_id] = true;
        });
      }
      return { ...prev, [day]: { ...st, muscles, counts, checked } };
    });
  };

  const toggleExercise = (day, exId) => {
    setDays((prev) => {
      const st = prev[day];
      return { ...prev, [day]: { ...st, checked: { ...st.checked, [exId]: !st.checked[exId] } } };
    });
  };

  const stepCount = (day, muscle, dir) => {
    setDays((prev) => {
      const st = prev[day];
      const val = Math.max(1, Math.min(10, (st.counts[muscle] || DEFAULT_COUNT) + dir));
      return { ...prev, [day]: { ...st, counts: { ...st.counts, [muscle]: val } } };
    });
  };

  const setDayName = (day, name) => {
    setDays((prev) => ({ ...prev, [day]: { ...prev[day], name } }));
  };

  const toggleEditorPool = (day) => setEditorPools((p) => ({ ...p, [day]: !p[day] }));
  const setManualTab = (day, muscle) => setManualTabs((p) => ({ ...p, [day]: muscle }));

  const buildPayload = () => {
    const daysArr = [];
    for (let d = 1; d <= daysPerWeek; d++) {
      const st = days[d];
      if (st.type === "manual") {
        const exercise_ids = [];
        MUSCLE_GROUPS.forEach((m) => {
          (exercisesByMuscle[m] || []).forEach((ex) => {
            if (st.checked[ex.exercise_id]) exercise_ids.push(ex.exercise_id);
          });
        });
        daysArr.push({ day_number: d, day_type: "manual", name: st.name.trim() || null, muscles: [], exercise_ids });
        continue;
      }
      const muscles = MUSCLE_GROUPS.filter((m) => st.muscles.includes(m));
      const exercise_ids = [];
      muscles.forEach((m) => {
        (exercisesByMuscle[m] || []).forEach((ex) => {
          if (st.checked[ex.exercise_id]) exercise_ids.push(ex.exercise_id);
        });
      });
      daysArr.push({
        day_number: d,
        day_type: "per_muscle",
        name: st.name.trim() || null,
        muscles: muscles.map((m) => ({ muscle_group: m, exercise_count: st.counts[m] || DEFAULT_COUNT })),
        exercise_ids,
      });
    }
    return { days: daysArr };
  };

  const validate = () => {
    for (let d = 1; d <= daysPerWeek; d++) {
      const st = days[d];
      if (!st.type) return { day: d, msg: t("routine.errNoType", { day: d }) };
      if (st.type === "manual") {
        const anyChecked = MUSCLE_GROUPS.some((m) =>
          (exercisesByMuscle[m] || []).some((ex) => st.checked[ex.exercise_id])
        );
        if (!anyChecked) return { day: d, msg: t("routine.errNeedExerciseManual", { day: d }) };
        continue;
      }
      if (!st.muscles.length) return { day: d, msg: t("routine.errNeedMuscle", { day: d }) };
      for (const m of st.muscles) {
        const exs = exercisesByMuscle[m] || [];
        const muscleName = t(`muscles.${m}`);
        if (!exs.length) return { day: d, msg: t("routine.errNoLibraryExercises", { muscle: muscleName }) };
        if (!exs.some((ex) => st.checked[ex.exercise_id]))
          return { day: d, msg: t("routine.errNeedExercise", { muscle: muscleName, day: d }) };
      }
    }
    return null;
  };

  const handleSave = async () => {
    const err = validate();
    if (err) {
      setSaveError(err);
      return;
    }
    setSaveError(null);
    try {
      const res = await apiPost(`/routine/${userId}`, buildPayload());
      if (!res.ok) throw new Error("save failed");
      const rt = await apiGet(`/routine/${userId}`).then((r) => (r.ok ? r.json() : null));
      setRoutine(rt);
      setMode("view");
      showToast(t("routine.saved"), "success");
    } catch {
      showToast(t("routine.saveError"), "error");
    }
  };

  const handleCancel = () => {
    setSaveError(null);
    setMode("view");
  };

  if (loading) return <p style={{ color: "var(--text)" }}>{t("common.loading")}</p>;

  // ================= EDITOR (new / edit routine) =================
  if (mode === "new" || mode === "edit") {
    const isEditing = mode === "edit";
    const defaultTab = MUSCLE_GROUPS.find((m) => (exercisesByMuscle[m] || []).length) || MUSCLE_GROUPS[0];

    return (
      <div>
        <h1 className="rt-title">{isEditing ? t("routine.editRoutine") : t("routine.newTitle")}</h1>
        {!isEditing && <p className="rt-subline">{t("routine.daysPerWeekQ")}</p>}

        <div className={`rt-days-selector${isEditing ? " locked" : ""}`}>
          {[1, 2, 3, 4, 5, 6, 7].map((n) => (
            <button
              key={n}
              type="button"
              className={`rt-day-num${daysPerWeek === n ? " selected" : ""}`}
              onClick={isEditing ? undefined : () => selectDaysPerWeek(n)}
              disabled={isEditing}
            >
              {n}
            </button>
          ))}
        </div>

        {daysPerWeek > 0 && (
          <>
            {Array.from({ length: daysPerWeek }, (_, i) => i + 1).map((d) => {
              const st = days[d] || emptyDay();
              const hasErr = saveError && saveError.day === d;
              const selectedMuscles = MUSCLE_GROUPS.filter((m) => st.muscles.includes(m));
              const poolOpen = !!editorPools[d];
              const activeTab = manualTabs[d] || defaultTab;
              const manualTotal = MUSCLE_GROUPS.reduce(
                (sum, m) => sum + (exercisesByMuscle[m] || []).filter((ex) => st.checked[ex.exercise_id]).length,
                0
              );

              return (
                <div key={d} className="rt-edit-day">
                  <div className={`rt-edit-heading${hasErr ? " error" : ""}`}>
                    <span>{t("common.day", { day: d })}</span>
                    <input
                      className="rt-day-name"
                      type="text"
                      maxLength={50}
                      placeholder={t("routine.namePlaceholder")}
                      value={st.name}
                      onChange={(e) => setDayName(d, e.target.value)}
                    />
                    {isEditing && daysPerWeek > 1 && (
                      <button
                        type="button"
                        className="rt-day-del"
                        title={t("routine.deleteDay")}
                        onClick={() => deleteDay(d)}
                      >
                        🗑
                      </button>
                    )}
                  </div>

                  <div className="rt-daytype-group">
                    <button
                      type="button"
                      className={`rt-daytype-btn${st.type === "per_muscle" ? " selected" : ""}`}
                      onClick={() => setDayType(d, "per_muscle")}
                    >
                      {t("routine.byMuscle")}
                    </button>
                    <button
                      type="button"
                      className={`rt-daytype-btn${st.type === "manual" ? " selected" : ""}`}
                      onClick={() => setDayType(d, "manual")}
                    >
                      {t("routine.manual")}
                    </button>
                  </div>

                  {st.type === "per_muscle" && (
                    <>
                      <div className="rt-muscle-group">
                        {MUSCLE_GROUPS.map((m) => (
                          <button
                            key={m}
                            type="button"
                            className={`rt-muscle-btn${st.muscles.includes(m) ? " selected" : ""}`}
                            onClick={() => toggleMuscle(d, m)}
                          >
                            {t(`muscles.${m}`)}
                          </button>
                        ))}
                      </div>

                      <div className="rt-selected-tag">
                        {t("routine.selected")}:{" "}
                        <span>
                          {selectedMuscles.length
                            ? selectedMuscles.map((m) => t(`muscles.${m}`)).join(", ")
                            : t("routine.none")}
                        </span>
                      </div>

                      {selectedMuscles.length > 0 && (
                        <>
                          <div className="rt-expander" onClick={() => toggleEditorPool(d)}>
                            {poolOpen ? "▾" : "▸"} {t("routine.selectExercises")}
                          </div>
                          {poolOpen && (
                            <div className="rt-pool-edit">
                              {selectedMuscles.map((m) => {
                                const exs = exercisesByMuscle[m] || [];
                                const count = st.counts[m] || DEFAULT_COUNT;
                                return (
                                  <div key={m} className="rt-pool-col">
                                    <div className="rt-pool-muscle-row">
                                      <span className="rt-pool-muscle-name">{t(`muscles.${m}`)}</span>
                                      <span className="rt-stepper">
                                        <span className="rt-step-label">{t("routine.perSession")}</span>
                                        <button type="button" className="rt-step-btn" onClick={() => stepCount(d, m, -1)}>
                                          −
                                        </button>
                                        <span className="rt-step-val">{count}</span>
                                        <button type="button" className="rt-step-btn" onClick={() => stepCount(d, m, 1)}>
                                          +
                                        </button>
                                      </span>
                                    </div>
                                    {exs.length ? (
                                      exs.map((ex) => (
                                        <label key={ex.exercise_id} className="rt-pool-ex">
                                          <input
                                            type="checkbox"
                                            className="rt-checkbox"
                                            checked={!!st.checked[ex.exercise_id]}
                                            onChange={() => toggleExercise(d, ex.exercise_id)}
                                          />
                                          <span>{ex.exercise_name}</span>
                                        </label>
                                      ))
                                    ) : (
                                      <div className="rt-pool-empty">
                                        {t("routine.noMuscleExercises", { muscle: t(`muscles.${m}`) })}
                                      </div>
                                    )}
                                  </div>
                                );
                              })}
                            </div>
                          )}
                        </>
                      )}
                    </>
                  )}

                  {st.type === "manual" && (
                    <>
                      <div className="rt-manual-layout">
                        <div className="rt-manual-tabs">
                          {MUSCLE_GROUPS.map((m) => {
                            const n = (exercisesByMuscle[m] || []).filter((ex) => st.checked[ex.exercise_id]).length;
                            return (
                              <div
                                key={m}
                                className={`rt-manual-tab${m === activeTab ? " active" : ""}`}
                                onClick={() => setManualTab(d, m)}
                              >
                                {t(`muscles.${m}`)}
                                {n ? <span className="rt-manual-cnt"> {n}</span> : null}
                              </div>
                            );
                          })}
                        </div>
                        <div className="rt-manual-list">
                          {(exercisesByMuscle[activeTab] || []).length ? (
                            (exercisesByMuscle[activeTab] || []).map((ex) => (
                              <label key={ex.exercise_id} className="rt-pool-ex">
                                <input
                                  type="checkbox"
                                  className="rt-checkbox"
                                  checked={!!st.checked[ex.exercise_id]}
                                  onChange={() => toggleExercise(d, ex.exercise_id)}
                                />
                                <span>{ex.exercise_name}</span>
                              </label>
                            ))
                          ) : (
                            <div className="rt-pool-empty">
                              {t("routine.noMuscleExercises", { muscle: t(`muscles.${activeTab}`) })}
                            </div>
                          )}
                        </div>
                      </div>
                      <div className="rt-selected-tag">
                        {t("routine.selected")}: <span>{t("routine.exercises", { count: manualTotal })}</span>
                      </div>
                    </>
                  )}
                </div>
              );
            })}

            {isEditing && daysPerWeek < 7 && (
              <button type="button" className="rt-btn rt-btn--ghost rt-add-day" onClick={addDay}>
                + {t("routine.addDay")}
              </button>
            )}

            {saveError && <p className="rt-error">{saveError.msg}</p>}

            <div className="rt-save-row">
              <button className="rt-btn rt-btn--primary" onClick={handleSave}>
                ✓ {t("routine.save")}
              </button>
              <button className="rt-btn rt-btn--negative" onClick={handleCancel}>
                ✕ {t("routine.cancel")}
              </button>
            </div>
          </>
        )}
      </div>
    );
  }

  // ================= VIEW (current routine) =================
  const hasRoutine = routine && routine.days_per_week > 0;
  if (!hasRoutine) {
    return (
      <div>
        <h1 className="rt-title">{t("routine.title")}</h1>
        <p className="rt-empty">{t("routine.noRoutine")}</p>
        <button className="rt-btn" onClick={startNew}>
          + {t("routine.createRoutine")}
        </button>
      </div>
    );
  }

  const viewDays = routine.days || [];
  const isMixed = new Set(viewDays.map((d) => d.day_type)).size > 1;

  return (
    <div>
      <h1 className="rt-title">{t("routine.title")}</h1>
      <p className="rt-subline">
        <Trans
          i18nKey="routine.trainingDays"
          count={routine.days_per_week}
          values={{ count: routine.days_per_week }}
          components={{ n: <span style={{ color: "var(--red)", margin: "0 4px" }} /> }}
        />
      </p>

      <div className="rt-cols">
        {viewDays.map((day) => {
          const head = day.name
            ? `${t("common.day", { day: day.day_number })} — ${day.name}`
            : t("common.day", { day: day.day_number });
          const rows = exRows(day);
          const open = !!openPools[day.day_number];

          return (
            <div key={day.day_number} className="rt-col">
              <div className="rt-col-headrow">
                <span className="rt-day-head">{head}</span>
                {isMixed && (
                  <span className="rt-daytype">
                    {day.day_type === "manual" ? t("routine.dayTypeManual") : t("routine.dayTypePerMuscle")}
                  </span>
                )}
              </div>

              {day.day_type === "manual" ? (
                rows.length ? (
                  rows.map((r) => (
                    <div key={r.key} className="rt-ex-row">
                      <span className="rt-muscle">{r.muscle}</span>
                      <span className="rt-ex">{r.name}</span>
                    </div>
                  ))
                ) : (
                  <div className="rt-ex-row">
                    <span className="rt-ex muted">{t("routine.noExercises")}</span>
                  </div>
                )
              ) : (
                <>
                  {day.muscles && day.muscles.length ? (
                    day.muscles.map((m) => (
                      <div key={m.muscle_group} className="rt-ex-row">
                        <span className="rt-muscle">{t(`muscles.${m.muscle_group}`)}</span>
                        <span className="rt-ex">{t("routine.exercises", { count: m.exercise_count })}</span>
                      </div>
                    ))
                  ) : (
                    <div className="rt-ex-row">
                      <span className="rt-ex muted">{t("routine.noMuscles")}</span>
                    </div>
                  )}

                  <div
                    className="rt-seepool"
                    onClick={() => setOpenPools((p) => ({ ...p, [day.day_number]: !open }))}
                  >
                    {open ? "▾" : "▸"} {t("routine.seePool")}
                  </div>
                  {open && (
                    <div className="rt-pool">
                      {rows.map((r) => (
                        <div key={r.key} className="rt-ex-row">
                          <span className="rt-muscle">{r.muscle}</span>
                          <span className="rt-ex">{r.name}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </>
              )}
            </div>
          );
        })}
      </div>

      <div className="rt-save-row" style={{ marginTop: "var(--space-md)" }}>
        <button className="rt-btn rt-btn--primary" onClick={enterEdit}>
          ✎ {t("routine.editRoutine")}
        </button>
        <button className="rt-btn rt-btn--secondary" onClick={startNew}>
          + {t("routine.newRoutine")}
        </button>
      </div>
    </div>
  );
}