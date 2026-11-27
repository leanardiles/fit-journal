-- Admin flag, for the admin dashboard.
--
-- create_all makes missing TABLES but never adds a COLUMN to a table that
-- already exists, so this runs by hand on every database, local and RDS,
-- BEFORE the Lambda carrying the model is deployed. Database ahead of code is
-- safe; code ahead of database means every authenticated request hits an
-- unknown column.
--
--   mysql -h <DB_HOST> -u <DB_USER> -p <DB_NAME> < scripts/add_user_is_admin.sql
--
-- The daily_stats table that ships alongside this needs NO migration: it is a
-- brand new table, so create_all makes it on the first cold start after
-- deploy, the same way login_attempts arrived.
--
-- Safe on a live database: a NOT NULL column with a default fills existing
-- rows with 0, and 0 is what every existing row should say.

ALTER TABLE users ADD COLUMN is_admin TINYINT(1) NOT NULL DEFAULT 0;

-- Verify.
SHOW COLUMNS FROM users LIKE 'is_admin';

-- Grant yourself access. Nothing reads this column until the dashboard ships,
-- so it is harmless to set now.
--   UPDATE users SET is_admin = 1 WHERE user_email = 'you@example.com';
--   SELECT user_email, is_admin FROM users WHERE is_admin = 1;
