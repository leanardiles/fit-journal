import { useState } from "react";
import { useToast } from "../../context/ToastContext";
import { useTranslation } from "react-i18next";
import { useNavigate, Link } from "react-router-dom";
import { Button } from "../../components/Button/Button";
import { PasswordToggle } from "../../components/PasswordToggle/PasswordToggle";
import { apiPost } from "../../api/client";
import logo from "../../assets/logo-and-name-dark.png";
import "../../layouts/AuthLayout/AuthLayout.css";

export function RegisterPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [repeatPassword, setRepeatPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showRepeat, setShowRepeat] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    setError("");
    if (password !== repeatPassword) {
      setError(t("register.passwordMismatch"));
      return;
    }
    setLoading(true);
    try {
      const res = await apiPost("/register", { user_email: email, user_password: password });
      const data = await res.json();
      if (!res.ok) {
        setError(data.detail || "Registration failed");
        return;
      }
      showToast(t("register.success"), "success");
      navigate("/verify-email", { state: { email } });
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
          <div className="auth-line auth-line--title">{t("register.title")}</div>
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

          <div className="auth-line">
            <label className="auth-field">
              <span className="auth-field-label">{t("register.repeatPasswordLabel")}</span>
              <input
                className="auth-field-input"
                type={showRepeat ? "text" : "password"}
                value={repeatPassword}
                placeholder={t("login.passwordPlaceholder")}
                onChange={(e) => setRepeatPassword(e.target.value)}
              />
              <PasswordToggle
                shown={showRepeat}
                onToggle={() => setShowRepeat((s) => !s)}
                showLabel={t("auth.showPassword")}
                hideLabel={t("auth.hidePassword")}
              />
            </label>
          </div>

          {error && <div className="auth-line auth-line--error">{error}</div>}

          <div className="auth-line" />
          <div className="auth-line auth-line--action">
            <Button labelKey="register.button" variant="yellow" onClick={handleSubmit} />
          </div>
          <div className="auth-line" />

          <div className="auth-line auth-line--link">
            <Link to="/login" className="auth-link">{t("register.haveAccount")}</Link>
          </div>
          <div className="auth-line" />
        </div>
      </div>
    </div>
  );
}