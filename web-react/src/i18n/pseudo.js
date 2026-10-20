import i18n from "i18next";

// Dev-only pseudo-localizer (the `en-XA` locale). It accents the letters so any
// hardcoded (untranslated) string stands out, pads the length to simulate how
// real translations expand, and brackets each string so clipping is visible.
// Expansion defaults to "tiered" (length-based: short strings expand most) and
// can be overridden live from the dev widget via setPseudoIntensity().

const ACCENTS = { a: "á", e: "é", i: "í", o: "ó", u: "ú",
                  A: "Á", E: "É", I: "Í", O: "Ó", U: "Ú" };

// Source-length expansion, mirroring real l10n tables: short strings expand most.
function expansionRatio(len) {
  if (len <= 10) return 1.0; // buttons, nav, chips: up to +100%
  if (len <= 20) return 0.8;
  if (len <= 30) return 0.6;
  if (len <= 50) return 0.4;
  return 0.3;                // long body text: ~+30%
}

// Current stress level: "tiered" for realistic per-length expansion, or a flat
// number (e.g. 0.2 for a uniform +20%) when probing a layout's headroom.
let intensity = "tiered";
export function setPseudoIntensity(value) { intensity = value; }
export function getPseudoIntensity() { return intensity; }

function padLength(len) {
  const ratio = intensity === "tiered" ? expansionRatio(len) : Number(intensity);
  return Math.ceil(len * ratio);
}

function pseudoize(str) {
  const accented = str.replace(/[a-z]/gi, (c) => ACCENTS[c] || c);
  const pad = "·".repeat(padLength(str.length));
  return `⟦${accented} ${pad}⟧`;
}

export const pseudo = {
  type: "postProcessor",
  name: "pseudo",
  process: (value) => (i18n.language === "en-XA" ? pseudoize(value) : value),
};
