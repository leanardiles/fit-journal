package com.example.fitjournal_capstone_leandro.data.network

import com.example.fitjournal_capstone_leandro.data.local.TokenManager
import com.example.fitjournal_capstone_leandro.data.model.LoginRequest
import com.example.fitjournal_capstone_leandro.data.model.LoginResponse
import com.example.fitjournal_capstone_leandro.data.model.RegisterRequest
import com.example.fitjournal_capstone_leandro.data.model.RegisterResponse
import com.example.fitjournal_capstone_leandro.data.model.UserProfile
import retrofit2.Retrofit
import okhttp3.OkHttpClient
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


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

    @GET("profile/{user_id}")
    suspend fun getProfile(
        @Path("user_id") userId: Int
    ): UserProfile

    // TODO: Add more endpoints as we build features
    // - GET /exercises?user_id={user_id}
    // - POST /routines
    // - etc.
}


/**
 * Retrofit instance for FitJournal API
 *
 * Singleton object that creates and configures Retrofit.
 * Must be initialized with a TokenManager before use.
 * Call RetrofitClient.initialize(tokenManager) in MainActivity.
 */
object RetrofitClient {

    private var tokenManager: TokenManager? = null

    /**
     * Initialize with TokenManager
     * Call this once from MainActivity before any API calls
     */
    fun initialize(tokenManager: TokenManager) {
        this.tokenManager = tokenManager
    }

    /**
     * OkHttp client with AuthInterceptor attached
     */
    private val okHttpClient by lazy {
        val tm = tokenManager
            ?: throw IllegalStateException("RetrofitClient not initialized. Call initialize() first.")

        okhttp3.OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tm))
            .build()
    }

    /**
     * Retrofit instance with OkHttp client
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)                          // Attach OkHttp client
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * FitJournalApiService instance
     */
    val apiService: FitJournalApiService by lazy {
        retrofit.create(FitJournalApiService::class.java)
    }
}