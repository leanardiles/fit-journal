import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate, useSearchParams, Link } from "react-router-dom";
import { Button } from "../../components/Button/Button";
import { PasswordToggle } from "../../components/PasswordToggle/PasswordToggle";
import { apiPost } from "../../api/client";
import logo from "../../assets/logo-and-name-dark.png";
import "../../layouts/AuthLayout/AuthLayout.css";

export function ResetPasswordPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") || "";

  const [password, setPassword] = useState("");
  const [repeat, setRepeat] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    setError("");
    if (password !== repeat) { setError(t("reset.mismatch")); return; }
    if (password.length < 8) { setError(t("reset.tooShort")); return; }
    setLoading(true);
    try {
      const res = await apiPost("/reset-password", { token, new_password: password });
      const data = await res.json();
      if (!res.ok) {
        setError(typeof data.detail === "string" ? data.detail : t("reset.invalidToken"));
        return;
      }
      navigate("/login");
    } catch (e) {
      setError("Network error , is the backend running?");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-header"><img src={logo} alt={t("appName")} /></div>
      <div className="auth-body">
        <div className="auth-sheet">
          <div className="auth-line" />
          <div className="auth-line auth-line--title">{t("reset.title")}</div>
          <div className="auth-line" />

          <div className="auth-line">
            <label className="auth-field">
              <span className="auth-field-label">{t("reset.passwordLabel")}</span>
              <input className="auth-field-input" type={showPassword ? "text" : "password"}
                value={password} placeholder={t("login.passwordPlaceholder")}
                onChange={(e) => setPassword(e.target.value)} />
              <PasswordToggle shown={showPassword} onToggle={() => setShowPassword((s) => !s)}
                showLabel={t("auth.showPassword")} hideLabel={t("auth.hidePassword")} />
            </label>
          </div>

          <div className="auth-line">
            <label className="auth-field">
              <span className="auth-field-label">{t("reset.repeatLabel")}</span>
              <input className="auth-field-input" type={showPassword ? "text" : "password"}
                value={repeat} placeholder={t("login.passwordPlaceholder")}
                onChange={(e) => setRepeat(e.target.value)} />
            </label>
          </div>

          {error && <div className="auth-line auth-line--error">{error}</div>}

          <div className="auth-line" />
          <div className="auth-line auth-line--action">
            <Button labelKey="reset.button" variant="yellow" onClick={handleSubmit} />
          </div>
          <div className="auth-line" />
          <div className="auth-line auth-line--link">
            <Link to="/login" className="auth-link">{t("reset.backToLogin")}</Link>
          </div>
          <div className="auth-line" />
        </div>
      </div>
    </div>
  );
}