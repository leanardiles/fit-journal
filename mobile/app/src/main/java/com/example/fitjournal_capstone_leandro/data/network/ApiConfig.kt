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
    const val IS_DEVELOPMENT = false

    /**
     * Physical device local IP
     *
     * Replace with your machine's IPv4 address when testing on a physical device
     * Find it by running `ipconfig` in Git Bash and looking for IPv4 Address
     */
    private const val LOCAL_IP = "192.168.1.202"

    /**
     * Detects whether the app is running on an emulator or a physical device
     */
    fun isEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || android.os.Build.BRAND.startsWith("generic")
                || android.os.Build.DEVICE.startsWith("generic")
                || android.os.Build.PRODUCT.contains("sdk")
                || android.os.Build.PRODUCT.contains("emulator")
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu"))
    }

    /**
     * Base URL for API requests
     *
     * Production:      https://app.fit-journal.com/
     * Emulator:        http://10.0.2.2:8000/  (emulator alias for localhost)
     * Physical device: http://<LOCAL_IP>:8000/ (your machine's local IP)
     */
    val BASE_URL = when {
        !IS_DEVELOPMENT -> "https://app.fit-journal.com/"
        isEmulator() -> "http://10.0.2.2:8000/"
        else -> "http://$LOCAL_IP:8000/"
    }

    /**
     * API endpoints
     */
    object Endpoints {
        const val LOGIN = "login/mobile"
        const val REGISTER = "register"
        const val GET_PROFILE = "profile/{user_id}"
        const val GET_EXERCISES = "exercises"
    }
}