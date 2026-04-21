package com.example.fitjournal_capstone_leandro.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure token storage using EncryptedSharedPreferences
 *
 * Handles storing and retrieving JWT tokens securely
 * Tokens are encrypted using Android Keystore
 */
class TokenManager(context: Context) {

    /**
     * Master key for encryption
     *
     * Created using Android Keystore (hardware-backed security)
     * The actual key never leaves secure hardware
     */
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)  // AES-256 encryption
        .build()

    /**
     * Encrypted SharedPreferences instance
     *
     * All data stored here is automatically encrypted/decrypted
     */
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,          // File name
        masterKey,           // Encryption key
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,    // Key encryption
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM   // Value encryption
    )

    /**
     * Save JWT token
     *
     * @param token The JWT token to store securely
     */
    fun saveToken(token: String) {
        sharedPreferences.edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    /**
     * Get stored JWT token
     *
     * @return The JWT token, or null if not found
     */
    fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    /**
     * Save user ID
     *
     * @param userId The user's ID
     */
    fun saveUserId(userId: Int) {
        sharedPreferences.edit()
            .putInt(KEY_USER_ID, userId)
            .apply()
    }

    /**
     * Get stored user ID
     *
     * @return The user ID, or -1 if not found
     */
    fun getUserId(): Int {
        return sharedPreferences.getInt(KEY_USER_ID, -1)
    }

    /**
     * Save user email
     *
     * @param email The user's email
     */
    fun saveUserEmail(email: String) {
        sharedPreferences.edit()
            .putString(KEY_USER_EMAIL, email)
            .apply()
    }

    /**
     * Get stored user email
     *
     * @return The user email, or null if not found
     */
    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Check if user is logged in
     *
     * @return true if token exists, false otherwise
     */
    fun isLoggedIn(): Boolean {
        return getToken() != null
    }

    /**
     * Clear all stored authentication data (logout)
     */
    fun clearAll() {
        sharedPreferences.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "fitjournal_secure_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
    }
}