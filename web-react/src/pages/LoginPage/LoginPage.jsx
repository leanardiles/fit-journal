import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Card } from "../../components/Card/Card";
import { Field } from "../../components/Field/Field";
import { Button } from "../../components/Button/Button";
import { apiPost } from "../../api/client";
import logo from "../../assets/logo-and-name-dark.png";
import { useNavigate } from "react-router-dom";


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
      const res = await apiPost("/login", {
        user_email: email,
        user_password: password,
      });

      const data = await res.json();

      if (!res.ok) {
        // Backend returned an error (e.g. 401). data.detail is the LOCALIZED
        // message from the Phase 1 backend i18n (errors.auth.*).
        setError(data.detail || "Login failed");
        return;
      }

      // Success , store the JWT so the app stays logged in across refreshes.
      localStorage.setItem("token", data.access_token);
      localStorage.setItem("user_id", data.user_id);
      navigate("/dashboard");
      // Next (after routing is set up): navigate to /dashboard
    } catch (e) {
      setError("Network error , is the backend running?");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "var(--bg)",
      }}
    >
      <Card>
        <div style={{ display: "flex", flexDirection: "column", gap: "var(--space-md)" }}>
          <img
            src={logo}
            alt={t("appName")}
            style={{ width: "85%", maxWidth: 280, alignSelf: "center" }}
          />
          <p style={{ color: "var(--muted)", fontFamily: "var(--font-body)", margin: 0 }}>
            {t("login.title")}
          </p>

          <Field
            labelKey="login.emailLabel"
            placeholderKey="login.emailPlaceholder"
            type="email"
            value={email}
            onChange={setEmail}
          />
          <Field
            labelKey="login.passwordLabel"
            type="password"
            value={password}
            onChange={setPassword}
          />

          {error && (
            <p style={{ color: "var(--error)", fontFamily: "var(--font-body)", margin: 0 }}>
              {error}
            </p>
          )}

          <Button labelKey="login.button" variant="primary" onClick={handleSubmit} />
        </div>
      </Card>
    </div>
  );
}