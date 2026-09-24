import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate, useLocation, Link } from "react-router-dom";
import { Button } from "../../components/Button/Button";
import { apiPost } from "../../api/client";
import { useToast } from "../../context/ToastContext";
import logo from "../../assets/logo-and-name-dark.png";
import "../../layouts/AuthLayout/AuthLayout.css";

export function VerifyEmailPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { showToast } = useToast();

  const [email, setEmail] = useState(location.state?.email || "");
  const [code, setCode] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleVerify = async () => {
    setError(""); setLoading(true);
    try {
      const res = await apiPost("/verify-email", { user_email: email, code });
      const data = await res.json();
      if (!res.ok) {
        setError(typeof data.detail === "string" ? data.detail : t("verify.invalid"));
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

  const handleResend = async () => {
    setError("");
    try {
      await apiPost("/resend-verification", { user_email: email });
      showToast(t("verify.resendSent"), "success");
    } catch (e) {
      setError("Network error , is the backend running?");
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-header"><img src={logo} alt={t("appName")} /></div>
      <div className="auth-body">
        <div className="auth-sheet">
          <div className="auth-line" />
          <div className="auth-line auth-line--title">{t("verify.title")}</div>
          <div className="auth-line" />

          <div className="auth-line">
            <label className="auth-field">
              <span className="auth-field-label">{t("login.emailLabel")}</span>
              <input className="auth-field-input" type="email" value={email}
                placeholder={t("login.emailPlaceholder")}
                onChange={(e) => setEmail(e.target.value)} />
            </label>
          </div>

          <div className="auth-line">
            <label className="auth-field">
              <span className="auth-field-label">{t("verify.codeLabel")}</span>
              <input className="auth-field-input" type="text" inputMode="numeric" value={code}
                placeholder={t("verify.codePlaceholder")}
                onChange={(e) => setCode(e.target.value)} />
            </label>
          </div>

          {error && <div className="auth-line auth-line--error">{error}</div>}

          <div className="auth-line" />
          <div className="auth-line auth-line--action">
            <Button labelKey="verify.button" variant="yellow" onClick={handleVerify} />
          </div>
          <div className="auth-line auth-line--right">
            <button type="button" className="auth-forgot" onClick={handleResend}>{t("verify.resend")}</button>
          </div>
          <div className="auth-line" />
          <div className="auth-line auth-line--link">
            <Link to="/login" className="auth-link">{t("verify.backToLogin")}</Link>
          </div>
          <div className="auth-line" />
        </div>
      </div>
    </div>
  );
}