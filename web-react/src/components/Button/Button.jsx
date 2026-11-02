import { useTranslation } from "react-i18next";

/**
 * Reusable, i18n-aware button.
 *
 * This used to carry its own inline styles, which meant styles/buttons.css
 * could not reach it: inline styles outrank stylesheets, so these buttons
 * stayed at their own size and radius while every other button on the page
 * moved. It now renders the shared classes and nothing else, so the family
 * is defined in exactly one place.
 *
 * Props:
 *   labelKey - translation key for the button text (e.g. "login.button")
 *   onClick  - click handler
 *   variant  - "primary" | "secondary" | "negative" | "neutral", matching the
 *              modifiers in styles/buttons.css
 *   type     - defaults to "button" so a button inside a form does not submit
 *              it by accident
 */
export function Button({ labelKey, onClick, variant = "primary", disabled = false, type = "button" }) {
  const { t } = useTranslation();
  const className = variant === "neutral" ? "btn" : `btn btn--${variant}`;

  return (
    <button type={type} className={className} onClick={onClick} disabled={disabled}>
      {t(labelKey)}
    </button>
  );
}
