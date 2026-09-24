import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate, Link } from "react-router-dom";
import { useToast } from "../../context/ToastContext";
import { Button } from "../../components/Button/Button";
import { apiPost } from "../../api/client";
import logo from "../../assets/logo-and-name-dark.png";
import "../../layouts/AuthLayout/AuthLayout.css";

export function ForgotPasswordPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [email, setEmail] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    setError(""); setLoading(true);
    try {
      await apiPost("/forgot-password", { user_email: email });
      showToast(t("forgot.sent"), "success");
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
          <div className="auth-line auth-line--title">{t("forgot.title")}</div>
          <div className="auth-line" />
          <div className="auth-line">{t("forgot.instruction")}</div>
          <div className="auth-line" />

          <div className="auth-line">
            <label className="auth-field">
              <span className="auth-field-label">{t("login.emailLabel")}</span>
              <input className="auth-field-input" type="email" value={email}
                placeholder={t("login.emailPlaceholder")}
                onChange={(e) => setEmail(e.target.value)} />
            </label>
          </div>

          {error && <div className="auth-line auth-line--error">{error}</div>}

          <div className="auth-line" />
          <div className="auth-line auth-line--action">
            <Button labelKey="forgot.button" variant="yellow" onClick={handleSubmit} />
          </div>
          <div className="auth-line" />
          <div className="auth-line auth-line--link">
            <Link to="/login" className="auth-link">{t("forgot.backToLogin")}</Link>
          </div>
          <div className="auth-line" />
        </div>
      </div>
    </div>
  );
}