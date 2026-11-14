-- Migration: add users.token_version, the session revocation counter.
--
-- Context: sessions are stateless JWTs, so until now nothing could invalidate
-- one. A token stayed valid for its full 30 days however compromised the
-- account turned out to be, and a password reset left an attacker's existing
-- token working. Every issued token now carries this value as a `ver` claim
-- and get_current_user refuses a token whose claim no longer matches, so
-- bumping the column ends every outstanding session for that user at once.
--
-- This one does need running by hand: the app calls
-- Base.metadata.create_all at import, which creates missing TABLES but never
-- adds a column to an existing one.
--
-- Apply to the local database first, then RDS. Snapshot RDS before running it,
-- per the standing rule. Safe to run on a live database: the column has a
-- default, so existing rows get 0 and no row is rewritten by hand.
--
-- After deploying the matching backend, every token issued before this change
-- is refused, because it carries no `ver` claim at all. Expect to sign in
-- again once on each client. That is the intended effect: it retires the
-- long-lived tokens already in circulation.

ALTER TABLE users
    ADD COLUMN token_version INT NOT NULL DEFAULT 0;

-- Verify:
--   SHOW COLUMNS FROM users LIKE 'token_version';
--   SELECT user_id, token_version FROM users;

-- To revoke one user's sessions by hand (support path, no endpoint yet):
--   UPDATE users SET token_version = token_version + 1 WHERE user_id = ?;
