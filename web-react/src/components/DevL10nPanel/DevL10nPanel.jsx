import { useState } from "react";
import { useTranslation } from "react-i18next";
import { setPseudoIntensity, getPseudoIntensity } from "../../i18n/pseudo";

const PSEUDO = "en-XA";
const LEVELS = ["tiered", 0.1, 0.2, 0.5];

// Dev-only localization panel: switch into the pseudo-locale, dial its expansion,
// and flip RTL to check mirroring. Rendered only under import.meta.env.DEV.
export function DevL10nPanel() {
  const { i18n } = useTranslation();
  const [, setTick] = useState(0);
  const [lastReal, setLastReal] = useState(i18n.language === PSEUDO ? "en" : i18n.language);
  const bump = () => setTick((n) => n + 1);

  const onPseudo = i18n.language === PSEUDO;
  const isRtl = document.documentElement.getAttribute("dir") === "rtl";

  const togglePseudo = () => {
    if (onPseudo) {
      i18n.changeLanguage(lastReal);
    } else {
      setLastReal(i18n.language);
      i18n.changeLanguage(PSEUDO);
    }
  };

  const toggleRtl = () => {
    const el = document.documentElement;
    el.setAttribute("dir", isRtl ? "ltr" : "rtl");
    bump();
  };

  const pickIntensity = (v) => {
    setPseudoIntensity(v);
    if (i18n.language === PSEUDO) i18n.emit("languageChanged", PSEUDO); // re-translate in place
    else i18n.changeLanguage(PSEUDO);
    bump();
  };

  const btn = (active) => ({
    padding: "2px 8px",
    border: "1px solid #777",
    borderRadius: 4,
    cursor: "pointer",
    font: "inherit",
    background: active ? "#f4d93e" : "transparent",
    color: active ? "#1a1a1a" : "#eee",
  });

  return (
    <div
      style={{
        position: "fixed",
        bottom: 12,
        insetInlineEnd: 12,
        zIndex: 9999,
        display: "flex",
        flexDirection: "column",
        gap: 6,
        padding: 8,
        borderRadius: 8,
        background: "rgba(0,0,0,0.82)",
        color: "#eee",
        border: "1px solid #555",
        font: "12px/1.3 system-ui, sans-serif",
      }}
    >
      <strong style={{ fontWeight: 600 }}>l10n dev</strong>
      <div style={{ display: "flex", gap: 6 }}>
        <button style={btn(onPseudo)} onClick={togglePseudo}>Pseudo</button>
        <button style={btn(isRtl)} onClick={toggleRtl}>RTL</button>
      </div>
      <div style={{ display: "flex", gap: 6 }}>
        {LEVELS.map((v) => (
          <button
            key={String(v)}
            style={btn(onPseudo && getPseudoIntensity() === v)}
            onClick={() => pickIntensity(v)}
          >
            {v === "tiered" ? "Tiered" : `${v * 100}%`}
          </button>
        ))}
      </div>
    </div>
  );
}
