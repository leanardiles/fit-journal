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
    <label style={{ display: "flex", flexDirection: "column", gap: 4, fontFamily: "sans-serif" }}>
      <span style={{ fontSize: 13, color: "#555" }}>{t(labelKey)}</span>
      <input
        type={type}
        value={value}
        placeholder={placeholderKey ? t(placeholderKey) : ""}
        onChange={(e) => onChange?.(e.target.value)}
        style={{
          padding: "10px 12px",
          borderRadius: 8,
          border: "1.5px solid #ccc",
          fontSize: 15,
          fontFamily: "inherit",
        }}
      />
    </label>
  );
}