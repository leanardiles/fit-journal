-- Weekly target sets per muscle group.
--
-- The third hand-run column migration. create_all makes missing TABLES but
-- never adds a COLUMN to a table that already exists, so this runs by hand on
-- every database, local and RDS, BEFORE the Lambda is deployed.
--
--   mysql -h <DB_HOST> -u <DB_USER> -p <DB_NAME> < scripts/add_user_weekly_set_goals.sql
--
-- Safe on a live database: a nullable column with no default rewrites no rows.
-- NULL means this user has never set a goal, and the API returns an empty map
-- for them.

ALTER TABLE users ADD COLUMN user_weekly_set_goals JSON NULL;

-- Verify.
SHOW COLUMNS FROM users LIKE 'user_weekly_set_goals';

-- Seed your own goals while the editor does not exist yet. Muscle names must
-- match MuscleGroupEnum exactly, capitalised:
--   UPDATE users
--      SET user_weekly_set_goals = '{"Chest": 12, "Back": 15, "Legs": 15, "Biceps": 9, "Triceps": 9, "Shoulders": 9}'
--    WHERE user_email = 'you@example.com';
