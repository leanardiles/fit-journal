import { useTranslation } from "react-i18next";

/**
 * Reusable, i18n-aware button.
 * Props:
 *   labelKey - translation key for the button text (e.g. "login.button")
 *   onClick  - click handler
 *   variant  - "primary" | "secondary" (visual style)
 */
export function Button({ labelKey, onClick, variant = "primary" }) {
  const { t } = useTranslation();

  const styles = {
    primary:   { background: "var(--red)", color: "var(--text)", border: "none" },
    secondary: { background: "transparent", color: "var(--text)", border: "1.5px solid var(--red-border)" },
  };

  return (
    <button
      onClick={onClick}
      style={{
        ...styles[variant],
        padding: "8px 20px",
        borderRadius: "var(--radius)",
        fontSize: "var(--fs)",
        fontFamily: "var(--font-body)",
        cursor: "pointer",
      }}
    >
      {t(labelKey)}
    </button>
  );
}