import i18n from "./config";

/**
 * Join a list of strings the way the active language joins lists.
 *
 * Replaces `.join(", ")`, which hardcodes an English convention. The comma
 * and space are not universal: Arabic separates with a different comma, and
 * most languages add a word before the final item rather than a comma.
 *
 * Two types, and the choice is about grammar, not taste:
 *
 *   unit         a list standing alone as a value or a label.
 *                ["Chest", "Back", "Legs"] -> "Chest, Back, Legs"
 *
 *   conjunction  a list inside a sentence, where a reader expects "and".
 *                ["Chest", "Back", "Legs"] -> "Chest, Back, and Legs"
 *
 * The rule: if the list sits inside a sentence, use conjunction. If it stands
 * alone as the value of a field, use unit.
 *
 * `locale` is injectable so this can be unit tested without initialising
 * i18next; in the app it is always the active language.
 */
export function formatList(items, { type = "unit", locale } = {}) {
  const list = (items || []).filter(Boolean).map(String);
  if (list.length === 0) return "";
  if (list.length === 1) return list[0];
  try {
    return new Intl.ListFormat(locale || i18n.language, { style: "long", type }).format(list);
  } catch {
    return list.join(", "); // unknown locale or no ListFormat: do not crash a render
  }
}
