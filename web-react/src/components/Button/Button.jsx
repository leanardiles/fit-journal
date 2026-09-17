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
    primary:   { background: "#FFEB3B", color: "#000", border: "none" },
    secondary: { background: "transparent", color: "#FFEB3B", border: "1.5px solid #FFEB3B" },
  };

  return (
    <button
      onClick={onClick}
      style={{
        ...styles[variant],
        padding: "10px 20px",
        borderRadius: 8,
        fontSize: 15,
        cursor: "pointer",
        fontFamily: "sans-serif",
      }}
    >
      {t(labelKey)}
    </button>
  );
}