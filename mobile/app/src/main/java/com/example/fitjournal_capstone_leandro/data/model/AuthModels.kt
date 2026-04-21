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