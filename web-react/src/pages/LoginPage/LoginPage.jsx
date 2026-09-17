import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate, Link } from "react-router-dom";
import { Field } from "../../components/Field/Field";
import { Button } from "../../components/Button/Button";
import { apiPost } from "../../api/client";
import logo from "../../assets/logo-and-name-dark.png";
import "../../layouts/AuthLayout/AuthLayout.css";

export function LoginPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    setError("");
    setLoading(true);
    try {
      const res = await apiPost("/login", { user_email: email, user_password: password });
      const data = await res.json();
      if (!res.ok) {
        setError(data.detail || "Login failed");
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
        <div className="auth-form">
          <p style={{ color: "var(--text)", margin: 0 }}>{t("login.title")}</p>

          <Field
            labelKey="login.emailLabel"
            placeholderKey="login.emailPlaceholder"
            type="email"
            value={email}
            onChange={setEmail}
          />
          <Field
            labelKey="login.passwordLabel"
            placeholderKey="login.passwordPlaceholder"
            type="password"
            value={password}
            onChange={setPassword}
          />

          {error && <p style={{ color: "var(--error)", margin: 0 }}>{error}</p>}

          <Button labelKey="login.button" variant="primary" onClick={handleSubmit} />

          <Link to="/register" style={{ color: "var(--text)", textAlign: "center", fontSize: 14 }}>
            {t("login.noAccount")}
          </Link>
        </div>
      </div>
    </div>
  );
}