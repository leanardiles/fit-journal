package com.example.fitjournal_capstone_leandro.data.network

import com.example.fitjournal_capstone_leandro.data.model.LoginRequest
import com.example.fitjournal_capstone_leandro.data.model.LoginResponse
import com.example.fitjournal_capstone_leandro.data.model.RegisterRequest
import com.example.fitjournal_capstone_leandro.data.model.RegisterResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST


/**
 One file, two parts:
  - Interface - Defines what endpoints exist
  - Object - Creates the Retrofit instance that implements the interface

  INTERFACE
 Retrofit API service  for FitJournal backend
 Defines all endpoints for authentication, exercises, routines, etc.
 */
interface FitJournalApiService {

    /**
     * Login endpoint for mobile
     *
     * POST /login/mobile
     *
     * @param loginRequest Email and password
     * @return LoginResponse with JWT token and user info
     */
    @POST("login/mobile")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): LoginResponse

    /**
     * Register new user
     *
     * POST /register
     *
     * @param registerRequest Email and password
     * @return RegisterResponse with user info
     */
    @POST("register")
    suspend fun register(
        @Body registerRequest: RegisterRequest
    ): RegisterResponse

    // TODO: Add more endpoints as we build features
    // - GET /profile/{user_id}
    // - GET /exercises?user_id={user_id}
    // - POST /routines
    // - etc.
}


/**
 * Retrofit instance for FitJournal API
 *
 * Singleton object that creates and configures Retrofit
 */
object RetrofitClient {

    /**
     * Create Retrofit instance with base URL and Gson converter
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)  // Use URL from ApiConfig
            .addConverterFactory(GsonConverterFactory.create())  // JSON converter
            .build()
    }

    /**
     * Create FitJournalApiService instance
     *
     * This is what you'll use to make API calls:
     * RetrofitClient.apiService.login(...)
     */
    val apiService: FitJournalApiService by lazy {
        retrofit.create(FitJournalApiService::class.java)
    }
}