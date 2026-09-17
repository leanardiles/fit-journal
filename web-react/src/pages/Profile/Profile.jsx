import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import i18n from "../../i18n/config";
import { apiGet, apiPut } from "../../api/client";
import { useToast } from "../../context/ToastContext";
import { useUser } from "../../context/UserContext";
import { Field } from "../../components/Field/Field";
import { Button } from "../../components/Button/Button";

// Supported locales for the picker: value = BCP 47 tag, label = friendly name.
const LOCALE_OPTIONS = [
  { value: "en",    label: "English" },
  { value: "es",    label: "Español (Latinoamérica)" },
  { value: "es-AR", label: "Español (Argentina)" },
  { value: "nl",    label: "Nederlands" },
];

export function Profile() {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const { user, setUser } = useUser();

  // Form state , initialized empty, filled once the profile loads.
  const [form, setForm] = useState({
    user_first_name: "",
    user_age: "",
    user_sex: "",
    user_unit_preference: "metric",
    user_timezone: "",
    user_locale: "",
  });
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // Load the profile once.
  useEffect(() => {
    const userId = localStorage.getItem("user_id");
    apiGet(`/profile/${userId}`)
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (data) {
          setEmail(data.user_email || "");
          setForm({
            user_first_name: data.user_first_name || "",
            user_age: data.user_age ?? "",
            user_sex: data.user_sex || "",
            user_unit_preference: data.user_unit_preference || "metric",
            user_timezone: data.user_timezone || "",
            user_locale: data.user_locale || i18n.language || "en",
          });
        }
      })
      .finally(() => setLoading(false));
  }, []);

  // Generic field updater.
  const update = (key, value) => setForm((prev) => ({ ...prev, [key]: value }));

  // Locale change = live app-language switch + form update.
  const handleLocaleChange = (value) => {
    update("user_locale", value);
    i18n.changeLanguage(value);   // switch the whole app's language immediately
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const userId = localStorage.getItem("user_id");
      const payload = {
        ...form,
        user_age: form.user_age === "" ? null : Number(form.user_age),
      };
      const res = await apiPut(`/profile/${userId}`, payload);
      if (!res.ok) {
        let detail = t("profile.saveError");
        try { const d = await res.json(); detail = d.detail || detail; } catch { /* no body */ }
        showToast(detail, "error");
        return;
      }
      const updated = await res.json();
      if (setUser) setUser(updated);        // refresh the shared user (header name, etc.)
      showToast(t("profile.saved"), "success");
    } catch (e) {
      showToast(t("profile.saveError"), "error");
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <p>{t("common.loading")}</p>;

  const selectStyle = {
    padding: "8px 4px",
    border: "none",
    borderBottom: "1px solid var(--border)",
    background: "transparent",
    color: "var(--text)",
    fontFamily: "var(--font-body)",
    fontSize: "var(--fs)",
    outline: "none",
  };
  const labelStyle = { fontSize: 14, color: "var(--muted)", fontFamily: "var(--font-body)" };
  const rowStyle = { display: "flex", flexDirection: "column", gap: "var(--space-xs)" };

  return (
    <div style={{ maxWidth: 420, display: "flex", flexDirection: "column", gap: "var(--space-md)" }}>
      <h1 style={{ fontFamily: "var(--font-body)", color: "var(--text)" }}>{t("profile.title")}</h1>

      {/* Email (read-only) */}
      <label style={rowStyle}>
        <span style={labelStyle}>{t("profile.email")}</span>
        <span style={{ color: "var(--text)" }}>{email}</span>
      </label>

      {/* Name */}
      <Field
        labelKey="profile.name"
        value={form.user_first_name}
        onChange={(v) => update("user_first_name", v)}
      />

      {/* Age */}
      <Field
        labelKey="profile.age"
        type="number"
        value={String(form.user_age)}
        onChange={(v) => update("user_age", v)}
      />

      {/* Sex */}
      <label style={rowStyle}>
        <span style={labelStyle}>{t("profile.sex")}</span>
        <select style={selectStyle} value={form.user_sex} onChange={(e) => update("user_sex", e.target.value)}>
          <option value="">--</option>
          <option value="M">{t("profile.sexMale")}</option>
          <option value="W">{t("profile.sexFemale")}</option>
          <option value="NB">{t("profile.sexNonBinary")}</option>
        </select>
      </label>

      {/* Unit preference */}
      <label style={rowStyle}>
        <span style={labelStyle}>{t("profile.units")}</span>
        <select style={selectStyle} value={form.user_unit_preference} onChange={(e) => update("user_unit_preference", e.target.value)}>
          <option value="metric">{t("profile.metric")}</option>
          <option value="imperial">{t("profile.imperial")}</option>
        </select>
      </label>

      {/* Timezone (simple text for now; a full tz list can come later) */}
      <Field
        labelKey="profile.timezone"
        value={form.user_timezone}
        onChange={(v) => update("user_timezone", v)}
      />

      {/* Locale / language picker (live-switches the app language) */}
      <label style={rowStyle}>
        <span style={labelStyle}>{t("profile.language")}</span>
        <select style={selectStyle} value={form.user_locale} onChange={(e) => handleLocaleChange(e.target.value)}>
          {LOCALE_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>{opt.label}</option>
          ))}
        </select>
      </label>

      <Button labelKey="profile.save" variant="primary" onClick={handleSave} />
    </div>
  );
}