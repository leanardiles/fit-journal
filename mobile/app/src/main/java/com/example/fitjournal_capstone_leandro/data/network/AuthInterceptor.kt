package com.example.fitjournal_capstone_leandro.data.network

import com.example.fitjournal_capstone_leandro.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor for JWT Authentication
 *
 * Automatically attaches the Bearer token to every outgoing request.
 * If no token is stored (user not logged in), the request goes through unchanged.
 */
class AuthInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Get stored token
        val token = tokenManager.getToken()

        // If no token, proceed without modification (e.g. login/register calls)
        if (token == null) {
            return chain.proceed(originalRequest)
        }

        // Attach Bearer token to the request header
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}