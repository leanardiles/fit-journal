import { Modal } from "../Modal/Modal";
import { Button } from "../Button/Button";
import "./ConfirmModal.css";

/**
 * A themed yes/no question. Use it for a destructive action whose trigger
 * cannot hold the answer, or whose consequence needs a sentence to explain.
 *
 * For a row-level delete where the consequence is obvious, swap the trigger
 * for the answer in place instead (see .ex-confirm in Exercises) rather than
 * covering the page to ask.
 *
 * Props:
 *   open         - whether the question is showing
 *   titleKey     - translation key for the heading (optional)
 *   message      - the question, already translated
 *   confirmKey   - translation key for the affirmative button
 *   cancelKey    - translation key for the dismissive button
 *   onConfirm    - called when the user agrees
 *   onCancel     - called on dismiss, Escape, or a backdrop click
 *   destructive  - true colours the affirmative as a loss (the default)
 */
export function ConfirmModal({
  open,
  titleKey,
  message,
  confirmKey,
  cancelKey = "common.cancel",
  onConfirm,
  onCancel,
  destructive = true,
}) {
  return (
    <Modal open={open} onClose={onCancel} titleKey={titleKey}>
      <p className="confirm-message">{message}</p>
      <div className="confirm-actions">
        <Button
          labelKey={confirmKey}
          variant={destructive ? "negative" : "primary"}
          onClick={onConfirm}
        />
        <Button labelKey={cancelKey} variant="neutral" onClick={onCancel} />
      </div>
    </Modal>
  );
}
