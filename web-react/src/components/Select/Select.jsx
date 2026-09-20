import { useTranslation } from "react-i18next";

/**
 * Reusable, i18n-aware labeled select.
 * Props:
 *   labelKey - translation key for the field label (optional)
 *   value    - selected value (owned by parent)
 *   onChange - called with the new value
 *   options  - either a flat list [{ value, label }] or grouped
 *              [{ label, options: [{ value, label }] }] (renders <optgroup>).
 *              Labels are display-ready (caller translates).
 */
export function Select({ labelKey, value, onChange, options = [] }) {
  const { t } = useTranslation();

  const renderOption = (opt) => (
    <option key={opt.value} value={opt.value} style={{ background: "var(--bg)", color: "var(--text)" }}>
      {opt.label}
    </option>
  );

  return (
    <label style={{ display: "flex", flexDirection: "column", gap: "var(--space-xs)", fontFamily: "var(--font-body)" }}>
      {labelKey && <span style={{ fontSize: 14, color: "var(--text)" }}>{t(labelKey)}</span>}
      <select
        value={value}
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
      >
        {options.map((opt) =>
          opt.options ? (
            <optgroup key={opt.label} label={opt.label}>
              {opt.options.map(renderOption)}
            </optgroup>
          ) : (
            renderOption(opt)
          )
        )}
      </select>
    </label>
  );
}