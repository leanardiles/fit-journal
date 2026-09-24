import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate, Link } from "react-router-dom";
import { Button } from "../../components/Button/Button";
import { PasswordToggle } from "../../components/PasswordToggle/PasswordToggle";
import { apiPost } from "../../api/client";
import logo from "../../assets/logo-and-name-dark.png";
import "../../layouts/AuthLayout/AuthLayout.css";

export function LoginPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    setError("");
    setLoading(true);
    try {
      const res = await apiPost("/login", { user_email: email, user_password: password });
      const data = await res.json();
      if (!res.ok) {
        const detail = data.detail;
        if (res.status === 403 && detail && typeof detail === "object" && detail.code === "email_not_verified") {
          navigate("/verify-email", { state: { email } });
          return;
        }
        setError(typeof detail === "string" ? detail : (detail?.message || "Login failed"));
        return;
      }
      localStorage.setItem("token", data.access_token);
      localStorage.setItem("user_id", data.user_id);
      navigate("/dashboard");
    } catch (e) {
      setError("Network error , is the backend running?");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-header">
        <img src={logo} alt={t("appName")} />
      </div>

      <div className="auth-body">
        <div className="auth-sheet">
          <div className="auth-line" />
          <div className="auth-line auth-line--title">{t("login.title")}</div>
          <div className="auth-line" />

          <div className="auth-line">
            <label className="auth-field">
              <span className="auth-field-label">{t("login.emailLabel")}</span>
              <input
                className="auth-field-input"
                type="email"
                value={email}
                placeholder={t("login.emailPlaceholder")}
                onChange={(e) => setEmail(e.target.value)}
              />
            </label>
          </div>

          <div className="auth-line">
            <label className="auth-field">
              <span className="auth-field-label">{t("login.passwordLabel")}</span>
              <input
                className="auth-field-input"
                type={showPassword ? "text" : "password"}
                value={password}
                placeholder={t("login.passwordPlaceholder")}
                onChange={(e) => setPassword(e.target.value)}
              />
              <PasswordToggle
                shown={showPassword}
                onToggle={() => setShowPassword((s) => !s)}
                showLabel={t("auth.showPassword")}
                hideLabel={t("auth.hidePassword")}
              />
            </label>
          </div>

          {/* placeholder , wires to a password-reset flow later */}
          <div className="auth-line auth-line--right">
            <button type="button" className="auth-forgot" onClick={() => navigate("/forgot-password")}>{t("login.forgotPassword")}</button>
          </div>

          {error && <div className="auth-line auth-line--error">{error}</div>}

          <div className="auth-line" />
          <div className="auth-line auth-line--action">
            <Button labelKey="login.button" variant="yellow" onClick={handleSubmit} />
          </div>
          <div className="auth-line" />

          <div className="auth-line auth-line--link">
            <Link to="/register" className="auth-link">{t("login.noAccount")}</Link>
          </div>
          <div className="auth-line" />
        </div>
      </div>
    </div>
  );
}