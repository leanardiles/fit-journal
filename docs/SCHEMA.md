# Database Schema

FitJournal uses MySQL (8.0.45 on AWS RDS in production). The schema is managed through SQLAlchemy ORM models defined in `src/models.py`.

A core design choice is the **copy-on-registration** model for exercises: a shared `default_exercises` catalog is copied into a per-user `exercises` table when a user registers, so each user owns and can freely edit their own exercise list without affecting anyone else.

## Entity overview

```
users ──┬── exercises ──────────── workout_logs
        ├── routine_days           │
        ├── routine_muscles_per_day│
        ├── workout_state          │
        ├── workout_sessions ──────┘
        └── next_workout_selections

default_exercises   (shared catalog, copied to exercises on registration)
```

## Tables

### `users`
| Column | Type | Notes |
|---|---|---|
| `user_id` | PK | Auto-increment |
| `user_email` | VARCHAR | Unique |
| `user_password` | VARCHAR | Bcrypt hash |
| `user_first_name`, `user_last_name` | VARCHAR | Optional |
| `user_sex` | ENUM | `M`, `F`, `NB` |
| `user_age` | INT | 0–120 |
| `user_unit_preference` | ENUM | `metric`, `imperial` |
| `user_weight`, `user_height` | DECIMAL / INT | Always stored in metric; converted for display |
| `user_timezone` | VARCHAR(50) | Default `America/New_York` |
| `user_subscription` | TINYINT | 0 or 1 |
| `user_is_active` | BOOL | |
| `user_created_at`, `user_updated_at` | TIMESTAMP | |

### `default_exercises`
Shared catalog of 101 starter exercises, copied to each user on registration.

| Column | Type | Notes |
|---|---|---|
| `default_exercise_id` | PK | |
| `exercise_name` | VARCHAR(50) | |
| `exercise_muscle_group` | ENUM | 10 muscle groups |
| `exercise_link` | VARCHAR(500) | URL to exercise demo |

### `exercises`
Each user's personal copy of exercises.

| Column | Type | Notes |
|---|---|---|
| `exercise_id` | PK | |
| `exercise_name` | VARCHAR(50) | |
| `exercise_muscle_group` | ENUM | 10 muscle groups |
| `exercise_user_current_weight` | DECIMAL(5,2) | |
| `user_id` | FK → `users.user_id` | |
| `exercise_is_in_routine` | BOOL | |
| `exercise_times_performed` | INT | Auto-increments on workout completion |
| `exercise_link` | VARCHAR(500) | |
| `comments` | VARCHAR(300) | |
| `exercise_created_at`, `exercise_updated_at` | TIMESTAMP | |

### `routine_days`
| Column | Type | Notes |
|---|---|---|
| `routine_id` | PK | |
| `user_id` | FK → `users.user_id` | |
| `days_per_week` | INT | 1–7 |
| `created_at`, `updated_at` | TIMESTAMP | |

### `routine_muscles_per_day`
Which muscle groups are trained on each day of a user's routine.

| Column | Type | Notes |
|---|---|---|
| `routine_day_id` | PK | |
| `user_id` | FK → `users.user_id` | |
| `day_number` | INT | 1–7 |
| `muscle_group` | ENUM | Biceps, Back, Triceps, Shoulders, Legs, Glutes, Chest, Calves, Abs, Forearms |
| `created_at`, `updated_at` | TIMESTAMP | |

### `workout_state`
Tracks where each user is in their routine rotation.

| Column | Type | Notes |
|---|---|---|
| `state_id` | PK | |
| `user_id` | FK → `users.user_id` | Unique |
| `current_day_number` | INT | Which routine day the user is on |
| `last_workout_date` | DATE | |
| `updated_at` | TIMESTAMP | |

### `workout_sessions`
| Column | Type | Notes |
|---|---|---|
| `session_id` | PK | |
| `user_id` | FK → `users.user_id` | |
| `routine_day_number` | INT | |
| `workout_date` | DATE | Stored in the user's timezone |
| `session_order` | INT | Incrementing session counter |
| `created_at` | TIMESTAMP | |

### `workout_logs`
One row per exercise performed in a session. `weight_used` is snapshotted at log time so historical records stay accurate even if the exercise's current weight changes later.

| Column | Type | Notes |
|---|---|---|
| `log_id` | PK | |
| `user_id` | FK → `users.user_id` | |
| `session_id` | FK → `workout_sessions.session_id` | |
| `routine_day_number` | INT | |
| `exercise_id` | FK → `exercises.exercise_id` | |
| `sets_completed` | INT | |
| `reps_completed` | INT | |
| `weight_used` | DECIMAL(5,2) | Snapshot at time of workout |
| `workout_date` | DATE | |
| `created_at` | TIMESTAMP | |

### `next_workout_selections`
Tracks which exercises a user has selected for their upcoming workout.

| Column | Type | Notes |
|---|---|---|
| `selection_id` | PK | |
| `user_id` | FK → `users.user_id` | |
| `exercise_id` | FK → `exercises.exercise_id` | |
| `is_selected` | BOOL | |
| `updated_at` | TIMESTAMP | |
| | | Unique constraint on (`user_id`, `exercise_id`) |

## Notes

- **Weights are stored in metric** regardless of the user's display preference; conversion happens at the presentation layer.
- **Dates are stored in the user's local timezone** (via `pytz`) rather than UTC, to avoid off-by-one date issues when logging workouts.
- **Muscle groups are currently an ENUM.** Adding a new group requires an `ALTER TABLE` migration on three columns (`exercises`, `default_exercises`, `routine_muscles_per_day`). A future enhancement (see [ROADMAP.md](../ROADMAP.md)) replaces this with a `muscle_groups` table.