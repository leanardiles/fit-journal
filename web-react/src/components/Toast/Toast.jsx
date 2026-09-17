import "./Toast.css";

/**
 * Presentational toast. No i18n , it renders whatever translated `message`
 * it is handed. `type` is "success" | "error".
 */
export function Toast({ message, type = "success", onDismiss }) {
  return (
    <div className={`toast toast-${type}`} role="status" onClick={onDismiss}>
      {message}
    </div>
  );
}