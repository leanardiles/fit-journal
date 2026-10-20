import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";

// Import the catalogs.
// Base 'es-419' holds general Latin American Spanish (DeepL's code for it).
// es-AR overrides it where Argentina differs (e.g. voseo) and inherits the rest
// via the fallback chain. Plain 'es' and other es-XX tags alias to es-419.
import enCommon from "./locales/en/common.json";
import es419Common from "./locales/es-419/common.json";
import esARCommon from "./locales/es-AR/common.json";
import nlCommon from "./locales/nl/common.json";
import frCommon from "./locales/fr/common.json";
import { pseudo } from "./pseudo";


// Supported locales and their catalogs.
const resources = {
  "en":     { common: enCommon },
  "es-419": { common: es419Common },
  "es-AR":  { common: esARCommon },
  "nl":     { common: nlCommon },
  "fr":     { common: frCommon },
};

// Dev-only pseudo-locale (en-XA): reuse the English catalog and register the
// pseudo postProcessor, which distorts strings to surface expansion and
// hardcoded-string bugs. Never added in production.
if (import.meta.env.DEV) {
  resources["en-XA"] = { common: enCommon };
  i18n.use(pseudo);
}

i18n
  .use(LanguageDetector)        // detect the user's language (browser, localStorage, etc.)
  .use(initReactI18next)        // wire i18next into React
  .init({
    resources,
    // Explicit fallback chains (mirrors the backend locale model):
    //   es-AR  -> es-419 -> en   (Argentina overrides base LatAm Spanish, then English)
    //   es-419 -> en
    //   es     -> es-419 -> en   (legacy/alias; also catches es-XX browsers such as
    //                             es-MX, es-CL, which land on es-419 via the language
    //                             part, so no Spanish speaker falls through to English)
    //   everything else -> en
    fallbackLng: {
      "es-AR":   ["es-419", "en"],
      "es-419":  ["en"],
      "es":      ["es-419", "en"],
      "default": ["en"],
    },
    defaultNS: "common",        // default namespace, so t("login.button") works
    interpolation: {
      escapeValue: false,       // React already escapes, so i18next shouldn't
    },
    postProcess: import.meta.env.DEV ? ["pseudo"] : [],
  });

export default i18n;
