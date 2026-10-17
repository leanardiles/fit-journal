// Single source of truth for the languages the app offers.
// Adding a language = one line here (+ its catalog). No component changes.
export const LOCALE_OPTIONS = [
  { value: "en",     label: "English" },
  { value: "es-419", label: "Español (Latinoamérica)" },
  { value: "es-AR",  label: "Español (Argentina)" },
  { value: "nl",     label: "Nederlands" },
  { value: "fr",     label: "Français" },
  // { value: "ar", label: "العربية" },   // future: also flips dir to rtl
];

// Normalize a stored or detected BCP 47 tag to the option the UI shows selected.
// Legacy 'es' and any es-XX region (es-MX, es-CL, es-ES, ...) are Latin American
// Spanish (es-419); es-AR stays its own option. Everything else is returned as-is.
export function normalizeLocale(tag) {
  if (!tag) return "en";
  if (tag === "es-AR") return "es-AR";
  if (tag === "es" || tag.startsWith("es-")) return "es-419";
  return tag;
}
