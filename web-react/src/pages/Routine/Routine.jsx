import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { apiGet } from "../../api/client";
import { MUSCLE_GROUPS } from "../../constants/muscles";
import "./Routine.css";

export function Routine() {
  const { t } = useTranslation();
  const userId = localStorage.getItem("user_id");

  const [routine, setRoutine] = useState(null);
  const [loading, setLoading] = useState(true);
  const [openPools, setOpenPools] = useState({}); // { [day_number]: bool }

  useEffect(() => {
    let cancelled = false;
    apiGet(`/routine/${userId}`)
      .then((r) => (r.ok ? r.json() : null))
      .then((rt) => {
        if (!cancelled) setRoutine(rt);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // Exercise names for a day, grouped by muscle in MUSCLE_GROUPS order. The
  // muscle label shows only on the first row of each group (a spanned look).
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

  if (loading) return <p style={{ color: "var(--text)" }}>{t("common.loading")}</p>;

  const hasRoutine = routine && routine.days_per_week > 0;
  if (!hasRoutine) {
    return (
      <div>
        <h1 className="rt-title">{t("routine.title")}</h1>
        <p className="rt-empty">{t("routine.noRoutine")}</p>
      </div>
    );
  }

  const days = routine.days || [];
  const isMixed = new Set(days.map((d) => d.day_type)).size > 1;

  return (
    <div>
      <h1 className="rt-title">{t("routine.title")}</h1>
      <p className="rt-subline">{t("routine.trainingDays", { count: routine.days_per_week })}</p>

      <div className="rt-cols">
        {days.map((day) => {
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
    </div>
  );
}