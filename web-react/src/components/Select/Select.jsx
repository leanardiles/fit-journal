import { useTranslation } from "react-i18next";
import "../Field/Field.css"; // shares the .field layout and the .field-select identity

/**
 * Reusable, i18n-aware labeled select.
 * Props:
 *   labelKey - translation key for the field label (optional)
 *   value    - selected value (owned by parent)
 *   onChange - called with the new value
 *   options  - either a flat list [{ value, label }] or grouped
 *              [{ label, options: [{ value, label }] }] (renders <optgroup>).
 *              Labels are display-ready (caller translates).
 *   inline   - label beside the select on one row (matches Field's inline
 *              mode). Default false = label stacked above.
 *
 * Both modes render the same markup and differ only by one class, so the
 * dropdown styling lives in Field.css and is defined once.
 */
export function Select({ labelKey, value, onChange, options = [], inline = false }) {
  const { t } = useTranslation();

  const renderOption = (opt) => (
    <option key={opt.value} value={opt.value}>
      {opt.label}
    </option>
  );

  const optionsMarkup = options.map((opt) =>
    opt.options ? (
      <optgroup key={opt.label} label={opt.label}>
        {opt.options.map(renderOption)}
      </optgroup>
    ) : (
      renderOption(opt)
    )
  );

  return (
    <label className={`field${inline ? " field--inline" : ""}`}>
      {labelKey && <span className="field-label">{t(labelKey)}</span>}
      <select
        className="field-input field-select"
        value={value}
        onChange={(e) => onChange?.(e.target.value)}
      >
        {optionsMarkup}
      </select>
    </label>
  );
}
