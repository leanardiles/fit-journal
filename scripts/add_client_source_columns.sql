-- Which client a signup and a logged workout came from.
--
-- The fifth hand-run column migration. create_all makes missing TABLES but
-- never adds a COLUMN to a table that already exists, so this runs by hand on
-- every database, local and RDS, BEFORE the Lambda is deployed.
--
--   mysql -h <DB_HOST> -u <DB_USER> -p <DB_NAME> < scripts/add_client_source_columns.sql
--
-- Safe on a live database: nullable columns with no default rewrite no rows.
--
-- NULL means unknown. Every row written before this migration says NULL
-- forever, and no later analysis recovers which app it came from. That is the
-- whole reason this ships ahead of the dashboard that reads it: the window
-- closes at the Play Store launch.

ALTER TABLE users            ADD COLUMN signup_source VARCHAR(20) NULL;
ALTER TABLE workout_sessions ADD COLUMN source        VARCHAR(20) NULL;

-- Verify.
SHOW COLUMNS FROM users            LIKE 'signup_source';
SHOW COLUMNS FROM workout_sessions LIKE 'source';

-- What the dashboard will group on, once there is data:
--   SELECT COALESCE(signup_source, 'unknown') AS client, COUNT(*)
--     FROM users GROUP BY client ORDER BY COUNT(*) DESC;
