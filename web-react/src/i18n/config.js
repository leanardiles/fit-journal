import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";

// Import the catalogs.
// Base 'es' holds general Latin American Spanish; es-AR overrides it where
// Argentina differs (e.g. voseo) and inherits the rest via the fallback chain.
import enCommon from "./locales/en/common.json";
import esCommon from "./locales/es/common.json";
import esARCommon from "./locales/es-AR/common.json";
import nlCommon from "./locales/nl/common.json";

i18n
  .use(LanguageDetector)        // detect the user's language (browser, localStorage, etc.)
  .use(initReactI18next)        // wire i18next into React
  .init({
    resources: {
      "en":    { common: enCommon },
      "es":    { common: esCommon },
      "es-AR": { common: esARCommon },
      "nl":    { common: nlCommon },
    },
    // Explicit fallback chains (mirrors the backend locale model):
    //   es-AR -> es -> en   (Argentina overrides base LatAm Spanish, then English)
    //   everything else -> en
    fallbackLng: {
      "es-AR": ["es", "en"],
      "default": ["en"],
    },
    defaultNS: "common",        // default namespace, so t("login.button") works
    interpolation: {
      escapeValue: false,       // React already escapes, so i18next shouldn't
    },
  });

export default i18n;