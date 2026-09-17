import { useTranslation } from "react-i18next";

/**
 * Reusable, i18n-aware labeled text input.
 * Props:
 *   labelKey       - translation key for the label (e.g. "login.emailLabel")
 *   placeholderKey - translation key for the placeholder (optional)
 *   type           - input type ("text" | "email" | "password"), default "text"
 *   value          - current value (owned by the parent)
 *   onChange       - called with the new value when the user types
 */
export function Field({ labelKey, placeholderKey, type = "text", value = "", onChange }) {
  const { t } = useTranslation();

  return (
    <label style={{ display: "flex", flexDirection: "column", gap: "var(--space-xs)", fontFamily: "var(--font-body)" }}>
      <span style={{ fontSize: 14, color: "var(--text)" }}>{t(labelKey)}</span>
      <input
        type={type}
        value={value}
        placeholder={placeholderKey ? t(placeholderKey) : ""}
        onChange={(e) => onChange?.(e.target.value)}
        style={{
          padding: "8px 4px",
          border: "none",
          borderBottom: "1px solid var(--border)",
          background: "transparent",
          color: "var(--text)",
          fontFamily: "var(--font-body)",
          fontSize: "var(--fs)",
          outline: "none",
        }}
      />
    </label>
  );
}