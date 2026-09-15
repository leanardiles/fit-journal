"""
Central application configuration.

All environment-driven settings (secrets, database, locale) live here in a single
Pydantic v2 Settings object, read and validated once at startup. Modules import the
`settings` singleton rather than calling os.getenv directly, so configuration has one
source of truth.

Requires: pydantic-settings (Pydantic v2 moved BaseSettings to this package).
"""

from functools import lru_cache
from pathlib import Path

from dotenv import load_dotenv

from pydantic_settings import BaseSettings, SettingsConfigDict


# config.py lives in src/; the .env file is at the project root (one level up).
# Derive the path from THIS file so it loads regardless of the working directory
# (terminal, uvicorn, or Lambda all resolve to the same absolute .env path).
_ENV_PATH = Path(__file__).resolve().parent.parent / ".env"

# Load the root .env into the process environment using python-dotenv (the parser
# the rest of the app already uses). Pydantic then reads validated values from the
# environment. This is more robust than Pydantic's own env-file reader across
# working directories and file quirks.
load_dotenv(_ENV_PATH)


# ---------------------------------------------------------------------------
# Locale model (BCP 47)
# ---------------------------------------------------------------------------
# Two branching language families (English, Spanish) plus one flat language
# (Dutch). A region subtag exists only when it carries real overrides; empty
# region files that merely duplicate their parent are avoided.
#
# Fallback is declared EXPLICITLY rather than auto-computed, because the macro
# region step (es-419) is exactly where automatic tag-matching tends to differ
# between libraries. Explicit chains are unambiguous and self-documenting.

DEFAULT_LOCALE = "en"

SUPPORTED_LOCALES = ["en", "en-US", "en-GB", "es", "es-ES", "es-419", "nl"]

# Each locale -> ordered fallbacks (excluding itself). The ultimate fallback is
# DEFAULT_LOCALE ("en"). es-AR is intentionally deferred; when added it slots in
# as "es-AR": ["es-419", "es", "en"] with no code change.
LOCALE_FALLBACKS = {
    "en":     [],
    "en-US":  ["en"],
    "en-GB":  ["en"],
    "es":     ["en"],
    "es-ES":  ["es", "en"],
    "es-419": ["es", "en"],
    "nl":     ["en"],
}


def resolve_locale_chain(locale: str) -> list[str]:
    """
    Return the full lookup chain for a requested locale, most specific first,
    always ending at DEFAULT_LOCALE. Unknown locales fall back to the default.

    Examples:
        resolve_locale_chain("es-419") -> ["es-419", "es", "en"]
        resolve_locale_chain("en-GB")  -> ["en-GB", "en"]
        resolve_locale_chain("pt-BR")  -> ["en"]   (unknown -> default)
    """
    if locale not in SUPPORTED_LOCALES:
        return [DEFAULT_LOCALE]
    chain = [locale] + LOCALE_FALLBACKS.get(locale, [])
    if DEFAULT_LOCALE not in chain:
        chain.append(DEFAULT_LOCALE)
    return chain


# ---------------------------------------------------------------------------
# Settings
# ---------------------------------------------------------------------------

class Settings(BaseSettings):
    """Environment-driven configuration, validated at startup."""

    # --- Security / auth ---
    secret_key: str                       # SECRET_KEY (required; startup fails if missing)
    algorithm: str = "HS256"

    # --- Database ---
    db_host: str
    db_port: str = "3306"
    db_user: str
    db_password: str
    db_name: str
    db_echo: bool = False

    # --- Locale (exposed on settings for convenience; sourced from module consts) ---
    default_locale: str = DEFAULT_LOCALE
    supported_locales: list[str] = SUPPORTED_LOCALES

    model_config = SettingsConfigDict(
        case_sensitive=False,   # SECRET_KEY / secret_key both resolve
        extra="ignore",         # ignore unrelated env vars
    )

    @property
    def database_url(self) -> str:
        """
        SQLAlchemy URL with charset=utf8mb4 so the CONNECTION speaks full 4-byte
        UTF-8 (schema is utf8mb4 independently). See database.py.
        """
        return (
            f"mysql+pymysql://{self.db_user}:{self.db_password}"
            f"@{self.db_host}:{self.db_port}/{self.db_name}?charset=utf8mb4"
        )


@lru_cache
def get_settings() -> Settings:
    """Cached settings singleton (read/validated once)."""
    return Settings()


# Convenience singleton for `from config import settings`
settings = get_settings()