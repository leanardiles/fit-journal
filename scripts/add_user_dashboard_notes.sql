-- Dashboard note preferences.
--
-- create_all makes missing TABLES but never adds a COLUMN to a table that
-- already exists, so this one has to be run by hand, the same way
-- add_user_token_version.sql was. Run it BEFORE deploying the Lambda:
-- deploying first gives a live API whose dashboard endpoints select a column
-- the database does not have.
--
--   mysql -h <DB_HOST> -u <DB_USER> -p <DB_NAME> < scripts/add_user_dashboard_notes.sql
--
-- Safe to run on a live database: adding a nullable column with no default
-- rewrites no rows. NULL means the user has never customized their dashboard,
-- and the API returns empty lists for them, which the client reads as the
-- default order.

ALTER TABLE users ADD COLUMN user_dashboard_notes JSON NULL;

-- Verify.
SHOW COLUMNS FROM users LIKE 'user_dashboard_notes';

-- Reset one user back to the default order, if ever needed:
--   UPDATE users SET user_dashboard_notes = NULL WHERE user_email = 'someone@example.com';
