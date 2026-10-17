-- Migration: rename the stored Spanish locale code from es to es-419.
--
-- Context: FitJournal's Spanish catalogs are Latin American Spanish, and DeepL's
-- code for that is es-419 (DeepL's "es" is European Spanish). The locale code is
-- the language's identity for DeepL Sync / Stringherd, so the app now uses es-419.
-- Users who previously saved user_locale = 'es' must keep getting Spanish.
--
-- Apply as a one-off against the target database (dev first, then prod), BEFORE
-- the backend deploy that drops the locales/es/ folder. The backend also keeps
-- 'es' as an accepted alias that falls back to es-419, so any row missed here
-- still resolves to Spanish rather than English.

UPDATE users
SET user_locale = 'es-419'
WHERE user_locale = 'es';

-- Verify (expect no 'es' rows remain; some 'es-419' / 'es-AR' may exist):
--   SELECT user_locale, COUNT(*) AS n FROM users GROUP BY user_locale ORDER BY n DESC;
