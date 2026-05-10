package com.example.fitjournal_capstone_leandro.data.network

import com.example.fitjournal_capstone_leandro.data.local.TokenManager
import com.example.fitjournal_capstone_leandro.data.model.CreateExerciseRequest
import com.example.fitjournal_capstone_leandro.data.model.LoginRequest
import com.example.fitjournal_capstone_leandro.data.model.LoginResponse
import com.example.fitjournal_capstone_leandro.data.model.RegisterRequest
import com.example.fitjournal_capstone_leandro.data.model.RegisterResponse
import com.example.fitjournal_capstone_leandro.data.model.RoutineResponse
import com.example.fitjournal_capstone_leandro.data.model.RoutineSetupRequest
import com.example.fitjournal_capstone_leandro.data.model.UpdateExerciseRequest
import com.example.fitjournal_capstone_leandro.data.model.UserExercise
import com.example.fitjournal_capstone_leandro.data.model.UserProfile
import com.example.fitjournal_capstone_leandro.data.model.UserProfileUpdate
import com.example.fitjournal_capstone_leandro.data.model.WorkoutSession
import com.example.fitjournal_capstone_leandro.data.model.WorkoutState
import retrofit2.Retrofit
import okhttp3.OkHttpClient
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.Response


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


    /**
     * Update user profile
     *
     * PUT /profile/{user_id}
     */
    @PUT("profile/{user_id}")
    suspend fun updateProfile(
        @Path("user_id") userId: Int,
        @Body profile: UserProfileUpdate
    ): UserProfile


    /**
     * Get all exercises for a user
     *
     * GET /exercises?user_id={id}
     */
    @GET("exercises")
    suspend fun getExercises(
        @Query("user_id") userId: Int
    ): List<UserExercise>

    /**
     * Create a new exercise
     *
     * POST /exercises?user_id={id}
     */
    @POST("exercises")
    suspend fun createExercise(
        @Query("user_id") userId: Int,
        @Body exercise: CreateExerciseRequest
    ): UserExercise


    /**
     * Delete an exercise
     *
     * DELETE /exercises/{exercise_id}?user_id={id}
     */
    @DELETE("exercises/{exercise_id}")
    suspend fun deleteExercise(
        @Path("exercise_id") exerciseId: Int,
        @Query("user_id") userId: Int
    ): Response<Unit>


    /**
     * Get user's routine
     *
     * GET /routine/{user_id}
     */
    @GET("routine/{user_id}")
    suspend fun getRoutine(
        @Path("user_id") userId: Int
    ): RoutineResponse

    /**
     * Save user's routine
     *
     * POST /routine/{user_id}
     */
    @POST("routine/{user_id}")
    suspend fun saveRoutine(
        @Path("user_id") userId: Int,
        @Body routine: RoutineSetupRequest
    ): Any

    /**
     * Delete user's routine
     *
     * DELETE /routine/{user_id}
     */
    @DELETE("routine/{user_id}")
    suspend fun deleteRoutine(
        @Path("user_id") userId: Int
    ): Response<Unit>


    /**
     * Update an exercise
     *
     * PUT /exercises/{exercise_id}?user_id={id}
     */
    @PUT("exercises/{exercise_id}")
    suspend fun updateExercise(
        @Path("exercise_id") exerciseId: Int,
        @Query("user_id") userId: Int,
        @Body exercise: UpdateExerciseRequest
    ): UserExercise

    @GET("workout/sessions/{user_id}")
    suspend fun getWorkoutSessions(
        @Path("user_id") userId: Int,
        @Query("limit") limit: Int
    ): List<WorkoutSession>

    @GET("workout/state/{user_id}")
    suspend fun getWorkoutState(
        @Path("user_id") userId: Int
    ): WorkoutState
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