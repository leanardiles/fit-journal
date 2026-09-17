import { createContext, useContext, useState, useCallback, useRef } from "react";
import { Toast } from "../components/Toast/Toast";

/**
 * App-wide toast/snackbar feedback.
 *
 * Any component calls:   const { showToast } = useToast();
 *                        showToast(t("some.key"), "success" | "error");
 *
 * The toast itself holds NO text , the CALLER passes an already-translated
 * string, so the toast component stays language-agnostic (like a Card, a
 * mechanism, not content). Messages live in the i18n catalogs.
 */

const ToastContext = createContext(null);

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null);   // { message, type } | null
  const timerRef = useRef(null);

  const showToast = useCallback((message, type = "success", duration = 3000) => {
    // Clear any pending auto-dismiss so rapid toasts don't cut each other short.
    if (timerRef.current) clearTimeout(timerRef.current);
    setToast({ message, type });
    timerRef.current = setTimeout(() => setToast(null), duration);
  }, []);

  const dismiss = useCallback(() => {
    if (timerRef.current) clearTimeout(timerRef.current);
    setToast(null);
  }, []);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      {toast && <Toast message={toast.message} type={toast.type} onDismiss={dismiss} />}
    </ToastContext.Provider>
  );
}

export function useToast() {
  return useContext(ToastContext);
}