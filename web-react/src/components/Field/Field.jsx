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
 *   optional       - append a muted "(optional)" hint after the label.
 */
export function Field({ labelKey, placeholderKey, type = "text", value = "", onChange, inline = false, optional = false }) {
  const { t } = useTranslation();

  return (
    <label className={`field${inline ? " field--inline" : ""}`}>
      <span className="field-label">
        {t(labelKey)}
        {optional && <span className="field-optional">{t("common.optional")}</span>}
      </span>
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