# ADR 0001: Plurals via built-in i18next suffixes, ICU deferred

**Status:** Accepted, October 2026
**Context item:** plan 1.3

## Context

Strings that count something need a different form per language, and the number
of forms is a property of the language, not of the string. English and Dutch
have two categories, French has two but puts zero in the singular, Polish has
three, Arabic has six.

Two mechanisms were available.

**Built-in i18next suffixes.** One catalog key per category, flat key and value:

```json
"exercises_one":   "{{count}} exercise",
"exercises_other": "{{count}} exercises"
```

The call site passes a count and i18next resolves the suffix. On i18next 26
with v4 JSON, which is what this project runs, resolution goes through
`Intl.PluralRules`, so the categories come from CLDR rather than from anything
hand-maintained.

**ICU MessageFormat** (through `i18next-icu`). The logic lives inside a single
message:

```
{count, plural, one {# exercise} other {# exercises}}
```

ICU is more expressive. It handles plural and select (gender, context) in one
string, and nested formatting, which matters for sentences with two variable
parts.

## Decision

Use the built-in suffixes. Defer ICU.

The deciding factor is the localization pipeline rather than the runtime.
Catalogs round-trip through machine translation (DeepL, via the Stringherd
workflow). ICU syntax inside a segment has to survive that trip intact: the
braces, the category names and the `#` are code, and an MT engine that treats
them as prose will corrupt them. Protecting them is possible, but it is a
pipeline capability to verify, not an assumption to build on.

Flat key and value pairs have no such exposure. Every TMS and MT engine
handles them, the translator sees a plain sentence, and a corrupted plural
category is not a failure mode that exists.

The expressiveness gap is also smaller than it looks here. i18next's `context`
option covers the common select case, and this app currently has four
count-bearing strings, none of which combines a plural with a gendered or
otherwise selected term.

## Consequences

Good:

1. Catalogs stay flat, so they are legible to translators and safe through MT.
2. No extra runtime dependency and no parser in the bundle.
3. Adding a language with more categories is a catalog change only. Arabic
   needs `_zero`, `_one`, `_two`, `_few`, `_many` and `_other`; the call sites
   do not change, because `Intl.PluralRules` picks the category.

Bad:

1. A sentence needing a plural and a select together has to be split into
   several keys, which is exactly the fragmentation this project otherwise
   avoids (see plan 1.2).
2. The count categories are spread across sibling keys rather than visible in
   one place, so a reviewer has to know to look for the suffixes.

## Revisit when

Either of these happens:

1. The Stringherd round-trip check confirms ICU placeholders survive machine
   translation unmodified, which removes the pipeline objection.
2. A string genuinely needs plural and select in one sentence, at which point
   splitting it into keys would hurt translation quality more than ICU would
   hurt the pipeline.
