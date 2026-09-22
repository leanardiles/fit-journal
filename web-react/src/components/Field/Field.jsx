import { useTranslation } from "react-i18next";
import "./Field.css";

/**
 * Reusable, i18n-aware labeled text input.
 * Props:
 *   labelKey       - translation key for the label
 *   placeholderKey - translation key for the placeholder (optional)
 *   type           - input type ("text" | "email" | "password"), default "text"
 *   value          - current value (owned by the parent)
 *   onChange       - called with the new value when the user types
 *   inline         - label beside the input on one row (collapses to stacked on
 *                    narrow screens). Default false = stacked column.
 */
export function Field({ labelKey, placeholderKey, type = "text", value = "", onChange, inline = false }) {
  const { t } = useTranslation();

  return (
    <label className={`field${inline ? " field--inline" : ""}`}>
      <span className="field-label">{t(labelKey)}</span>
      <input
        className="field-input"
        type={type}
        value={value}
        placeholder={placeholderKey ? t(placeholderKey) : ""}
        onChange={(e) => onChange?.(e.target.value)}
      />
    </label>
  );
}