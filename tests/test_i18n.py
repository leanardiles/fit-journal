"""
i18n / locale resolution tests.

Covers the es -> es-419 rename: es-419 holds the Latin American Spanish content,
es-AR overrides it (voseo), and plain es / es-XX aliases resolve to es-419 so no
Spanish speaker falls through to English.
"""

from config import DEFAULT_LOCALE, SUPPORTED_LOCALES, resolve_locale_chain
from i18n import t, resolve_request_locale

ES419_INVALID = "Correo electrónico o contraseña incorrectos."
EN_INVALID = "Incorrect email or password."


# --- resolve_locale_chain ----------------------------------------------------

def test_chain_es419_holds_content():
    assert resolve_locale_chain("es-419") == ["es-419", "en"]

def test_chain_es_alias_through_es419():
    assert resolve_locale_chain("es") == ["es", "es-419", "en"]

def test_chain_es_ar_through_es419():
    assert resolve_locale_chain("es-AR") == ["es-AR", "es-419", "en"]

def test_chain_es_es_through_es419():
    assert resolve_locale_chain("es-ES") == ["es-ES", "es-419", "en"]

def test_chain_unknown_defaults_to_en():
    assert resolve_locale_chain("pt-BR") == ["en"]


# --- resolve_request_locale --------------------------------------------------

def test_request_user_locale_es419():
    assert resolve_request_locale(user_locale="es-419") == "es-419"

def test_request_user_locale_es_alias_still_supported():
    # A migrated row becomes es-419, but a missed legacy "es" must not fall through.
    assert "es" in SUPPORTED_LOCALES
    assert resolve_request_locale(user_locale="es") == "es"

def test_request_accept_language_region_reduces_to_es():
    assert resolve_request_locale(accept_language="es-CL,en;q=0.8") == "es"
    assert resolve_request_locale(accept_language="es-MX") == "es"

def test_request_accept_language_exact_es419():
    assert resolve_request_locale(accept_language="es-419") == "es-419"

def test_request_accept_language_es_es():
    assert resolve_request_locale(accept_language="es-ES,en;q=0.9") == "es-ES"

def test_request_unknown_and_empty_default():
    assert resolve_request_locale(accept_language="pt-BR") == DEFAULT_LOCALE
    assert resolve_request_locale() == DEFAULT_LOCALE


# --- t(): errors resolve to Spanish via es-419 -------------------------------

def test_t_es419_spanish():
    assert t("errors.auth.invalidCredentials", "es-419") == ES419_INVALID

def test_t_es_alias_gets_spanish():
    assert t("errors.auth.invalidCredentials", "es") == ES419_INVALID

def test_t_es_cl_browser_gets_spanish_end_to_end():
    loc = resolve_request_locale(accept_language="es-CL")
    assert t("errors.auth.invalidCredentials", loc) == ES419_INVALID

def test_t_es_ar_falls_back_to_es419_without_override():
    # es-AR errors.json does not override invalidCredentials -> comes from es-419.
    assert t("errors.auth.invalidCredentials", "es-AR") == ES419_INVALID

def test_t_es_ar_uses_its_own_voseo_override():
    es_ar = t("errors.auth.invalidToken", "es-AR")
    es419 = t("errors.auth.invalidToken", "es-419")
    assert es_ar != es419
    assert "Iniciá" in es_ar   # voseo
    assert "Inicia" in es419   # tuteo

def test_t_english_unchanged():
    assert t("errors.auth.invalidCredentials", "en") == EN_INVALID
