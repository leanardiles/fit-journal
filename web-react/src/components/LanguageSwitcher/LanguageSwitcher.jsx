import { useTranslation } from "react-i18next";
import { apiPut } from "../../api/client";
import { useToast } from "../../context/ToastContext";
import { LOCALE_OPTIONS } from "../../i18n/locales";

/**
 * Global language switcher for the app header.
 * Switches the UI language instantly, then persists the choice to the
 * user's profile (user_locale) in the background so it follows them across
 * devices and drives backend error localization. The UI never waits on
 * the network; a failed persist just shows a toast.
 */
export function LanguageSwitcher() {
  const { i18n, t } = useTranslation();
  const { showToast } = useToast();

  // Active language, guarded so an unexpected tag (e.g. "en-US" from the
  // browser detector) still selects a sensible option instead of blank.
  const current = LOCALE_OPTIONS.some((o) => o.value === i18n.language)
    ? i18n.language
    : i18n.resolvedLanguage || "en";

  const handleChange = async (value) => {
    i18n.changeLanguage(value); // instant UI switch (also caches to localStorage)

    const userId = localStorage.getItem("user_id");
    if (!userId) return; // not logged in -> session-only, nothing to persist

    try {
      const res = await apiPut(`/profile/${userId}`, { user_locale: value });
      if (!res.ok) showToast(t("profile.saveError"), "error");
    } catch {
      showToast(t("profile.saveError"), "error"); // UI already switched
    }
  };

  return (
    <select
      aria-label={t("nav.language")}
      className="app-lang-select"
      value={current}
      onChange={(e) => handleChange(e.target.value)}
    >
      {LOCALE_OPTIONS.map((opt) => (
        <option key={opt.value} value={opt.value}>
          {opt.label}
        </option>
      ))}
    </select>
  );
}