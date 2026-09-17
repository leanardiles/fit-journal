import i18n from "../../i18n/config.js";
import { Field } from "./Field";

export default {
  title: "Components/Field",
  component: Field,
};

export const Email = {
  args: {
    labelKey: "login.emailLabel",
    placeholderKey: "login.emailPlaceholder",
    type: "email",
  },
};

export const Password = {
  args: {
    labelKey: "login.passwordLabel",
    type: "password",
  },
};

// Showcase: the same field label across every locale, to eyeball text length.
export const AllLocales = {
  render: () => {
    const locales = ["en", "es", "es-AR", "nl"];
    return (
      <div style={{ display: "flex", flexDirection: "column", gap: 16, maxWidth: 320 }}>
        {locales.map((lng) => {
          const t = i18n.getFixedT(lng);
          return (
            <label key={lng} style={{ display: "flex", flexDirection: "column", gap: 4, fontFamily: "sans-serif" }}>
              <span style={{ fontSize: 13, color: "#555" }}>
                <code>{lng}</code> , {t("login.emailLabel")}
              </span>
              <input
                placeholder={t("login.emailPlaceholder")}
                style={{ padding: "10px 12px", borderRadius: 8, border: "1.5px solid #ccc", fontSize: 15 }}
              />
            </label>
          );
        })}
      </div>
    );
  },
};