// Weight and height units. Storage is canonical (kilograms, centimeters); the UI
// shows the user's unit and converts back at the boundary. Single source of truth
// for conversions across Profile, Exercises, and Workout.

export const KG_PER_LB = 0.45359237;
export const CM_PER_IN = 2.54;

// --- Exercise / plate weight (decimal precision) ---

// A stored (kg) weight shown in the user's unit; "" for unset/zero.
export function kgToDisplay(kg, isImperial) {
  if (kg == null || Number(kg) === 0) return "";
  if (isImperial) {
    // Round to 1 decimal so half-pound values (17.5) survive the kg round-trip
    // instead of snapping to a whole number.
    return String(Math.round((Number(kg) / KG_PER_LB) * 10) / 10);
  }
  return String(Number(kg)); // metric: show the exact stored value
}

// A displayed value (user's unit) back to canonical kg.
export function displayToKg(display, isImperial) {
  const num = String(display).trim() === "" ? 0 : Number(display);
  if (Number.isNaN(num)) return 0;
  return isImperial ? Number((num * KG_PER_LB).toFixed(2)) : num;
}

// --- Body weight (whole-number precision) ---

export function kgToLb(kg) {
  if (kg == null || kg === "") return "";
  return Math.round(kg / KG_PER_LB);
}
export function lbToKg(lb) {
  return Math.round(Number(lb) * KG_PER_LB);
}

// --- Height ---

export function cmToFeetInches(cm) {
  if (cm == null || cm === "") return { feet: "", inches: "" };
  // Round to whole inches FIRST, then split, so 30cm -> 12in -> 1ft 0in
  // instead of 0ft 12in (independent rounding could yield inches === 12).
  const totalIn = Math.round(cm / CM_PER_IN);
  const feet = Math.floor(totalIn / 12);
  const inches = totalIn - feet * 12;
  return { feet, inches };
}
export function feetInchesToCm(feet, inches) {
  const f = Number(feet) || 0;
  const i = Number(inches) || 0;
  if (f === 0 && i === 0) return null;
  return Math.round((f * 12 + i) * CM_PER_IN);
}
/**
 * Short symbol for a measurement unit in the given locale, e.g. cm, kg, lb.
 *
 * Taken from Intl rather than a translated string per unit. The symbols come
 * from CLDR, so adding a language needs no new keys and cannot get them
 * wrong, and locales that abbreviate differently are handled for free.
 * The unit name must be one of the sanctioned ECMA-402 identifiers
 * ("centimeter", "kilogram", "pound", "foot", "inch").
 */
export function unitSymbol(locale, unit) {
  try {
    const parts = new Intl.NumberFormat(locale, {
      style: "unit",
      unit,
      unitDisplay: "short",
    }).formatToParts(0);
    return parts.find((p) => p.type === "unit")?.value ?? "";
  } catch {
    return ""; // unknown unit or no Intl unit support: show the value alone
  }
}
