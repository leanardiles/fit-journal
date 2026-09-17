import { useState } from "react";
import { useToast } from "../../context/ToastContext";
import { useTranslation } from "react-i18next";
import { useNavigate, Link } from "react-router-dom";
import { Card } from "../../components/Card/Card";
import { Field } from "../../components/Field/Field";
import { Button } from "../../components/Button/Button";
import { apiPost } from "../../api/client";
import logo from "../../assets/logo-and-name-dark.png";

export function RegisterPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    setError("");
    setLoading(true);
    try {
      const res = await apiPost("/register", {
        user_email: email,
        user_password: password,
      });

      if (!res.ok) {
        // Error response , read the localized detail (safely).
        let detail = "Registration failed";
        try {
          const data = await res.json();
          detail = data.detail || detail;
        } catch { /* no/invalid JSON body */ }
        setError(detail);
        return;
      }

      // Success (2xx) , we don't need the body. Confirm + go to login.
      showToast(t("register.success"), "success");
      navigate("/login");
    } catch (e) {
      setError("Network error , is the backend running?");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card>
      <div style={{ display: "flex", flexDirection: "column", gap: "var(--space-md)" }}>
        <img src={logo} alt={t("appName")} style={{ width: "85%", maxWidth: 280, alignSelf: "center" }} />
        <p style={{ color: "var(--muted)", fontFamily: "var(--font-body)", margin: 0 }}>
          {t("register.title")}
        </p>

        <Field labelKey="login.emailLabel" placeholderKey="login.emailPlaceholder" type="email" value={email} onChange={setEmail} />
        <Field labelKey="login.passwordLabel" type="password" value={password} onChange={setPassword} />

        {error && <p style={{ color: "var(--error)", fontFamily: "var(--font-body)", margin: 0 }}>{error}</p>}

        <Button labelKey="register.button" variant="primary" onClick={handleSubmit} />
        <Link to="/login" style={{ color: "var(--red)", textAlign: "center", fontSize: 14 }}>
          {t("register.haveAccount")}
        </Link>
      </div>
    </Card>
  );
}