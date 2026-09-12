package com.example.fitjournal_capstone_leandro.data.model

/**
 * Data models for authentication (login, register, tokens)
 */

/**
 * Login request body
 *
 * What we send to: POST /login/mobile
 */
data class LoginRequest(
    val user_email: String,
    val user_password: String
)


/**
 * Login response
 *
 * What we receive from: POST /login/mobile
 */
data class LoginResponse(
    val access_token: String,      // JWT token
    val token_type: String,         // "bearer"
    val user_id: Int,
    val user_email: String
)


/**
 * Register request body
 *
 * What we send to: POST /register
 */
data class RegisterRequest(
    val user_email: String,
    val user_password: String
)


/**
 * Register response
 *
 * What we receive from: POST /register
 */
data class RegisterResponse(
    val user_id: Int,
    val user_email: String,
    val user_is_active: Boolean,
    val user_first_name: String?,
    val user_sex: String?,
    val user_age: Int?,
    val user_unit_preference: String?,
    val user_height: Float?,
    val user_weight: Float?
)


/**
 * User model (simplified)
 *
 * Stored locally after login
 */
data class User(
    val userId: Int,
    val email: String
)


/**
 * User profile from backend
 */
data class UserProfile(
    val user_id: Int,
    val user_email: String,
    val user_first_name: String?,
    val user_sex: String?,
    val user_age: Int?,
    val user_timezone: String? = "America/New_York",
    val user_unit_preference: String?,
    val user_height: Float?,
    val user_weight: Float?
)


/**
 * Profile update request body
 *
 * What we send to: PUT /profile/{user_id}
 */
data class UserProfileUpdate(
    val user_first_name: String? = null,
    val user_sex: String? = null,
    val user_age: Int? = null,
    val user_timezone: String? = null,
    val user_unit_preference: String? = null,
    val user_height: Float? = null,
    val user_weight: Float? = null
)


/**
 * User exercise from backend
 *
 * What we receive from: GET /exercises?user_id={id}
 */
data class UserExercise(
    val exercise_id: Int,
    val user_id: Int,
    val exercise_name: String,
    val exercise_muscle_group: String,
    val exercise_user_current_weight: Float?,
    val exercise_times_performed: Int,
    val exercise_link: String?,
    val comments: String?
)


/**
 * Create exercise request body
 *
 * What we send to: POST /exercises?user_id={id}
 */
data class CreateExerciseRequest(
    val exercise_name: String,
    val exercise_muscle_group: String,
    val exercise_user_current_weight: Float? = null,
    val exercise_link: String? = null,
    val comments: String? = null
)

data class UpdateExerciseRequest(
    val exercise_user_current_weight: Float?
)


// ===== Routine models (training-day shape) =====
// A routine is a list of days; each day is either per_muscle (a pool of
// exercises per muscle, rotated by the backend) or manual (a fixed list).
// The app READS every type; for now it only WRITES per_muscle days.

/** A muscle targeted on a per_muscle day, with how many to draw per session. */
data class TrainingDayMuscleRequest(
    val muscle_group: String,
    val exercise_count: Int = 3
)

/** Superset grouping for a manual day (write side; unused until parity). */
data class TrainingDayExerciseGroup(
    val exercise_id: Int,
    val group_index: Int,
    val position: Int
)

/** One day in the routine setup request. */
data class TrainingDayRequest(
    val day_number: Int,
    val day_type: String,                                    // "per_muscle" | "manual"
    val name: String? = null,
    val muscles: List<TrainingDayMuscleRequest> = emptyList(),   // per_muscle days
    val exercise_ids: List<Int> = emptyList(),                   // per_muscle pool / manual list
    val exercise_groups: List<TrainingDayExerciseGroup> = emptyList()  // manual grouping (parity)
)

/**
 * Routine setup request body.
 *
 * What we send to: POST /routine/{user_id}
 * days_per_week is derived by the backend from the number of days.
 */
data class RoutineSetupRequest(
    val days: List<TrainingDayRequest>
)


/** A muscle on a per_muscle day, as returned by the backend. */
data class TrainingDayMuscleResponse(
    val muscle_group: String,
    val exercise_count: Int
)

/** An exercise attached to a day, as returned by the backend. */
data class TrainingDayExerciseResponse(
    val exercise_id: Int,
    val exercise_name: String,
    val muscle_group: String,
    val position: Int? = null,
    val group_index: Int? = null        // manual grouping; null = ungrouped
)

/** One day in the routine response. */
data class TrainingDayResponse(
    val training_day_id: Int,
    val day_number: Int,
    val day_type: String,
    val name: String? = null,
    val muscles: List<TrainingDayMuscleResponse> = emptyList(),
    val exercises: List<TrainingDayExerciseResponse> = emptyList()
)

/**
 * Routine response from backend.
 *
 * What we receive from: GET /routine/{user_id}
 */
data class RoutineResponse(
    val days_per_week: Int,
    val days: List<TrainingDayResponse> = emptyList()
)

data class WorkoutSession(
    val session_id: Int,
    val user_id: Int,
    val routine_day_number: Int?,   // null = off-routine (manual) session
    val workout_date: String,
    val session_order: Int
)

data class WorkoutState(
    val state_id: Int,
    val user_id: Int,
    val current_day_number: Int,
    val last_workout_date: String?
)

// Next workout selection from backend
data class NextWorkoutSelection(
    val selection_id: Int,
    val user_id: Int,
    val exercise_id: Int,
    val is_selected: Boolean
)

// Exercise log for completing a workout
data class ExerciseLog(
    val exercise_id: Int,
    val sets_completed: Int,
    val reps_completed: Int,
    val weight_used: Float
)

// Workout complete request body
data class WorkoutCompleteRequest(
    val day_number: Int,
    val exercises: List<ExerciseLog>
)


/**
 * A single workout log entry from the backend.
 *
 * What we receive from POST /v1/workout/logs-by-sessions/{user_id}
 * (also from GET /v1/workout/logs/{user_id}, though the calendar uses
 * the by-sessions variant).
 */
data class WorkoutLog(
    val log_id: Int,
    val user_id: Int,
    val routine_day_number: Int?,   // null = off-routine (manual) log
    val exercise_id: Int,
    val sets_completed: Int,
    val reps_completed: Int,
    val weight_used: Float,
    val workout_date: String,
    val session_id: Int
)


/**
 * Request body for POST /v1/workout/logs-by-sessions/{user_id}
 *
 * Returns the logs for the given session IDs (rather than the user's
 * full log history). Used by the Calendar screen to populate the table
 * cells for a selected day's last 10 sessions.
 */
data class LogsBySessionsRequest(
    val session_ids: List<Int>
)


/**
 * Request body for POST /v1/next-workout/toggle
 *
 * Toggles whether an exercise is part of the user's next workout selection.
 * Used by the Calendar screen's tap-to-select interaction.
 */
data class ToggleSelectionRequest(
    val user_id: Int,
    val exercise_id: Int,
    val is_selected: Boolean
)


/**
 * Request body for DELETE /v1/account/{user_id}
 *
 * The account password is re-checked by the backend before deletion,
 * on top of the JWT. Sent as a body on a DELETE request.
 */
data class DeleteAccountRequest(
    val user_password: String
)