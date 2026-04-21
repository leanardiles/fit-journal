package com.example.fitjournal_capstone_leandro.data.network

/**
 * API Configuration for FitJournal
 *
 * Controls which backend URL to use (local development vs production)
 * Toggle IS_DEVELOPMENT to switch between environments
 */
object ApiConfig {

    /**
     * Development mode flag
     *
     * true  = Use local FastAPI backend (for development)
     * false = Use production backend (after deployment)
     */
    const val IS_DEVELOPMENT = true

    /**
     * Base URL for API requests
     *
     * Development: http://10.0.2.2:8000/
     *   - 10.0.2.2 is Android emulator's way to access localhost
     *   - Points to FastAPI running on your computer
     *
     * Production: https://api.fitjournal.com/
     *   - Will point to deployed backend (AWS, Railway, etc.)
     */
    val BASE_URL = if (IS_DEVELOPMENT) {
        "http://10.0.2.2:8000/"  // Local development
    } else {
        "https://api.fitjournal.com/"  // Production (change when deployed)
    }

    /**
     * API endpoints
     */
    object Endpoints {
        const val LOGIN = "login/mobile"
        const val REGISTER = "register"
        const val GET_PROFILE = "profile/{user_id}"
        const val GET_EXERCISES = "exercises"
        // Add more endpoints as we build them
    }
}