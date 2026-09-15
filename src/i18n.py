"""
Backend i18n runtime.

Loads JSON translation catalogs from the top-level locales/ directory and
resolves keys through the fallback chain defined in config.LOCALE_FALLBACKS.

Used for strings the BACKEND itself emits (API error messages now; emails /
PDFs later). Frontend UI strings are handled client-side (React / i18next).

Key format: dotted, feature-first, matching the catalog file + nesting, e.g.
    t("errors.auth.invalidCredentials", "es-419")
resolves errors.json -> {"auth": {"invalidCredentials": ...}} walking
es-419 -> es -> en until a value is found.
"""

import json
from functools import lru_cache
from pathlib import Path
from typing import Optional

from config import DEFAULT_LOCALE, SUPPORTED_LOCALES, resolve_locale_chain

# locales/ sits at the project root; i18n.py is in src/ (one level down).
_LOCALES_DIR = Path(__file__).resolve().parent.parent / "locales"


@lru_cache(maxsize=None)
def _load_catalog(locale: str, namespace: str) -> dict:
    """
    Load one catalog file (locales/<locale>/<namespace>.json). Missing files
    return {} so a locale can hold thin overrides (only the keys that differ).
    Cached so files are read once.
    """
    path = _LOCALES_DIR / locale / f"{namespace}.json"
    if not path.exists():
        return {}
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except (json.JSONDecodeError, OSError):
        return {}


def _lookup(catalog: dict, dotted_path: list[str]):
    """Walk a nested dict by a list of keys; return None if any step is missing."""
    node = catalog
    for part in dotted_path:
        if not isinstance(node, dict) or part not in node:
            return None
        node = node[part]
    return node if isinstance(node, str) else None


def t(key: str, locale: Optional[str] = None, **kwargs) -> str:
    """
    Translate a dotted key for a locale, walking the fallback chain.

    - key: "<namespace>.<...nested>", e.g. "errors.auth.invalidCredentials"
    - locale: a BCP 47 tag; unknown/None -> DEFAULT_LOCALE
    - kwargs: values for {{placeholder}} interpolation

    Returns the resolved string, or the key itself if nothing is found (so a
    missing translation is visible rather than crashing).
    """
    if not key or "." not in key:
        return key

    namespace, *rest = key.split(".")
    if not rest:
        return key

    locale = locale or DEFAULT_LOCALE
    for loc in resolve_locale_chain(locale):
        value = _lookup(_load_catalog(loc, namespace), rest)
        if value is not None:
            return _interpolate(value, kwargs)

    # Nothing found anywhere in the chain — return the key for visibility.
    return key


def _interpolate(template: str, values: dict) -> str:
    """Replace {{name}} placeholders. Missing values are left as-is."""
    if not values:
        return template
    out = template
    for name, val in values.items():
        out = out.replace("{{" + name + "}}", str(val))
    return out


def resolve_request_locale(
    user_locale: Optional[str] = None,
    accept_language: Optional[str] = None,
) -> str:
    """
    Determine the locale to use, by priority:
        1. user_locale   (explicit, stored on the user record)
        2. accept_language (browser hint, for users who haven't chosen)
        3. DEFAULT_LOCALE

    Only supported locales are honored; anything else falls through.
    """
    # 1. explicit user preference
    if user_locale and user_locale in SUPPORTED_LOCALES:
        return user_locale

    # 2. Accept-Language header — take the first tag we support (exact, then
    #    language-only, e.g. "es-CL" -> "es").
    if accept_language:
        for part in accept_language.split(","):
            tag = part.split(";")[0].strip()
            if not tag:
                continue
            if tag in SUPPORTED_LOCALES:
                return tag
            base = tag.split("-")[0]
            if base in SUPPORTED_LOCALES:
                return base

    # 3. default
    return DEFAULT_LOCALE
