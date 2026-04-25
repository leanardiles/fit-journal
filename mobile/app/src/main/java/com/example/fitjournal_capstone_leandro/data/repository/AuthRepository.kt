package com.example.fitjournal_capstone_leandro.data.repository

import com.example.fitjournal_capstone_leandro.data.local.TokenManager
import com.example.fitjournal_capstone_leandro.data.model.LoginRequest
import com.example.fitjournal_capstone_leandro.data.model.RegisterRequest
import com.example.fitjournal_capstone_leandro.data.model.User
import com.example.fitjournal_capstone_leandro.data.model.UserProfile
import com.example.fitjournal_capstone_leandro.data.network.RetrofitClient

/**
 * Authentication Repository
 *
 * The single source of truth for auth operations.
 * Sits between the ViewModel and the network/storage layers.
 *
 * Responsibilities:
 * - Call the API (via RetrofitClient)
 * - Store/clear the token (via TokenManager)
 * - Return a clean Result to the ViewModel (no raw exceptions)
 */
class AuthRepository(
    private val tokenManager: TokenManager
) {

    // Direct reference to the API service
    private val apiService = RetrofitClient.apiService

    /**
     * Login
     *
     * 1. Sends credentials to POST /login/mobile
     * 2. On success: saves token, userId, email to secure storage
     * 3. Returns Result.success(User) or Result.failure(exception)
     *
     * @param email    User's email address
     * @param password User's password (plain text — HTTPS encrypts in transit)
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            // Build request body
            val request = LoginRequest(
                user_email = email,
                user_password = password
            )

            // Make the API call (suspends here until response arrives)
            val response = apiService.login(request)

            // Save everything to encrypted storage
            tokenManager.saveToken(response.access_token)
            tokenManager.saveUserId(response.user_id)
            tokenManager.saveUserEmail(response.user_email)

            // Return a clean User object to the ViewModel
            Result.success(
                User(
                    userId = response.user_id,
                    email = response.user_email
                )
            )

        } catch (e: Exception) {
            // Network error, wrong credentials, server down, etc.
            Result.failure(e)
        }
    }

    /**
     * Register
     *
     * 1. Sends credentials to POST /register
     * 2. Does NOT auto-login (user must login separately after registering)
     * 3. Returns Result.success(Unit) or Result.failure(exception)
     *
     * @param email    Desired email address
     * @param password Desired password
     */
    suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            val request = RegisterRequest(
                user_email = email,
                user_password = password
            )

            // We get back user details but don't need them here
            apiService.register(request)

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * Fetch user profile from backend
     *
     * @return Result.success(UserProfile) or Result.failure(exception)
     */
    suspend fun getProfile(): Result<UserProfile> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))

            val profile = apiService.getProfile(userId)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * Logout
     *
     * Clears all stored auth data locally.
     * No API call needed — JWT is stateless on the backend.
     */
    fun logout() {
        tokenManager.clearAll()
    }

    /**
     * Check if user is already logged in
     *
     * Used at app startup to decide whether to show
     * the login screen or go straight to the dashboard.
     */
    fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn()
    }

    /**
     * Get the currently logged-in user
     *
     * Returns null if no user is stored (not logged in).
     */
    fun getCurrentUser(): User? {
        val userId = tokenManager.getUserId()
        val email = tokenManager.getUserEmail()

        // Both must exist for a valid session
        return if (userId != -1 && email != null) {
            User(userId = userId, email = email)
        } else {
            null
        }
    }
}