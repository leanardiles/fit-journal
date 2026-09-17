import i18n from "../../i18n/config.js";
import { Button } from "./Button";

// This tells Storybook which component these stories are for.
export default {
  title: "Components/Button",
  component: Button,
};

// A single default button (uses whatever locale is active).
export const Primary = {
  args: {
    labelKey: "login.button",
    variant: "primary",
  },
};

export const Secondary = {
  args: {
    labelKey: "nav.logout",
    variant: "secondary",
  },
};

// The showcase: the SAME button rendered across every locale, side by side.
// This visually demonstrates that the component adapts to locale automatically
// (and lets you eyeball text-length differences between languages).
export const AllLocales = {
  render: () => {
    const locales = ["en", "es", "es-AR", "nl"];
    return (
      <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
        {locales.map((lng) => {
          // Get a translator fixed to THIS locale (doesn't change global state).
          const t = i18n.getFixedT(lng);
          return (
            <div key={lng} style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <code style={{ width: 60 }}>{lng}</code>
              <button
                style={{
                  background: "#FFEB3B", color: "#000", border: "none",
                  padding: "10px 20px", borderRadius: 8, fontSize: 15, cursor: "pointer",
                }}
              >
                {t("login.button")}
              </button>
            </div>
          );
        })}
      </div>
    );
  },
};