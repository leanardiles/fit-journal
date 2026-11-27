package com.example.fitjournal_capstone_leandro.data.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor that identifies this client to the backend.
 *
 * Adds X-FitJournal-Client to every outgoing request. The backend stores it on
 * registration and on every logged workout, so the admin dashboard can tell
 * Android users from web ones. That is not reconstructable later: a row
 * written without the header says "unknown" forever.
 *
 * Separate from AuthInterceptor on purpose. That one returns early when there
 * is no stored token, which is precisely the register and login calls, and
 * registration is the request whose client matters most.
 */
class ClientInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(CLIENT_HEADER, CLIENT_NAME)
            .build()

        return chain.proceed(request)
    }

    companion object {
        const val CLIENT_HEADER = "X-FitJournal-Client"
        const val CLIENT_NAME = "android"
    }
}
