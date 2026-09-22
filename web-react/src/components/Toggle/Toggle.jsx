import { useTranslation } from "react-i18next";
import "./Toggle.css";

/**
 * Reusable, i18n-aware single-select toggle (segmented chips).
 * Sibling to Field / Select: the field label is i18n-aware (labelKey), while
 * option labels are display-ready strings the caller passes.
 * Props:
 *   labelKey - translation key for the field label (optional)
 *   value    - selected value (owned by parent)
 *   onChange - called with the new value
 *   options  - [{ value, label }], label already display-ready
 *   vertical - stack the chips in a column instead of a row (default false)
 */
export function Toggle({ labelKey, value, onChange, options = [], vertical = false }) {
  const { t } = useTranslation();
  return (
    <div className="toggle-field">
      {labelKey && <span className="toggle-label">{t(labelKey)}</span>}
      <div className={`toggle-group${vertical ? " toggle-group--vertical" : ""}`} role="group">
        {options.map((opt) => {
          const selected = opt.value === value;
          return (
            <button
              key={opt.value}
              type="button"
              className={`toggle-chip${selected ? " selected" : ""}`}
              aria-pressed={selected}
              onClick={() => onChange?.(opt.value)}
            >
              {opt.label}
            </button>
          );
        })}
      </div>
    </div>
  );
}