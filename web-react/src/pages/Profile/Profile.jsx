import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import i18n from "../../i18n/config";
import { apiGet, apiPut, apiDelete } from "../../api/client";
import { logout } from "../../api/auth";
import { useToast } from "../../context/ToastContext";
import { useUser } from "../../context/UserContext";
import { Field } from "../../components/Field/Field";
import { Button } from "../../components/Button/Button";
import { LOCALE_OPTIONS } from "../../i18n/locales";


const KG_PER_LB = 0.45359237;
const CM_PER_IN = 2.54;

function cmToFeetInches(cm) {
  if (cm == null || cm === "") return { feet: "", inches: "" };
  const totalIn = cm / CM_PER_IN;
  const feet = Math.floor(totalIn / 12);
  const inches = Math.round(totalIn - feet * 12);
  return { feet, inches };
}
function feetInchesToCm(feet, inches) {
  const f = Number(feet) || 0;
  const i = Number(inches) || 0;
  if (f === 0 && i === 0) return null;
  return Math.round((f * 12 + i) * CM_PER_IN);
}
function kgToLb(kg) {
  if (kg == null || kg === "") return "";
  return Math.round(kg / KG_PER_LB);
}

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
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // Delete-account (danger zone) state
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
  const { feet, inches } = cmToFeetInches(heightCm);

  const handleSave = async () => {
    setSaving(true);
    try {
      const userId = localStorage.getItem("user_id");
      const payload = {
        ...form,
        user_age: form.user_age === "" ? null : Number(form.user_age),
        user_height: heightCm === "" ? null : Number(heightCm),
        user_weight: weightKg === "" ? null : Number(weightKg),
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
        // Validation error (e.g. wrong password) , show inline so it persists
        // while the user re-types, consistent with login/register.
        let detail = t("profile.deleteError");
        try { const d = await res.json(); detail = d.detail || detail; } catch { /* no body */ }
        setDeleteError(detail);
        return;
      }
      logout();
      showToast(t("profile.accountDeleted"), "success");   // success = confirmation = toast
      navigate("/login");
    } catch (e) {
      showToast(t("profile.deleteError"), "error");         // system/network error = toast
    } finally {
      setDeleting(false);
    }
  };

  if (loading) return <p>{t("common.loading")}</p>;

  const selectStyle = {
    padding: "8px 4px", border: "none", borderBottom: "1px solid var(--border)",
    background: "transparent", color: "var(--text)", fontFamily: "var(--font-body)",
    fontSize: "var(--fs)", outline: "none",
  };
  const inputStyle = { ...selectStyle };
  const labelStyle = { fontSize: 14, color: "var(--muted)", fontFamily: "var(--font-body)" };
  const rowStyle = { display: "flex", flexDirection: "column", gap: "var(--space-xs)" };

  return (
    <div style={{ maxWidth: 420, display: "flex", flexDirection: "column", gap: "var(--space-md)" }}>
      <h1 style={{ fontFamily: "var(--font-body)", color: "var(--text)" }}>{t("profile.title")}</h1>

      <label style={rowStyle}>
        <span style={labelStyle}>{t("profile.email")}</span>
        <span style={{ color: "var(--text)" }}>{email}</span>
      </label>

      <Field labelKey="profile.name" value={form.user_first_name} onChange={(v) => update("user_first_name", v)} />
      <Field labelKey="profile.age" type="number" value={String(form.user_age)} onChange={(v) => update("user_age", v)} />

      <label style={rowStyle}>
        <span style={labelStyle}>{t("profile.sex")}</span>
        <select style={selectStyle} value={form.user_sex} onChange={(e) => update("user_sex", e.target.value)}>
          <option value="">--</option>
          <option value="M">{t("profile.sexMale")}</option>
          <option value="W">{t("profile.sexFemale")}</option>
          <option value="NB">{t("profile.sexNonBinary")}</option>
        </select>
      </label>

      <label style={rowStyle}>
        <span style={labelStyle}>{t("profile.units")}</span>
        <select style={selectStyle} value={form.user_unit_preference} onChange={(e) => update("user_unit_preference", e.target.value)}>
          <option value="metric">{t("profile.metric")}</option>
          <option value="imperial">{t("profile.imperial")}</option>
        </select>
      </label>

      <label style={rowStyle}>
        <span style={labelStyle}>{t("profile.height")}</span>
        {isImperial ? (
          <div style={{ display: "flex", gap: 12 }}>
            <input style={inputStyle} type="number" placeholder={t("profile.feet")} value={feet}
              onChange={(e) => setHeightCm(feetInchesToCm(e.target.value, inches) ?? "")} />
            <input style={inputStyle} type="number" placeholder={t("profile.inches")} value={inches}
              onChange={(e) => setHeightCm(feetInchesToCm(feet, e.target.value) ?? "")} />
          </div>
        ) : (
          <input style={inputStyle} type="number" placeholder="cm" value={heightCm}
            onChange={(e) => setHeightCm(e.target.value)} />
        )}
      </label>

      <label style={rowStyle}>
        <span style={labelStyle}>{t("profile.weight")}</span>
        {isImperial ? (
          <input style={inputStyle} type="number" placeholder="lb" value={kgToLb(weightKg)}
            onChange={(e) => setWeightKg(e.target.value === "" ? "" : Math.round(Number(e.target.value) * KG_PER_LB))} />
        ) : (
          <input style={inputStyle} type="number" placeholder="kg" value={weightKg}
            onChange={(e) => setWeightKg(e.target.value)} />
        )}
      </label>

      <Field labelKey="profile.timezone" value={form.user_timezone} onChange={(v) => update("user_timezone", v)} />

      <label style={rowStyle}>
        <span style={labelStyle}>{t("profile.language")}</span>
        <select style={selectStyle} value={form.user_locale} onChange={(e) => handleLocaleChange(e.target.value)}>
          {LOCALE_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>{opt.label}</option>
          ))}
        </select>
      </label>

      <Button labelKey="profile.save" variant="primary" onClick={handleSave} />

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