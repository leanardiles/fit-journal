import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Modal } from "../Modal/Modal";
import { Button } from "../Button/Button";
import "./DashboardNotesModal.css";

/**
 * Pick which dashboard notes show, and in what order.
 *
 * Reordering is move-up / move-down rather than drag. Two reasons, and
 * neither is that dragging is hard: the buttons are operable by keyboard and
 * by screen reader with no extra work, and on a phone, where the notes stack
 * and this matters most, tapping an arrow beats long-pressing and dragging
 * inside a scrolling page. A drag handle can be added to these same rows
 * later without touching the stored shape, which is why the order lives in an
 * array rather than in the markup.
 *
 * Props:
 *   open     - whether the dialog is showing
 *   notes    - the full catalogue: [{ id, nameKey, descKey }], in default order
 *   order    - current ids, most important first
 *   hidden   - ids currently switched off
 *   onCancel - dismiss without saving
 *   onSave   - called with (order, hidden)
 */
export function DashboardNotesModal({ open, notes, order, hidden, onCancel, onSave }) {
  const { t } = useTranslation();

  // The dialog edits a copy, so Cancel is a real cancel rather than an undo
  // the user has to perform themselves. Reseeded each time it opens, or a
  // second visit would start from the last abandoned edit.
  const [draftOrder, setDraftOrder] = useState(order);
  const [draftHidden, setDraftHidden] = useState(hidden);

  useEffect(() => {
    if (!open) return;
    setDraftOrder(order);
    setDraftHidden(hidden);
  }, [open, order, hidden]);

  const move = (index, delta) => {
    const target = index + delta;
    if (target < 0 || target >= draftOrder.length) return;
    const next = [...draftOrder];
    [next[index], next[target]] = [next[target], next[index]];
    setDraftOrder(next);
  };

  const toggle = (id) => {
    setDraftHidden((prev) => (prev.includes(id) ? prev.filter((h) => h !== id) : [...prev, id]));
  };

  const byId = (id) => notes.find((n) => n.id === id);

  return (
    <Modal open={open} onClose={onCancel} titleKey="dashboard.notesTitle">
      <p className="dnm-help">{t("dashboard.notesHelp")}</p>

      <ul className="dnm-list">
        {draftOrder.map((id, index) => {
          const note = byId(id);
          if (!note) return null;
          const isHidden = draftHidden.includes(id);
          const name = t(note.nameKey);
          return (
            <li key={id} className={`dnm-row${isHidden ? " is-hidden" : ""}`}>
              <label className="dnm-name" htmlFor={`dnm-${id}`}>{name}</label>

              <div className="dnm-moves">
                <button
                  type="button"
                  className="dnm-move"
                  aria-label={t("dashboard.notesMoveUp", { name })}
                  disabled={index === 0}
                  onClick={() => move(index, -1)}
                >
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                       strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <path d="M18 15l-6-6-6 6" />
                  </svg>
                </button>
                <button
                  type="button"
                  className="dnm-move"
                  aria-label={t("dashboard.notesMoveDown", { name })}
                  disabled={index === draftOrder.length - 1}
                  onClick={() => move(index, 1)}
                >
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                       strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <path d="M6 9l6 6 6-6" />
                  </svg>
                </button>
              </div>

              <input
                id={`dnm-${id}`}
                className="dnm-check"
                type="checkbox"
                checked={!isHidden}
                onChange={() => toggle(id)}
              />

              <p className="dnm-desc">{t(note.descKey)}</p>
            </li>
          );
        })}
      </ul>

      <div className="dnm-actions">
        <Button labelKey="common.cancel" variant="neutral" onClick={onCancel} />
        <Button
          labelKey="dashboard.notesSave"
          variant="primary"
          onClick={() => onSave(draftOrder, draftHidden)}
        />
      </div>
    </Modal>
  );
}
