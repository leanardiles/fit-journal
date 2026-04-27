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
    val user_unit_preference: String?,
    val user_height: Float?,
    val user_weight: Float?
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
    val exercise_is_in_routine: Boolean,
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


/**
 * A single day in the routine setup request
 */
data class RoutineDay(
    val day_number: Int,
    val muscle_groups: List<String>
)

/**
 * Routine setup request body
 *
 * What we send to: POST /routine/{user_id}
 */
data class RoutineSetupRequest(
    val days_per_week: Int,
    val routine_days: List<RoutineDay>
)

/**
 * Routine response from backend
 *
 * What we receive from: GET /routine/{user_id}
 */
data class RoutineResponse(
    val days_per_week: Int,
    val routine_days: Map<String, List<String>>  // "1" -> ["Legs", "Back"]
)