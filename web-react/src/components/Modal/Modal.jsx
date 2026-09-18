import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import "./Modal.css";

/**
 * Reusable modal overlay. Renders nothing when `open` is false.
 * Closes on Escape and on clicking the backdrop (not the content).
 */
export function Modal({ open, onClose, titleKey, children }) {
  const { t } = useTranslation();

  useEffect(() => {
    if (!open) return;
    const onKey = (e) => { if (e.key === "Escape") onClose?.(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-box" onClick={(e) => e.stopPropagation()}>
        {titleKey && <h2 className="modal-title">{t(titleKey)}</h2>}
        {children}
      </div>
    </div>
  );
}