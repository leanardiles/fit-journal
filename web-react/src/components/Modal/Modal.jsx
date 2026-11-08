import { useEffect, useId, useRef } from "react";
import { useTranslation } from "react-i18next";
import "./Modal.css";

/**
 * Reusable modal overlay. Renders nothing when `open` is false.
 * Closes on Escape and on clicking the backdrop (not the content).
 *
 * The dialog semantics and the focus handling are not decoration: this
 * replaced window.confirm in places, and a native dialog traps focus for
 * free. A plain div does not, so without the trap below a keyboard user
 * could tab straight out of a question into the page behind it, which is
 * worse than the browser dialog it replaced.
 */
export function Modal({ open, onClose, titleKey, children }) {
  const { t } = useTranslation();
  const boxRef = useRef(null);
  const returnFocusRef = useRef(null);
  const titleId = useId();

  useEffect(() => {
    if (!open) return;

    // Remember what had focus so it can be handed back on close.
    returnFocusRef.current = document.activeElement;

    const focusables = () =>
      Array.from(
        boxRef.current?.querySelectorAll(
          'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
        ) || []
      );

    // Move focus in: the first field if there is one, otherwise the box.
    const first = focusables()[0];
    (first || boxRef.current)?.focus();

    const onKey = (e) => {
      if (e.key === "Escape") { onClose?.(); return; }
      if (e.key !== "Tab") return;
      const items = focusables();
      if (items.length === 0) { e.preventDefault(); return; }
      const firstItem = items[0];
      const lastItem = items[items.length - 1];
      // Wrap at both ends so focus cannot leave the dialog.
      if (e.shiftKey && document.activeElement === firstItem) {
        e.preventDefault();
        lastItem.focus();
      } else if (!e.shiftKey && document.activeElement === lastItem) {
        e.preventDefault();
        firstItem.focus();
      }
    };

    window.addEventListener("keydown", onKey);
    return () => {
      window.removeEventListener("keydown", onKey);
      // Hand focus back to whatever opened the dialog.
      if (returnFocusRef.current instanceof HTMLElement) returnFocusRef.current.focus();
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-box"
        ref={boxRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleKey ? titleId : undefined}
        tabIndex={-1}
        onClick={(e) => e.stopPropagation()}
      >
        {titleKey && <h2 className="modal-title" id={titleId}>{t(titleKey)}</h2>}
        {children}
      </div>
    </div>
  );
}
