import i18n from "../../i18n/config.js";
import { Card } from "./Card";
import { Field } from "../Field/Field";
import { Button } from "../Button/Button";

export default {
  title: "Components/Card",
  component: Card,
};

// A simple card with some text inside.
export const Basic = {
  render: () => (
    <Card>
      <p style={{ margin: 0 }}>Anything can go inside a Card.</p>
    </Card>
  ),
};

// A card composing Field + Button — basically a mini login form.
// This previews how the real login page will be assembled from components.
export const LoginForm = {
  render: () => (
    <Card>
      <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
        <Field labelKey="login.emailLabel" placeholderKey="login.emailPlaceholder" type="email" />
        <Field labelKey="login.passwordLabel" type="password" />
        <Button labelKey="login.button" variant="primary" />
      </div>
    </Card>
  ),
};

// The composed form across every locale — the whole mini-form localized.
export const AllLocales = {
  render: () => {
    const locales = ["en", "es", "es-AR", "nl"];
    return (
      <div style={{ display: "flex", gap: 20, flexWrap: "wrap" }}>
        {locales.map((lng) => (
          <div key={lng}>
            <code>{lng}</code>
            <div style={{ marginTop: 6 }}>
              {/* Force this subtree to render in a specific locale */}
              <LocaleBox lng={lng} />
            </div>
          </div>
        ))}
      </div>
    );
  },
};

// Helper: render the mini-form fixed to one locale.
function LocaleBox({ lng }) {
  const t = i18n.getFixedT(lng);
  return (
    <Card>
      <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
        <label style={{ display: "flex", flexDirection: "column", gap: 4, fontFamily: "sans-serif" }}>
          <span style={{ fontSize: 13, color: "#555" }}>{t("login.emailLabel")}</span>
          <input placeholder={t("login.emailPlaceholder")} style={{ padding: "10px 12px", borderRadius: 8, border: "1.5px solid #ccc", fontSize: 15 }} />
        </label>
        <label style={{ display: "flex", flexDirection: "column", gap: 4, fontFamily: "sans-serif" }}>
          <span style={{ fontSize: 13, color: "#555" }}>{t("login.passwordLabel")}</span>
          <input type="password" style={{ padding: "10px 12px", borderRadius: 8, border: "1.5px solid #ccc", fontSize: 15 }} />
        </label>
        <button style={{ background: "#FFEB3B", color: "#000", border: "none", padding: "10px 20px", borderRadius: 8, fontSize: 15, cursor: "pointer" }}>
          {t("login.button")}
        </button>
      </div>
    </Card>
  );
}