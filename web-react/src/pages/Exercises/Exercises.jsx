import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { apiGet, apiPost, apiDelete } from "../../api/client";
import { useUser } from "../../context/UserContext";
import { useToast } from "../../context/ToastContext";
import { MUSCLE_GROUPS } from "../../constants/muscles";
import { Field } from "../../components/Field/Field";
import { Button } from "../../components/Button/Button";
import { Select } from "../../components/Select/Select";
import { Modal } from "../../components/Modal/Modal";
import { useSearchParams } from "react-router-dom";
import "./Exercises.css";

const KG_PER_LB = 0.45359237;

export function Exercises() {
  const { t } = useTranslation();
  const { user } = useUser();
  const { showToast } = useToast();
  const isImperial = user?.user_unit_preference === "imperial";
  const weightUnit = isImperial ? "lb" : "kg";

  const [exercises, setExercises] = useState([]);
  const [loading, setLoading] = useState(true);

    // Selected muscle lives in the URL (?muscle=Chest) so it survives refresh,
  // is bookmarkable/shareable, and works with browser back/forward.
  const [searchParams, setSearchParams] = useSearchParams();
  const muscleParam = searchParams.get("muscle");
  const selectedMuscle = MUSCLE_GROUPS.includes(muscleParam) ? muscleParam : "Legs";
  const selectMuscle = (m) => setSearchParams({ muscle: m });

  // Add-exercise modal
  const [showAddModal, setShowAddModal] = useState(false);
  const [newName, setNewName] = useState("");
  const [newMuscle, setNewMuscle] = useState("Legs");
  const [newWeight, setNewWeight] = useState("");
  const [newLink, setNewLink] = useState("");
  const [newNotes, setNewNotes] = useState("");
  const [addError, setAddError] = useState("");
  const [adding, setAdding] = useState(false);

  // Delete confirmation (which row is awaiting confirm)
  const [confirmingDeleteId, setConfirmingDeleteId] = useState(null);

  useEffect(() => {
    const userId = localStorage.getItem("user_id");
    apiGet(`/exercises?user_id=${userId}`)
      .then((res) => (res.ok ? res.json() : []))
      .then((data) => setExercises(Array.isArray(data) ? data : []))
      .finally(() => setLoading(false));
  }, []);

  const forMuscle = exercises.filter(
    (ex) => ex.exercise_muscle_group === selectedMuscle
  );

  const displayWeight = (kg) => {
    if (kg == null || kg === "" || Number(kg) === 0) return "—";
    const value = isImperial ? Math.round(Number(kg) / KG_PER_LB) : Number(kg);
    return `${value} ${weightUnit}`;
  };

  const openAdd = () => {
    setNewName("");
    setNewMuscle(selectedMuscle);
    setNewWeight("");
    setNewLink("");
    setNewNotes("");
    setAddError("");
    setShowAddModal(true);
  };

  const handleAdd = async () => {
    setAddError("");
    const name = newName.trim();
    if (!name) {
      setAddError(t("exercises.errorNameRequired"));
      return;
    }
    const dup = exercises.some(
      (ex) => ex.exercise_name.trim().toLowerCase() === name.toLowerCase()
    );
    if (dup) {
      setAddError(t("exercises.errorDuplicate"));
      return;
    }

    const weightKg =
      newWeight === ""
        ? null
        : isImperial
        ? Math.round(Number(newWeight) * KG_PER_LB)
        : Number(newWeight);

    setAdding(true);
    try {
      const userId = localStorage.getItem("user_id");
      const res = await apiPost(`/exercises?user_id=${userId}`, {
        exercise_name: name,
        exercise_muscle_group: newMuscle,
        exercise_user_current_weight: weightKg,
        exercise_link: newLink.trim() || null,
        comments: newNotes.trim() || null,
      });
      if (!res.ok) {
        setAddError(t("exercises.errorAddFailed"));
        return;
      }
      const created = await res.json();
      setExercises((prev) => [...prev, created]);
      selectMuscle(newMuscle);
      setShowAddModal(false);
      showToast(t("exercises.added"), "success");
    } catch {
      setAddError(t("exercises.errorAddFailed"));
    } finally {
      setAdding(false);
    }
  };

  const confirmDelete = async (id) => {
    try {
      const userId = localStorage.getItem("user_id");
      const res = await apiDelete(`/exercises/${id}?user_id=${userId}`);
      if (!res.ok) {
        showToast(t("exercises.deleteError"), "error");
        return;
      }
      setExercises((prev) => prev.filter((ex) => ex.exercise_id !== id));
      showToast(t("exercises.deleted"), "success");
    } catch {
      showToast(t("exercises.deleteError"), "error");
    } finally {
      setConfirmingDeleteId(null);
    }
  };

  const rowStyle = { display: "flex", flexDirection: "column", gap: "var(--space-xs)", fontFamily: "var(--font-body)" };
  const labelStyle = { fontSize: 14, color: "var(--text)" };
  const inputStyle = {
    padding: "8px 4px", border: "none", borderBottom: "1px solid var(--border)",
    background: "transparent", color: "var(--text)", fontFamily: "var(--font-body)",
    fontSize: "var(--fs)", outline: "none",
  };

  return (
    <div>
      <div className="ex-header">
        <h1 className="ex-title">{t("exercises.title")}</h1>
        <Button labelKey="exercises.addButton" variant="secondary" onClick={openAdd} />
      </div>

      <div className="ex-layout">
        <nav className="ex-muscle-col">
          {MUSCLE_GROUPS.map((m) => (
            <button
              key={m}
              type="button"
              className={`ex-muscle-tab${m === selectedMuscle ? " active" : ""}`}
              onClick={() => selectMuscle(m)}
            >
              {t(`muscles.${m}`)}
            </button>
          ))}
        </nav>

        <div className="ex-exercise-col">
          {loading ? (
            <p className="ex-muted">{t("common.loading")}</p>
          ) : forMuscle.length === 0 ? (
            <p className="ex-muted">{t("exercises.empty")}</p>
          ) : (
            <table className="ex-table">
              <thead>
                <tr>
                  <th className="ex-th-name">{t("exercises.colExercise")}</th>
                  <th className="ex-th-center">
                    {t("exercises.colWeight")} ({weightUnit})
                  </th>
                  <th className="ex-th-center">{t("exercises.colLink")}</th>
                  <th className="ex-th-center">{t("exercises.colDelete")}</th>
                </tr>
              </thead>
              <tbody>
                {forMuscle.map((ex) => (
                  <tr key={ex.exercise_id}>
                    <td>{ex.exercise_name}</td>
                    <td className="ex-td-center">
                      {displayWeight(ex.exercise_user_current_weight)}
                    </td>
                    <td className="ex-td-center">
                      {ex.exercise_link ? (
                        <a
                          href={ex.exercise_link}
                          target="_blank"
                          rel="noreferrer"
                          className="ex-link"
                        >
                          {t("exercises.viewLink")}
                        </a>
                      ) : (
                        <span className="ex-muted">—</span>
                      )}
                    </td>
                    <td className="ex-td-center">
                      {confirmingDeleteId === ex.exercise_id ? (
                        <span className="ex-confirm">
                          <button className="ex-confirm-yes" onClick={() => confirmDelete(ex.exercise_id)}>
                            {t("exercises.confirmDelete")}
                          </button>
                          <button className="ex-confirm-no" onClick={() => setConfirmingDeleteId(null)}>
                            {t("exercises.cancelDelete")}
                          </button>
                        </span>
                      ) : (
                        <button
                          className="ex-delete-btn"
                          onClick={() => setConfirmingDeleteId(ex.exercise_id)}
                          aria-label={t("exercises.deleteAria")}
                          title={t("exercises.deleteAria")}
                        >
                          🗑
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      <Modal open={showAddModal} onClose={() => setShowAddModal(false)} titleKey="exercises.newTitle">
        <div style={{ display: "flex", flexDirection: "column", gap: "var(--space-md)" }}>
          <Field labelKey="exercises.fieldName" placeholderKey="exercises.namePlaceholder" value={newName} onChange={setNewName} />

          <Select
            labelKey="exercises.fieldMuscle"
            value={newMuscle}
            onChange={setNewMuscle}
            options={MUSCLE_GROUPS.map((m) => ({ value: m, label: t(`muscles.${m}`) }))}
          />

          <label style={rowStyle}>
            <span style={labelStyle}>{t("exercises.fieldWeight")} ({weightUnit})</span>
            <input style={inputStyle} type="number" value={newWeight}
              onChange={(e) => setNewWeight(e.target.value)} placeholder="0" />
          </label>

          <Field labelKey="exercises.fieldLink" placeholderKey="exercises.linkPlaceholder" value={newLink} onChange={setNewLink} />
          <Field labelKey="exercises.fieldNotes" placeholderKey="exercises.notesPlaceholder" value={newNotes} onChange={setNewNotes} />

          {addError && <p style={{ color: "var(--error)", margin: 0, fontFamily: "var(--font-body)" }}>{addError}</p>}

          <div style={{ display: "flex", gap: 12 }}>
            <Button labelKey="exercises.save" variant="primary" onClick={handleAdd} />
            <Button labelKey="exercises.cancel" variant="secondary" onClick={() => setShowAddModal(false)} />
          </div>
        </div>
      </Modal>
    </div>
  );
}