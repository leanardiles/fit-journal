import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import i18n from "../../i18n/config";
import { apiGet, apiPut, apiDelete } from "../../api/client";
import { logout } from "../../api/auth";
import { useToast } from "../../context/ToastContext";
import { useUser } from "../../context/UserContext";
import { LOCALE_OPTIONS } from "../../i18n/locales";
import { TIMEZONE_GROUPS } from "../../constants/timezones";
import { cmToFeetInches, feetInchesToCm, kgToLb, lbToKg } from "../../constants/units";
import { Button } from "../../components/Button/Button";
import { Select } from "../../components/Select/Select";
import { Toggle } from "../../components/Toggle/Toggle";
import "./Profile.css";

export function Profile() {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const { user, setUser } = useUser();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    user_first_name: "", user_age: "", user_sex: "",
    user_unit_preference: "metric", user_timezone: "", user_locale: "",
  });
  const [heightCm, setHeightCm] = useState("");
  const [weightKg, setWeightKg] = useState("");
  // Free-typing display buffers for imperial fields. They only flush to the
  // canonical cm/kg on blur, so an in-progress entry is never converted mid-type.
  const [heightImp, setHeightImp] = useState({ feet: "", inches: "" });
  const [weightLb, setWeightLb] = useState("");
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [deletePassword, setDeletePassword] = useState("");
  const [deleteError, setDeleteError] = useState("");
  const [deleting, setDeleting] = useState(false);

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
          setHeightCm(data.user_height ?? "");
          setWeightKg(data.user_weight ?? "");
        }
      })
      .finally(() => setLoading(false));
  }, []);

  const update = (key, value) => setForm((prev) => ({ ...prev, [key]: value }));
  const handleLocaleChange = (value) => { update("user_locale", value); i18n.changeLanguage(value); };
  const isImperial = form.user_unit_preference === "imperial";

  // Keep the imperial buffers in sync with the canonical values: on load, on a
  // unit switch to imperial, and after a blur commits a normalized value. Does
  // not fire while typing (heightCm/weightKg don't change until blur).
  useEffect(() => {
    if (!isImperial) return;
    const { feet, inches } = cmToFeetInches(heightCm);
    setHeightImp({ feet: feet === "" ? "" : String(feet), inches: inches === "" ? "" : String(inches) });
  }, [heightCm, isImperial]);

  useEffect(() => {
    if (!isImperial) return;
    setWeightLb(weightKg === "" || weightKg == null ? "" : String(kgToLb(weightKg)));
  }, [weightKg, isImperial]);

  const commitHeight = () => setHeightCm(feetInchesToCm(heightImp.feet, heightImp.inches) ?? "");
  const commitWeight = () =>
    setWeightKg(weightLb === "" ? "" : lbToKg(weightLb));

  // Timezone options: grouped by region with translated headers. If the user's
  // stored zone isn't in our list, keep it selectable so saving never silently
  // changes it.
  const allZoneValues = TIMEZONE_GROUPS.flatMap((g) => g.zones.map((z) => z.value));
  const timezoneOptions = [
    ...(form.user_timezone && !allZoneValues.includes(form.user_timezone)
      ? [{ value: form.user_timezone, label: form.user_timezone }]
      : []),
    ...TIMEZONE_GROUPS.map((g) => ({ label: t(g.labelKey), options: g.zones })),
  ];

  const handleSave = async () => {
    setSaving(true);
    try {
      const userId = localStorage.getItem("user_id");
      // Derive canonical from the live imperial buffers so a not-yet-blurred edit
      // still saves correctly; metric binds to the canonical state directly.
      const heightForSave = isImperial
        ? feetInchesToCm(heightImp.feet, heightImp.inches)
        : heightCm === "" ? null : Number(heightCm);
      const weightForSave = isImperial
        ? weightLb === "" ? null : lbToKg(weightLb)
        : weightKg === "" ? null : Number(weightKg);
      const payload = {
        ...form,
        user_age: form.user_age === "" ? null : Number(form.user_age),
        user_height: heightForSave,
        user_weight: weightForSave,
      };
      const res = await apiPut(`/profile/${userId}`, payload);
      if (!res.ok) {
        let detail = t("profile.saveError");
        try { const d = await res.json(); detail = d.detail || detail; } catch { /* no body */ }
        showToast(detail, "error");
        return;
      }
      const updated = await res.json();
      if (setUser) setUser(updated);
      showToast(t("profile.saved"), "success");
    } catch (e) {
      showToast(t("profile.saveError"), "error");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    setDeleting(true);
    setDeleteError("");
    try {
      const userId = localStorage.getItem("user_id");
      const res = await apiDelete(`/account/${userId}`, { user_password: deletePassword });
      if (!res.ok) {
        let detail = t("profile.deleteError");
        try { const d = await res.json(); detail = d.detail || detail; } catch { /* no body */ }
        setDeleteError(detail);
        return;
      }
      logout();
      showToast(t("profile.accountDeleted"), "success");
      navigate("/login");
    } catch (e) {
      showToast(t("profile.deleteError"), "error");
    } finally {
      setDeleting(false);
    }
  };

  if (loading) return <p>{t("common.loading")}</p>;

  // Kept for the Danger Zone block below (inline-styled, unchanged).
  const inputStyle = {
    padding: "8px 4px", border: "none", borderBottom: "1px solid var(--border)",
    background: "transparent", color: "var(--text)", fontFamily: "var(--font-body)",
    fontSize: "var(--fs)", outline: "none",
  };
  const labelStyle = { fontSize: 14, color: "var(--muted)", fontFamily: "var(--font-body)" };
  const rowStyle = { display: "flex", flexDirection: "column", gap: "var(--space-xs)" };

  return (
    <div className="pf">
      <h1 className="pf-title">{t("profile.title")}</h1>

      <div className="pf-row">
        <span className="pf-label">{t("profile.email")}</span>
        <span className="pf-value">{email}</span>
      </div>

      <div className="pf-row">
        <span className="pf-label">{t("profile.name")}</span>
        <input
          className="pf-input"
          type="text"
          value={form.user_first_name}
          onChange={(e) => update("user_first_name", e.target.value)}
        />
      </div>

      <div className="pf-row">
        <span className="pf-label">{t("profile.age")}</span>
        <input
          className="pf-input"
          type="number"
          value={String(form.user_age)}
          onChange={(e) => update("user_age", e.target.value)}
        />
      </div>

      <div className="pf-row">
        <span className="pf-label">{t("profile.sex")}</span>
        <Toggle
          value={form.user_sex}
          onChange={(v) => update("user_sex", v)}
          options={[
            { value: "M", label: t("profile.sexMale") },
            { value: "W", label: t("profile.sexFemale") },
            { value: "NB", label: t("profile.sexNonBinary") },
          ]}
        />
      </div>

      <div className="pf-row">
        <span className="pf-label">{t("profile.units")}</span>
        <Toggle
          value={form.user_unit_preference}
          onChange={(v) => update("user_unit_preference", v)}
          options={[
            { value: "metric", label: t("profile.metric") },
            { value: "imperial", label: t("profile.imperial") },
          ]}
        />
      </div>

      <div className="pf-row">
        <span className="pf-label">{t("profile.height")}</span>
        {isImperial ? (
          <div className="pf-height">
            <input className="pf-input" type="number" placeholder={t("profile.feet")} value={heightImp.feet}
              onChange={(e) => setHeightImp((p) => ({ ...p, feet: e.target.value }))}
              onBlur={commitHeight} />
            <input className="pf-input" type="number" placeholder={t("profile.inches")} value={heightImp.inches}
              onChange={(e) => setHeightImp((p) => ({ ...p, inches: e.target.value }))}
              onBlur={commitHeight} />
          </div>
        ) : (
          <input className="pf-input" type="number" placeholder="cm" value={heightCm}
            onChange={(e) => setHeightCm(e.target.value)} />
        )}
      </div>

      <div className="pf-row">
        <span className="pf-label">{t("profile.weight")}</span>
        {isImperial ? (
          <input className="pf-input" type="number" placeholder="lb" value={weightLb}
            onChange={(e) => setWeightLb(e.target.value)}
            onBlur={commitWeight} />
        ) : (
          <input className="pf-input" type="number" placeholder="kg" value={weightKg}
            onChange={(e) => setWeightKg(e.target.value)} />
        )}
      </div>

      <div className="pf-row">
        <span className="pf-label">{t("profile.timezone")}</span>
        <Select
          value={form.user_timezone}
          onChange={(v) => update("user_timezone", v)}
          options={timezoneOptions}
        />
      </div>

      <div className="pf-row">
        <span className="pf-label">{t("profile.language")}</span>
        <Select
          value={form.user_locale}
          onChange={handleLocaleChange}
          options={LOCALE_OPTIONS}
        />
      </div>

      <div className="pf-actions">
        <Button labelKey="profile.save" variant="yellow" onClick={handleSave} />
      </div>

      {/* ---------- Danger Zone ---------- */}
      <div style={{
        marginTop: "var(--space-lg)", paddingTop: "var(--space-md)",
        borderTop: "1px solid var(--error)",
        display: "flex", flexDirection: "column", gap: "var(--space-sm)",
      }}>
        <span style={{ color: "var(--error)", fontFamily: "var(--font-body)" }}>
          {t("profile.dangerZone")}
        </span>

        {!confirmingDelete ? (
          <button
            onClick={() => setConfirmingDelete(true)}
            style={{
              background: "transparent", border: "1px solid var(--error)", color: "var(--error)",
              fontFamily: "var(--font-body)", fontSize: 14, padding: "6px 16px",
              borderRadius: "var(--radius)", cursor: "pointer", alignSelf: "flex-start",
            }}
          >
            {t("profile.deleteAccount")}
          </button>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "var(--space-sm)" }}>
            <span style={{ color: "var(--text)", fontFamily: "var(--font-body)", fontSize: 14 }}>
              {t("profile.deleteWarning")}
            </span>
            <label style={rowStyle}>
              <span style={labelStyle}>{t("profile.confirmPassword")}</span>
              <input style={inputStyle} type="password" value={deletePassword}
                onChange={(e) => setDeletePassword(e.target.value)} />
            </label>
            {deleteError && (
              <span style={{ color: "var(--error)", fontFamily: "var(--font-body)", fontSize: 14 }}>
                {deleteError}
              </span>
            )}
            <div style={{ display: "flex", gap: 12 }}>
              <button
                onClick={handleDelete} disabled={deleting || !deletePassword}
                style={{
                  background: "var(--error)", border: "none", color: "#fff",
                  fontFamily: "var(--font-body)", fontSize: 14, padding: "6px 16px",
                  borderRadius: "var(--radius)", cursor: "pointer",
                }}
              >
                {t("profile.confirmDelete")}
              </button>
              <button
                onClick={() => { setConfirmingDelete(false); setDeletePassword(""); }}
                style={{
                  background: "transparent", border: "1px solid var(--muted)", color: "var(--muted)",
                  fontFamily: "var(--font-body)", fontSize: 14, padding: "6px 16px",
                  borderRadius: "var(--radius)", cursor: "pointer",
                }}
              >
                {t("profile.cancel")}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}